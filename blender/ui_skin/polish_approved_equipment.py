from pathlib import Path
p=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\inventory_screen.gd")
s=p.read_text(encoding="utf-8")
def rep(a,b):
 global s
 assert s.count(a)==1,(a[:100],s.count(a))
 s=s.replace(a,b)
rep('''\tcaption.position = Vector2(4,-18)''','''\tcaption.position = Vector2(4,3)''')
rep('''\tvar caption := _label("EYES (GOGGLES)" if slot=="Eyes" else slot.to_upper(),12,HORIZONTAL_ALIGNMENT_CENTER)''','''\tvar caption := _label("GOGGLES" if slot=="Eyes" else slot.to_upper(),10,HORIZONTAL_ALIGNMENT_CENTER)''')
rep('''\tpreview_camera.size = 2.14
\tpreview_camera.position = Vector3(0,1.0,4)
\tpreview_camera.look_at(Vector3(0,0.93,0))''','''\tpreview_camera.size = 1.97
\tpreview_camera.position = Vector3(0,1.06,4)
\tpreview_camera.look_at(Vector3(0,1.06,0))''')
rep('''\tclose.position = Vector2(1465,151)
\tclose.size = Vector2(104,45)''','''\tclose.position = Vector2(1455,18)
\tclose.size = Vector2(132,45)''')
rep('''\t\tvar info := _label("",14 if col==0 else 15)''','''\t\tvar info := _label("",13 if col==0 else 14)''')
rep('''\t\tinfo.position = Vector2(x+(94 if col==0 else 22),64)
\t\tinfo.size = Vector2(128 if col==0 else 186,126)''','''\t\tinfo.position = Vector2(x+(122 if col==0 else 22),64)
\t\tinfo.size = Vector2(102 if col==0 else 186,126)''')
rep('''\t\t"Level: —\nRace: Not set\nClass: Fighter (test)\nBackground: Not set",''','''\t\t"Level —\nRace —\nFighter (test)\nBG —",''')
# The selected item details must render below the source panel upper bronze rail.
rep('''\tdetails.position = Vector2(50,19)
\tdetails.size = Vector2(854,181)''','''\tdetails.position = Vector2(50,33)
\tdetails.size = Vector2(854,169)''')
rep('''\ttop.custom_minimum_size.y = 130''','''\ttop.custom_minimum_size.y = 107''')
rep('''\tpreview_frame.custom_minimum_size = Vector2(183,130)''','''\tpreview_frame.custom_minimum_size = Vector2(204,107)''')
rep('''\tpicture.custom_minimum_size = Vector2(163,120)''','''\tpicture.custom_minimum_size = Vector2(176,103)''')
rep('''\tinfo.custom_minimum_size.x = 340''','''\tinfo.custom_minimum_size.x = 310''')
rep('''\tsummary.custom_minimum_size = Vector2(325,62)''','''\tsummary.custom_minimum_size = Vector2(306,58)''')
rep('''\tproperties.custom_minimum_size.x = 230''','''\tproperties.custom_minimum_size.x = 216''')
# Use real referenced top banner without replacing screen behavior.
rep('''\thud_art.add_theme_stylebox_override("panel",_frame_type("tab"))
\thud_art.mouse_filter = Control.MOUSE_FILTER_IGNORE
\tcanvas.add_child(hud_art)''','''\thud_art.add_theme_stylebox_override("panel",_approved_empty_style())
\thud_art.mouse_filter = Control.MOUSE_FILTER_IGNORE
\tcanvas.add_child(hud_art)
\t_approved_art(hud_art,"top_banner",Rect2(Vector2.ZERO,hud_art.size))''')
p.write_text(s,encoding="utf-8",newline="\n")
print("APPROVED_EQUIPMENT_TEXT_AND_CHARACTER_ALIGNMENT_OK")
