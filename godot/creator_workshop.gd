extends Control
## Landscape workshop presentation using the original supplied UI element atlas.
## All controls use the approved 66-bone female character without modifying the rig.
signal completed
signal cancelled
const APPEARANCE=preload("res://character_appearance.gd")
const WEARABLE=preload("res://player_equipment_visual.gd")
const FONT: Font=preload("res://hud_serif.ttf")
const GOLD:=Color("#ffbf55")
const TEXT:=Color("#ffe5ac")
const PAGES:=["Race","Subrace","Class","Background","Abilities","Appearance","Review"]
const CATEGORIES:=["Body","Skin Tone","Makeup","Hair","Beard","Ears","Eye Color","Body Markings"]
var tab_tex: Texture2D
var panel_tex: Texture2D
var button_tex: Texture2D
var compact_tex: Texture2D
var compact_active_tex: Texture2D
var arrow_tex: Texture2D
var stone_tex: Texture2D
var model: Node3D
var appearance: Dictionary={}
var gender_selection: String="Female"
var active_category: String="Body"
var camera: Camera3D
var model_root: Node3D
var model_base_scale:=1.0
var model_bottom:=0.0
var selected_height_inches:=68.0
var selected_weight_lb:=145.0
var title_label: Label
var controls: Control
var page_controls: Dictionary={}
var category_controls: Dictionary={}
var selected_widget: Control
var _warning_label: Label
var _ui_root: Control
var _preview: SubViewportContainer
var _hero_name: String=""
var _profile: ConfigFile=ConfigFile.new()
func _ready() -> void:
	tab_tex=_load_image("res://creator_ui/creator_button_clean.png")
	panel_tex=_load_image("res://creator_ui/creator_panel_clean.png")
	button_tex=_load_image("res://creator_ui/creator_button_clean.png")
	stone_tex=_load_image("res://creator_ui/creator_stone_oval.png")
	compact_tex=_load_image("res://creator_ui/creator_button_compact.png")
	compact_active_tex=_load_image("res://creator_ui/creator_button_compact_active.png")
	arrow_tex=_load_image("res://creator_ui/creator_arrow_compact.png")
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	_profile.load("user://hero.cfg")
	_hero_name=str(_profile.get_value("hero","name","Avatar"))
	appearance=APPEARANCE.settings()
	selected_height_inches=clampf(68.0*float(appearance.get("height_scale",1.0)),54.0,84.0)
	selected_weight_lb=clampf(145.0*float(appearance.get("weight_scale",1.0)),90.0,260.0)
	_ui_root=Control.new()
	add_child(_ui_root)
	_ui_root.anchor_left=0.0
	_ui_root.anchor_top=0.0
	_ui_root.anchor_right=0.0
	_ui_root.anchor_bottom=0.0
	resized.connect(_fit)
	_build()
	_fit()
	_show_category("Body")
	cancelled.connect(func():get_tree().change_scene_to_file("res://front_menu.tscn"))

func _load_image(path:String) -> Texture2D:
	var resource:Resource=load(path) if ResourceLoader.exists(path) else null
	if resource is Texture2D:return resource as Texture2D
	var img:=Image.load_from_file(path)
	return ImageTexture.create_from_image(img) if img!=null else null

func _fit() -> void:
	if not is_instance_valid(_ui_root):return
	var sx:float=size.x/1673.0
	var sy:float=size.y/940.0
	var fit:=minf(sx,sy)
	_ui_root.scale=Vector2.ONE*fit
	_ui_root.position=(size-Vector2(1673,940)*fit)/2.0
	_ui_root.size=Vector2(1673,940)

func _font_label(text:String,position:Vector2,extent:Vector2,px:int=25,centered:bool=false) -> Label:
	var label:=Label.new()
	label.text=text
	label.position=position
	label.size=extent
	label.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER if centered else HORIZONTAL_ALIGNMENT_LEFT
	label.vertical_alignment=VERTICAL_ALIGNMENT_CENTER
	label.add_theme_font_override("font",FONT)
	label.add_theme_font_size_override("font_size",px)
	label.add_theme_color_override("font_color",TEXT)
	label.mouse_filter=Control.MOUSE_FILTER_IGNORE
	_ui_root.add_child(label)
	return label

