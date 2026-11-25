package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;

final class TownHub {
    static final int WIDTH=26, HEIGHT=22, GOAL=10;
    static final float BOARD_X=13f, BOARD_Y=11f, DUMMY_X=13f, DUMMY_Y=18.5f;
    static final float SPAWN_X=13f, SPAWN_Y=9.5f;
    static final float HALF_W=70.0036f, HALF_H=35.83905f;
    private static final float HEIGHT_SCALE=85f;
    private static final float BUILDING_SCALE=(float)Math.sqrt(2)*HALF_W/36f;

    private static final float BED_X=6.7f, BED_Y=5.7f;

    private enum Kind { BUILDING, WALL, GATE, BOARD, DUMMY, TABLE, BAR, BED }
    private static final class Prop {
        final Kind kind;
        final float x,y,w,h;
        final int index;
        Prop(Kind kind,float x,float y,float w,float h,int index) {
            this.kind=kind;this.x=x;this.y=y;this.w=w;this.h=h;this.index=index;
        }
        float depth() { return x-y+(w+h)*.5f; }
        boolean collides(float px,float py,float radius) {
            return Math.abs(px-x)<w*.5f+radius && Math.abs(py-y)<h*.5f+radius;
        }
    }
    interface Actor {
        void draw(Canvas canvas);
    }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.LightingColorFilter memoryTint =
        new android.graphics.LightingColorFilter(0xFF595959,0);
    private final Path polygon=new Path();
    private final RectF bounds=new RectF();
    private final Bitmap[] facades=new Bitmap[6];
    private final ArrayList<Prop> town=new ArrayList<>(), tavern=new ArrayList<>();
    private final String[] names={"Tavern","Blacksmith","General Store","Apothecary","Watch Office","Cottage"};
    private boolean inside;

    TownHub(Context context) {
        float[][] homes={{6,5,6,5},{20,5,5,4},{6,11,4,4},
            {20,11,4,3},{6,17,4,4},{20,17,4,3}};
        for (int i=0;i<homes.length;i++) {
            float[] b=homes[i];
            town.add(new Prop(Kind.BUILDING,b[0],b[1],b[2],b[3],i));
            try (InputStream in=context.getAssets().open("town/building_"+i+".webp")) {
                facades[i]=BitmapFactory.decodeStream(in);
            } catch (IOException ignored) {
                // Geometry remains visible if an asset cannot be decoded.
            }
        }
        for (int x=0;x<WIDTH;x++) {
            town.add(new Prop(Kind.WALL,x+.5f,.5f,1,1,0));
            town.add(new Prop(Kind.WALL,x+.5f,HEIGHT-.5f,1,1,0));
        }
        for (int y=1;y<HEIGHT-1;y++) {
            Kind kind=(y==10 || y==11) ? Kind.GATE : Kind.WALL;
            town.add(new Prop(kind,.5f,y+.5f,1,1,0));
            town.add(new Prop(kind,WIDTH-.5f,y+.5f,1,1,0));
        }
        town.add(new Prop(Kind.BOARD,BOARD_X,BOARD_Y,.85f,.3f,0));
        town.add(new Prop(Kind.DUMMY,DUMMY_X,DUMMY_Y,.40f,.40f,0));
        tavern.add(new Prop(Kind.BAR,1.4f,4.8f,.65f,3,0));
        tavern.add(new Prop(Kind.TABLE,4.8f,4.9f,1.3f,1.1f,0));
        tavern.add(new Prop(Kind.TABLE,5.6f,2.8f,1.3f,1.1f,0));
        tavern.add(new Prop(Kind.BED,BED_X,BED_Y,1.65f,.9f,0));
        town.sort(Comparator.comparingDouble(Prop::depth));
        tavern.sort(Comparator.comparingDouble(Prop::depth));
    }

    boolean training(float x,float y) { return !inside && x>=11f && x<15f && y>=16.5f && y<20.5f; }
    boolean atBoard(float x,float y) { return !inside && distance(x,y,BOARD_X,BOARD_Y)<1.42f; }
    boolean atTavern(float x,float y) { return !inside && distance(x,y,6f,1.8f)<1.35f; }
    boolean atBed(float x,float y) { return inside && distance(x,y,BED_X,BED_Y)<1.5f; }
    boolean atExit(float x,float y) { return inside && distance(x,y,3f,.75f)<1.4f; }
    boolean atGate(float x,float y) {
        return !inside && Math.abs(y-11f)<1.3f && (x<2.5f || x>WIDTH-2.5f);
    }
    void setInside(boolean value) { inside=value; }
    boolean isInside() { return inside; }

