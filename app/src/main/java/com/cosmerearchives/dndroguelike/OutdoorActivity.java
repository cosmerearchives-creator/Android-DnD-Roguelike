package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import java.util.Locale;

/**
 * First isometric outdoor vertical slice.
 * Pure world-coordinate generation: tile (x,y) always produces the same terrain.
 * Does not claim to replace the dungeon combat simulation or implement save streaming yet.
 */
public final class OutdoorActivity extends Activity {
    private Landscape landscape;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        landscape=new Landscape(this);
        setContentView(landscape);
        hideBars();
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
        if(landscape!=null&&!landscape.paused){landscape.paused=true;landscape.invalidate();}
        else super.onBackPressed();
    }

    static final class Landscape extends View {
        private static final float TILE_W=88f,TILE_H=44f,LEVEL_H=24f;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path=new Path();
        private final PlayerCharacter hero;
        private final IllustratedFighter fighterArt;
        private final String heroName;
        private float x=0.5f,y=0.5f,camX=x,camY=y,targetX=x,targetY=y;
        private float joyX,joyY,knobX,knobY;
        private boolean joystick=false,moving=false,paused=false;
        private int facing=0;
        private float walkTime=0;
        private long lastFrame;
        private float zoom=1.8f; // Approved close camera for illustrated character detail.
        private int controlsBottom=0;
        private int lastVisitedX=Integer.MIN_VALUE,lastVisitedY=Integer.MIN_VALUE;
        private int discovered=0;
        private final java.util.HashSet<Long> seen=new java.util.HashSet<>();
        private int[] colorRange={0xFF698D65,0xFF527952,0xFF375C4D,0xFF9B966B,0xFF77858C,0xFF497A8F};
        private final float density;
        Landscape(Context context){
            super(context);hero=PlayerCharacter.load(context);
            fighterArt=new IllustratedFighter(context,hero);
            heroName=hero!=null&&hero.completed?hero.name:"Explorer";
            density=getResources().getDisplayMetrics().density;
            setFocusable(true);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
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
        private boolean river(int xx,int yy){
            if(Math.abs(xx)<7&&Math.abs(yy)<7)return false;
            float meander=(field(yy,0,17,51)-.5f)*13f;
            return Math.abs(xx-(23+meander))<1.5f;
        }
        private boolean road(int xx,int yy){
            if(Math.abs(xx)<7&&Math.abs(yy)<7)return yy==0;
            float bend=(field(xx,0,23,19)-.5f)*7f;
            return Math.abs(yy-bend)<1.25f;
        }
        private int height(int xx,int yy){
            if(Math.abs(xx)<8&&Math.abs(yy)<8)return 0;
            float level=field(xx,yy,16,37)*1.15f+field(xx,yy,7,39)*.35f;
            return level>.93f?2:level>.58f?1:0;
        }
        private int biome(int xx,int yy){
            float wet=field(xx,yy,29,12),temp=field(xx+300,yy-600,31,17);
            if(river(xx,yy))return 5;
            if(height(xx,yy)==2)return 4;
            if(wet>.7f)return 2;
            if(wet<.36f)return 3;
            return temp>.55f?0:1;
        }
        private boolean bridge(int xx,int yy){return river(xx,yy)&&road(xx,yy);}
        private boolean blocked(int xx,int yy){
            if(bridge(xx,yy))return false;
            if(river(xx,yy))return true;
            if(Math.abs(xx)<6&&Math.abs(yy)<6)return false;
            if(road(xx,yy))return false;
            long h=hash(xx,yy,83);
            boolean tree=(h&31)==4&&(biome(xx,yy)==1||biome(xx,yy)==2);
            boolean rock=(h&63)==15&&height(xx,yy)>0;
            return tree||rock;
        }
        private boolean walkable(float xx,float yy){
            int tx=(int)Math.floor(xx),ty=(int)Math.floor(yy);
            int fromH=height((int)Math.floor(x),(int)Math.floor(y));
            return !blocked(tx,ty)&&Math.abs(height(tx,ty)-fromH)<=1;
        }
        private float screenX(float tx,float ty){return (tx-ty)*TILE_W*.5f;}
        private float screenY(float tx,float ty){return (tx+ty)*TILE_H*.5f;}
        private float originX(){return getWidth()*.5f-(screenX(camX,camY))*zoom;}
        private float originY(){return (getHeight()-controlsBottom)*.48f-(screenY(camX,camY)-height((int)camX,(int)camY)*LEVEL_H)*zoom;}
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
        private void text(Canvas c,String s,float xx,float yy,float size,int color){
            p.setColor(color);p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));
            p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,xx,yy,p);
        }
        private void terrain(Canvas c,int xx,int yy){
            int level=height(xx,yy),kind=biome(xx,yy);
            float sx=screenX(xx+.5f,yy+.5f),sy=screenY(xx+.5f,yy+.5f)-level*LEVEL_H;
            int base=colorRange[kind];
            if(road(xx,yy)&&!river(xx,yy))base=0xFF947E60;
            if(bridge(xx,yy))base=0xFF967653;
            if(level>0){
                polygon(c,0xFF313B34,sx-TILE_W*.5f,sy,sx,sy+TILE_H*.5f,
                    sx,sy+TILE_H*.5f+level*LEVEL_H,sx-TILE_W*.5f,sy+level*LEVEL_H);
                polygon(c,0xFF40463B,sx,sy+TILE_H*.5f,sx+TILE_W*.5f,sy,
                    sx+TILE_W*.5f,sy+level*LEVEL_H,sx,sy+TILE_H*.5f+level*LEVEL_H);
            }
            polygon(c,base,sx,sy-TILE_H*.5f,sx+TILE_W*.5f,sy,sx,sy+TILE_H*.5f,sx-TILE_W*.5f,sy);
            long seed=hash(xx,yy,27);
            if(kind==5){
                line(c,0x5539D5E7,1,sx-20,sy-2,sx+3,sy-13);
                if(bridge(xx,yy)){
                    polygon(c,0xFFB19A78,sx,sy-18,sx+40,sy,sx,sy+18,sx-40,sy);
                    line(c,0xFF473728,3,sx-35,sy+3,sx+3,sy+18);
                }
            }else if((seed&3)==1){
                line(c,kind==3?0xFFB6AA80:0xFF98A77E,1,sx-12,sy+6,sx-6,sy+1);
            }
            // Low-cost hand-illustrated texture accents: ground clusters and irregular
            // fitted stone pavers (repeat deterministically across generated tiles).
            if(road(xx,yy)&&!river(xx,yy)){
                for(int n=0;n<3;n++){
                    float px=sx-19+n*15+((seed>>(n*5))&3);
                    float py=sy-3+((seed>>(n*4+1))&3);
                    polygon(c,n%2==0?0xFFB19F7A:0xFF756F60,
                        px-9,py-2,px,py-7,px+10,py-2,px+5,py+4,px-6,py+4);
                    line(c,0xFF635849,1,px-6,py+3,px+5,py+3);
                }
            }else if(kind!=5){
                for(int n=0;n<3;n++){
                    float px=sx-20+((seed>>(n*7))&31);
                    float py=sy-8+((seed>>(n*5+11))&15);
                    int leaf=(kind==3?0xFFB9AB74:0xFF8AAA63);
                    line(c,leaf,1,px,py+3,px-2,py-3);
                    line(c,0xFF466B40,1,px,py+3,px+3,py-1);
                }
                if((seed&15)==7){
                    float px=sx+((seed>>19)&15)-8,py=sy+((seed>>24)&7)-4;
                    oval(c,0xFFF0E1B4,px-2,py-2,px+2,py+2);
                    oval(c,0xFFE1B96E,px-1,py-1,px+1,py+1);
                }
            }
        }
        private boolean landmark(int xx,int yy){
            // Stable landmark positions, clearly separated from starting clearing.
            return (xx==13&&yy==-11)||(xx==-19&&yy==12)||(xx==30&&yy==25);
        }
        private void prop(Canvas c,int xx,int yy){
            float sx=screenX(xx+.5f,yy+.5f),sy=screenY(xx+.5f,yy+.5f)-height(xx,yy)*LEVEL_H;
            if(landmark(xx,yy)){
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
            if(Math.abs(xx)<7&&Math.abs(yy)<7)return;
            if(road(xx,yy)||river(xx,yy))return;
            long seed=hash(xx,yy,83);int kind=biome(xx,yy);
            if((seed&31)==4&&(kind==1||kind==2)){
                oval(c,0x99000000,sx-19,sy-3,sx+17,sy+5);
                rect(c,0xFF66513A,sx-4,sy-35,sx+4,sy);
                polygon(c,0xFF244A37,sx-24,sy-33,sx,sy-92,sx+25,sy-33);
                polygon(c,0xFF37634B,sx-23,sy-47,sx,sy-82,sx+21,sy-47);
            }else if((seed&63)==15&&height(xx,yy)>0){
                polygon(c,0xFF7F8880,sx-15,sy+1,sx-7,sy-18,sx+5,sy-24,sx+17,sy-8,sx+12,sy+2);
                line(c,0xFFB1B3A5,2,sx-7,sy-18,sx+1,sy-14);
            }else if((seed&63)==22&&kind==0){
                oval(c,0xFF557B42,sx-10,sy-7,sx+10,sy+4);
            }
        }
        private void character(Canvas c,float wx,float wy,int color,String label,float scale){
            int level=height((int)Math.floor(wx),(int)Math.floor(wy));
            float sx=screenX(wx,wy),sy=screenY(wx,wy)-level*LEVEL_H;
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
            float dx=0,dy=0;
            if(joystick){
                // Screen-oriented joystick: diagonal isometric world axes.
                dx=(knobX+knobY)*.70710677f;
                dy=(knobY-knobX)*.70710677f;
            }else if(moving){
                dx=targetX-x;dy=targetY-y;
                if(dx*dx+dy*dy<.0225f){moving=false;dx=0;dy=0;}
            }
            float distance=(float)Math.sqrt(dx*dx+dy*dy);
            if(distance>.02f){
                walkTime+=dt;
                float cameraDX=dx-dy,cameraDY=dx+dy;
                facing=Math.abs(cameraDX)>Math.abs(cameraDY)?(cameraDX>0?1:3):(cameraDY>0?0:2);
                float speed=(hero!=null&&hero.completed?hero.loadout().movementFactor():1f)*2.4f;
                float step=Math.min(distance,speed*dt),sx=dx/distance*step,sy=dy/distance*step;
                if(walkable(x+sx,y))x+=sx;
                if(walkable(x,y+sy))y+=sy;
            }
            float easing=Math.min(1f,dt*7f);
            camX+=(x-camX)*easing;camY+=(y-camY)*easing;
            int tx=(int)Math.floor(x),ty=(int)Math.floor(y);
            if(tx!=lastVisitedX||ty!=lastVisitedY){
                lastVisitedX=tx;lastVisitedY=ty;
                for(int ay=-4;ay<=4;ay++)for(int ax=-4;ax<=4;ax++){
                    long key=((long)(tx+ax)<<32)^((ty+ay)&0xffffffffL);
                    if(seen.add(key))discovered++;
                }
            }
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            long now=SystemClock.uptimeMillis();
            if(lastFrame==0)lastFrame=now;
            float dt=Math.min(.05f,(now-lastFrame)/1000f);lastFrame=now;update(dt);
            c.drawColor(0xFF101D22);
            float ox=originX(),oy=originY();
            c.save();c.translate(ox,oy);c.scale(zoom,zoom);
            int radiusX=(int)Math.ceil(getWidth()/zoom/TILE_W)+8;
            int radiusY=(int)Math.ceil(getHeight()/zoom/TILE_H)+8;
            radiusX=Math.min(28,Math.max(13,radiusX));
            radiusY=Math.min(27,Math.max(15,radiusY));
            int ix=(int)Math.floor(camX),iy=(int)Math.floor(camY);
            // Painter order matches iso projected depth. Draw all tiles at depth, then their
            // props and party so taller props correctly overlap tiles behind them.
            int minX=ix-radiusX,maxX=ix+radiusX,minY=iy-radiusY,maxY=iy+radiusY;
            for(int sum=minX+minY;sum<=maxX+maxY;sum++){
                int start=Math.max(minX,sum-maxY),end=Math.min(maxX,sum-minY);
                for(int tx=start;tx<=end;tx++){
                    int ty=sum-tx;
                    float screenTileX=ox+screenX(tx+.5f,ty+.5f)*zoom;
                    float screenTileY=oy+(screenY(tx+.5f,ty+.5f)-height(tx,ty)*LEVEL_H)*zoom;
                    if(screenTileX < -100*zoom || screenTileX > getWidth()+100*zoom ||
                        screenTileY < -180*zoom || screenTileY > getHeight()+100*zoom)continue;
                    terrain(c,tx,ty);
                    prop(c,tx,ty);
                }
            }
            // The art renderer is world-agnostic. The player uses the saved
            // creator palette and equipment, rendered at detail-readable scale.
            character(c,x-.92f,y+.84f,0xFF4B9275,"Guardian",.80f);
            character(c,x+.92f,y+.84f,0xFF8E6BAE,"Arcanist",.80f);
            float playerSX=screenX(x,y);
            float playerSY=screenY(x,y)-height((int)Math.floor(x),(int)Math.floor(y))*LEVEL_H;
            fighterArt.draw(c,playerSX,playerSY,.86f,facing,walkTime,
                joystick||moving?1:0);
            text(c,heroName,playerSX-25,playerSY-120,12,0xFFFFF0D1);
            if(moving){
                float sx=screenX(targetX,targetY),sy=screenY(targetX,targetY);
                oval(c,0xFFEDD59A,sx-7,sy-4,sx+7,sy+4);
            }
            c.restore();
            float h=getHeight()-controlsBottom;
            float d=ui();
            rect(c,0xE51C2B35,0,0,getWidth(),58*d);
            text(c,"THE WILDLANDS  •  ISOMETRIC EXPLORATION",13*d,22*d,13*d,0xFFF3E5BE);
            text(c,String.format(Locale.US,"REGION 01   ELEV %d   SEEN %d   (%.0f, %.0f)",height((int)x,(int)y),discovered,x,y),
                13*d,43*d,11*d,0xFFBDD7D7);
            rect(c,0xE51C2B35,0,h-112*d,getWidth(),h);
            float cx=65*d,cy=h-62*d;
            oval(c,0xFF3B5966,cx-46*d,cy-46*d,cx+46*d,cy+46*d);
            oval(c,0xFF96B8BA,cx+knobX*23*d-19*d,cy+knobY*23*d-19*d,
                cx+knobX*23*d+19*d,cy+knobY*23*d+19*d);
            float right=getWidth();
            rect(c,0xFF3A5560,right-214*d,h-93*d,right-118*d,h-33*d);
            rect(c,0xFF816944,right-110*d,h-93*d,right-10*d,h-33*d);
            text(c,"ARENA",right-204*d,h-57*d,13*d,Color.WHITE);
            text(c,paused?"RESUME":"PAUSE",right-103*d,h-57*d,13*d,Color.WHITE);
            text(c,"Tap terrain to move • Joystick overrides destination",135*d,h-10*d,10*d,0xFFBFD3D6);
            if(paused){
                rect(c,0x99000000,0,58*d,getWidth(),h-112*d);
                text(c,"EXPLORATION PAUSED",getWidth()*.25f,h*.47f,23*d,Color.WHITE);
            }
            postInvalidateDelayed(32);
        }
        private float ui(){return Math.min(density,Math.max(1f,getWidth()/390f));}
        @Override public boolean onTouchEvent(MotionEvent e){
            int event=e.getActionMasked();float sx=e.getX(),sy=e.getY(),d=ui(),h=getHeight()-controlsBottom;
            if(event==MotionEvent.ACTION_DOWN){
                if(sy>h)return true;
                if(sy>h-110*d){
                    if(sx>getWidth()-111*d){
                        paused=!paused;lastFrame=SystemClock.uptimeMillis();invalidate();return true;
                    }
                    if(sx>getWidth()-218*d){
                        getContext().startActivity(new Intent(getContext(),GameActivity.class));return true;
                    }
                    if(sx<124*d&&!paused){
                        joystick=true;moving=false;
                        joyX=65*d;joyY=h-62*d;updateStick(sx,sy);return true;
                    }
                    return true;
                }
                if(paused)return true;
                // Undo camera projection to get the target tile in stable world coordinates.
                float wx=(sx-originX())/zoom,wy=(sy-originY())/zoom;
                targetX=(wx/(TILE_W*.5f)+wy/(TILE_H*.5f))*.5f;
                targetY=(wy/(TILE_H*.5f)-wx/(TILE_W*.5f))*.5f;
                moving=true;
                return true;
            }
            if(event==MotionEvent.ACTION_MOVE){if(joystick)updateStick(sx,sy);return true;}
            if(event==MotionEvent.ACTION_CANCEL||event==MotionEvent.ACTION_UP){
                joystick=false;knobX=0;knobY=0;return true;
            }
            return true;
        }
        private void updateStick(float sx,float sy){
            float vx=(sx-joyX)/(47*ui()),vy=(sy-joyY)/(47*ui());
            float mag=(float)Math.sqrt(vx*vx+vy*vy);
            if(mag>1){vx/=mag;vy/=mag;}
            knobX=vx;knobY=vy;
        }
    }
}