func _tex(path:Texture2D,position:Vector2,extent:Vector2,under:Control=_ui_root) -> TextureRect:
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

func _style(active:bool=false, family:String="compact") -> StyleBoxTexture:
	var s:=StyleBoxTexture.new()
	match family:
		"action":
			s.texture=button_tex
			s.texture_margin_left=38
			s.texture_margin_right=38
			s.texture_margin_top=29
			s.texture_margin_bottom=29
		"arrow":
			s.texture=arrow_tex
			s.texture_margin_left=15
			s.texture_margin_right=15
			s.texture_margin_top=15
			s.texture_margin_bottom=15
		_:
			s.texture=compact_active_tex if active else compact_tex
			s.texture_margin_left=22
			s.texture_margin_right=22
			s.texture_margin_top=20
			s.texture_margin_bottom=20
	s.content_margin_left=8
	s.content_margin_right=8
	s.content_margin_top=4
	s.content_margin_bottom=4
	s.modulate_color=Color.WHITE
	return s

func _button(text:String,position:Vector2,extent:Vector2,pressed:Callable,active:bool=false) -> Button:
	var b:=Button.new()
	b.position=position
	b.size=extent
	b.text=text
	b.add_theme_font_override("font",FONT)
	b.add_theme_font_size_override("font_size",25)
	b.add_theme_color_override("font_color",TEXT)
	b.add_theme_stylebox_override("normal",_style(active))
	b.add_theme_stylebox_override("hover",_style(true))
	b.add_theme_stylebox_override("pressed",_style(true))
	b.add_theme_stylebox_override("disabled",_style(false))
	_ui_root.add_child(b)
	b.pressed.connect(pressed)
	return b

