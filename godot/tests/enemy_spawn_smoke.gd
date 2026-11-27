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
	spawner.setup(hero)
	await process_frame
	await process_frame
	if spawner.mobs.size()!=1:
		printerr("Initial spawn count expected 1, got ",spawner.mobs.size())
		quit(3);return
	var target_count:=5
	while spawner.mobs.size()<target_count:
		spawner._spawn_random()
		if spawner.mobs.size()==target_count:break
	for i in range(spawner.mobs.size()):
		var position:Vector3=spawner.mobs[i].body.global_position
		var distance:float=position.distance_to(hero.global_position)
		if distance<spawner.SPAWN_MIN-.1 or distance>spawner.SPAWN_MAX+.1:
			printerr("Enemy spawned in unsafe distance ",distance)
			quit(4);return
		for j in range(i):
			var other:Vector3=spawner.mobs[j].body.global_position
			if other.distance_to(position)<7.4:
				printerr("Enemies are clustered at spawn ",other,position)
				quit(5);return
	print("ENEMY_SPAWN_SMOKE_OK initial=1 count=",spawner.mobs.size(),"range=",spawner.SPAWN_MIN,"-",spawner.SPAWN_MAX)
	quit(0)
