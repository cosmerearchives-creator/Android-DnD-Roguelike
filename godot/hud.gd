extends Control
signal command(name: String)
signal movement(value: Vector2)
const GOLD = Color("#cfb576")
const INK = Color("#f3dfb8")
const TEAL = Color("#0b292e")
var stick := Vector2.ZERO
var finger := -1
var toggles := {}
var font: Font = ThemeDB.fallback_font
var hud_text := "FORESTS OF HELL"
var combat_status := "HP 20/20   AC 10"
var combat_notice := ""
var buttons := {}
func _ready():
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_process_input(true)
	var bundled := load("res://hud_serif.ttf") as Font
	if bundled != null: font = bundled
func plate(r: Rect2, cut: float, active: bool = false):
	var p := r.position
	var e := r.end
	var pts := PackedVector2Array([p+Vector2(cut,0),Vector2(e.x-cut,p.y),Vector2(e.x,p.y+cut),e-Vector2(0,cut),e-Vector2(cut,0),Vector2(p.x+cut,e.y),Vector2(p.x,e.y-cut),p+Vector2(0,cut)])
	draw_colored_polygon(pts,Color("#364744") if active else TEAL)
	pts.append(pts[0])
	if active:
		for w in [12.0,8.0,5.0]: draw_polyline(pts,Color(1.0,0.67,0.24,0.10),w,true)
	draw_polyline(pts,INK if active else GOLD,3.0 if active else 1.2,true)
	var inner := r.grow(-5)
	var q := inner.position
	var z := inner.end
	var c := maxf(2,cut-3)
	draw_polyline(PackedVector2Array([q+Vector2(c,0),Vector2(z.x-c,q.y),Vector2(z.x,q.y+c),z-Vector2(0,c),z-Vector2(c,0),Vector2(q.x+c,z.y),Vector2(q.x,z.y-c),q+Vector2(0,c),q+Vector2(c,0)]),GOLD.darkened(0.3),1,true)
func label_at(t: String, r: Rect2, px: int):
	draw_string(font,Vector2(r.position.x+(r.size.x-font.get_string_size(t,HORIZONTAL_ALIGNMENT_LEFT,-1,px).x)/2,r.position.y+r.size.y/2+px*0.35),t,HORIZONTAL_ALIGNMENT_LEFT,-1,px,INK)
func diamond(p: Vector2, s: float):
	draw_colored_polygon(PackedVector2Array([p+Vector2(0,-s),p+Vector2(s,0),p+Vector2(0,s),p+Vector2(-s,0)]),INK)
func branch(p: Vector2, angle: float, length: float, depth: int, width: float):
	var end := p+Vector2.from_angle(angle)*length
	draw_line(p,end,INK,width,true)
	if depth <= 0: return
	branch(end,angle-0.52,length*0.67,depth-1,maxf(0.7,width*0.65))
	branch(end,angle+0.58,length*0.63,depth-1,maxf(0.7,width*0.65))
func draw_symbol(key: String, p: Vector2):
	if key == "Jump" or key == "Crouch":
		var crouch := key == "Crouch"
		draw_circle(p+Vector2(9,-19),7,INK)
		var hip := p+Vector2(-5,8 if crouch else 2)
		draw_line(p+Vector2(4,-10),hip,INK,10,true)
		draw_polyline(PackedVector2Array([p+Vector2(4,-9),p+Vector2(17,-1),p+Vector2(24,-10)]),INK,5,true)
		draw_polyline(PackedVector2Array([hip,p+Vector2(13,12),p+Vector2(7,23)]),INK,7,true)
		draw_polyline(PackedVector2Array([hip,p+Vector2(-17,12),p+Vector2(-24,23 if not crouch else 15)]),INK,7,true)
	else:
		for side in ([-1,1] if key == "Attack" else [1]):
			var d := Vector2(side*0.7,-0.7)
			var n := Vector2(-d.y,d.x)
			draw_colored_polygon(PackedVector2Array([p+d*29,p+d*17+n*5,p-d*12+n*5,p-d*12-n*5,p+d*17-n*5]),INK)
			draw_line(p-d*12+n*11,p-d*12-n*11,INK,4,true)
			draw_line(p-d*13,p-d*25,INK,5,true)
