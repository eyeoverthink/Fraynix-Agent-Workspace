
"""Read the exported file independently of the Java CSV writer."""
import csv
from hashlib import sha256
import json
from pathlib import Path
import sys

source = Path(sys.argv[1])
expected = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))["outcomes"]
with source.open(encoding="utf-8", newline="") as file:
    reader = csv.reader(file, strict=True)
    assert next(reader) == ["key_json", "unit", "status", "earlier_readings", "later_readings", "delta"]
    actual = []
    for key, unit, status, earlier, later, delta in reader:
        actual.append(dict(key=json.loads(key), unit=unit, status=status, earlier=json.loads(earlier),
                           later=json.loads(later), delta=delta if delta else None))
assert actual == expected
print(json.dumps({"passed": True, "groups": len(actual), "sha256": sha256(source.read_bytes()).hexdigest()}))
