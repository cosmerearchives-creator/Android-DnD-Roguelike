extends RefCounted
const MODELS = ["human_adventurer","male_human","female_leather_trial","female_goblin"]
const NAMES = ["Human Adventurer","Male Human","Female Human (Walk / Run)","Female Goblin"]
static func asset(model: String) -> String:
	match model:
		"human_adventurer": return "res://assets/human_adventurer_modular.glb"
		"male_human": return "res://assets/male_human_leather_armored.glb"
	return "res://assets/%s.glb" % model
static func apply(model: Node, equipped: bool) -> void:
	var garment := model.find_child("WearableLeather",true,false) as MeshInstance3D
	if garment == null:
		garment = model.find_child("Leather Armor - Male Fitted",true,false) as MeshInstance3D
	if garment != null: garment.visible = equipped
	var feet := model.find_child("BootCoverage",true,false) as MeshInstance3D
	if feet != null: feet.visible = not equipped
