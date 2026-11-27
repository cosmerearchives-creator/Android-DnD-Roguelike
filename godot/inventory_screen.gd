extends Control
## Forests of Hell restored inventory overlay. UI geometry follows the approved
## Wildlands portrait equipment reference, using our current Godot character model.
signal closed
signal equipment_changed(slot: String, item_id: String)
const GOLD := Color("#d4a15c")
const PALE := Color("#f0dbaf")
const BG := Color("#1b120d")
const PANEL := Color("#2b1c13")
const DATA = preload("res://inventory_data.gd")
const FONT: Font = preload("res://hud_serif.ttf")
const SLOTS := DATA.SLOT_NAMES
const SIGNS := DATA.SLOT_SIGNS
const TABS := ["Equipment","Items","Materials","Quest"]
const PROFILE := ["human_adventurer", "male_human"]
const PLATFORM = preload("res://platform_layout.gd")
var data
var active_tab := "Equipment"
var hero_model := "human_adventurer"
var preview: SubViewport
var preview_body: Node3D
var preview_camera: Camera3D
var left_view: SubViewportContainer
var details: VBoxContainer
var slot_cells: Dictionary = {}
var item_grid: GridContainer
var tab_controls: Dictionary = {}
var selection: String = "ration"
var equipped_body: Node3D
var torso_armor: Node3D
var current_slot := ""
var portrait_turn := 0.0
var section_filter := "All Items"
var quest_selected := "goblins"
var quest_kills := 0
var quest_tracking := false
var tracked_quest_id := ""
var list_scroll: ScrollContainer
var list_container: VBoxContainer
var detail_panel: Panel
var footer: Panel
var footer_action: Button
var filter_buttons: Dictionary = {}
var owned_label: Label
var content_header: Label
var _list_panel: Panel
var desktop_layout := false
var viewport_width := 720.0
var viewport_height := 1280.0
var desktop_equipment: Control
var desktop_item_grid: GridContainer
var character_summary: Dictionary = {}
var compare_visible := false

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	data = DATA.new()
	selection = data.selected_id
	var profile := ConfigFile.new()
	profile.load("user://hero.cfg")
	hero_model = PROFILE[clampi(int(profile.get_value("hero","model",0)),0,PROFILE.size()-1)]
	var settings:=ConfigFile.new()
	if settings.load("user://tracked_quest.cfg")==OK:
		quest_tracking=bool(settings.get_value("quest","tracking",false))
	desktop_layout = PLATFORM.is_pc()
	_construct()
	_refresh()
func set_model(model: String) -> void:
	if not PROFILE.has(model): return
	hero_model=model
	if is_inside_tree(): _load_model()

func _style(fill: Color = PANEL, thick: int = 2) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color=fill
	s.border_color=GOLD
	s.set_border_width_all(thick)
	s.set_corner_radius_all(11)
	s.content_margin_left=6
	s.content_margin_right=6
	s.content_margin_top=5
	s.content_margin_bottom=5
	return s

func _label(txt:String, size:int=22, alignment:HorizontalAlignment=HORIZONTAL_ALIGNMENT_LEFT) -> Label:
	var l:=Label.new()
	l.text=txt
	l.horizontal_alignment=alignment
	l.vertical_alignment=VERTICAL_ALIGNMENT_CENTER
	l.add_theme_font_override("font",FONT)
	l.add_theme_font_size_override("font_size",size)
	l.add_theme_color_override("font_color",PALE)
	l.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
	return l

var _frames: Dictionary={}
func _frame(active: bool = false) -> StyleBox:
	var key: String="active" if active else "normal"
	if _frames.has(key): return _frames[key]
	var path: String="res://ui_skin/panel_active.svg" if active else "res://ui_skin/panel.svg"
	var tex: Texture2D=null
	if ResourceLoader.exists(path):
		tex=load(path) as Texture2D
	else:
		var img:=Image.load_from_file(path)
		if img!=null:tex=ImageTexture.create_from_image(img)
	if tex==null:return _style(Color("#315052") if active else PANEL,3 if active else 2)
	var style:=StyleBoxTexture.new()
	style.texture=tex
	style.texture_margin_left=43
	style.texture_margin_right=43
	style.texture_margin_top=43
	style.texture_margin_bottom=43
	style.content_margin_left=7
	style.content_margin_right=7
	style.content_margin_top=6
	style.content_margin_bottom=6
	_frames[key]=style
	return style

func _compact_frame(active: bool = false) -> StyleBoxTexture:
	var box := _frame(active).duplicate() as StyleBoxTexture
	box.texture_margin_left = 21
	box.texture_margin_right = 21
	box.texture_margin_top = 21
	box.texture_margin_bottom = 21
	return box

func _button(txt:String, callback:Callable, px:int=22) -> Button:
	var b:=Button.new()
	b.text=txt
	b.add_theme_font_override("font",FONT)
	b.add_theme_font_size_override("font_size",px)
	b.add_theme_color_override("font_color",PALE)
	b.add_theme_stylebox_override("normal",_frame())
	b.add_theme_stylebox_override("hover",_frame(true))
	b.add_theme_stylebox_override("pressed",_frame(true))
	b.pressed.connect(callback)
	return b

func _panel(parent:Control) -> PanelContainer:
	var panel:=PanelContainer.new()
	panel.add_theme_stylebox_override("panel",_style(BG))
	parent.add_child(panel)
	return panel
func _column(parent:Node) -> VBoxContainer:
	var v:=VBoxContainer.new()
	v.add_theme_constant_override("separation",8)
	parent.add_child(v)
	return v
func _fixed_panel(parent:Control, area:Rect2) -> Panel:
	var panel:=Panel.new()
	panel.position=area.position
	panel.size=area.size
	panel.add_theme_stylebox_override("panel",_frame())
	parent.add_child(panel)
	return panel

