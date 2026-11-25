package com.cosmerearchives.dndroguelike;

import java.util.Random;

final class WorldTimeTest {
    private static final long S = WorldClock.SECOND, H = WorldClock.HOUR, D = WorldClock.DAY;

    private static void require(boolean result,String description) {
        if (!result) throw new AssertionError(description);
    }

    private static final class Map implements WorldLighting.Geometry, Senses.Geometry {
        final WorldLighting lighting = new WorldLighting(this,WorldLighting.Weather.CLEAR);
        boolean roof, magic, wall;
        public WorldLighting.Cover cover(float x,float y) {
            return roof ? WorldLighting.Cover.ROOF : x<0 ? WorldLighting.Cover.CANOPY : WorldLighting.Cover.OPEN;
        }
        public boolean lightBlocked(float ax,float ay,float bx,float by) { return wall; }
        public boolean magicalDarkness(float x,float y) { return magic; }
        public boolean obstructed(float ax,float ay,float bx,float by,boolean terrain) { return wall; }
        public Senses.Light light(float x,float y) { return lighting.illumination(x,y); }
        public boolean sunlight(float x,float y) { return lighting.exposed(x,y); }
    }

    private static final class Lamp implements WorldLighting.Source {
        float x;
        final Senses.Light level;
        Lamp(Senses.Light level) { this.level=level; }
        public float x() { return x; }
        public float y() { return 0; }
        public Senses.Light illumination(float feet) { return feet<=5 ? level : Senses.Light.DARK; }
    }

    private static void clock() {
        WorldClock clock = new WorldClock(WorldClock.START);
        require(clock.time(0)==8*H,"new characters at 8 AM");
        require(clock.time(5*H)==8*H,"loading does not advance the clock");
        clock.setRunning(true,5*H);
        require(clock.time(5*H+127*S+17)==8*H+127*S+17,"1:1 including subsecond time");
        clock.setRunning(false,5*H+127*S+17);
        long saved=clock.time(12*H);
        require(saved==8*H+127*S+17,"background and pause time excluded");
        require(new WorldClock(saved).time(40*H)==saved,"close, reopen and map load restore exact time");
        require(WorldClock.nextHour(saved,9)-saved==H-127*S-17,"exact later duration with minutes and seconds");
        require(WorldClock.nextHour(saved,8)==D+8*H,"current hour already passed");
        require(WorldClock.nextHour(8*H,8)==D+8*H,"current exact hour selects next occurrence");
        require(WorldClock.nextHour(23*H+S,0)==D,"wait past midnight");
        require(WorldClock.nextHour(saved,4)==D+4*H,"earlier hour is tomorrow");
        WorldClock.nextHour(saved,3);
        require(clock.time(13*H)==saved,"preview and cancel leave time unchanged");
        clock.waitUntil(4,13*H);
        require(WorldClock.day(clock.time(13*H))==1 && clock.time(13*H)==D+4*H,"confirmed wait advances immediately");
        require(new WorldClock(clock.time(13*H)).time(14*H)==D+4*H,"wait survives reload");
        WorldClock midnight = new WorldClock(D-S);
        midnight.setRunning(true,0);
        require(WorldClock.day(midnight.time(S))==1 && WorldClock.format(midnight.time(S),true).equals("12:00:00 AM"),
            "real-time midnight rollover");
    }

