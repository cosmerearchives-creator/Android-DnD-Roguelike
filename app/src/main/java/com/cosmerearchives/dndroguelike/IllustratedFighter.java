package com.cosmerearchives.dndroguelike;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.content.Context;

/**
 * Original layered illustration prototype. Geometry and animation are separate from
 * world simulation, so these layers can later be replaced with authored sprite atlases.
 * Coordinates are local to the character's feet; forward is negative screen Y.
 */
final class IllustratedFighter {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final PlayerCharacter hero;
    private final Bitmap spriteAtlas;
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    IllustratedFighter(Context context, PlayerCharacter hero){
        this.hero=hero;
        Bitmap art=null;
        try(java.io.InputStream stream=context.getAssets().open("elf_fighter_atlas.webp")){
            art=BitmapFactory.decodeStream(stream);
        }catch(java.io.IOException ignored){
            // Retain original vector fallback until artwork is packaged.
        }
        spriteAtlas=art;
    }
    boolean hasIllustratedAtlas(){return spriteAtlas!=null;}
    private void poly(Canvas c,int color,float... coords){
        path.reset();path.moveTo(coords[0],coords[1]);
        for(int i=2;i<coords.length;i+=2)path.lineTo(coords[i],coords[i+1]);
        path.close();p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(color);
        c.drawPath(path,p);
    }
    private void oval(Canvas c,int color,float a,float b,float d,float e){
        p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(a,b,d,e,p);
    }
    private void line(Canvas c,int color,float width,float ax,float ay,float bx,float by){
        p.setShader(null);p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);c.drawLine(ax,ay,bx,by,p);p.setStyle(Paint.Style.FILL);
    }
    private void metal(Canvas c,float l,float t,float r,float b,int top,int bottom){
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(l,t,r,b,top,bottom,Shader.TileMode.CLAMP));
        c.drawRoundRect(l,t,r,b,4,4,p);p.setShader(null);
    }
    private int select(String choice,String[] names,int[] colors,int fallback){
        for(int k=0;k<names.length;k++)if(names[k].equals(choice))return colors[k];return fallback;
    }
    private int hair(){return hero==null?0xFFE2C78A:select(hero.hair,
        new String[]{"Silver","White","Blonde","Brown","Black","Red"},
        new int[]{0xFFC6D0D6,0xFFEAE4D4,0xFFE0BC78,0xFF685044,0xFF302C35,0xFF994D36},0xFFE2C78A);}
    private int skin(){return hero==null?0xFFDFB99B:select(hero.skin,
        new String[]{"Fair","Light","Warm","Olive","Brown","Deep"},
        new int[]{0xFFEFC7AD,0xFFE2B89E,0xFFC89473,0xFFAC815C,0xFF885A41,0xFF5A392C},0xFFE2B89E);}
    private int tunic(){return hero==null?0xFF48678B:select(hero.clothingColor,
        new String[]{"Blue","Green","Red","Purple","Black","White"},
        new int[]{0xFF42678E,0xFF3C6E56,0xFF9A4548,0xFF6D568D,0xFF333C47,0xFFCED0CA},0xFF42678E);}
    private boolean shield(){return hero==null||"shield".equals(hero.martialTwo);}
    private boolean plate(){return hero==null||"chain_mail".equals(hero.armorChoice);}
    /**
     * facing: 0 front, 1 right, 2 back, 3 left. phase 0 idle, 1 walking, 2 attack.
     * drawn at width ~62 and height ~118 world-pixels, with equipment extending beyond.
     */
    void draw(Canvas canvas,float footX,float footY,float scale,int facing,float elapsed,int phase){
        Canvas c=canvas;
        if(spriteAtlas!=null){
            // Four 160x210 frames: front, left, back, right.
            int[] order={0,3,2,1};
            int index=order[Math.max(0,Math.min(3,facing))];
            int fw=spriteAtlas.getWidth()/4,fh=spriteAtlas.getHeight();
            Rect src=new Rect(index*fw,0,(index+1)*fw,fh);
            float width=125f*scale,height=166f*scale; // Larger, less compressed feminine silhouette.
            float bounce=phase==1?(float)Math.sin(elapsed*10f)*1.2f*scale:0f;
            RectF dst=new RectF(footX-width*.5f,footY-height+bounce,footX+width*.5f,footY+bounce);
            spritePaint.setAlpha(255);
            spritePaint.setFilterBitmap(true);
            c.drawBitmap(spriteAtlas,src,dst,spritePaint);
            return;
        }
        c.save();c.translate(footX,footY);c.scale(scale,scale);
        final float step=phase==1?(float)Math.sin(elapsed*10)*5f:0;
        final float breathe=(float)Math.sin(elapsed*2f)*1.4f;
        final int cloth=tunic(),skin=skin(),hair=hair(),gold=0xFFD6AE66;
        final boolean side=facing==1||facing==3,back=facing==2;
        oval(c,0x77070E10,-21,-7,22,6);
        c.save();c.translate(0,breathe);
        if(facing==3)c.scale(-1,1);
        if(side)c.scale(.82f,1f);
        // Cloth silhouette and long layered cloak.
        poly(c,0xFF192C3C,-16,-94,15,-94,20,-50,29,-19,17,-13,-23,-17,-20,-52);
        poly(c,cloth,-17,-91,16,-91,21,-39,-7,-17,-22,-23);
        poly(c,0xFF203947,-11,-63,11,-63,18,-24,-10,-24);
        line(c,0xFFB89C64,1.8f,-9,-56,14,-25);
        // Boot and leg armor, with a small gait cycle.
        c.save();c.translate(-10,0);c.rotate(step,-1,-25);
        poly(c,0xFF232B32,-8,-44,6,-44,8,-5,-8,-5);
        metal(c,-9,-30,7,-6,0xFF8B99A6,0xFF354450);
        line(c,0xFFCFBE93,2,-6,-26,5,-26);
        poly(c,0xFF272829,-12,-8,9,-8,12,1,-14,1);c.restore();
        c.save();c.translate(10,0);c.rotate(-step,1,-25);
        poly(c,0xFF232B32,-7,-44,8,-44,8,-5,-8,-5);
        metal(c,-8,-30,9,-6,0xFF778A96,0xFF374653);
        line(c,0xFFCFBE93,2,-5,-26,6,-26);
        poly(c,0xFF272829,-9,-8,12,-8,14,1,-12,1);c.restore();
        // Long armor skirt panels.
        poly(c,0xFF374D5B,-19,-60,18,-60,18,-35,9,-28,6,-42,-6,-42,-9,-30,-20,-38);
        poly(c,cloth,-10,-58,10,-58,11,-28,0,-24,-12,-30);
        line(c,gold,1.7f,0,-52,0,-28);
        // Chest plate with neck trim and central ornament.
        poly(c,0xFF171E27,-24,-105,24,-105,23,-64,17,-53,-17,-53,-23,-64);
        poly(c,0xFF8395A1,-21,-103,21,-103,18,-64,0,-58,-19,-64);
        poly(c,0xFF5B6F83,-18,-99,0,-93,18,-99,15,-76,0,-69,-16,-76);
        poly(c,0xFFADB9B9,-14,-99,0,-94,14,-99,9,-91,0,-88,-9,-91);
        poly(c,0xFF2E4153,-12,-83,0,-75,12,-83,8,-65,0,-62,-8,-65);
        line(c,gold,2.5f,-19,-63,18,-63);
        line(c,0xFFEAD9B4,1,-10,-81,0,-74);
        line(c,0xFFBBA77D,1,0,-74,10,-81);
        oval(c,0xFFDBC17E,-3,-73,3,-66);
        // Silver-trimmed shoulder pauldrons, arm guards and gloves.
        metal(c,-34,-102,-18,-86,0xFFB8C2BE,0xFF566879);
        metal(c,18,-102,34,-86,0xFFC0C6C4,0xFF5A6F7B);
        line(c,gold,2,-32,-90,-19,-91);line(c,gold,2,19,-91,31,-90);
        poly(c,0xFF536678,-31,-87,-20,-88,-17,-60,-25,-56,-36,-67);
        poly(c,0xFF536678,20,-88,31,-87,36,-67,25,-56,17,-60);
        line(c,0xFFE0D8B8,2,-30,-79,-26,-63);
        line(c,0xFFE0D8B8,2,30,-79,26,-63);
        oval(c,0xFF373234,-31,-61,-19,-47);
        oval(c,0xFF373234,19,-61,31,-47);
        // Back view maintains the cloak and hair but hides facial details.
        if(back){
            poly(c,cloth,-19,-93,19,-93,23,-25,0,-17,-24,-25);
            line(c,gold,1.7f,-8,-72,-8,-26);
            line(c,gold,1.7f,8,-72,8,-26);
        }
        // Long neck, angular ears, face and swept hair.
        poly(c,skin,-8,-109,8,-109,8,-99,-8,-99);
        oval(c,0xFF5B4231,-13,-143,14,-111);
        poly(c,skin,-11,-139,11,-139,12,-121,6,-110,-6,-110,-12,-121);
        poly(c,skin,-10,-124,-27,-132,-13,-115);
        poly(c,skin,10,-124,27,-132,13,-115);
        if(!back){
            poly(c,0xFF5B4640,-9,-126,-3,-127,-2,-123,-9,-123);
            poly(c,0xFF5B4640,3,-127,9,-126,9,-123,2,-123);
            line(c,0xFF4F5754,1,-6,-119,-2,-119);
            line(c,0xFF4F5754,1,2,-119,7,-119);
            line(c,0xFF8C5E53,1,-3,-113,5,-113);
        }
        poly(c,hair,-14,-130,-10,-148,0,-155,12,-148,16,-134,9,-138,5,-147,-2,-143,-9,-134);
        poly(c,hair,9,-144,18,-136,19,-110,15,-102,12,-126);
        poly(c,0xFFBA9A69,-7,-144,-1,-152,7,-148,3,-142);
        if(back){poly(c,hair,-13,-146,13,-146,17,-107,3,-95,-15,-109);}
        // Shield and weapon are separate equipment layers, to be replaced with texture sprites.
        if(shield()){
            poly(c,0xFF302D2A,-43,-83,-19,-90,-17,-52,-28,-28,-46,-53);
            poly(c,0xFF7C8796,-42,-82,-22,-87,-20,-54,-28,-34,-44,-55);
            poly(c,0xFF3E5A76,-38,-79,-25,-81,-24,-55,-28,-40,-40,-56);
            line(c,gold,2.3f,-32,-80,-31,-43);
            line(c,gold,2.3f,-39,-59,-23,-61);
            oval(c,0xFFE7C67E,-35,-66,-28,-60);
        }
        float attack=phase==2?(float)Math.sin(Math.min(1,elapsed*2.2f)*Math.PI)*48:0;
        c.save();c.rotate(-attack,26,-59);
        line(c,0xFF4B362B,5,28,-51,35,-18);
        line(c,0xFFD1B17E,3,26,-63,39,-61);
        poly(c,0xFFDEE5E5,30,-65,34,-114,38,-132,41,-111,37,-65);
        line(c,0xFFFFFFFF,1,37,-123,34,-76);
        c.restore();
        // Armor edge highlights.
        line(c,0xFFE3D4AB,1,-17,-100,-18,-67);
        line(c,0xFFE3D4AB,1,17,-100,18,-67);
        c.restore();c.restore();
    }
}
