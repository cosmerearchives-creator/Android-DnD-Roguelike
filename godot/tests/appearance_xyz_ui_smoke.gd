extends SceneTree
const APPEARANCE=preload("res://character_appearance.gd")
func _initialize():call_deferred("_run")
func _run():
	root.size=Vector2i(1673,940)
	var screen:Control=(load("res://creator_workshop.tscn") as PackedScene).instantiate() as Control
	root.add_child(screen)
	await process_frame
	await process_frame
	assert(screen.body_panels.size()==4)
	screen._show_category("Body")
	assert(not screen.category_panel.visible)
	assert(screen.body_panels[0].visible)
	screen._show_category("Hair")
	assert(screen.category_panel.visible)
	assert(not screen.body_panels[0].visible)
	var slider:=screen.controls.get_node_or_null("HairSizeSlider") as HSlider
	assert(slider!=null)
	assert(screen.controls.get_node_or_null("hairAxis0")!=null)
	assert(screen.controls.get_node_or_null("hairAxis1")!=null)
	assert(screen.controls.get_node_or_null("hairAxis2")!=null)
	screen.appearance["hair"]=1
	screen.appearance["beard"]=1
	screen._update_appearance()
	var hair_mount:=screen.model.find_child("ModularAppearance",true,false) as BoneAttachment3D
	assert(hair_mount!=null)
	var hair:=hair_mount.get_node("Hair") as Node3D
	var before:Vector3=hair.position
	var original_cfg:Dictionary=APPEARANCE.settings()
	screen._nudge_attachment("hair",0,0.01)
	assert(hair.position.x>before.x+0.009)
	var from_disk:Dictionary=APPEARANCE.settings()
	assert(absf(from_disk["hair_offset"].x-screen.appearance["hair_offset"].x)<0.001)
	screen._reset_attachment("hair")
	assert(screen.appearance["hair_offset"]==Vector3.ZERO)
	screen._show_category("Beard")
	assert(screen.category_panel.visible)
	assert(screen.controls.get_node_or_null("beardAxis0")!=null)
	assert(screen.controls.get_node_or_null("beardAxis2")!=null)
	var beard:=hair_mount.get_node("Beard") as Node3D
	var old_z:float=beard.position.z
	screen._nudge_attachment("beard",2,0.02)
	assert(beard.position.z>old_z+0.019)
	var found:Dictionary=APPEARANCE.settings()
	assert(absf(found["beard_offset"].z-screen.appearance["beard_offset"].z)<0.001)
	screen._reset_attachment("beard")
	APPEARANCE.save(original_cfg)
	print("APPEARANCE_XYZ_UI_SMOKE_OK saved,reset,beard,hair,panels")
	quit(0)
