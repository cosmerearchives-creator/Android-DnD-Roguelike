extends SceneTree
## Desktop-only source-fidelity review screenshot, never a substitute for device QA.
func _initialize() -> void:
	call_deferred("_run")
func _run() -> void:
	root.size = Vector2i(1600,900)
	var world := (load("res://world.tscn") as PackedScene).instantiate() as Node3D
	root.add_child(world)
	for i in 75:
		await process_frame
		if world.ground_cover_layer.is_finished() and world.scenery_finished and i>20:
			break
	world.set_physics_process(false)
	world.camera.position = Vector3(-4.0,2.25,8.2)
	world.camera.look_at(Vector3(-5.5,.03,0.0))
	for i in 5: await process_frame
	var output := "G:/ChatGPT Projects/Forests of Hell/Ground Cover Review/in_game_ground_cover.png"
	var image: Image = root.get_texture().get_image()
	var err := image.save_png(output)
	print("GROUND_COVER_VISUAL_CAPTURE ",output," saved=",err," batches=",world.ground_cover_layer.batches.size()," count=",world.ground_cover_layer.instance_counts.values().reduce(func(a,b): return a+b,0))
	quit(0 if err==OK else 1)
