package com.cosmerearchives.dndroguelike;

/** Binds one continuous texture to two bones sharing a joint. */
final class LimbMesh {
    private final int width, height, columns, rows;
    private final float jointY, jointFeather;
    private final Bone upper, lower;
    private static final float PELVIS_OVERLAP = 5f;
    private final float pixelsToRig, rootX, rootY, restKneeOffsetX, restKneeOffsetY;
    private final boolean attached;
    private final float rearHipFit;
    final float[] vertices;

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY) {
        this(width, height, columns, rows, pixelsToRig, rootX, rootY, jointX, jointY, tipX, tipY, false);
    }

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY,
             boolean attached) {
        this(width, height, columns, rows, pixelsToRig, rootX, rootY, jointX, jointY,
            tipX, tipY, attached, 0f);
    }

    LimbMesh(int width, int height, int columns, int rows, float pixelsToRig,
             float rootX, float rootY, float jointX, float jointY, float tipX, float tipY,
             boolean attached, float rearHipFit) {
        this.width = width;
        this.rearHipFit = rearHipFit;
        this.rootX = rootX;
        this.rootY = rootY;
        this.pixelsToRig = pixelsToRig;
        this.attached = attached;
        restKneeOffsetX = (jointX - rootX) * pixelsToRig;
        restKneeOffsetY = (jointY - rootY) * pixelsToRig;
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
        if (!attached) upper.pose(rootX,rootY,jointX,jointY);
        lower.pose(jointX,jointY,tipX,tipY);
        float kneeShiftX = jointX - rootX - restKneeOffsetX;
        float kneeShiftY = jointY - rootY - restKneeOffsetY;
        int index = 0;
        for (int row=0; row<=rows; row++) {
            float y=height*(float)row/rows;
            float weight = smoothStep((y - this.jointY + jointFeather) / (2f * jointFeather));
            // Below the fixed shorts overlap, distribute knee travel across
            // the available thigh length so a lifted knee cannot fold the seam.
            float fromRoot = (y - this.rootY) * pixelsToRig;
            float thigh = Math.max(0f, (fromRoot - PELVIS_OVERLAP) / (restKneeOffsetY - PELVIS_OVERLAP));
            // Fit the rear edge of the upper thigh to the side-view shorts.
            // The fit fades down the leg and leaves the hip and knee anchors alone.
            float hipFit = rearHipFit * (1f - smoothStep((fromRoot - 1f) / 9f));
            for (int column=0; column<=columns; column++) {
                float x=width*(float)column/columns;
                float fromCenter = (x - this.rootX) * pixelsToRig;
                float ux = attached
                    ? rootX + fromCenter + thigh * kneeShiftX - hipFit * smoothStep(-fromCenter / 9f)
                    : upper.x(x,y);
                float uy = attached ? rootY + fromRoot + thigh * kneeShiftY : upper.y(x,y);
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
