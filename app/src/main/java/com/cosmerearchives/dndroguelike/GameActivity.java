package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** First playable vertical slice. All positions are in game-world units, not pixels. */
public class GameActivity extends Activity {
    private Arena arena;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        arena = new Arena(this);
        setContentView(arena);
    }
    @Override public void onBackPressed() {
        if (arena != null && !arena.paused) { arena.paused = true; arena.invalidate(); }
        else super.onBackPressed();
    }

    static class Unit {
        float x,y, hp,maxHp,attackTimer, awarenessTimer;
        int team, color;
        boolean alerted;
        String name;
        Unit(String name,float x,float y,int team,int color,float hp) {
            this.name=name;this.x=x;this.y=y;this.team=team;this.color=color;this.hp=hp;this.maxHp=hp;
        }
        boolean alive(){return hp>0;}
    }
    static class Arena extends View {
        static final float WORLD_W=960, WORLD_H=1200;
        final Paint p=new Paint(3);
        final Random rng=new Random(17);
        final List<Unit> party=new ArrayList<>();
        final List<Unit> enemies=new ArrayList<>();
        final RectF[] walls={
            new RectF(180,260,380,315),new RectF(590,360,780,415),
            new RectF(410,660,555,715),new RectF(145,850,330,905),
            new RectF(680,860,830,915)
        };
        boolean paused=false, gameOver=false;
        int active=0, kills=0, wave=1;
        float targetX=480,targetY=950;
        boolean moving=false, joystick=false;
        float joyX,joyY,stickX,stickY;
        long lastFrame=0;
        float scale,originX,originY;
        Unit focusedEnemy;
        Arena(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            party.add(new Unit("Hero",480,985,0,Color.rgb(78,172,235),100));
            party.add(new Unit("Guardian",420,1050,0,Color.rgb(79,206,145),125));
            party.add(new Unit("Arcanist",540,1050,0,Color.rgb(190,132,237),75));
            spawnWave();
            setFocusable(true);
        }
        void spawnWave(){
            for(int i=0;i<3+wave;i++){
                float x=170+(i%3)*300, y=145+(i/3)*155;
                enemies.add(new Unit("Goblin",x,y,1,Color.rgb(213,96,84),33+wave*8));
            }
        }
        float distance(float x,float y,float a,float b){
            float dx=x-a,dy=y-b;return (float)Math.sqrt(dx*dx+dy*dy);
        }
        boolean blocked(float x,float y){
            if(x<36||y<100||x>WORLD_W-36||y>WORLD_H-38)return true;
            for(RectF wall:walls) if(wall.contains(x,y))return true;
            return false;
        }
        void move(Unit u,float dx,float dy){
            float nx=u.x+dx,ny=u.y+dy;
            if(!blocked(nx,u.y))u.x=nx;
            if(!blocked(u.x,ny))u.y=ny;
        }
        boolean clearSight(Unit a,Unit b){
            int steps=Math.max(1,(int)(distance(a.x,a.y,b.x,b.y)/18));
            for(int i=1;i<steps;i++){
                float t=i/(float)steps, x=a.x+(b.x-a.x)*t,y=a.y+(b.y-a.y)*t;
                for(RectF wall:walls)if(wall.contains(x,y))return false;
            }
            return true;
        }
        Unit closest(Unit u,List<Unit> candidates){
            Unit best=null;float bd=Float.MAX_VALUE;
            for(Unit c:candidates)if(c.alive()){
                float d=distance(u.x,u.y,c.x,c.y);
                if(d<bd&&clearSight(u,c)){best=c;bd=d;}
            }
            return best;
        }
        void pursue(Unit u,Unit target,float dt,float speed,float stop){
            float d=distance(u.x,u.y,target.x,target.y);
            if(d>stop&&d>1)move(u,(target.x-u.x)/d*speed*dt,(target.y-u.y)/d*speed*dt);
        }
        void attack(Unit u,Unit enemy,float dt,float range,float damage){
            u.attackTimer-=dt;
            if(enemy!=null&&enemy.alive()&&u.attackTimer<=0
                    &&distance(u.x,u.y,enemy.x,enemy.y)<range&&clearSight(u,enemy)){
                enemy.hp=Math.max(0,enemy.hp-damage);
                u.attackTimer=1.05f;
                if(enemy.team==1&&enemy.hp==0)kills++;
            }
        }
        void step(float dt){
            if(paused||gameOver)return;
            Unit controlled=party.get(active);
            if(controlled.alive()){
                if(joystick){
                    float magnitude=(float)Math.sqrt(stickX*stickX+stickY*stickY);
                    if(magnitude>0.12f)move(controlled,stickX*220*dt,stickY*220*dt);
                }else if(moving){
                    float d=distance(controlled.x,controlled.y,targetX,targetY);
                    if(d<8)moving=false;
                    else {
                        float px=controlled.x,py=controlled.y;
                        move(controlled,(targetX-controlled.x)/d*Math.min(d,220*dt),
                            (targetY-controlled.y)/d*Math.min(d,220*dt));
                        if(distance(px,py,controlled.x,controlled.y)<0.25f)moving=false;
                    }
                }
            }
            for(int i=0;i<party.size();i++){
                Unit ally=party.get(i);if(!ally.alive())continue;
                if(i!=active){
                    Unit leader=party.get(active);
                    if(!leader.alive())leader=party.get(0);
                    Unit threat=closest(ally,enemies);
                    if(threat!=null&&distance(ally.x,ally.y,threat.x,threat.y)<230){
                        pursue(ally,threat,dt,150,i==2?175:65);
                    }else{
                        float ox=(i==1?-67:67),oy=70;
                        Unit anchor=leader;
                        float d=distance(ally.x,ally.y,anchor.x+ox,anchor.y+oy);
                        if(d>32){
                            float tx=anchor.x+ox,ty=anchor.y+oy;
                            move(ally,(tx-ally.x)/d*185*dt,(ty-ally.y)/d*185*dt);
                        }
                    }
                }
                Unit enemy=focusedEnemy!=null&&focusedEnemy.alive()?focusedEnemy:closest(ally,enemies);
                if(enemy==null||distance(ally.x,ally.y,enemy.x,enemy.y)>275)enemy=closest(ally,enemies);
                attack(ally,enemy,dt,i==2?205:90,i==2?8:12);
            }
            for(Unit e:enemies){
                if(!e.alive())continue;
                Unit visible=closest(e,party);
                if(visible!=null&&distance(e.x,e.y,visible.x,visible.y)<260){
                    e.awarenessTimer+=dt;
                    if(e.awarenessTimer>=0.75f)e.alerted=true;
                }else if(!e.alerted)e.awarenessTimer=0;
                if(e.alerted&&visible!=null){
                    pursue(e,visible,dt,95,65);
                    attack(e,visible,dt,78,7+wave);
                }
            }
            boolean anyAlive=false;
            for(Unit u:party)if(u.alive())anyAlive=true;
            if(!anyAlive){gameOver=true;paused=true;}
            boolean anyEnemy=false;
            for(Unit e:enemies)if(e.alive())anyEnemy=true;
            if(!anyEnemy){wave++;enemies.clear();focusedEnemy=null;spawnWave();}
        }
        void fill(Canvas c,int color,float left,float top,float right,float bottom){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(left,top,right,bottom,p);
        }
        void text(Canvas c,String s,float x,float y,int size,int color){
            p.setColor(color);p.setTextSize(size);p.setStyle(Paint.Style.FILL);
            p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));
            c.drawText(s,x,y,p);
        }
        void circle(Canvas c,float x,float y,float radius,int color){
            p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,radius,p);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            long now=SystemClock.uptimeMillis();
            if(lastFrame==0)lastFrame=now;
            float dt=Math.min(.05f,(now-lastFrame)/1000f);lastFrame=now;
            step(dt);
            int w=getWidth(),h=getHeight();
            canvas.drawColor(Color.rgb(16,24,30));
            float top=94*getResources().getDisplayMetrics().density;
            float bottom=110*getResources().getDisplayMetrics().density;
            scale=Math.min(w/WORLD_W,Math.max(1,h-top-bottom)/WORLD_H);
            originX=(w-WORLD_W*scale)/2f;
            originY=top+(Math.max(1,h-top-bottom)-WORLD_H*scale)/2f;
            canvas.save();canvas.translate(originX,originY);canvas.scale(scale,scale);
            fill(canvas,Color.rgb(41,58,54),0,0,WORLD_W,WORLD_H);
            p.setColor(Color.rgb(50,70,62));p.setStrokeWidth(2);
            for(int x=0;x<WORLD_W;x+=60)canvas.drawLine(x,0,x,WORLD_H,p);
            for(int y=0;y<WORLD_H;y+=60)canvas.drawLine(0,y,WORLD_W,y,p);
            for(RectF wall:walls){
                fill(canvas,Color.rgb(106,106,103),wall.left,wall.top,wall.right,wall.bottom);
                fill(canvas,Color.rgb(140,134,121),wall.left,wall.top,wall.right,wall.top+8);
            }
            for(Unit e:enemies)if(e.alive()){
                p.setColor(0x35FF9A65);p.setStyle(Paint.Style.FILL);canvas.drawCircle(e.x,e.y,260,p);
                circle(canvas,e.x,e.y,24,e.color);
                fill(canvas,Color.DKGRAY,e.x-24,e.y-39,e.x+24,e.y-33);
                fill(canvas,Color.rgb(239,99,92),e.x-24,e.y-39,e.x-24+48*e.hp/e.maxHp,e.y-33);
                if(e==focusedEnemy){
                    p.setColor(Color.YELLOW);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);
                    canvas.drawCircle(e.x,e.y,29,p);p.setStyle(Paint.Style.FILL);
                }
            }
            for(int i=0;i<party.size();i++){
                Unit u=party.get(i);
                circle(canvas,u.x,u.y,27,u.alive()?u.color:Color.DKGRAY);
                if(i==active){
                    p.setStyle(Paint.Style.STROKE);p.setColor(Color.WHITE);p.setStrokeWidth(4);
                    canvas.drawCircle(u.x,u.y,33,p);p.setStyle(Paint.Style.FILL);
                }
                fill(canvas,Color.DKGRAY,u.x-26,u.y-45,u.x+26,u.y-39);
                fill(canvas,Color.rgb(104,223,137),u.x-26,u.y-45,u.x-26+52*u.hp/u.maxHp,u.y-39);
            }
            if(moving&&!joystick){
                p.setColor(Color.CYAN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
                canvas.drawCircle(targetX,targetY,14,p);p.setStyle(Paint.Style.FILL);
            }
            canvas.restore();
            float density=getResources().getDisplayMetrics().density;
            fill(canvas,0xF51A222D,0,0,w,top);
            float cardWidth=w/3f;
            for(int i=0;i<3;i++){
                Unit u=party.get(i);
                if(i==active)fill(canvas,0xFF364D66,i*cardWidth,0,(i+1)*cardWidth,top);
                circle(canvas,i*cardWidth+21*density,29*density,13*density,u.color);
                text(canvas,u.name,i*cardWidth+40*density,30*density,(int)(12*density),Color.WHITE);
                text(canvas,(int)u.hp+"/"+(int)u.maxHp+" HP",i*cardWidth+12*density,57*density,
                    (int)(12*density),u.alive()?0xFFAEE7BE:0xFFFF8E84);
            }
            text(canvas,"Wave "+wave+"  •  KOs "+kills,14*density,85*density,(int)(12*density),Color.LTGRAY);
            float controlY=h-63*density;
            circle(canvas,66*density,controlY,49*density,0x77557B91);
            circle(canvas,66*density+(joystick?stickX*24*density:0),
                controlY+(joystick?stickY*24*density:0),22*density,0xFF91C4E3);
            fill(canvas,0xFF385C67,w-118*density,h-94*density,w-12*density,h-34*density);
            text(canvas,paused?"RESUME":"PAUSE",w-108*density,h-58*density,
                (int)(15*density),Color.WHITE);
            text(canvas,"Tap ground to move • tap foes to target",13*density,h-10*density,
                (int)(10*density),Color.LTGRAY);
            if(paused){
                fill(canvas,0x88000000,0,top,w,h-110*density);
                text(canvas,gameOver?"PARTY DEFEATED":"TACTICAL PAUSE",w*.14f,h*.44f,
                    (int)(25*density),Color.WHITE);
                text(canvas,gameOver?"Tap RESTART below":"Choose a character or target",w*.13f,h*.49f,
                    (int)(14*density),Color.WHITE);
                if(gameOver){
                    fill(canvas,0xFF507A67,w*.24f,h*.54f,w*.76f,h*.63f);
                    text(canvas,"RESTART",w*.34f,h*.60f,(int)(20*density),Color.WHITE);
                }
            }
            postInvalidateDelayed(16);
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            float x=event.getX(),y=event.getY(),d=getResources().getDisplayMetrics().density;
            if(action==MotionEvent.ACTION_DOWN){
                if(gameOver){
                    if(y>getHeight()*.54f&&y<getHeight()*.65f){reset();}
                    return true;
                }
                if(y<94*d){
                    int index=Math.min(2,(int)(x/(getWidth()/3f)));
                    if(party.get(index).alive()){active=index;moving=false;joystick=false;}
                    invalidate();return true;
                }
                if(x>getWidth()-125*d&&y>getHeight()-110*d){
                    paused=!paused;lastFrame=SystemClock.uptimeMillis();invalidate();return true;
                }
                if(paused){
                    selectAt(x,y);invalidate();return true;
                }
                if(x<130*d&&y>getHeight()-140*d){
                    joystick=true;joyX=66*d;joyY=getHeight()-63*d;setStick(x,y);return true;
                }
                selectAt(x,y);return true;
            }else if(action==MotionEvent.ACTION_MOVE){
                if(joystick){setStick(x,y);return true;}
            }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                joystick=false;stickX=0;stickY=0;return true;
            }
            return true;
        }
        void setStick(float x,float y){
            float dx=x-joyX,dy=y-joyY,range=49*getResources().getDisplayMetrics().density;
            float length=(float)Math.sqrt(dx*dx+dy*dy);
            if(length>range){dx*=range/length;dy*=range/length;}
            stickX=dx/range;stickY=dy/range;
        }
        void selectAt(float sx,float sy){
            float x=(sx-originX)/scale,y=(sy-originY)/scale;
            for(Unit e:enemies)if(e.alive()&&distance(x,y,e.x,e.y)<55){
                focusedEnemy=e;return;
            }
            if(!paused&&x>=0&&x<=WORLD_W&&y>=0&&y<=WORLD_H){
                targetX=x;targetY=y;moving=true;focusedEnemy=null;
            }
        }
        void reset(){
            wave=1;kills=0;active=0;focusedEnemy=null;moving=false;paused=false;gameOver=false;
            enemies.clear();
            for(int i=0;i<party.size();i++){
                Unit u=party.get(i);u.hp=u.maxHp;u.x=480+(i-1)*60;u.y=985+(i==0?0:65);
                u.attackTimer=0;
            }
            spawnWave();lastFrame=SystemClock.uptimeMillis();invalidate();
        }
    }
}
