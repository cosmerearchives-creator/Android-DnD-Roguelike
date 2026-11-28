from PIL import Image
from pathlib import Path
src=Image.open(r'C:\Users\thebl\Downloads\Untit121212led.png').convert('RGBA')
out=Path(r'G:\ChatGPT Projects\Forests of Hell\ForestsOfHellSource\godot\creator_ui')
# Clean connected art components: use complete raw panels from original atlas.
areas={
 'long_button':(29,171,226,237),
 'wide_button':(229,176,482,234),
 'blank_tall_panel':(792,752,1000,1152),
 'slot_neutral':(725,311,806,403),
 'small_panel':(838,309,950,411),
 'horizontal_gold_bar':(724,429,955,471),
 'inventory_grid':(262,717,644,1017),
 'big_frame':(790,755,999,1505),
 'preview_stone':(1063,1712,1234,1785),
}
for key,r in areas.items():
 src.crop(r).save(out/(key+'.png'))