func _build() -> void:
	# Preserve all source artwork in project; use the camp as a live preview backdrop,
	# while the dark, forged workshop colors come from the provided atlas.
	_tex(_load_image("res://menu_landscape.png"),Vector2.ZERO,Vector2(1673,940))
	var dim:=ColorRect.new()
	dim.color=Color(0.015,0.026,0.032,0.48)
	dim.position=Vector2.ZERO
	dim.size=Vector2(1673,940)
	dim.mouse_filter=Control.MOUSE_FILTER_IGNORE
	_ui_root.add_child(dim)
	_nine_slice(panel_tex,Vector2(28,106),Vector2(382,689),42)
	for pair in [[106.0,154.0],[274.0,148.0],[436.0,154.0],[610.0,154.0]]:
		_nine_slice(panel_tex,Vector2(926,float(pair[0])),Vector2(704,float(pair[1])),35)
	_tex(stone_tex,Vector2(485,699),Vector2(360,136))
	_nine_slice(button_tex,Vector2(18,8),Vector2(377,82),34)
	_font_label("FORESTS OF HELL",Vector2(52,8),Vector2(318,40),33,true)
	_font_label("Character Creation",Vector2(63,47),Vector2(300,37),27,true)
	_font_label("Appearance",Vector2(88,128),Vector2(258,56),31,true)
	for i in range(PAGES.size()):
		var page:String=PAGES[i]
		var x:=424.0+i*172.0
		var button:=_button(page,Vector2(x,29),Vector2(156,47),func():_select_page(page),page=="Appearance")
		button.add_theme_font_size_override("font_size",21)
		page_controls[page]=button
	for i in range(CATEGORIES.size()):
		var category:String=CATEGORIES[i]
		var b:=_button(category,Vector2(63,204+i*46),Vector2(315,46),func():_show_category(category),category=="Body")
		b.add_theme_font_size_override("font_size",25)
		category_controls[category]=b
	_preview=SubViewportContainer.new()
	_preview.name="CreatorModelPreview"
	_preview.position=Vector2(435,97)
	_preview.size=Vector2(458,645)
	_preview.stretch=true
	_preview.mouse_filter=Control.MOUSE_FILTER_STOP
	_ui_root.add_child(_preview)
	var viewport:=SubViewport.new()
	viewport.size=Vector2i(458,645)
	viewport.transparent_bg=true
	viewport.own_world_3d=true
	viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS
	_preview.add_child(viewport)
	model_root=Node3D.new()
	viewport.add_child(model_root)
	var light:=DirectionalLight3D.new()
	light.rotation_degrees=Vector3(-42,32,0)
	light.light_energy=2.0
	model_root.add_child(light)
	var fill:=DirectionalLight3D.new()
	fill.rotation_degrees=Vector3(-18,-142,0)
	fill.light_energy=.85
	model_root.add_child(fill)
	var env:=WorldEnvironment.new()
	env.environment=Environment.new()
	env.environment.background_mode=Environment.BG_COLOR
	env.environment.background_color=Color(0.0,0.0,0.0,0.0)
	env.environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
	env.environment.ambient_light_color=Color("#c9c4b7")
	env.environment.ambient_light_energy=.75
	model_root.add_child(env)
	camera=Camera3D.new()
	camera.projection=Camera3D.PROJECTION_ORTHOGONAL
	camera.size=2.20
	camera.position=Vector3(0,1.08,4.2)
	model_root.add_child(camera)
	camera.current=true
	camera.look_at(Vector3(0,1.08,0))
	_refresh_model()
	_preview.gui_input.connect(_handle_preview)
	for spec in [
		["◀",472.0,func():_rotate(-PI/8.0)],
		["−",578.0,func():_zoom(1.2)],
		["+",687.0,func():_zoom(1.0/1.2)],
		["▶",797.0,func():_rotate(PI/8.0)]
	]:
		var button:=_button(str(spec[0]),Vector2(float(spec[1]),841),Vector2(75,49),spec[2])
		button.add_theme_stylebox_override("normal",_style(false,"arrow"))
		button.add_theme_stylebox_override("hover",_style(true,"arrow"))
		button.add_theme_stylebox_override("pressed",_style(true,"arrow"))
	var back:=_button("Back",Vector2(1000,824),Vector2(233,64),func():cancelled.emit())
	var next:=_button("Next",Vector2(1261,824),Vector2(353,64),_finish)
	for action in [back,next]:
		action.add_theme_stylebox_override("normal",_style(false,"action"))
		action.add_theme_stylebox_override("hover",_style(true,"action"))
		action.add_theme_stylebox_override("pressed",_style(true,"action"))
	controls=Control.new()
	controls.position=Vector2(948,115)
	controls.size=Vector2(659,649)
	_ui_root.add_child(controls)
	_warning_label=_font_label("",Vector2(963,776),Vector2(625,35),16)
	_warning_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART

func _clear_controls() -> void:
	for node in controls.get_children():
		controls.remove_child(node)
		node.queue_free()

func _control_label(text:String,y:float,size:int=30) -> void:
	var l:=Label.new()
	l.text=text
	l.position=Vector2(45,y)
	l.size=Vector2(575,47)
	l.add_theme_font_override("font",FONT)
	l.add_theme_font_size_override("font_size",size)
	l.add_theme_color_override("font_color",TEXT)
	controls.add_child(l)

func _control_button(title:String,pos:Vector2,extent:Vector2,callback:Callable,active:bool=false) -> Button:
	var b:=Button.new()
	b.text=title
	b.position=pos
	b.size=extent
	b.add_theme_font_override("font",FONT)
	b.add_theme_font_size_override("font_size",27)
	b.add_theme_color_override("font_color",TEXT)
	b.add_theme_stylebox_override("normal",_style(active))
	b.add_theme_stylebox_override("hover",_style(true))
	b.pressed.connect(callback)
	controls.add_child(b)
	return b

