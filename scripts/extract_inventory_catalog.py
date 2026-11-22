"""Extract the actual in-game Equipment catalog for asset production, including generated artisan tools."""
import re,json
from pathlib import Path
src=Path("app/src/main/java/com/cosmerearchives/dndroguelike/Equipment.java").read_text()
items=[]
for match in re.finditer(r'(?:weapon|basic|container|make)\("([^"]+)","([^"]+)"',src):
    item_id,name=match.groups()
    kind="weapon" if match.group(0).startswith("weapon") else ("armor" if "Kind.ARMOR" in src[match.start():match.start()+100] else "item")
    if item_id not in {x["id"] for x in items}:items.append({"id":item_id,"name":name,"category":kind})
for key in ("artisan","names"):
    match=re.search(r'String\[\] '+key+r'=\{(.*?)\};',src,re.S)
    assert match,key
    globals()[key]=re.findall(r'"([^"]+)"',match.group(1))
assert len(artisan)==len(names)==17
for item_id,name in zip(artisan,names):
    items.append({"id":item_id,"name":name,"category":"item"})
assert len(items)>75,len(items)
out=Path("build/inventory-factory");out.mkdir(parents=True,exist_ok=True)
(out/"catalog.json").write_text(json.dumps(items,indent=2))
print("IN_GAME_ITEMS_DISCOVERED",len(items))
