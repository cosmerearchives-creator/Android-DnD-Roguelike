extends SceneTree
func _initialize(): call_deferred("run")
func run():
	root.size=Vector2i(1280,800)
	var w=load("res://world.tscn").instantiate()
	root.add_child(w)
	w.set_physics_process(false)
	for i in 80: await process_frame
	var terrain=w.get_node("TerrainLayer")
	w.camera.projection=Camera3D.PROJECTION_PERSPECTIVE
	w.camera.position=Vector3(-5,3.7,8)
	w.camera.look_at(Vector3(-5,0,4),Vector3.UP)
	for width in [0,30]:
		terrain.set_blend_inches(width)
		for i in 4: await process_frame
		await RenderingServer.frame_post_draw
		var fname="res://../Terrain "+OS.get_environment("FOH_CAPTURE")+" "+str(width)+".png"
		print("TERRAIN_CAPTURE ",root.get_texture().get_image().save_png(ProjectSettings.globalize_path(fname)))
	quit()
