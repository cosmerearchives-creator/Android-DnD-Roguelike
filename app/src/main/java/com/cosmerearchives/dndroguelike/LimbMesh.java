package com.cosmerearchives.dndroguelike;

/** Binds one continuous texture to two bones sharing a joint. */
final class LimbMesh {
    private final int width, height, columns, rows;
    private final float jointY, jointFeather;
    private final Bone upper, lower;
    final float[] vertices;

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY) {
        this.width = width;
        this.height = height;
        this.columns = columns;
        this.rows = rows;
        this.jointY = jointY;
        jointFeather = 5f / pixelsToRig;
        upper = new Bone(rootX,rootY,jointX,jointY,pixelsToRig);
        lower = new Bone(jointX,jointY,tipX,tipY,pixelsToRig);
        vertices = new float[(columns+1)*(rows+1)*2];
    }

    void pose(float rootX,float rootY,float jointX,float jointY,float tipX,float tipY) {
        upper.pose(rootX,rootY,jointX,jointY);
        lower.pose(jointX,jointY,tipX,tipY);
        int index = 0;
        for (int row=0; row<=rows; row++) {
            float y=height*(float)row/rows;
            float t=Math.max(0f,Math.min(1f,(y-this.jointY+jointFeather)/(2f*jointFeather)));
            float weight=t*t*(3f-2f*t);
            for (int column=0; column<=columns; column++) {
                float x=width*(float)column/columns;
                float ux=upper.x(x,y), uy=upper.y(x,y);
                vertices[index++]=ux+(lower.x(x,y)-ux)*weight;
                vertices[index++]=uy+(lower.y(x,y)-uy)*weight;
            }
        }
    }

    private static final class Bone {
        private final float restX, restY, sourceX, sourceY, sourceLength, thickness;
        private float x0,y0,xx,xy,yx,yy;

        Bone(float x,float y,float x1,float y1,float thickness) {
            restX=x;
            restY=y;
            sourceLength=(float)Math.hypot(x1-x,y1-y);
            sourceX=(x1-x)/sourceLength;
            sourceY=(y1-y)/sourceLength;
            this.thickness=thickness;
        }

        void pose(float x,float y,float x1,float y1) {
            float dx=x1-x,dy=y1-y;
            float length=(float)Math.hypot(dx,dy);
            float nx=length>.001f ? dx/length : sourceX;
            float ny=length>.001f ? dy/length : sourceY;
            // Stretch only along the bone. Preserve the authored cross-section.
            xx=dx*sourceX/sourceLength+ny*sourceY*thickness;
            xy=dx*sourceY/sourceLength-ny*sourceX*thickness;
            yx=dy*sourceX/sourceLength-nx*sourceY*thickness;
            yy=dy*sourceY/sourceLength+nx*sourceX*thickness;
            x0=x-xx*restX-xy*restY;
            y0=y-yx*restX-yy*restY;
        }

        float x(float x,float y) { return x0+xx*x+xy*y; }
        float y(float x,float y) { return y0+yx*x+yy*y; }
    }
}
