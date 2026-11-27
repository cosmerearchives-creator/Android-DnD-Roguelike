from pathlib import Path
p=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\inventory_screen.gd")
s=p.read_text(encoding="utf-8")
def rep(a,b):
 global s
 assert s.count(a)==1,(a[:100],s.count(a))
 s=s.replace(a,b)
rep('''\tinventory_backdrop.color = Color(0.055,0.036,0.025,0.86)''','''\t# The supplied leather art is fully opaque; keep the paused world visible
\t# around the frame as in the approved target, not through its content.
\tinventory_backdrop.color = Color(0.055,0.036,0.025,0.54)''')
rep('''\tinfo.add_theme_color_override("font_color",Color("#e5cea3"))''','''\tinfo.add_theme_color_override("font_color",Color("#e5cea3"))''')
rep('''\t\tvar info := _label("",13 if col==0 else 14)''','''\t\tvar info := _label("",15 if col==0 else 15)''')
rep('''\tpreview_frame.custom_minimum_size = Vector2(204,107)''','''\tpreview_frame.custom_minimum_size = Vector2(235,107)''')
rep('''\tpicture.custom_minimum_size = Vector2(176,103)''','''\tpicture.custom_minimum_size = Vector2(190,103)''')
rep('''\tinfo.custom_minimum_size.x = 310''','''\tinfo.custom_minimum_size.x = 275''')
rep('''\tsummary.custom_minimum_size = Vector2(306,58)''','''\tsummary.custom_minimum_size = Vector2(268,58)''')
rep('''\tproperties.custom_minimum_size.x = 216''','''\tproperties.custom_minimum_size.x = 214''')
rep('''\tapproved_ui_header.visible = reference_active
\tvar title_panel''','''\tapproved_ui_header.visible = reference_active
\tvar status_panel := find_child("DecorativeStatusHeader",true,false) as Panel
\tif status_panel!=null:
\t\tstatus_panel.add_theme_stylebox_override("panel",_approved_empty_style() if reference_active else _frame_type("tab"))
\tif approved_art_panels.has("top_banner"):
\t\t(approved_art_panels["top_banner"] as TextureRect).visible = reference_active
\tvar title_panel''')
p.write_text(s,encoding="utf-8",newline="\n")
print("APPROVED_EQUIPMENT_FINAL_LEGIBILITY_PASS_OK")
