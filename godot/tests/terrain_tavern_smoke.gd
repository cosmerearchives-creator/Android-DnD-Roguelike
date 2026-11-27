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
    var batches := 0
    for kind in TERRAIN.KINDS:
        var count := int(terrain.terrain_counts.get(kind, 0))
        if count <= 0:
            printerr("TERRAIN_ASSET_MISSING_INSTANCES ",kind)
            quit(1)
            return
        total += count
        var mesh_name: String = kind.capitalize() + "Ground"
        var batch := terrain.get_node_or_null(mesh_name) as MultiMeshInstance3D
        if batch == null or batch.multimesh == null or batch.multimesh.instance_count != count:
            printerr("TERRAIN_MISSING_DRAW_BATCH ",kind)
            quit(2)
            return
        var mesh: Mesh = batch.multimesh.mesh
        if mesh.get_surface_count() != 1 or mesh.surface_get_material(0) == null:
            printerr("TERRAIN_MATERIAL_MISSING ",kind)
            quit(3)
            return
        var transform: Transform3D = batch.multimesh.get_instance_transform(0)
        var transformed_size := transform.basis * mesh.get_aabb().size
        # AABB transform corners can flip with rotation: lengths matter.
        if absf(transformed_size.x) > 1.5251 or absf(transformed_size.z) > 1.5251:
            printerr("TERRAIN_SPAN_BAD ",kind,transformed_size)
            quit(4)
            return
        batches += 1
        print("TERRAIN_BATCH_OK ",kind," count=",count)
    if total < 100 or batches != 6:
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
    print("TERRAIN_TAVERN_SMOKE_OK batches=",batches," total=",total)
    quit(0)
