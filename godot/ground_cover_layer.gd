extends Node3D
## Procedurally distributes intact, source-textured miniature woodland assets.
## One MultiMesh draw batch per active variant; no per-object collision/scripts.
const TERRAIN := preload("res://terrain_layer.gd")
const GROUPS := ["plants", "debris", "elements", "woodland_bonus"]
const MAX_BONUS_VARIANTS := 4
const BASE_SEED := 19326
const CATEGORY_SCALE := {
	"tall_grass": 3.0, "small_fern": 2.1, "woodland_plant": 2.0,
	"woodland_extra": 1.8, "loose_stone": 1.0,
	"fallen_leaves": .95, "fallen_twig": 1.0,
	"fallen_branch": 1.05, "mushroom": .9,
	"moss_cluster": 1.45, "exposed_root": 1.2, "pinecone": .65
}
const BIOME_PALETTE := {
	"grass": ["tall_grass", "tall_grass", "woodland_plant", "woodland_plant", "small_fern", "woodland_extra", "fallen_leaves", "fallen_twig", "mushroom"],
	"dirt": ["fallen_twig", "fallen_branch", "loose_stone", "exposed_root", "fallen_leaves", "woodland_extra"],
	"leaves": ["fallen_leaves", "fallen_leaves", "fallen_twig", "fallen_branch", "mushroom", "mushroom", "pinecone", "woodland_plant"],
	"moss": ["moss_cluster", "moss_cluster", "small_fern", "woodland_plant", "mushroom", "fallen_branch"],
	"mud": ["exposed_root", "fallen_twig", "loose_stone", "moss_cluster"],
	"rocky": ["loose_stone", "loose_stone", "fallen_branch", "moss_cluster", "pinecone"]
}
var batches: Dictionary = {}
var instance_counts: Dictionary = {}
var usable_variant_count := 0
var _next_group := 0
var _terrain
var _placements: Dictionary = {}
var _finished := false

func _ready() -> void:
	name = "GroundCoverLayer"
	_terrain = get_parent().get_node_or_null("TerrainLayer")
	_generate_placements()
	set_process(true)

func _hash(x: int, z: int) -> int:
	return absi((x * 73856093 ^ z * 19349663) ^ ((x * 83492791 + z * 912783) >> 11))

func _category(id: String) -> String:
	for prefix in CATEGORY_SCALE:
		if id.begins_with(str(prefix) + "_"):
			return str(prefix)
	return ""

func _generate_placements() -> void:
	var rng := RandomNumberGenerator.new()
	for x in range(TERRAIN.MIN_X,TERRAIN.MAX_X+1):
		for z in range(TERRAIN.MIN_Z,TERRAIN.MAX_Z+1):
			if not _terrain._cell_enabled(x,z): continue
			var kind: String = _terrain._type_at(x,z)
			var palette: Array = BIOME_PALETTE[kind]
			rng.seed = BASE_SEED + _hash(x,z)
			# Clearer near spawn, and sparse across exposed mud/rocky soils.
			var density := 4 if kind in ["grass","leaves","moss"] else 3
			if kind in ["mud","rocky"]: density = 2
			if Vector2(float(x)*TERRAIN.CELL,float(z)*TERRAIN.CELL).length() < 2.4:
				density = mini(density,1)
			for i in range(density):
				var chosen: String = palette[rng.randi_range(0,palette.size()-1)]
				# No checkerboard-edge clutter: favor but do not require tile joins.
				var offset := Vector2(rng.randf_range(-.49,.49),rng.randf_range(-.49,.49))
				if rng.randf() < .38:
					if rng.randf()<.5:
						offset.x = (1.0 if rng.randf()<.5 else -1.0)*rng.randf_range(.36,.49)
					else:
						offset.y = (1.0 if rng.randf()<.5 else -1.0)*rng.randf_range(.36,.49)
				var pos := Vector3((float(x)+offset.x)*TERRAIN.CELL,.025,(float(z)+offset.y)*TERRAIN.CELL)
				var scale := rng.randf_range(.82,1.18)
				var yaw := rng.randf_range(0.0,TAU)
				if not _placements.has(chosen): _placements[chosen] = []
				(_placements[chosen] as Array).append({"position":pos,"scale":scale,"yaw":yaw,"rng":rng.randi()})

func _process(_delta: float) -> void:
	if _next_group >= GROUPS.size():
		_finished = true
		set_process(false)
		return
	var group: String = GROUPS[_next_group]
	_next_group += 1
	var packed := load("res://assets/ground_cover/%s.glb" % group) as PackedScene
	if packed == null:
		push_error("Ground-cover source cannot be loaded: "+group)
		return
	var temporary := packed.instantiate() as Node3D
	add_child(temporary)
	var meshes := temporary.find_children("*","MeshInstance3D",true,false)
	for child in meshes:
		var inst := child as MeshInstance3D
		var name_id: String = inst.name
		var category := _category(name_id)
		if category.is_empty():continue
		if category == "woodland_extra" and int(name_id.get_slice("_",2))>MAX_BONUS_VARIANTS:
			continue
		if inst.mesh == null or inst.mesh.get_surface_count() != 1:
			push_error("Ground-cover variant mesh invalid: "+name_id)
			continue
		usable_variant_count += 1
		var transforms: Array = _placements.get(category,[])
		# Distribution is category based: spread evenly over all real variants.
		var variant_number := int(name_id.get_slice("_",name_id.get_slice_count("_")-1))-1
		var siblings := 0
		for candidate in meshes:
			if _category((candidate as MeshInstance3D).name)==category:
				siblings += 1
		if category=="woodland_extra":siblings=MAX_BONUS_VARIANTS
		var chosen: Array = []
		for j in transforms.size():
			if j % siblings == variant_number: chosen.append(transforms[j])
		if chosen.is_empty(): continue
		var multi := MultiMesh.new()
		multi.transform_format = MultiMesh.TRANSFORM_3D
		multi.mesh = inst.mesh
		multi.instance_count = chosen.size()
		for i in chosen.size():
			var placement: Dictionary = chosen[i]
			var base_scale: float = CATEGORY_SCALE[category] * float(placement.scale)
			var transform := Transform3D(Basis(Vector3.UP,float(placement.yaw)).scaled(Vector3.ONE*base_scale),placement.position)
			multi.set_instance_transform(i,transform)
		var batch := MultiMeshInstance3D.new()
		batch.name = name_id.capitalize().replace("_","") + "Batch"
		batch.multimesh = multi
		add_child(batch)
		batches[name_id] = batch
		instance_counts[name_id] = chosen.size()
	temporary.queue_free()

func is_finished() -> bool:
	return _finished
