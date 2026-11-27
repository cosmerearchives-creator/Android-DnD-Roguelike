from pathlib import Path
from PIL import Image
import re,json,shutil
root=Path(r"G:/ChatGPT Projects/3d Converter/ForestsOfHellSource")
atlas=root/"app/src/main/assets/inventory_icons.webp"
index=root/"app/src/main/assets/inventory_icons.txt"
dst=root/"godot"
shutil.copy2(atlas,dst/"inventory_icons.webp")
Image.open(atlas).convert("RGBA").save(dst/"inventory_icons.png", optimize=True)
lookups={}
for line in index.read_text().splitlines():
    try:
        key,num=line.split(" ")
        lookups[key]=int(num)
    except ValueError:
        pass
(dst/"inventory_icon_map.json").write_text(json.dumps(lookups,indent=2),encoding="utf-8")
source=(root/"app/src/main/java/com/cosmerearchives/dndroguelike/Equipment.java").read_text()
pattern=r'(weapon|basic|container|make)\("([^"]+)","([^"]+)"'
items={}
for m in re.finditer(pattern,source):
    kind,key,name=m.groups()
    if key in items:continue
    # Parse the first numeric literal after the item ID and display name.
    tail=source[m.end():source.find(";",m.end())]
    numeric=re.search(r',\s*(\d+(?:\.\d+)?)(?:f)?\s*,',tail)
    weight=float(numeric.group(1)) if numeric else 0.
    if kind=="weapon":category="Weapon"
    elif kind=="container":category="Container"
    elif "Kind.ARMOR" in tail:category="Armor"
    elif "Kind.SHIELD" in tail:category="Shield"
    elif "Kind.TOOL" in tail:category="Tool"
    elif "Kind.SUPPLY" in tail:category="Supply"
    elif "Kind.MATERIAL" in tail:category="Material"
    else:category="Other"
    items[key]={"id":key,"name":name,"weight":weight,"type":category,"icon":lookups.get(key,-1)}
# Correct common constructs that don't match the static catalog regex.
items["ration"]={"id":"ration","name":"Ration (1 day)","weight":2.0,"type":"Supply","icon":lookups["ration"]}
items["shield"]["type"]="Shield"
for id,weight in [("chain_mail",55),("longsword",3),("shield",6),("handaxe",2),("backpack",5),("bedroll",7),("mess_kit",1),("tinderbox",1),("torch",1),("ration",2),("waterskin",0),("rope_hemp",10)]:
    if id in items:items[id]["weight"]=float(weight)
# The legacy catalog also constructs 17 artisan tools in a table.
names=re.search(r'String\[\] names=\{(.*?)\};',source,re.S)
artisan=re.search(r'String\[\] artisan=\{(.*?)\};',source,re.S)
lbs=re.search(r'int\[\] lbs=\{(.*?)\};',source,re.S)
if names and artisan and lbs:
    ids=re.findall(r'"([^"]+)"',artisan.group(1))
    labels=re.findall(r'"([^"]+)"',names.group(1))
    weights=[int(x) for x in re.findall(r'\d+',lbs.group(1))]
    for id,name,weight in zip(ids,labels,weights):
        items[id]={"id":id,"name":name,"weight":weight,"type":"Tool","icon":lookups.get(id,-1)}
(dst/"item_catalog.json").write_text(json.dumps(items,indent=2),encoding="utf-8")
(dst/"item_catalog.gd").write_text(
    "extends RefCounted\n## Exported from the legacy Equipment.java catalog.\nconst ITEMS = "
    + json.dumps(items,ensure_ascii=False,indent=2) + "\n", encoding="utf-8")
print("CATALOG_AND_ICONS",len(items),len(lookups),Image.open(atlas).size)