func _draw():
	var h := size.y
	var y := h-460
	draw_rect(Rect2(0,y,720,460),TEAL)
	var edge := PackedVector2Array()
	for x in range(0,721,4):
		var dx := absf(x-360.0)
		var lift := 70.0*sqrt(maxf(0.0,1.0-pow(dx/74.0,2.0))) if dx<74 else 0.0
		lift = maxf(lift,28.0*exp(-pow((dx-100.0)/45.0,2.0)))
		edge.append(Vector2(x,y-lift))
	var fill := edge.duplicate()
	fill.append(Vector2(720,y+2))
	fill.append(Vector2(0,y+2))
	draw_colored_polygon(fill,TEAL)
	draw_polyline(edge,GOLD,2,true)
	branch(Vector2(360,y-5),-PI/2,27,4,5)
	for side in [-1,1]:
		diamond(Vector2(360+side*65,y-8),7)
		for i in range(3):
			var t := Vector2(360+side*(88+i*18),y-3)
			draw_colored_polygon(PackedVector2Array([t+Vector2(-10,0),t+Vector2(0,-20-i%2*5),t+Vector2(10,0)]),Color("#42665c"))
	var center := Vector2(145,y+135)
	draw_arc(center,106,0,TAU,80,GOLD,2,true)
	draw_arc(center,98,0,TAU,80,GOLD,1,true)
	for i in range(4): diamond(center+Vector2.RIGHT.rotated(i*PI/2)*106,9)
	draw_circle(center+stick*65,43,Color("#a2d6c5"))
	draw_line(Vector2(280,y+30),Vector2(280,y+245),GOLD,1,true)
	buttons = {"Jump":Rect2(305,y+25,185,100),"Crouch":Rect2(510,y+25,185,100),"Attack":Rect2(305,y+140,185,100),"Attack Mode":Rect2(510,y+140,185,100),"Inventory":Rect2(32,y+305,310,75),"Pause":Rect2(378,y+305,310,75),"Lock":Rect2(420,20,62,60),"A":Rect2(490,20,62,60),"Menu":Rect2(565,20,140,60)}
	var glyphs := {"Jump":"↑","Crouch":"↓","Attack":"⚔","Attack Mode":"↔","Inventory":"▣","Pause":"Ⅱ","Lock":"⊕","A":"A","Menu":"≡ Menu"}
	for key in buttons:
		var r: Rect2 = buttons[key]
		plate(r,15,toggles.get(key,false) or key == "Attack")
		if key in ["Jump","Crouch","Attack","Attack Mode"]:
			draw_symbol(key,r.position+Vector2(r.size.x/2,30))
			label_at("AUTO ON" if key == "Attack Mode" and toggles.get(key,false) else key,
				Rect2(r.position+Vector2(0,52),Vector2(r.size.x,45)),22)
		else:
			label_at(glyphs[key] + ("  "+key if key in ["Inventory","Pause"] else ""),r,24)
	plate(Rect2(16,16,390,84),12)
	draw_string(font,Vector2(30,49),hud_text,HORIZONTAL_ALIGNMENT_LEFT,-1,24,INK)
	draw_line(Vector2(30,58),Vector2(390,58),GOLD,1,true)
	draw_string(font,Vector2(30,85),combat_status,HORIZONTAL_ALIGNMENT_LEFT,-1,21,INK)
	if not combat_notice.is_empty():
		draw_string(font,Vector2(310,y-18),combat_notice,HORIZONTAL_ALIGNMENT_LEFT,-1,20,GOLD)
	draw_line(Vector2(40,y+275),Vector2(680,y+275),GOLD,1,true)
	diamond(Vector2(360,y+275),9)
func _input(event):
	var center := Vector2(145,size.y-325)
	if event is InputEventScreenTouch:
		if event.pressed:
			if event.position.distance_to(center)<125:
				finger = event.index
				stick = ((event.position-center)/65).limit_length()
				movement.emit(stick)
				queue_redraw()
				get_viewport().set_input_as_handled()
			else:
				for key in buttons:
					if buttons[key].has_point(event.position):
						command.emit(key)
						get_viewport().set_input_as_handled()
						break
		elif event.index == finger:
			finger = -1
			stick = Vector2.ZERO
			movement.emit(stick)
			queue_redraw()
			get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index == finger:
		stick = ((event.position-center)/65).limit_length()
		movement.emit(stick)
		queue_redraw()
		get_viewport().set_input_as_handled()