func _slider(label:String,key:String,at_y:float,min_value:float,max_value:float,value:float,subtitle:String) -> void:
	_control_label(label,at_y)
	# Physical gold rails are drawn independently of Godot's HSlider theme:
	# the previous asset was hidden by the engine's slot background.
	for detail in [
		[Color(0.23,0.09,0.015,1),Vector2(107,at_y+61),Vector2(347,9)],
		[Color(0.82,0.40,0.07,1),Vector2(109,at_y+63),Vector2(343,5)],
		[Color(1.0,0.79,0.37,1),Vector2(110,at_y+64),Vector2(341,2)]
	]:
		var rail:=ColorRect.new()
		rail.color=detail[0]
		rail.position=detail[1]
		rail.size=detail[2]
		rail.mouse_filter=Control.MOUSE_FILTER_IGNORE
		controls.add_child(rail)
	var slider:=HSlider.new()
	slider.position=Vector2(107,at_y+51)
	slider.size=Vector2(347,30)
	slider.min_value=min_value
	slider.max_value=max_value
	slider.step=0.01
	slider.value=value
	var invisible:=StyleBoxEmpty.new()
	slider.add_theme_stylebox_override("slider",invisible)
	slider.add_theme_stylebox_override("grabber_area",invisible)
	slider.add_theme_stylebox_override("grabber_area_highlight",invisible)
	var grip:=_load_image("res://creator_ui/creator_slider_diamond_compact.png")
	slider.add_theme_icon_override("grabber",grip)
	slider.add_theme_icon_override("grabber_highlight",grip)
	controls.add_child(slider)
	var value_frame:=_nine_slice(compact_tex,Vector2(529,at_y+58),Vector2(105,45),14,controls)
	value_frame.name=key.capitalize()+"ValueFrame"
	var reading:=Label.new()
	reading.position=Vector2(529,at_y+58)
	reading.size=Vector2(105,45)
	reading.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
	reading.add_theme_font_override("font",FONT)
	reading.add_theme_font_size_override("font_size",25)
	reading.add_theme_color_override("font_color",TEXT)
	controls.add_child(reading)
	var lower:=Label.new()
	lower.text=subtitle
	lower.position=Vector2(92,at_y+97)
	lower.size=Vector2(445,33)
	lower.horizontal_alignment=HORIZONTAL_ALIGNMENT_CENTER
	lower.add_theme_font_override("font",FONT)
	lower.add_theme_font_size_override("font_size",21)
	lower.add_theme_color_override("font_color",TEXT)
	controls.add_child(lower)
	var update:Callable=func(v:float):
		match key:
			"height":
				selected_height_inches=v
				appearance["height_scale"]=clampf(v/68.0,0.7,1.3)
				reading.text="%d' %d\" " % [int(v)/12,int(v)%12]
			"frame":
				appearance["frame_scale"]=v
				reading.text="%.2f" % v
			"weight":
				selected_weight_lb=v
				appearance["weight_scale"]=clampf(v/145.0,0.5,1.3)
				reading.text="%d lb" % roundi(v)
		_update_model_scale()
	slider.value_changed.connect(update)
	update.call(value)
	for side in [-1,1]:
		var x:=30 if side<0 else 462
		var arrow:=_control_button("◀" if side<0 else "▶",Vector2(x,at_y+55),Vector2(59,53),func():slider.value=clampf(slider.value+side*(max_value-min_value)/20.0,min_value,max_value))
		arrow.add_theme_stylebox_override("normal",_style(false,"arrow"))
		arrow.add_theme_stylebox_override("hover",_style(true,"arrow"))

func _hair_size_slider() -> void:
	_control_label("Hair Size",390)
	var slider:=HSlider.new()
	slider.position=Vector2(105,453)
	slider.size=Vector2(410,32)
	slider.min_value=0.5
	slider.max_value=3.0
	slider.step=.05
	slider.value=float(appearance.get("hair_scale",1.0))
	controls.add_child(slider)
	slider.value_changed.connect(func(value:float):
		appearance["hair_scale"]=value
		_update_appearance())