    private static void lighting() {
        WorldLighting.Ambient[] expected = {
            WorldLighting.Ambient.DARKNESS,WorldLighting.Ambient.TWILIGHT,WorldLighting.Ambient.DAYLIGHT,
            WorldLighting.Ambient.SUNLIGHT,WorldLighting.Ambient.DAYLIGHT,WorldLighting.Ambient.TWILIGHT,
            WorldLighting.Ambient.DARKNESS
        };
        int[] starts={0,4,6,8,16,18,20};
        Map map=new Map();
        for (int i=0;i<starts.length;i++) {
            long time=starts[i]*H;
            require(WorldLighting.Ambient.at(time)==expected[i],"transition at hour "+starts[i]);
            require(WorldLighting.Ambient.at(time==0?D-1:time-1)==expected[i==0?6:i-1],"edge before transition");
            long revision=map.lighting.revision();
            map.lighting.setAmbient(WorldLighting.Ambient.at(time));
            require(map.lighting.revision()>revision,"ambient changes invalidate stationary visibility");
            require(map.light(1,0)==expected[i].intensity && map.sunlight(1,0)==expected[i].sunlight,"clear preserves schedule");
            require(map.light(-1,0)==(expected[i]==WorldLighting.Ambient.DARKNESS ? Senses.Light.DARK : Senses.Light.DIM)
                && !map.sunlight(-1,0),"canopy dims or preserves light without sunlight");
        }
        require(!WorldLighting.canopyContains(20,0,30,10)
            && WorldLighting.canopyContains(20,0,60,20),"large canopy covers a wider footprint");
        require(WorldLighting.canopyContains(0,8,30,10)
            && !WorldLighting.canopyContains(0,11,30,10),"canopy depth matters");

        Lamp dim=new Lamp(Senses.Light.DIM), normal=new Lamp(Senses.Light.NORMAL);
        map.lighting.addSource(dim);
        map.lighting.addSource(normal);
        require(map.light(1,0)==Senses.Light.NORMAL && !map.sunlight(1,0),"normal artificial light never causes exposure");
        map.wall=true;
        require(map.light(1,0)==Senses.Light.DARK,"geometry stops light");
        map.wall=false;
        long revision=map.lighting.revision();
        normal.x=8;
        map.lighting.invalidate();
        require(map.lighting.revision()>revision && map.light(1,0)==Senses.Light.DIM,"moving source reveals overlapping dim light");
        map.lighting.removeSource(dim);
        require(map.light(1,0)==Senses.Light.DARK,"source removal");
        map.lighting.setAmbient(WorldLighting.Ambient.SUNLIGHT);
        map.roof=true;
        require(!map.sunlight(1,0) && map.light(1,0)==Senses.Light.DARK,"roof blocks daytime sunlight");
        require(WorldLighting.Weather.load("CLEAR")==WorldLighting.Weather.CLEAR
            && WorldLighting.Weather.load("unknown")==WorldLighting.Weather.CLEAR,"weather persistence and legacy fallback");
    }

    private static void racialSenses() {
        Map map=new Map();
        Senses.Profile elf=new Senses.Profile(300,360,60,0,0,0,60,3,false,false,false);
        Senses.Profile drow=new Senses.Profile(300,360,120,0,0,0,60,3,false,false,false,true);
        require(Senses.sunlightDisadvantage(true,map,1,0,-1,0),"attacker in sun");
        require(Senses.sunlightDisadvantage(true,map,-1,0,1,0),"target in sun");
        require(!Senses.sunlightDisadvantage(true,map,-1,0,-2,0),"both shaded");
        require(!Senses.sunlightDisadvantage(false,map,1,0,2,0),"other races unaffected");
        require(drow.sightDisadvantage(Senses.Sight.CLEAR,false,map,-1,0,1,0)
            && drow.sightDisadvantage(Senses.Sight.CLEAR,false,map,1,0,-1,0),"sunlight affects sight Perception both ways");
        require(!drow.sightDisadvantage(Senses.Sight.BLINDSIGHT,false,map,1,0,2,0),"nonvisual sense has no sunlight penalty");
        require(elf.sightDisadvantage(Senses.Sight.DIM,false,map,0,0,20,0),"ordinary dim light sight disadvantage");
        require(!elf.sightDisadvantage(Senses.Sight.CLEAR,false,map,0,0,-1,0),"darkvision upgrades shade to clear");
        require(Senses.canHide(Senses.Sight.CLEAR,true,true)
            && !Senses.canHide(Senses.Sight.CLEAR,true,false)
            && !Senses.canHide(Senses.Sight.CLEAR,false,true),"Mask of the Wild requires qualifying natural obscuration");
        require(elf.passive(false,true)==8 && elf.passive(true,true)==13,"passive Perception and cancellation");
        Random dice = new Random() {
            @Override public int nextInt(int bound) { return 13; }
        };
        require(Senses.check(dice,3,true,true)==17,"advantage and disadvantage cancel for active checks");
        map.lighting.setAmbient(WorldLighting.Ambient.DARKNESS);
        require(!Senses.sunlightDisadvantage(true,map,1,0,2,0),"night attacks have no sunlight penalty");
        require(Senses.see(elf,map,0,0,0,12,0,false)==Senses.Sight.DARKVISION
            && Senses.see(elf,map,0,0,0,12.01f,0,false)==Senses.Sight.NONE,"60-foot darkvision");
        require(Senses.see(drow,map,0,0,0,24,0,false)==Senses.Sight.DARKVISION
            && Senses.see(drow,map,0,0,0,24.01f,0,false)==Senses.Sight.NONE,"120-foot superior darkvision");
        map.magic=true;
        require(Senses.see(elf,map,0,0,0,1,0,false)==Senses.Sight.NONE
            && Senses.see(drow,map,0,0,0,1,0,false)==Senses.Sight.NONE,"magical darkness blocks both darkvision types");
    }

    public static void main(String[] args) {
        clock();
        lighting();
        racialSenses();
        System.out.println("World clock, lighting, waiting and racial sensory tests passed");
    }
}
