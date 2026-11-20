package com.cosmerearchives.dndroguelike;

/** Binds one continuous texture to two bones sharing a joint. */
final class LimbMesh {
    private final int width, height, columns, rows;
    private final float jointY, jointFeather;
    private final Bone upper, lower;
    private final float pixelsToRig, rootX, rootY;
    private final boolean attached;
    final float[] vertices;

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY) {
        this(width, height, columns, rows, pixelsToRig, rootX, rootY, jointX, jointY, tipX, tipY, false);
    }

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY,
             boolean attached) {
        this.width = width;
        this.rootX = rootX;
        this.rootY = rootY;
        this.pixelsToRig = pixelsToRig;
        this.attached = attached;
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
            float weight = smoothStep((y - this.jointY + jointFeather) / (2f * jointFeather));
            // Keep the part of the thigh tucked under the shorts aligned
            // with the pelvis, then blend into the rotating upper leg.
            float bend = attached ? smoothStep(((y - this.rootY) * pixelsToRig - 5f) / 7f) : 1f;
            for (int column=0; column<=columns; column++) {
                float x=width*(float)column/columns;
                float ux = upper.x(x,y), uy = upper.y(x,y);
                if (bend < 1f) {
                    float fixedX = rootX + (x - this.rootX) * pixelsToRig;
                    float fixedY = rootY + (y - this.rootY) * pixelsToRig;
                    ux = fixedX + (ux - fixedX) * bend;
                    uy = fixedY + (uy - fixedY) * bend;
                }
                vertices[index++] = ux + (lower.x(x,y) - ux) * weight;
                vertices[index++] = uy + (lower.y(x,y) - uy) * weight;
            }
        }
    }

    private static float smoothStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
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
