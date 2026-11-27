extends Control
## Native fidelity preview and strict anatomy review for our 46-bone wolf reference.
const SHARED = preload("res://quadruped_shared_animations.gd")
const GOLD := Color("#cfb576")
const PALE := Color("#f3dfb8")
const SERIF = preload("res://hud_serif.ttf")
const TARGETS := [
    {"name": "Wolf (46-bone quadruped reference)", "asset": "res://assets/wolf_quadruped.glb"}
]
var viewport: SubViewport
var model_root: Node3D
var model: Node3D
var camera: Camera3D
var shared_player: AnimationPlayer
var imported_player: AnimationPlayer
var model_picker: OptionButton
var clip_picker: OptionButton
var status: Label
var dragging := false
var touch_positions := {}

func _panel() -> StyleBoxFlat:
    var style := StyleBoxFlat.new()
    style.bg_color = Color(0.035, 0.11, 0.13, 0.94)
    style.border_color = GOLD
    style.set_border_width_all(2)
    style.set_corner_radius_all(12)
    style.set_content_margin_all(12)
    return style

func _text(value: String, size := 22) -> Label:
    var label := Label.new()
    label.text = value
    label.add_theme_font_override("font", SERIF)
    label.add_theme_font_size_override("font_size", size)
    label.add_theme_color_override("font_color", PALE)
    label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    return label

func _button(value: String, callback: Callable) -> Button:
    var button := Button.new()
    button.text = value
    button.custom_minimum_size.y = 62
    button.add_theme_font_override("font", SERIF)
    button.add_theme_font_size_override("font_size", 24)
    button.add_theme_color_override("font_color", PALE)
    button.add_theme_stylebox_override("normal", _panel())
    button.pressed.connect(callback)
    return button

func _ready() -> void:
    var background := TextureRect.new()
    background.texture = preload("res://menu_camp.webp")
    background.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
    background.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
    add_child(background)
    background.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    var margins := MarginContainer.new()
    add_child(margins)
    margins.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    for side in ["left", "right", "top", "bottom"]:
        margins.add_theme_constant_override("margin_" + side, 20)
    var layout := VBoxContainer.new()
    layout.add_theme_constant_override("separation", 12)
    margins.add_child(layout)
    var back := _button("‹ Back to Main Menu", func(): get_tree().change_scene_to_file("res://front_menu.tscn"))
    layout.add_child(back)
    var heading := _text("QUADRUPED ANIMATION LAB", 34)
    heading.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    layout.add_child(heading)
    var description := _text("Native wolf quadruped library • 46 source bones • 4 source animations", 20)
    description.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    layout.add_child(description)
    model_picker = OptionButton.new()
    model_picker.custom_minimum_size.y = 60
    model_picker.add_theme_font_override("font", SERIF)
    model_picker.add_theme_font_size_override("font_size", 24)
    for target in TARGETS:
        model_picker.add_item(target.name)
    model_picker.item_selected.connect(_load_target)
    layout.add_child(model_picker)
    clip_picker = OptionButton.new()
    clip_picker.custom_minimum_size.y = 60
    clip_picker.add_theme_font_override("font", SERIF)
    clip_picker.add_theme_font_size_override("font_size", 22)
    var names := _all_clips()
    for clip_name in names:
        var humanized: String = clip_name.replace("_", " ").capitalize()
        clip_picker.add_item(humanized)
        clip_picker.set_item_metadata(clip_picker.item_count - 1, clip_name)
    clip_picker.item_selected.connect(_play_clip)
    layout.add_child(clip_picker)
    var panel := PanelContainer.new()
    panel.size_flags_vertical = Control.SIZE_EXPAND_FILL
    panel.custom_minimum_size.y = 370
    panel.add_theme_stylebox_override("panel", _panel())
    layout.add_child(panel)
    var holder := SubViewportContainer.new()
    holder.stretch = true
    holder.mouse_filter = Control.MOUSE_FILTER_STOP
    panel.add_child(holder)
    viewport = SubViewport.new()
    viewport.size = Vector2i(600, 700)
    viewport.transparent_bg = true
    viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
    holder.add_child(viewport)
    var scene := Node3D.new()
    viewport.add_child(scene)
    model_root = Node3D.new()
    scene.add_child(model_root)
    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-35, -35, 0)
    sun.light_energy = 2.0
    scene.add_child(sun)
    var fill := OmniLight3D.new()
    fill.position = Vector3(-2, 2, 2)
    fill.light_energy = 1.2
    scene.add_child(fill)
    camera = Camera3D.new()
    camera.current = true
    camera.fov = 45
    scene.add_child(camera)
    holder.gui_input.connect(_preview_input)
    var actions := HBoxContainer.new()
    layout.add_child(actions)
    var replay := _button("Replay", func(): _play_clip(clip_picker.selected))
    replay.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    actions.add_child(replay)
    var stop := _button("Stop", func():
        if shared_player: shared_player.stop()
    )
    stop.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    actions.add_child(stop)
    status = _text("Loading shared library…", 20)
    status.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    layout.add_child(status)
    layout.add_child(_text("Drag to rotate • Scroll or pinch to zoom • Wolf uses original unmodified animation tracks", 18))
    _load_target(0)
    if clip_picker.item_count > 0:
        var initial := _index_of_clip("Idle_Standing_01_Standard")
        clip_picker.select(initial)
        _play_clip(initial)