func _construct() -> void:
	if desktop_layout:
		_construct_desktop()
		return
	var h:float=size.y if size.y>100 else 1280.0
	var w:float=size.x if size.x>100 else 720.0
	viewport_width=w
	viewport_height=h
	var scale_x:float=w/720.0
	var scale_y:float=h/1280.0
	# Use the full available portrait height, while reserving the top HUD.
	var veil:=ColorRect.new()
	veil.color=Color(0.0,0.025,0.03,0.95)
	add_child(veil)
	veil.position=Vector2(0,104)
	veil.size=Vector2(w,h-104)
	veil.mouse_filter=Control.MOUSE_FILTER_STOP
	var canvas:=Control.new()
	canvas.name="InventoryCanvas"
	add_child(canvas)
	canvas.position=Vector2.ZERO
	canvas.size=Vector2(w,h)
	# Logical coordinates remain 720 wide, with a flexible vertical inventory area.
	canvas.scale=Vector2.ONE if desktop_layout else Vector2(scale_x,1.0)
	var head:=_fixed_panel(canvas,Rect2(16,110,w-32 if desktop_layout else 688,92))
	var crest:=Polygon2D.new()
	crest.color=Color("#092b30")
	crest.polygon=PackedVector2Array([Vector2(235,4),Vector2(270,-9),Vector2(305,-24),Vector2(344,-30),Vector2(383,-24),Vector2(418,-9),Vector2(453,4)])
	head.add_child(crest)
	var title:=_label("Inventory",42,HORIZONTAL_ALIGNMENT_CENTER)
	title.position=Vector2(155,18)
	title.size=Vector2(440,58)
	head.add_child(title)
	var satchel:=TextureRect.new()
	satchel.texture=data.icon("backpack")
	satchel.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	satchel.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	satchel.position=Vector2(163,20)
	satchel.size=Vector2(62,58)
	head.add_child(satchel)
	var close:=_button("×",func():closed.emit(),36)
	close.position=Vector2(w-84 if desktop_layout else 624,16)
	close.size=Vector2(52,60)
	head.add_child(close)
	var tabs_y:=212.0
	for i in range(TABS.size()):
		var tab: String=TABS[i]
		var button:=_button("Quests" if tab=="Quest" else tab,func():_select_tab(tab),24)
		button.position=Vector2(20+i*(173 if not desktop_layout else 188),tabs_y)
		button.size=Vector2(163 if not desktop_layout else 180,62)
		canvas.add_child(button)
		tab_controls[tab]=button
	var equipment_y:=285.0
	var equipment_height:float=418.0+maxf(0.0,h-1280.0)*0.46
	var primary:=_fixed_panel(canvas,Rect2(20,equipment_y,680,equipment_height))
	var left_width:=265.0
	left_view=SubViewportContainer.new()
	left_view.stretch=true
	left_view.position=Vector2(14,16)
	left_view.size=Vector2(left_width,equipment_height-75)
	left_view.mouse_filter=Control.MOUSE_FILTER_STOP
	primary.add_child(left_view)
	preview=SubViewport.new()
	preview.size=Vector2i(400,620)
	preview.own_world_3d=true
	preview.transparent_bg=true
	preview.render_target_update_mode=SubViewport.UPDATE_ALWAYS
	left_view.add_child(preview)
	var scene:=Node3D.new()
	preview.add_child(scene)
	var sun:=DirectionalLight3D.new()
	sun.rotation_degrees=Vector3(-45,25,0)
	sun.light_energy=1.6
	scene.add_child(sun)
	var environment:=WorldEnvironment.new()
	environment.environment=Environment.new()
	environment.environment.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
	environment.environment.ambient_light_color=Color.WHITE
	environment.environment.ambient_light_energy=0.65
	environment.environment.background_mode=Environment.BG_COLOR
	environment.environment.background_color=Color(0.0,0.0,0.0,0.0)
	scene.add_child(environment)
	equipped_body=Node3D.new()
	scene.add_child(equipped_body)
	preview_camera=Camera3D.new()
	scene.add_child(preview_camera)
	preview_camera.current=true
	preview_camera.projection=Camera3D.PROJECTION_ORTHOGONAL
	preview_camera.size=2.35
	preview_camera.position=Vector3(0,0.95,4.0)
	preview_camera.look_at(Vector3(0,0.95,0))
	left_view.gui_input.connect(_rotate_preview)
	# Pedestal outline and portrait-turn buttons from the approved layout.
	var pedestal:=Line2D.new()
	pedestal.width=1.7
	pedestal.default_color=GOLD
	for i in range(65):
		var a:=float(i)*TAU/64.0
		pedestal.add_point(Vector2(145+108*cos(a),equipment_height-68+23*sin(a)))
	primary.add_child(pedestal)
	for pair in [[-1,"↶",17],[1,"↷",234]]:
		var turn:=_button(pair[1],_turn_preview.bind(pair[0]),23)
		turn.position=Vector2(pair[2],equipment_height-57)
		turn.size=Vector2(42,38)
		primary.add_child(turn)
	# Fifteen approved equipment slots: no separate Ears or Legs, Ranged is Secondary.
	var columns := [["Head","Cloak","Hands","Wrists","Left Ring","Main Hand"],["Neck","Body","Waist","Feet"],["Eyes","Accessory","Off Hand","Right Ring","Secondary"]]
	var step:float=(equipment_height-28)/6.0
	for col in range(3):
		for row in range(columns[col].size()):
			var slot:String=columns[col][row]
			var cell:=_button(SIGNS[SLOTS.find(slot)],_select_slot.bind(slot),24)
			var sw:float=100.0 if col==1 else 82.0
			cell.position=Vector2(342+col*132-sw/2,14+row*step)
			cell.size=Vector2(sw,step-7)
			cell.tooltip_text=slot
			cell.icon_alignment=HORIZONTAL_ALIGNMENT_CENTER
			primary.add_child(cell)
			slot_cells[slot]=cell
	var stat_y:=equipment_y if desktop_layout else equipment_y+equipment_height+10
	var stat_height:=150.0+maxf(0.0,h-1280.0)*0.06
	var left:=_fixed_panel(canvas,Rect2(720 if desktop_layout else 20,stat_y,294,stat_height))
	var stats:=_column(left)
	stats.add_theme_constant_override("separation",0)
	stats.position=Vector2(16,7)
	stats.size=Vector2(265,stat_height-12)
	stats.add_child(_label("Stats",23,HORIZONTAL_ALIGNMENT_CENTER))
	for line in ["HP                 —","Attack              —","Defense             —","Move            30 ft","Carry"]:
		var l:=_label(line,18)
		if line=="Carry": l.name="CarryValue"
		stats.add_child(l)
	var right:=_fixed_panel(canvas,Rect2(1024 if desktop_layout else 325,stat_y,w-1044 if desktop_layout else 375,stat_height))
	details=_column(right)
	details.position=Vector2(12,8)
	details.size=Vector2(350,stat_height-16)
	var inventory_y:=stat_y+stat_height+10
	var inventory_h:=maxf(180.0,h-inventory_y-17)
	var inventory_panel:=_fixed_panel(canvas,Rect2(720 if desktop_layout else 20,inventory_y,w-740 if desktop_layout else 680,inventory_h))
	var inventory_label:=_label("Inventory",27)
	inventory_label.name="CarryHeading"
	inventory_label.position=Vector2(18,8)
	inventory_label.size=Vector2(505,37)
	inventory_panel.add_child(inventory_label)
	var sort:=_button("Sort ▾",_sort_items,20)
	sort.position=Vector2(w-880 if desktop_layout else 547,7)
	sort.size=Vector2(118,38)
	inventory_panel.add_child(sort)
	var scroll:=ScrollContainer.new()
	scroll.position=Vector2(12,51)
	scroll.size=Vector2(w-764 if desktop_layout else 656,inventory_h-62)
	scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED
	inventory_panel.add_child(scroll)
	item_grid=GridContainer.new()
	item_grid.columns=6 if not desktop_layout else maxi(4,int((w-780)/105))
	item_grid.size_flags_horizontal=Control.SIZE_EXPAND_FILL
	item_grid.add_theme_constant_override("h_separation",7)
	item_grid.add_theme_constant_override("v_separation",7)
	scroll.add_child(item_grid)
	_load_model()

func _footer_pressed() -> void:
	if active_tab=="Items":
		_equip_selected()
		return
	if active_tab!="Quest" or quest_selected!="goblins":return
	quest_tracking=not quest_tracking
	var settings:=ConfigFile.new()
	settings.set_value("quest","tracking",quest_tracking)
	settings.save("user://tracked_quest.cfg")
	_render_list_details()

