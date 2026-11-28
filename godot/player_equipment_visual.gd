extends RefCounted
const MODELS = ["female_leather_trial"]
const NAMES = ["Female Human"]
static func asset(model: String) -> String:
	match model:
		"female_goblin": return "res://assets/femalegobsimpleskel.glb"
		"human_adventurer", "female_leather_trial": return "res://assets/female_no_race_base.glb"
		"male_human": return "res://assets/male_human_leather_armored.glb"
	return "res://assets/%s.glb" % model
static func apply(model: Node, equipped: bool) -> void:
	var garment := model.find_child("WearableLeather",true,false) as MeshInstance3D
	if garment == null:
		garment = model.find_child("Leather Armor - Male Fitted",true,false) as MeshInstance3D
	if garment != null: garment.visible = equipped
	var feet := model.find_child("BootCoverage",true,false) as MeshInstance3D
	if feet != null: feet.visible = not equipped
