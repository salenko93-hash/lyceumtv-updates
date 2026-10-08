from pathlib import Path
import argparse, hashlib, json, shutil

REQUIRED = [
    "schedule_numerator.json",
    "schedule_denominator.json",
    "shelter_numerator.json",
    "shelter_denominator.json",
]

def sha256(path: Path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

ap = argparse.ArgumentParser()
ap.add_argument("--folder", required=True)
ap.add_argument("--version", required=True)
ap.add_argument("--apk")
ap.add_argument("--apk-code", type=int)
args = ap.parse_args()

folder = Path(args.folder)
manifest = {"version": args.version, "schedules": {}}
for name in REQUIRED:
    p = folder / name
    if not p.exists():
        raise SystemExit(f"missing {p}")
    manifest["schedules"][name] = {"url": name, "sha256": sha256(p)}

for optional in ("calendar.json", "content.json"):
    p = folder / optional
    if p.exists():
        manifest[optional[:-5]] = {"url": optional, "sha256": sha256(p)}

if args.apk:
    if args.apk_code is None:
        raise SystemExit("--apk-code is required with --apk")
    out = folder / "LyceumTV.apk"
    shutil.copy2(args.apk, out)
    manifest["apk"] = {"url": out.name, "sha256": sha256(out), "versionCode": args.apk_code}

(folder / "content_manifest.json").write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(folder / "content_manifest.json")
