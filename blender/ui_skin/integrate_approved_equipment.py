from pathlib import Path
p=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\inventory_screen.gd")
s=p.read_text(encoding="utf-8")
def rep(a,b):
 global s
 assert s.count(a)==1,(a[:130],s.count(a))
 s=s.replace(a,b)
rep("var inventory_backdrop: ColorRect","var inventory_backdrop: ColorRect\nvar approved_ui_header: TextureRect\nvar approved_art_panels: Dictionary = {}")
needle="func _construct_desktop() -> void:"
assert needle in s
helper='''# Equipment-only reference artwork. These full-resolution PNGs are extracted
# without repainting from the supplied 1280x1229 layout sheet.
func _approved_art(parent: Control, section: String, area: Rect2) -> TextureRect:
\tvar artwork := TextureRect.new()
\tartwork.name = "Approved" + section.capitalize().replace("_","")
\tartwork.texture = load("res://ui_skin/approved_equipment/%s.png" % section)
\tartwork.position = area.position
\tartwork.size = area.size
\tartwork.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
\tartwork.stretch_mode = TextureRect.STRETCH_SCALE
\tartwork.mouse_filter = Control.MOUSE_FILTER_IGNORE
\tparent.add_child(artwork)
\tapproved_art_panels[section] = artwork
\treturn artwork

func _approved_empty_style() -> StyleBoxEmpty:
\treturn StyleBoxEmpty.new()

func _approved_highlight() -> StyleBoxFlat:
\tvar s := StyleBoxFlat.new()
\ts.bg_color = Color(0,0,0,0)
\ts.border_color = Color(1.0,.62,.12,.98)
\ts.set_border_width_all(3)
\ts.set_corner_radius_all(6)
\treturn s

'''
s=s.replace(needle,helper+needle)
rep('''\tdesktop_canvas = canvas
\t# Decorative status strip''','''\tdesktop_canvas = canvas
\tapproved_ui_header = _approved_art(canvas,"tab_header",Rect2(16,87,622,129))
\t# Decorative status strip''')
rep('''\tmasthead.name = "InventoryTitlePanel"
\tvar heading''','''\tmasthead.name = "InventoryTitlePanel"
\tvar heading''')
rep('''\tcharacter_panel.name = "CharacterEquipmentPanel"
\t# Dedicated visual portrait stage''','''\tcharacter_panel.name = "CharacterEquipmentPanel"
\tcharacter_panel.add_theme_stylebox_override("panel",_approved_empty_style())
\t_approved_art(character_panel,"character_panel",Rect2(Vector2.ZERO,character_panel.size))
\t# Dedicated visual portrait stage''')
rep('''\tportrait_stage.add_child(portrait_art)
\tleft_view = SubViewportContainer.new()''','''\tportrait_stage.add_child(portrait_art)
\t# Original artwork already includes the stone plinth and charcoal stage.
\tportrait_stage.hide()
\tleft_view = SubViewportContainer.new()''')
rep('''\tstats_panel.name = "CharacterStatsPanel"
\tvar stat_names''','''\tstats_panel.name = "CharacterStatsPanel"
\tstats_panel.add_theme_stylebox_override("panel",_approved_empty_style())
\t_approved_art(stats_panel,"stat_panel",Rect2(Vector2.ZERO,stats_panel.size))
\tvar stat_names''')
rep('''\tequipment_inventory.name = "EquipmentInventoryGrid"
\tvar carry_heading''','''\tequipment_inventory.name = "EquipmentInventoryGrid"
\tequipment_inventory.add_theme_stylebox_override("panel",_approved_empty_style())
\t_approved_art(equipment_inventory,"inventory_panel",Rect2(Vector2.ZERO,equipment_inventory.size))
\tvar carry_heading''')
rep('''\titem_details.name = "EquippedItemDetails"
\tdetails = _column(item_details)''','''\titem_details.name = "EquippedItemDetails"
\titem_details.add_theme_stylebox_override("panel",_approved_empty_style())
\t_approved_art(item_details,"detail_panel",Rect2(Vector2.ZERO,item_details.size))
\tdetails = _column(item_details)''')
# The header's title/tab art is equipment only. Other tabs retain v0.3.195.
rep('''\tfor tab in TABS:
\t\tvar button: Button = tab_controls[tab]
\t\tbutton.add_theme_stylebox_override("normal",_frame_type("tab",tab==active_tab))
\tdesktop_equipment.visible = active_tab=="Equipment"''','''\tvar reference_active: bool = active_tab=="Equipment"
\tapproved_ui_header.visible = reference_active
\tvar title_panel := find_child("InventoryTitlePanel",true,false) as Panel
\tif title_panel!=null:
\t\ttitle_panel.add_theme_stylebox_override("panel",_approved_empty_style() if reference_active else _frame_type("panel"))
\tfor tab in TABS:
\t\tvar button: Button = tab_controls[tab]
\t\tbutton.add_theme_stylebox_override("normal",_approved_empty_style() if reference_active else _frame_type("tab",tab==active_tab))
\t\tbutton.add_theme_stylebox_override("hover",_approved_highlight() if reference_active else _frame_type("tab",true))
\t\tbutton.add_theme_stylebox_override("pressed",_approved_highlight() if reference_active else _frame_type("tab",true))
\t\tvar sym := button.get_child(0) as Label if button.get_child_count()>0 else null
\t\tif sym!=null: sym.visible = not reference_active
\tdesktop_equipment.visible = reference_active''')
# Disable the existing slot frames when the baked source slot artwork is visible.
rep('''\tbutton.add_theme_stylebox_override("normal",_compact_frame())
\tbutton.add_theme_stylebox_override("hover",_compact_frame(true))
\tbutton.add_theme_stylebox_override("pressed",_compact_frame(true))
\tparent.add_child(button)''','''\tbutton.add_theme_stylebox_override("normal",_approved_empty_style())
\tbutton.add_theme_stylebox_override("hover",_approved_highlight())
\tbutton.add_theme_stylebox_override("pressed",_approved_highlight())
\tparent.add_child(button)''')
rep('''\t\tbutton.text=""
\t\tbutton.add_theme_stylebox_override("normal",_compact_frame(slot==current_slot))
\t\tif icon!=null and icon.texture==null:
\t\t\tbutton.text=SIGNS[SLOTS.find(slot)]''','''\t\tbutton.text=""
\t\tbutton.add_theme_stylebox_override("normal",_approved_highlight() if slot==current_slot else _approved_empty_style())
\t\t# Equipment placeholders are already embedded in the approved sheet.
\t\t# For equipped items the actual icon is shown as a dynamic overlay.''')
# Match the source sprite sheet's 10 by 3 connected inventory cells.
rep('''\titem_grid.columns = 11
\titem_grid.size_flags_horizontal''','''\titem_grid.columns = 10
\titem_grid.size_flags_horizontal''')
rep('''\tscroll.position = Vector2(20,55)
\tscroll.size = Vector2(895,244)''','''\tscroll.position = Vector2(40,105)
\tscroll.size = Vector2(872,202)''')
rep('''\titem_grid.add_theme_constant_override("h_separation",0)''','''\titem_grid.add_theme_constant_override("h_separation",0)''')
rep('''\t\tcell.custom_minimum_size=Vector2(79,77) if desktop_layout else Vector2(98,88)
\t\tcell.add_theme_stylebox_override("normal",_compact_frame(selection==id))''','''\t\tcell.custom_minimum_size=Vector2(86,66) if desktop_layout else Vector2(98,88)
\t\tcell.add_theme_stylebox_override("normal",_approved_highlight() if desktop_layout and selection==id else _approved_empty_style() if desktop_layout else _compact_frame(selection==id))
\t\tif desktop_layout:
\t\t\tcell.add_theme_stylebox_override("hover",_approved_highlight())
\t\t\tcell.add_theme_stylebox_override("pressed",_approved_highlight())''')
rep('''\tfor i in range(maxi(0,(33 if desktop_layout else 24)-count)):
\t\tvar blank:=PanelContainer.new()
\t\tblank.add_theme_stylebox_override("panel",_compact_frame())
\t\tblank.custom_minimum_size=Vector2(79,77) if desktop_layout else Vector2(98,88)''','''\tfor i in range(maxi(0,(30 if desktop_layout else 24)-count)):
\t\tvar blank:=PanelContainer.new()
\t\tblank.add_theme_stylebox_override("panel",_approved_empty_style() if desktop_layout else _compact_frame())
\t\tblank.custom_minimum_size=Vector2(86,66) if desktop_layout else Vector2(98,88)''')
p.write_text(s,encoding="utf-8",newline="\n")
print("APPROVED_EQUIPMENT_SPRITE_INTEGRATION_OK")