func _all_clips() -> PackedStringArray:
    return PackedStringArray([
        "Idle_Standing_01_Standard",
        "Movement_Run_01_Standard",
        "Action_RollOver_01_Standard",
        "Attack_Pounce_01_Standard"
    ])

func _index_of_clip(name: String) -> int:
    for index in clip_picker.item_count:
        if clip_picker.get_item_metadata(index) == name:
            return index
    return 0

func _load_target(index: int) -> void:
    var target: Dictionary = TARGETS[index]
    if is_instance_valid(model):
        model_root.remove_child(model)
        model.queue_free()
    model = load(target.asset).instantiate()
    model_root.add_child(model)
    imported_player = SHARED.find_animator(model)
    shared_player = SHARED.attach(model)
    if imported_player: imported_player.stop()
    var bounds := _bounds_for(model, Transform3D.IDENTITY)
    if bounds.size.length() > 0.01:
        model.position = -bounds.get_center()
        var diameter := maxf(maxf(bounds.size.x, bounds.size.z), bounds.size.y)
        camera.position = Vector3(0, 0, maxf(diameter * 1.7, 1))
        camera.look_at(Vector3.ZERO)
        camera.near = 0.02
        camera.far = 500
    if shared_player:
        status.text = "%s • %d mapped bones • %d available clips" % [target.name, shared_player.get_meta("mapped_bones"), shared_player.get_animation_list().size()]
        if clip_picker and clip_picker.item_count > 0:
            _play_clip(clip_picker.selected)
    else:
        status.text = "Incompatible quadruped skeleton; requires exact 46-bone reference and rest pose"

func _play_clip(index: int) -> void:
    if not shared_player or index < 0 or index >= clip_picker.item_count:
        return
    if imported_player: imported_player.stop()
    var clip: String = clip_picker.get_item_metadata(index)
    shared_player.stop()
    shared_player.play(clip)
    shared_player.advance(0)

func _bounds_for(node: Node3D, parent_transform: Transform3D) -> AABB:
    var transform := parent_transform * node.transform
    var bounds := AABB()
    var present := false
    if node is MeshInstance3D and node.mesh:
        for i in 8:
            var vertex: Vector3 = transform * node.get_aabb().get_endpoint(i)
            bounds = bounds.expand(vertex) if present else AABB(vertex, Vector3.ZERO)
            present = true
    for child in node.get_children():
        if child is Node3D:
            var found := _bounds_for(child, transform)
            if found.size.length() > 0.0001:
                bounds = bounds.merge(found) if present else found
                present = true
    return bounds

func _preview_input(event: InputEvent) -> void:
    if event is InputEventMouseButton:
        if event.button_index == MOUSE_BUTTON_LEFT:
            dragging = event.pressed
        if event.pressed and event.button_index == MOUSE_BUTTON_WHEEL_UP:
            camera.position.z = maxf(camera.position.z * 0.9, 0.4)
        elif event.pressed and event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
            camera.position.z = minf(camera.position.z * 1.1, 40.0)
    elif event is InputEventMouseMotion and dragging:
        model_root.rotation.y += event.relative.x * 0.012
    elif event is InputEventScreenTouch:
        if event.pressed: touch_positions[event.index] = event.position
        else: touch_positions.erase(event.index)
    elif event is InputEventScreenDrag:
        if touch_positions.size() > 1 and touch_positions.has(event.index):
            var before: Array = touch_positions.values()
            var old_distance: float = before[0].distance_to(before[1])
            touch_positions[event.index] = event.position
            var after: Array = touch_positions.values()
            var new_distance: float = after[0].distance_to(after[1])
            if old_distance > 1 and new_distance > 1:
                camera.position.z = clampf(camera.position.z * old_distance / new_distance, 0.4, 40.0)
        else:
            touch_positions[event.index] = event.position
            model_root.rotation.y += event.relative.x * 0.012