func _filters() -> Array:
	if active_tab=="Materials":
		return ["All Materials","Ores & Metals","Wood & Plant","Hides & Leathers","Cloth & Fiber","Bone & Horn","Stone & Mineral","Alchemy Reagents","Essences & Rares","Crafting Components"]
	if active_tab=="Quest":
		return ["All Quests","Main Quest","Side Quests","Faction","Task","Completed","Failed"]
	return ["All Items","Weapons","Armor","Consumables","Tools","Loot","Keys & Access","Quest Items","Miscellaneous"]

func _make_list_layout() -> void:
	if desktop_layout:
		_make_list_layout_desktop()
		return
	var canvas:=get_node("InventoryCanvas") as Control
	_list_panel=_fixed_panel(canvas,Rect2(16,285,viewport_width-32 if desktop_layout else 688,
		viewport_height-303 if desktop_layout else maxf(640,size.y-303)))
	_list_panel.name="TabbedListLayout"
	var list_height:float=_list_panel.size.y-350.0
	if desktop_layout: list_height=_list_panel.size.y-20.0
	var sidebar:=_fixed_panel(_list_panel,Rect2(12,10,204,list_height))
	var sidebar_stack:=VBoxContainer.new()
	sidebar_stack.position=Vector2(7,8)
	sidebar_stack.size=Vector2(190,list_height-18)
	sidebar_stack.add_theme_constant_override("separation",10)
	sidebar.add_child(sidebar_stack)
	sidebar_stack.name="FilterSidebar"
	var main:=_fixed_panel(_list_panel,Rect2(222,10,(viewport_width-265)*0.43 if desktop_layout else 453,list_height))
	content_header=_label("",28)
	content_header.position=Vector2(22,13)
	content_header.size=Vector2(265,43)
	main.add_child(content_header)
	var sort:=_button("Sort ▾",_sort_items,20)
	sort.position=Vector2(330,12)
	sort.size=Vector2(110,41)
	main.add_child(sort)
	list_scroll=ScrollContainer.new()
	list_scroll.position=Vector2(14,64)
	list_scroll.size=Vector2(main.size.x-28 if desktop_layout else 428,list_height-76)
	list_scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED
	main.add_child(list_scroll)
	list_container=VBoxContainer.new()
	list_container.add_theme_constant_override("separation",6)
	list_container.size_flags_horizontal=Control.SIZE_EXPAND_FILL
	list_scroll.add_child(list_container)
	detail_panel=_fixed_panel(_list_panel,Rect2(main.position.x+main.size.x+12 if desktop_layout else 12,10 if desktop_layout else list_height+12,
		viewport_width-main.position.x-main.size.x-54 if desktop_layout else 664,list_height-74 if desktop_layout else 260))
	detail_panel.name="DetailPane"
	footer=_fixed_panel(_list_panel,Rect2(main.position.x+main.size.x+12 if desktop_layout else 12,list_height-56 if desktop_layout else list_height+282,
		viewport_width-main.position.x-main.size.x-54 if desktop_layout else 664,57))
	owned_label=_label("",23)
	owned_label.position=Vector2(18,6)
	owned_label.size=Vector2(415,44)
	footer.add_child(owned_label)
	footer_action=_button("Use",_footer_pressed,23)
	footer_action.position=Vector2(footer.size.x-178 if desktop_layout else 484,7)
	footer_action.size=Vector2(166,44)
	footer.add_child(footer_action)
	footer_action.disabled=true

func _replace_children(container:Node) -> void:
	for child in container.get_children():
		container.remove_child(child)
		child.queue_free()

func _refresh_sidebar() -> void:
	var sidebar_stack:VBoxContainer=_list_panel.find_child("FilterSidebar",true,false)
	_replace_children(sidebar_stack)
	filter_buttons.clear()
	for group in _filters():
		var title:String=group
		var glyphs:Dictionary={
			"All Items":"▦","Consumables":"◇","Tools":"⚒","Supplies":"▣","Utility":"♙","Other":"•••",
			"All Materials":"▦","Raw Materials":"◆","Plants & Herbs":"♧","Animal Parts":"♟",
			"Ore & Gems":"◈","Crafting Parts":"⚙",
			"All Quests":"▦","Main Story":"▤","Side Quests":"◇","Tasks":"▤","Completed":"✓"
		}
		var btn:=_button(str(glyphs.get(group,"•"))+"  "+group,func():
			section_filter=title
			_refresh(),20)
		btn.custom_minimum_size.y=52 if desktop_layout else 64
		btn.add_theme_stylebox_override("normal",_frame(group==section_filter))
		sidebar_stack.add_child(btn)
		filter_buttons[group]=btn

func _belongs_filter(item:Dictionary, id:String) -> bool:
	var kind: String = str(item.get("type","Other"))
	if section_filter.begins_with("All"): return true
	if active_tab=="Items":
		match section_filter:
			"Weapons": return kind=="Weapon"
			"Armor": return kind in ["Armor","Shield"]
			"Consumables": return kind=="Supply" and id in ["ration","waterskin","oil","candle","potion"]
			"Tools": return kind=="Tool"
			"Loot": return kind in ["Treasure","Loot"]
			"Keys & Access": return "key" in id or "insignia" in id or "pass" in id
			"Quest Items": return data.category(id)=="Quest"
			"Miscellaneous": return kind not in ["Weapon","Armor","Shield","Tool","Material"]
	elif active_tab=="Materials":
		match section_filter:
			"Ores & Metals": return "ore" in id or "ingot" in id or "metal" in id
			"Wood & Plant": return "wood" in id or "log" in id or "herb" in id or "plant" in id
			"Hides & Leathers": return "hide" in id or "leather" in id or "fur" in id
			"Cloth & Fiber": return "cloth" in id or "fiber" in id or "silk" in id or "linen" in id
			"Bone & Horn": return "bone" in id or "horn" in id or "antler" in id
			"Stone & Mineral": return "stone" in id or "granite" in id or "obsidian" in id
			"Alchemy Reagents": return "reagent" in id or "root" in id or "shade" in id
			"Essences & Rares": return "essence" in id or "rare" in id
			"Crafting Components": return "component" in id or "gear" in id
	return false

