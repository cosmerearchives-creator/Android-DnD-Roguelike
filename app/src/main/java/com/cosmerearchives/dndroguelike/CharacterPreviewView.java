package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Lightweight, original drawn character paper-doll; choices visibly change this preview. */
final class CharacterPreviewView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private PlayerCharacter hero;
    private boolean topDown;
    private int direction=0, animation=0;
    private long animationStart=android.os.SystemClock.uptimeMillis();
    CharacterPreviewView(Context context, PlayerCharacter h){super(context);hero=h;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void character(PlayerCharacter h){hero=h;invalidate();}
    void mode(boolean value){topDown=value;invalidate();}
    boolean mode(){return topDown;}
    void direction(int value){direction=((value%4)+4)%4;invalidate();}
    int direction(){return direction;}
    void animation(int value){animation=value;animationStart=android.os.SystemClock.uptimeMillis();invalidate();}
    int animation(){return animation;}
    private void fill(Canvas c,int col,float l,float t,float r,float b,float rad){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawRoundRect(l,t,r,b,rad,rad,p);
    }
    private void oval(Canvas c,int col,float l,float t,float r,float b){
        p.setShader(null);p.setColor(col);p.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,p);
    }
    private void line(Canvas c,int col,float width,float x,float y,float xx,float yy){
        p.setShader(null);p.setColor(col);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);
    }
    private void polygon(Canvas c,int color,float... xy){
        Path path=new Path();path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();
        p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);
    }
    private int palette(String value,int[] colors,String[] options,int fallback){
        for(int i=0;i<options.length;i++)if(options[i].equals(value))return colors[i];return fallback;
    }
    private int skin(){
        return palette(hero.skin,new int[]{0xFFECCBB1,0xFFE2B89E,0xFFC89473,0xFFAC815C,0xFF885A41,0xFF5A392C},
            new String[]{"Fair","Light","Warm","Olive","Brown","Deep"},0xFFECCBB1);
    }
    private int hair(){
        return palette(hero.hair,new int[]{0xFFCDD7DD,0xFFE9E8E0,0xFFD8B878,0xFF694938,0xFF292D36,0xFF9E4E38},
            new String[]{"Silver","White","Blonde","Brown","Black","Red"},0xFFCDD7DD);
    }
    private int cloth(){
        return palette(hero.clothingColor,new int[]{0xFF5278A5,0xFF487C67,0xFFA25052,0xFF79629B,0xFF303A47,0xFFD5D2C6},
            new String[]{"Blue","Green","Red","Purple","Black","White"},0xFF5278A5);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(hero==null)return;
        float w=getWidth(),h=getHeight();
        p.setShader(new LinearGradient(0,0,w,h,0xFF1C2D3B,0xFF0C151F,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,p);p.setShader(null);
        float size=Math.min(w,h);
        canvas.save();canvas.translate(w/2,h/2);canvas.scale(size/320f,size/320f);
        // A subtle staged character card, not a screenshot of the gameplay renderer.
        for(int i=0;i<6;i++){p.setColor(0x1828B7B5);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.6f);
            canvas.drawCircle(0,5,65+i*19,p);}
        p.setStyle(Paint.Style.FILL);
        oval(canvas,0x664BA5B1,-105,116,105,144);
        long elapsed=android.os.SystemClock.uptimeMillis()-animationStart;
        float bob=animation==1?(float)Math.sin(elapsed/135.0)*4f:animation==2?(float)Math.sin(elapsed/190.0)*1.5f:0;
        boolean back=direction==2,side=direction==1||direction==3;
        boolean plate=hero.armorChoice.equals("chain_mail");
        boolean shield=hero.martialTwo.equals("shield");
        Equipment.Item weapon=Equipment.get(hero.martialOne);
        boolean twoHanded=weapon!=null&&weapon.twoHanded;
        int clothColor=cloth(),body=skin(),hairColor=hair();
        canvas.save();canvas.translate(0,bob);
        if(topDown){canvas.scale(.83f,.7f);canvas.translate(0,20);}
        // Ground shadows and feet.
        oval(canvas,0xAA060B10,-45,114,46,130);
        fill(canvas,0xFF293748,-31,69,-9,114,8);
        fill(canvas,0xFF293748,9,69,31,114,8);
        fill(canvas,0xFF171E28,-37,105,-7,121,6);
        fill(canvas,0xFF171E28,7,105,37,121,6);
        polygon(canvas,0xFF283343,-42,-34,42,-34,37,79,-37,79);
        polygon(canvas,clothColor,-31,-28,31,-28,36,66,-35,66);
        fill(canvas,0xFF273646,-29,61,29,72,5);
        if(plate){
            polygon(canvas,0xFF8196A0,-40,-38,40,-38,34,35,-33,35);
            polygon(canvas,0xFF506573,-29,-20,29,-20,24,17,-24,17);
            line(canvas,0xFFA6B6BE,3,-28,-12,28,-12);
            line(canvas,0xFF9DABB3,3,0,-27,0,28);
            for(int i=0;i<4;i++)line(canvas,0xFF465A66,2,-30,27+i*8,30,27+i*8);
        }else{
            polygon(canvas,0xFF4B5B65,-38,-34,38,-34,30,18,-29,18);
            line(canvas,0xFFBCA16E,4,-25,-17,25,16);
        }
        // Arms, gloves and chosen weapon.
        fill(canvas,plate?0xFF758B99:clothColor,-51,-30,-34,47,9);
        fill(canvas,plate?0xFF758B99:clothColor,34,-30,51,47,9);
        fill(canvas,0xFF323C48,-50,40,-33,52,6);
        fill(canvas,0xFF323C48,33,40,50,52,6);
        if(shield){
            polygon(canvas,0xFFB49A69,-75,-11,-38,-16,-39,56,-56,71,-75,45);
            polygon(canvas,0xFF526D7E,-70,-4,-44,-8,-45,49,-55,60,-69,39);
            line(canvas,0xFFE0C68D,3,-60,-4,-60,54);
        }
        if(weapon!=null){
            float attackOffset=animation==2?(float)Math.sin(Math.min(1.0,elapsed/330.0)*Math.PI)*35:0;
            canvas.save();canvas.translate(50,-10);canvas.rotate(attackOffset);
            if(weapon.ranged){
                line(canvas,0xFF8B694B,5,0,-62,0,65);
                line(canvas,0xFFE1D4B3,2,0,-62,9,0);
                line(canvas,0xFFE1D4B3,2,9,0,0,65);
            }else{
                line(canvas,0xFF422D26,7,0,31,0,73);
                line(canvas,0xFFE3E2DB,weapon.properties.contains("Heavy")?10:6,0,-62,0,30);
                polygon(canvas,0xFFE0E9E8,-5,-60,0,-82,5,-60);
                line(canvas,0xFFD0B47B,5,-16,29,16,29);
            }
            canvas.restore();
        }
        // Neck and pointed elf ears.
        fill(canvas,body,-10,-65,10,-37,8);
        polygon(canvas,body,-26,-92,-56,-109,-35,-71);
        polygon(canvas,body,26,-92,56,-109,35,-71);
        oval(canvas,body,-35,-118,35,-54);
        if(side){oval(canvas,0xFFB8896E,0,-106,28,-66);}
        if(!back){
            oval(canvas,0xFF243F50,side?1:-21,-88,side?9:-13,-80);
            if(!side)oval(canvas,0xFF243F50,13,-88,21,-80);
            line(canvas,0xFF9C674F,2,side?4:-8,-68,side?15:8,-68);
        }
        polygon(canvas,hairColor,-38,-110,-25,-129,2,-139,27,-128,39,-109,29,-106,15,-118,-16,-115,-29,-103);
        if(back){polygon(canvas,hairColor,-31,-110,32,-110,35,-42,-37,-42);}
        else{
            polygon(canvas,hairColor,-33,-109,-24,-120,-17,-100,-27,-79,-41,-41);
            if(!side)polygon(canvas,hairColor,28,-119,38,-107,38,-49,24,-76);
        }
        canvas.restore();
        if(animation!=0){postInvalidateDelayed(32);}
        canvas.restore();
    }
}
