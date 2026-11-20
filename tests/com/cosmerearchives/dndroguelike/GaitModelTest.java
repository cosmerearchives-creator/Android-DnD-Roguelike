package com.cosmerearchives.dndroguelike;

/** Runnable JVM tests: javac -d temp GaitModel.java GaitModelTest.java && java -cp temp ... */
final class GaitModelTest {
    static void require(boolean yes, String issue) {
        if(!yes) throw new AssertionError(issue);
    }
    static void close(float a,float b,float epsilon,String issue){
        require(Math.abs(a-b)<epsilon,issue+": "+a+" vs "+b);
    }
    public static void main(String[] args) {
        GaitModel g=new GaitModel();
        require(g.cycles()==0f,"Initial phase zero");
        g.advance(.36f);
        close(g.cycles(),.25f,.0001f,"Stride responds to travel distance");
        g.advance(0);
        close(g.cycles(),.25f,.0001f,"No advance while stopped");
        g.advance(Float.NaN);
        close(g.cycles(),.25f,.0001f,"Invalid advance ignored");
        for(int i=0;i<2000;i++) {
            float phase=i/2000f;
            for(int foot=0;foot<2;foot++) {
                GaitModel.Foot pose=GaitModel.sample(phase,foot);
                require(pose.along>=-.501f&&pose.along<=.501f,"Stride within reach");
                require(pose.lift>=-.001f&&pose.lift<=1.001f,"Lift in bounds");
                if(pose.planted)close(pose.lift,0f,.0001f,"Plant keeps foot on ground");
            }
        }
        // Both feet alternate their planted/swing portions half a cycle apart.
        require(GaitModel.sample(.20f,0).planted,"Left planted");
        require(!GaitModel.sample(.20f,1).planted,"Right is swinging");
        require(GaitModel.sample(.70f,1).planted,"Right planted later");
        require(!GaitModel.sample(.70f,0).planted,"Left is swinging later");
        // No visible jumps at planting and lift-off; positional offsets continuous.
        for(int f=0;f<2;f++) {
            for(float edge:new float[]{0f,GaitModel.STANCE,.5f,1f}) {
                GaitModel.Foot a=GaitModel.sample(edge-.00001f,f);
                GaitModel.Foot b=GaitModel.sample(edge+.00001f,f);
                close(a.along,b.along,.001f,"Continuous contact at phase boundary");
                close(a.lift,b.lift,.001f,"Continuous height at phase boundary");
            }
        }
        require(GaitModel.sample(GaitModel.STANCE + .001f, 0).lift < .0001f,
            "foot eases vertically into the swing");
        g.reset();close(g.cycles(),0f,.0001f,"Reset phase");
        System.out.println("Gait model tests passed: speed, foot-plant, alternation, continuity");
    }
}