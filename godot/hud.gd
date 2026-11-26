extends Control
signal command(name: String)
signal movement(value: Vector2)
const GOLD = Color("#cfb576")
const INK = Color("#f3dfb8")
const TEAL = Color("#0b292e")
var stick := Vector2.ZERO
var finger := -1
var toggles := {}
var font := SystemFont.new()
var hud_text := "FORESTS OF HELL"
var buttons := {}
func _ready():
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	font.font_names = PackedStringArray(["Noto Serif", "serif"])
	set_process_input(true)
func plate(r: Rect2, cut: float, active: bool = false):
	var p := r.position
	var e := r.end
	var pts := PackedVector2Array([p+Vector2(cut,0),Vector2(e.x-cut,p.y),Vector2(e.x,p.y+cut),e-Vector2(0,cut),e-Vector2(cut,0),Vector2(p.x+cut,e.y),Vector2(p.x,e.y-cut),p+Vector2(0,cut)])
	draw_colored_polygon(pts,Color("#364744") if active else TEAL)
	pts.append(pts[0])
	draw_polyline(pts,GOLD,3.0 if active else 1.5,true)
	var inner := r.grow(-5)
	var q := inner.position
	var z := inner.end
	var c := maxf(2,cut-3)
	draw_polyline(PackedVector2Array([q+Vector2(c,0),Vector2(z.x-c,q.y),Vector2(z.x,q.y+c),z-Vector2(0,c),z-Vector2(c,0),Vector2(q.x+c,z.y),Vector2(q.x,z.y-c),q+Vector2(0,c),q+Vector2(c,0)]),GOLD.darkened(0.3),1,true)
func label_at(t: String, r: Rect2, px: int):
	draw_string(font,Vector2(r.position.x+(r.size.x-font.get_string_size(t,HORIZONTAL_ALIGNMENT_LEFT,-1,px).x)/2,r.position.y+r.size.y/2+px*0.35),t,HORIZONTAL_ALIGNMENT_LEFT,-1,px,INK)
func diamond(p: Vector2, s: float):
	draw_colored_polygon(PackedVector2Array([p+Vector2(0,-s),p+Vector2(s,0),p+Vector2(0,s),p+Vector2(-s,0)]),INK)
func _draw():
	var h := size.y
	var y := h-430
	draw_rect(Rect2(0,y,720,430),TEAL)
	draw_arc(Vector2(360,y),64,PI,TAU,40,GOLD,2,true)
	draw_line(Vector2(0,y),Vector2(296,y),GOLD,2,true)
	draw_line(Vector2(424,y),Vector2(720,y),GOLD,2,true)
	draw_line(Vector2(360,y-12),Vector2(360,y-48),GOLD,3,true)
	for i in range(5):
		var d := float(i+1)*5
		draw_line(Vector2(360,y-15-d),Vector2(360-d,y-26-d),GOLD,1.5,true)
		draw_line(Vector2(360,y-15-d),Vector2(360+d,y-26-d),GOLD,1.5,true)
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
		plate(r,12,toggles.get(key,false))
		if key in ["Jump","Crouch","Attack","Attack Mode"]:
			label_at(glyphs[key],Rect2(r.position,r.size*Vector2(1,0.55)),36)
			label_at(key,Rect2(r.position+Vector2(0,52),Vector2(r.size.x,45)),22)
		else:
			label_at(glyphs[key] + ("  "+key if key in ["Inventory","Pause"] else ""),r,24)
	plate(Rect2(16,16,390,84),12)
	label_at(hud_text,Rect2(20,18,382,40),22)
	draw_line(Vector2(30,58),Vector2(390,58),GOLD,1,true)
	label_at("HP —   XP —   GOLD —",Rect2(20,60,382,35),20)
	draw_line(Vector2(40,y+275),Vector2(680,y+275),GOLD,1,true)
	diamond(Vector2(360,y+275),9)
func _input(event):
	var center := Vector2(145,size.y-295)
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
