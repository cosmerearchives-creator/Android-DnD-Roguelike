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
public final class IsometricOverworldActivity extends Activity {
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
        private static final float HALF_W = 45f, HALF_H = 23f;
        private static final int VISIBLE_RADIUS = 17;
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
        private final RiggedElf character;
        private final ElfAppearance appearance;
        private final float movementFactor;
        private final GaitModel gait = new GaitModel();
        private float x, y, cameraX, cameraY, stickX, stickY;
        private float idleTime, walkCycles, attackRemaining, blend;
        private int facing, pointer = -1;
        private boolean running, paused;
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
            appearance = hero == null ? ElfAppearance.COILED : hero.appearance;
            character = new RiggedElf(context, appearance);
            movementFactor = hero == null ? 1f : hero.loadout().movementFactor();
            atlas = load(context, "wildlands_terrain_v031.png");
            vegetation = load(context, "wildlands_vegetation.webp");
            setFocusable(true);
        }
        private static Bitmap load(Context c, String name) {
            try (InputStream in = c.getAssets().open(name)) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) { return null; }
        }
        void save() {
            save.edit().putFloat("player_x", x).putFloat("player_y", y).apply();
        }
        private float sx(float wx, float wy) { return (wx - cameraX + wy - cameraY) * HALF_W; }
        private float sy(float wx, float wy) { return (wx - cameraX - (wy - cameraY)) * HALF_H; }
        private boolean clear(float wx, float wy) {
            int minX=(int)Math.floor(wx-PLAYER_RADIUS), maxX=(int)Math.floor(wx+PLAYER_RADIUS);
            int minY=(int)Math.floor(wy-PLAYER_RADIUS), maxY=(int)Math.floor(wy+PLAYER_RADIUS);
            int originHeight=terrain.height((int)Math.floor(x),(int)Math.floor(y));
            for (int tx=minX;tx<=maxX;tx++) for (int ty=minY;ty<=maxY;ty++) {
                if (terrain.blocked(tx,ty) || Math.abs(terrain.height(tx,ty)-originHeight)>1) return false;
            }
            return true;
        }
        private void update(float dt) {
            if (paused) return;
            idleTime+=dt;
            if (attackRemaining>0) attackRemaining=Math.max(0,attackRemaining-dt);
            float magnitude=(float)Math.hypot(stickX,stickY);
            boolean moving=false;
            if (magnitude>.08f && attackRemaining==0) {
                float speed=WALK_SPEED*movementFactor*(running?1.8f:1f);
                float dx=stickX/magnitude*speed*dt, dy=stickY/magnitude*speed*dt;
                float oldX=x,oldY=y;
                if (clear(x+dx,y)) x+=dx;
                if (clear(x,y+dy)) y+=dy;
                float actualX=x-oldX,actualY=y-oldY;
                float travelled=(float)Math.hypot(actualX,actualY);
                moving=travelled>0.00001f;
                if (moving) {
                    facing=Math.abs(actualX)>Math.abs(actualY)
                        ? (actualX>0?1:3) : (actualY>0?0:2);
                    gait.advance(travelled);
                    walkCycles=gait.cycles();
                }
            }
            blend=Math.max(0,Math.min(1,blend+(moving?10f:-13f)*dt));
            float follow=Math.min(1,dt*7f);
            cameraX+=(x-cameraX)*follow;cameraY+=(y-cameraY)*follow;
        }
        private void tile(Canvas c,int tx,int ty) {
            float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f);
            int elevation=terrain.height(tx,ty);
            py-=elevation*11f;
            int kind=terrain.biome(tx,ty);
            int art=terrain.art(tx,ty);
            diamond.reset();
            diamond.moveTo(px,py-HALF_H);diamond.lineTo(px+HALF_W,py);
            diamond.lineTo(px,py+HALF_H);diamond.lineTo(px-HALF_W,py);
            diamond.close();
            paint.setColor(kind==5?0xFF326E80:kind==4?0xFF6B6C66:0xFF587554);
            c.drawPath(diamond,paint);
            if (atlas!=null) {
                int size=atlas.getWidth()/4;
                source.set((art%4)*size,(art/4)*size,(art%4+1)*size,(art/4+1)*size);
                c.save();
                c.clipPath(diamond);
                paint.setColor(Color.WHITE);
                destination.set(px-HALF_W,py-HALF_H,px+HALF_W,py+HALF_H);
                c.drawBitmap(atlas,source,destination,paint);
                c.restore();
            }
            // Visible raised edge; terrain remains continuous beyond the camera.
            if(elevation>terrain.height(tx+1,ty) || elevation>terrain.height(tx,ty-1)){
                paint.setColor(0xBB283C36);
                c.drawLine(px,py+HALF_H,px,py+HALF_H+elevation*5f,paint);
            }
        }
        private void prop(Canvas c,int tx,int ty) {
            int kind=terrain.prop(tx,ty);
            if(kind<0)return;
            float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f)-terrain.height(tx,ty)*11f;
            if(vegetation!=null && kind<4) {
                source.set(kind*256,0,(kind+1)*256,256);
                destination.set(px-35,py-95,px+35,py+6);
                paint.setColor(Color.WHITE);
                c.drawBitmap(vegetation,source,destination,paint);
            } else {
                paint.setColor(0xFF354D40);
                c.drawCircle(px,py-17,12,paint);
            }
        }
        @Override protected void onDraw(Canvas c) {
            long now=System.nanoTime();
            float dt=lastFrame==0?0:Math.min(.05f,(now-lastFrame)*1e-9f);
            lastFrame=now;
            update(dt);
            c.drawColor(0xFF14242A);
            float centerX=getWidth()*.5f,centerY=getHeight()*.44f;
            zoom=Math.max(.8f,Math.min(1.6f,getWidth()/440f));
            c.save();c.translate(centerX,centerY);c.scale(zoom,zoom);
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            // Painter order for isometric diamond tiles is ascending x-y.
            for(int depth=-VISIBLE_RADIUS*2;depth<=VISIBLE_RADIUS*2;depth++){
                for(int xx=-VISIBLE_RADIUS;xx<=VISIBLE_RADIUS;xx++){
                    int yy=xx-depth;
                    if(Math.abs(yy)>VISIBLE_RADIUS)continue;
                    int tx=cx+xx,ty=cy+yy;
                    tile(c,tx,ty);
                }
            }
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
            float px=sx(x,y),py=sy(x,y)-terrain.height((int)Math.floor(x),(int)Math.floor(y))*11f;
            character.draw(c,px,py,.75f*appearance.heightScale,facing,walkCycles,
                blend,idleTime,attackRemaining>0?1-attackRemaining/RigAttackPose.DURATION:0,
                false,0f);
        }
        private void drawControls(Canvas c) {
            float d=getResources().getDisplayMetrics().density;
            float bottom=getHeight()-18*d;
            joystick.set(18*d,bottom-115*d,133*d,bottom);
            runButton.set(getWidth()-125*d,bottom-111*d,getWidth()-18*d,bottom-70*d);
            attackButton.set(getWidth()-125*d,bottom-59*d,getWidth()-18*d,bottom);
            exitButton.set(getWidth()-94*d,18*d,getWidth()-12*d,54*d);
            paint.setColor(0x99354B52);c.drawCircle(joystick.centerX(),joystick.centerY(),55*d,paint);
            paint.setColor(0xFFB7D6C7);
            c.drawCircle(joystick.centerX()+stickX*36*d,joystick.centerY()+stickY*36*d,19*d,paint);
            button(c,runButton,running?"RUN ON":"RUN");
            button(c,attackButton,"ATTACK");
            button(c,exitButton,"MENU");
            paint.setTextSize(13*d);paint.setColor(0xFFF1D8AA);
            c.drawText("WILDLANDS · FREE ROAM",15*d,32*d,paint);
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
            float dx=(px-joystick.centerX())/(48*d),dy=(py-joystick.centerY())/(48*d);
            float mag=(float)Math.hypot(dx,dy);
            if(mag>1){dx/=mag;dy/=mag;}
            // Screen diagonal to world axes: inverse isometric projection.
            stickX=(dx+dy)*.70710678f;
            stickY=(dx-dy)*.70710678f;
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),index=e.getActionIndex();
            int id=e.getPointerId(index);
            if(action==MotionEvent.ACTION_MOVE) {
                int active=e.findPointerIndex(pointer);
                if(active>=0)stick(e.getX(active),e.getY(active));
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL) {
                pointer=-1;stickX=stickY=0;return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP) {
                if(id==pointer){pointer=-1;stickX=stickY=0;}
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
                    if(attackRemaining<=0)attackRemaining=RigAttackPose.DURATION;
                    return true;
                }
                if(joystick.contains(px,py)&&pointer==-1) {
                    pointer=id;stick(px,py);return true;
                }
                if(paused)paused=false;
                return true;
            }
            return true;
        }
    }
}
