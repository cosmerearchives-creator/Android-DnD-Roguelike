package com.cosmerearchives.dndroguelike;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * v0.1.2 artwork facade. All draw methods use world coordinates.
 * Future bitmap/AI artwork can replace these renderers without affecting physics.
 * Units have a 48x70 visual footprint but retain their independent game hit boxes.
 */
final class FantasyArt {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int OUTLINE=0xFF24262B;
    private static final int STONE=0xFF626A68;
    private static final int MORTAR=0xFF2F3938;
    private static final int GOLD=0xFFD3AB69;

    private void oval(Canvas c,float l,float t,float r,float b,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setShader(null);
        c.drawOval(l,t,r,b,paint);
    }
    private void rect(Canvas c,float l,float t,float r,float b,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setShader(null);
        c.drawRect(l,t,r,b,paint);
    }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setShader(null);
        c.drawRoundRect(l,t,r,b,radius,radius,paint);
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(color);paint.setShader(null);
        c.drawLine(x1,y1,x2,y2,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void polygon(Canvas c,int color,float... xy){
        Path path=new Path();path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);
        path.close();paint.setColor(color);paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        c.drawPath(path,paint);
    }
    private void orb(Canvas c,float x,float y,float r,int color){
        paint.setColor(color);paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        c.drawCircle(x,y,r,paint);
    }
    void ground(Canvas c,int width,int height){
        rect(c,0,0,width,height,0xFF424E47);
        for(int y=0;y<height;y+=80){
            for(int x=0;x<width;x+=80){
                int offset=((x/80+y/80)%3)*2;
                rect(c,x+3,y+3,x+77,y+76,Color.rgb(76+offset,85+offset,78+offset));
                rect(c,x+7,y+7,x+73,y+10,0xFF606D62);
                line(c,x+16,y+63,x+32,y+61,0xFF586359,2);
                if(((x/80)*17+(y/80)*31)%7==0){
                    line(c,x+45,y+21,x+56,y+32,0xFF313C37,2);
                    line(c,x+56,y+32,x+52,y+44,0xFF313C37,2);
                }
                if(((x/80)*13+(y/80)*11)%9==0){
                    orb(c,x+30,y+37,3,0xFF6D785E);
                    orb(c,x+38,y+40,2,0xFF6D785E);
                }
            }
        }
        // Dark edging for a room-like arena, drawn independently of collision geometry.
        rect(c,0,0,width,22,0xFF303A36);
        rect(c,0,height-22,width,height,0xFF303A36);
        rect(c,0,0,22,height,0xFF303A36);
        rect(c,width-22,0,width,height,0xFF303A36);
    }
    void wall(Canvas c,RectF wall){
        rect(c,wall.left+9,wall.top+15,wall.right+13,wall.bottom+16,0x8830211B);
        rect(c,wall.left,wall.top,wall.right,wall.bottom,0xFF303B3B);
        rect(c,wall.left+5,wall.top+5,wall.right-5,wall.bottom-5,STONE);
        for(int row=0;row<2;row++){
            float y=wall.top+6+row*(wall.height()-12)/2f;
            float h=(wall.height()-15)/2f;
            for(float x=wall.left+6;x<wall.right-6;x+=48){
                float rr=Math.min(x+45,wall.right-6);
                rect(c,x+1,y+1,rr,y+h,((int)(x+row*19)/48)%2==0?0xFF777E78:0xFF626E6B);
                line(c,x+3,y+4,rr-3,y+4,0xFF92958B,2);
            }
        }
        line(c,wall.left,wall.bottom,wall.right,wall.bottom,MORTAR,6);
    }
    void crate(Canvas c,float x,float y){
        oval(c,x-23,y+14,x+25,y+24,0x77302219);
        round(c,x-24,y-21,x+23,y+19,3,OUTLINE);
        rect(c,x-19,y-17,x+19,y+15,0xFF8A6543);
        rect(c,x-19,y-17,x+19,y-11,0xFFA17D53);
        line(c,x-15,y-13,x+15,y+11,0xFF5B432F,5);
        line(c,x+15,y-13,x-15,y+11,0xFF5B432F,5);
        line(c,x-22,y-13,x+22,y-13,0xFFC39B65,4);
        line(c,x-22,y+13,x+22,y+13,0xFFC39B65,4);
    }
    void barrel(Canvas c,float x,float y){
        oval(c,x-23,y+11,x+24,y+24,0x77302219);
        oval(c,x-21,y-23,x+21,y-9,0xFF24292A);
        round(c,x-23,y-15,x+23,y+17,8,0xFF71503A);
        rect(c,x-22,y-6,x+22,y-1,0xFF353A3B);
        rect(c,x-22,y+10,x+22,y+14,0xFF353A3B);
        oval(c,x-22,y-23,x+22,y-8,0xFF98704C);
        oval(c,x-12,y-19,x+12,y-13,0xFF63432F);
    }
    void torch(Canvas c,float x,float y,long tick){
        line(c,x,y+12,x,y+40,0xFF312923,8);
        line(c,x,y+9,x,y+36,0xFFA67A49,4);
        float flicker=(float)Math.sin(tick*.008+x)*3;
        orb(c,x,y-7,20+flicker,0x4475BA43);
        polygon(c,0xFFE88B3C,x-10,y+4,x-3,y-24+flicker,x+4,y-13,x+11,y+4);
        polygon(c,0xFFFFD078,x-5,y+4,x,y-12+flicker,x+6,y+4);
    }
    void character(Canvas canvas,float x,float y,int kind,boolean alive,long tick){
        canvas.save();canvas.translate(x,y);
        float bob=alive?(float)Math.sin(tick*.004f+kind)*1.5f:0;
        if(!alive){
            canvas.rotate(75);paint.setAlpha(100);
        }
        oval(canvas,-26,24,27,35,0x77212324);
        canvas.translate(0,bob);
        int coat=kind==0?0xFF497D8E:kind==1?0xFF79816C:0xFF544D87;
        int trim=kind==0?GOLD:kind==1?0xFFB7BEC0:0xFFBA92DA;
        int skin=0xFFCDA785;
        // Boots, cape and body layers remain independently rendered.
        if(kind==2)polygon(canvas,0xFF373B68,-24,-9,0,-25,23,-9,30,29,-30,29);
        else polygon(canvas,0xFF464A43,-23,-12,0,-23,23,-12,20,27,-20,27);
        round(canvas,-19,17,-4,31,4,0xFF3C3332);
        round(canvas,5,17,20,31,4,0xFF3C3332);
        oval(canvas,-28,-10,-13,17,OUTLINE);
        oval(canvas,14,-10,28,17,OUTLINE);
        oval(canvas,-24,-6,-15,14,coat);
        oval(canvas,15,-6,24,14,coat);
        polygon(canvas,OUTLINE,-21,-18,0,-25,22,-18,21,20,-21,20);
        polygon(canvas,coat,-17,-16,0,-21,18,-16,17,17,-17,17);
        line(canvas,-15,6,15,6,trim,4);
        rect(canvas,-5,-15,5,17,kind==2?0xFF745CA0:trim);
        if(kind==1){
            // Heavy armor with rivets and broad shoulder plates.
            oval(canvas,-29,-16,-11,3,0xFFB1B4A9);
            oval(canvas,10,-16,29,3,0xFFB1B4A9);
            line(canvas,-17,-13,17,-13,0xFFE0DED1,5);
            orb(canvas,-12,9,3,0xFFC7CBBD);orb(canvas,12,9,3,0xFFC7CBBD);
        }
        // Neck/head, stylized from a near-overhead camera angle.
        oval(canvas,-16,-34,17,-10,OUTLINE);
        oval(canvas,-13,-32,14,-12,skin);
        if(kind==0){
            polygon(canvas,0xFF5B4130,-15,-25,-13,-36,4,-39,15,-29,14,-24,1,-30);
            line(canvas,-8,-16,7,-16,0xFF7B5039,3);
        }else if(kind==1){
            polygon(canvas,0xFFA2AAA8,-18,-24,-13,-39,9,-39,18,-24,14,-16,-14,-16);
            line(canvas,-12,-27,12,-27,0xFFE1DED0,3);
            line(canvas,0,-37,0,-18,0xFF505E62,5);
        }else{
            polygon(canvas,0xFF313959,-18,-17,-16,-36,0,-43,16,-36,18,-17,7,-27,-7,-27);
            oval(canvas,-11,-27,11,-14,0xFFC8A4A0);
            orb(canvas,-5,-21,2,0xFFBBDFFF);orb(canvas,6,-21,2,0xFFBBDFFF);
        }
        if(kind==0){
            // Compact sword at the right and a round shield on the left.
            line(canvas,23,8,30,-19,0xFFDDD8C7,7);
            line(canvas,20,-1,34,3,0xFF9A7549,5);
            oval(canvas,-38,-4,-16,20,OUTLINE);
            oval(canvas,-34,0,-19,16,0xFFA37A50);
            orb(canvas,-27,8,5,0xFFCFB579);
        }else if(kind==1){
            // Wider shield and warhammer, no artificial extended hitbox.
            round(canvas,-39,-9,-18,22,4,0xFF687C86);
            line(canvas,-34,0,-22,0,0xFFB6C7C5,4);
            line(canvas,25,13,32,-24,0xFF72523C,5);
            round(canvas,21,-29,43,-17,3,0xFFB6BCB6);
        }else{
            // Long staff, gem and a tiny magical aura.
            line(canvas,28,23,32,-45,0xFF6B503D,5);
            orb(canvas,32,-47,10,0xFF2A788F);
            orb(canvas,32,-49,6,0xFF9AD5EB);
            orb(canvas,-24,3,5,0xFF997BC9);
        }
        canvas.restore();
    }
    void goblin(Canvas canvas,float x,float y,int variant,boolean alerted,long tick){
        canvas.save();canvas.translate(x,y);
        oval(canvas,-23,23,23,32,0x77212122);
        int skin=variant%2==0?0xFF78975E:0xFF86A266;
        polygon(canvas,0xFF58473B,-17,-8,0,-23,18,-8,21,22,-19,22);
        oval(canvas,-17,-10,18,20,0xFF73533D);
        line(canvas,-5,4,5,4,0xFFAE8D5C,4);
        oval(canvas,-18,-32,19,-8,OUTLINE);
        oval(canvas,-15,-29,16,-10,skin);
        polygon(canvas,skin,-13,-21,-29,-29,-24,-8,-12,-12);
        polygon(canvas,skin,14,-21,29,-29,24,-8,13,-12);
        polygon(canvas,0xFF2E4331,-17,-22,0,-39,18,-22,6,-27,-8,-27);
        orb(canvas,-6,-18,4,alerted?0xFFFFC76C:0xFFFFE7A0);
        orb(canvas,7,-18,4,alerted?0xFFFFC76C:0xFFFFE7A0);
        line(canvas,-4,-11,6,-11,0xFF4F3129,3);
        // Jagged cleaver is visibly a close-range weapon.
        line(canvas,20,5,26,-9,0xFF77523C,5);
        polygon(canvas,0xFFB7B9B0,24,-7,19,-20,36,-27,38,-15);
        oval(canvas,-27,-1,-13,14,0xFF614C39);
        canvas.restore();
    }
    void ring(Canvas c,float x,float y,int color,float radius){
        paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);c.drawOval(x-radius,y+16-radius*.38f,x+radius,y+16+radius*.38f,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    void hit(Canvas c,float x,float y,float intensity,boolean magic){
        int color=magic?0xFF9ED5F6:0xFFFFD99B;
        paint.setStyle(Paint.Style.STROKE);paint.setColor(color);
        paint.setStrokeWidth(3+intensity*3);
        c.drawCircle(x,y,22+intensity*13,paint);
        paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<5;i++){
            double a=i*Math.PI*2/5;
            line(c,x+(float)Math.cos(a)*12,y+(float)Math.sin(a)*12,
                x+(float)Math.cos(a)*34,y+(float)Math.sin(a)*34,color,3);
        }
    }
}
