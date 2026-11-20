package com.cosmerearchives.dndroguelike;

final class LimbMeshTest {
    public static void main(String[] args) {
        LimbMesh mesh = new LimbMesh(20,100,2,20,.2f,10,0,10,50,10,100);
        mesh.pose(4,8,4,18,4,28);
        point(mesh,0,1,4,8);
        point(mesh,10,1,4,18);
        point(mesh,20,1,4,28);

        mesh.pose(4,8,4,18,14,24);
        point(mesh,0,1,4,8);
        point(mesh,10,1,4,18);
        point(mesh,20,1,14,24);
        float crossSection=Math.abs(coordinate(mesh,10,2,0)-coordinate(mesh,10,0,0));
        if(crossSection<2f || crossSection>4.01f)throw new AssertionError("pinched joint");
        for(float vertex:mesh.vertices)if(!Float.isFinite(vertex))throw new AssertionError("invalid vertex");
        LimbMesh thigh = new LimbMesh(20, 100, 2, 20, .3f, 10, 10, 10, 55, 10, 95, true);
        thigh.pose(6, 10, 15, 20, 9, 30);
        point(thigh, 4, 0, 3f, 13f);
        point(thigh, 4, 2, 9f, 13f);
        point(thigh, 11, 1, 15f, 20f);
        System.out.println("Limb mesh passed: both bones, shared joint, and continuous bent cross-section.");
    }

    private static void point(LimbMesh mesh,int row,int column,float x,float y) {
        if(Math.abs(coordinate(mesh,row,column,0)-x)>.001f
            ||Math.abs(coordinate(mesh,row,column,1)-y)>.001f) {
            throw new AssertionError("position missed at row "+row);
        }
    }

    private static float coordinate(LimbMesh mesh,int row,int column,int axis) {
        return mesh.vertices[(row*3+column)*2+axis];
    }
}
