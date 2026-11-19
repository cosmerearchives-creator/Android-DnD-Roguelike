package com.cosmerearchives.dndroguelike;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Saved creation choices, independently of the temporary arena units. */
final class PlayerCharacter {
    static final String FILE="high_elf_fighter.json";
    static final String[] ABILITIES={"Strength","Dexterity","Constitution","Intelligence","Wisdom","Charisma"};
    static final String[] BACKGROUNDS={"Acolyte","Wilderness Pathfinder","Caravan Guard","Relic Seeker","Guild Apprentice"};
    static final String[] STYLES={"Archery","Defense","Dueling","Great Weapon Fighting","Protection","Two-Weapon Fighting"};
    static final String[] CANTRIPS={"Fire Bolt","Ray of Frost","Shocking Grasp"};
    static final String[] FIGHTER_SKILLS={"Acrobatics","Animal Handling","Athletics","History","Insight","Intimidation","Perception","Survival"};
    static final String[] LANGUAGES={"Dwarvish","Giant","Gnomish","Goblin","Halfling","Orc","Abyssal","Celestial","Draconic","Deep Speech","Infernal","Primordial","Sylvan","Undercommon"};
    String name="";String gender="Female",background=BACKGROUNDS[0],style=STYLES[0],cantrip=CANTRIPS[0];
    String skillOne="Athletics",skillTwo="Survival",languageOne="Dwarvish",languageTwo="Sylvan",languageThree="Draconic";
    String toolChoice="cartographer_tools",gamingChoice="dice_set",artisanChoice="smith_tools";
    String armorChoice="chain_mail",martialOne="longsword",martialTwo="shield",rangedChoice="light_crossbow",packChoice="explorer";
    String skin="Fair",hair="Silver",clothingColor="Blue";
    int age=120,heightInches=65;
    String alignment="Neutral Good";
    int[] base={15,14,13,12,10,8};
    final List<int[]> rolls=new ArrayList<>();
    boolean completed;
    int experience=0,level=1;
    int maxHp(){return 10+modifier(score(2));}
    int score(int index){return base[index]+(index==1?2:index==3?1:0);}
    static int modifier(int score){return (int)Math.floor((score-10)/2.0);}
    int proficiency(){return 2+(level-1)/4;}
    int attackBonus(){return modifier(score(0))+proficiency();}
    int passivePerception(){return 10+modifier(score(4))+proficiency();}
    int maxCarry(){return score(0)*15;}
    String[] backgroundSkills(){
        switch(background){
            case "Acolyte":return new String[]{"Insight","Religion"};
            case "Wilderness Pathfinder":return new String[]{"Survival","Nature"};
            case "Caravan Guard":return new String[]{"Athletics","Perception"};
            case "Relic Seeker":return new String[]{"Investigation","History"};
            default:return new String[]{"Insight","Persuasion"};
        }
    }
    static int[] rollScores(Random rng){
        int[] result=new int[6];
        for(int i=0;i<6;i++){
            int sum=0,lowest=7;
            for(int j=0;j<4;j++){int roll=rng.nextInt(6)+1;sum+=roll;lowest=Math.min(lowest,roll);}
            result[i]=sum-lowest;
        }
        return result;
    }
    boolean valid(){
        if(name.trim().isEmpty()||skillOne.equals(skillTwo))return false;
        int[] sorted=base.clone();java.util.Arrays.sort(sorted);
        if(sorted[0]<3||sorted[5]>18)return false;
        return true;
    }
    Equipment.Bag loadout(){
        Equipment.Bag bag=new Equipment.Bag();
        bag.strength=score(0);bag.dexterity=score(1);bag.constitution=score(2);
        if(armorChoice.equals("chain_mail"))bag.add("chain_mail",1);
        else{bag.add("leather_armor",1);bag.add("longbow",1);}
        bag.add(martialOne,1);
        if(martialTwo.equals("shield"))bag.add("shield",1);
        else bag.add(martialTwo,1);
        if(rangedChoice.equals("light_crossbow"))bag.add("light_crossbow",1);
        else bag.add("handaxe",2);
        if(packChoice.equals("dungeon"))Equipment.dungeonPack(bag);else Equipment.explorerPack(bag);
        if(background.equals("Acolyte")){
            bag.add("holy_symbol",1);bag.add("prayer_book",1);bag.add("incense",5);bag.gold=15;
        }else if(background.equals("Wilderness Pathfinder")){
            bag.add("walking_staff",1);bag.add("hunting_trap",1);bag.add("wilderness_map",1);
            bag.add(toolChoice,1);bag.gold=10;
        }else if(background.equals("Caravan Guard")){
            bag.add(gamingChoice,1);bag.add("travel_documents",1);bag.add("employer_insignia",1);bag.gold=10;
        }else if(background.equals("Relic Seeker")){
            bag.add("archaeological_kit",1);bag.add("sketchbook",1);
            bag.add("charcoal",1);bag.add("artifact_fragment",1);bag.add(toolChoice,1);bag.gold=10;
        }else{
            bag.add(artisanChoice,1);bag.add("guild_id",1);bag.gold=10;
        }
        if(armorChoice.equals("chain_mail"))bag.equip("chain_mail",Equipment.Slot.BODY);
        else bag.equip("leather_armor",Equipment.Slot.BODY);
        bag.equip(martialOne,Equipment.Slot.MAIN_HAND);
        if(martialTwo.equals("shield"))bag.equip("shield",Equipment.Slot.OFF_HAND);
        else if(Equipment.get(martialOne)!=null&&!Equipment.get(martialOne).twoHanded)
            bag.equip(martialTwo,Equipment.Slot.OFF_HAND);
        return bag;
    }
    JSONObject json(){
        JSONObject o=new JSONObject();
        try{
            o.put("name",name);o.put("gender",gender);o.put("background",background);
            o.put("style",style);o.put("cantrip",cantrip);o.put("skillOne",skillOne);o.put("skillTwo",skillTwo);
            o.put("languageOne",languageOne);o.put("languageTwo",languageTwo);o.put("languageThree",languageThree);
            o.put("toolChoice",toolChoice);o.put("gamingChoice",gamingChoice);o.put("artisanChoice",artisanChoice);
            o.put("armorChoice",armorChoice);o.put("martialOne",martialOne);o.put("martialTwo",martialTwo);
            o.put("rangedChoice",rangedChoice);o.put("packChoice",packChoice);
            o.put("skin",skin);o.put("hair",hair);o.put("clothingColor",clothingColor);
            o.put("age",age);o.put("heightInches",heightInches);o.put("alignment",alignment);
            o.put("completed",completed);o.put("experience",experience);o.put("level",level);
            JSONArray scores=new JSONArray();for(int x:base)scores.put(x);o.put("base",scores);
            JSONArray history=new JSONArray();for(int[] set:rolls){JSONArray a=new JSONArray();for(int n:set)a.put(n);history.put(a);}o.put("rolls",history);
        }catch(Exception e){throw new IllegalStateException(e);}
        return o;
    }
    static PlayerCharacter fromJson(JSONObject o){
        PlayerCharacter p=new PlayerCharacter();
        p.name=o.optString("name",p.name);p.gender=o.optString("gender",p.gender);
        p.background=o.optString("background",p.background);p.style=o.optString("style",p.style);
        p.cantrip=o.optString("cantrip",p.cantrip);p.skillOne=o.optString("skillOne",p.skillOne);
        p.skillTwo=o.optString("skillTwo",p.skillTwo);p.languageOne=o.optString("languageOne",p.languageOne);
        p.languageTwo=o.optString("languageTwo",p.languageTwo);p.languageThree=o.optString("languageThree",p.languageThree);
        p.toolChoice=o.optString("toolChoice",p.toolChoice);p.gamingChoice=o.optString("gamingChoice",p.gamingChoice);
        p.artisanChoice=o.optString("artisanChoice",p.artisanChoice);
        p.armorChoice=o.optString("armorChoice",p.armorChoice);
        p.martialOne=o.optString("martialOne",p.martialOne);p.martialTwo=o.optString("martialTwo",p.martialTwo);
        p.rangedChoice=o.optString("rangedChoice",p.rangedChoice);p.packChoice=o.optString("packChoice",p.packChoice);
        p.skin=o.optString("skin",p.skin);p.hair=o.optString("hair",p.hair);
        p.clothingColor=o.optString("clothingColor",p.clothingColor);
        p.age=o.optInt("age",p.age);p.heightInches=o.optInt("heightInches",p.heightInches);
        p.alignment=o.optString("alignment",p.alignment);
        p.completed=o.optBoolean("completed",false);p.experience=o.optInt("experience",0);p.level=o.optInt("level",1);
        JSONArray a=o.optJSONArray("base");
        if(a!=null&&a.length()==6)for(int i=0;i<6;i++)p.base[i]=a.optInt(i,p.base[i]);
        JSONArray history=o.optJSONArray("rolls");
        if(history!=null)for(int j=0;j<history.length();j++){
            JSONArray set=history.optJSONArray(j);if(set==null||set.length()!=6)continue;
            int[] values=new int[6];for(int i=0;i<6;i++)values[i]=set.optInt(i);p.rolls.add(values);
        }
        return p;
    }
    void save(Context context){
        try(java.io.FileOutputStream out=context.openFileOutput(FILE,Context.MODE_PRIVATE)){
            out.write(json().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(Exception e){throw new IllegalStateException("Character save failed",e);}
    }
    static PlayerCharacter load(Context context){
        try(java.io.FileInputStream input=context.openFileInput(FILE)){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] b=new byte[4096];int n;
            while((n=input.read(b))!=-1)out.write(b,0,n);
            return fromJson(new JSONObject(out.toString("UTF-8")));
        }catch(Exception ignored){return null;}
    }
}
