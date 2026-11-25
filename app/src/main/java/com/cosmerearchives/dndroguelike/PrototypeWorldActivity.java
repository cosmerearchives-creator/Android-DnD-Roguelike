package com.cosmerearchives.dndroguelike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;

/**
 * Streaming isometric view of the same coordinate-generated Wildlands terrain.
 * Floating-point movement, camera following and tile collision are independent
 * of the tile art. No fixed room bounds or pre-generated world bitmap.
 */
public final class PrototypeWorldActivity extends Activity {
    static final String EXTRA_TOWN="wildlands_town";
    private World view;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PlayerCharacter hero = PlayerCharacter.loadCompleted(this);
        if (hero == null && !getIntent().getBooleanExtra(OutdoorActivity.EXTRA_FREE_EXPLORE, false)) {
            startActivity(new Intent(this, CharacterCreatorActivity.class));
            finish();
            return;
        }
        view = new World(this, hero,getIntent().getBooleanExtra(EXTRA_TOWN,false));
        setContentView(view);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
    }
    @Override protected void onPause() {
        if (view != null) view.save();
        super.onPause();
    }
    @Override public void onBackPressed() {
        if (view != null) view.paused = !view.paused;
    }

    static final class World extends View {
        private static final float HALF_W = 70.0036f, HALF_H = 35.83905f;
        private static final int VISIBLE_RADIUS = 11;
        private static final float PLATFORM_RISE = 28.7339f;
        private static final float WALK_SPEED = 1.75f;
        private static final float PLAYER_RADIUS = .20f;
        private static final float MELEE_REACH = 1f;

        private static final float COMBAT_LEASH = 7f;

        private static final int GOBLIN_HP = 7, GOBLIN_AC = 12;
        private static final float PLAYER_ACTION_REFILL = 1f;
        private static final float GOBLIN_ATTACK_PERIOD = 6f;
        private static final float IMPACT_TIME = RigAttackPose.DURATION * .5f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path diamond = new Path();
        private final Rect source = new Rect();
        private final RectF destination = new RectF();
        private final WorldTerrain terrain = new WorldTerrain();
        private final SharedPreferences save;
        private final TownHub town;
        private boolean onQuest;
        private int questKills, dummyHp=20;
        private final TownHub.Actor townHero=this::drawHero;
        private final Bitmap atlas;
        private final Bitmap vegetation;
        private final float[] trunkCenter = {.5f,.5f,.5f,.5f};
        private final float[] trunkWidth = {.15f,.15f,.15f,.15f};
        private final Bitmap[][] factoryProps = new Bitmap[4][4];
        private final Bitmap bakedElf, bakedWalk, bakedPunch, bakedHit, sneakIdle, sneakWalk;
        private final Bitmap goblinIdle, goblinWalk, goblinAttack;
        private final WeaponTrials weaponTrials;
        private final AvatarActions avatarActions;
        private final WeaponTrials.Motion[] motions=WeaponTrials.Motion.values();
        private int motionIndex;
        private String equippedAttack="";
        private boolean throwSelected, activeThrow;
        private final int dexterityModifier, strengthModifier;
        private WeaponTrials.Motion activeMotion=WeaponTrials.Motion.UNARMED;
        private String activeWeapon="";
        private float arrowTime,arrowX,arrowY,arrowEndX,arrowEndY;
        private final int playerAttackBonus, playerDamage;
        private final Equipment.Bag combatBag;
        private final boolean freeWeaponTrials;
        private boolean activeTwoHands;
        private final java.util.Random combatDice = new java.util.Random();
        private static final int MAX_GOBLINS=5;
        private enum Cantrip {
            FIRE("Fire Bolt",24f,10,0xFFFF7934),
            FROST("Ray of Frost",12f,8,0xFF79E4FF),
            SHOCK("Shocking Grasp",1f,8,0xFFC7AAFF);
            final String title;
            final float range;
            final int die,color;
            Cantrip(String title,float range,int die,int color) {
                this.title=title;this.range=range;this.die=die;this.color=color;
            }
        }
        private final Cantrip knownCantrip;
        private final int spellBonus, spellDice;
        private Cantrip casting, missile;
        private boolean spellSelected, spellAttack;
        private final android.text.TextPaint hudText=new android.text.TextPaint();
        private float spellX,spellY,spellStartX,spellStartY,spellDx,spellDy,spellTravel,spellFlash;
        private final RectF hud=new RectF(), innerFrame=new RectF();
        private final Path hudPath=new Path();

        private static final class Goblin {
            float frost;
            float x,y,homeX,homeY,walkCycle,swing,recharge;
            int facing,hp=GOBLIN_HP;
            WeaponTrials.Motion motion=WeaponTrials.Motion.THRUST;
            String weapon="dagger";
            boolean moving,impact,drawn;
            final Senses.Awareness awareness=new Senses.Awareness();
            int heardEvent;
            float sensoryCooldown, searchTurn;
            boolean tracked;
            Senses.Sight playerSight=Senses.Sight.NONE;
            float prone;
        }
        private final java.util.ArrayList<Goblin> goblins=new java.util.ArrayList<>();
        private float spawnCooldown=2f,playerRecharge,punchX,punchY;
        private boolean hostile,playerImpact;
        private final int athleticsBonus;
        private int pendingShove;
        private final RectF shoveButton=new RectF();
        private final Bitmap prototypeFloor, prototypeTree, prototypeLedge;
        private final boolean bakedEnvironment;
        private final float movementFactor;
        private final GaitModel gait = new GaitModel();
        private float x, y, cameraX, cameraY, stickX, stickY;
        private float screenStickX, screenStickY;
        private float stickCenterX, stickCenterY;
        private boolean stickActive;
        private float walkCycles, attackRemaining, blend;
        private final int jumpStrength;
        private float jumpCooldown,jumpElapsed,jumpDistance,jumpTravel,jumpDx,jumpDy,runUp;
        private boolean jumping;
        private static final float JUMP_DURATION=.75f;
        private static final float HIT_REACTION_SECONDS=.34f;
        private float hitRemaining;
        private static final float AUTO_SHEATHE_SECONDS=6f;
        private float weaponQuietSeconds=AUTO_SHEATHE_SECONDS;
        private boolean weaponDrawn;
        private int facing, pointer = -1;
        private int pace;
        private boolean sneaking;
        private final Senses.Profile playerSenses;
        private final int stealthBonus;
        private final boolean maskOfTheWild, sunlightSensitive;
        private int stealthRoll=Integer.MIN_VALUE, noiseEvent;
        private float revealTime, stepDistance, noiseTime, noiseX, noiseY, noiseRadius;
        private Exploration exploration=new Exploration();
        private String fogMap="";
        private float fogX=Float.NaN,fogY,fogSaveTime;
        private int fogOriginX,fogOriginY, surveyX,surveyY,surveyIndex;
        private static final int SIGHT_TILES=(int)(Senses.DEFAULT_SIGHT/Senses.FEET_PER_TILE);
        private static final int SURVEY_WIDTH=2*SIGHT_TILES+1;
        private static final int FOG_RADIUS=15, FOG_SIZE=FOG_RADIUS*2+1;
        private final TownHub.Fog townFog=this::fogShade;
        private final long[] propKeys=new long[16384];
        private final byte[] propKinds=new byte[propKeys.length];
        private final Senses.Sight[] fog=new Senses.Sight[FOG_SIZE*FOG_SIZE];
        private final Senses.Light wildernessLight;
        private final android.graphics.ColorMatrixColorFilter grayscale =
            new android.graphics.ColorMatrixColorFilter(new android.graphics.ColorMatrix(new float[]{
                .213f,.715f,.072f,0,0, .213f,.715f,.072f,0,0,
                .213f,.715f,.072f,0,0, 0,0,0,1,0}));
        private final android.graphics.ColorMatrixColorFilter memoryTint =
            new android.graphics.ColorMatrixColorFilter(new android.graphics.ColorMatrix(new float[]{
                .075f,.25f,.025f,0,0, .075f,.25f,.025f,0,0,
                .075f,.25f,.025f,0,0, 0,0,0,1,0}));
        private final Senses.Geometry sensoryMap=new Senses.Geometry() {
            public Senses.Light light(float wx,float wy) {
                return inTown() ? (town.isInside()?Senses.Light.DIM:Senses.Light.BRIGHT) : wildernessLight;
            }
            public boolean obstructed(float ax,float ay,float bx,float by,boolean terrain) {
                return !visionClear(ax,ay,bx,by,terrain);
            }
        };
        private boolean paused;
        private Goblin defeatSource;
        private int health, experience, gold;
        private String worldMessage="";
        private float messageRemaining;
        private long lastFrame;
        private final RectF joystick = new RectF(), runButton = new RectF(), attackButton = new RectF();
        private final RectF exitButton = new RectF();
        private final RectF avatarActionButton = new RectF();
        private final RectF targetButton=new RectF(), autoAttackButton=new RectF();
        private Goblin lockedTarget, attackTarget;
        private boolean dummyLocked, attackDummy, autoAttack, autoLock;
        private final RectF jumpHud=new RectF(), crouchHud=new RectF(), secondaryHud=new RectF(), inventoryHud=new RectF(), pauseHud=new RectF();
        private float zoom = 1f;

        World(Context context, PlayerCharacter hero,boolean townSession) {
            super(context);
            int lightIndex=((Activity)context).getIntent().getIntExtra("visibility_light",0);
            wildernessLight=Senses.Light.values()[Math.max(0,Math.min(3,lightIndex))];
            RaceDefinition race=hero==null?null:hero.race;
            playerSenses=new Senses.Profile(Senses.DEFAULT_SIGHT,360,race==null?0:race.darkvisionFeet,
                0,0,0,60,hero==null?0:hero.skillBonus("Perception",4),false,false,false);
            stealthBonus=hero==null?0:hero.skillBonus("Stealth",1);
            maskOfTheWild=race!=null && race.traits.contains("mask_of_the_wild");
            sunlightSensitive=race!=null && race.traits.contains("sunlight_sensitivity");
            PetiteAppearance petite=hero!=null
                ? hero.petite : PetiteAppearance.DEFAULT;
            weaponTrials=new WeaponTrials(context,petite);
            avatarActions=new AvatarActions(context,petite);
            Cantrip known=null;
            if (hero!=null && hero.race==RaceDefinition.HIGH_ELF) {
                for (Cantrip spell:Cantrip.values()) if (spell.title.equals(hero.cantrip)) known=spell;
            }
            knownCantrip=known;
            int wizardModifier=hero==null?0:PlayerCharacter.modifier(hero.score(3));
            spellBonus=hero==null?2:hero.proficiency()+wizardModifier;
            int level=hero==null?1:hero.level;
            spellDice=level>=17?4:level>=11?3:level>=5?2:1;
            dexterityModifier=hero==null ? 0 : PlayerCharacter.modifier(hero.score(1));
            strengthModifier=hero==null ? 0 : PlayerCharacter.modifier(hero.score(0));
            jumpStrength=hero==null?10:hero.score(0);
            athleticsBonus=hero==null ? 0 : hero.skillBonus("Athletics",0);
            save = context.getSharedPreferences("wildlands_world", Context.MODE_PRIVATE);
            town=townSession ? new TownHub(context) : null;
            onQuest=townSession && save.getBoolean("town_quest_active",false);
            questKills=onQuest ? Math.max(0,Math.min(TownHub.GOAL-1,save.getInt("town_quest_kills",0))) : 0;
            String position=positionPrefix();
            x=save.getFloat(position+"_x",inTown()?TownHub.SPAWN_X:.5f);
            y=save.getFloat(position+"_y",inTown()?TownHub.SPAWN_Y:.5f);
            if (inTown() && !town.clear(x,y,PLAYER_RADIUS)) {
                x=TownHub.SPAWN_X;
                y=TownHub.SPAWN_Y;
            }
            if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 1000000
                    || Math.abs(y) > 1000000) { x=.5f; y=.5f; }
            cameraX = x; cameraY = y;
            health=save.getInt("hero_health",100);
            experience=save.getInt("hero_experience",0);
            gold=save.getInt("hero_gold",0);
            movementFactor = hero == null ? 1f : hero.race.movementFactor();
            playerAttackBonus = hero == null ? 2 : hero.attackBonus();
            playerDamage = hero == null ? 1 : Math.max(1, 1 + PlayerCharacter.modifier(hero.score(0)));
            combatBag=hero==null?new Equipment.Bag():hero.loadout();
            freeWeaponTrials=hero==null || !townSession;
            Bitmap bakedTerrain = load(context, "blender_world/terrain.png");
            Bitmap bakedProps = load(context, "blender_world/props.png");
            bakedEnvironment = bakedTerrain != null && bakedProps != null;
            atlas = bakedEnvironment ? bakedTerrain : load(context, "wildlands_terrain_v031.png");
            vegetation = bakedEnvironment ? bakedProps : load(context, "wildlands_vegetation.webp");
            String[] groups = {"rocks", "trees", "bushes", "ruins"};
            for (int group=0;group<groups.length;group++) {
                for (int variant=0;variant<4;variant++) {
                    factoryProps[group][variant] = trimTransparentMargins(load(context, String.format(java.util.Locale.US,
                        "factory_world/%s_%02d.png",groups[group],variant)));
                }
            }
            for (int i=0;i<trunkCenter.length;i++) measureTrunk(factoryProps[1][i],i);
            goblinIdle = load(context, "blender_world/goblin_idle.png");
            goblinWalk = load(context, "blender_world/goblin_walk.png");
            goblinAttack = load(context, "blender_world/goblin_punch.png");
            spawnGoblin();
            bakedElf = petite.apply(context,load(context,"blender_world/petite_detail_idle.png"),"idle");
            sneakIdle=petite.apply(context,load(context,"blender_world/sneak_idle.png"),"sneak_idle");
            sneakWalk=petite.apply(context,load(context,"blender_world/sneak_walk.png"),"sneak_walk");
            bakedWalk = petite.apply(context,load(context,"blender_world/petite_detail_walk.png"),"walk");
            bakedPunch = petite.apply(context,load(context,"blender_world/petite_detail_punch.png"),"punch");
            bakedHit = petite.apply(context,load(context,"blender_world/petite_detail_hit.png"),"hit");
            prototypeFloor = load(context,"isometric_lab/floor.png");
            prototypeTree = load(context,"isometric_lab/tree.png");
            prototypeLedge = load(context,"isometric_lab/ledge.png");
            setFocusable(true);
        }
        // Blender's orthographic PNGs contain substantial transparent space
        // below the trunk. Crop it once when loading; draw the visible trunk
        // base at the tile's ground point instead of anchoring the PNG canvas.
        private static Bitmap trimTransparentMargins(Bitmap bitmap) {
            if (bitmap == null) return null;
            int w=bitmap.getWidth(), h=bitmap.getHeight();
            int[] pixels=new int[w*h];
            bitmap.getPixels(pixels,0,w,0,0,w,h);
            int left=w,top=h,right=-1,bottom=-1;
            for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
                if ((pixels[y*w+x] >>> 24) < 12) continue;
                if (x<left) left=x;
                if (x>right) right=x;
                if (y<top) top=y;
                if (y>bottom) bottom=y;
            }
            if (right<left) return bitmap;
            if (left==0 && top==0 && right==w-1 && bottom==h-1) return bitmap;
            return Bitmap.createBitmap(bitmap,left,top,right-left+1,bottom-top+1);
        }
        private static Bitmap load(Context c, String name) {
            try (InputStream in = c.getAssets().open(name)) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) { return null; }
        }
        private boolean inTown() { return town!=null && !onQuest; }
        private String positionPrefix() {
            return town==null ? "player" : onQuest ? "town_quest_player" : "town_player";
        }
        void save() {
            if (defeatSource!=null) finishDefeat();
            String position=positionPrefix();
            float savedX=inTown() && town.isInside() ? 6f : x;
            float savedY=inTown() && town.isInside() ? 1.8f : y;
            SharedPreferences.Editor editor=save.edit()
                .putFloat(position+"_x",savedX).putFloat(position+"_y",savedY)
                .putInt("hero_health",health).putInt("hero_experience",experience).putInt("hero_gold",gold);
            if (town!=null) editor.putBoolean("town_quest_active",onQuest).putInt("town_quest_kills",questKills);
            saveExploration();
            editor.apply();
        }
        private void moveTo(float wx,float wy) {
            x=cameraX=wx;
            y=cameraY=wy;
            releaseStick();
            pace=0;
            avatarActions.play(null);
            lockedTarget=attackTarget=null;
            dummyLocked=attackDummy=false;
            hostile=false;
            attackRemaining=0f;
            playerImpact=false;
            casting=null;
            spellAttack=false;
            missile=null;
            spellFlash=0;
            arrowTime=0f;
            weaponDrawn=false;
            weaponQuietSeconds=0f;
            blend=0f;
        }
        private void startTownQuest() {
            if (!inTown() || !town.atBoard(x,y)) return;
            questKills=0;
            onQuest=true;
            goblins.clear();
            spawnCooldown=.7f;
            moveTo(.5f,.5f);
            save();
            combatMessage("Quest started · Defeat 10 goblins");
        }
        private void returnToTown() {
            onQuest=false;
            goblins.clear();
            town.setInside(false);
            moveTo(TownHub.SPAWN_X,TownHub.SPAWN_Y);
            save();
            combatMessage("Quest complete · 10 goblins defeated");
        }
        private void interactInTown() {
            if (town.training(x,y)) {
                beginPunch();
            } else if (town.atBoard(x,y)) {
                releaseStick();
                avatarActions.play(AvatarActions.Clip.INTERACT);
                WildlandsUi.choose(getContext(),"Defeat 10 goblins",
                    "Travel to the Wildlands. Defeat ten goblins to return to the quest board.",
                    new String[]{"Accept quest"},-1,which->startTownQuest());
            } else if (town.atTavern(x,y)) {
                town.setInside(true);
                moveTo(3f,1.35f);
                avatarActions.play(AvatarActions.Clip.INTERACT);
                combatMessage("Tavern");
            } else if (town.atExit(x,y)) {
                town.setInside(false);
                moveTo(6f,1.8f);
                avatarActions.play(AvatarActions.Clip.INTERACT);
            } else if (town.atGate(x,y)) {
                avatarActions.play(AvatarActions.Clip.INTERACT);
                combatMessage("The town gates are closed");
            }
        }
        private float sx(float wx, float wy) { return (wx - cameraX + wy - cameraY) * HALF_W; }
        private float sy(float wx, float wy) { return (wx - cameraX - (wy - cameraY)) * HALF_H; }
        private void measureTrunk(Bitmap tree,int variant) {
            if (tree == null) return;
            int width=tree.getWidth(), height=tree.getHeight();
            int left=width,right=-1;
            int bottomBand=Math.max(2,height/24);
            for (int row=Math.max(0,height-bottomBand);row<height;row++) {
                for (int col=0;col<width;col++) {
                    if ((tree.getPixel(col,row) >>> 24) < 64) continue;
                    left=Math.min(left,col);
                    right=Math.max(right,col);
                }
            }
            if (right>=left) {
                trunkCenter[variant]=(left+right+1)*.5f/width;
                trunkWidth[variant]=(right-left+1f)/width;
            }
        }
        private static float anchorOffset(int kind) { return kind==4 ? 1f : .5f; }
        private static float treeScale(int kind,int seed) {
            return kind==4 ? 1.85f + Math.floorMod(seed,5)*.14f
                           : .72f + Math.floorMod(seed,7)*.155f;
        }
        private boolean clear(float fromX,float fromY,float wx,float wy,float radius) {
            if (inTown()) return town.clear(wx,wy,radius);
            int minX=(int)Math.floor(wx-radius), maxX=(int)Math.floor(wx+radius);
            int minY=(int)Math.floor(wy-radius), maxY=(int)Math.floor(wy+radius);
            for (int tx=minX;tx<=maxX;tx++) for (int ty=minY;ty<=maxY;ty++) {
                if (wildernessProp(tx,ty)==3) return false;
            }
            // A corner trunk can overlap the tiles beside the tile that owns it.
            float searchRadius=radius+.30f;
            for (int tx=(int)Math.floor(wx-searchRadius)-1;tx<=(int)Math.floor(wx+searchRadius);tx++) {
                for (int ty=(int)Math.floor(wy-searchRadius)-1;ty<=(int)Math.floor(wy+searchRadius);ty++) {
                    int kind=wildernessProp(tx,ty);
                    if (kind!=1 && kind!=4) continue;
                    int seed=terrain.decoration(tx,ty);
                    float width=trunkWidth[Math.floorMod(seed,4)];
                    float trunkRadius=Math.min(.30f,Math.max(.06f,
                        width*125f*treeScale(kind,seed)/(2f*(float)Math.sqrt(2)*HALF_W)));
                    float anchorX=tx+anchorOffset(kind), anchorY=ty+anchorOffset(kind);
                    float separation=radius+trunkRadius;
                    float distanceSquared=squaredDistance(wx,wy,anchorX,anchorY);
                    if (distanceSquared<separation*separation
                        && distanceSquared<=squaredDistance(fromX,fromY,anchorX,anchorY)) return false;
                }
            }
            return true;
        }
        private static float squaredDistance(float ax,float ay,float bx,float by) {
            float dx=ax-bx,dy=ay-by;
            return dx*dx+dy*dy;
        }
        private static int heading(float dx,float dy) {
            return Math.floorMod((int)Math.round(Math.atan2(dx,-dy)/(Math.PI/4)),8);
        }
        private void spawnGoblin() {
            if (goblinIdle==null || goblinWalk==null || goblinAttack==null
                || goblins.size()>=MAX_GOBLINS) return;
            // Spawn in an annulus outside immediate melee range, at random
            // bearings. Each goblin has independent HP, AI and attack timing.
            for (int attempt=0;attempt<48;attempt++) {
                double angle=combatDice.nextDouble()*Math.PI*2;
                float distance=4.5f+combatDice.nextFloat()*5f;
                float px=x+(float)Math.sin(angle)*distance;
                float py=y-(float)Math.cos(angle)*distance;
                if (!clear(px,py,px,py,.19f)) continue;
                boolean occupied=false;
                for (Goblin other:goblins)
                    if (Math.hypot(other.x-px,other.y-py)<1.5f) occupied=true;
                if (occupied) continue;
                Goblin goblin=new Goblin();
                int variant=combatDice.nextInt(3);
                if (variant==1) {
                    goblin.motion=WeaponTrials.Motion.SWING;
                    goblin.weapon="scimitar";
                    goblin.hp=10;
                } else if (variant==2) {
                    goblin.motion=WeaponTrials.Motion.BOW;
                    goblin.weapon="shortbow";
                    goblin.hp=10;
                }
                goblin.x=goblin.homeX=px;
                goblin.y=goblin.homeY=py;
                goblin.facing=combatDice.nextInt(8);
                goblin.recharge=combatDice.nextFloat()*2f;
                goblins.add(goblin);
                return;
            }
        }
        private float distanceToGoblin(Goblin goblin) {
            return (float)Math.hypot(goblin.x-x,goblin.y-y);
        }
        private void combatMessage(String message) {
            worldMessage=message;
            messageRemaining=3f;
        }
        private String selectedWeapon() {
            return equippedAttack;
        }
        private WeaponTrials.Motion selectedMotion() {
            Equipment.Item item=Equipment.get(selectedWeapon());
            if(item!=null && (item.ranged || throwSelected)) return WeaponTrials.Motion.BOW;
            return motions[motionIndex];
        }
        private void chooseAttackMode() {
            releaseStick();
            java.util.ArrayList<String> labels=new java.util.ArrayList<>();
            java.util.ArrayList<String> ids=new java.util.ArrayList<>();
            java.util.ArrayList<Boolean> throwsWeapon=new java.util.ArrayList<>();
            labels.add("Unarmed");ids.add("");throwsWeapon.add(false);
            if(knownCantrip!=null){
                labels.add(knownCantrip.title);ids.add("spell");throwsWeapon.add(false);
            }
            for(Equipment.Item item:Equipment.catalog()){
                if(!item.weapon() || combatBag.find(item.id)==null)continue;
                if(!item.ranged && !item.id.equals("net") && !item.id.equals("dart")){
                    labels.add(item.name);ids.add(item.id);throwsWeapon.add(false);
                } else if(item.ranged){
                    labels.add(item.name+" · "+item.rangeNormal+" ft");ids.add(item.id);throwsWeapon.add(false);
                }
                if(item.thrown && !item.id.equals("net")){
                    labels.add("Throw "+item.name+" · "+item.rangeNormal+" ft");
                    ids.add(item.id);throwsWeapon.add(true);
                }
            }
            WildlandsUi.choose(getContext(),"Attack mode",null,labels.toArray(new String[0]),-1,index->{
                equippedAttack=ids.get(index);
                throwSelected=throwsWeapon.get(index);
                spellSelected=equippedAttack.equals("spell");
                if(spellSelected)equippedAttack="";
                motionIndex=WeaponTrials.Motion.UNARMED.ordinal();
                Equipment.Item item=Equipment.get(equippedAttack);
                if(item!=null) {
                    motionIndex=item.properties.contains("Reach") || item.damageType.equals("piercing")
                        ? WeaponTrials.Motion.THRUST.ordinal():WeaponTrials.Motion.SWING.ordinal();
                    if(item.twoHanded && !item.ranged)motionIndex=WeaponTrials.Motion.HEAVY.ordinal();
                }
            });
        }
        private float reach(WeaponTrials.Motion motion,String weapon) {
            if (motion!=WeaponTrials.Motion.BOW) return MELEE_REACH;
            Equipment.Item item=Equipment.get(weapon);
            return item==null?MELEE_REACH:item.rangeNormal/Senses.FEET_PER_TILE;
        }
        private boolean shotClear(float ax,float ay,float bx,float by) {
            int samples=(int)Math.ceil(Math.hypot(bx-ax,by-ay)*8);
            for (int i=1;i<samples;i++) {
                float t=(float)i/samples;
                float xx=ax+(bx-ax)*t, yy=ay+(by-ay)*t;
                if (!projectilePointClear(xx,yy,.035f)) return false;
            }
            return true;
        }
        private boolean projectilePointClear(float wx,float wy,float radius) {
            if(!clear(wx,wy,wx,wy,radius))return false;
            if(inTown())return true;
            int tx=(int)Math.floor(wx),ty=(int)Math.floor(wy);
            float rockRadius=.27f+radius;
            return wildernessProp(tx,ty)!=0
                || squaredDistance(wx,wy,tx+.5f,ty+.5f)>=rockRadius*rockRadius;
        }
        private int wildernessProp(int tx,int ty) {
            long key=((long)tx<<32)|(ty&0xffffffffL);
            int slot=(int)((key^(key>>>33))*0x9e3779b97f4a7c15L>>>50);
            if(propKinds[slot]!=0 && propKeys[slot]==key)return propKinds[slot]-2;
            int kind=terrain.wildernessProp(tx,ty);
            propKeys[slot]=key;
            propKinds[slot]=(byte)(kind+2);
            return kind;
        }
        private boolean visionClear(float ax,float ay,float bx,float by,boolean terrainTarget) {
            if (inTown()) return town.visionClear(ax,ay,bx,by,terrainTarget);
            int steps=(int)Math.ceil(Math.hypot(bx-ax,by-ay)*8);
            int endX=(int)Math.floor(bx),endY=(int)Math.floor(by);
            for(int i=1;i<=steps;i++){
                float t=(float)i/steps, wx=ax+(bx-ax)*t,wy=ay+(by-ay)*t;
                int tx=(int)Math.floor(wx),ty=(int)Math.floor(wy);
                if(terrainTarget && tx==endX && ty==endY) continue;
                if(wildernessProp(tx,ty)==3) return false;
                for(int ix=tx-1;ix<=tx+1;ix++) for(int iy=ty-1;iy<=ty+1;iy++){
                    int kind=wildernessProp(ix,iy);
                    if(kind!=1 && kind!=4 && kind!=0) continue;
                    if(terrainTarget && ix==endX && iy==endY) continue;
                    float offset=anchorOffset(kind);
                    float distance=squaredDistance(wx,wy,ix+offset,iy+offset);
                    if(distance>.30f*.30f)continue;
                    int seed=terrain.decoration(ix,iy);
                    float radius=kind==0?.27f:Math.min(.30f,Math.max(.06f,
                        trunkWidth[Math.floorMod(seed,4)]*125f*treeScale(kind,seed)
                            /(2f*(float)Math.sqrt(2)*HALF_W)));
                    if(distance<radius*radius)return false;
                }
            }
            return true;
        }
        private Senses.Sight sightAt(float wx,float wy,boolean terrainTarget) {
            return Senses.see(playerSenses,sensoryMap,x,y,facing,wx,wy,terrainTarget);
        }
        private void saveExploration() {
            if(fogMap.isEmpty()) return;
            SharedPreferences.Editor editor=save.edit();
            String prefix=fogMap;
            exploration.save((key,value)->editor.putLong(prefix+key,value));
            editor.apply();
        }
        private void updateFog() {
            String map=inTown()?(town.isInside()?"fog_v1_tavern_":"fog_v1_town_"):"fog_v1_wilderness_";
            if(!map.equals(fogMap)){
                saveExploration();
                exploration=new Exploration();
                fogMap=map;
                for(java.util.Map.Entry<String,?> entry:save.getAll().entrySet()){
                    if(entry.getKey().startsWith(map) && entry.getValue() instanceof Long)
                        exploration.restore(Long.parseLong(entry.getKey().substring(map.length())),(Long)entry.getValue());
                }
                fogX=Float.NaN;
                surveyX=(int)Math.floor(x);surveyY=(int)Math.floor(y);surveyIndex=0;
            }
            if(!inTown()) surveyDistantTerrain();
            if(!Float.isNaN(fogX) && squaredDistance(x,y,fogX,fogY)<.18f*.18f) return;
            fogX=x;fogY=y;
            fogOriginX=(int)Math.floor(x)-FOG_RADIUS;
            fogOriginY=(int)Math.floor(y)-FOG_RADIUS;
            for(int ix=0;ix<FOG_SIZE;ix++) for(int iy=0;iy<FOG_SIZE;iy++){
                int tx=fogOriginX+ix,ty=fogOriginY+iy;
                Senses.Sight sight=sightAt(tx+.5f,ty+.5f,true);
                fog[iy*FOG_SIZE+ix]=sight;
                if(sight!=Senses.Sight.NONE) exploration.discover(tx,ty);
            }
        }
        private void surveyDistantTerrain() {
            int cx=(int)Math.floor(x),cy=(int)Math.floor(y);
            if(Math.abs(surveyX-cx)>3 || Math.abs(surveyY-cy)>3){
                surveyX=cx;surveyY=cy;surveyIndex=0;
            }
            int total=SURVEY_WIDTH*SURVEY_WIDTH;
            for(int i=0;i<64 && surveyIndex<total;i++,surveyIndex++){
                // Permute the samples so movement cannot starve any one compass direction.
                int sample=(surveyIndex*137)%total;
                int tx=surveyX+sample%SURVEY_WIDTH-SIGHT_TILES;
                int ty=surveyY+sample/SURVEY_WIDTH-SIGHT_TILES;
                if(Math.abs(tx-cx)<=FOG_RADIUS && Math.abs(ty-cy)<=FOG_RADIUS || exploration.known(tx,ty))continue;
                if(sightAt(tx+.5f,ty+.5f,true)!=Senses.Sight.NONE)exploration.discover(tx,ty);
            }
        }
        private Senses.Sight fogSight(int tx,int ty) {
            int ix=tx-fogOriginX,iy=ty-fogOriginY;
            return ix<0||iy<0||ix>=FOG_SIZE||iy>=FOG_SIZE?Senses.Sight.NONE:fog[iy*FOG_SIZE+ix];
        }
        private int fogShade(int tx,int ty) {
            if(fogSight(tx,ty)!=Senses.Sight.NONE) return 0;
            return exploration.known(tx,ty)?0xB0000000:0xFF000000;
        }
        private void shadeGround(Canvas c,float px,float py,int shade) {
            if(shade==0)return;
            paint.setColor(shade);
            diamond.reset();
            diamond.moveTo(px,py-HALF_H);
            diamond.lineTo(px+HALF_W,py);
            diamond.lineTo(px,py+HALF_H);
            diamond.lineTo(px-HALF_W,py);
            diamond.close();
            c.drawPath(diamond,paint);
        }
        private void emitNoise(float feet) {
            if(noiseTime>.25f && noiseRadius>feet)return;
            noiseX=x;noiseY=y;noiseRadius=feet;noiseTime=.3f;
            noiseEvent++;
        }
        private void revealAttack() {
            revealTime=Senses.REVEAL_SECONDS;
            stealthRoll=Integer.MIN_VALUE;
            emitNoise(30f);
        }
        private boolean detectsPlayer(Goblin goblin) {
            Senses.Profile senses=Senses.GOBLIN;
            if(Senses.feelsVibration(senses,Senses.distanceFeet(goblin.x,goblin.y,x,y),
                !jumping,blend>.15f))return true;
            Senses.Sight sight=Senses.see(senses,sensoryMap,goblin.x,goblin.y,goblin.facing,x,y,false);
            if(sight==Senses.Sight.NONE)return false;
            boolean foliage=wildernessProp((int)Math.floor(x),(int)Math.floor(y))==2;
            boolean clear=sight==Senses.Sight.CLEAR && !foliage;
            boolean canHide=sight!=Senses.Sight.CLEAR || foliage && maskOfTheWild;
            if(clear || !canHide || !sneaking || revealTime>0 || stealthRoll==Integer.MIN_VALUE) return true;
            boolean disadvantage=Senses.disadvantaged(sight) || foliage;
            Senses.Awareness awareness=goblin.awareness;
            if(awareness.state==Senses.Alert.SUSPICIOUS || awareness.state==Senses.Alert.SEARCHING){
                if(awareness.searchRoll==Integer.MIN_VALUE)
                    awareness.searchRoll=Senses.check(combatDice,senses.perception,senses.keenSight,disadvantage);
                return awareness.searchRoll>=stealthRoll;
            }
            return senses.passive(senses.keenSight,disadvantage)>=stealthRoll;
        }
        private boolean lockable(Goblin goblin) {
            return goblin!=null && goblin.hp>0 && sightAt(goblin.x,goblin.y,false)!=Senses.Sight.NONE;
        }
        private boolean atPracticeTarget() {
            return inTown() && town.training(x,y)
                && sightAt(TownHub.DUMMY_X,TownHub.DUMMY_Y,false)!=Senses.Sight.NONE;
        }
        private Goblin nearestTarget() {
            Goblin nearest=null;
            float distance=Float.POSITIVE_INFINITY;
            for (Goblin candidate:goblins) {
                float candidateDistance=distanceToGoblin(candidate);
                if (lockable(candidate) && candidateDistance<distance) {
                    nearest=candidate;
                    distance=candidateDistance;
                }
            }
            return nearest;
        }
        private void updateLock() {
            if (!lockable(lockedTarget)) lockedTarget=null;
            if (inTown()) lockedTarget=null;
            if (!atPracticeTarget()) dummyLocked=false;
        }
        private void changeTarget() {
            updateLock();
            if (inTown()) {
                dummyLocked=atPracticeTarget();
            } else {
                int index=goblins.indexOf(lockedTarget);
                if (index<0) lockedTarget=nearestTarget();
                else for (int step=1;step<=goblins.size();step++) {
                    Goblin candidate=goblins.get((index+step)%goblins.size());
                    if (lockable(candidate)) {lockedTarget=candidate;break;}
                }
            }
            if (lockedTarget==null && !dummyLocked) combatMessage("No target nearby");
        }
        private boolean targetInReach(Goblin target,boolean dummy,float range) {
            if (!dummy && !lockable(target)) return false;
            float tx=dummy?TownHub.DUMMY_X:target.x,ty=dummy?TownHub.DUMMY_Y:target.y;
            // The training dummy itself is a solid town prop.
            return Math.hypot(tx-x,ty-y)<=range && (dummy || shotClear(x,y,tx,ty));
        }
        private void acquireTarget() {
            updateLock();
            if (atPracticeTarget()) dummyLocked=true;
            else if (!inTown() && lockedTarget==null) lockedTarget=nearestTarget();
        }
        private void faceTarget(Goblin target,boolean dummy) {
            if (!dummy && !lockable(target)) return;
            float dx=(dummy?TownHub.DUMMY_X:target.x)-x,dy=(dummy?TownHub.DUMMY_Y:target.y)-y;
            if (dx*dx+dy*dy>.0001f) facing=heading(dx,dy);
        }
        private void tryAutoAttack() {
            if (!autoAttack || playerRecharge>0f || attackRemaining>0f || avatarActions.active()) return;
            if (lockedTarget==null && !dummyLocked) return;
            if (!spellSelected) {
                Equipment.Item item=Equipment.get(selectedWeapon());
                if (item!=null && ((!freeWeaponTrials && combatBag.find(item.id)==null)
                    || ((item.twoHanded || (item.versatile && motions[motionIndex]==WeaponTrials.Motion.HEAVY))
                        && combatBag.slots[1]!=null))) return;
            }
            float range=spellSelected && knownCantrip!=null ? knownCantrip.range
                : reach(selectedMotion(),selectedWeapon());
            if (targetInReach(lockedTarget,dummyLocked,range)) beginPunch();
        }
        private boolean spendAttack() {
            if (attackRemaining>0 || playerRecharge>0 || avatarActions.active()) return false;
            attackRemaining=RigAttackPose.DURATION;
            playerRecharge=PLAYER_ACTION_REFILL;
            playerImpact=true;
            return true;
        }
        private void beginPunch() {
            if (attackRemaining>0f || playerRecharge>0f || avatarActions.active()) return;
            acquireTarget();
            float desiredRange=spellSelected && knownCantrip!=null ? knownCantrip.range
                : reach(selectedMotion(),selectedWeapon());
            if ((lockedTarget!=null || dummyLocked) && !targetInReach(lockedTarget,dummyLocked,desiredRange)) {
                combatMessage("Target out of reach or blocked");
                return;
            }
            if (!spendAttack()) return;
            attackTarget=lockedTarget;
            attackDummy=dummyLocked;
            faceTarget(attackTarget,attackDummy);
            pendingShove=0;
            casting=spellSelected?knownCantrip:null;
            spellAttack=casting!=null;
            if (casting!=null) {
                revealAttack();
                weaponDrawn=false;
                activeMotion=WeaponTrials.Motion.UNARMED;
                activeWeapon="";
                double heading=facing*Math.PI/4;
                punchX=x+(float)Math.sin(heading)*casting.range;
                punchY=y-(float)Math.cos(heading)*casting.range;
                return;
            }
            activeMotion=selectedMotion();
            activeThrow=throwSelected;
            activeWeapon=selectedWeapon();
            Equipment.Item selected=Equipment.get(activeWeapon);
            activeTwoHands=selected!=null && (selected.twoHanded || (selected.versatile && activeMotion==WeaponTrials.Motion.HEAVY));
            if(selected!=null){
                if(combatBag.find(activeWeapon)==null && freeWeaponTrials)combatBag.add(activeWeapon,1);
                if(combatBag.find(activeWeapon)==null){attackRemaining=0;playerRecharge=0;playerImpact=false;combatMessage("Weapon not in inventory");return;}
                if(activeTwoHands && combatBag.slots[1]!=null){attackRemaining=0;playerRecharge=0;playerImpact=false;WildlandsUi.choose(getContext(),"Two-handed grip","Unequip your off-hand item to use this weapon?",
                    new String[]{"Unequip"},-1,which->combatBag.slots[1]=null);return;}
                combatBag.equip(activeWeapon,Equipment.Slot.MAIN_HAND);
                if(selected.versatile)combatBag.setTwoHanded(activeTwoHands);
            }
            // Drawing happens automatically with the attack and never
            // consumes a separate action or recharge.
            revealAttack();
            weaponDrawn=activeMotion!=WeaponTrials.Motion.UNARMED;
            weaponQuietSeconds=0f;
            double angle=facing*Math.PI/4;
            float range=reach(activeMotion,activeWeapon);
            punchX=x+(float)Math.sin(angle)*range;
            punchY=y-(float)Math.cos(angle)*range;
        }
        private void fireArrow(float ax,float ay,float bx,float by) {
            arrowX=ax; arrowY=ay; arrowEndX=bx; arrowEndY=by;
            arrowTime=.19f;
        }
        private int rollDamage(Equipment.Item weapon,int ability) {
            return rollDamage(weapon,ability,false);
        }
        private int rollDamage(Equipment.Item weapon,int ability,boolean twoHands) {
            String value=weapon.damageDice(twoHands);
            if(!value.contains("d"))return value.equals("1")?Math.max(1,1+ability):0;
            String[] dice=value.split("d",2);
            int count=Integer.parseInt(dice[0]), sides=Integer.parseInt(dice[1]);
            int damage=ability;
            for (int i=0;i<count;i++) damage+=combatDice.nextInt(sides)+1;
            return Math.max(1,damage);
        }
        private void chooseShove() {
            if (inTown() || attackRemaining>0 || playerRecharge>0) return;
            releaseStick();
            WildlandsUi.choose(getContext(),"Shove · one attack",null,
                new String[]{"Push 5 feet","Knock prone"},-1,which->{
                    acquireTarget();
                    if (!targetInReach(lockedTarget,false,MELEE_REACH)) {
                        combatMessage("No enemy in shove reach");
                        return;
                    }
                    if (!spendAttack()) return;
                    revealAttack();
                    attackTarget=lockedTarget;
                    attackDummy=false;
                    faceTarget(attackTarget,false);
                    casting=null;
                    spellAttack=false;
                    pendingShove=which+1;
                    activeMotion=WeaponTrials.Motion.UNARMED;
                    activeWeapon="";
                });
        }
        private void resolveShove(int mode) {
            Goblin target=attackTarget;
            if(!targetInReach(target,false,MELEE_REACH)){combatMessage("No enemy in shove reach");return;}
            // Standard goblin STR 8 / DEX 14, no Athletics or Acrobatics proficiency.
            int attack=combatDice.nextInt(20)+1+athleticsBonus;
            int defense=combatDice.nextInt(20)+1+2;
            if(attack<=defense){combatMessage("Shove resisted");return;}
            if(target.awareness.state!=Senses.Alert.ALERTED)
                target.awareness.update(0,false,0,0,true,noiseX,noiseY);
            if(mode==2){
                // Real-time adaptation: stand on the next six-second movement cycle.
                target.prone=GOBLIN_ATTACK_PERIOD;
                target.swing=0;target.impact=false;
                combatMessage("Goblin knocked prone");return;
            }
            float dx=target.x-x,dy=target.y-y;
            float d=(float)Math.hypot(dx,dy);
            if(d<.001f){dx=(float)Math.sin(facing*Math.PI/4);dy=-(float)Math.cos(facing*Math.PI/4);d=1;}
            for(int i=0;i<10;i++){
                float nx=target.x+dx/d*.1f,ny=target.y+dy/d*.1f;
                if(!clear(target.x,target.y,nx,ny,.16f))break;
                boolean blocked=false;
                for(Goblin g:goblins)if(g!=target && Math.hypot(g.x-nx,g.y-ny)<.32f)blocked=true;
                if(blocked)break;
                target.x=nx;target.y=ny;
            }
            combatMessage("Goblin pushed");
        }
        private void releaseCantrip() {
            missile=casting;
            casting=null;
            double angle=facing*Math.PI/4;
            spellDx=(float)Math.sin(angle);
            spellDy=-(float)Math.cos(angle);
            if (attackDummy || lockable(attackTarget)) {
                float dx=(attackDummy?TownHub.DUMMY_X:attackTarget.x)-x;
                float dy=(attackDummy?TownHub.DUMMY_Y:attackTarget.y)-y;
                float distance=(float)Math.hypot(dx,dy);
                if (distance>.001f) {spellDx=dx/distance;spellDy=dy/distance;}
            }
            spellX=spellStartX=x;
            spellY=spellStartY=y;
            spellTravel=0;
            spellFlash=0;
        }

        private void updateCantrip(float dt) {
            spellFlash=Math.max(0,spellFlash-dt);
            if (missile==null || spellFlash>0) return;
            if (spellTravel>=missile.range) { missile=null;return; }
            // Substeps sweep the narrow hitbox so fast effects cannot tunnel.
            float advance=Math.min(missile.range-spellTravel,
                missile==Cantrip.SHOCK ? missile.range : dt*(missile==Cantrip.FROST?60f:25f));
            int steps=Math.max(1,(int)Math.ceil(advance/.10f));
            for (int i=0;i<steps;i++) {
                spellTravel+=advance/steps;
                spellX=spellStartX+spellDx*spellTravel;
                spellY=spellStartY+spellDy*spellTravel;
                if (inTown() && town.training(spellStartX,spellStartY)
                    && squaredDistance(spellX,spellY,TownHub.DUMMY_X,TownHub.DUMMY_Y)<.32f*.32f) {
                    int damage=cantripDamage();
                    dummyHp-=damage;
                    combatMessage(missile.title+" · "+damage+" damage"+(dummyHp<=0?" · Reset":" · "+dummyHp+"/20"));
                    if (dummyHp<=0) dummyHp=20;
                    finishCantrip();
                    return;
                }
                if (!projectilePointClear(spellX,spellY,.025f)) {finishCantrip();return;}
                if (!inTown()) {
                    for (Goblin target:goblins) {
                        if (squaredDistance(spellX,spellY,target.x,target.y)>.29f*.29f) continue;
                        int armor=target.motion==WeaponTrials.Motion.THRUST?GOBLIN_AC:13;
                        int roll=combatDice.nextInt(20)+1;
                        if (roll==20 || (roll!=1 && roll+spellBonus>=armor)) {
                            if (missile==Cantrip.FROST) target.frost=6f;
                            damageGoblin(target,cantripDamage()+(roll==20?cantripDamage():0));
                        } else combatMessage(missile.title+" missed");
                        if (missile!=null) finishCantrip();
                        return;
                    }
                }
            }
            if (spellTravel>=missile.range-.001f) finishCantrip();
        }

        private int cantripDamage() {
            int total=0;
            for (int i=0;i<spellDice;i++) total+=combatDice.nextInt(missile.die)+1;
            return total;
        }

        private void finishCantrip() {
            spellTravel=missile.range;
            spellFlash=.22f;
        }

        private void resolvePunch() {
            if (inTown()) {
                if (!town.training(x,y)) return;
                float dx=TownHub.DUMMY_X-x,dy=TownHub.DUMMY_Y-y;
                float range=reach(activeMotion,activeWeapon);
                if (!attackDummy || Math.hypot(dx,dy)>range) {
                    combatMessage("Move within attack range of the dummy");
                    return;
                }
                if (activeMotion==WeaponTrials.Motion.BOW) fireArrow(x,y,TownHub.DUMMY_X,TownHub.DUMMY_Y);
                Equipment.Item weapon=Equipment.get(activeWeapon);
                int damage=weapon==null ? playerDamage : rollDamage(weapon,
                    activeMotion==WeaponTrials.Motion.BOW && !activeThrow ? dexterityModifier : strengthModifier,activeTwoHands);
                dummyHp-=damage;
                if (dummyHp<=0) {
                    dummyHp=20;
                    combatMessage("Dummy hit · "+damage+" damage · Reset");
                } else combatMessage("Dummy hit · "+damage+" damage · "+dummyHp+"/20");
                return;
            }
            Goblin target=attackTarget;
            boolean ranged=activeMotion==WeaponTrials.Motion.BOW;
            if (!targetInReach(target,false,reach(activeMotion,activeWeapon))) target=null;
            if (ranged) fireArrow(x,y,target==null?punchX:target.x,target==null?punchY:target.y);
            if (target==null) {
                if (hostile) combatMessage("No enemy in reach");
                return;
            }
            hostile=true;pace=0;
            Equipment.Item item=Equipment.get(activeWeapon);
            int ability=ranged && !activeThrow || item!=null && item.id.equals("dart") ? dexterityModifier : item!=null && item.finesse
                ? Math.max(strengthModifier,dexterityModifier) : strengthModifier;
            int bonus=playerAttackBonus-strengthModifier+ability;
            int armor=target.motion==WeaponTrials.Motion.THRUST ? GOBLIN_AC : 13;
            int roll=combatDice.nextInt(20)+1;
            boolean advantage=target.prone>0 && distanceToGoblin(target)<=MELEE_REACH;
            boolean disadvantage=(target.prone>0 && distanceToGoblin(target)>MELEE_REACH)
                || sunlightSensitive && wildernessLight==Senses.Light.BRIGHT;
            if(advantage!=disadvantage){
                int second=combatDice.nextInt(20)+1;
                roll=advantage?Math.max(roll,second):Math.min(roll,second);
            }
            if (roll+bonus<armor) {
                combatMessage("Attack missed");return;
            }
            int damage=item==null ? playerDamage : rollDamage(item,ability,activeTwoHands);
            damageGoblin(target,damage);
        }
        private void damageGoblin(Goblin target,int damage) {

            hostile=true;
            pace=0;
            target.hp=Math.max(0,target.hp-damage);
            if (target.hp>0) {
                combatMessage("Goblin hit · "+damage+" damage");return;
            }
            goblins.remove(target);
            if (lockedTarget==target) lockedTarget=null;
            int reward=target.motion==WeaponTrials.Motion.THRUST ? 25 : 50;
            experience+=reward;
            if (onQuest && ++questKills>=TownHub.GOAL) {
                returnToTown();
                return;
            }
            save();
            combatMessage("Goblin defeated · +"+reward+" XP");
        }
        private void updateGoblins(float dt) {
            spawnCooldown-=dt;
            if (spawnCooldown<=0f) {
                spawnCooldown=5f+combatDice.nextFloat()*5f;
                spawnGoblin();
            }
            hostile=false;
            for (Goblin goblin:goblins) {
                goblin.moving=false;
                goblin.frost=Math.max(0,goblin.frost-dt);
                if(goblin.prone>0){goblin.prone=Math.max(0,goblin.prone-dt);hostile=true;pace=0;continue;}
                Senses.Awareness awareness=goblin.awareness;
                goblin.sensoryCooldown-=dt;
                boolean detected=goblin.tracked;
                boolean sampled=false;
                if(goblin.sensoryCooldown<=0){
                    sampled=true;
                    goblin.sensoryCooldown=.09f+combatDice.nextFloat()*.04f;
                    detected=goblin.tracked=detectsPlayer(goblin);
                }
                // Before acting on any new position, confirm contact this update.
                if(!sampled && detected && (goblin.swing>0 || distanceToGoblin(goblin)<2f)){
                    sampled=true;
                    detected=goblin.tracked=detectsPlayer(goblin);
                }
                boolean heard=false;
                if(noiseTime>0 && goblin.heardEvent!=noiseEvent){
                    goblin.heardEvent=noiseEvent;
                    float feet=Senses.distanceFeet(goblin.x,goblin.y,noiseX,noiseY);
                    heard=feet<=noiseRadius && Senses.hears(Senses.GOBLIN,feet,noiseRadius,
                        !visionClear(goblin.x,goblin.y,noiseX,noiseY,false),1f,
                        sneaking && revealTime<=0?stealthRoll:Integer.MIN_VALUE);
                }
                awareness.update(dt,detected,sampled?x:awareness.x,sampled?y:awareness.y,heard,noiseX,noiseY);
                if(awareness.state!=Senses.Alert.ALERTED){
                    goblin.swing=0;
                    if(awareness.state!=Senses.Alert.UNAWARE){
                        if(squaredDistance(goblin.x,goblin.y,awareness.x,awareness.y)>.22f*.22f)
                            moveGoblin(goblin,awareness.x,awareness.y,dt,.18f);
                        else if((goblin.searchTurn-=dt)<=0){
                            goblin.facing=(goblin.facing+1)%8;
                            goblin.searchTurn=.36f;
                        }
                    }
                    continue;
                }
                float distance=distanceToGoblin(goblin);
                hostile=true;pace=0;
                goblin.recharge=Math.max(0f,goblin.recharge-dt);
                float range=reach(goblin.motion,goblin.weapon);
                boolean canHit=distance<=range
                    && (sampled?detected:detectsPlayer(goblin)) && shotClear(goblin.x,goblin.y,x,y);
                if (goblin.swing>0) {
                    goblin.swing=Math.max(0f,goblin.swing-dt);
                    if (goblin.impact && goblin.swing<=IMPACT_TIME) {
                        goblin.impact=false;
                        if (goblin.motion==WeaponTrials.Motion.BOW && canHit) fireArrow(goblin.x,goblin.y,x,y);
                        if (!canHit) combatMessage("You evaded the goblin");
                        else if (combatDice.nextInt(20)+5<combatBag.armorClass()) combatMessage("Goblin missed");
                        else {
                            int damage=rollDamage(Equipment.get(goblin.weapon),2);
                            health=Math.max(0,health-damage);
                            hitRemaining=health>0 ? HIT_REACTION_SECONDS : 0f;
                            combatMessage("Goblin "+goblin.weapon+" · "+damage+" damage");
                            if (health==0) {
                                defeatSource=goblin;
                                attackRemaining=0f;
                                playerImpact=false;
                                releaseStick();
                                avatarActions.play(AvatarActions.Clip.DEFEAT);
                                return;
                            }
                            save();
                        }
                    }
                    continue;
                }
                goblin.facing=heading(awareness.x-goblin.x,awareness.y-goblin.y);
                if (canHit) {
                    if (goblin.recharge==0f) {
                        goblin.swing=RigAttackPose.DURATION;
                        goblin.recharge=GOBLIN_ATTACK_PERIOD;
                        goblin.impact=true;
                    }
                    continue;
                }
                moveGoblin(goblin,awareness.x,awareness.y,dt,.83f);
            }
        }
        private void moveGoblin(Goblin goblin,float targetX,float targetY,float dt,float stop){
                float dx=targetX-goblin.x,dy=targetY-goblin.y;
                float distance=(float)Math.hypot(dx,dy);
                if(distance<.001f)return;
                goblin.facing=heading(dx,dy);
                float delta=Math.max(0f,Math.min(distance-stop,dt*WALK_SPEED*(goblin.frost>0?2f/3f:1f)));
                float nextX=goblin.x+dx*delta/distance;
                float nextY=goblin.y+dy*delta/distance;
                float oldX=goblin.x,oldY=goblin.y;
                if (clear(goblin.x,goblin.y,nextX,goblin.y,.16f)) goblin.x=nextX;
                if (clear(goblin.x,goblin.y,goblin.x,nextY,.16f)) goblin.y=nextY;
                float moved=(float)Math.hypot(goblin.x-oldX,goblin.y-oldY);
                goblin.moving=moved>.00001f;
                if (goblin.moving) {
                    goblin.walkCycle=GaitModel.fractional(goblin.walkCycle+moved/GaitModel.CYCLE_TILES);
                    goblin.facing=heading(goblin.x-oldX,goblin.y-oldY);
                }
        }
        private void finishDefeat() {
            Goblin source=defeatSource;
            if (source==null) return;
            defeatSource=null;
            avatarActions.play(null);
            lockedTarget=attackTarget=null;
            dummyLocked=attackDummy=false;
            autoAttack=false;
            health=100;
            retreatFromGoblin(source);
            for (Goblin other:goblins) {
                other.x=other.homeX;other.y=other.homeY;
                other.awareness.state=Senses.Alert.UNAWARE;other.tracked=false;other.swing=0f;
            }
            hostile=false;
            weaponDrawn=false;
            blend=0f;
            combatMessage("Goblin pack defeated you · Returned to safety");
        }
        private void retreatFromGoblin(Goblin goblin) {
            for (int direction=0;direction<8;direction++) {
                double angle=direction*Math.PI/4;
                float px=goblin.homeX+(float)Math.sin(angle)*(COMBAT_LEASH+1);
                float py=goblin.homeY-(float)Math.cos(angle)*(COMBAT_LEASH+1);
                if (clear(px,py,px,py,PLAYER_RADIUS)) {
                    x=px;y=py;return;
                }
            }
        }
        private void drawGoblin(Canvas c,Goblin goblin) {
            Bitmap sheet=goblin.swing>0 ? goblinAttack : goblin.moving ? goblinWalk : goblinIdle;
            if (sheet==null) return;
            int frame=goblin.swing>0 ? Math.min(4,(int)(5*(1-goblin.swing/RigAttackPose.DURATION)))
                : goblin.moving ? (int)(8*goblin.walkCycle) : 0;
            float px=sx(goblin.x,goblin.y),py=sy(goblin.x,goblin.y);
            source.set(frame*168,goblin.facing*192,(frame+1)*168,(goblin.facing+1)*192);
            destination.set(px-84,py-160,px+84,py+32);
            paint.setColor(Color.WHITE);
            float progress=goblin.swing>0 ? 1-goblin.swing/RigAttackPose.DURATION
                : goblin.moving ? -1f-goblin.walkCycle : -1f;
            if(goblin.playerSight==Senses.Sight.DARKVISION)paint.setColorFilter(grayscale);
            if(goblin.prone>0){c.save();c.rotate(85,px,py);c.drawBitmap(goblinIdle,source,destination,paint);c.restore();}
            else if (goblin.motion==WeaponTrials.Motion.THRUST
                || !weaponTrials.draw(c,paint,4,goblin.motion,goblin.weapon,goblin.facing,
                    progress,px,py,192f/164f)) c.drawBitmap(sheet,source,destination,paint);
            paint.setColorFilter(null);
            if (goblin.awareness.state==Senses.Alert.ALERTED) {
                paint.setColor(0xFF322A2B);
                c.drawRect(px-18,py-111,px+18,py-107,paint);
                paint.setColor(0xFFD27060);
                c.drawRect(px-18,py-111,px-18+36f*goblin.hp/(goblin.motion==WeaponTrials.Motion.THRUST ? GOBLIN_HP : 10),py-107,paint);
            }
            if (goblin==lockedTarget) drawLockMarker(c,px,py-122);
        }
        private void drawLockMarker(Canvas c,float x,float y) {
            paint.setColor(WildlandsUi.GOLD);
            paint.setStrokeWidth(2.5f);
            c.drawLine(x-7,y-5,x,y+2,paint);
            c.drawLine(x,y+2,x+7,y-5,paint);
            paint.setStrokeWidth(1f);
        }
        private float elevation(float wx,float wy) {
            int tx=Math.round(wx),ty=Math.round(wy);
            return 0f;
        }
        private void startJump(){
            if(paused||defeatSource!=null||jumping||jumpCooldown>0||attackRemaining>0||avatarActions.active())return;
            float mag=(float)Math.hypot(stickX,stickY);
            jumpDx=mag>.08f?stickX/mag:(float)Math.sin(facing*Math.PI/4);
            jumpDy=mag>.08f?stickY/mag:-(float)Math.cos(facing*Math.PI/4);
            jumpDistance=Math.max(0,jumpStrength*(runUp>=2f?1f:.5f)/5f);
            jumpElapsed=jumpTravel=runUp=0;
            jumpCooldown=1f;jumping=true;
            facing=heading(jumpDx,jumpDy);
            avatarActions.play(AvatarActions.Clip.JUMP_START);
        }
        private void advanceJump(float dt){
            jumpElapsed=Math.min(JUMP_DURATION,jumpElapsed+dt);
            float phase=Math.max(0,Math.min(1,(jumpElapsed-.20f)/.33f));
            float remaining=Math.max(0,jumpDistance*phase-jumpTravel);
            while(remaining>.0001f){
                float step=Math.min(.08f,remaining);
                float nx=x+jumpDx*step,ny=y+jumpDy*step;
                if(!clear(x,y,nx,ny,PLAYER_RADIUS)){jumpDistance=jumpTravel;break;}
                x=nx;y=ny;jumpTravel+=step;remaining-=step;
            }
            if(jumpElapsed>=JUMP_DURATION){jumping=false;avatarActions.play(null);}
        }
        private void update(float dt) {
            if (paused) return;
            jumpCooldown=Math.max(0,jumpCooldown-dt);
            if (avatarActions.update(dt)) {
                finishDefeat();
                save();
            }
            if (defeatSource!=null) return;
            if (attackRemaining<=0f) hitRemaining=Math.max(0f,hitRemaining-dt);
            messageRemaining=Math.max(0,messageRemaining-dt);
            playerRecharge=Math.max(0,playerRecharge-dt);
            arrowTime=Math.max(0,arrowTime-dt);
            if (attackRemaining>0) {
                attackRemaining=Math.max(0,attackRemaining-dt);
                if (playerImpact && attackRemaining<=IMPACT_TIME) {
                    playerImpact=false;
                    if(pendingShove!=0){int mode=pendingShove;pendingShove=0;resolveShove(mode);}
                    else if (casting!=null) releaseCantrip();
                    else resolvePunch();
                }
            }
            updateCantrip(dt);
            revealTime=Math.max(0,revealTime-dt);
            noiseTime=Math.max(0,noiseTime-dt);
            if(sneaking && revealTime==0 && stealthRoll==Integer.MIN_VALUE){
                Equipment.Item armor=Equipment.get(combatBag.slots[2]);
                stealthRoll=Senses.check(combatDice,stealthBonus,false,
                    armor!=null && armor.properties.contains("Stealth disadvantage"));
            }
            if (!inTown()) updateGoblins(dt);
            if (defeatSource!=null) return;
            updateLock();
            // Combat keeps the weapon ready. After six peaceful seconds
            // following the last attack, the sheathed state is just idle art.
            if (hostile || attackRemaining>0f) {
                weaponQuietSeconds=0f;
            } else if (weaponDrawn) {
                weaponQuietSeconds+=dt;
                if (weaponQuietSeconds>=AUTO_SHEATHE_SECONDS) weaponDrawn=false;
            }
            float magnitude=(float)Math.hypot(stickX,stickY);
            boolean moving=false;
            if (magnitude>.08f && avatarActions.seated()) avatarActions.play(AvatarActions.Clip.STAND);
            if(jumping){
                advanceJump(dt);
                if(!jumping)emitNoise(15f);
            }
            else if (magnitude>.08f && attackRemaining==0 && avatarActions.allowsMotion()) {
                float speed=WALK_SPEED*movementFactor*combatBag.movementFactor()*(sneaking?Senses.SNEAK_MULTIPLIER:pace==1?1.4f:pace==2?1.8f:1f);
                float dx=stickX*speed*dt, dy=stickY*speed*dt;
                float oldX=x,oldY=y;
                if (clear(x,y,x+dx,y,PLAYER_RADIUS)) x+=dx;
                if (clear(x,y,x,y+dy,PLAYER_RADIUS)) y+=dy;
                float actualX=x-oldX,actualY=y-oldY;
                float travelled=(float)Math.hypot(actualX,actualY);
                moving=travelled>0.00001f;
                runUp=moving?Math.min(2f,runUp+travelled):0;
                if (moving) {
                    stepDistance+=travelled;
                    if(stepDistance>=.38f){
                        stepDistance-=.38f;
                        emitNoise(Senses.FOOTSTEP_FEET[sneaking?0:pace==0?1:2]);
                    }
                    if (lockedTarget==null && !dummyLocked) facing=heading(actualX,actualY);
                    gait.advance(travelled);
                    walkCycles=gait.cycles();
                }
            }
            if(!jumping&&!moving)runUp=0;
            blend=Math.max(0,Math.min(1,blend+(moving?10f:-13f)*dt));
            if (attackRemaining>0f) faceTarget(attackTarget,attackDummy);
            else if(!jumping)faceTarget(lockedTarget,dummyLocked);
            if (autoLock) acquireTarget();
            tryAutoAttack();
            // Lock the camera to the character. A lagging camera made the
            // joystick appear to drift and the walk animation slide.
            cameraX=x;cameraY=y;
        }
        // Continuous coordinate-seeded wilderness. Draw world-space diamonds rather
        // than repeating a room bitmap; large-scale noise creates connected meadows,
        // woodland, stony ridges and a wandering trail without visible atlas seams.
        private void ground(Canvas c) {
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            for(int depth=-VISIBLE_RADIUS*2-3;depth<=VISIBLE_RADIUS*2+3;depth++) {
                for(int xx=-VISIBLE_RADIUS-3;xx<=VISIBLE_RADIUS+3;xx++) {
                    int yy=xx-depth;
                    if(Math.abs(yy)>VISIBLE_RADIUS+3)continue;
                    int tx=cx+xx,ty=cy+yy;
                    float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f);
                    int shade=fogShade(tx,ty);
                    if(shade==0xFF000000){shadeGround(c,px,py,shade);continue;}
                    if(fogSight(tx,ty)==Senses.Sight.DARKVISION)paint.setColorFilter(grayscale);
                    int biome=terrain.wildernessBiome(tx,ty);
                    int base=biome==3?0xFF6D725C:biome==2?0xFF354D3C:
                        biome==1?0xFF496947:0xFF668457;
                    if(terrain.trail(tx,ty))base=0xFF77634B;
                    float tint=terrain.groundVariation(tx,ty);
                    int delta=(int)((tint-.5f)*21);
                    int r=Math.max(0,Math.min(255,Color.red(base)+delta));
                    int g=Math.max(0,Math.min(255,Color.green(base)+delta));
                    int b=Math.max(0,Math.min(255,Color.blue(base)+delta));
                    paint.setColor(Color.rgb(r,g,b));
                    diamond.reset();
                    diamond.moveTo(px,py-HALF_H);
                    diamond.lineTo(px+HALF_W,py);
                    diamond.lineTo(px,py+HALF_H);
                    diamond.lineTo(px-HALF_W,py);
                    diamond.close();
                    c.drawPath(diamond,paint);
                    int h=terrain.decoration(tx,ty);
                    if(!terrain.trail(tx,ty)&&h%5==0) {
                        paint.setColor(biome==2?0x664F7652:0x667F9C65);
                        c.drawOval(px-18,py-5,px+19,py+6,paint);
                    }
                    if(h%9==1) {
                        paint.setColor(0x88502F22);
                        c.drawCircle(px+(h%17)-8,py+((h/17)%13)-6,2.3f,paint);
                    }
                    if(h%13==3&&biome!=3&&!terrain.trail(tx,ty)) {
                        paint.setColor(0xFFB3A47B);
                        c.drawCircle(px-8,py+2,1.7f,paint);
                        paint.setColor(0xFFB3A1C1);
                        c.drawCircle(px+5,py-3,1.5f,paint);
                    }
                    paint.setColorFilter(null);
                    shadeGround(c,px,py,shade);
                }
            }
        }
        private void prop(Canvas c,int tx,int ty) {
            if(fogShade(tx,ty)==0xFF000000)return;
            int kind=wildernessProp(tx,ty);
            if(kind<0)return;
            float px=sx(tx+.5f,ty+.5f),py=sy(tx+.5f,ty+.5f);
            int group = kind==0 ? 0 : kind==2 ? 2 : kind==3 ? 3 : 1;
            int variation = Math.floorMod(terrain.decoration(tx,ty),4);
            Bitmap rendered = factoryProps[group][variation];
            if(rendered != null) {
                // Preserve the old wilderness size hierarchy, independently of
                // which Blender model variant was selected for this tile.
                int seed = terrain.decoration(tx,ty);
                float scale;
                if (group==1) {
                    // Four model variants x seven deterministic scale tiers.
                    // Kind 4 remains a landmark-sized tree, not an ordinary sapling.
                    scale = treeScale(kind,seed);
                } else if (group==2) {
                    scale = .70f + Math.floorMod(seed,8)*.105f;
                } else if (group==0) {
                    scale = .68f + Math.floorMod(seed,7)*.115f;
                } else {
                    scale = .80f + Math.floorMod(seed,6)*.12f;
                }
                float width = (group==1 ? 125f : group==3 ? 100f : 83f)*scale;
                float height = (group==1 ? 165f : group==3 ? 100f : 79f)*scale;
                // Keep the sprite's feet on the same tile anchor at every size.
                float anchorX = sx(tx+anchorOffset(kind),ty+anchorOffset(kind));
                float horizontalAnchor=group==1 ? trunkCenter[variation] : .5f;
                float left=anchorX-width*horizontalAnchor;
                paint.setColor(Color.WHITE);
                destination.set(left,py-height+3f,left+width,py+3f);
                c.drawBitmap(rendered,null,destination,paint);
                return;
            }
            if(kind==4) {
                drawTree(c,px+HALF_W,py,2.35f,tx*17+ty*31);
                return;
            }
            int variety=terrain.decoration(tx,ty);
            if(kind==0) {
                c.save();
                c.translate(px,py);
                c.scale(.7f+(variety%7)*.11f,.72f+((variety>>>3)%6)*.12f);
                px=0;py=0;
                // Low, rounded rock with ground shadow instead of a raised square platform.
                paint.setColor(0x66302224);
                c.drawOval(px-25,py-7,px+26,py+13,paint);
                paint.setColor(0xFF555D58);
                c.drawOval(px-19,py-16,px+19,py+8,paint);
                paint.setColor(0xFF849083);
                c.drawOval(px-14,py-17,px+13,py-5,paint);
                if((variety&2)!=0){paint.setColor(0xFF74876D);c.drawOval(px-12,py-15,px+4,py-9,paint);}
                c.restore();
                return;
            }
            if(kind==3) {
                paint.setColor(0x77332828);
                c.drawOval(px-28,py-8,px+29,py+11,paint);
                paint.setColor(0xFF777B6E);
                c.drawRect(px-15,py-35,px-5,py+2,paint);
                c.drawRect(px+6,py-26,px+17,py+2,paint);
                paint.setColor(0xFF9A9A83);
                c.drawRect(px-19,py-37,px-3,py-31,paint);
                return;
            }
            if(kind==2) {
                c.save();
                c.translate(px,py);
                c.scale(.7f+(variety%8)*.11f,.65f+((variety>>>4)%8)*.12f);
                px=0;py=0;
                paint.setColor(0xAA2A4734);
                c.drawOval(px-22,py-8,px+22,py+8,paint);
                paint.setColor(0xFF53764A);
                c.drawCircle(px-10,py-8,15,paint);
                c.drawCircle(px+8,py-12,18,paint);
                paint.setColor(0xFF6F955B);
                c.drawCircle(px+1,py-19,13,paint);
                if((variety&4)!=0){paint.setColor(0xFF9B9872);c.drawCircle(px+9,py-16,3,paint);}
                c.restore();
                return;
            }
            // A typical tree is substantially taller than the player; deterministic
            // size variation makes groves less uniform.
            int seed=terrain.decoration(tx,ty);
            float scale=1.12f+(seed%7)*.12f;
            drawTree(c,px,py,scale,seed);
        }
        private void drawTree(Canvas c,float px,float py,float scale,int seed) {
            c.save();
            c.translate(px,py);
            c.scale(scale,scale);
            float shade=(seed&3)*.03f;
            paint.setColor(0x77302628);
            c.drawOval(-30,-8,32,13,paint);
            paint.setColor(0xFF544838);
            c.drawRect(-5,-78,6,3,paint);
            paint.setColor(0xFF233D34);
            c.drawCircle(-19,-85,25,paint);
            c.drawCircle(18,-88,29,paint);
            c.drawCircle(0,-111,33,paint);
            paint.setColor(shade>.05f?0xFF365940:0xFF3D6144);
            c.drawCircle(-13,-107,27,paint);
            c.drawCircle(12,-117,24,paint);
            paint.setColor(0xFF567B4D);
            c.drawCircle(-8,-129,15,paint);
            c.restore();
        }

        @Override protected void onDraw(Canvas c) {
            long now=System.nanoTime();
            float dt=lastFrame==0?0:Math.min(.05f,(now-lastFrame)*1e-9f);
            lastFrame=now;
            update(dt);
            updateFog();
            fogSaveTime+=dt;
            if(fogSaveTime>=3f){fogSaveTime=0;saveExploration();}
            c.drawColor(Color.BLACK);
            float centerX=getWidth()*.5f,centerY=getHeight()*.44f;
            zoom=Math.max(.65f,Math.min(1.15f,getWidth()/768f));
            c.save();c.translate(centerX,centerY);c.scale(zoom,zoom);
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            if (inTown()) town.ground(c,cameraX,cameraY,townFog);
            else ground(c);
            if (attackRemaining>0f && !spellAttack) {
                float markerX=sx(punchX,punchY),markerY=sy(punchX,punchY);
                paint.setColor(0x98F2C774);
                c.drawOval(markerX-17f,markerY-8f,markerX+17f,markerY+8f,paint);
            }
            float heroDepth=x-y;
            if (inTown()) {
                town.props(c,cameraX,cameraY,heroDepth,townHero,townFog);
                if (dummyLocked) drawLockMarker(c,sx(TownHub.DUMMY_X,TownHub.DUMMY_Y),
                    sy(TownHub.DUMMY_X,TownHub.DUMMY_Y)-66f);
                if (arrowTime>0f) drawArrow(c);
                if(missile==null || sightAt(spellX,spellY,false)!=Senses.Sight.NONE)drawCantrip(c);
                c.restore();
                drawControls(c);
                postInvalidateOnAnimation();
                return;
            }
            boolean actorDrawn=false;
            for (Goblin goblin:goblins) {
                goblin.playerSight=sightAt(goblin.x,goblin.y,false);
                goblin.drawn=goblin.playerSight==Senses.Sight.NONE || Math.abs(goblin.x-cameraX)>VISIBLE_RADIUS+1
                    || Math.abs(goblin.y-cameraY)>VISIBLE_RADIUS+1;
            }
            for(int depth=-VISIBLE_RADIUS*2;depth<=VISIBLE_RADIUS*2;depth++){
                for(int xx=-VISIBLE_RADIUS;xx<=VISIBLE_RADIUS;xx++){
                    int yy=xx-depth;
                    if(Math.abs(yy)>VISIBLE_RADIUS)continue;
                    int tx=cx+xx,ty=cy+yy;
                    float propDepth=tx-ty;
                    for (Goblin goblin:goblins) {
                        if (!goblin.drawn && goblin.x-goblin.y<=propDepth
                            && (actorDrawn || goblin.x-goblin.y<=heroDepth)) {
                            drawGoblin(c,goblin);goblin.drawn=true;
                        }
                    }
                    if (!actorDrawn && propDepth>heroDepth) {
                        drawHero(c);actorDrawn=true;
                    }
                    int shade=fogShade(tx,ty);
                    if(shade!=0)paint.setColorFilter(memoryTint);
                    else if(fogSight(tx,ty)==Senses.Sight.DARKVISION)paint.setColorFilter(grayscale);
                    prop(c,tx,ty);
                    paint.setColorFilter(null);
                }
            }
            for (Goblin goblin:goblins)
                if (!goblin.drawn && goblin.x-goblin.y<=heroDepth) {
                    drawGoblin(c,goblin);goblin.drawn=true;
                }
            if(!actorDrawn)drawHero(c);
            for (Goblin goblin:goblins)
                if(!goblin.drawn)drawGoblin(c,goblin);
            if (arrowTime>0f && sightAt(arrowX,arrowY,false)!=Senses.Sight.NONE
                && sightAt(arrowEndX,arrowEndY,false)!=Senses.Sight.NONE) drawArrow(c);
            if(missile==null || sightAt(spellX,spellY,false)!=Senses.Sight.NONE) drawCantrip(c);
            c.restore();
            drawControls(c);
            postInvalidateOnAnimation();
        }
        private void drawCantrip(Canvas c) {
            if (casting!=null) {
                float width=5f+10f*(1f-attackRemaining/RigAttackPose.DURATION);
                paint.setColor(casting.color);
                c.drawCircle(sx(x,y)+8f,sy(x,y)-92f,width,paint);
            }
            if (missile==null) return;
            float xx=sx(spellX,spellY),yy=sy(spellX,spellY)-83f;
            float ax=sx(spellStartX,spellStartY)+8f,ay=sy(spellStartX,spellStartY)-92f;
            paint.setColor(missile.color);
            if (missile==Cantrip.FIRE) {
                c.drawCircle(xx,yy,9f,paint);
                paint.setColor(0xFFFFEDA4);
                c.drawCircle(xx,yy,4f,paint);
            } else {
                paint.setStrokeWidth(missile==Cantrip.FROST?5f:3f);
                if (missile==Cantrip.SHOCK) {
                    float midX=(ax+xx)*.5f,midY=(ay+yy)*.5f-13f;
                    c.drawLine(ax,ay,midX,midY,paint);c.drawLine(midX,midY,xx,yy,paint);
                } else c.drawLine(ax,ay,xx,yy,paint);
                paint.setStrokeWidth(1f);
            }
            if (spellFlash>0) {
                paint.setColor(missile.color);
                paint.setAlpha((int)(200*spellFlash/.22f));
                c.drawCircle(xx,yy,10f+11f*(1f-spellFlash/.22f),paint);
                paint.setAlpha(255);
            }
        }
        private void drawArrow(Canvas c) {
            float progress=1f-arrowTime/.19f;
            float tail=Math.max(0f,progress-.12f);
            float startX=sx(arrowX,arrowY), endX=sx(arrowEndX,arrowEndY);
            float startY=sy(arrowX,arrowY)-83f, endY=sy(arrowEndX,arrowEndY)-56f;
            paint.setColor(0xFFE6CB8C);
            paint.setStrokeWidth(2.5f);
            c.drawLine(startX+(endX-startX)*tail,startY+(endY-startY)*tail,
                startX+(endX-startX)*progress,startY+(endY-startY)*progress,paint);
            paint.setStrokeWidth(1f);
        }
        private void drawHero(Canvas c) {
            float px=sx(x,y),py=sy(x,y)-elevation(x,y);
            if (avatarActions.draw(c,px,py,facing)) return;
            if (spellAttack && attackRemaining>0f && avatarActions.drawCast(c,px,py,facing,attackRemaining)) return;
            boolean reacting=bakedHit!=null && hitRemaining>0f && attackRemaining<=0f;
            if (!reacting && !sneaking && pace>0 && blend>.15f && attackRemaining<=0f
                && avatarActions.drawRun(c,px,py,facing,pace,walkCycles-(float)Math.floor(walkCycles))) return;
            if(sneaking && !reacting && attackRemaining<=0f){
                Bitmap sneakSheet=blend>.15f?sneakWalk:sneakIdle;
                if(sneakSheet!=null){
                    int sneakFrame=blend>.15f?Math.floorMod((int)(walkCycles*12),8):0;
                    source.set(sneakFrame*168,facing*192,(sneakFrame+1)*168,(facing+1)*192);
                    destination.set(px-84,py-160,px+84,py+32);
                    paint.setColor(Color.WHITE);
                    c.drawBitmap(sneakSheet,source,destination,paint);
                    return;
                }
            }
            WeaponTrials.Motion motion=attackRemaining>0 ? activeMotion : selectedMotion();
            String weapon=attackRemaining>0 ? activeWeapon : selectedWeapon();
            if (!reacting && motion!=WeaponTrials.Motion.UNARMED && (attackRemaining>0 || weaponDrawn)) {
                float progress=0f;
                if (attackRemaining>0) progress=1-attackRemaining/RigAttackPose.DURATION;
                else if (blend>.15f)
                    progress=-1f-(walkCycles-(float)Math.floor(walkCycles));
                paint.setColor(Color.WHITE);
                if (weaponTrials.draw(c,paint,1,motion,weapon,facing,progress,px,py,
                    192f/164f,attackRemaining<=0f)) return;
            }
            Bitmap sheet=bakedElf;
            int frame=0;
            if (reacting) {
                sheet=bakedHit;
                frame=Math.min(4,(int)(5*(1-hitRemaining/HIT_REACTION_SECONDS)));
            } else if (attackRemaining>0) {
                sheet=bakedPunch;
                frame=Math.min(4,(int)(5*(1-attackRemaining/RigAttackPose.DURATION)));
            } else if (blend>.15f) {
                sheet=bakedWalk;
                frame=Math.floorMod((int)(walkCycles*8),8);
            }
            if (sheet==null) return;
            source.set(frame*168,facing*192,(frame+1)*168,(facing+1)*192);
            destination.set(px-84,py-160,px+84,py+32);
            paint.setColor(Color.WHITE);
            c.drawBitmap(sheet,source,destination,paint);
        }
        private void interact() {
            if (avatarActions.active()) return;
            if (inTown()) {
                interactInTown();
                return;
            }
            if (spellSelected || motions[motionIndex]!=WeaponTrials.Motion.UNARMED || hostile || !goblins.isEmpty()) {
                beginPunch();
                return;
            }
            int px=(int)Math.floor(x),py=(int)Math.floor(y);
            int closest=0,tx=0,ty=0;
            for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
                int xx=px+dx,yy=py+dy;
                int kind=terrain.landmark(xx,yy);
                if((kind==1 || kind==2)&&Math.hypot(xx+.5-x,yy+.5-y)<2.2){
                    closest=kind;tx=xx;ty=yy;
                }
            }
            if(closest==0){
                beginPunch();
                return;
            }
            avatarActions.play(AvatarActions.Clip.INTERACT);
            String key="visited_"+tx+"_"+ty;
            boolean visited=save.getBoolean(key,false);
            if(closest==1){
                health=100;
                worldMessage="HAMLET · Restored to full health";
            } else {
                if(!visited){gold+=25;experience+=40;}
                worldMessage=visited?"ANCIENT RUINS · Already explored":"ANCIENT RUINS · Found 25 gold and 40 XP";
            }
            save.edit().putBoolean(key,true).apply();
            messageRemaining=4f;
            save();
        }

        private void showAvatarActions() {
            if (attackRemaining>0f || defeatSource!=null) return;
            if (avatarActions.seated()) {
                avatarActions.play(AvatarActions.Clip.STAND);
                return;
            }
            if (avatarActions.active()) return;
            boolean tavern=inTown() && town.isInside();
            String[] labels=tavern ? new String[]{"Jump","Roll","Sit"} : new String[]{"Jump","Roll"};
            AvatarActions.Clip[] clips={AvatarActions.Clip.JUMP_START,AvatarActions.Clip.ROLL,AvatarActions.Clip.SIT};
            releaseStick();
            WildlandsUi.choose(getContext(),"Avatar actions",null,labels,-1,choice->{
                if (!avatarActions.active() && attackRemaining<=0f)
                    avatarActions.play(clips[choice]);
            });
        }
        private void drawControls(Canvas c) {
            float w=getWidth(), h=getHeight(), d=w/360f;
            float top=h*.69f, bottom=h-12*d;
            paint.setColor(0xFF0B292F);
            hudPath.reset();hudPath.moveTo(0,top+18*d);
            hudPath.quadTo(w*.04f,top,w*.22f,top);
            hudPath.quadTo(w*.36f,top,w*.40f,top-12*d);
            hudPath.quadTo(w*.50f,top-48*d,w*.60f,top-12*d);
            hudPath.quadTo(w*.64f,top,w*.78f,top);
            hudPath.quadTo(w*.96f,top,w,top+18*d);
            hudPath.lineTo(w,h);hudPath.lineTo(0,h);hudPath.close();
            c.drawPath(hudPath,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*d);paint.setColor(WildlandsUi.GOLD);c.drawPath(hudPath,paint);paint.setStyle(Paint.Style.FILL);
            float split=w*.40f, row=Math.min(61*d,(bottom-top-65*d)/2f);
            joystick.set(14*d,top+14*d,split-10*d,top+14*d+split-24*d);
            if(!stickActive){stickCenterX=joystick.centerX();stickCenterY=joystick.centerY();}
            float radius=joystick.width()*.46f;
            paint.setColor(WildlandsUi.GOLD);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);
            c.drawCircle(stickCenterX,stickCenterY,radius,paint);c.drawCircle(stickCenterX,stickCenterY,radius-4*d,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(WildlandsUi.GOLD);
            for(int i=0;i<4;i++){double a=i*Math.PI/2;hudDiamond(c,stickCenterX+(float)Math.cos(a)*radius,stickCenterY+(float)Math.sin(a)*radius,4*d);}
            paint.setColor(0xFFB7D6C7);
            c.drawCircle(stickCenterX+screenStickX*radius*.65f,stickCenterY+screenStickY*radius*.65f,radius*.38f,paint);
            float mid=(split+w-10*d)/2;
            jumpHud.set(split+8*d,top+12*d,mid-3*d,top+row);
            crouchHud.set(mid+4*d,top+12*d,w-10*d,top+row);
            attackButton.set(split+8*d,top+row+7*d,mid-3*d,top+2*row-3*d);
            secondaryHud.set(mid+4*d,top+row+7*d,w-10*d,top+2*row-3*d);
            inventoryHud.set(14*d,bottom-43*d,w*.5f-5*d,bottom);
            pauseHud.set(w*.5f+5*d,bottom-43*d,w-14*d,bottom);
            hudControl(c,jumpHud,"↗","Jump",jumpCooldown<=0&&!jumping,d);
            hudControl(c,crouchHud,"↓","Sneak",sneaking,d);
            hudControl(c,attackButton,"⚔",inTown()&&!atPracticeTarget()?"Interact":spellSelected?"Cast":"Attack",playerRecharge<=0,d);
            hudControl(c,secondaryHud,"➶","Attack mode",false,d);
            hudControl(c,inventoryHud,"▣","Inventory",false,d);
            hudControl(c,pauseHud,"Ⅱ","Pause",false,d);
            paint.setColor(WildlandsUi.GOLD);paint.setStrokeWidth(d);
            c.drawLine(20*d,bottom-51*d,w*.47f,bottom-51*d,paint);c.drawLine(w*.53f,bottom-51*d,w-20*d,bottom-51*d,paint);hudDiamond(c,w*.5f,bottom-51*d,5*d);
            c.drawLine(split-2*d,top+15*d,split-2*d,top+2*row-3*d,paint);
            paint.setTypeface(WildlandsUi.SERIF);paint.setTextSize(23*d);paint.setTextAlign(Paint.Align.CENTER);
            hudIcon(c,"tree",w*.5f,top-10*d,34*d);paint.setTextAlign(Paint.Align.LEFT);
            hud.set(8*d,9*d,w*.55f,51*d);
            WildlandsUi.plate(c,paint,hudPath,hud,7*d,WildlandsUi.TEAL,WildlandsUi.GOLD,d);
            paint.setTypeface(WildlandsUi.SERIF);paint.setTextSize(11*d);paint.setColor(WildlandsUi.INK);
            c.drawText(inTown()?town.isInside()?"WILDLANDS · TAVERN":"WILDLANDS · TOWN":"WILDLANDS · FREE ROAM",14*d,24*d,paint);
            c.drawLine(14*d,29*d,hud.right-6*d,29*d,paint);
            c.drawText("HP "+health+"  XP "+experience+"  GOLD "+gold,14*d,43*d,paint);
            targetButton.set(w*.57f,14*d,w*.65f,46*d);
            autoAttackButton.set(w*.67f,14*d,w*.75f,46*d);
            exitButton.set(w*.77f,14*d,w-8*d,46*d);
            hudControl(c,targetButton,"◎","",autoLock,d);
            hudControl(c,autoAttackButton,"A","",autoAttack,d);
            hudControl(c,exitButton,"","☰ Menu",false,d);
            paint.setTextSize(12*d);paint.setColor(WildlandsUi.INK);
            if(onQuest)c.drawText("Goblins "+questKills+"/"+TownHub.GOAL,14*d,65*d,paint);
            if(messageRemaining>0)c.drawText(worldMessage,14*d,(onQuest?80:65)*d,paint);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
        }
        private void hudControl(Canvas c,RectF r,String icon,String label,boolean active,float d){
            if(active){for(int i=4;i>=1;i--){innerFrame.set(r);innerFrame.inset(-i*d*.55f,-i*d*.55f);WildlandsUi.plate(c,paint,hudPath,innerFrame,9*d,Color.TRANSPARENT,((5-i)*24<<24)|0xFFD58A,1.5f*d);}}
            WildlandsUi.plate(c,paint,hudPath,r,8*d,0xFF102D33,active?0xFFFFDA91:WildlandsUi.GOLD,active?2*d:d);
            innerFrame.set(r);innerFrame.inset(3*d,3*d);
            WildlandsUi.plate(c,paint,hudPath,innerFrame,6*d,Color.TRANSPARENT,WildlandsUi.GOLD,.7f*d);
            paint.setColor(WildlandsUi.INK);paint.setTypeface(WildlandsUi.SERIF);paint.setTextAlign(Paint.Align.CENTER);
            boolean horizontal=label.equals("Inventory")||label.equals("Pause");
            if(!icon.isEmpty())hudIcon(c,icon,horizontal?r.left+r.width()*.23f:r.centerX(),horizontal||label.isEmpty()?r.centerY():r.top+r.height()*.37f,(horizontal?23:label.isEmpty()?18:27)*d);
            if(!label.isEmpty()){paint.setTextSize(12*d);c.drawText(label,horizontal?r.left+r.width()*.61f:r.centerX(),horizontal||icon.isEmpty()?r.centerY()-(paint.ascent()+paint.descent())/2:r.bottom-8*d,paint);}
            paint.setTextAlign(Paint.Align.LEFT);
        }
        private void hudDiamond(Canvas c,float x,float y,float r){
            Path p=new Path();p.moveTo(x,y-r);p.lineTo(x+r,y);p.lineTo(x,y+r);p.lineTo(x-r,y);p.close();c.drawPath(p,paint);
        }
        private void hudIcon(Canvas c,String icon,float x,float y,float size){
            c.save();c.translate(x-size/2,y-size/2);c.scale(size/32f,size/32f);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(WildlandsUi.GOLD);p.setStrokeWidth(2.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(icon){
                case "↗":case "↓":
                    boolean crouch=icon.equals("↓");
                    c.drawCircle(crouch?21:22,5,4,p);
                    p.setStrokeWidth(5);c.drawLine(19,11,crouch?12:15,crouch?20:18,p);
                    p.setStrokeWidth(3.5f);c.drawLine(18,12,25,17,p);c.drawLine(25,17,29,12,p);
                    c.drawLine(16,13,9,10,p);c.drawLine(9,10,4,14,p);
                    c.drawLine(crouch?12:15,crouch?20:18,22,23,p);c.drawLine(22,23,crouch?17:26,29,p);
                    c.drawLine(crouch?12:15,crouch?20:18,9,25,p);c.drawLine(9,25,4,29,p);break;
                case "⚔":case "➶":
                    for(int i=0;i<(icon.equals("⚔")?2:1);i++){c.save();if(i==1){c.translate(32,0);c.scale(-1,1);}
                    Path blade=new Path();blade.moveTo(27,3);blade.lineTo(25,11);blade.lineTo(10,25);blade.lineTo(7,22);blade.close();c.drawPath(blade,p);
                    c.drawLine(5,18,14,27,p);c.drawLine(9,23,4,28,p);c.restore();}break;
                case "▣":c.drawRoundRect(6,8,26,29,4,4,p);p.setStyle(Paint.Style.STROKE);c.drawRoundRect(11,2,21,12,3,3,p);p.setColor(0xFF102D33);c.drawLine(12,9,12,26,p);c.drawLine(20,9,20,26,p);c.drawLine(7,20,25,20,p);break;
                case "Ⅱ":c.drawRect(8,4,13,28,p);c.drawRect(19,4,24,28,p);break;
                case "◎":p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);c.drawCircle(16,16,10,p);c.drawCircle(16,16,4,p);c.drawLine(16,1,16,10,p);c.drawLine(16,22,16,31,p);c.drawLine(1,16,10,16,p);c.drawLine(22,16,31,16,p);break;
                case "tree":p.setStrokeWidth(2.6f);c.drawLine(16,29,16,8,p);c.drawLine(8,30,24,30,p);for(int i=0;i<5;i++){float yy=10+i*3;float xx=5+i*.7f;p.setStrokeWidth(1.2f);c.drawLine(16,yy+5,xx,yy-3,p);c.drawLine(16,yy+5,32-xx,yy-3,p);c.drawLine(xx+4,yy,xx+2,yy-7,p);c.drawLine(28-xx,yy,30-xx,yy-7,p);}break;
                default:p.setTypeface(WildlandsUi.SERIF);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(28);c.drawText(icon,16,16-(p.ascent()+p.descent())/2,p);
            }c.restore();
        }
        private void button(Canvas c,RectF bounds,String label) {
            float d=getResources().getDisplayMetrics().density;
            WildlandsUi.plate(c,paint,hudPath,bounds,8*d,WildlandsUi.TEAL,WildlandsUi.GOLD,d);
            innerFrame.set(bounds);
            innerFrame.inset(3*d,3*d);
            WildlandsUi.plate(c,paint,hudPath,innerFrame,6*d,Color.TRANSPARENT,0x909E895E,d);
            paint.setColor(WildlandsUi.INK);paint.setTextSize(13*d);
            paint.setTypeface(WildlandsUi.SERIF);
            boolean reserveDot=bounds==attackButton;
            hudText.set(paint);
            String visible=android.text.TextUtils.ellipsize(label,hudText,
                bounds.width()-(reserveDot?27:16)*d,android.text.TextUtils.TruncateAt.END).toString();
            c.drawText(visible,bounds.left+8*d,bounds.centerY()-(paint.ascent()+paint.descent())*.5f,paint);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
        }
        private void stick(float px,float py) {
            float d=getResources().getDisplayMetrics().density;
            float dx=(px-stickCenterX)/(48*d),dy=(py-stickCenterY)/(48*d);
            float mag=(float)Math.hypot(dx,dy);
            if(mag>1){dx/=mag;dy/=mag;mag=1;}
            screenStickX=dx;screenStickY=dy;
            // Deadzone suppresses noise; outside it, displacement controls speed.
            if(mag<.12f){stickX=stickY=0;return;}
            float power=Math.min(1f,(mag-.12f)/.88f);
            // Inverse of screen=(x+y)*HALF_W,(x-y)*HALF_H.
            float wx=dx/HALF_W+dy/HALF_H;
            float wy=dx/HALF_W-dy/HALF_H;
            float length=(float)Math.hypot(wx,wy);
            if(length>0){stickX=wx/length*power;stickY=wy/length*power;}
            else {stickX=stickY=0;}
        }
        private boolean selectVisibleActor(float px,float py) {
            for(Goblin goblin:goblins){
                if(!lockable(goblin))continue;
                float gx=getWidth()*.5f+sx(goblin.x,goblin.y)*zoom;
                float gy=getHeight()*.44f+sy(goblin.x,goblin.y)*zoom;
                if(Math.abs(px-gx)<=25*zoom && py<=gy && py>=gy-105*zoom){
                    lockedTarget=goblin;
                    return true;
                }
            }
            return false;
        }
        private void releaseStick() {
            pointer=-1;stickActive=false;
            stickX=stickY=screenStickX=screenStickY=0f;
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),index=e.getActionIndex();
            int id=e.getPointerId(index);
            if(action==MotionEvent.ACTION_MOVE) {
                int active=e.findPointerIndex(pointer);
                if(active>=0)stick(e.getX(active),e.getY(active));
                else if(pointer!=-1)releaseStick();
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL) {
                releaseStick();return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP) {
                if(id==pointer)releaseStick();
                return true;
            }
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
                float px=e.getX(index),py=e.getY(index);
                if(exitButton.contains(px,py)) {
                    save();getContext().startActivity(new Intent(getContext(),MainActivity.class));
                    ((Activity)getContext()).finish();return true;
                }
                if(targetButton.contains(px,py)){autoLock=!autoLock;if(autoLock)acquireTarget();else {lockedTarget=null;dummyLocked=false;}return true;}
                if(autoAttackButton.contains(px,py)){autoAttack=!autoAttack;return true;}
                if(jumpHud.contains(px,py)){startJump();return true;}
                if(crouchHud.contains(px,py)){sneaking=!sneaking;pace=0;return true;}
                if(secondaryHud.contains(px,py)){chooseAttackMode();return true;}
                if(inventoryHud.contains(px,py)||pauseHud.contains(px,py))return true;
                if(attackButton.contains(px,py)){
                    interact();
                    return true;
                }
                // Floating touchpad: any touch in the left lower half
                // captures its own pointer; action buttons remain independent.
                if(pointer==-1 && joystick.contains(px,py)) {
                    pointer=id;stickActive=true;
                    stickCenterX=joystick.centerX();stickCenterY=joystick.centerY();
                    stick(px,py);return true;
                }
                if(!inTown() && selectVisibleActor(px,py))return true;
                if(paused)paused=false;
                return true;
            }
            return true;
        }
    }
}
