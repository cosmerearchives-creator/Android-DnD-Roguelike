extends SceneTree
## Import, geometry, material, batching and tall-character doorway regression.
const TERRAIN = preload("res://terrain_layer.gd")
const TAVERN = preload("res://tavern_site.gd")
func _initialize() -> void:
    call_deferred("_run")
func _run() -> void:
    var terrain := TERRAIN.new()
    root.add_child(terrain)
    for frame in 8:
        await process_frame
    var total := 0
    var biomes := 0
    for kind in TERRAIN.KINDS:
        var count := int(terrain.terrain_counts.get(kind, 0))
        if count <= 0:
            printerr("TERRAIN_ASSET_MISSING_INSTANCES ",kind)
            quit(1)
            return
        total += count
        biomes += 1
    if terrain.material.get_shader_parameter("biome_map")==null:
        printerr("Missing terrain distribution texture")
        quit(2)
        return
    if total < 100 or biomes != 6:
        printerr("TERRAIN_COVERAGE_TOO_SPARSE ",total)
        quit(5)
        return
    var tavern := TAVERN.new()
    tavern.position = Vector3(21,0,0)
    tavern.rotation.y = -PI / 2
    root.add_child(tavern)
    var meshes := tavern.source_visual.find_children("*","MeshInstance3D",true,false)
    if meshes.size() != 1:
        printerr("TAVERN_MESH_INVALID ",meshes.size())
        quit(6)
        return
    if (meshes[0] as MeshInstance3D).mesh.get_aabb().size.y < .59:
        printerr("TAVERN_SOURCE_MESH_TOO_SMALL")
        quit(7)
        return
    if tavern.get_children().filter(func(c): return c is CollisionShape3D).size() != 6:
        printerr("TAVERN_COLLIDERS_WRONG")
        quit(8)
        return
    if TAVERN.DOOR_HEIGHT_M < 9.0 * TAVERN.FOOT or TAVERN.DOOR_WIDTH_M < 5.0 * TAVERN.FOOT:
        printerr("TAVERN_ENTRANCE_TOO_SMALL")
        quit(9)
        return
    print("TAVERN_CLEARANCE_OK nominal_height=",TAVERN.DOOR_HEIGHT_M," width=",TAVERN.DOOR_WIDTH_M," tall_race=",TAVERN.TALL_PLAYABLE_REFERENCE_M)
    var game: PackedScene = load("res://world.tscn")
    if game == null:
        printerr("WORLD_SCENE_PARSE_FAILED")
        quit(10)
        return
    print("TERRAIN_TAVERN_SMOKE_OK biomes=",biomes," total=",total)
    quit(0)
