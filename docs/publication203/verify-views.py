
"""Independently derive all explorer filters from the original comparison CSV."""
from collections import Counter
import csv
from decimal import Decimal
from hashlib import sha256
import json
from pathlib import Path
import sys

source = Path(sys.argv[1])
proof = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
assert proof["import"]["sha256"] == sha256(source.read_bytes()).hexdigest()
with source.open(encoding="utf-8", newline="") as file:
    rows = list(csv.DictReader(file, strict=True))
assert len(rows) == proof["import"]["source"]["groups"]


def decimal_text(value):
    return format(value.normalize(), "f") if value else "0"


def format_change(value, unit, signed):
    if value is None:
        return "—"
    return ("+" if signed and value > 0 else "") + decimal_text(value) + " " + unit


def reading_text(cell):
    return " · ".join(f"#{row['row']}: {row['value']}" for row in json.loads(cell)) or "—"


def view(unit, status):
    visible = [row for row in rows if unit in ("all", row["unit"]) and status in ("all", row["status"])]
    totals = []
    lines = []
    for name in ("°F", "mm"):
        same_unit = [row for row in visible if row["unit"] == name]
        if not same_unit:
            continue
        counts = Counter(row["status"] for row in same_unit)
        values = [Decimal(row["delta"]) for row in same_unit if row["status"] == "Matched"]
        net = sum(values, Decimal(0)) if values else None
        absolute = sum(map(abs, values), Decimal(0)) if values else None
        totals.append(dict(unit=name, matched=counts["Matched"], added=counts["Added"], missing=counts["Missing"],
                           duplicate=counts["Duplicate"], netDelta=decimal_text(net) if net is not None else None,
                           absoluteDelta=decimal_text(absolute) if absolute is not None else None))
        count_labels = " · ".join(f"{name} {counts[name]}" for name in ("Matched", "Added", "Missing", "Duplicate"))
        lines.append(f"{name} · {count_labels} · Net {format_change(net, name, True)} · Absolute {format_change(absolute, name, False)}")
    display_rows = [[json.loads(row["key_json"]), row["unit"], row["status"], reading_text(row["earlier_readings"]),
                     reading_text(row["later_readings"]), ("+" if Decimal(row["delta"]) > 0 else "") + row["delta"]
                     if row["delta"] else "—"] for row in visible]
    return dict(unit=unit, status=status, groups=len(visible), totals=totals, lines=lines or ["No matching groups."],
                rows=display_rows)


records = [view(unit, status) for unit in ("all", "°F", "mm")
           for status in ("all", "Matched", "Added", "Missing", "Duplicate")]
for recorded in proof["views"]:
    expected = next(row for row in records if row["unit"] == recorded["unit"] and row["status"] == recorded["status"])
    assert recorded == {key: expected[key] for key in ("unit", "status", "groups", "totals")}
print(json.dumps({"passed": True, "views": records}))
