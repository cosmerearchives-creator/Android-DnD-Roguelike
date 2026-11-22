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
}