func _refresh_list() -> void:
	if desktop_layout:
		_refresh_desktop_list()
		return
	_replace_children(list_container)
	var rows:Array[Dictionary]=[]
	if active_tab=="Quest":
		var goblin:Dictionary={"id":"goblins","name":"Goblin Menace","group":"Side Quests","icon":"goblin_warrior"}
		rows.append(goblin)
		if section_filter not in ["All Quests","Side Quests","Completed"]:rows.clear()
		if section_filter=="Completed" and quest_kills<10:rows.clear()
		if section_filter=="Side Quests" and quest_kills>=10:rows.clear()
		content_header.text="Quests (%d)" % rows.size()
	else:
		for stack in data.items:
			var id:String=str(stack.id)
			if data.category(id)!=active_tab:continue
			var item:Dictionary=data.detail(id)
			if not _belongs_filter(item,id):continue
			rows.append({"id":id,"name":str(item.get("name",id)),"count":int(stack.count),"item":item})
		content_header.text=("%s (%d)" % [active_tab,rows.size()])
	if active_tab!="Quest":
		var visible:=false
		for entry in rows:
			if str(entry.id)==selection:visible=true
		if not visible:selection=str(rows[0].id) if not rows.is_empty() else ""
	for entry in rows:
		var id:String=str(entry.id)
		var card:=_button("",func():
			if active_tab=="Quest":quest_selected=id
			else:selection=id
			_refresh_list()
			_render_list_details(),19)
		card.custom_minimum_size.y=70
		card.add_theme_stylebox_override("normal",_compact_frame(id==selection if active_tab!="Quest" else id==quest_selected))
		list_container.add_child(card)
		var icon:=TextureRect.new()
		icon.position=Vector2(11,7)
		icon.size=Vector2(58,56)
		icon.texture=data.icon(id) if active_tab!="Quest" else data.icon("enemy_trophy")
		icon.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
		icon.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		icon.mouse_filter=Control.MOUSE_FILTER_IGNORE
		card.add_child(icon)
		var name:=_label(str(entry.name),23)
		name.position=Vector2(83,8)
		name.size=Vector2(244,35 if active_tab=="Quest" else 54)
		name.mouse_filter=Control.MOUSE_FILTER_IGNORE
		card.add_child(name)
		if active_tab=="Quest":
			var subtitle:=_label("Side Quest" if quest_kills<10 else "Completed",17)
			subtitle.position=Vector2(84,38)
			subtitle.size=Vector2(245,25)
			subtitle.mouse_filter=Control.MOUSE_FILTER_IGNORE
			card.add_child(subtitle)
		if entry.has("count"):
			var qty:=_label("×%d" % int(entry.count),21,HORIZONTAL_ALIGNMENT_RIGHT)
			qty.position=Vector2(332,12)
			qty.size=Vector2(73,46)
			qty.mouse_filter=Control.MOUSE_FILTER_IGNORE
			card.add_child(qty)

func _render_list_details() -> void:
	if desktop_layout:
		_render_desktop_list_details()
		return
	# Detail text is separate from the list, like the reference screenshots.
	_replace_children(detail_panel)
	footer_action.text=("Tracking" if quest_tracking else "Track") if active_tab=="Quest" else "Use"
	footer_action.disabled=true
	owned_label.text="Inventory  %.1f / %.0f" % [data.weight(),data.capacity()]
	if active_tab=="Quest":
		var available:=section_filter in ["All Quests","Side Quests"] and quest_kills<10 or section_filter in ["All Quests","Completed"] and quest_kills>=10
		if not available:
			var none:=_label("No quests in this category.",23)
			none.position=Vector2(20,20)
			none.size=Vector2(605,70)
			detail_panel.add_child(none)
			return
		var name:=_label("Goblin Menace",32)
		name.position=Vector2(145,14)
		name.size=Vector2(480,47)
		detail_panel.add_child(name)
		var sub:=_label("Side Quest",22)
		sub.position=Vector2(145,63)
		sub.size=Vector2(440,28)
		detail_panel.add_child(sub)
		var text:=_label("Objectives: Defeat Goblins (%d / 10)" % mini(quest_kills,10),24)
		text.position=Vector2(25,108)
		text.size=Vector2(600,43)
		detail_panel.add_child(text)
		var description:=_label("The town has been plagued by goblin attacks. Defeat ten goblins to make the roads safer.",19)
		description.position=Vector2(25,160)
		description.size=Vector2(610,75)
		detail_panel.add_child(description)
		footer_action.disabled=false
		return
	var item:Dictionary=data.detail(selection)
	if item.is_empty() or data.category(selection)!=active_tab:
		var choose:=_label("Select an item to see its details.",25)
		choose.position=Vector2(20,12)
		choose.size=Vector2(605,60)
		detail_panel.add_child(choose)
		return
	var big:=TextureRect.new()
	big.texture=data.icon(selection)
	big.position=Vector2(20,18)
	big.size=Vector2(135,135)
	big.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	big.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	detail_panel.add_child(big)
	var title:=_label(str(item.get("name",selection)),31)
	title.position=Vector2(170,14)
	title.size=Vector2(467,53)
	detail_panel.add_child(title)
	var kind:=_label(str(item.get("type","Item")).to_upper(),19)
	kind.position=Vector2(170,66)
	kind.size=Vector2(470,28)
	detail_panel.add_child(kind)
	var weight:=_label("Weight: %.1f lb" % float(item.get("weight",0)),22)
	weight.position=Vector2(170,102)
	weight.size=Vector2(470,35)
	detail_panel.add_child(weight)
	var description:=_label("Select this item to inspect or equip it.",20)
	description.position=Vector2(170,153)
	description.size=Vector2(470,65)
	detail_panel.add_child(description)

func set_goblin_kills(count:int) -> void:
	quest_kills=maxi(0,count)
	if active_tab=="Quest":_refresh()
func _load_model() -> void:
	if not is_instance_valid(equipped_body):return
	for child in equipped_body.get_children():child.queue_free()
	var path:="res://assets/%s.glb" % hero_model
	if hero_model=="male_human" and data.equipped_id("Body")=="leather_armor":
		path="res://assets/male_human_leather_armored.glb"
	if not ResourceLoader.exists(path):return
	var body:Node3D=(load(path) as PackedScene).instantiate()
	body.name="PortraitModel"
	equipped_body.add_child(body)
	var bounds:=_find_bounds(body,Transform3D.IDENTITY)
	if bounds.y>bounds.x and is_finite(bounds.x):
		var scale:=1.85/(bounds.y-bounds.x)
		body.scale=Vector3.ONE*scale
		body.position.y=-bounds.x*scale
	body.rotation.y=portrait_turn
func _find_bounds(node:Node3D, parent_transform:Transform3D) -> Vector2:
	var transform:=parent_transform*node.transform
	var bottom:=INF
	var top:=-INF
	if node is MeshInstance3D and node.mesh!=null:
		var bounds:AABB=node.get_aabb()
		for i in range(8):
			var y:float=(transform*bounds.get_endpoint(i)).y
			bottom=minf(bottom,y)
			top=maxf(top,y)
	for child in node.get_children():
		if child is Node3D:
			var sub:=_find_bounds(child,transform)
			bottom=minf(bottom,sub.x)
			top=maxf(top,sub.y)
	return Vector2(bottom,top)
func _turn_preview(steps:int) -> void:
	portrait_turn+=float(steps)*PI/4.0
	if is_instance_valid(equipped_body) and equipped_body.get_child_count()>0:
		equipped_body.get_child(0).rotation.y=portrait_turn

func _rotate_preview(event:InputEvent) -> void:
	if event is InputEventScreenDrag or (event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT)):
		portrait_turn+=event.relative.x*.014
		if equipped_body.get_child_count()>0:
			equipped_body.get_child(0).rotation.y=portrait_turn

func _select_tab(value:String) -> void:
	active_tab=value
	section_filter="All " + ("Materials" if value=="Materials" else "Quests" if value=="Quest" else "Items")
	_refresh()
func _select_slot(slot:String) -> void:
	current_slot=slot
	var item: String=data.equipped_id(slot)
	if item!="":selection=item
	_refresh()
func _select_item(id:String) -> void:
	selection=id
	data.select(id)
	if active_tab!="Equipment":
		_refresh_list()
		_render_list_details()
	else:_refresh()
func _sort_items() -> void:
	data.items.sort_custom(func(a:Dictionary,b:Dictionary) -> bool:
		return str(data.detail(str(a.id)).get("name",a.id))<str(data.detail(str(b.id)).get("name",b.id)))
	data._save()
	_refresh()

