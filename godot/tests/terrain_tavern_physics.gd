extends SceneTree
const TAVERN = preload("res://tavern_site.gd")
func _initialize() -> void:
    call_deferred("_run")
func _run() -> void:
    var building := TAVERN.new()
    building.position = Vector3(21,0,0)
    building.rotation.y = -PI/2.0
    root.add_child(building)
    await physics_frame
    await physics_frame
    var west := building.get_node("EntranceWestWall") as CollisionShape3D
    var east := building.get_node("EntranceEastWall") as CollisionShape3D
    var left_limit: float = west.position.x + (west.shape as BoxShape3D).size.x/2
    var right_limit: float = east.position.x - (east.shape as BoxShape3D).size.x/2
    var gap := right_limit - left_limit
    if gap < 1.524 or (left_limit > .0 or right_limit < .0):
        printerr("TAVERN_PHYSICS_ENTRANCE_TOO_NARROW ",left_limit,right_limit)
        quit(1)
        return
    var capsule := CapsuleShape3D.new()
    capsule.radius = .30
    capsule.height = TAVERN.TALL_PLAYABLE_REFERENCE_M
    var test := PhysicsShapeQueryParameters3D.new()
    test.shape = capsule
    test.collision_mask = 1
    for x in [10.0,11.0,12.0,13.0,13.5,14.0,14.5,15.0,16.0,17.0]:
        test.transform = Transform3D(Basis.IDENTITY, Vector3(x, capsule.height/2, .25))
        var hits := root.get_world_3d().direct_space_state.intersect_shape(test, 8)
        if not hits.is_empty():
            printerr("TAVERN_TALL_CAPSULE_BLOCKED_AT ",x," hit_count=",hits.size())
            quit(2)
            return
    print("TAVERN_TALL_CHARACTER_DOORWAY_CLEAR gap=",gap," 8ft_height=",capsule.height," 10_approach_points_pass")
    quit(0)
