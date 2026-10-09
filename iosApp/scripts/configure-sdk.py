"""Replace only the machine-specific SDK entry, preserving other local settings."""
import pathlib
import re
import sys

sdk = pathlib.Path(sys.argv[1]).resolve(strict=True)
root = pathlib.Path(__file__).resolve().parents[2]
properties = root / "local.properties"
existing = properties.read_text(encoding="utf-8") if properties.exists() else ""
# The SDK entry written by Android Studio is a single-line Java property.
lines = [line for line in existing.splitlines() if not re.match(r"\s*sdk\.dir\s*[:=]", line)]
escaped = str(sdk).replace("\\", "\\\\").replace(":", "\\:")
properties.write_text("\n".join(lines + ["sdk.dir=" + escaped]) + "\n", encoding="utf-8")
