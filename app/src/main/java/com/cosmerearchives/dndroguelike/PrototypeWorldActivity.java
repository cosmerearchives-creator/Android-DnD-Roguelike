package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;

/**
 * Streaming isometric view of the same coordinate-generated Wildlands terrain.
 * Floating-point movement, camera following and tile collision are independent
 * of the tile art. No fixed room bounds or pre-generated world bitmap.
 */
public final class PrototypeWorldActivity extends Activity {
    private World view;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PlayerCharacter hero = PlayerCharacter.loadCompleted(this);
        if (hero == null && !getIntent().getBooleanExtra(OutdoorActivity.EXTRA_FREE_EXPLORE, false)) {
            startActivity(new Intent(this, CharacterCreatorActivity.class));
            finish();
            return;
        }
        view = new World(this, hero);
        setContentView(view);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
    }
    @Override protected void onPause() {
        if (view != null) view.save();
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (view != null) view.paused = !view.paused;
    }

    static final class World extends View {
        private static final float HALF_W = 70.0036f, HALF_H = 35.83905f;
        private static final int VISIBLE_RADIUS = 11;
        private static final float PLATFORM_RISE = 28.7339f;
        private static final float WALK_SPEED = 1.75f;
        private static final float PLAYER_RADIUS = .20f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path diamond = new Path();
        private final Rect source = new Rect();
        private final RectF destination = new RectF();
        private final WorldTerrain terrain = new WorldTerrain();
        private final SharedPreferences save;
        private final Bitmap atlas;
        private final Bitmap vegetation;
        private final Bitmap bakedElf, bakedWalk, bakedPunch;
        private final Bitmap prototypeFloor, prototypeTree, prototypeLedge;
        private final Bitmap prototypeIdle, prototypeWalk, prototypePunch;
        private final boolean bakedEnvironment;
        private final RiggedElf character;
        private final ElfAppearance appearance;
        private final float movementFactor;
        private final GaitModel gait = new GaitModel();
        private float x, y, cameraX, cameraY, stickX, stickY;
        private float screenStickX, screenStickY;
        private float stickCenterX, stickCenterY;
        private boolean stickActive;
        private float idleTime, walkCycles, attackRemaining, blend;
        private int facing, pointer = -1;
        private boolean running, paused;
        private int health, experience, gold;
        private String worldMessage="";
        private float messageRemaining;
        private long lastFrame;
        private final RectF joystick = new RectF(), runButton = new RectF(), attackButton = new RectF();
        private final RectF exitButton = new RectF(), styleButton = new RectF();
        private boolean petiteStyle;
        private float zoom = 1f;

        World(Context context, PlayerCharacter hero) {
            super(context);
            save = context.getSharedPreferences("wildlands_world", Context.MODE_PRIVATE);
            x = save.getFloat("player_x", .5f);
            y = save.getFloat("player_y", .5f);
            if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 1000000
                    || Math.abs(y) > 1000000) { x=.5f; y=.5f; }
            cameraX = x; cameraY = y;
            health=save.getInt("hero_health",100);
            experience=save.getInt("hero_experience",0);
            gold=save.getInt("hero_gold",0);
            appearance = hero == null ? ElfAppearance.COILED : hero.appearance;
            petiteStyle=save.getBoolean("petite_style",appearance==ElfAppearance.PETITE);
            character = new RiggedElf(context, appearance);
            movementFactor = hero == null ? 1f : hero.loadout().movementFactor();
            Bitmap bakedTerrain = load(context, "blender_world/terrain.png");
            Bitmap bakedProps = load(context, "blender_world/props.png");
            bakedEnvironment = bakedTerrain != null && bakedProps != null;
            atlas = bakedEnvironment ? bakedTerrain : load(context, "wildlands_terrain_v031.png");
            vegetation = bakedEnvironment ? bakedProps : load(context, "wildlands_vegetation.webp");
            bakedElf = load(context, "blender_world/petite_idle.png");
            bakedWalk = load(context, "blender_world/petite_walk.png");
            bakedPunch = load(context, "blender_world/petite_punch.png");
            prototypeFloor = load(context,"isometric_lab/floor.png");
            prototypeTree = load(context,"isometric_lab/tree.png");
            prototypeLedge = load(context,"isometric_lab/ledge.png");
            prototypeIdle = load(context,"isometric_lab/idle.png");
            prototypeWalk = load(context,"isometric_lab/walk.png");
            prototypePunch = load(context,"isometric_lab/punch.png");
            setFocusable(true);
        }
        private static Bitmap load(Context c, String name) {
            try (InputStream in = c.getAssets().open(name)) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) { return null; }
        }
        void save() {
            save.edit().putFloat("player_x", x).putFloat("player_y", y)
                .putInt("hero_health",health).putInt("hero_experience",experience)
                .putInt("hero_gold",gold).apply();
        }
        private float sx(float wx, float wy) { return (wx - cameraX + wy - cameraY) * HALF_W; }
        private float sy(float wx, float wy) { return (wx - cameraX - (wy - cameraY)) * HALF_H; }
        private boolean clear(float wx, float wy) {
            int minX=(int)Math.floor(wx-PLAYER_RADIUS), maxX=(int)Math.floor(wx+PLAYER_RADIUS);
            int minY=(int)Math.floor(wy-PLAYER_RADIUS), maxY=(int)Math.floor(wy+PLAYER_RADIUS);

            for (int tx=minX;tx<=maxX;tx++) for (int ty=minY;ty<=maxY;ty++) {
                if (isTree(tx,ty) || terrain.ancientFootprint(tx,ty)) return false;
            }
            return true;
        }
        private boolean isTree(int tx,int ty) { int kind=terrain.wildernessProp(tx,ty); return kind==1||kind==3||kind==4; }
        private float elevation(float wx,float wy) {
            int tx=Math.round(wx),ty=Math.round(wy);
            return 0f;
        }
        private void update(float dt) {
            if (paused) return;
            idleTime+=dt;
            messageRemaining=Math.max(0,messageRemaining-dt);
            if (attackRemaining>0) attackRemaining=Math.max(0,attackRemaining-dt);
            float magnitude=(float)Math.hypot(stickX,stickY);
            boolean moving=false;
            if (magnitude>.08f && attackRemaining==0) {
                float speed=WALK_SPEED*movementFactor*(running?1.8f:1f);
                float dx=stickX*speed*dt, dy=stickY*speed*dt;
                float oldX=x,oldY=y;
                if (clear(x+dx,y)) x+=dx;
                if (clear(x,y+dy)) y+=dy;
                float actualX=x-oldX,actualY=y-oldY;
                float travelled=(float)Math.hypot(actualX,actualY);
                moving=travelled>0.00001f;
                if (moving) {
                    // Prototype directions: 0 north (-Y), 1 east (+X),
                    // 2 south (+Y), 3 west (-X). Do not reverse north/south.
                    // Eight directions, ordered N, NE, E, SE, S, SW, W, NW.
                    // The Blender atlas contains all eight orientations.
                    float heading=(float)Math.atan2(actualX,-actualY);
                    facing=Math.floorMod(Math.round(heading/(float)(Math.PI/4)),8);
                    gait.advance(travelled);
                    walkCycles=gait.cycles();
                }
            }
            blend=Math.max(0,Math.min(1,blend+(moving?10f:-13f)*dt));
            // Lock the camera to the character. A lagging camera made the
            // joystick appear to drift and the walk animation slide.
            cameraX=x;cameraY=y;
        }
        // Continuous coordinate-seeded wilderness. Draw world-space diamonds rather
        // than repeating a room bitmap; large-scale noise creates connected meadows,
        // woodland, stony ridges and a wandering trail without visible atlas seams.
        private void ground(Canvas c) {
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            for(int depth=-VISIBLE_RADIUS*2-3;depth<=VISIBLE_RADIUS*2+3;depth++) {
                for(int xx=-VISIBLE_RADIUS-3;xx<=VISIBLE_RADIUS+3;xx++) {
                    int yy=xx-depth;
                    if(Math.abs(yy)>VISIBLE_RADIUS+3)continue;
                    int tx=cx+xx,ty=cy+yy;
                    float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f);
                    int biome=terrain.wildernessBiome(tx,ty);
                    int base=biome==3?0xFF6D725C:biome==2?0xFF354D3C:
                        biome==1?0xFF496947:0xFF668457;
                    if(terrain.trail(tx,ty))base=0xFF77634B;
                    float tint=terrain.groundVariation(tx,ty);
                    int delta=(int)((tint-.5f)*21);
                    int r=Math.max(0,Math.min(255,Color.red(base)+delta));
                    int g=Math.max(0,Math.min(255,Color.green(base)+delta));
                    int b=Math.max(0,Math.min(255,Color.blue(base)+delta));
                    paint.setColor(Color.rgb(r,g,b));
                    diamond.reset();
                    diamond.moveTo(px,py-HALF_H);
                    diamond.lineTo(px+HALF_W,py);
                    diamond.lineTo(px,py+HALF_H);
                    diamond.lineTo(px-HALF_W,py);
                    diamond.close();
                    c.drawPath(diamond,paint);
                    int h=terrain.decoration(tx,ty);
                    if(!terrain.trail(tx,ty)&&h%5==0) {
                        paint.setColor(biome==2?0x664F7652:0x667F9C65);
                        c.drawOval(px-18,py-5,px+19,py+6,paint);
                    }
                    if(h%9==1) {
                        paint.setColor(0x88502F22);
                        c.drawCircle(px+(h%17)-8,py+((h/17)%13)-6,2.3f,paint);
                    }
                    if(h%13==3&&biome!=3&&!terrain.trail(tx,ty)) {
                        paint.setColor(0xFFB3A47B);
                        c.drawCircle(px-8,py+2,1.7f,paint);
                        paint.setColor(0xFFB3A1C1);
                        c.drawCircle(px+5,py-3,1.5f,paint);
                    }
                }
            }
        }
        private void prop(Canvas c,int tx,int ty) {
            int kind=terrain.wildernessProp(tx,ty);
            if(kind<0)return;
            float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f);
            if(kind==4) {
                drawTree(c,px+HALF_W,py,2.35f,tx*17+ty*31);
                return;
            }
            int variety=terrain.decoration(tx,ty);
            if(kind==0) {
                c.save();
                c.translate(px,py);
                c.scale(.7f+(variety%7)*.11f,.72f+((variety>>>3)%6)*.12f);
                px=0;py=0;
                // Low, rounded rock with ground shadow instead of a raised square platform.
                paint.setColor(0x66302224);
                c.drawOval(px-25,py-7,px+26,py+13,paint);
                paint.setColor(0xFF555D58);
                c.drawOval(px-19,py-16,px+19,py+8,paint);
                paint.setColor(0xFF849083);
                c.drawOval(px-14,py-17,px+13,py-5,paint);
                if((variety&2)!=0){paint.setColor(0xFF74876D);c.drawOval(px-12,py-15,px+4,py-9,paint);}
                c.restore();
                return;
            }
            if(kind==3) {
                paint.setColor(0x77332828);
                c.drawOval(px-28,py-8,px+29,py+11,paint);
                paint.setColor(0xFF777B6E);
                c.drawRect(px-15,py-35,px-5,py+2,paint);
                c.drawRect(px+6,py-26,px+17,py+2,paint);
                paint.setColor(0xFF9A9A83);
                c.drawRect(px-19,py-37,px-3,py-31,paint);
                return;
            }
            if(kind==2) {
                c.save();
                c.translate(px,py);
                c.scale(.7f+(variety%8)*.11f,.65f+((variety>>>4)%8)*.12f);
                px=0;py=0;
                paint.setColor(0xAA2A4734);
                c.drawOval(px-22,py-8,px+22,py+8,paint);
                paint.setColor(0xFF53764A);
                c.drawCircle(px-10,py-8,15,paint);
                c.drawCircle(px+8,py-12,18,paint);
                paint.setColor(0xFF6F955B);
                c.drawCircle(px+1,py-19,13,paint);
                if((variety&4)!=0){paint.setColor(0xFF9B9872);c.drawCircle(px+9,py-16,3,paint);}
                c.restore();
                return;
            }
            // A typical tree is substantially taller than the player; deterministic
            // size variation makes groves less uniform.
            int seed=terrain.decoration(tx,ty);
            float scale=1.12f+(seed%7)*.12f;
            drawTree(c,px,py,scale,seed);
        }
        private void drawTree(Canvas c,float px,float py,float scale,int seed) {
            c.save();
            c.translate(px,py);
            c.scale(scale,scale);
            float shade=(seed&3)*.03f;
            paint.setColor(0x77302628);
            c.drawOval(-30,-8,32,13,paint);
            paint.setColor(0xFF544838);
            c.drawRect(-5,-78,6,3,paint);
            paint.setColor(0xFF233D34);
            c.drawCircle(-19,-85,25,paint);
            c.drawCircle(18,-88,29,paint);
            c.drawCircle(0,-111,33,paint);
            paint.setColor(shade>.05f?0xFF365940:0xFF3D6144);
            c.drawCircle(-13,-107,27,paint);
            c.drawCircle(12,-117,24,paint);
            paint.setColor(0xFF567B4D);
            c.drawCircle(-8,-129,15,paint);
            c.restore();
        }

        @Override protected void onDraw(Canvas c) {
            long now=System.nanoTime();
            float dt=lastFrame==0?0:Math.min(.05f,(now-lastFrame)*1e-9f);
            lastFrame=now;
            update(dt);
            c.drawColor(0xFF14242A);
            float centerX=getWidth()*.5f,centerY=getHeight()*.44f;
            zoom=Math.max(.65f,Math.min(1.15f,getWidth()/768f));
            c.save();c.translate(centerX,centerY);c.scale(zoom,zoom);
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            ground(c);
            float heroDepth=x-y;
            boolean actorDrawn=false;
            for(int depth=-VISIBLE_RADIUS*2;depth<=VISIBLE_RADIUS*2;depth++){
                for(int xx=-VISIBLE_RADIUS;xx<=VISIBLE_RADIUS;xx++){
                    int yy=xx-depth;
                    if(Math.abs(yy)>VISIBLE_RADIUS)continue;
                    int tx=cx+xx,ty=cy+yy;
                    if(!actorDrawn && tx-ty>heroDepth) {
                        drawHero(c);actorDrawn=true;
                    }
                    prop(c,tx,ty);
                }
            }
            if(!actorDrawn)drawHero(c);
            c.restore();
            drawControls(c);
            postInvalidateOnAnimation();
        }
        private void drawHero(Canvas c) {
            float px=sx(x,y),py=sy(x,y)-elevation(x,y);
            // The original approved test-room character and its real walk/punch
            // frames take priority over the later mismatched Blender prototype.
            if (petiteStyle && bakedElf!=null && bakedWalk!=null && bakedPunch!=null) {
                Bitmap sheet=bakedElf;
                int frame=Math.floorMod((int)(idleTime*4f),4);
                if (attackRemaining>0 && bakedPunch!=null) {
                    sheet=bakedPunch;
                    frame=Math.min(4,(int)(5*(1-attackRemaining/RigAttackPose.DURATION)));
                } else if (blend>.15f && bakedWalk!=null) {
                    sheet=bakedWalk;
                    frame=Math.floorMod((int)(walkCycles*8),8);
                }
                source.set(frame*168,facing*192,(frame+1)*168,(facing+1)*192);
                destination.set(px-84,py-160,px+84,py+32);
                paint.setColor(Color.WHITE);
                c.drawBitmap(sheet,source,destination,paint);
                return;
            }
            if (prototypeIdle!=null
                    && prototypeWalk!=null && prototypePunch!=null) {
                Bitmap sheet=prototypeIdle;
                int frame=Math.floorMod((int)(idleTime*2f),4);
                if(attackRemaining>0){
                    sheet=prototypePunch;
                    frame=Math.min(6,(int)(7*(1-attackRemaining/RigAttackPose.DURATION)));
                } else if(blend>.15f){
                    sheet=prototypeWalk;
                    frame=Math.floorMod((int)(walkCycles*8),8);
                }
                int cardinal=(facing+1)/2%4;
                source.set(frame*144,cardinal*160,(frame+1)*144,(cardinal+1)*160);
                destination.set(px-72,py-135.303f,px+72,py+24.697f);
                paint.setColor(Color.WHITE);
                c.drawBitmap(sheet,source,destination,paint);
                return;
            }
            character.draw(c,px,py,.75f*appearance.heightScale,(facing+1)/2%4,walkCycles,
                blend,idleTime,attackRemaining>0?1-attackRemaining/RigAttackPose.DURATION:0,
                false,0f);
        }
        private void interact() {
            int px=(int)Math.floor(x),py=(int)Math.floor(y);
            int closest=0,tx=0,ty=0;
            for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
                int xx=px+dx,yy=py+dy;
                int kind=terrain.landmark(xx,yy);
                if(kind!=0&&Math.hypot(xx+.5-x,yy+.5-y)<2.2){
                    closest=kind;tx=xx;ty=yy;
                }
            }
            if(closest==0){
                if(attackRemaining<=0)attackRemaining=RigAttackPose.DURATION;
                return;
            }
            String key="visited_"+tx+"_"+ty;
            boolean visited=save.getBoolean(key,false);
            if(closest==1){
                health=100;
                worldMessage="HAMLET · Restored to full health";
            } else if(closest==2){
                if(!visited){gold+=25;experience+=40;}
                worldMessage=visited?"ANCIENT RUINS · Already explored":"ANCIENT RUINS · Found 25 gold and 40 XP";
            } else {
                if(!visited){health=Math.max(1,health-12);gold+=8;experience+=20;}
                worldMessage=visited?"WILDERNESS · Encounter cleared":"WILDERNESS · Enemy defeated · +20 XP";
                attackRemaining=RigAttackPose.DURATION;
            }
            save.edit().putBoolean(key,true).apply();
            messageRemaining=4f;
            save();
        }
        private void drawControls(Canvas c) {
            float d=getResources().getDisplayMetrics().density;
            float bottom=getHeight()-18*d;
            joystick.set(18*d,bottom-115*d,133*d,bottom);
            runButton.set(getWidth()-125*d,bottom-111*d,getWidth()-18*d,bottom-70*d);
            attackButton.set(getWidth()-125*d,bottom-59*d,getWidth()-18*d,bottom);
            exitButton.set(getWidth()-94*d,18*d,getWidth()-12*d,54*d);
            styleButton.set(getWidth()-153*d,62*d,getWidth()-12*d,100*d);
            if (!stickActive) {stickCenterX=joystick.centerX();stickCenterY=joystick.centerY();}
            paint.setColor(0x99354B52);c.drawCircle(stickCenterX,stickCenterY,55*d,paint);
            paint.setColor(0xFFB7D6C7);
            c.drawCircle(stickCenterX+screenStickX*36*d,stickCenterY+screenStickY*36*d,19*d,paint);
            button(c,runButton,running?"RUN ON":"RUN");
            button(c,attackButton,"ATTACK / USE");
            button(c,exitButton,"MENU");
            button(c,styleButton,petiteStyle?"STYLE: PETITE":"STYLE: CLASSIC");
            paint.setTextSize(13*d);paint.setColor(0xFFF1D8AA);
            c.drawText("WILDLANDS · FREE ROAM",15*d,32*d,paint);
            c.drawText("HP "+health+"   XP "+experience+"   GOLD "+gold,15*d,50*d,paint);
            if(messageRemaining>0){
                paint.setTextSize(12*d);
                c.drawText(worldMessage,15*d,72*d,paint);
            }
            if(paused) {
                paint.setTextSize(25*d);
                c.drawText("PAUSED",getWidth()*.38f,getHeight()*.35f,paint);
            }
        }
        private void button(Canvas c,RectF bounds,String label) {
            paint.setColor(0xCC304952);c.drawRoundRect(bounds,9,9,paint);
            paint.setColor(0xFFF1D8AA);paint.setTextSize(13*getResources().getDisplayMetrics().density);
            c.drawText(label,bounds.left+12,bounds.centerY()+5,paint);
        }
        private void stick(float px,float py) {
            float d=getResources().getDisplayMetrics().density;
            float dx=(px-stickCenterX)/(48*d),dy=(py-stickCenterY)/(48*d);
            float mag=(float)Math.hypot(dx,dy);
            if(mag>1){dx/=mag;dy/=mag;mag=1;}
            screenStickX=dx;screenStickY=dy;
            // Deadzone suppresses noise; outside it, displacement controls speed.
            if(mag<.12f){stickX=stickY=0;return;}
            float power=Math.min(1f,(mag-.12f)/.88f);
            // Inverse of screen=(x+y)*HALF_W,(x-y)*HALF_H.
            float wx=dx/HALF_W+dy/HALF_H;
            float wy=dx/HALF_W-dy/HALF_H;
            float length=(float)Math.hypot(wx,wy);
            if(length>0){stickX=wx/length*power;stickY=wy/length*power;}
            else {stickX=stickY=0;}
        }
        private void releaseStick() {
            pointer=-1;stickActive=false;
            stickX=stickY=screenStickX=screenStickY=0f;
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),index=e.getActionIndex();
            int id=e.getPointerId(index);
            if(action==MotionEvent.ACTION_MOVE) {
                int active=e.findPointerIndex(pointer);
                if(active>=0)stick(e.getX(active),e.getY(active));
                else if(pointer!=-1)releaseStick();
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL) {
                releaseStick();return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP) {
                if(id==pointer)releaseStick();
                return true;
            }
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
                float px=e.getX(index),py=e.getY(index);
                if(exitButton.contains(px,py)) {
                    save();getContext().startActivity(new Intent(getContext(),MainActivity.class));
                    ((Activity)getContext()).finish();return true;
                }
                if(styleButton.contains(px,py)){
                    petiteStyle=!petiteStyle;
                    save.edit().putBoolean("petite_style",petiteStyle).apply();
                    return true;
                }
                if(runButton.contains(px,py)){running=!running;return true;}
                if(attackButton.contains(px,py)){
                    interact();
                    return true;
                }
                // Floating touchpad: any touch in the left lower half
                // captures its own pointer; action buttons remain independent.
                if(pointer==-1 && px<getWidth()*.52f && py>getHeight()*.38f) {
                    pointer=id;stickActive=true;
                    stickCenterX=px;stickCenterY=py;
                    stick(px,py);return true;
                }
                if(paused)paused=false;
                return true;
            }
            return true;
        }
    }
}