func _show_category(category:String) -> void:
	active_category=category
	for name in category_controls.keys():
		category_controls[name].add_theme_stylebox_override("normal",_style(name==category))
	_clear_controls()
	_warning_label.text=""
	if category=="Body":
		_control_label("Gender",13,30)
		_control_button("♀   Female",Vector2(36,57),Vector2(270,57),func():gender_selection="Female";_show_category("Body"),gender_selection=="Female")
		_control_button("♂   Male",Vector2(345,57),Vector2(268,57),func():gender_selection="Male";_show_category("Body"),gender_selection=="Male")
		_slider("Height","height",169,54,84,selected_height_inches,"Range: 4' 6\" — 7' 0\"")
		_slider("Frame","frame",338,0.5,1.6,float(appearance.get("frame_scale",1.0)),"Slender                       Large")
		_slider("Weight","weight",510,90,260,selected_weight_lb,"Range: 90 – 260 lb")
		_warning_label.text="Body changes use the existing model; only the provided female rig is available."
	elif category=="Hair" or category=="Beard":
		var kind: String=category.to_lower()
		_control_label(category+" Style",20)
		var names: Array=APPEARANCE.HAIR if kind=="hair" else APPEARANCE.BEARDS
		for i in range(names.size()):
			var idx:int=i
			_control_button(str(names[i]),Vector2(30,87+i*70),Vector2(585,58),func():appearance[kind]=idx;_update_appearance();_show_category(category),int(appearance.get(kind,0))==i)
		if kind=="hair":
			_hair_size_slider()
		_warning_label.text="These selections use existing modular hair and beard assets."
	else:
		_control_label(category,28)
		_warning_label.text=category+" controls need compatible supplied textures or morph targets; no unsupported changes are applied."

func _select_page(page:String) -> void:
	if page=="Appearance":_show_category("Body")
	else:
		_warning_label.text=page+" will be restored in a later character-creation pass."
	for name in page_controls.keys():
		page_controls[name].add_theme_stylebox_override("normal",_style(name==page))

func _bounds(node:Node3D,parent:Transform3D) -> Vector2:
	var t:=parent*node.transform
	var low:=INF
	var high:=-INF
	if node is MeshInstance3D and node.mesh!=null:
		var aabb:AABB=node.get_aabb()
		for i in range(8):
			var y:float=(t*aabb.get_endpoint(i)).y
			low=minf(low,y)
			high=maxf(high,y)
	for child in node.get_children():
		if child is Node3D:
			var b:=_bounds(child,t)
			low=minf(low,b.x)
			high=maxf(high,b.y)
	return Vector2(low,high)

func _refresh_model() -> void:
	if is_instance_valid(model): model.queue_free()
	var path:=WEARABLE.asset(WEARABLE.MODELS[0])
	if not ResourceLoader.exists(path):return
	model=(load(path) as PackedScene).instantiate()
	model_root.add_child(model)
	WEARABLE.apply(model,false)
	var b:=_bounds(model,Transform3D.IDENTITY)
	if is_finite(b.x) and b.y>b.x:
		model_base_scale=1.85/(b.y-b.x)
		model_bottom=b.x
	_update_model_scale()
	_update_appearance()

func _update_model_scale() -> void:
	if not is_instance_valid(model):return
	model.scale=APPEARANCE.body_scale(appearance)*model_base_scale
	model.position.y=-model_bottom*model.scale.y

func _update_appearance() -> void:
	if is_instance_valid(model):APPEARANCE.apply(model,appearance)

func _rotate(amount:float) -> void:
	if is_instance_valid(model):model.rotation.y+=amount

func _zoom(factor:float) -> void:
	if is_instance_valid(camera):camera.size=clampf(camera.size*factor,0.48,4.0)

func _handle_preview(event:InputEvent) -> void:
	if event is InputEventScreenDrag:
		_rotate(event.relative.x*0.012)
	elif event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		_rotate(event.relative.x*0.012)
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index==MOUSE_BUTTON_WHEEL_UP:_zoom(0.9)
		if event.button_index==MOUSE_BUTTON_WHEEL_DOWN:_zoom(1.1)

func _finish() -> void:
	_profile.set_value("hero","name",_hero_name)
	_profile.set_value("hero","model",0)
	_profile.set_value("hero","gender_display",gender_selection)
	_profile.save("user://hero.cfg")
	APPEARANCE.save(appearance)
	completed.emit()
	var loader:=get_node_or_null("/root/ForestLoading")
	if loader!=null:
		loader.begin()
		loader.prefetch_assets()
	get_tree().change_scene_to_file("res://world.tscn")
