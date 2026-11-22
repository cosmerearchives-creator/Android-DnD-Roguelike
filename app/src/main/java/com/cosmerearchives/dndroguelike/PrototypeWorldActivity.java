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
        private final RectF exitButton = new RectF();
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
                if (isTree(tx,ty)) return false;
            }
            return true;
        }
        private boolean isTree(int tx,int ty) { int kind=terrain.prop(tx,ty); return kind==1||kind==2; }
        private float elevation(float wx,float wy) {
            int tx=Math.round(wx),ty=Math.round(wy);
            int kind=terrain.prop(tx,ty);
            return (kind==3||kind==4) && Math.abs(wx-tx)<.48f && Math.abs(wy-ty)<.48f ? PLATFORM_RISE : 0f;
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
                    facing=Math.abs(actualX)>Math.abs(actualY)
                        ? (actualX>0?1:3) : (actualY>0?2:0);
                    gait.advance(travelled);
                    walkCycles=gait.cycles();
                }
            }
            blend=Math.max(0,Math.min(1,blend+(moving?10f:-13f)*dt));
            // Lock the camera to the character. A lagging camera made the
            // joystick appear to drift and the walk animation slide.
            cameraX=x;cameraY=y;
        }
        // Keep the approved test-room art direction. Repeat its illustrated floor
        // across the camera's visible world instead of painting Blender blocks.
        // Each source floor covers a 5 x 5 isometric patch; its coordinate system
        // is identical to IsometricRoomActivity (70.0036 / 35.83905).
        private void drawPrototypeGround(Canvas c) {
            if(prototypeFloor==null)return;
            int cx=Math.floorDiv((int)Math.floor(cameraX),5);
            int cy=Math.floorDiv((int)Math.floor(cameraY),5);
            paint.setColor(Color.WHITE);
            for(int depth=-8;depth<=8;depth++)for(int ix=-4;ix<=4;ix++){
                int iy=ix-depth;
                if(Math.abs(iy)>4)continue;
                float wx=ix*5f+cx*5f,wy=iy*5f+cy*5f;
                float px=sx(wx,wy),py=sy(wx,wy);
                destination.set(px-384,py-304.493f,px+384,py+271.507f);
                c.drawBitmap(prototypeFloor,null,destination,paint);
            }
        }
        private void prop(Canvas c,int tx,int ty) {
            int kind=terrain.prop(tx,ty);
            if(kind<0)return;
            float px=sx(tx,ty),py=sy(tx,ty);
            Bitmap art=(kind==1||kind==2)?prototypeTree:prototypeLedge;
            if(art==null)return;
            paint.setColor(Color.WHITE);
            float anchor=art==prototypeTree?165.8486f:87.4622f;
            destination.set(px-art.getWidth()*.5f,py-anchor,
                px+art.getWidth()*.5f,py-anchor+art.getHeight());
            c.drawBitmap(art,null,destination,paint);
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
            drawPrototypeGround(c);
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
            if (appearance==ElfAppearance.PETITE && bakedElf!=null) {
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
                source.set(frame*144,facing*160,(frame+1)*144,(facing+1)*160);
                destination.set(px-72,py-135.303f,px+72,py+24.697f);
                paint.setColor(Color.WHITE);
                c.drawBitmap(sheet,source,destination,paint);
                return;
            }
            character.draw(c,px,py,.75f*appearance.heightScale,facing,walkCycles,
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
            if (!stickActive) {stickCenterX=joystick.centerX();stickCenterY=joystick.centerY();}
            paint.setColor(0x99354B52);c.drawCircle(stickCenterX,stickCenterY,55*d,paint);
            paint.setColor(0xFFB7D6C7);
            c.drawCircle(stickCenterX+screenStickX*36*d,stickCenterY+screenStickY*36*d,19*d,paint);
            button(c,runButton,running?"RUN ON":"RUN");
            button(c,attackButton,"ATTACK / USE");
            button(c,exitButton,"MENU");
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
