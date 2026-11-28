from pathlib import Path
p=Path(r"G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\creator_workshop.gd")
s=p.read_text(encoding="utf-8-sig")
s=s.replace('tab_tex=_load_image("res://creator_ui/wide_button.png")','tab_tex=_load_image("res://creator_ui/creator_button_clean.png")')
s=s.replace('panel_tex=_load_image("res://creator_ui/blank_tall_panel.png")','panel_tex=_load_image("res://creator_ui/creator_panel_clean.png")')
s=s.replace('button_tex=_load_image("res://creator_ui/long_button.png")','button_tex=_load_image("res://creator_ui/creator_button_clean.png")')
s=s.replace('stone_tex=_load_image("res://creator_ui/preview_stone.png")','stone_tex=_load_image("res://creator_ui/creator_stone_pedestal.png")')
a=s.index('func _tex(path:Texture2D,')
b=s.index('func _style(',a)
s=s[:a]+'''func _tex(path:Texture2D,position:Vector2,extent:Vector2,under:Control=_ui_root) -> TextureRect:
	var sprite:=TextureRect.new()
	sprite.texture=path
	sprite.position=position
	sprite.size=extent
	sprite.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	sprite.stretch_mode=TextureRect.STRETCH_SCALE
	sprite.mouse_filter=Control.MOUSE_FILTER_IGNORE
	under.add_child(sprite)
	return sprite

func _nine_slice(path:Texture2D,position:Vector2,extent:Vector2,margin:float=22.0,under:Control=_ui_root) -> NinePatchRect:
	var panel:=NinePatchRect.new()
	panel.texture=path
	panel.position=position
	panel.size=extent
	panel.patch_margin_left=int(margin)
	panel.patch_margin_right=int(margin)
	panel.patch_margin_top=int(margin)
	panel.patch_margin_bottom=int(margin)
	panel.mouse_filter=Control.MOUSE_FILTER_IGNORE
	under.add_child(panel)
	return panel

'''+s[b:]
# Don't stamp static art underneath controls; styleboxes take care of 9-slice.
a=s.index('func _style(active:bool=false) -> StyleBoxFlat:')
b=s.index('func _button(',a)
s=s[:a]+'''func _style(active:bool=false) -> StyleBoxTexture:
	var s:=StyleBoxTexture.new()
	s.texture=button_tex
	s.texture_margin_left=54.0
	s.texture_margin_right=54.0
	s.texture_margin_top=33.0
	s.texture_margin_bottom=33.0
	s.content_margin_left=9.0
	s.content_margin_right=9.0
	s.content_margin_top=5.0
	s.content_margin_bottom=5.0
	s.modulate_color=Color(1.0,0.74,0.48) if active else Color(1,1,1)
	return s

'''+s[b:]
s=s.replace('''	_tex(panel_tex,Vector2(28,106),Vector2(382,689))
	for pair in [[106.0,154.0],[274.0,148.0],[436.0,154.0],[610.0,154.0]]:
		_tex(panel_tex,Vector2(926,float(pair[0])),Vector2(704,float(pair[1])))
	_tex(stone_tex,Vector2(430,711),Vector2(480,157))
	_tex(tab_tex,Vector2(18,8),Vector2(377,82))''','''	_nine_slice(panel_tex,Vector2(28,106),Vector2(382,689),42)
	for pair in [[106.0,154.0],[274.0,148.0],[436.0,154.0],[610.0,154.0]]:
		_nine_slice(panel_tex,Vector2(926,float(pair[0])),Vector2(704,float(pair[1])),35)
	_tex(stone_tex,Vector2(482,741),Vector2(375,96))
	_nine_slice(button_tex,Vector2(18,8),Vector2(377,82),34)''')
# Fix button font and overlay scaling; convert long tabs to source art without generic color fill.
s=s.replace('''	b.add_theme_stylebox_override("pressed",_style(true))
	_ui_root.add_child(b)''','''	b.add_theme_stylebox_override("pressed",_style(true))
	b.add_theme_stylebox_override("disabled",_style(false))
	_ui_root.add_child(b)''',1)
# Keep page buttons inside the 1673px canvas.
s=s.replace('''var x:=424.0+i*172.0''','''var x:=424.0+i*172.0''')
# Reserve space for body slider details in each frame without overlapping.
s=s.replace('''	_warning_label=_font_label("",Vector2(960,754),Vector2(630,52),17)''','''	_warning_label=_font_label("",Vector2(963,775),Vector2(625,37),16)''')
# Avoid old stray arrow glyphs and portrait center issues.
s=s.replace('''	_preview.position=Vector2(425,88)''','''	_preview.position=Vector2(435,97)''')
s=s.replace('''	_preview.size=Vector2(474,683)''','''	_preview.size=Vector2(458,645)''')
s=s.replace('''	viewport.size=Vector2i(474,683)''','''	viewport.size=Vector2i(458,645)''')
s=s.replace('''	camera.size=2.35''','''	camera.size=2.20''')
# Make the height/frame/weight controls feel like real gold objects rather than default Godot sliders.
s=s.replace('''	slider.value=value
	controls.add_child(slider)''','''	slider.value=value
	var track:=StyleBoxTexture.new()
	track.texture=_load_image("res://creator_ui/creator_slider_track.png")
	track.texture_margin_left=20
	track.texture_margin_right=20
	track.texture_margin_top=5
	track.texture_margin_bottom=5
	slider.add_theme_stylebox_override("slider",track)
	slider.add_theme_stylebox_override("grabber_area",track)
	slider.add_theme_stylebox_override("grabber_area_highlight",track)
	var grip:=_load_image("res://creator_ui/creator_slider_diamond.png")
	slider.add_theme_icon_override("grabber",grip)
	slider.add_theme_icon_override("grabber_highlight",grip)
	controls.add_child(slider)''',1)
# Layout gold controls with value boxes aligned after arrow buttons.
s=s.replace('''	reading.position=Vector2(510,at_y+58)''','''	reading.position=Vector2(511,at_y+54)''')
# Existing panel art should not overlay the bottom buttons: controls are fully enclosed in their frames.
p.write_text(s,encoding="utf-8")
print("CREATOR_NINESLICE_APPLIED")
