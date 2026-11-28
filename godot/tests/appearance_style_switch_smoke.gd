extends SceneTree
const AP=preload("res://character_appearance.gd")
func _initialize() -> void:call_deferred("_run")
func _click_style(ui:Control,idx:int) -> void:
    var name:String=AP.HAIR[idx]
    for child in ui.controls.get_children():
        if child is Button and child.text==name:
            child.pressed.emit()
            return
    assert(false,"Missing hairstyle button "+name)
func _verify(ui:Control,idx:int,expected_offset:Vector3,expected_size:float,mount:BoneAttachment3D) -> void:
    assert(int(ui.appearance["hair"])==idx)
    assert((ui.appearance["hair_offset"] as Vector3).distance_to(expected_offset)<0.0001)
    assert(is_equal_approx(float(ui.appearance["hair_scale"]),expected_size))
    var attached:=ui.model.find_child("ModularAppearance",true,false) as BoneAttachment3D
    assert(attached==mount,"Head mount must stay attached across style switches")
    var hair:=attached.get_node("Hair") as Node3D
    assert(hair!=null)
    assert(hair.global_position.y>1.0,"Hair is back at pedestal rather than head")
    var baseline:Vector3=AP.base_position("hair",idx,AP.mesh_factor("hair",idx,ui.appearance))
    assert(hair.position.distance_to(baseline+expected_offset)<0.0001)
    var label:Label=ui.controls.get_node("hairAxis0") as Label
    assert(label.text=="%+.2f m" % expected_offset.x,"Displayed X differs from style-specific fit")
    label=ui.controls.get_node("hairAxis1") as Label
    assert(label.text=="%+.2f m" % expected_offset.y)
    label=ui.controls.get_node("hairAxis2") as Label
    assert(label.text=="%+.2f m" % expected_offset.z)
    var slider:=ui.controls.get_node("HairSizeSlider") as HSlider
    assert(is_equal_approx(slider.value,expected_size))
    print("STYLE_SWITCH_OK ",AP.HAIR[idx]," offsets=",expected_offset," size=",expected_size," world_y=",hair.global_position.y)
func _run() -> void:
    root.size=Vector2i(1673,940)
    var saved:Dictionary=AP.settings()
    var ui:Control=(load("res://creator_workshop.tscn") as PackedScene).instantiate()
    root.add_child(ui)
    await process_frame
    await process_frame
    ui._show_category("Hair")
    # Isolate the regression from the user\u0027s pre-existing saved style profiles.
    ui.appearance["hair"]=1
    ui.appearance["hair_offset"]=Vector3.ZERO
    ui.appearance["hair_scale"]=1.0
    ui.appearance["hair_offsets"]=[Vector3.ZERO,Vector3.ZERO,Vector3.ZERO,Vector3.ZERO]
    ui.appearance["hair_scales"]=[1.0,1.0,1.0,1.0]
    ui.appearance["hair_offset"]=Vector3(-0.03,0.03,0.01)
    ui.appearance["hair_scale"]=1.90
    AP.remember_current_hair(ui.appearance)
    ui._update_appearance()
    ui._show_category("Hair")
    var mount:=ui.model.find_child("ModularAppearance",true,false) as BoneAttachment3D
    _verify(ui,1,Vector3(-0.03,0.03,0.01),1.9,mount)
    # This reproduces the user's exact Wavy -> Short -> Braid -> Wavy flow.
    for idx in [2,3]:
        _click_style(ui,idx)
        _verify(ui,idx,Vector3.ZERO,1.0,mount) # immediate frame: no floor flash
        await process_frame
        _verify(ui,idx,Vector3.ZERO,1.0,mount)
    _click_style(ui,1)
    await process_frame
    _verify(ui,1,Vector3(-0.03,0.03,0.01),1.9,mount)
    # Short keeps its own manual offsets and independent size.
    _click_style(ui,2)
    await process_frame
    ui._nudge_attachment("hair",0,0.02)
    var hslider:=ui.controls.get_node("HairSizeSlider") as HSlider
    hslider.value=1.25
    await process_frame
    _verify(ui,2,Vector3(0.02,0,0),1.25,mount)
    _click_style(ui,1)
    await process_frame
    _verify(ui,1,Vector3(-0.03,0.03,0.01),1.9,mount)
    _click_style(ui,2)
    await process_frame
    _verify(ui,2,Vector3(0.02,0,0),1.25,mount)
    var disk:Dictionary=AP.settings()
    assert(disk["hair"]==2)
    assert(disk["hair_offset"]==Vector3(0.02,0,0))
    assert(is_equal_approx(disk["hair_scale"],1.25))
    AP.select_hair(disk,1)
    assert(disk["hair_offset"]==Vector3(-0.03,0.03,0.01))
    assert(is_equal_approx(float(disk["hair_scale"]),1.9))
    AP.save(saved)
    print("STYLE_SWITCH_PERSISTENCE_OK Wavy/Short/Braid independent profiles, no floor placement, same head mount")
    quit(0)
