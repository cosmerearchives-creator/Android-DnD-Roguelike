package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;

/** Independent color and ear layers for the detailed Petite Pale sprite rig. */
final class PetiteAppearance {
    static final String[] EARS={"Rounded","Pointed elf"};
    static final String[] HAIR={"Dark brown","Black","Chestnut","Auburn","Silver"};
    static final String[] SKIN={"Original","Fair","Warm brown","Deep brown"};
    static final PetiteAppearance DEFAULT=new PetiteAppearance(EARS[0],HAIR[0],SKIN[0]);
    final String ears, hair, skin;
    private final int hairIndex, skinIndex;

    PetiteAppearance(String ears,String hair,String skin) {
        this.ears=EARS[index(EARS,ears)];
        hairIndex=index(HAIR,hair);
        skinIndex=index(SKIN,skin);
        this.hair=HAIR[hairIndex];
        this.skin=SKIN[skinIndex];
    }

    private static int index(String[] options,String value) {
        for (int i=0;i<options.length;i++) if (options[i].equals(value)) return i;
        return 0;
    }

    static Bitmap load(Context context,String path) {
        try (InputStream input=context.getAssets().open(path)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException ignored) {
            return null;
        }
    }

    Bitmap apply(Context context,Bitmap bitmap,String motion) {
        boolean pointed=ears.equals(EARS[1]);
        if (bitmap==null || (!pointed && hairIndex==0 && skinIndex==0)) return bitmap;
        Bitmap mask=load(context,"petite_modules/"+motion+".png");
        int width=bitmap.getWidth(),height=bitmap.getHeight();
        if (mask==null || mask.getWidth()!=width || mask.getHeight()!=height) return bitmap;
        int[] pixels=new int[width], weights=new int[width];
        Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        for (int y=0;y<height;y++) {
            bitmap.getPixels(pixels,0,width,0,y,width,1);
            mask.getPixels(weights,0,width,0,y,width,1);
            for (int x=0;x<width;x++) {
                int base=pixels[x], control=weights[x];
                int skinWeight=(control>>>16)&255, hairWeight=(control>>>8)&255;
                int earWeight=pointed ? control&255 : 0;
                if (skinWeight>12 && skinIndex!=0) base=mix(base,skinColor(base),skinWeight);
                if (hairWeight>12 && hairIndex!=0) base=mix(base,hairColor(base),hairWeight);
                if (earWeight>0) {
                    int shade=skinWeight>100 && (base>>>24)>200 ? base : skinColor(0xffba8a68);
                    int alpha=base>>>24;
                    int covered=alpha*(255-earWeight)/255;
                    int outAlpha=earWeight+covered;
                    base=mix(base,shade,earWeight*255/outAlpha)&0x00ffffff;
                    base|=outAlpha<<24;
                }
                pixels[x]=base;
            }
            result.setPixels(pixels,0,width,0,y,width,1);
        }
        mask.recycle();
        return result;
    }

    private int hairColor(int value) {
        int r=(value>>>16)&255,g=(value>>>8)&255,b=value&255;
        switch(hairIndex) {
            case 1: return color(value,r*65/100,g*69/100,b*77/100);
            case 2: return color(value,r*142/100+17,g*112/100+7,b);
            case 3: return color(value,r*164/100+27,g*114/100+8,b*86/100);
            case 4:
                int light=(r*3+g*6+b)/10*2+60;
                return color(value,light,light+3,light+9);
            default: return value;
        }
    }

    private int skinColor(int value) {
        int r=(value>>>16)&255,g=(value>>>8)&255,b=value&255;
        switch(skinIndex) {
            case 1: return color(value,r+24,g+22,b+21);
            case 2: return color(value,r*81/100,g*72/100,b*66/100);
            case 3: return color(value,r*56/100,g*45/100,b*43/100);
            default: return value;
        }
    }

    private static int color(int alpha,int r,int g,int b) {
        return (alpha&0xff000000)|(Math.min(255,r)<<16)|(Math.min(255,g)<<8)|Math.min(255,b);
    }

    private static int mix(int from,int to,int weight) {
        int result=0;
        for (int shift=0;shift<32;shift+=8) {
            int start=(from>>>shift)&255,end=(to>>>shift)&255;
            result|=((start*(255-weight)+end*weight+127)/255)<<shift;
        }
        return result;
    }
}
