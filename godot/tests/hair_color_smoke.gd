extends SceneTree
const AP=preload("res://character_appearance.gd")
func _initialize() -> void: call_deferred("_run")
func _mesh(node:Node) -> MeshInstance3D:
	if node is MeshInstance3D and (node as MeshInstance3D).mesh != null:return node as MeshInstance3D
	for child in node.get_children():
		var found:=_mesh(child)
		if found!=null:return found
	return null
func _run() -> void:
	root.size=Vector2i(1673,940)
	var saved:Dictionary=AP.settings()
	var ui=(load("res://creator_workshop.tscn") as PackedScene).instantiate()
	root.add_child(ui)
	await process_frame
	await process_frame
	ui._show_category("Hair")
	assert(ui.controls.get_node_or_null("HairColorPicker") is ColorPickerButton)
	var presets:=ui.controls.get_node_or_null("HairColorPresets") as OptionButton
	assert(presets!=null and presets.item_count==ui.HAIR_COLOR_PRESETS.size()+1)
	assert(presets.position.y>=ui.controls.get_node("HairSizeSlider").position.y)
	assert(presets.position.y+presets.size.y<=ui.controls.size.y)
	assert((ui.controls.get_node("HairColorPicker") as Control).position.y+48<=ui.controls.size.y)
	for entry in ui.HAIR_COLOR_PRESETS:
		assert(entry.has("name") and entry.has("color"))
	assert(ui.controls.get_node_or_null("HairSizeSlider") is HSlider)
	ui.appearance["hair"]=1
	ui._update_appearance()
	var attach:=ui.model.find_child("ModularAppearance",true,false) as BoneAttachment3D
	var hair:=attach.get_node("Hair")
	var mesh:=_mesh(hair)
	assert(mesh!=null)
	var initial_position:Vector3=hair.position
	var original_material:Material=mesh.mesh.surface_get_material(0)
	ui._set_hair_color(Color("#743321"))
	assert(AP.hair_color(ui.appearance).to_html(false)=="743321")
	var tinted:=mesh.get_surface_override_material(0) as BaseMaterial3D
	assert(tinted!=null)
	assert(tinted.albedo_color.to_html(false)=="743321")
	assert(mesh.mesh.surface_get_material(0)==original_material)
	assert(hair.position==initial_position)
	ui._select_hair_style(2)
	var changed_mesh:=_mesh(attach.get_node("Hair"))
	assert((changed_mesh.get_surface_override_material(0) as BaseMaterial3D).albedo_color.to_html(false)=="743321")
	var from_disk:Dictionary=AP.settings()
	assert(AP.hair_color(from_disk).to_html(false)=="743321")
	ui._set_hair_color(Color("#5e7cbb"))
	assert(AP.hair_color(AP.settings()).to_html(false)=="5e7cbb")
	assert((changed_mesh.get_surface_override_material(0) as BaseMaterial3D).albedo_color.to_html(false)=="5e7cbb")
	assert(attach.get_node_or_null("Beard")==null)
	AP.save(saved)
	print("HAIR_COLOR_SMOKE_OK swatches picker persistence material isolation hairstyle and fit")
	quit(0)
