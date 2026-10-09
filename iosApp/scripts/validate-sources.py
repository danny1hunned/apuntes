"""Static checks for Windows; these do not replace an Xcode build."""
import json
import pathlib
import plistlib
import re
import sys

root = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / "iosApp/build/validation-tools"))
import tree_sitter
import tree_sitter_swift
import yaml

parser = tree_sitter.Parser(tree_sitter.Language(tree_sitter_swift.language()))
errors = []
files = list((root / "iosApp").glob("**/*.swift"))
for path in files:
    if "build" in path.parts:
        continue
    tree = parser.parse(path.read_bytes())
    nodes = [tree.root_node]
    while nodes:
        node = nodes.pop()
        if node.type == "ERROR" or node.is_missing:
            errors.append(f"{path.relative_to(root)}:{node.start_point.row + 1}: Swift syntax {node.type}")
        nodes.extend(node.children)

for path in [root / "iosApp/project.yml", root / ".github/workflows/build-ios.yml"]:
    with path.open(encoding="utf-8") as file:
        assert isinstance(yaml.safe_load(file), dict), path

for path in (root / "iosApp/Apuntes/Assets.xcassets").glob("**/Contents.json"):
    content = json.loads(path.read_text(encoding="utf-8"))
    for image in content.get("images", []):
        if "filename" in image and image["filename"] != "AppIcon.png":
            assert (path.parent / image["filename"]).exists(), path

for path in [root / "iosApp/Apuntes/PrivacyInfo.xcprivacy", root / "shared/PrivacyInfo.xcprivacy"]:
    with path.open("rb") as file:
        plistlib.load(file)

for path in (root / "iosApp/Apuntes").glob("*.lproj/*.strings"):
    keys = re.findall(r'^"((?:[^"\\]|\\.)*)"\s*=', path.read_text(encoding="utf-8"), re.MULTILINE)
    if len(set(keys)) != len(keys):
        errors.append(f"Duplicate localization keys: {path.relative_to(root)}")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Swift syntax ({len(files)} files), project/workflow YAML, privacy plist, asset references and localization keys passed.")