func _refresh() -> void:
	if desktop_layout:
		_refresh_desktop()
		return
	for tab in TABS:
		var b:Button=tab_controls[tab]
		b.add_theme_stylebox_override("normal",_frame(tab==active_tab))
	# Switch the content region, preserving equipment's original layout.
	if active_tab=="Equipment":
		if is_instance_valid(_list_panel): _list_panel.visible=false
	else:
		if not is_instance_valid(_list_panel): _make_list_layout()
		_list_panel.visible=true
		_refresh_sidebar()
		_refresh_list()
	var carrying: float=data.weight()
	var max_weight: float=data.capacity()
	find_child("CarryHeading",true,false).text="Inventory  %.1f / %.0f lb" % [carrying,max_weight]
	find_child("CarryValue",true,false).text="Carry  %.1f / %.0f" % [carrying,max_weight]
	for slot in SLOTS:
		var b:Button=slot_cells[slot]
		var id: String=data.equipped_id(slot)
		b.icon=data.icon(id) if id!="" else null
		b.expand_icon=true
		b.text="" if id!="" else SIGNS[SLOTS.find(slot)]
		b.add_theme_stylebox_override("normal",_compact_frame(slot==current_slot))
	if active_tab=="Equipment":
		_render_items()
		_render_details()
	else:
		_render_list_details()

func _render_items() -> void:
	for child in item_grid.get_children():
		item_grid.remove_child(child)
		child.queue_free()
	var entries: Array[Dictionary]=[]
	if active_tab=="Equipment": entries=data.items.duplicate(true)
	elif active_tab=="Items": entries=data.stacks("Items")
	elif active_tab=="Materials":entries=data.stacks("Materials")
	elif active_tab=="Quest":entries=data.stacks("Quest")
	var count:=0
	for stack in entries:
		var id:=str(stack.id)
		var item:Dictionary=data.detail(id)
		var cell:=_button("",func():_select_item(id),18)
		cell.custom_minimum_size=Vector2(81,74) if desktop_layout else Vector2(98,88)
		cell.add_theme_stylebox_override("normal",_compact_frame(selection==id))
		item_grid.add_child(cell)
		var icon: Texture2D=data.icon(id)
		if icon!=null:
			cell.icon=icon
			cell.expand_icon=true
			cell.icon_alignment=HORIZONTAL_ALIGNMENT_CENTER
			cell.vertical_icon_alignment=VERTICAL_ALIGNMENT_CENTER
		else:cell.text=str(item.get("name",id)).substr(0,8)
		cell.tooltip_text=str(item.get("name",id))
		var quantity:=int(stack.count)
		if quantity>0:
			var label:=_label(str(quantity),16,HORIZONTAL_ALIGNMENT_RIGHT)
			label.mouse_filter=Control.MOUSE_FILTER_IGNORE
			cell.add_child(label)
			label.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
			label.offset_left=-34
			label.offset_top=-25
			label.offset_right=-7
			label.offset_bottom=-4
		count+=1
	if active_tab=="Quest" and entries.is_empty():
		var notice:=_label("No quest items collected.",22)
		item_grid.add_child(notice)
	elif active_tab=="Materials" and entries.is_empty():
		var notice:=_label("No materials collected.",22)
		item_grid.add_child(notice)
	for i in range(maxi(0,24-count)):
		var blank:=PanelContainer.new()
		blank.add_theme_stylebox_override("panel",_compact_frame())
		blank.custom_minimum_size=Vector2(81,74) if desktop_layout else Vector2(98,88)
		item_grid.add_child(blank)

func _render_details() -> void:
	if desktop_layout:
		_render_desktop_equipment_details()
		return
	for child in details.get_children():
		details.remove_child(child)
		child.queue_free()
	var item:Dictionary=data.detail(selection)
	if item.is_empty():
		details.add_child(_label("Select an item or slot",22))
		return
	var row:=HBoxContainer.new()
	details.add_child(row)
	var image:=TextureRect.new()
	image.texture=data.icon(selection)
	image.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
	image.custom_minimum_size=Vector2(75,75)
	image.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	row.add_child(image)
	var title:=_column(row)
	row.size_flags_horizontal=Control.SIZE_EXPAND_FILL
	title.size_flags_horizontal=Control.SIZE_EXPAND_FILL
	title.custom_minimum_size.x=230
	title.add_child(_label(str(item.get("name",selection)),23))
	title.add_child(_label(str(item.get("type","Item")).to_upper(),18))
	title.add_child(_label("%.1f lb" % float(item.get("weight",0)),20))
	if current_slot!="":
		details.add_child(_label("Slot: "+current_slot,18))
		if data.compatible(selection,current_slot) and data.equipped_id(current_slot)!=selection:
			details.add_child(_button("Equip",func():
				if data.set_slot(current_slot,selection):
					_load_model()
					equipment_changed.emit(current_slot,selection)
					_refresh(),20))
		elif data.equipped_id(current_slot)!="":
			details.add_child(_button("Unequip",func():
				if data.set_slot(current_slot,""):
					_load_model()
					equipment_changed.emit(current_slot,"")
					_refresh(),20))
		else:
			details.add_child(_label("Select compatible equipment.",16))
	else:details.add_child(_label("Select an equipment slot to equip.",16))

