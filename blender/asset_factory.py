"""Deterministic Blender prop factory. Run headless with -- --category trees --output build/assets."""
import bpy, math, random, sys, os
from pathlib import Path
from mathutils import Vector
args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
def option(key,default):
    return args[args.index(key)+1] if key in args else default
category=option("--category","trees")
out=Path(option("--output","build/asset-factory"));out.mkdir(parents=True,exist_ok=True)
assert category in ("trees","bushes","rocks","ruins")
count=max(1,min(24,int(option("--count","12"))))
def material(name,rgb):
    m=bpy.data.materials.new(name);m.diffuse_color=(*rgb,1)
    m.use_nodes=True;m.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value=(*rgb,1)
    m.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value=.88
    return m
bark=material("bark",(.22,.13,.075));leaf=[material("foliage"+str(i),c) for i,c in enumerate(((.12,.25,.13),(.19,.34,.16),(.26,.37,.19),(.13,.28,.23)))]
stone=[material("stone"+str(i),c) for i,c in enumerate(((.28,.30,.29),(.39,.40,.36),(.23,.27,.27)))]
moss=material("moss",(.18,.30,.15));flower=material("flowers",(.65,.45,.23))
def mesh(name,loc,scale,mat,segments=12):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=8,location=loc)
    o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(mat)
    for f in o.data.polygons:f.use_smooth=True
    return o
def cylinder(name,loc,r,depth,mat,vertices=10):
    bpy.ops.mesh.primitive_cone_add(vertices=vertices,radius1=r,radius2=r*.75,depth=depth,location=loc)
    o=bpy.context.object;o.name=name;o.data.materials.append(mat);return o
bpy.ops.object.light_add(type="AREA",location=(3,-4,8));bpy.context.object.data.energy=750;bpy.context.object.data.size=5
bpy.ops.object.camera_add(location=(5,-7,6));camera=bpy.context.object
camera.rotation_euler=(Vector((0,0,1))-camera.location).to_track_quat("-Z","Y").to_euler()
camera.data.type="ORTHO";camera.data.ortho_scale=5
scene=bpy.context.scene;scene.camera=camera;scene.render.engine="BLENDER_EEVEE"
scene.render.resolution_x=256;scene.render.resolution_y=256;scene.render.resolution_percentage=100
scene.render.film_transparent=True;scene.render.image_settings.file_format="PNG"
scene.view_settings.view_transform="Standard"
for i in range(count):
    for obj in list(bpy.data.objects):
        if obj.type=="MESH":bpy.data.objects.remove(obj,do_unlink=True)
    rng=random.Random(6101+ i*719 + sum(map(ord,category))*101)
    if category=="trees":
        h=rng.uniform(1.5,3.3);cylinder("trunk",(0,0,h*.45),rng.uniform(.12,.23),h*.9,bark)
        for j in range(rng.randint(5,10)):
            a=rng.uniform(0,math.tau);r=rng.uniform(.1,.65)
            mesh("leaf cluster",(math.cos(a)*r,math.sin(a)*r,h*rng.uniform(.65,1.03)),
                 (rng.uniform(.35,.65),rng.uniform(.35,.65),rng.uniform(.35,.7)),rng.choice(leaf))
    elif category=="bushes":
        for j in range(rng.randint(4,11)):
            a=rng.uniform(0,math.tau);r=rng.uniform(0,.65)
            mesh("shrub",(math.cos(a)*r,math.sin(a)*r,rng.uniform(.2,.6)),
                 (rng.uniform(.2,.43),rng.uniform(.2,.43),rng.uniform(.22,.5)),rng.choice(leaf))
        if i%3==0:
            for j in range(4):mesh("flowers",(rng.uniform(-.6,.6),rng.uniform(-.6,.6),rng.uniform(.35,.8)),(.055,.055,.055),flower)
    elif category=="rocks":
        for j in range(rng.randint(1,5)):
            mesh("rock",(rng.uniform(-.55,.55),rng.uniform(-.45,.45),rng.uniform(.15,.35)),
                 (rng.uniform(.2,.6),rng.uniform(.2,.5),rng.uniform(.2,.65)),rng.choice(stone),8)
        if i%2==0:mesh("moss cap",(0,0,.5),(.4,.4,.05),moss)
    else:
        for j in range(rng.randint(2,6)):
            x=rng.uniform(-.6,.6);y=rng.uniform(-.5,.5)
            cylinder("ruin fragment",(x,y,rng.uniform(.25,.7)),rng.uniform(.13,.32),rng.uniform(.4,1.1),rng.choice(stone),6)
        if i%2==0:mesh("moss",(0,0,.3),(.4,.4,.09),moss)
    scene.render.filepath=str((out/("%s_%02d.png"%(category,i))).resolve())
    bpy.ops.render.render(write_still=True)
print("ASSET_FACTORY_COMPLETE",category,count)
