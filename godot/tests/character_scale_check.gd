extends SceneTree
const AP=preload("res://character_appearance.gd")
func _initialize():call_deferred("run")
func run():
 var data=AP.settings()
 data.height_scale=0.8
 data.frame_scale=0.9
 data.weight_scale=1.2
 AP.save(data)
 var loaded=AP.settings()
 assert(is_equal_approx(loaded.height_scale,0.8))
 assert(AP.body_scale(loaded).is_equal_approx(Vector3(0.72,0.8,0.96)))
 var menu=load("res://front_menu.tscn").instantiate()
 root.add_child(menu)
 await process_frame
 menu.creator()
 menu.chosen=0
 menu._refresh_preview()
 assert(is_equal_approx(menu.preview_model.scale.x/menu.preview_model.scale.y,0.9))
 assert(is_equal_approx(menu.preview_model.scale.z/menu.preview_model.scale.y,1.2))
 print("SCALE_PERSISTENCE_AND_PREVIEW_OK")
 quit()
