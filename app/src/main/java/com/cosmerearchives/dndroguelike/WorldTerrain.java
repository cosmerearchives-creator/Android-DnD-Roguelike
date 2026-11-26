package com.cosmerearchives.dndroguelike;

/** Deterministic terrain matching the original OutdoorActivity noise and biome rules. */
final class WorldTerrain {
    private long hash(int x,int y,int salt) {
        long z=((long)x*0x9e3779b97f4a7c15L)^((long)y*0xbf58476d1ce4e5b9L)
            ^(salt*0x94d049bb133111ebL);
        z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;
        z=(z^(z>>>27))*0x94d049bb133111ebL;
        return z^(z>>>31);
    }
    private float noise(int x,int y,int salt) {
        return (hash(x,y,salt)>>>11)/(float)(1L<<53);
    }
    private float smooth(float x) { return x*x*(3-2*x); }
    private float field(float x,float y,int scale,int salt) {
        float px=x/scale,py=y/scale;
        int ix=(int)Math.floor(px),iy=(int)Math.floor(py);
        float a=smooth(px-ix),b=smooth(py-iy);
        float v0=noise(ix,iy,salt)*(1-a)+noise(ix+1,iy,salt)*a;
        float v1=noise(ix,iy+1,salt)*(1-a)+noise(ix+1,iy+1,salt)*a;
        return v0*(1-b)+v1*b;
    }
    boolean river(int x,int y) {
        if(Math.abs(x)<7&&Math.abs(y)<7)return false;
        float meander=(field(y,0,17,51)-.5f)*13f;
        return Math.abs(x-(23+meander))<1.5f;
    }
    boolean road(int x,int y) {
        if(Math.abs(x)<7&&Math.abs(y)<7)return y==0;
        float bend=(field(x,0,23,19)-.5f)*7f;
        return Math.abs(y-bend)<1.25f;
    }
    int height(int x,int y) {
        if(Math.abs(x)<8&&Math.abs(y)<8)return 0;
        float level=field(x,y,16,37)*1.15f+field(x,y,7,39)*.35f;
        return level>.93f?2:level>.58f?1:0;
    }
    int biome(int x,int y) {
        if(river(x,y))return 5;
        int level=height(x,y);
        if(level==2)return 4;
        float wet=field(x,y,29,12),temp=field(x+300,y-600,31,17);
        if(wet>.7f)return 2;
        if(wet<.36f)return 3;
        return temp>.55f?0:1;
    }
    /** Sparse persistent world landmarks: 1 = hamlet, 2 = ancient ruins, 3 = wilderness encounter. */
    int landmark(int x,int y) {
        if (Math.abs(x)<9&&Math.abs(y)<9) return 0;
        int cx=Math.floorDiv(x,24),cy=Math.floorDiv(y,24);
        int lx=Math.floorMod(x,24),ly=Math.floorMod(y,24);
        if(lx!=12||ly!=12)return 0;
        long h=hash(cx,cy,111);
        if((h&7)==0)return 1;
        if((h&7)==1)return 2;
        if((h&7)==2)return 3;
        return 0;
    }
    int prop(int x,int y) {
        int landmark=landmark(x,y);
        if(landmark==1)return 4;
        if(landmark==2)return 0;
        if(landmark==3)return 3;
        if((x==13&&y==-11)||(x==-19&&y==12)||(x==30&&y==25))return 0;
        if(Math.abs(x)<7&&Math.abs(y)<7||road(x,y)||river(x,y))return -1;
        long seed=hash(x,y,83);
        int kind=biome(x,y);
        // Denser woodland: approximately one tree in four forest tiles,
        // and one in eight temperate grassland tiles. Preserve clear roads,
        // rivers and the starting area for movement.
        if((kind==1||kind==2) && (seed&3)==0)return (seed&64)==0?1:2;
        if(kind==0 && (seed&7)==4)return (seed&64)==0?1:2;
        if((seed&31)==15&&height(x,y)>0)return 3;
        if((seed&31)==22&&kind<=2)return 4;
        return -1;
    }
    boolean blocked(int x,int y) {
        if(landmark(x,y)!=0)return true;
        if(road(x,y))return false;
        if(river(x,y))return true;
        if(Math.abs(x)<6&&Math.abs(y)<6)return false;
        long seed=hash(x,y,83);
        int kind=biome(x,y);
        return ((seed&31)==4&&(kind==1||kind==2))
            ||((seed&63)==15&&height(x,y)>0);
    }
    int art(int x,int y) {
        int kind=biome(x,y),level=height(x,y);
        long seed=hash(x,y,27);
        if(road(x,y)&&river(x,y))return 11;
        if(kind==5)return 10;
        float variation=field(x,y,8,67);
        if(road(x,y)) {
            if(Math.abs(x)>=8&&variation>.68f)return level==2?6:9;
            return (seed&3)==0?5:4;
        }
        if(kind==4)return variation>.69f?7:variation<.31f?15:8;
        if(kind==3)return 3;
        if(kind==2)return variation>.65f?14:(seed&3)==0?12:2;
        if(kind==1)return variation<.30f?13:(seed&3)==0?1:0;
        return (seed&7)==0?12:0;
    }
    /** Broad, smoothly blended biome regions: meadow, grassland, woodland, ridge. */
    int wildernessBiome(int x,int y) {
        float forest=field(x,y,25,301)*.75f+field(x,y,10,302)*.25f;
        float ridge=field(x+900,y-400,36,303);
        if(ridge>.77f)return 3;
        if(forest>.64f)return 2;
        if(forest>.47f)return 1;
        return 0;
    }
    // Sample continuous terrain colors in world space so neighboring cells share edges.
    int landscapeColor(float x,float y) {
        float broad=field(x,y,13,314);
        float grain=field(x,y,3,313);
        int grass=mix(0xFF456126,0xFF91AE58,broad*.68f+grain*.32f);
        float ridge=field(x+900,y-400,36,303);
        grass=mix(grass,0xFF85816C,smooth(clamp((ridge-.67f)/.15f)));
        float trail=(field(x,0,34,311)-.5f)*11f;
        float dirt=clamp((1.8f-Math.abs(y-trail))*.72f);
        int land=mix(grass,mix(0xFF78503D,0xFFAA7957,grain),smooth(dirt));
        if(x<3.5f || x>14.5f || y<-14.5f || y>-5.5f)return land;
        float shore=lakeDistance(x,y);
        land=mix(land,0xFFAD9265,smooth(clamp((1.17f-shore)*6)));
        return mix(land,mix(0xFF346D71,0xFF568D8A,grain),smooth(clamp((1.05f-shore)*12)));
    }

