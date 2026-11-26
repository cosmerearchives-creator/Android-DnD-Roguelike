package com.cosmerearchives.dndroguelike;

import java.util.Random;

final class Senses {
    static final float FEET_PER_TILE = 5f;
    static final float RANGE_SCALE = .1f;
    static final float DEFAULT_SIGHT = 300f * RANGE_SCALE;
    static final float SNEAK_MULTIPLIER = 2f / 3f;
    static final float[] FOOTSTEP_FEET = {5f, 15f, 30f};
    static final float SEARCH_SECONDS = 8f;
    static final float INVESTIGATE_SECONDS = 5f;
    static final float REVEAL_SECONDS = .8f;

    enum Light { NORMAL, DIM, DARK, MAGICAL_DARK }
    enum Sight { NONE, CLEAR, DIM, DARKVISION, BLINDSIGHT, TRUESIGHT }
    enum Alert { UNAWARE, SUSPICIOUS, ALERTED, SEARCHING }

    interface Geometry {
        boolean obstructed(float ax, float ay, float bx, float by, boolean terrain);
        Light light(float x, float y);
        default boolean sunlight(float x, float y) { return false; }
    }

    static final class Profile {
        final float normal, angle, darkvision, blind, tremor, trueSight, hearing;
        final int perception;
        final boolean keenHearing, keenSmell, keenSight, sunlightSensitive;

        Profile(float normal, float angle, float darkvision, float blind, float tremor,
                float trueSight, float hearing, int perception,
                boolean keenHearing, boolean keenSmell, boolean keenSight) {
            this(normal,angle,darkvision,blind,tremor,trueSight,hearing,perception,
                keenHearing,keenSmell,keenSight,false);
        }

        Profile(float normal, float angle, float darkvision, float blind, float tremor,
                float trueSight, float hearing, int perception,
                boolean keenHearing, boolean keenSmell, boolean keenSight, boolean sunlightSensitive) {
            this.normal = normal;
            this.angle = angle;
            this.darkvision = darkvision * RANGE_SCALE;
            this.blind = blind * RANGE_SCALE;
            this.tremor = tremor;
            this.trueSight = trueSight * RANGE_SCALE;
            this.hearing = hearing;
            this.perception = perception;
            this.keenHearing = keenHearing;
            this.keenSmell = keenSmell;
            this.keenSight = keenSight;
            this.sunlightSensitive = sunlightSensitive;
        }

        boolean sightDisadvantage(Sight sight, boolean lightlyObscured, Geometry map,
                                  float ax, float ay, float bx, float by) {
            boolean visual = sight != Sight.BLINDSIGHT;
            return visual && (disadvantaged(sight) || lightlyObscured
                || sunlightDisadvantage(sunlightSensitive,map,ax,ay,bx,by));
        }

        int passive(boolean advantage, boolean disadvantage) {
            return 10 + perception + (advantage ? 5 : 0) - (disadvantage ? 5 : 0);
        }
    }

    static final Profile GOBLIN = new Profile(DEFAULT_SIGHT, 120, 60, 0, 0, 0,
        60, -1, false, false, false);

    static float distanceFeet(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(bx - ax, by - ay) * FEET_PER_TILE;
    }

    static Sight see(Profile p, Geometry map, float ax, float ay, int facing,
                     float bx, float by, boolean terrain) {
        float distance = distanceFeet(ax, ay, bx, by);
        boolean directed = p.angle >= 360 || distance == 0
            || Math.abs(Math.IEEEremainder(Math.atan2(bx - ax, ay - by)
                - facing * Math.PI / 4, Math.PI * 2)) <= Math.toRadians(p.angle / 2);
        Sight sight = Sight.NONE;
        Light light = map.light(bx, by);
        if (directed && p.trueSight > 0 && distance <= p.trueSight) sight = Sight.TRUESIGHT;
        else if (p.blind > 0 && distance <= p.blind) sight = Sight.BLINDSIGHT;
        else if (directed && light != Light.MAGICAL_DARK) {
            if (p.darkvision > 0 && distance <= p.darkvision)
                sight = light == Light.DARK ? Sight.DARKVISION : Sight.CLEAR;
            else if (distance <= p.normal)
                sight = light == Light.NORMAL ? Sight.CLEAR : light == Light.DIM ? Sight.DIM : Sight.NONE;
        }
        return sight != Sight.NONE && !map.obstructed(ax, ay, bx, by, terrain) ? sight : Sight.NONE;
    }

    static boolean disadvantaged(Sight sight) {
        return sight == Sight.DIM || sight == Sight.DARKVISION;
    }

    static boolean sunlightDisadvantage(boolean sensitive, Geometry map,
                                        float ax, float ay, float bx, float by) {
        return sensitive && (map.sunlight(ax, ay) || map.sunlight(bx, by));
    }

    static boolean canHide(Sight sight, boolean naturalObscuration, boolean maskOfTheWild) {
        return sight != Sight.CLEAR || naturalObscuration && maskOfTheWild;
    }

    static boolean feelsVibration(Profile p, float distance, boolean sameSurface, boolean moving) {
        return p.tremor > 0 && distance <= p.tremor && sameSurface && moving;
    }

    static boolean hears(Profile p, float distance, float radius, boolean obstructed,
                         float environmentalFactor, int stealth) {
        float reach = Math.min(p.hearing, radius) * environmentalFactor * (obstructed ? .45f : 1f);
        return distance <= reach && p.passive(p.keenHearing, false) >= stealth;
    }

    static int check(Random dice, int bonus, boolean advantage, boolean disadvantage) {
        int roll = dice.nextInt(20) + 1;
        if (advantage != disadvantage) {
            int second = dice.nextInt(20) + 1;
            roll = advantage ? Math.max(roll, second) : Math.min(roll, second);
        }
        return roll + bonus;
    }

    static final class Awareness {
        Alert state = Alert.UNAWARE;
        float x, y, remaining;
        int searchRoll = Integer.MIN_VALUE;
        private int secondSearchRoll;

        int searchCheck(Random dice, int bonus, boolean advantage, boolean disadvantage) {
            if (searchRoll == Integer.MIN_VALUE) {
                searchRoll = dice.nextInt(20) + 1;
                secondSearchRoll = dice.nextInt(20) + 1;
            }
            if (advantage == disadvantage) return searchRoll + bonus;
            return (advantage ? Math.max(searchRoll,secondSearchRoll)
                : Math.min(searchRoll,secondSearchRoll)) + bonus;
        }

        void update(float dt, boolean detected, float targetX, float targetY,
                    boolean heard, float noiseX, float noiseY) {
            if (detected) {
                state = Alert.ALERTED;
                x = targetX;
                y = targetY;
                remaining = SEARCH_SECONDS;
                return;
            }
            if (state == Alert.ALERTED) {
                state = Alert.SEARCHING;
                searchRoll = Integer.MIN_VALUE;
            }
            if (heard) {
                if (state != Alert.SEARCHING) state = Alert.SUSPICIOUS;
                x = noiseX;
                y = noiseY;
                remaining = state == Alert.SEARCHING ? SEARCH_SECONDS : INVESTIGATE_SECONDS;
            } else if (state != Alert.UNAWARE && (remaining -= dt) <= 0) {
                state = Alert.UNAWARE;
                searchRoll = Integer.MIN_VALUE;
            }
        }
    }
}
