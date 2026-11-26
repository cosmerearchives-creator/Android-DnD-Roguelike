package com.cosmerearchives.dndroguelike;

import java.util.HashMap;
import java.util.Random;

final class SensesTest {
    private static final class Map implements Senses.Geometry {
        Senses.Light light = Senses.Light.NORMAL;
        boolean wall;
        public Senses.Light light(float x,float y) { return light; }
        public boolean obstructed(float ax,float ay,float bx,float by,boolean terrain) { return wall; }
    }

    private static void require(boolean result, String description) {
        if (!result) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        Map map = new Map();
        require(Senses.SNEAK_MULTIPLIER*3==2,"sneak uses exactly two-thirds of walking speed");
        Senses.Profile seismic=new Senses.Profile(Senses.DEFAULT_SIGHT,120,0,10,30,0,60,0,false,false,false);
        require(Senses.feelsVibration(seismic,30,true,true),"grounded vibration at range");
        require(!Senses.feelsVibration(seismic,30,false,true),"airborne creature gives no vibration");
        require(!Senses.feelsVibration(seismic,30,true,false),"stationary creature gives no vibration");
        Senses.Profile human = new Senses.Profile(Senses.DEFAULT_SIGHT,360,0,0,0,0,60,2,false,false,false);
        Senses.Profile drow = new Senses.Profile(Senses.DEFAULT_SIGHT,360,120,0,0,0,60,2,false,false,false);
        Senses.Profile goblin = Senses.GOBLIN;
        require(Senses.see(human,map,0,0,0,0,6,false)==Senses.Sight.CLEAR,"360-degree player sight");
        require(Senses.see(human,map,0,0,0,0,6.01f,false)==Senses.Sight.NONE,"30-foot cap");
        require(Senses.see(goblin,map,0,0,0,0,-6,false)==Senses.Sight.CLEAR,"enemy forward 30 feet");
        require(Senses.see(goblin,map,0,0,0,0,1,false)==Senses.Sight.NONE,"enemy rear blind spot");
        require(Senses.see(goblin,map,0,0,0,1,-1,false)==Senses.Sight.CLEAR,"enemy diagonal inside cone");
        require(Senses.see(goblin,map,0,0,0,1,0,false)==Senses.Sight.NONE,"enemy side outside cone");
        map.light=Senses.Light.DIM;
        require(Senses.see(human,map,0,0,0,0,1,false)==Senses.Sight.DIM,"ordinary dim light");
        require(Senses.see(goblin,map,0,0,0,0,-1,false)==Senses.Sight.CLEAR,"darkvision upgrades dim");
        map.light=Senses.Light.DARK;
        require(Senses.see(human,map,0,0,0,0,1,false)==Senses.Sight.NONE,"human darkness");
        require(Senses.see(goblin,map,0,0,0,0,-1.2f,false)==Senses.Sight.DARKVISION,"goblin 6 feet");
        require(Senses.see(goblin,map,0,0,0,0,-1.3f,false)==Senses.Sight.NONE,"goblin limit");
        require(Senses.see(drow,map,0,0,0,0,2.4f,false)==Senses.Sight.DARKVISION,"drow 12 feet");
        map.wall=true;
        require(Senses.see(drow,map,0,0,0,0,2.4f,false)==Senses.Sight.NONE,"wall blocks darkvision");
        require(goblin.passive(false,false)==9 && human.passive(true,false)==17
            && human.passive(false,true)==7 && human.passive(true,true)==12,"passive perception");
        Random dice = new Random() {
            int roll;
            @Override public int nextInt(int bound) { return roll++ == 0 ? 15 : 3; }
        };
        require(Senses.check(dice,4,false,true)==8,"disadvantage plus modifier");
        require(Senses.hears(goblin,5,5,false,1,9),"audible footstep and tied passive perception");
        require(!Senses.hears(goblin,5,5,true,1,9),"wall attenuates hearing");
        require(!Senses.hears(goblin,5,5,false,1,10),"stealth suppresses detection");
        Senses.Awareness a=new Senses.Awareness(), b=new Senses.Awareness();
        a.update(0,false,90,90,true,2,3);
        require(a.state==Senses.Alert.SUSPICIOUS && a.x==2 && b.state==Senses.Alert.UNAWARE,"independent awareness");
        a.update(0,true,4,5,false,0,0);
        a.update(1,false,90,90,false,0,0);
        require(a.state==Senses.Alert.SEARCHING && a.x==4 && a.y==5,"last seen position");
        a.update(Senses.SEARCH_SECONDS,false,90,90,false,0,0);
        require(a.state==Senses.Alert.UNAWARE,"search expires");
        Exploration exploration=new Exploration();
        exploration.discover(-1,-8);
        exploration.discover(8,0);
        HashMap<Long,Long> saved=new HashMap<>();
        exploration.save(saved::put);
        Exploration loaded=new Exploration();
        saved.forEach(loaded::restore);
        require(loaded.known(-1,-8) && loaded.known(8,0) && !loaded.known(-1,-7),"exploration round trip");
        System.out.println("Sensory and exploration tests passed");
    }
}
