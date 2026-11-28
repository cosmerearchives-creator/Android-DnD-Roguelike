extends SceneTree
func _initialize() -> void:
    call_deferred("_test")
func _test() -> void:
    root.size=Vector2i(1600,900)
    var scene=(load("res://front_menu.tscn") as PackedScene).instantiate()
    root.add_child(scene)
    await process_frame
    await process_frame
    var center=scene.get_child(1) as Control
    var scroll=center.get_child(0) as ScrollContainer
    var menu=scene.content as VBoxContainer
    print("MENU_LAYOUT ", "viewport=",root.size," root=",scene.size," bg=",scene.get_child(0).get_global_rect()," center=",center.get_global_rect()," scroll=",scroll.get_global_rect()," content=",menu.get_global_rect())
    for child in menu.get_children():
        if child is Button:
            print("BUTTON ",child.text," rect=",child.get_global_rect())
    var visible=0
    var canvas_view=Rect2(Vector2.ZERO,root.size)
    for child in menu.get_children():
        if child is Button and scroll.get_global_rect().intersects(child.get_global_rect()) and canvas_view.encloses(child.get_global_rect()):
            visible+=1
    if visible<3:
        printerr("MENU_OFFSCREEN")
        quit(1)
    else:
        print("MENU_LAYOUT_PASSED ",visible)
        quit(0)
