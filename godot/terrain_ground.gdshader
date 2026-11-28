shader_type spatial;
uniform sampler2D patches_color : source_color, filter_linear_mipmap, repeat_disable;
uniform sampler2D patches_normal : hint_normal, filter_linear_mipmap, repeat_disable;
uniform sampler2D patches_orm : filter_linear_mipmap, repeat_disable;
uniform sampler2D meadow : source_color, filter_linear_mipmap, repeat_enable;
uniform sampler2D biome_map : filter_nearest, repeat_disable;
uniform vec2 map_origin;
uniform float cell_size;
uniform float blend_inches = 6.0;
varying vec2 ground;
void vertex(){ground=(MODEL_MATRIX*vec4(VERTEX,1.0)).xz;}
float hash2(vec2 p){return fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453);}
float noise2(vec2 p){
	vec2 i=floor(p), f=fract(p);f=f*f*(3.0-2.0*f);
	return mix(mix(hash2(i),hash2(i+vec2(1,0)),f.x),mix(hash2(i+vec2(0,1)),hash2(i+1.0),f.x),f.y);
}
int biome(ivec2 cell){
	ivec2 at=cell-ivec2(map_origin);
	if(any(lessThan(at,ivec2(0))) || any(greaterThanEqual(at,textureSize(biome_map,0)))) return 0;
	return int(round(texelFetch(biome_map,at,0).r*255.0));
}
// Rows carry linear color, tangent-space slope and material roughness/metallic.
mat3 sample_ground(int id, vec2 uv, vec2 dx, vec2 dy){
	if(id==0){
		vec3 col=textureGrad(meadow,ground*.145,dx*.145,dy*.145).rgb;
		return mat3(mix(col*.62,vec3(.29,.27,.20),.34),vec3(0),vec3(.95,0,0));
	}
	int index=id-1;
	vec2 tile=vec2(float(index%3),float(index/3));
	vec2 p=uv+float(id)*.173;
	vec2 flip=1.0-2.0*mod(floor(p),2.0);
	vec2 mirrored=mix(fract(p),1.0-fract(p),.5-.5*flip);
	vec2 scale=vec2(1.0/3.0,.5);
	vec2 st=(tile+mix(vec2(.004),vec2(.996),mirrored))*scale;
	vec2 gx=dx*.55*flip*scale,gy=dy*.55*flip*scale;
	vec3 c=textureGrad(patches_color,st,gx,gy).rgb;
	if(id==2) c*=vec3(.69,.59,.48);
	if(id==3) c*=vec3(.72,.65,.52);
	vec2 slope=(textureGrad(patches_normal,st,gx,gy).rg*2.0-1.0)*flip;
	vec3 orm=textureGrad(patches_orm,st,gx,gy).rgb;
	return mat3(c,vec3(slope,0),vec3(clamp(orm.g,.08,1.0),orm.b,0));
}
void fragment(){
	vec2 dx=dFdx(ground),dy=dFdy(ground);
	vec2 warp=vec2(noise2(ground*.72),noise2(ground*.83+13.7))-.5;
	vec2 uv=ground*.55+.24*warp;
	float width=clamp(blend_inches*.0254/cell_size,0.0,.5);
	vec2 grid=ground/cell_size+warp*width*.75;
	ivec2 cell=ivec2(floor(grid));vec2 f=fract(grid);
	vec2 w=smoothstep(vec2(.5-max(width,.00001)),vec2(.5+max(width,.00001)),f);
	int a=biome(cell),b=biome(cell+ivec2(1,0)),c=biome(cell+ivec2(0,1)),d=biome(cell+ivec2(1,1));
	mat3 A=sample_ground(a,uv,dx,dy);
	mat3 B=A,C=A,D=A;
	if(b!=a && w.x>0.0) B=sample_ground(b,uv,dx,dy);
	if(c!=a && w.y>0.0) C=sample_ground(c,uv,dx,dy);
	if(d!=a && w.x*w.y>0.0) D=sample_ground(d,uv,dx,dy);
	mat3 m=(A*(1.0-w.x)+B*w.x)*(1.0-w.y)+(C*(1.0-w.x)+D*w.x)*w.y;
	ALBEDO=m[0];ROUGHNESS=m[2].x;METALLIC=m[2].y;
	NORMAL=normalize(mat3(VIEW_MATRIX)*vec3(m[1].x,1.0,-m[1].y));
}