# Landscape layout authored against the user's four ornate 1670x941 references.
# The legacy portrait layout above remains available for phones.
func _construct_desktop() -> void:
	var w := size.x if size.x > 100 else 1600.0
	var h := size.y if size.y > 100 else 900.0
	viewport_width = 1600.0
	viewport_height = 900.0
	var veil := ColorRect.new()
	veil.color = Color(.10,.055,.02,.82)
	veil.position = Vector2(0,80)
	veil.size = Vector2(w,h-80)
	veil.mouse_filter = Control.MOUSE_FILTER_STOP
	add_child(veil)
	var canvas := Control.new()
	canvas.name = "InventoryCanvas"
	canvas.size = Vector2(1600,900)
	canvas.scale = Vector2(w/1600.0,h/900.0)
	add_child(canvas)
	var heading := _fixed_panel(canvas,Rect2(16,84,618,60))
	var title := _label("INVENTORY",39,HORIZONTAL_ALIGNMENT_CENTER)
	title.position = Vector2(110,6)
	title.size = Vector2(398,48)
	heading.add_child(title)
	var close := _button("X",func():closed.emit(),24)
	close.position = Vector2(1530,90)
	close.size = Vector2(54,48)
	canvas.add_child(close)
	for i in range(TABS.size()):
		var tab: String = TABS[i]
		var label: String = "Quests" if tab=="Quest" else tab
		var button := _button(label,func():_select_tab(tab),22)
		button.position = Vector2(18+i*155,149)
		button.size = Vector2(153,57)
		button.tooltip_text = "Manage "+label.to_lower()
		canvas.add_child(button)
		tab_controls[tab] = button
	desktop_equipment = Control.new()
	desktop_equipment.name = "EquipmentDesktop"
	desktop_equipment.size = Vector2(1600,900)
	canvas.add_child(desktop_equipment)
	var character_panel := _fixed_panel(desktop_equipment,Rect2(18,214,618,668))
	left_view = SubViewportContainer.new()
	left_view.stretch = true
	left_view.position = Vector2(137,15)
	left_view.size = Vector2(345,558)
	left_view.mouse_filter = Control.MOUSE_FILTER_STOP
	character_panel.add_child(left_view)
	preview = SubViewport.new()
	preview.size = Vector2i(430,660)
	preview.own_world_3d = true
	preview.transparent_bg = true
	preview.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	left_view.add_child(preview)
	var portrait_world := Node3D.new()
	preview.add_child(portrait_world)
	var sunlight := DirectionalLight3D.new()
	sunlight.rotation_degrees = Vector3(-45,25,0)
	sunlight.light_energy = 1.65
	portrait_world.add_child(sunlight)
	var environment := WorldEnvironment.new()
	environment.environment = Environment.new()
	environment.environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.environment.ambient_light_color = Color.WHITE
	environment.environment.ambient_light_energy = .72
	environment.environment.background_mode = Environment.BG_COLOR
	environment.environment.background_color = Color(0,0,0,0)
	portrait_world.add_child(environment)
	equipped_body = Node3D.new()
	portrait_world.add_child(equipped_body)
	preview_camera = Camera3D.new()
	portrait_world.add_child(preview_camera)
	preview_camera.current = true
	preview_camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	preview_camera.size = 2.4
	preview_camera.position = Vector3(0,1.0,4)
	preview_camera.look_at(Vector3(0,1.0,0))
	left_view.gui_input.connect(_rotate_preview)
	var pedestal := Line2D.new()
	pedestal.width = 2.1
	pedestal.default_color = GOLD
	for i in range(65):
		var a := float(i)*TAU/64.0
		pedestal.add_point(Vector2(309+111*cos(a),597+19*sin(a)))
	character_panel.add_child(pedestal)
	var left_slots := ["Head","Eyes","Main Hand","Off Hand","Secondary","Hands","Wrists"]
	var right_slots := ["Body","Neck","Cloak","Left Ring","Right Ring","Accessory","Waist","Feet"]
	for row in range(left_slots.size()):
		_desktop_slot(character_panel,left_slots[row],Vector2(18,15+row*86),Vector2(114,77))
	for row in range(right_slots.size()):
		_desktop_slot(character_panel,right_slots[row],Vector2(485,15+row*78),Vector2(114,70))
	var turn_left := _button("<",_turn_preview.bind(-1),18)
	turn_left.position = Vector2(179,610)
	turn_left.size = Vector2(54,39)
	character_panel.add_child(turn_left)
	var turn_right := _button(">",_turn_preview.bind(1),18)
	turn_right.position = Vector2(386,610)
	turn_right.size = Vector2(54,39)
	character_panel.add_child(turn_right)
	# Character, 5e ability names, combat values and proficiencies.
	var stats_panel := _fixed_panel(desktop_equipment,Rect2(646,112,940,215))
	var stat_names := ["CHARACTER","ABILITY SCORES","COMBAT & DEFENSES","PROFICIENCIES"]
	for col in range(4):
		var box := _fixed_panel(stats_panel,Rect2(7+col*232,9,228,197))
		var head := _label(stat_names[col],20,HORIZONTAL_ALIGNMENT_CENTER)
		head.position = Vector2(7,4)
		head.size = Vector2(212,35)
		box.add_child(head)
		var info := _label("",16)
		info.name = "CharacterStatBlock%d" % col
		info.position = Vector2(20,52)
		info.size = Vector2(190,132)
		box.add_child(info)
	var equipment_inventory := _fixed_panel(desktop_equipment,Rect2(646,338,940,311))
	var carry_heading := _label("Inventory",25)
	carry_heading.name = "CarryHeading"
	carry_heading.position = Vector2(22,7)
	carry_heading.size = Vector2(495,43)
	equipment_inventory.add_child(carry_heading)
	var sorting := _button("Sort: Name",_sort_items,19)
	sorting.position = Vector2(775,8)
	sorting.size = Vector2(144,40)
	equipment_inventory.add_child(sorting)
	var scroll := ScrollContainer.new()
	scroll.position = Vector2(15,54)
	scroll.size = Vector2(912,244)
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	equipment_inventory.add_child(scroll)
	item_grid = GridContainer.new()
	item_grid.columns = 10
	item_grid.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	item_grid.add_theme_constant_override("h_separation",3)
	item_grid.add_theme_constant_override("v_separation",4)
	scroll.add_child(item_grid)
	var item_details := _fixed_panel(desktop_equipment,Rect2(646,659,940,223))
	details = _column(item_details)
	details.position = Vector2(19,12)
	details.size = Vector2(904,202)
	_load_model()

func _desktop_slot(parent: Control, slot: String, location: Vector2, extent: Vector2) -> void:
	var button := _button(SIGNS[SLOTS.find(slot)],_select_slot.bind(slot),27)
	button.position = location
	button.size = extent
	button.tooltip_text = slot
	button.icon_alignment = HORIZONTAL_ALIGNMENT_CENTER
	parent.add_child(button)
	var caption := _label("GOGGLES" if slot=="Eyes" else slot.to_upper(),12,HORIZONTAL_ALIGNMENT_CENTER)
	caption.position = Vector2(3,4)
	caption.size = Vector2(extent.x-6,22)
	caption.mouse_filter = Control.MOUSE_FILTER_IGNORE
	button.add_child(caption)
	button.add_theme_stylebox_override("normal",_compact_frame())
	button.add_theme_stylebox_override("hover",_compact_frame(true))
	slot_cells[slot] = button

func set_character_summary(summary: Dictionary) -> void:
	character_summary = summary.duplicate()
	if desktop_layout and is_inside_tree(): _update_character_stats()

func _update_character_stats() -> void:
	if not desktop_layout: return
	var values := character_summary
	var strength := int(data.strength)
	var ability := "STR  %d (%+d)\n" % [strength,floori((float(strength)-10)/2)]
	for stat in ["DEX","CON","INT","WIS","CHA"]:
		ability += "%s  %s\n" % [stat,str(values[stat]) if values.has(stat) else "—"]
	var blocks := [
		"Level —\nRace: Not set\nClass: Fighter (test)\nBackground: Not set",
		ability,
		"AC     %s\nHP     %s / %s\nSpeed  30 ft\nInitiative  —" % [str(values.get("AC","—")),str(values.get("HP","—")),str(values.get("MaxHP","—"))],
		"Weapons: Not recorded\nArmor: Not recorded\nSkills: Not recorded\nSaves: Not recorded"]
	for i in range(4):
		var info := find_child("CharacterStatBlock%d"%i,true,false) as Label
		if info != null: info.text = blocks[i]

