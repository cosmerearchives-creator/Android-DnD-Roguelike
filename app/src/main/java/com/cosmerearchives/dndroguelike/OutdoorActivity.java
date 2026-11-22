package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import java.util.Locale;

/**
 * Overhead outdoor exploration on a screen-aligned grid.
 * Pure world-coordinate generation: tile (x,y) always produces the same terrain.
 */
public final class OutdoorActivity extends Activity {
    static final String EXTRA_FREE_EXPLORE = "free_explore";
    private Landscape landscape;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        PlayerCharacter hero=PlayerCharacter.loadCompleted(this);
        if(hero==null&&!getIntent().getBooleanExtra(EXTRA_FREE_EXPLORE,false)){
            startActivity(new Intent(this,CharacterCreatorActivity.class));
            finish();
            return;
        }
        ElfAppearance appearance=state!=null
            ? ElfAppearance.fromId(state.getString("worldAppearance"))
            : hero==null ? ElfAppearance.CLASSIC : hero.appearance;
        landscape=new Landscape(this,hero,appearance);
        setContentView(landscape);
        hideBars();
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        if(landscape!=null)state.putString("worldAppearance",landscape.appearance.id);
    }

    private void hideBars(){
        if(Build.VERSION.SDK_INT>=30){
            WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null){
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsets.Type.systemBars());
            }
        }else{
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);if(focused)hideBars();
    }
    @Override public void onBackPressed(){
        if(landscape!=null) landscape.setPaused(!landscape.paused);
        else super.onBackPressed();
    }

    static final class Landscape extends View {
        private static final float TILE_SIZE=64f, LEVEL_H=20f;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint groundPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Bitmap groundAtlas;
        private static final int WATER_TILE = 10;
        private static final int BRIDGE_TILE = 11;
        private static final int SCENERY_SCALE = 2;
        private static final int PROP_HALF_WIDTH = 64;
        private static final int PROP_ABOVE_FOOT = 140;
        private static final int PROP_BELOW_FOOT = 12;
        private final android.util.LruCache<Long, Bitmap> groundAccents = new android.util.LruCache<>(128);
        private final Bitmap[] propSprites = new Bitmap[5];
        private final Bitmap vegetationAtlas;
        private final Rect tileSource=new Rect();
        private final RectF tileDestination=new RectF();
        private final Path path=new Path();
        private final Typeface hudTypeface = Typeface.create("sans-serif", Typeface.BOLD);
        // The viewport spans fewer than 64 tiles on each axis. Reuse a bounded
        // window of generated properties as the camera moves.
        private static final int TILE_CACHE_SIDE = 64;
        private final Tile[] tileCache = new Tile[TILE_CACHE_SIDE * TILE_CACHE_SIDE];

        private static final class Tile {
            int x, y, level, kind, art, prop;
            boolean road, river;
            long terrainSeed, propSeed;
        }
        private final float movementFactor;
        private final RiggedElf riggedArt;
        private final GaitModel gait=new GaitModel();
        private ElfAppearance appearance;
        private final RectF modelToggleBounds=new RectF();
        private final RectF viewToggleBounds=new RectF();
        private boolean closeUp, shoulderBinds;
        private final RectF shoulderBindBounds=new RectF();
        private final String heroName;
        private float x=0.5f,y=0.5f,camX=x,camY=y,targetX=x,targetY=y;
        private float joyX,joyY,knobX,knobY;
        private static final int NO_POINTER = -1;
        // One five-foot square per second, or six squares per six-second round.
        private static final float WALK_TILES_PER_SECOND = 1f;
        private static final float RUN_MULTIPLIER = 2f;
        private int joystickPointer=NO_POINTER;
        private boolean runEnabled;
        private boolean moving=false,paused=false;
        private final RectF resumeBounds=new RectF(),menuBounds=new RectF(),quitBounds=new RectF();
        private final Context appContext;
        private int facing=0;
        private float walkTime=0;
        private float idleTime=0;
        private boolean visuallyWalking=false;
        private float gaitBlend=0f;
        private static final long ACTION_POINT_REFILL_MS = 6000L;
        private long actionPointReadyAt;
        private float attackRemaining=0f;
        private float punchX,punchY;
        private int punchLevel;
        private final RectF attackBounds=new RectF(), runBounds=new RectF();
        private long lastFrame;
        private static final float WORLD_RIG_SCALE=.86f;
        private static final float WORLD_COMPARISON_ZOOM=1f;
        private float zoom=1.8f; // Approved close camera for illustrated character detail.
        private int controlsBottom=0;
        private int lastVisitedX=Integer.MIN_VALUE,lastVisitedY=Integer.MIN_VALUE;
        private int discovered=0;
        private final java.util.HashSet<Long> seen=new java.util.HashSet<>();
        private int[] colorRange={0xFF526D47,0xFF425E41,0xFF345044,0xFF807B55,0xFF66766C,0xFF33697A};
        private final float density;
        Landscape(Context context,PlayerCharacter hero,ElfAppearance appearance){
            super(context);appContext=context;
            movementFactor=hero==null ? 1f : hero.loadout().movementFactor();
            Bitmap atlas=null;
            try(java.io.InputStream input=context.getAssets().open("wildlands_terrain_v031.png")){
                atlas=BitmapFactory.decodeStream(input);
            }catch(java.io.IOException ignored){ }
            groundAtlas=atlas;
            Bitmap props=null;
            try(java.io.InputStream input=context.getAssets().open("wildlands_vegetation.webp")){
                props=BitmapFactory.decodeStream(input);
            }catch(java.io.IOException ignored){ }
            vegetationAtlas=props;
            this.appearance=appearance;
            riggedArt=new RiggedElf(context,appearance);
            heroName=hero==null ? "Explorer" : hero.name;
            android.content.SharedPreferences saved=context.getSharedPreferences("wildlands_world",Context.MODE_PRIVATE);
            x=saved.getFloat("player_x",0.5f);
            y=saved.getFloat("player_y",0.5f);
            if(!Float.isFinite(x)||!Float.isFinite(y)||Math.abs(x)>1000000f||Math.abs(y)>1000000f){x=.5f;y=.5f;}
            camX=x;camY=y;targetX=x;targetY=y;
            density=getResources().getDisplayMetrics().density;
            setFocusable(true);
            if (Build.VERSION.SDK_INT < 28) setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setOnApplyWindowInsetsListener((v,insets)->{
                int bottom=Build.VERSION.SDK_INT>=30?
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom:insets.getSystemWindowInsetBottom();
                controlsBottom=bottom;invalidate();return insets;
            });
        }
        private long hash(int xx,int yy,int salt){
            long z=((long)xx*0x9e3779b97f4a7c15L)^((long)yy*0xbf58476d1ce4e5b9L)^(salt*0x94d049bb133111ebL);
            z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
            z=(z^(z>>>27))*0x94d049bb133111ebL;
            return z^(z>>>31);
        }
        private float noise(int xx,int yy,int salt){return (hash(xx,yy,salt)>>>11)/(float)(1L<<53);}
        private float smooth(float v){return v*v*(3-2*v);}
        private float field(float xx,float yy,int scale,int salt){
            float px=xx/scale,py=yy/scale;
            int ix=(int)Math.floor(px),iy=(int)Math.floor(py);
            float a=smooth(px-ix),b=smooth(py-iy);
            float v0=noise(ix,iy,salt)*(1-a)+noise(ix+1,iy,salt)*a;
            float v1=noise(ix,iy+1,salt)*(1-a)+noise(ix+1,iy+1,salt)*a;
            return v0*(1-b)+v1*b;
        }
        private boolean generateRiver(int xx,int yy){
            if(Math.abs(xx)<7&&Math.abs(yy)<7)return false;
            float meander=(field(yy,0,17,51)-.5f)*13f;
            return Math.abs(xx-(23+meander))<1.5f;
        }
        private boolean generateRoad(int xx,int yy){
            if(Math.abs(xx)<7&&Math.abs(yy)<7)return yy==0;
            float bend=(field(xx,0,23,19)-.5f)*7f;
            return Math.abs(yy-bend)<1.25f;
        }
        private int generateHeight(int xx,int yy){
            if(Math.abs(xx)<8&&Math.abs(yy)<8)return 0;
            float level=field(xx,yy,16,37)*1.15f+field(xx,yy,7,39)*.35f;
            return level>.93f?2:level>.58f?1:0;
        }
        private int generateBiome(int xx,int yy,int level,boolean river){
            if(river)return 5;
            if(level==2)return 4;
            float wet=field(xx,yy,29,12),temp=field(xx+300,yy-600,31,17);
            if(wet>.7f)return 2;
            if(wet<.36f)return 3;
            return temp>.55f?0:1;
        }
        private Tile tileAt(int xx, int yy) {
            int mask = TILE_CACHE_SIDE - 1;
            int index = (xx & mask) * TILE_CACHE_SIDE + (yy & mask);
            Tile tile = tileCache[index];
            if (tile != null && tile.x == xx && tile.y == yy) return tile;
            if (tile == null) tileCache[index] = tile = new Tile();
            tile.x = xx;
            tile.y = yy;
            tile.level = generateHeight(xx, yy);
            tile.river = generateRiver(xx, yy);
            tile.road = generateRoad(xx, yy);
            tile.kind = generateBiome(xx, yy, tile.level, tile.river);
            tile.terrainSeed = hash(xx, yy, 27);
            tile.propSeed = hash(xx, yy, 83);
            tile.art = tileArt(tile);
            tile.prop = propKind(tile);
            return tile;
        }

        private int height(int xx, int yy) { return tileAt(xx, yy).level; }

        private boolean blocked(Tile tile) {
            if (tile.road) return false;
            if (tile.river) return true;
            if (Math.abs(tile.x)<6 && Math.abs(tile.y)<6) return false;
            boolean tree = (tile.propSeed&31)==4 && (tile.kind==1 || tile.kind==2);
            boolean rock = (tile.propSeed&63)==15 && tile.level>0;
            return tree || rock;
        }

        private boolean walkable(float xx,float yy){
            int fromH = height((int)Math.floor(x),(int)Math.floor(y));
            Tile tile = tileAt((int)Math.floor(xx), (int)Math.floor(yy));
            return !blocked(tile) && Math.abs(tile.level - fromH)<=1;
        }
        private float screenX(float worldX){return worldX*TILE_SIZE;}
        private float screenY(float worldY,int level){return worldY*TILE_SIZE-level*LEVEL_H;}
        private int gridIndex(float coordinate){return (int)Math.floor(coordinate);}
        private float originX(){return getWidth()*.5f-screenX(camX)*zoom;}
        private float originY(){
            return (getHeight()-controlsBottom)*.48f
                -screenY(camY,height(gridIndex(camX),gridIndex(camY)))*zoom;
        }
        private void targetTerrain(float sx,float sy){
            float worldX=(sx-originX())/(zoom*TILE_SIZE);
            float projectedY=(sy-originY())/zoom;
            int tx=gridIndex(worldX);
            int first=gridIndex(projectedY/TILE_SIZE);
            // A raised row can reach into the preceding row. Check the
            // foreground first, just as the painter draws it last.
            int last=gridIndex((projectedY+2*LEVEL_H)/TILE_SIZE);
            for(int ty=last;ty>=first;ty--){
                int level=height(tx,ty);
                float top=screenY(ty,level);
                int nextLevel=height(tx,ty+1);
                float bottom=screenY(ty+1,Math.min(level,nextLevel));
                if(projectedY>=top && projectedY<bottom){
                    targetX=worldX;
                    float localY=(projectedY-top)/TILE_SIZE;
                    targetY=ty+Math.min(.98f,localY);
                    moving=true;
                    return;
                }
            }
        }
        private void polygon(Canvas c,int color,float... xy){
            path.reset();path.moveTo(xy[0],xy[1]);
            for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);
            path.close();p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);
        }
        private void line(Canvas c,int color,float width,float x1,float y1,float x2,float y2){
            p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
        }
        private void oval(Canvas c,int color,float a,float b,float cc,float d){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(a,b,cc,d,p);
        }
        private void rect(Canvas c,int color,float a,float b,float cc,float d){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(a,b,cc,d,p);
        }
        private void rounded(Canvas c,int fill,float x1,float y1,float x2,float y2,float radius){
            p.setColor(fill);p.setStyle(Paint.Style.FILL);p.setShader(null);
            c.drawRoundRect(x1,y1,x2,y2,radius,radius,p);
        }
        private void text(Canvas c,String s,float xx,float yy,float size,int color){
            p.setColor(color);p.setTypeface(hudTypeface);
            p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,xx,yy,p);
        }
        private int shade(int color,int delta){
            int r=Math.max(0,Math.min(255,Color.red(color)+delta));
            int g=Math.max(0,Math.min(255,Color.green(color)+delta));
            int b=Math.max(0,Math.min(255,Color.blue(color)+delta));
            return Color.rgb(r,g,b);
        }
        private void groundTexture(Canvas c,int id,float left,float top,float right,float bottom){
            if(groundAtlas==null)return;
            int size=groundAtlas.getWidth()/4;
            tileSource.set((id%4)*size,(id/4)*size,(id%4+1)*size,(id/4+1)*size);
            tileDestination.set(left,top,right,bottom);
            c.drawBitmap(groundAtlas,tileSource,tileDestination,groundPaint);
        }
        private int tileArt(Tile tile){
            int kind = tile.kind;
            long seed = tile.terrainSeed;
            if(tile.road && tile.river)return BRIDGE_TILE;
            if(kind==5)return WATER_TILE;
            float variation=field(tile.x,tile.y,8,67);
            if(tile.road){
                if(Math.abs(tile.x)>=8 && variation>.68f)return tile.level==2?6:9;
                return (seed&3)==0?5:4;
            }
            if(kind==4){
                if(variation>.69f)return 7;
                if(variation<.31f)return 15;
                return 8;
            }
            if(kind==3)return 3;
            if(kind==2){
                if(variation>.65f)return 14;
                return (seed&3)==0?12:2;
            }
            if(kind==1){
                if(variation<.30f)return 13;
                return (seed&3)==0?1:0;
            }
            return (seed&7)==0?12:0;
        }

        private void terrain(Canvas c, Tile tile, float sx, float sy){
            int level = tile.level, kind = tile.kind;
            int base=colorRange[kind];
            if(tile.road && !tile.river)base=0xFF866D4C;
            if(tile.road && tile.river)base=0xFF8F7454;
            float left=sx-TILE_SIZE*.5f, right=left+TILE_SIZE;
            float top=sy-TILE_SIZE*.5f, bottom=top+TILE_SIZE;
            int south=height(tile.x,tile.y+1);
            if(level>south){
                float baseY=bottom+(level-south)*LEVEL_H;
                if(groundAtlas==null)rect(c,0xFF40463B,left,bottom,right,baseY);
                else groundTexture(c,8,left,bottom,right,baseY);
                rect(c,0x55402018,left,bottom,right,baseY);
            }
            long seed=tile.terrainSeed;
            if(kind!=5 && (seed&7)==3)base=shade(base,-9);
            if(groundAtlas==null)rect(c,base,left,top,right,bottom);
            groundTexture(c,tile.art,left,top,right,bottom);
            long position=((long)tile.x<<32)^(tile.y&0xffffffffL);
            Bitmap accents=groundAccents.get(position);
            if(accents==null){
                int size=(int)TILE_SIZE*SCENERY_SCALE;
                accents=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
                Canvas surface=new Canvas(accents);
                surface.scale(SCENERY_SCALE,SCENERY_SCALE);
                surface.translate(TILE_SIZE*.5f,TILE_SIZE*.5f);
                drawGroundAccents(surface,tile);
                groundAccents.put(position,accents);
            }
            tileDestination.set(left,top,right,bottom);
            c.drawBitmap(accents,null,tileDestination,groundPaint);
        }

        private void drawGroundAccents(Canvas c, Tile tile){
            float sx=0, sy=0;
            int kind=tile.kind;
            long seed=tile.terrainSeed;
            if(kind!=5 && !tile.road){
                polygon(c,0x224C8550,sx-24,sy-2,sx-4,sy-13,sx+17,sy-1,sx+4,sy+8);
                if((seed&31)==11)oval(c,0x66375438,sx-21,sy-2,sx-3,sy+3);
            }
            if(kind==5 && !tile.road){
                line(c,0x8874C8CD,1.3f,sx-21,sy-6,sx+5,sy-6);
                line(c,0x665FA9B2,1.2f,sx+2,sy+7,sx+22,sy+7);
            }else if((seed&3)==1){
                line(c,kind==3?0xFFB6AA80:0xFF98A77E,1,sx-12,sy+6,sx-6,sy+1);
            }
            // Low-cost hand-illustrated texture accents: ground clusters and irregular
            // fitted stone pavers (repeat deterministically across generated tiles).
            if(tile.road && !tile.river){
                for(int n=0;n<3;n++){
                    float px=sx-19+n*15+((seed>>(n*5))&3);
                    float py=sy-3+((seed>>(n*4+1))&3);
                    polygon(c,n%2==0?0xFFB19F7A:0xFF756F60,
                        px-9,py-2,px,py-7,px+10,py-2,px+5,py+4,px-6,py+4);
                    line(c,0xFF635849,1,px-6,py+3,px+5,py+3);
                }
            }else if(kind!=5 && !tile.road){
                for(int n=0;n<3;n++){
                    float px=sx-20+((seed>>(n*7))&31);
                    float py=sy-8+((seed>>(n*5+11))&15);
                    int leaf=(kind==3?0xFFB8AC78:0xFF80A665);
                    line(c,0xFF233F2D,1.1f,px,py+4,px,py-5);
                    line(c,leaf,1.4f,px,py+1,px-3,py-3);
                    line(c,0xFF6A9256,1.4f,px,py,px+3,py-3);
                    if(n==0 && (seed&7)==5){
                        oval(c,0xFFE8D8A0,px-2,py-7,px+2,py-3);
                        oval(c,0xFFD7A26C,px,py-6,px+2,py-4);
                    }
                }
                if((seed&15)==7){
                    float px=sx+((seed>>19)&15)-8,py=sy+((seed>>24)&7)-4;
                    oval(c,0xFFF0E1B4,px-2,py-2,px+2,py+2);
                    oval(c,0xFFE1B96E,px-1,py-1,px+1,py+1);
                }
            }
        }
        private void illustratedProp(Canvas c,int tile,float centerX,float footY,float width,float height){
            if(vegetationAtlas==null)return;
            int size=vegetationAtlas.getWidth()/4;
            tileSource.set(tile*size,0,(tile+1)*size,size);
            tileDestination.set(centerX-width*.5f,footY-height,centerX+width*.5f,footY);
            c.drawBitmap(vegetationAtlas,tileSource,tileDestination,groundPaint);
        }
        private boolean landmark(int xx,int yy){
            // Stable landmark positions, clearly separated from starting clearing.
            return (xx==13&&yy==-11)||(xx==-19&&yy==12)||(xx==30&&yy==25);
        }
        private int propKind(Tile tile){
            if(landmark(tile.x,tile.y))return 0;
            if((Math.abs(tile.x)<7 && Math.abs(tile.y)<7) || tile.road || tile.river)return -1;
            long seed=tile.propSeed;
            int kind=tile.kind;
            if((seed&15)==4 && (kind==1 || kind==2))return (seed&64)==0?1:2;
            if((seed&31)==15 && tile.level>0)return 3;
            if((seed&31)==22 && kind<=2)return 4;
            return -1;
        }

        private void prop(Canvas c, int kind, float sx, float sy){
            Bitmap sprite=propSprites[kind];
            if(sprite==null){
                sprite=Bitmap.createBitmap(2*PROP_HALF_WIDTH*SCENERY_SCALE,
                    (PROP_ABOVE_FOOT+PROP_BELOW_FOOT)*SCENERY_SCALE,Bitmap.Config.ARGB_8888);
                Canvas surface=new Canvas(sprite);
                surface.scale(SCENERY_SCALE,SCENERY_SCALE);
                surface.translate(PROP_HALF_WIDTH,PROP_ABOVE_FOOT);
                drawProp(surface,kind);
                propSprites[kind]=sprite;
            }
            tileDestination.set(sx-PROP_HALF_WIDTH,sy-PROP_ABOVE_FOOT,
                sx+PROP_HALF_WIDTH,sy+PROP_BELOW_FOOT);
            c.drawBitmap(sprite,null,tileDestination,groundPaint);
        }

        private void drawProp(Canvas c, int kind){
            float sx=0, sy=0;
            if(kind==0){
                oval(c,0x88000000,sx-24,sy-6,sx+28,sy+10);
                polygon(c,0xFF97958A,sx-17,sy-2,sx-14,sy-53,sx-3,sy-60,sx+4,sy-3);
                polygon(c,0xFF7B817D,sx+7,sy+1,sx+9,sy-39,sx+19,sy-43,sx+21,sy+3);
                // Broken sandstone arch with mortar courses, ivy and warm lit accents.
                polygon(c,0xFF8F9080,sx-30,sy-3,sx-26,sy-49,sx-12,sy-54,sx-11,sy-3);
                polygon(c,0xFFB1AE94,sx-31,sy-48,sx-28,sy-57,sx-9,sy-62,sx+12,sy-54,sx+18,sy-46,sx+12,sy-42,sx-5,sy-49);
                polygon(c,0xFF777C73,sx+19,sy-45,sx+25,sy-44,sx+29,sy-3,sx+16,sy-3);
                line(c,0xFF595D53,2,sx-28,sy-28,sx-13,sy-25);
                line(c,0xFF595D53,2,sx+19,sy-25,sx+27,sy-26);
                line(c,0xFFCFCAA8,2,sx-25,sy-52,sx-13,sy-55);
                line(c,0xFF3F6650,3,sx-24,sy-49,sx-20,sy-21);
                oval(c,0xFF41694C,sx-30,sy-36,sx-19,sy-30);
                oval(c,0xFF476C45,sx-24,sy-18,sx-12,sy-10);
                text(c,"RUINS",sx-18,sy-69,11,0xFFE9D7A2);return;
            }
            if(kind==1 || kind==2){
                if(vegetationAtlas!=null){
                    oval(c,0x77000000,sx-19,sy-4,sx+19,sy+5);
                    illustratedProp(c,kind-1,sx,sy+4,122,138);
                    return;
                }
                oval(c,0x99000000,sx-22,sy-4,sx+20,sy+7);
                polygon(c,0xFF57402C,sx-6,sy+1,sx-5,sy-39,sx+4,sy-39,sx+6,sy+1);
                polygon(c,0xFF213D33,sx-26,sy-28,sx-10,sy-68,sx,sy-101,sx+14,sy-63,sx+27,sy-26);
                polygon(c,0xFF305447,sx-27,sy-42,sx-4,sy-83,sx+2,sy-93,sx+21,sy-42);
                polygon(c,0xFF406952,sx-24,sy-55,sx-5,sy-86,sx+1,sy-96,sx+18,sy-56);
                polygon(c,0xFF547656,sx-6,sy-82,sx,sy-97,sx+3,sy-79);
                line(c,0xFF9A9E69,1.3f,sx-12,sy-46,sx-18,sy-34);
                line(c,0xFF8E9D69,1.2f,sx+8,sy-65,sx+17,sy-42);
            }else if(kind==3){
                if(vegetationAtlas!=null){
                    oval(c,0x77000000,sx-17,sy-3,sx+19,sy+6);
                    illustratedProp(c,2,sx,sy+6,102,76);
                    return;
                }
                oval(c,0x77000000,sx-19,sy-1,sx+20,sy+6);
                polygon(c,0xFF525B59,sx-18,sy+1,sx-14,sy-13,sx-3,sy-22,sx+11,sy-16,sx+19,sy-3,sx+12,sy+3);
                polygon(c,0xFF92968B,sx-14,sy-12,sx-4,sy-25,sx+6,sy-22,sx+13,sy-11,sx+1,sy-7);
                line(c,0xFFD0C6A1,2,sx-8,sy-17,sx+4,sy-20);
                line(c,0xFF526D50,2,sx-14,sy-3,sx-5,sy-5);
            }else{
                if(vegetationAtlas!=null){
                    illustratedProp(c,3,sx,sy+5,100,65);
                    return;
                }
                oval(c,0xFF557B42,sx-10,sy-7,sx+10,sy+4);
            }
        }
        private void character(Canvas c,float wx,float wy,int color,String label,float scale){
            int level=height((int)Math.floor(wx),(int)Math.floor(wy));
            float sx=screenX(wx),sy=screenY(wy,level);
            c.save();c.translate(sx,sy);c.scale(scale,scale);
            oval(c,0x99000000,-15,-3,15,6);
            polygon(c,0xFF27313B,-11,-23,11,-23,10,-3,-10,-3);
            polygon(c,color,-13,-29,13,-29,15,-11,-15,-11);
            oval(c,0xFFE0BDA0,-9,-48,9,-30);
            polygon(c,0xFFD9D7C6,-11,-43,-10,-52,9,-52,12,-43);
            polygon(c,0xFFE0BDA0,-9,-39,-18,-44,-11,-31);
            polygon(c,0xFFE0BDA0,9,-39,18,-44,11,-31);
            line(c,0xFFE5E6E0,4,17,-44,21,-3);
            c.restore();
            text(c,label,sx-24,sy-56,12,0xFFFFF0D1);
        }
        private void update(float dt){
            if(paused)return;
            idleTime += dt;
            if (idleTime >= RigIdlePose.PERIOD) idleTime -= RigIdlePose.PERIOD;
            boolean wasWalking=visuallyWalking;
            visuallyWalking=false;
            float dx=0,dy=0;
            if(joystickPointer!=NO_POINTER){
                dx=knobX;
                dy=knobY;
            }else if(moving){
                dx=targetX-x;dy=targetY-y;
                if(dx*dx+dy*dy<.000001f){moving=false;dx=0;dy=0;}
            }
            float distance=(float)Math.sqrt(dx*dx+dy*dy);
            if(distance>(joystickPointer!=NO_POINTER?.02f:.001f) && attackRemaining<=0f){
                float speed=movementFactor*WALK_TILES_PER_SECOND;
                if(runEnabled && canRun())speed*=RUN_MULTIPLIER;
                float step=Math.min(distance,speed*dt),sx=dx/distance*step,sy=dy/distance*step;
                float oldX=x,oldY=y;
                if(walkable(x+sx,y))x+=sx;
                if(walkable(x,y+sy))y+=sy;
                visuallyWalking=Math.abs(x-oldX)+Math.abs(y-oldY)>0.0001f;
                if(visuallyWalking){
                    float movedX=x-oldX,movedY=y-oldY;
                    facing=Math.abs(movedX)>Math.abs(movedY)?(movedX>0?1:3):(movedY>0?0:2);
                    float travelled=(float)Math.hypot(movedX,movedY);
                    // Keep the longer cycle synchronized to distance traveled.
                    if(!wasWalking && gaitBlend<=0f)gait.reset();
                    gait.advance(travelled);
                    walkTime=gait.cycles();
                }
            }
            if(attackRemaining>0f)attackRemaining=Math.max(0f,attackRemaining-dt);
            // Blend in/out over approximately a tenth of a second; never tween facing.
            float blendRate=(visuallyWalking?10f:13f)*dt;
            gaitBlend=Math.max(0f,Math.min(1f,gaitBlend+(visuallyWalking?blendRate:-blendRate)));
            float easing=Math.min(1f,dt*7f);
            camX+=(x-camX)*easing;camY+=(y-camY)*easing;
            int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
            if(tx!=lastVisitedX||ty!=lastVisitedY){
                lastVisitedX=tx;lastVisitedY=ty;
                appContext.getSharedPreferences("wildlands_world",Context.MODE_PRIVATE).edit()
                    .putFloat("player_x",x).putFloat("player_y",y).apply();
                for(int ay=-4;ay<=4;ay++)for(int ax=-4;ax<=4;ax++){
                    long key=((long)(tx+ax)<<32)^((ty+ay)&0xffffffffL);
                    if(seen.add(key))discovered++;
                }
            }
        }
        private float attackProgress() {
            return attackRemaining>0f ? 1f-attackRemaining/RigAttackPose.DURATION : 0f;
        }

        private void drawCloseUp(Canvas c,float h,float d){
            float top=62*d, bottom=h-158*d;
            if(bottom<=top)return;
            rect(c,0xC0101D22,0,top,getWidth(),h-112*d);
            // 1.0x means the same physical size as the normal World View.
            float referenceScale=WORLD_RIG_SCALE*zoom*WORLD_COMPARISON_ZOOM*appearance.heightScale;
            float referenceWidth=Math.min(getWidth()*.34f,
                Math.max(getWidth()*.23f,124f*referenceScale+8*d));
            float closeWidth=getWidth()-referenceWidth;
            float scale=Math.min(closeWidth/124f*appearance.heightScale,(bottom-top)/178f);
            float feetY=bottom-Math.max(15f*d,13f*scale);
            float phase=attackProgress();
            riggedArt.draw(c,closeWidth*.5f,feetY,scale,facing,walkTime,
                gaitBlend,idleTime,phase,false,closeWidth,shoulderBinds);

            if(appearance==ElfAppearance.SILVER){
                shoulderBindBounds.set(12*d,72*d,112*d,103*d);
                rounded(c,shoulderBinds?0xFF3C685B:0xFF324B55,
                    shoulderBindBounds.left,shoulderBindBounds.top,
                    shoulderBindBounds.right,shoulderBindBounds.bottom,6*d);
                text(c,"SHOULDER BINDS",18*d,91*d,9*d,0xFFF3DBA4);
                if(shoulderBinds){
                    text(c,"Cyan: armhole",18*d,117*d,9*d,0xFF75E5F5);
                    text(c,"Gold: arm root",18*d,130*d,9*d,0xFFFFD481);
                }
            }

            float referenceX=closeWidth+referenceWidth*.5f;
            oval(c,0x99112125,referenceX-23*referenceScale,feetY-4*referenceScale,
                referenceX+23*referenceScale,feetY+5*referenceScale);
            riggedArt.draw(c,referenceX,feetY,referenceScale,facing,walkTime,
                gaitBlend,idleTime,phase,false,0f);
            text(c,"1.0× WORLD",referenceX-31*d,feetY+13*d,10*d,0xFFF3DBA4);
        }

        private void drawExplorer(Canvas c){
            float feetX=screenX(x), feetY=screenY(y,height(gridIndex(x),gridIndex(y)));
            oval(c,0x880A1614,feetX-21,feetY-6,feetX+21,feetY+6);
            riggedArt.draw(c,feetX,feetY,WORLD_RIG_SCALE*appearance.heightScale,facing,walkTime,
                gaitBlend,idleTime,attackProgress(),false,0f);
            text(c,heroName,feetX-25,feetY-157*appearance.heightScale,12,0xFFFFF0D1);
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            long now = System.nanoTime();
            if (lastFrame == 0) lastFrame = now;
            float dt = Math.min(.05f, (now - lastFrame) * 1e-9f);
            lastFrame = now;
            update(dt);
            c.drawColor(0xFF101D22);
            float ox=originX(),oy=originY();
            c.save();c.translate(ox,oy);c.scale(zoom,zoom);
            int minY=gridIndex((-oy/zoom-64f)/TILE_SIZE);
            int maxY=gridIndex(((getHeight()-oy)/zoom+220f)/TILE_SIZE);
            float viewLeft=-ox/zoom, viewRight=(getWidth()-ox)/zoom;
            float viewTop=-oy/zoom, viewBottom=(getHeight()-oy)/zoom;
            int firstGroundColumn=gridIndex(viewLeft/TILE_SIZE);
            int lastGroundColumn=gridIndex(viewRight/TILE_SIZE);
            int firstPropColumn=gridIndex((viewLeft-PROP_HALF_WIDTH)/TILE_SIZE);
            int lastPropColumn=gridIndex((viewRight+PROP_HALF_WIDTH)/TILE_SIZE);
            int heroRow=gridIndex(y);
            for(int ty=minY;ty<=maxY;ty++){
                for(int tx=firstGroundColumn;tx<=lastGroundColumn;tx++){
                    Tile tile=tileAt(tx,ty);
                    float top=screenY(ty,tile.level);
                    // South-facing cliff walls can extend to the unraised row boundary.
                    if(top>=viewBottom || screenY(ty+1,0)<=viewTop)continue;
                    terrain(c,tile,screenX(tx+.5f),top+TILE_SIZE*.5f);
                }
                // Place the explorer among props by ground depth within this row.
                if(!closeUp && ty==heroRow && y<ty+.5f)drawExplorer(c);
                for(int tx=firstPropColumn;tx<=lastPropColumn;tx++){
                    Tile tile=tileAt(tx,ty);
                    if(tile.prop<0)continue;
                    float propY=screenY(ty+.5f,tile.level);
                    if(propY+PROP_BELOW_FOOT<=viewTop || propY-PROP_ABOVE_FOOT>=viewBottom)continue;
                    prop(c,tile.prop,screenX(tx+.5f),propY);
                }
                if(!closeUp && ty==heroRow && y>=ty+.5f)drawExplorer(c);
            }
            if(moving){
                float sx=screenX(targetX);
                float sy=screenY(targetY,height(gridIndex(targetX),gridIndex(targetY)));
                oval(c,0xFFEDD59A,sx-7,sy-4,sx+7,sy+4);
            }
            c.restore();
            float h=getHeight()-controlsBottom;
            float d=ui();
            if(closeUp)drawCloseUp(c,h,d);
            if(attackRemaining>0f){
                float markerX=ox+screenX(punchX)*zoom;
                float markerY=oy+screenY(punchY,punchLevel)*zoom;
                float radius=TILE_SIZE*zoom*.28f;
                oval(c,0x70E7A766,markerX-radius,markerY-radius,markerX+radius,markerY+radius);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2*d);
                p.setColor(0xFFF7CC85);
                c.drawCircle(markerX,markerY,radius,p);
                p.setStyle(Paint.Style.FILL);
            }
            // Dark blue / antique gold HUD matches the illustrated elven equipment.
            rect(c,0xF0192630,0,0,getWidth(),62*d);
            rect(c,0xFFB89A62,0,61*d,getWidth(),62*d);
            float titleSize = Math.max(8*d,Math.min(16*d,(getWidth()-236*d)/8f));
            text(c,"THE WILDLANDS",15*d,24*d,titleSize,0xFFF4DCAA);
            viewToggleBounds.set(getWidth()-213*d,5*d,getWidth()-120*d,34*d);
            rounded(c,closeUp?0xFF3C685B:0xFF324B55,
                viewToggleBounds.left,viewToggleBounds.top,
                viewToggleBounds.right,viewToggleBounds.bottom,6*d);
            text(c,closeUp?"CLOSE-UP":"WORLD VIEW",viewToggleBounds.left+8*d,24*d,10*d,0xFFF3DBA4);
            rounded(c,0xFF394249,getWidth()-113*d,8*d,getWidth()-9*d,49*d,6*d);
            text(c,"BASE ONLY",getWidth()-104*d,36*d,11*d,0xFFF3DBA4);
            text(c,String.format(Locale.US,"REGION 01  ·  ELEV %d  ·  EXPLORED %d  ·  %.0f, %.0f",
                height((int)x,(int)y),discovered,x,y),15*d,45*d,10*d,0xFFBFC5B6);
            rounded(c,0xEF192632,0,h-112*d,getWidth(),h,10*d);
            rect(c,0xFFB89A62,0,h-112*d,getWidth(),h-110*d);
            float cx=65*d,cy=h-62*d;
            oval(c,0xFF263A42,cx-46*d,cy-46*d,cx+46*d,cy+46*d);
            oval(c,0xFF566B6B,cx-35*d,cy-35*d,cx+35*d,cy+35*d);
            oval(c,0xFFD0BD90,cx+knobX*23*d-18*d,cy+knobY*23*d-18*d,
                cx+knobX*23*d+18*d,cy+knobY*23*d+18*d);
            float right=getWidth();
            rounded(c,0xFF6B543D,right-110*d,h-93*d,right-10*d,h-33*d,7*d);
            modelToggleBounds.set(right-214*d,h-93*d,right-118*d,h-33*d);
            rounded(c,0xFF31454A,modelToggleBounds.left,modelToggleBounds.top,
                modelToggleBounds.right,modelToggleBounds.bottom,7*d);
            text(c,"APPEARANCE  ›",right-207*d,h-69*d,9*d,0xFFB4C7C1);
            text(c,appearance.label,right-207*d,h-51*d,10*d,0xFFF2DBA8);
            text(c,paused?"RESUME":"PAUSE",right-103*d,h-56*d,13*d,0xFFF2DBA8);
            attackBounds.set(right-110*d,h-158*d,right-10*d,h-112*d);
            rounded(c,attackRemaining>0f?0xFF9B674A:0xFF4E5144,
                attackBounds.left,attackBounds.top,attackBounds.right,attackBounds.bottom,7*d);
            boolean pointReady=actionPointReady();
            text(c,"ATTACK",right-96*d,h-130*d,12*d,pointReady?0xFFF5DFB1:0xFFB4AA92);
            p.setColor(0xFFF5DFB1);
            p.setStyle(pointReady?Paint.Style.FILL:Paint.Style.STROKE);
            p.setStrokeWidth(1.5f*d);
            c.drawCircle(right-24*d,h-136*d,5*d,p);
            p.setStyle(Paint.Style.FILL);
            runBounds.set(right-214*d,h-158*d,right-118*d,h-112*d);
            boolean runAvailable=canRun();
            rounded(c,runAvailable?(runEnabled?0xFF3C685B:0xFF324B55):0xFF394249,
                runBounds.left,runBounds.top,runBounds.right,runBounds.bottom,7*d);
            text(c,"RUN",runBounds.left+14*d,h-130*d,12*d,runAvailable?0xFFF5DFB1:0xFF9B9E94);
            text(c,"Tap terrain to move  ·  Joystick to steer",135*d,h-9*d,10*d,0xFFB4C7C1);
            if(paused){
                rect(c,0xD90A131C,0,0,getWidth(),h);
                // Scale entire panel to FIT the available real screen height.
                // Previously fixed 260*d often placed the Quit button off-screen.
                float ps=Math.min(d,Math.min((getWidth()-24f)/350f,(h-12f)/260f));
                ps=Math.max(.5f,ps);
                float panelWidth=330f*ps;
                float mid=getWidth()*.5f,top=(h-260f*ps)*.5f;
                rounded(c,0xFF182731,mid-panelWidth*.5f-3*ps,top,mid+panelWidth*.5f+3*ps,top+260*ps,12*ps);
                rounded(c,0xFFB49962,mid-panelWidth*.5f,top+3*ps,mid+panelWidth*.5f,top+6*ps,2*ps);
                text(c,"EXPLORATION PAUSED",mid-panelWidth*.5f+22*ps,top+44*ps,18*ps,0xFFF1D9A4);
                float x1=mid-panelWidth*.5f+18*ps,x2=mid+panelWidth*.5f-18*ps;
                resumeBounds.set(x1,top+62*ps,x2,top+112*ps);
                menuBounds.set(x1,top+123*ps,x2,top+173*ps);
                quitBounds.set(x1,top+184*ps,x2,top+234*ps);
                rounded(c,0xFF46645A,resumeBounds.left,resumeBounds.top,resumeBounds.right,resumeBounds.bottom,8*ps);
                rounded(c,0xFF344A55,menuBounds.left,menuBounds.top,menuBounds.right,menuBounds.bottom,8*ps);
                rounded(c,0xFF654438,quitBounds.left,quitBounds.top,quitBounds.right,quitBounds.bottom,8*ps);
                text(c,"RESUME",x1+20*ps,top+95*ps,15*ps,0xFFF9E5BB);
                text(c,"RETURN TO MAIN MENU",x1+20*ps,top+156*ps,14*ps,0xFFF9E5BB);
                text(c,"QUIT GAME",x1+20*ps,top+217*ps,15*ps,0xFFF9E5BB);
            }
            // Display-synchronized invalidation replaces the old 32 ms (~31 fps)
            // timer. The rig interpolates continuously; actual FPS depends on device load.
            if (!paused) postInvalidateOnAnimation();
        }
        private boolean actionPointReady(){
            return android.os.SystemClock.elapsedRealtime()>=actionPointReadyAt;
        }

        private void punch(){
            if(attackRemaining>0f || !actionPointReady())return;
            actionPointReadyAt=android.os.SystemClock.elapsedRealtime()+ACTION_POINT_REFILL_MS;
            attackRemaining=RigAttackPose.DURATION;
            punchX=x+(facing==1?1:facing==3?-1:0);
            punchY=y+(facing==0?1:facing==2?-1:0);
            punchLevel=height(gridIndex(punchX),gridIndex(punchY));
            runEnabled=false;
            moving=false;
            releaseJoystick();
        }

        private boolean canRun(){
            // Add hostile encounter state here when outdoor enemies are introduced.
            return !paused && attackRemaining<=0f;
        }

        private void releaseJoystick(){
            joystickPointer=NO_POINTER;
            knobX=0;
            knobY=0;
        }

        private void setPaused(boolean value) {
            paused = value;
            releaseJoystick();
            lastFrame = 0;
            invalidate();
        }

        private float ui(){return Math.min(density,Math.max(1f,getWidth()/390f));}
        @Override public boolean onTouchEvent(MotionEvent e){
            int event=e.getActionMasked(), index=e.getActionIndex();
            int pointer=e.getPointerId(index);
            float sx=e.getX(index),sy=e.getY(index),d=ui(),h=getHeight()-controlsBottom;
            if(event==MotionEvent.ACTION_MOVE){
                int stickIndex=e.findPointerIndex(joystickPointer);
                if(stickIndex>=0)updateStick(e.getX(stickIndex),e.getY(stickIndex));
                return true;
            }
            if(event==MotionEvent.ACTION_CANCEL || event==MotionEvent.ACTION_UP){
                releaseJoystick();
                return true;
            }
            if(event==MotionEvent.ACTION_POINTER_UP){
                if(pointer==joystickPointer)releaseJoystick();
                return true;
            }
            if(event==MotionEvent.ACTION_DOWN || event==MotionEvent.ACTION_POINTER_DOWN){
                if(runBounds.contains(sx,sy) && !paused){
                    if(canRun())runEnabled=!runEnabled;
                    return true;
                }
                if(event==MotionEvent.ACTION_POINTER_DOWN)return true;
                if(paused){
                    if(resumeBounds.contains(sx,sy)){setPaused(false);return true;}
                    if(menuBounds.contains(sx,sy)){
                        Intent home=new Intent(getContext(),MainActivity.class);
                        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        getContext().startActivity(home);
                        ((Activity)getContext()).finish();
                        return true;
                    }
                    if(quitBounds.contains(sx,sy)){
                        ((Activity)getContext()).finishAffinity();
                        return true;
                    }
                    if(sy>h-110*d && sx>getWidth()-111*d){
                        setPaused(false);
                    }
                    return true;
                }
                if(sy>h)return true;
                if(closeUp && appearance==ElfAppearance.SILVER && shoulderBindBounds.contains(sx,sy)){
                    shoulderBinds=!shoulderBinds;
                    invalidate();
                    return true;
                }
                if(viewToggleBounds.contains(sx,sy)){
                    closeUp=!closeUp;
                    invalidate();return true;
                }
                if(attackBounds.contains(sx,sy)){
                    punch();
                    invalidate();return true;
                }
                if(sy<62*d)return true;
                if(sy>h-110*d){
                    if(sx>getWidth()-111*d){
                        setPaused(!paused);return true;
                    }
                    if(modelToggleBounds.contains(sx,sy)){
                        appearance=appearance.cycle(1);
                        riggedArt.setAppearance(appearance);
                        attackRemaining=0f;
                        invalidate();
                        return true;
                    }
                    if(sx<124*d&&!paused){
                        startJoystick(pointer,sx,sy,h,d);
                        return true;
                    }
                    return true;
                }
                if(paused)return true;
                targetTerrain(sx,sy);
                return true;
            }
            return true;
        }
        private void startJoystick(int pointer,float sx,float sy,float h,float d){
            joystickPointer=pointer;
            moving=false;
            joyX=65*d;
            joyY=h-62*d;
            updateStick(sx,sy);
        }
        private void updateStick(float sx,float sy){
            float vx=(sx-joyX)/(47*ui()),vy=(sy-joyY)/(47*ui());
            float mag=(float)Math.sqrt(vx*vx+vy*vy);
            if(mag>1){vx/=mag;vy/=mag;}
            knobX=vx;knobY=vy;
        }
    }
}
