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
    private World view;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PlayerCharacter hero = PlayerCharacter.loadCompleted(this);
        if (hero == null && !getIntent().getBooleanExtra(OutdoorActivity.EXTRA_FREE_EXPLORE, false)) {
            startActivity(new Intent(this, CharacterCreatorActivity.class));
            finish();
            return;
        }
        view = new World(this, hero);
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
        private static final float COMBAT_NOTICE = 3.5f;
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
        private final Bitmap atlas;
        private final Bitmap vegetation;
        private final float[] trunkCenter = {.5f,.5f,.5f,.5f};
        private final float[] trunkWidth = {.15f,.15f,.15f,.15f};
        private final Bitmap[][] factoryProps = new Bitmap[4][4];
        private final Bitmap bakedElf, bakedWalk, bakedPunch;
        private final Bitmap classicElf, classicWalk, classicPunch;
        private final Bitmap[] maleClassic = new Bitmap[3], malePetite = new Bitmap[3];
        private int styleIndex;
        private final Bitmap goblinIdle, goblinWalk, goblinAttack;
        private final WeaponTrials weaponTrials;
        private final WeaponTrials.Motion[] motions=WeaponTrials.Motion.values();
        private final int[] selectedWeapons=new int[motions.length];
        private int motionIndex;
        private final int dexterityModifier, strengthModifier;
        private WeaponTrials.Motion activeMotion=WeaponTrials.Motion.UNARMED;
        private String activeWeapon="";
        private float arrowTime,arrowX,arrowY,arrowEndX,arrowEndY;
        private final RectF motionButton=new RectF(), weaponButton=new RectF();
        private final int playerAttackBonus, playerDamage, playerAc;
        private final java.util.Random combatDice = new java.util.Random();
        private static final int MAX_GOBLINS=5;
        private static final class Goblin {
            float x,y,homeX,homeY,walkCycle,swing,recharge;
            int facing,hp=GOBLIN_HP;
            WeaponTrials.Motion motion=WeaponTrials.Motion.THRUST;
            String weapon="dagger";
            boolean moving,impact,aggro,drawn;
        }
        private final java.util.ArrayList<Goblin> goblins=new java.util.ArrayList<>();
        private float spawnCooldown=2f,playerRecharge,punchX,punchY;
        private boolean hostile,playerImpact;
        private final Bitmap prototypeFloor, prototypeTree, prototypeLedge;
        private final Bitmap prototypeIdle, prototypeWalk, prototypePunch;
        private final boolean bakedEnvironment;
        private final RiggedElf character;
        private final ElfAppearance appearance;
        private final float movementFactor;
        private final GaitModel gait = new GaitModel();
        private float x, y, cameraX, cameraY, stickX, stickY;
        private float screenStickX, screenStickY;
        private float stickCenterX, stickCenterY;
        private boolean stickActive;
        private float idleTime, walkCycles, attackRemaining, blend;
        private static final float AUTO_SHEATHE_SECONDS=6f;
        private float weaponQuietSeconds=AUTO_SHEATHE_SECONDS;
        private boolean weaponDrawn;
        private int facing, pointer = -1;
        private boolean running, paused;
        private int health, experience, gold;
        private String worldMessage="";
        private float messageRemaining;
        private long lastFrame;
        private final RectF joystick = new RectF(), runButton = new RectF(), attackButton = new RectF();
        private final RectF exitButton = new RectF(), styleButton = new RectF();
        private boolean petiteStyle;
        private float zoom = 1f;

        World(Context context, PlayerCharacter hero) {
            super(context);
            weaponTrials=new WeaponTrials(context);
            dexterityModifier=hero==null ? 0 : PlayerCharacter.modifier(hero.score(1));
            strengthModifier=hero==null ? 0 : PlayerCharacter.modifier(hero.score(0));
            save = context.getSharedPreferences("wildlands_world", Context.MODE_PRIVATE);
            x = save.getFloat("player_x", .5f);
            y = save.getFloat("player_y", .5f);
            if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 1000000
                    || Math.abs(y) > 1000000) { x=.5f; y=.5f; }
            cameraX = x; cameraY = y;
            health=save.getInt("hero_health",100);
            experience=save.getInt("hero_experience",0);
            gold=save.getInt("hero_gold",0);
            appearance = hero == null ? ElfAppearance.COILED : hero.appearance;
            styleIndex=Math.floorMod(save.getInt("hero_style_index",
                save.getBoolean("petite_style",appearance==ElfAppearance.PETITE)?1:0),4);
            petiteStyle=(styleIndex%2)==1;
            character = new RiggedElf(context, appearance);
            movementFactor = hero == null ? 1f : hero.loadout().movementFactor();
            playerAttackBonus = hero == null ? 2 : hero.attackBonus();
            playerDamage = hero == null ? 1 : Math.max(1, 1 + PlayerCharacter.modifier(hero.score(0)));
            playerAc = hero == null ? 10 : hero.loadout().armorClass();
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
            bakedElf = load(context, "blender_world/petite_idle.png");
            bakedWalk = load(context, "blender_world/petite_walk.png");
            bakedPunch = load(context, "blender_world/petite_punch.png");
            classicElf = load(context, "blender_world/classic_idle.png");
            classicWalk = load(context, "blender_world/classic_walk.png");
            classicPunch = load(context, "blender_world/classic_punch.png");
            String[] animations={"idle","walk","punch"};
            for (int i=0;i<animations.length;i++) {
                maleClassic[i]=load(context,"blender_world/classic_male_"+animations[i]+".png");
                malePetite[i]=load(context,"blender_world/petite_male_"+animations[i]+".png");
            }
            prototypeFloor = load(context,"isometric_lab/floor.png");
            prototypeTree = load(context,"isometric_lab/tree.png");
            prototypeLedge = load(context,"isometric_lab/ledge.png");
            prototypeIdle = load(context,"isometric_lab/idle.png");
            prototypeWalk = load(context,"isometric_lab/walk.png");
            prototypePunch = load(context,"isometric_lab/punch.png");
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
        void save() {
            save.edit().putFloat("player_x", x).putFloat("player_y", y)
                .putInt("hero_health",health).putInt("hero_experience",experience)
                .putInt("hero_gold",gold).apply();
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
            int minX=(int)Math.floor(wx-radius), maxX=(int)Math.floor(wx+radius);
            int minY=(int)Math.floor(wy-radius), maxY=(int)Math.floor(wy+radius);
            for (int tx=minX;tx<=maxX;tx++) for (int ty=minY;ty<=maxY;ty++) {
                if (terrain.wildernessProp(tx,ty)==3) return false;
            }
            // A corner trunk can overlap the tiles beside the tile that owns it.
            float searchRadius=radius+.30f;
            for (int tx=(int)Math.floor(wx-searchRadius)-1;tx<=(int)Math.floor(wx+searchRadius);tx++) {
                for (int ty=(int)Math.floor(wy-searchRadius)-1;ty<=(int)Math.floor(wy+searchRadius);ty++) {
                    int kind=terrain.wildernessProp(tx,ty);
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
                goblin.facing=heading(x-px,y-py);
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
        private String selectedWeapon() { return motions[motionIndex].weapons[selectedWeapons[motionIndex]]; }
        private float reach(WeaponTrials.Motion motion,String weapon) {
            if (motion!=WeaponTrials.Motion.BOW) return MELEE_REACH;
            // Limit the test shot to distances readable on the current camera.
            return weapon.equals("longbow") ? 6f : 4f;
        }
        private boolean shotClear(float ax,float ay,float bx,float by) {
            int samples=(int)Math.ceil(Math.hypot(bx-ax,by-ay)*8);
            for (int i=1;i<samples;i++) {
                float t=(float)i/samples;
                float xx=ax+(bx-ax)*t, yy=ay+(by-ay)*t;
                if (!clear(xx,yy,xx,yy,.035f)) return false;
            }
            return true;
        }
        private void beginPunch() {
            if (attackRemaining>0 || playerRecharge>0) return;
            activeMotion=motions[motionIndex];
            activeWeapon=selectedWeapon();
            // Drawing happens automatically with the attack and never
            // consumes a separate action or recharge.
            weaponDrawn=activeMotion!=WeaponTrials.Motion.UNARMED;
            weaponQuietSeconds=0f;
            attackRemaining=RigAttackPose.DURATION;
            playerRecharge=PLAYER_ACTION_REFILL;
            playerImpact=true;
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
            String[] dice=weapon.damage.split("d",2);
            int count=Integer.parseInt(dice[0]), sides=Integer.parseInt(dice[1]);
            int damage=ability;
            for (int i=0;i<count;i++) damage+=combatDice.nextInt(sides)+1;
            return Math.max(1,damage);
        }
        private void resolvePunch() {
            Goblin target=null;
            boolean ranged=activeMotion==WeaponTrials.Motion.BOW;
            float nearest=reach(activeMotion,activeWeapon)+.001f;
            for (Goblin goblin:goblins) {
                float distance=distanceToGoblin(goblin);
                if (distance>nearest || (distance>.01f &&
                    heading(goblin.x-x,goblin.y-y)!=facing)) continue;
                if (ranged && !shotClear(x,y,goblin.x,goblin.y)) continue;
                target=goblin;nearest=distance;
            }
            if (ranged) fireArrow(x,y,target==null?punchX:target.x,target==null?punchY:target.y);
            if (target==null) {
                if (hostile) combatMessage("No enemy in reach");
                return;
            }
            hostile=true;running=false;
            Equipment.Item item=Equipment.get(activeWeapon);
            int ability=ranged ? dexterityModifier : item!=null && item.finesse
                ? Math.max(strengthModifier,dexterityModifier) : strengthModifier;
            int bonus=playerAttackBonus-strengthModifier+ability;
            int armor=target.motion==WeaponTrials.Motion.THRUST ? GOBLIN_AC : 13;
            if (combatDice.nextInt(20)+1+bonus<armor) {
                combatMessage("Attack missed");return;
            }
            int damage=item==null ? playerDamage : rollDamage(item,ability);
            target.hp=Math.max(0,target.hp-damage);
            if (target.hp>0) {
                combatMessage("Goblin hit · "+damage+" damage");return;
            }
            goblins.remove(target);
            int reward=target.motion==WeaponTrials.Motion.THRUST ? 25 : 50;
            experience+=reward;
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
                float distance=distanceToGoblin(goblin);
                goblin.aggro=distance<=(goblin.aggro?COMBAT_LEASH:COMBAT_NOTICE);
                if (!goblin.aggro) {
                    goblin.swing=0f;
                    continue;
                }
                hostile=true;running=false;
                goblin.recharge=Math.max(0f,goblin.recharge-dt);
                float range=reach(goblin.motion,goblin.weapon);
                boolean canHit=distance<=range && (goblin.motion!=WeaponTrials.Motion.BOW
                    || shotClear(goblin.x,goblin.y,x,y));
                if (goblin.swing>0) {
                    goblin.swing=Math.max(0f,goblin.swing-dt);
                    if (goblin.impact && goblin.swing<=IMPACT_TIME) {
                        goblin.impact=false;
                        if (goblin.motion==WeaponTrials.Motion.BOW && canHit) fireArrow(goblin.x,goblin.y,x,y);
                        if (!canHit) combatMessage("You evaded the goblin");
                        else if (combatDice.nextInt(20)+5<playerAc) combatMessage("Goblin missed");
                        else {
                            int damage=rollDamage(Equipment.get(goblin.weapon),2);
                            health=Math.max(0,health-damage);
                            combatMessage("Goblin "+goblin.weapon+" · "+damage+" damage");
                            if (health==0) {
                                health=100;
                                retreatFromGoblin(goblin);
                                for (Goblin other:goblins) {
                                    other.x=other.homeX;other.y=other.homeY;
                                    other.aggro=false;other.swing=0f;
                                }
                                hostile=false;
                                combatMessage("Goblin pack defeated you · Returned to safety");
                            }
                            save();
                        }
                    }
                    continue;
                }
                goblin.facing=heading(x-goblin.x,y-goblin.y);
                if (canHit && distance<=range*.90f) {
                    if (goblin.recharge==0f) {
                        goblin.swing=RigAttackPose.DURATION;
                        goblin.recharge=GOBLIN_ATTACK_PERIOD;
                        goblin.impact=true;
                    }
                    continue;
                }
                float delta=Math.max(0f,Math.min(distance-.83f,dt*WALK_SPEED));
                float nextX=goblin.x+(x-goblin.x)*delta/distance;
                float nextY=goblin.y+(y-goblin.y)*delta/distance;
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
            if (goblin.motion==WeaponTrials.Motion.THRUST
                || !weaponTrials.draw(c,paint,4,goblin.motion,goblin.weapon,goblin.facing,
                    progress,px,py,192f/164f)) c.drawBitmap(sheet,source,destination,paint);
            if (goblin.aggro) {
                paint.setColor(0xFF322A2B);
                c.drawRect(px-18,py-111,px+18,py-107,paint);
                paint.setColor(0xFFD27060);
                c.drawRect(px-18,py-111,px-18+36f*goblin.hp/(goblin.motion==WeaponTrials.Motion.THRUST ? GOBLIN_HP : 10),py-107,paint);
            }
        }
        private float elevation(float wx,float wy) {
            int tx=Math.round(wx),ty=Math.round(wy);
            return 0f;
        }
        private void update(float dt) {
            if (paused) return;
            idleTime+=dt;
            messageRemaining=Math.max(0,messageRemaining-dt);
            playerRecharge=Math.max(0,playerRecharge-dt);
            arrowTime=Math.max(0,arrowTime-dt);
            if (attackRemaining>0) {
                attackRemaining=Math.max(0,attackRemaining-dt);
                if (playerImpact && attackRemaining<=IMPACT_TIME) {
                    playerImpact=false;
                    resolvePunch();
                }
            }
            updateGoblins(dt);
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
            if (magnitude>.08f && attackRemaining==0) {
                float speed=WALK_SPEED*movementFactor*(running?1.8f:1f);
                float dx=stickX*speed*dt, dy=stickY*speed*dt;
                float oldX=x,oldY=y;
                if (clear(x,y,x+dx,y,PLAYER_RADIUS)) x+=dx;
                if (clear(x,y,x,y+dy,PLAYER_RADIUS)) y+=dy;
                float actualX=x-oldX,actualY=y-oldY;
                float travelled=(float)Math.hypot(actualX,actualY);
                moving=travelled>0.00001f;
                if (moving) {
                    facing=heading(actualX,actualY);
                    gait.advance(travelled);
                    walkCycles=gait.cycles();
                }
            }
            blend=Math.max(0,Math.min(1,blend+(moving?10f:-13f)*dt));
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
                }
            }
        }
        private void prop(Canvas c,int tx,int ty) {
            int kind=terrain.wildernessProp(tx,ty);
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
            c.drawColor(0xFF14242A);
            float centerX=getWidth()*.5f,centerY=getHeight()*.44f;
            zoom=Math.max(.65f,Math.min(1.15f,getWidth()/768f));
            c.save();c.translate(centerX,centerY);c.scale(zoom,zoom);
            int cx=(int)Math.floor(cameraX),cy=(int)Math.floor(cameraY);
            ground(c);
            if (attackRemaining>0f) {
                float markerX=sx(punchX,punchY),markerY=sy(punchX,punchY);
                paint.setColor(0x98F2C774);
                c.drawOval(markerX-17f,markerY-8f,markerX+17f,markerY+8f,paint);
            }
            float heroDepth=x-y;
            boolean actorDrawn=false;
            for (Goblin goblin:goblins) goblin.drawn=false;
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
                    prop(c,tx,ty);
                }
            }
            for (Goblin goblin:goblins)
                if (!goblin.drawn && goblin.x-goblin.y<=heroDepth) {
                    drawGoblin(c,goblin);goblin.drawn=true;
                }
            if(!actorDrawn)drawHero(c);
            for (Goblin goblin:goblins)
                if(!goblin.drawn)drawGoblin(c,goblin);
            if (arrowTime>0f) drawArrow(c);
            c.restore();
            drawControls(c);
            postInvalidateOnAnimation();
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
            // Both selectable styles use their own Blender-baked 8-direction
            // character; never silently substitute the old Coiled Puff prototype.
            Bitmap idle = styleIndex==3 ? malePetite[0] : styleIndex==2 ? maleClassic[0] : petiteStyle ? bakedElf : classicElf;
            Bitmap walk = styleIndex==3 ? malePetite[1] : styleIndex==2 ? maleClassic[1] : petiteStyle ? bakedWalk : classicWalk;
            Bitmap punch = styleIndex==3 ? malePetite[2] : styleIndex==2 ? maleClassic[2] : petiteStyle ? bakedPunch : classicPunch;
            WeaponTrials.Motion motion=attackRemaining>0 ? activeMotion : motions[motionIndex];
            String weapon=attackRemaining>0 ? activeWeapon : selectedWeapon();
            if (motion!=WeaponTrials.Motion.UNARMED && (attackRemaining>0 || weaponDrawn)) {
                float progress=attackRemaining>0 ? 1-attackRemaining/RigAttackPose.DURATION : 0f;
                paint.setColor(Color.WHITE);
                if (weaponTrials.draw(c,paint,styleIndex,motion,weapon,facing,progress,px,py,
                    (petiteStyle ? 1f : 1.12f)*192f/164f,attackRemaining<=0f)) return;
            }
            if (idle!=null && walk!=null && punch!=null) {
                Bitmap sheet=idle;
                int frame=0;
                if (attackRemaining>0) {
                    sheet=punch;
                    frame=Math.min(4,(int)(5*(1-attackRemaining/RigAttackPose.DURATION)));
                } else if (blend>.15f) {
                    sheet=walk;
                    frame=Math.floorMod((int)(walkCycles*8),8);
                }
                source.set(frame*168,facing*192,(frame+1)*168,(facing+1)*192);
                float size = petiteStyle ? 1f : 1.12f;
                destination.set(px-84*size,py-160*size,px+84*size,py+32*size);
                paint.setColor(Color.WHITE);
                c.drawBitmap(sheet,source,destination,paint);
                return;
            }
            if (prototypeIdle!=null
                    && prototypeWalk!=null && prototypePunch!=null) {
                Bitmap sheet=prototypeIdle;
                int frame=Math.floorMod((int)(idleTime*2f),4);
                if(attackRemaining>0){
                    sheet=prototypePunch;
                    frame=Math.min(6,(int)(7*(1-attackRemaining/RigAttackPose.DURATION)));
                } else if(blend>.15f){
                    sheet=prototypeWalk;
                    frame=Math.floorMod((int)(walkCycles*8),8);
                }
                int cardinal=(facing+1)/2%4;
                source.set(frame*144,cardinal*160,(frame+1)*144,(cardinal+1)*160);
                destination.set(px-72,py-135.303f,px+72,py+24.697f);
                paint.setColor(Color.WHITE);
                c.drawBitmap(sheet,source,destination,paint);
                return;
            }
            character.draw(c,px,py,.75f*appearance.heightScale,(facing+1)/2%4,walkCycles,
                blend,idleTime,attackRemaining>0?1-attackRemaining/RigAttackPose.DURATION:0,
                false,0f);
        }
        private void interact() {
            if (motions[motionIndex]!=WeaponTrials.Motion.UNARMED || hostile || !goblins.isEmpty()) {
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
        private void drawControls(Canvas c) {
            float d=getResources().getDisplayMetrics().density;
            float bottom=getHeight()-18*d;
            joystick.set(18*d,bottom-115*d,133*d,bottom);
            runButton.set(getWidth()-125*d,bottom-111*d,getWidth()-18*d,bottom-70*d);
            attackButton.set(getWidth()-125*d,bottom-59*d,getWidth()-18*d,bottom);
            exitButton.set(getWidth()-94*d,18*d,getWidth()-12*d,54*d);
            styleButton.set(getWidth()-153*d,62*d,getWidth()-12*d,100*d);
            motionButton.set(getWidth()-175*d,106*d,getWidth()-12*d,141*d);
            weaponButton.set(getWidth()-175*d,146*d,getWidth()-12*d,181*d);
            if (!stickActive) {stickCenterX=joystick.centerX();stickCenterY=joystick.centerY();}
            paint.setColor(0x99354B52);c.drawCircle(stickCenterX,stickCenterY,55*d,paint);
            paint.setColor(0xFFB7D6C7);
            c.drawCircle(stickCenterX+screenStickX*36*d,stickCenterY+screenStickY*36*d,19*d,paint);
            button(c,runButton,hostile?"IN COMBAT":running?"RUN ON":"RUN");
            button(c,attackButton,hostile || motionIndex!=0?"ATTACK":"ATTACK / USE");
            paint.setColor(0xFFF1D8AA);
            if (playerRecharge>0) {
                paint.setStyle(Paint.Style.STROKE);
                c.drawCircle(attackButton.right-12*d,attackButton.centerY(),4*d,paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                c.drawCircle(attackButton.right-12*d,attackButton.centerY(),4*d,paint);
            }
            button(c,exitButton,"MENU");
            button(c,styleButton,new String[]{"F: CLASSIC","F: PETITE","M: CLASSIC","M: PETITE"}[styleIndex]);
            button(c,motionButton,motions[motionIndex].title+" ›");
            Equipment.Item selection=Equipment.get(selectedWeapon());
            button(c,weaponButton,selection==null ? "Bare hands" : selection.name+" ›");
            paint.setTextSize(13*d);paint.setColor(0xFFF1D8AA);
            c.drawText("WILDLANDS · FREE ROAM",15*d,32*d,paint);
            c.drawText("HP "+health+"   XP "+experience+"   GOLD "+gold,15*d,50*d,paint);
            if(messageRemaining>0){
                paint.setTextSize(12*d);
                c.drawText(worldMessage,15*d,72*d,paint);
            }
            if(paused) {
                paint.setTextSize(25*d);
                c.drawText("PAUSED",getWidth()*.38f,getHeight()*.35f,paint);
            }
        }
        private void button(Canvas c,RectF bounds,String label) {
            paint.setColor(0xCC304952);c.drawRoundRect(bounds,9,9,paint);
            paint.setColor(0xFFF1D8AA);paint.setTextSize(13*getResources().getDisplayMetrics().density);
            c.drawText(label,bounds.left+12,bounds.centerY()+5,paint);
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
                if (motionButton.contains(px,py)) {
                    motionIndex=(motionIndex+1)%motions.length;
                    return true;
                }
                if (weaponButton.contains(px,py)) {
                    selectedWeapons[motionIndex]=(selectedWeapons[motionIndex]+1)%motions[motionIndex].weapons.length;
                    return true;
                }
                if(styleButton.contains(px,py)){
                    styleIndex=(styleIndex+1)%4;
                    petiteStyle=(styleIndex%2)==1;
                    save.edit().putInt("hero_style_index",styleIndex)
                        .putBoolean("petite_style",petiteStyle).apply();
                    return true;
                }
                if(runButton.contains(px,py)){if(!hostile)running=!running;return true;}
                if(attackButton.contains(px,py)){
                    interact();
                    return true;
                }
                // Floating touchpad: any touch in the left lower half
                // captures its own pointer; action buttons remain independent.
                if(pointer==-1 && px<getWidth()*.52f && py>getHeight()*.38f) {
                    pointer=id;stickActive=true;
                    stickCenterX=px;stickCenterY=py;
                    stick(px,py);return true;
                }
                if(paused)paused=false;
                return true;
            }
            return true;
        }
    }
}