func _make_list_layout_desktop() -> void:
	var canvas := get_node("InventoryCanvas") as Control
	_list_panel = _fixed_panel(canvas,Rect2(18,214,1568,668))
	_list_panel.name = "TabbedListLayout"
	var sidebar := _fixed_panel(_list_panel,Rect2(9,9,278,649))
	var sidebar_stack := VBoxContainer.new()
	sidebar_stack.name = "FilterSidebar"
	sidebar_stack.position = Vector2(9,10)
	sidebar_stack.size = Vector2(258,630)
	sidebar_stack.add_theme_constant_override("separation",5)
	sidebar.add_child(sidebar_stack)
	var main := _fixed_panel(_list_panel,Rect2(296,9,799,649))
	content_header = _label("",26)
	content_header.position = Vector2(20,11)
	content_header.size = Vector2(482,40)
	main.add_child(content_header)
	var sort := _button("Sort: Name",_sort_items,19)
	sort.position = Vector2(620,9)
	sort.size = Vector2(166,43)
	main.add_child(sort)
	list_scroll = ScrollContainer.new()
	list_scroll.position = Vector2(14,64)
	list_scroll.size = Vector2(770,571)
	list_scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	main.add_child(list_scroll)
	list_container = VBoxContainer.new()
	list_container.add_theme_constant_override("separation",8)
	list_container.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	list_scroll.add_child(list_container)
	desktop_item_grid = GridContainer.new()
	desktop_item_grid.columns = 8
	desktop_item_grid.add_theme_constant_override("h_separation",5)
	desktop_item_grid.add_theme_constant_override("v_separation",5)
	desktop_item_grid.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	list_scroll.add_child(desktop_item_grid)
	detail_panel = _fixed_panel(_list_panel,Rect2(1103,9,455,566))
	detail_panel.name = "DetailPane"
	footer = _fixed_panel(_list_panel,Rect2(1103,582,455,76))
	owned_label = _label("",17)
	owned_label.position = Vector2(13,8)
	owned_label.size = Vector2(235,57)
	footer.add_child(owned_label)
	footer_action = _button("Use",_footer_pressed,21)
	footer_action.position = Vector2(267,12)
	footer_action.size = Vector2(175,50)
	footer.add_child(footer_action)
	footer_action.disabled = true

func _refresh_desktop() -> void:
	for tab in TABS:
		var b: Button = tab_controls[tab]
		b.add_theme_stylebox_override("normal",_frame(tab==active_tab))
	desktop_equipment.visible = active_tab == "Equipment"
	if active_tab == "Equipment":
		if is_instance_valid(_list_panel): _list_panel.hide()
	else:
		if not is_instance_valid(_list_panel): _make_list_layout_desktop()
		_list_panel.show()
		_refresh_sidebar()
		_refresh_desktop_list()
		_render_desktop_list_details()
	var carry: Label = find_child("CarryHeading",true,false)
	if carry != null: carry.text = "Inventory  %.1f / %.0f lb" % [data.weight(),data.capacity()]
	for slot in SLOTS:
		var button: Button = slot_cells[slot]
		var id: String = data.equipped_id(slot)
		button.icon = data.icon(id) if id!="" else null
		button.expand_icon = true
		button.text = "" if id!="" else SIGNS[SLOTS.find(slot)]
		button.add_theme_stylebox_override("normal",_compact_frame(slot==current_slot))
	_update_character_stats()
	if active_tab=="Equipment":
		_render_items()
		_render_desktop_equipment_details()

func _refresh_desktop_list() -> void:
	_replace_children(list_container)
	_replace_children(desktop_item_grid)
	var rows: Array[Dictionary] = []
	var quest_mode := active_tab=="Quest"
	list_container.visible = quest_mode
	desktop_item_grid.visible = not quest_mode
	if quest_mode:
		var finished := quest_kills>=10
		if section_filter in ["All Quests","Completed" if finished else "Side Quests"]:
			rows.append({"id":"goblins","name":"Goblin Menace","count":quest_kills})
		content_header.text = "Quests (%d)" % rows.size()
	else:
		for stack in data.items:
			var id: String = str(stack.id)
			var category: String = data.category(id)
			if active_tab=="Materials" and category!="Materials": continue
			if active_tab=="Items" and category=="Materials": continue
			var item: Dictionary = data.detail(id)
			if not _belongs_filter(item,id): continue
			rows.append({"id":id,"name":str(item.get("name",id)),"count":int(stack.count)})
		content_header.text = "%s  %.1f / %.0f lb" % [active_tab,data.weight(),data.capacity()] if active_tab=="Items" else "Materials (%d)" % rows.size()
		var selection_found := false
		for item in rows:
			if item.id==selection: selection_found=true
		if not selection_found: selection = str(rows[0].id) if not rows.is_empty() else ""
	if quest_mode:
		for entry in rows:
			var selected := str(entry.id)
			var quest := _button("",func():
				quest_selected=selected
				_refresh_desktop_list()
				_render_desktop_list_details(),20)
			quest.custom_minimum_size = Vector2(752,84)
			quest.add_theme_stylebox_override("normal",_frame(selected==quest_selected))
			list_container.add_child(quest)
			var name := _label("⚔  "+str(entry.name),27)
			name.position = Vector2(20,9)
			name.size = Vector2(510,38)
			name.mouse_filter = Control.MOUSE_FILTER_IGNORE
			quest.add_child(name)
			var state := _label("Completed" if quest_kills>=10 else "Side Quest  —  Active",18)
			state.position = Vector2(60,45)
			state.size = Vector2(530,29)
			state.mouse_filter = Control.MOUSE_FILTER_IGNORE
			quest.add_child(state)
			var status := _label("%d / 10" % mini(quest_kills,10),22,HORIZONTAL_ALIGNMENT_RIGHT)
			status.position = Vector2(630,19)
			status.size = Vector2(100,40)
			status.mouse_filter = Control.MOUSE_FILTER_IGNORE
			quest.add_child(status)
		if rows.is_empty():
			var empty := _label("No quests in this category.",24)
			list_container.add_child(empty)
	else:
		desktop_item_grid.columns = 7 if active_tab=="Materials" else 8
		for entry in rows:
			var id: String = str(entry.id)
			var cell := _button("",func():
				selection=id
				data.select(id)
				_refresh_desktop_list()
				_render_desktop_list_details(),17)
			cell.custom_minimum_size = Vector2(102,112) if active_tab=="Materials" else Vector2(88,92)
			cell.clip_contents = true
			cell.tooltip_text = str(entry.name)
			cell.add_theme_stylebox_override("normal",_compact_frame(selection==id))
			desktop_item_grid.add_child(cell)
			var sprite := TextureRect.new()
			sprite.texture = data.icon(id)
			sprite.position = Vector2(22,11)
			sprite.size = Vector2(60,55) if active_tab=="Materials" else Vector2(45,49)
			sprite.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
			sprite.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
			sprite.mouse_filter = Control.MOUSE_FILTER_IGNORE
			cell.add_child(sprite)
			var label_text := str(entry.name).substr(0,15) if active_tab=="Materials" else str(entry.name).substr(0,11)
			var label := _label(label_text,14,HORIZONTAL_ALIGNMENT_CENTER)
			label.position = Vector2(3,76 if active_tab=="Materials" else 66)
			label.size = Vector2(97 if active_tab=="Materials" else 82,19)
			label.mouse_filter = Control.MOUSE_FILTER_IGNORE
			cell.add_child(label)
			var qty := _label("×%d" % int(entry.count),14,HORIZONTAL_ALIGNMENT_RIGHT)
			qty.position = Vector2(50,5)
			qty.size = Vector2(43,23)
			qty.mouse_filter = Control.MOUSE_FILTER_IGNORE
			cell.add_child(qty)
		if rows.is_empty():
			var empty := _label("No materials collected." if active_tab=="Materials" else "No items in this category.",23)
			list_container.visible = true
			list_container.add_child(empty)
		else:
			var columns: int = desktop_item_grid.columns
			for i in range(maxi(0,columns*4-rows.size())):
				var slot := PanelContainer.new()
				slot.custom_minimum_size = Vector2(102,112) if active_tab=="Materials" else Vector2(88,92)
				slot.add_theme_stylebox_override("panel",_compact_frame())
				desktop_item_grid.add_child(slot)

