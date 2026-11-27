from pathlib import Path
p=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\inventory_screen.gd")
s=p.read_text(encoding="utf-8")
def rep(a,b):
 global s
 assert s.count(a)==1,(a[:95],s.count(a))
 s=s.replace(a,b)
# Align click targets with the user-artwork's right 8-slot, left 7-slot columns.
rep('''_desktop_slot(character_panel,left_slots[row],Vector2(18,17+row*87),Vector2(113,78))''','''_desktop_slot(character_panel,left_slots[row],Vector2(35,34+row*85),Vector2(132,77))''')
rep('''_desktop_slot(character_panel,right_slots[row],Vector2(487,17+row*77),Vector2(113,69))''','''_desktop_slot(character_panel,right_slots[row],Vector2(459,30+row*76),Vector2(137,75))''')
rep('''\tcaption.position = Vector2(4,-4)''','''\tcaption.position = Vector2(4,-18)''')
rep('''\tbutton.add_child(caption)
\tvar icon := TextureRect.new()''','''\tbutton.add_child(caption)
\tvar filled_back := TextureRect.new()
\tfilled_back.name = "OccupiedBacking"
\tfilled_back.texture = preload("res://ui_skin/approved_equipment/occupied_slot.png")
\tfilled_back.size = extent
\tfilled_back.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
\tfilled_back.stretch_mode = TextureRect.STRETCH_SCALE
\tfilled_back.mouse_filter = Control.MOUSE_FILTER_IGNORE
\tfilled_back.visible = false
\tbutton.add_child(filled_back)
\tvar icon := TextureRect.new()''')
rep('''\t\tif icon!=null: icon.texture=data.icon(id) if id!="" else null
\t\tbutton.text=""''','''\t\tif icon!=null: icon.texture=data.icon(id) if id!="" else null
\t\tvar filled_back := button.get_node_or_null("OccupiedBacking") as TextureRect
\t\tif filled_back != null: filled_back.visible = id != ""
\t\tbutton.text=""''')
# Highlight can be shown independently of the baked art; no old box effect.
rep('''\t\tif sym!=null: sym.visible = not reference_active
\tdesktop_equipment.visible''','''\t\tif sym!=null: sym.visible = not reference_active
\t\tbutton.text = ("" if reference_active else "    "+("Quests" if tab=="Quest" else tab))
\tdesktop_equipment.visible''')
rep('''\tpreview_camera.look_at(Vector3(0,1.0,0))''','''\tpreview_camera.look_at(Vector3(0,0.93,0))''')
# Use the exact image's stat-column widths and leave the crest clear for live data.
rep('''\t\tinfo.position = Vector2(x+16,64)
\t\tinfo.size = Vector2(199,123)''','''\t\tinfo.position = Vector2(x+(94 if col==0 else 22),64)
\t\tinfo.size = Vector2(128 if col==0 else 186,126)''')
rep('''\t\tvar info := _label("",16)''','''\t\tvar info := _label("",14 if col==0 else 15)''')
# Source has 10x3 cells 73px wide, no need duplicated frames or too many placeholders.
rep('''\t\tcell.custom_minimum_size=Vector2(86,66) if desktop_layout else Vector2(98,88)''','''\t\tcell.custom_minimum_size=Vector2(88,66) if desktop_layout else Vector2(98,88)''')
rep('''\t\tblank.custom_minimum_size=Vector2(86,66) if desktop_layout else Vector2(98,88)''','''\t\tblank.custom_minimum_size=Vector2(88,66) if desktop_layout else Vector2(98,88)''')
rep('''\tscroll.position = Vector2(40,105)
\tscroll.size = Vector2(872,202)''','''\tscroll.position = Vector2(36,105)
\tscroll.size = Vector2(886,202)''')
rep('''\tcarry_heading.position = Vector2(28,14)''','''\tcarry_heading.position = Vector2(46,47)''')
rep('''\tsorting.position = Vector2(765,13)
\tsorting.size = Vector2(150,41)
\tequipment_inventory.add_child(sorting)''','''\tsorting.position = Vector2(548,39)
\tsorting.size = Vector2(225,43)
\tsorting.text = "Sort: Name"
\tsorting.add_theme_stylebox_override("normal",_approved_empty_style())
\tsorting.add_theme_stylebox_override("hover",_approved_highlight())
\tequipment_inventory.add_child(sorting)''')
rep('''\tdetails.position = Vector2(20,14)
\tdetails.size = Vector2(895,191)''','''\tdetails.position = Vector2(50,19)
\tdetails.size = Vector2(854,181)''')
rep('''\tpreview_frame.add_theme_stylebox_override("panel",_compact_frame(true))
\tpreview_frame.custom_minimum_size = Vector2(138,130)''','''\tpreview_frame.add_theme_stylebox_override("panel",_approved_empty_style())
\tpreview_frame.custom_minimum_size = Vector2(183,130)''')
rep('''\tpicture.custom_minimum_size = Vector2(111,105)''','''\tpicture.custom_minimum_size = Vector2(163,120)''')
rep('''\tinfo.custom_minimum_size.x = 435''','''\tinfo.custom_minimum_size.x = 340''')
rep('''\tsummary.custom_minimum_size = Vector2(425,68)''','''\tsummary.custom_minimum_size = Vector2(325,62)''')
rep('''\tproperties.custom_minimum_size.x = 260''','''\tproperties.custom_minimum_size.x = 230''')
# Source buttons already have embossed bronze and pictograms. Make clickable,
# accessible text overlay without drawing a second stack of invented borders.
rep('''\tactions.add_child(equip)
\tvar comparison := _button''','''\t_approved_action_style(equip,"Equip")
\tactions.add_child(equip)
\tvar comparison := _button''')
rep('''\tactions.add_child(comparison)
\tvar drop := _button''','''\t_approved_action_style(comparison,"Compare")
\tactions.add_child(comparison)
\tvar drop := _button''')
rep('''\tactions.add_child(drop)
\tvar inspect := _button''','''\t_approved_action_style(drop,"Drop")
\tactions.add_child(drop)
\tvar inspect := _button''')
rep('''\tactions.add_child(inspect)

func _compatible_selected_slot()''','''\t_approved_action_style(inspect,"Details")
\tactions.add_child(inspect)

func _approved_action_style(button: Button, caption: String) -> void:
\tbutton.tooltip_text = caption
\tbutton.add_theme_stylebox_override("normal",_approved_empty_style())
\tbutton.add_theme_stylebox_override("hover",_approved_highlight())
\tbutton.add_theme_stylebox_override("pressed",_approved_highlight())

func _compatible_selected_slot()''')
p.write_text(s,encoding="utf-8",newline="\n")
print("APPROVED_EQUIPMENT_LAYOUT_ALIGNMENT_OK")