    boolean visionClear(float ax,float ay,float bx,float by,boolean terrainTarget) {
        if(bx<0 || by<0 || bx>=(inside?8:WIDTH) || by>=(inside?7:HEIGHT)) return false;
        int steps=(int)Math.ceil(Math.hypot(bx-ax,by-ay)*10);
        for(Prop p:inside?tavern:town){
            if(p.kind!=Kind.BUILDING && p.kind!=Kind.WALL && p.kind!=Kind.GATE)continue;
            if(terrainTarget && p.collides(bx,by,0))continue;
            for(int i=1;i<=steps;i++){
                float t=(float)i/steps;
                if(p.collides(ax+(bx-ax)*t,ay+(by-ay)*t,0))return false;
            }
        }
        return true;
    }
    interface Fog { int shade(int x,int y); }
    interface Sight { Senses.Sight at(int x,int y); }
    interface Lighting { android.graphics.ColorFilter at(float x,float y,Senses.Sight sight); }

    boolean clear(float x,float y,float radius) {
        if (x-radius<0 || y-radius<0 || x+radius>=(inside?8:WIDTH)
            || y+radius>=(inside?7:HEIGHT)) return false;
        for (Prop p:inside?tavern:town) if (p.collides(x,y,radius)) return false;
        return true;
    }
    private static double distance(float x,float y,float px,float py) {
        return Math.hypot(x-px,y-py);
    }
    private static float x(float a,float b,float cameraX,float cameraY) {
        return (a-cameraX+b-cameraY)*HALF_W;
    }
    private static float y(float a,float b,float cameraX,float cameraY) {
        return (a-cameraX-b+cameraY)*HALF_H;
    }
    void ground(Canvas c,float cx,float cy,Fog fog,Lighting light,Sight sight) {
        int W=inside?8:WIDTH,H=inside?7:HEIGHT;
        for (int depth=1-H;depth<W;depth++) {
            for(int i=Math.max(0,depth);i<Math.min(W,H+depth);i++) {
                int j=i-depth;
                if (Math.abs(i-cx)>12 || Math.abs(j-cy)>12) continue;
                boolean plaza=i>=10 && i<16 && j>=8 && j<14;
                boolean path=i>=11 && i<15 || j>=9 && j<12 || j==2 || j==7 || j==14;
                int shade=(i*11+j*17)&3;
                int color=inside ? 0xFF6D4932 : training(i+.5f,j+.5f) ? 0xFF91704E :
                    plaza ? 0xFF788079 : path ? 0xFF9C896D : 0xFF4F6344;
                int fogShade=fog.shade(i,j);
                paint.setColorFilter(light.at(i+.5f,j+.5f,sight.at(i,j)));
                paint.setColor(color+shade*0x00020202);
                float xx=x(i+.5f,j+.5f,cx,cy), yy=y(i+.5f,j+.5f,cx,cy);
                face(c,xx,yy-HALF_H-.25f,xx+HALF_W+.45f,yy,
                    xx,yy+HALF_H+.25f,xx-HALF_W-.45f,yy);
                paint.setColorFilter(null);
                if(fogShade!=0){
                    paint.setColor(fogShade);
                    face(c,xx,yy-HALF_H-.25f,xx+HALF_W+.45f,yy,
                        xx,yy+HALF_H+.25f,xx-HALF_W-.45f,yy);
                }
            }
        }
    }
    void props(Canvas c,float cx,float cy,float heroDepth,Actor hero,Fog fog,Lighting light,Sight sight) {
        boolean drawn=false;
        for (Prop p:inside?tavern:town) {
            if (Math.abs(p.x-cx)>13 || Math.abs(p.y-cy)>13) continue;
            if (!drawn && p.depth()>heroDepth) {hero.draw(c); drawn=true;}
            int shade=fog.shade((int)p.x,(int)p.y);
            if(shade==0xFF000000 || p.kind==Kind.DUMMY && shade!=0)continue;
            paint.setColorFilter(shade!=0 ? memoryTint : light.at(p.x,p.y,sight.at((int)p.x,(int)p.y)));
            draw(c,p,cx,cy);
            paint.setColorFilter(null);
        }
        if (!drawn) hero.draw(c);
    }
    private void draw(Canvas c,Prop p,float cx,float cy) {
        float px=x(p.x,p.y,cx,cy), py=y(p.x,p.y,cx,cy);
        switch (p.kind) {
            case BUILDING:
                Bitmap b=facades[p.index];
                if (b!=null) {
                    paint.setColor(0xFFFFFFFF);
                    bounds.set(px-192*BUILDING_SCALE,py-264*BUILDING_SCALE,
                        px+192*BUILDING_SCALE,py+120*BUILDING_SCALE);
                    c.drawBitmap(b,null,bounds,paint);
                } else block(c,p,cx,cy,1.8f,0xFF6B5140,0xFFAA8060);
                label(c,names[p.index],x(p.x,p.y-p.h*.5f-.30f,cx,cy),
                    y(p.x,p.y-p.h*.5f-.30f,cx,cy)+17);
                break;
            case WALL: block(c,p,cx,cy,.72f,0xFF5E615B,0xFF919085);break;
            case GATE:
                block(c,p,cx,cy,1.34f,0xFF62442F,0xFF8D6B47);
                float xx=px+HALF_W, base=py, side=-HALF_W;
                paint.setColor(0xFF382A24);
                paint.setStrokeWidth(7f);
                c.drawLine(xx-2,base-18,xx+side+1,base+HALF_H-98,paint);
                c.drawLine(xx-2,base-94,xx+side+1,base+HALF_H-18,paint);
                paint.setStrokeWidth(1f);
                break;
            case BED:
                block(c,p,cx,cy,.24f,0xFF694631,0xFFAA8258);
                block(c,new Prop(Kind.BED,p.x+.10f,p.y,1.39f,.77f,0),
                    cx,cy,.34f,0xFF5B3448,0xFF8E5167);
                block(c,new Prop(Kind.BED,p.x-.55f,p.y,.38f,.70f,0),
                    cx,cy,.40f,0xFF988467,0xFFE6D6AC);
                label(c,"Bed · Wait",px,py-75);
                break;
            case TABLE: block(c,p,cx,cy,.48f,0xFF5E3623,0xFF9E6A3C);break;
            case BAR: block(c,p,cx,cy,.64f,0xFF4D2E20,0xFF825534);break;
            case BOARD:
                paint.setColor(0xFF503320);
                c.drawRect(px-31,py-83,px-25,py+1,paint);
                c.drawRect(px+25,py-83,px+31,py+1,paint);
                c.drawRect(px-37,py-92,px+37,py-34,paint);
                paint.setColor(0xFFE8D3A5);
                c.drawRect(px-23,py-81,px-3,py-43,paint);
                c.drawRect(px+4,py-77,px+22,py-48,paint);
                label(c,"Quest Board",px,py-103);
                break;
            case DUMMY:
                paint.setColor(0xFF543824);
                c.drawRect(px-4,py-79,px+4,py,paint);
                c.drawRect(px-31,py-72,px+31,py-65,paint);
                paint.setColor(0xFFAF8E55);
                c.drawOval(px-19,py-88,px+19,py-41,paint);
                c.drawCircle(px,py-98,13,paint);
                label(c,"Training Dummy",px,py-117);
                break;
        }
    }
    private void block(Canvas c,Prop p,float cx,float cy,float height,int side,int top) {
        float a=p.x-p.w*.5f,b=p.y-p.h*.5f,d=p.x+p.w*.5f,e=p.y+p.h*.5f;
        float ax=x(a,b,cx,cy),ay=y(a,b,cx,cy),dx=x(d,b,cx,cy),dy=y(d,b,cx,cy);
        float ex=x(d,e,cx,cy),ey=y(d,e,cx,cy),fx=x(a,e,cx,cy),fy=y(a,e,cx,cy);
        float z=height*HEIGHT_SCALE;
        paint.setColor(side);
        face(c,ax,ay-z,dx,dy-z,dx,dy,ax,ay);
        paint.setColor(side-0x00090404);
        face(c,dx,dy-z,ex,ey-z,ex,ey,dx,dy);
        paint.setColor(top);
        face(c,ax,ay-z,dx,dy-z,ex,ey-z,fx,fy-z);
    }
    private void label(Canvas c,String text,float x,float y) {
        paint.setTextSize(18);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(WildlandsUi.SERIF);
        float half=paint.measureText(text)*.5f+12;
        bounds.set(x-half,y-21,x+half,y+6);
        WildlandsUi.plate(c,paint,polygon,bounds,6,WildlandsUi.TEAL,WildlandsUi.GOLD,1.5f);
        paint.setColor(WildlandsUi.INK);
        c.drawText(text,x,y,paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }
    private void face(Canvas c,float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy) {
        polygon.reset();polygon.moveTo(ax,ay);polygon.lineTo(bx,by);
        polygon.lineTo(cx,cy);polygon.lineTo(dx,dy);polygon.close();
        c.drawPath(polygon,paint);
    }
}