func _equip_selected() -> void:
	if selection.is_empty(): return
	var kind := str(data.detail(selection).get("type",""))
	var slot := current_slot
	if slot.is_empty() or not data.compatible(selection,slot):
		if kind=="Armor": slot="Body"
		elif kind=="Shield": slot="Off Hand"
		elif kind=="Weapon": slot="Main Hand"
		else: return
	if not data.compatible(selection,slot): return
	if data.set_slot(slot,selection):
		current_slot = slot
		_load_model()
		equipment_changed.emit(slot,selection)
		_refresh()

func _render_desktop_equipment_details() -> void:
	_replace_children(details)
	var item: Dictionary = data.detail(selection)
	if item.is_empty():
		details.add_child(_label("Select an inventory item or equipment slot.",23))
		return
	var top := HBoxContainer.new()
	top.custom_minimum_size.y = 127
	top.add_theme_constant_override("separation",15)
	details.add_child(top)
	var picture := TextureRect.new()
	picture.texture = data.icon(selection)
	picture.custom_minimum_size = Vector2(130,126)
	picture.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	picture.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	top.add_child(picture)
	var info := VBoxContainer.new()
	info.custom_minimum_size.x = 440
	top.add_child(info)
	info.add_child(_label(str(item.get("name",selection)),26))
	info.add_child(_label(str(item.get("type","Item")),19))
	info.add_child(_label("Weight: %.1f lb" % float(item.get("weight",0)),19))
	info.add_child(_label("Slot: "+current_slot if current_slot!="" else "Select a slot to equip.",17))
	var compare := VBoxContainer.new()
	compare.custom_minimum_size.x = 258
	top.add_child(compare)
	if compare_visible and current_slot!="":
		var previous: String = data.equipped_id(current_slot)
		var former: Dictionary = data.detail(previous)
		compare.add_child(_label("COMPARE",20))
		compare.add_child(_label("Equipped: "+str(former.get("name","Nothing")),18))
		if not former.is_empty():
			compare.add_child(_label("Weight: %.1f lb" % float(former.get("weight",0)),17))
	else:
		compare.add_child(_label("Properties",19))
		compare.add_child(_label("2014 D&D 5e item",16))
	var actions := HBoxContainer.new()
	actions.add_theme_constant_override("separation",12)
	details.add_child(actions)
	var equip := _button("⚔ Equip",_equip_selected,19)
	equip.custom_minimum_size = Vector2(184,55)
	var item_kind := str(item.get("type",""))
	equip.disabled = item_kind not in ["Armor","Shield","Weapon"] or data.count_of(selection)==0
	actions.add_child(equip)
	var comparison := _button("Compare",func():
		compare_visible = not compare_visible
		_render_desktop_equipment_details(),19)
	comparison.custom_minimum_size = Vector2(184,55)
	comparison.disabled = current_slot==""
	actions.add_child(comparison)
	var drop := _button("Drop",func():pass,19)
	drop.custom_minimum_size = Vector2(184,55)
	drop.disabled = true
	drop.tooltip_text = "World drops and pickup persistence are not implemented yet."
	actions.add_child(drop)
	var inspect := _button("Details",func():
		compare_visible = not compare_visible
		_render_desktop_equipment_details(),19)
	inspect.custom_minimum_size = Vector2(184,55)
	actions.add_child(inspect)

func _render_desktop_list_details() -> void:
	_replace_children(detail_panel)
	owned_label.text = "Weight %.1f / %.0f lb" % [data.weight(),data.capacity()] if active_tab!="Quest" else "Quest journal"
	footer_action.disabled = true
	footer_action.text = "Track" if active_tab=="Quest" else "Use"
	if active_tab=="Quest":
		var visible_quest := section_filter in ["All Quests","Completed" if quest_kills>=10 else "Side Quests"]
		if not visible_quest:
			_desktop_detail_label("No quests in this category.",Vector2(22,28),Vector2(408,92),23)
			return
		_desktop_detail_label("⚔  Goblin Menace",Vector2(28,18),Vector2(400,62),30)
		_desktop_detail_label("Side Quest  —  "+("Completed" if quest_kills>=10 else "Active"),Vector2(28,79),Vector2(399,45),20)
		_desktop_detail_label("The roads near town are threatened by goblins. Defeat ten goblins.",Vector2(28,142),Vector2(395,102),20)
		_desktop_detail_label("OBJECTIVES",Vector2(28,266),Vector2(395,40),24)
		_desktop_detail_label("Defeat 10 goblins   (%d / 10)" % mini(quest_kills,10),Vector2(28,319),Vector2(392,40),20)
		var progress := ProgressBar.new()
		progress.name="QuestProgressBar"
		progress.position = Vector2(28,366)
		progress.size = Vector2(388,29)
		progress.min_value = 0
		progress.max_value = 10
		progress.value = mini(quest_kills,10)
		progress.show_percentage = false
		detail_panel.add_child(progress)
		_desktop_detail_label("Progress is saved automatically.",Vector2(28,422),Vector2(397,68),18)
		footer_action.text = "Tracking" if quest_tracking else "Track quest"
		footer_action.disabled = false
		return
	var item: Dictionary = data.detail(selection)
	if item.is_empty():
		_desktop_detail_label("No items in this category." if active_tab=="Items" else "No materials collected.",Vector2(26,30),Vector2(395,98),23)
		return
	var big := TextureRect.new()
	big.texture = data.icon(selection)
	big.position = Vector2(25,24)
	big.size = Vector2(143,146)
	big.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	big.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	detail_panel.add_child(big)
	_desktop_detail_label(str(item.get("name",selection)),Vector2(187,26),Vector2(243,65),31)
	_desktop_detail_label(str(item.get("type","Item")),Vector2(187,99),Vector2(243,50),19)
	_desktop_detail_label("WEIGHT",Vector2(28,203),Vector2(395,41),22)
	_desktop_detail_label("%.1f lb per item" % float(item.get("weight",0)),Vector2(28,250),Vector2(389,41),20)
	_desktop_detail_label("OWNED",Vector2(28,319),Vector2(390,41),22)
	_desktop_detail_label("Quantity: %d" % data.count_of(selection),Vector2(28,359),Vector2(386,40),20)
	_desktop_detail_label("Additional item properties appear as catalog data becomes available.",Vector2(28,445),Vector2(393,88),18)
	var type := str(item.get("type",""))
	footer_action.text = "Equip" if type in ["Weapon","Armor","Shield"] else "Use"
	footer_action.disabled = type not in ["Weapon","Armor","Shield"]

func _desktop_detail_label(message: String, pos: Vector2, dimensions: Vector2, text_size: int) -> void:
	var label := _label(message,text_size)
	label.position=pos
	label.size=dimensions
	detail_panel.add_child(label)

func _unhandled_input(event: InputEvent) -> void:
	# Inventory remains responsive while gameplay is genuinely tree-paused.
	if event is InputEventKey and event.pressed and not event.echo:
		if event.keycode in [KEY_ESCAPE,KEY_I]:
			closed.emit()
			get_viewport().set_input_as_handled()
		elif event.keycode == KEY_Q:
			_select_tab("Quest")
			get_viewport().set_input_as_handled()
