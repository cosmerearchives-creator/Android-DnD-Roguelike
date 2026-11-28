extends SceneTree
const TERRAIN = preload("res://terrain_layer.gd")
const COVER = preload("res://ground_cover_layer.gd")

func _initialize() -> void:
	call_deferred("_run")

func _fail(message: String) -> void:
	printerr("GROUND_COVER_FAILED: "+message)
	quit(1)

func _run() -> void:
	var root_node := Node3D.new()
	root.add_child(root_node)
	var terrain := TERRAIN.new()
	root_node.add_child(terrain)
	var cover := COVER.new()
	root_node.add_child(cover)
	for i in 15: await process_frame
	if not cover.is_finished():
		_fail("Four source groups not finished")
		return
	var count: int = cover.batches.size()
	if count < 30 or cover.usable_variant_count != 40:
		_fail("Missing or excess variants: %d / %d" % [count,cover.usable_variant_count])
		return
	var total := 0
	for key in cover.batches:
		var instance := cover.batches[key] as MultiMeshInstance3D
		if instance == null or instance.multimesh == null:
			_fail("No instance mesh for "+str(key))
			return
		var multi := instance.multimesh
		if multi.mesh.get_surface_count()<1:
			_fail("No source surface: "+str(key))
			return
		var material := multi.mesh.surface_get_material(0) as StandardMaterial3D
		if material == null or material.albedo_texture == null or material.albedo_texture.get_size() != Vector2(2048,2048):
			_fail("Original full-resolution texture missing: "+str(key))
			return
		var bounds := multi.mesh.get_aabb()
		if bounds.size.x > .5 or bounds.size.z > .5 or bounds.position.y < -.02:
			_fail("Uncentered or ungrounded model: "+str(key)+" "+str(bounds))
			return
		total += multi.instance_count
	var map: Image = terrain.material.get_shader_parameter("biome_map").get_image()
	for x in range(TERRAIN.MIN_X,TERRAIN.MAX_X+1):
		for z in range(TERRAIN.MIN_Z,TERRAIN.MAX_Z+1):
			var id := TERRAIN.KINDS.find(terrain._type_at(x,z))+1 if terrain._cell_enabled(x,z) else 0
			if map.get_pixel(x-TERRAIN.MIN_X,z-TERRAIN.MIN_Z).r8!=id:
				_fail("Ground cover and terrain map disagree at "+str(Vector2i(x,z)))
				return
	if total < 700 or total > 2400:
		_fail("Unexpected instance count: %d" % total)
		return
	for group in COVER.GROUPS:
		var source := load("res://assets/ground_cover/%s.glb" % group) as PackedScene
		if source==null:
			_fail("Missing model collection "+group)
			return
		var model := source.instantiate() as Node3D
		var all_meshes := model.find_children("*","MeshInstance3D",true,false)
		if all_meshes.size()<11:
			_fail("Grouped collection missing source meshes "+group)
			return
		model.free()
	print("GROUND_COVER_SMOKE_OK collections=4 available=48 active=%d instances=%d" % [count,total])
	quit(0)
