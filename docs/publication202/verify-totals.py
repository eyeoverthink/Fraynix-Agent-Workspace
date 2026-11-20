
"""Check every published total with a separate CSV reader and decimal implementation."""
from collections import Counter, defaultdict
import csv
from decimal import Decimal
from hashlib import sha256
import json
from pathlib import Path
import sys

source = Path(sys.argv[1])
proof = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
receipt = proof["import"]
assert receipt["sha256"] == sha256(source.read_bytes()).hexdigest()
assert receipt["bytes"] == source.stat().st_size
statuses = defaultdict(Counter)
changes = defaultdict(list)
with source.open(encoding="utf-8", newline="") as file:
    for row in csv.DictReader(file, strict=True):
        statuses[row["unit"]][row["status"]] += 1
        if row["status"] == "Matched":
            changes[row["unit"]].append(Decimal(row["delta"]))
        else:
            assert not row["delta"]
assert set(statuses) == {unit["unit"] for unit in proof["totals"]}
assert sum(sum(counter.values()) for counter in statuses.values()) == receipt["source"]["groups"]
for row in proof["totals"]:
    unit = row["unit"]
    for status in ("Matched", "Added", "Missing", "Duplicate"):
        assert row[status.lower()] == statuses[unit][status]
    if changes[unit]:
        assert Decimal(row["netDelta"]) == sum(changes[unit])
        assert Decimal(row["absoluteDelta"]) == sum(map(abs, changes[unit]))
    else:
        assert row["netDelta"] is None and row["absoluteDelta"] is None
print(json.dumps({"passed": True, "groups": receipt["source"]["groups"], "units": len(statuses)}))