    private float lakeDistance(float x,float y) {
        float dx=(x-9f)/4.3f,dy=(y+10f)/3.3f;
        return dx*dx+dy*dy+(field(x,y,3,317)-.5f)*.21f;
    }

    boolean lake(float x,float y) {
        return x>4 && x<14 && y>-14 && y<-6 && lakeDistance(x,y)<.98f;
    }

    private static float clamp(float value) { return Math.max(0f,Math.min(1f,value)); }

    private static int mix(int a,int b,float weight) {
        int t=Math.round(weight*256), inv=256-t;
        int r=(((a>>16)&255)*inv+((b>>16)&255)*t)>>8;
        int g=(((a>>8)&255)*inv+((b>>8)&255)*t)>>8;
        int blue=((a&255)*inv+(b&255)*t)>>8;
        return 0xff000000|(r<<16)|(g<<8)|blue;
    }

    /** A meandering path whose center drifts gradually rather than repeating diagonals. */
    boolean trail(int x,int y) {
        float center=(field(x,0,34,311)-.5f)*11f;
        return Math.abs(y-center)<.62f;
    }
    float groundVariation(int x,int y) {
        return field(x,y,5,313)*.7f+field(x,y,13,314)*.3f;
    }
    int decoration(int x,int y) {
        return (int)(hash(x,y,315)&0x7fffffff);
    }
    /** Ancient trees use deterministic 2x2 footprints on a 19-tile grid. */
    boolean ancientTree(int x,int y) {
        if(Math.floorMod(x,19)!=9||Math.floorMod(y,19)!=9)return false;
        if(Math.abs(x)<8&&Math.abs(y)<8||trail(x,y))return false;
        return (hash(Math.floorDiv(x,19),Math.floorDiv(y,19),731)&3)==0;
    }
    boolean ancientFootprint(int x,int y) {
        for(int dx=0;dx<=1;dx++)for(int dy=0;dy<=1;dy++)
            if(ancientTree(x-dx,y-dy))return true;
        return false;
    }
    /** 1 tree, 2 bush, 0 boulder, 3 ruin, 4 ancient tree. */

    int wildernessProp(int x,int y) {
        if(lake(x+.5f,y+.5f))return -1;
        if(ancientTree(x,y))return 4;
        if(ancientFootprint(x,y))return -1;
        if(Math.abs(x)<3&&Math.abs(y)<3||trail(x,y))return -1;
        int landmark=landmark(x,y);
        if(landmark==2)return 3;
        if(landmark==1)return 0;
        int biome=wildernessBiome(x,y);
        int roll=(int)(hash(x,y,316)&255);
        if(biome==2) {
            if(roll<47)return 1;
            if(roll<75)return 2;
            if(roll<82)return 0;
        } else if(biome==1) {
            if(roll<17)return 1;
            if(roll<33)return 2;
            if(roll<38)return 0;
        } else if(biome==3) {
            if(roll<19)return 0;
            if(roll<27)return 2;
        } else {
            if(roll<5)return 1;
            if(roll<17)return 2;
            if(roll<21)return 0;
        }
        return -1;
    }

}
