extends RefCounted
## Godot inventory data derived from the original 2014 5e Equipment catalog.
## Currently persists the initial Fighter test loadout independently of the legacy Java save.
const STARTER := [
	{"id":"leather_armor","count":1},{"id":"chain_mail","count":1},{"id":"longsword","count":1},{"id":"shield","count":1},
	{"id":"handaxe","count":2},{"id":"backpack","count":1},{"id":"bedroll","count":1},
	{"id":"mess_kit","count":1},{"id":"tinderbox","count":1},{"id":"torch","count":10},
	{"id":"ration","count":10},{"id":"waterskin","count":1},{"id":"rope_hemp","count":1}
]
const SLOT_NAMES := ["Head","Eyes","Main Hand","Off Hand","Secondary","Hands","Wrists","Body","Neck","Cloak","Left Ring","Right Ring","Accessory","Waist","Feet"]
const SLOT_SIGNS := ["\u265c","\u25c7","\u2694","\u25c6","\u279a","\u270b","\u25c8","\u25a4","\u2667","\u2726","\u25cb","\u25cb","\u2726","\u25ac","\u265f"]
const ITEM_DATA = preload("res://item_catalog.gd")
const QUEST_IDS := ["artifact_fragment","guild_letter","guild_id","employer_insignia","enemy_trophy"]
const STORAGE := "user://inventory_v1.json"
var catalog: Dictionary = {}
var atlas: Texture2D
var items: Array[Dictionary] = []
var equipped: Dictionary = {}
var strength := 10
var gold := 10
var selected_id := "ration"
func _init() -> void:
	catalog=ITEM_DATA.ITEMS.duplicate(true)
	var imported: Resource=null
	if ResourceLoader.exists("res://inventory_icons.png"):
		imported=ResourceLoader.load("res://inventory_icons.png","Texture2D")
	if imported is Texture2D:
		atlas=imported as Texture2D
	else:
		# Allows direct local script testing before the editor imports textures.
		var img:=Image.load_from_file("res://inventory_icons.png")
		if img!=null:atlas=ImageTexture.create_from_image(img)
	if not _load_saved():
		for entry in STARTER:
			items.append(entry.duplicate(true))
		equipped={"Body":"chain_mail","Main Hand":"longsword","Off Hand":"shield"}
		_save()
func _load_saved() -> bool:
	if not FileAccess.file_exists(STORAGE): return false
	var parsed: Variant = JSON.parse_string(FileAccess.get_file_as_string(STORAGE))
	if not parsed is Dictionary: return false
	if not parsed.get("items",null) is Array: return false
	for stack in parsed.items:
		if stack is Dictionary and catalog.has(str(stack.get("id",""))):
			items.append({"id":str(stack.id),"count":maxi(1,int(stack.get("count",1)))})
	equipped=parsed.get("equipped",{})
	if not equipped is Dictionary:equipped={}
	# Preserve saves from the prior 17-slot inventory.
	if equipped.has("Ranged") and not equipped.has("Secondary"):
		equipped["Secondary"]=equipped["Ranged"]
	for legacy in ["Ranged","Ears","Legs"]: equipped.erase(legacy)
	strength=clampi(int(parsed.get("strength",10)),1,30)
	gold=maxi(0,int(parsed.get("gold",10)))
	if int(parsed.get("starter_revision",1))<2:
		if count_of("leather_armor")==0: items.append({"id":"leather_armor","count":1})
		_save()
	return true
func _save() -> void:
	var f := FileAccess.open(STORAGE,FileAccess.WRITE)
	if f!=null: f.store_string(JSON.stringify({"starter_revision":2,"items":items,"equipped":equipped,"strength":strength,"gold":gold}))
func count_of(id: String) -> int:
	var total := 0
	for stack in items:
		if str(stack.id)==id: total+=int(stack.count)
	return total
func capacity() -> float: return float(strength)*15.0
func weight() -> float:
	var total:=0.0
	for stack in items:
		var item: Dictionary=catalog.get(str(stack.id),{})
		total+=float(item.get("weight",0))*int(stack.count)
	if count_of("waterskin")>0: total+=4.18
	return total
func detail(id:String) -> Dictionary:
	return catalog.get(id,{})
func category(id:String) -> String:
	if QUEST_IDS.has(id):return "Quest"
	var item: Dictionary=detail(id)
	var kind:=str(item.get("type",""))
	if kind=="Material":return "Materials"
	if kind in ["Weapon","Armor","Shield"]:return "Equipment"
	return "Items"
func stacks(tab:String) -> Array[Dictionary]:
	var result: Array[Dictionary]=[]
	for stack in items:
		if category(str(stack.id))==tab:result.append(stack)
	return result
func icon(id:String) -> Texture2D:
	if atlas==null: return null
	var item: Dictionary=detail(id)
	var slot:=int(item.get("icon",-1))
	if slot<0:return null
	var region:=AtlasTexture.new()
	region.atlas=atlas
	region.region=Rect2(float(slot%10)*128,float(floori(float(slot)/10.0))*128.0,128,128)
	return region
func compatible(id:String,slot:String) -> bool:
	var kind:=str(detail(id).get("type",""))
	if id=="":return true
	if count_of(id)<1:return false
	match slot:
		"Body":return kind=="Armor"
		"Main Hand":return kind=="Weapon"
		"Off Hand":return kind=="Shield" or kind=="Weapon"
		"Secondary":return kind=="Weapon"
		"Head","Eyes","Neck","Cloak","Wrists","Hands","Waist","Feet","Left Ring","Right Ring","Accessory":return false
	return false
func set_slot(slot:String,id:String) -> bool:
	if not compatible(id,slot):return false
	if id!="" and slot!="Body":
		for key in equipped.keys():
			if str(key)!=slot and str(equipped[key])==id: equipped.erase(key)
	if id=="":equipped.erase(slot)
	else:equipped[slot]=id
	_save()
	return true
func equipped_id(slot:String) -> String:return str(equipped.get(slot,""))
func select(id:String) -> void:selected_id=id

