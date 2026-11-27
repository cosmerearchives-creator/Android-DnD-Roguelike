extends SceneTree
func _initialize() -> void: call_deferred("_run")
func _run() -> void:
	var visual:=Node3D.new()
	var mesh:=MeshInstance3D.new()
	mesh.mesh=CapsuleMesh.new()
	visual.add_child(mesh)
	mesh.owner=visual
	var packed:=PackedScene.new()
	var result:=packed.pack(visual)
	visual.free()
	if result!=OK:
		printerr("Could not create test asset")
		quit(2);return
	var hero:=CharacterBody3D.new()
	root.add_child(hero)
	var spawner=load("res://enemy_spawner.gd").new()
	root.add_child(spawner)
	spawner.set_physics_process(false)
	var list: Array=load("res://goblin_variants.gd").ENTRIES
	for entry in list:
		spawner.scenes[str(entry.asset)]=packed
	spawner.scenes[spawner.BEETLE]=packed
	spawner.scenes[spawner.TWIG_BLIGHT]=packed
	spawner.setup(hero)
	await process_frame
	await process_frame
	if spawner.mobs.size()!=0 or spawner.initial_remaining!=spawner.STARTING_POPULATION:
		printerr("Spawn queue should be scheduled, not created in one frame.")
		quit(3);return
	var previous := 0
	var spawn_points: Array[Vector3] = []
	for step in range(spawner.STARTING_POPULATION):
		spawner._advance_initial_spawn(spawner.INITIAL_SPAWN_INTERVAL + 0.01)
		if spawner.mobs.size()-previous > 1:
			printerr("Startup creates multiple creatures in one frame")
			quit(3);return
		if spawner.mobs.size() > previous:
			spawn_points.append(spawner.mobs.back().body.global_position)
		previous=spawner.mobs.size()
	if spawner.initial_remaining!=0 or spawner.mobs.size()<5:
		printerr("Startup queue did not finish",spawner.initial_remaining,spawner.mobs.size())
		quit(3);return
	# Validate exact creation points while AI movement is paused by the test.
	for i in range(spawn_points.size()):
		var position:Vector3=spawn_points[i]
		var distance:float=position.distance_to(hero.global_position)
		if distance<spawner.SPAWN_MIN-.1 or distance>spawner.SPAWN_MAX+.1:
			printerr("Enemy spawned in unsafe distance ",distance)
			quit(4);return
		for j in range(i):
			var other:Vector3=spawn_points[j]
			if other.distance_to(position)<7.4:
				printerr("Enemies are clustered at spawn ",other,position)
				quit(5);return
	print("ENEMY_SPAWN_SMOKE_OK staggered count=",spawner.mobs.size(),"range=",spawner.SPAWN_MIN,"-",spawner.SPAWN_MAX)
	quit(0)
