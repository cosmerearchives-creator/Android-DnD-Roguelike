package com.cosmerearchives.dndroguelike;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Saved creation choices for a Wildlands character. */
final class PlayerCharacter {
    private static final String FILE="high_elf_fighter.json";
    private static final String DRAFT_FILE="high_elf_draft.json";
    static final String[] STARTING_CLASSES={"Fighter"};
    static final String[] ABILITIES={"Strength","Dexterity","Constitution","Intelligence","Wisdom","Charisma"};
    static final String[] BACKGROUNDS={"Acolyte","Outlander","Soldier","Archaeologist","Guild Artisan"};
    static final String[] STYLES={"Archery","Defense","Dueling","Great Weapon Fighting","Protection","Two-Weapon Fighting"};
    static final String[] CANTRIPS={"Fire Bolt","Ray of Frost","Shocking Grasp"};
    static final String[] FIGHTER_SKILLS={"Acrobatics","Animal Handling","Athletics","History","Insight","Intimidation","Perception","Survival"};
    static final String[] LANGUAGES={"Common","Elvish","Dwarvish","Giant","Gnomish","Goblin","Halfling","Orc","Abyssal","Celestial","Draconic","Deep Speech","Infernal","Primordial","Sylvan","Undercommon"};
    RaceDefinition race=RaceDefinition.HIGH_ELF;
    final ElfAppearance appearance=ElfAppearance.PETITE;
    PetiteAppearance petite=PetiteAppearance.DEFAULT;
    String startingClass=STARTING_CLASSES[0];
    String name="";String background=BACKGROUNDS[0],style=STYLES[0],cantrip=CANTRIPS[0];
    String skillOne="Athletics",skillTwo="Survival",languageOne="Dwarvish",languageTwo="Sylvan",languageThree="Draconic";
    String toolChoice="cartographer_tools",gamingChoice="dice_set",artisanChoice="smith_tools";
    String armorChoice="chain_mail",martialOne="longsword",martialTwo="shield",rangedChoice="light_crossbow",packChoice="explorer";
    int age=120;
    String alignment="Neutral Good";
    int[] base={15,14,13,12,10,8};
    final List<int[]> rolls=new ArrayList<>();
    boolean completed;
    int experience=0,level=1;
    int maxHp(){return 10+modifier(score(2));}
    int score(int index){return base[index]+race.bonus(index);}
    static int modifier(int score){return (int)Math.floor((score-10)/2.0);}
    int proficiency(){return 2+(level-1)/4;}
    int attackBonus(){return modifier(score(0))+proficiency();}
    int passivePerception(){return 10+skillBonus("Perception",4);}
    int maxCarry(){return score(0)*15;}
    String[] backgroundSkills(){
        switch(background){
            case "Acolyte":return new String[]{"Insight","Religion"};
            case "Outlander":return new String[]{"Athletics","Survival"};
            case "Soldier":return new String[]{"Athletics","Intimidation"};
            case "Archaeologist":return new String[]{"History","Survival"};
            default:return new String[]{"Insight","Persuasion"};
        }
    }
    String backgroundFeature(){
        switch(background){
            case "Acolyte":return "Shelter of the Faithful";
            case "Outlander":return "Wanderer";
            case "Soldier":return "Military Rank";
            case "Archaeologist":return "Historical Knowledge";
            default:return "Guild Membership";
        }
    }
    String backgroundFeatureTag(){
        return backgroundFeature().toLowerCase(java.util.Locale.ROOT).replace(' ','_');
    }
    java.util.Set<String> backgroundFeatureTags(){
        return java.util.Collections.singleton(backgroundFeatureTag());
    }
    boolean hasBackgroundFeature(String tag){
        return backgroundFeatureTag().equals(tag.trim().toLowerCase(java.util.Locale.ROOT).replace(' ','_'));
    }
    int backgroundLanguageCount(){
        return background.equals("Acolyte")?2:background.equals("Soldier")?0:1;
    }
    String[] backgroundTools(){
        switch(background){
            case "Acolyte":return new String[0];
            case "Outlander":return new String[]{instrumentChoice};
            case "Soldier":return new String[]{gamingChoice,"land_vehicles"};
            case "Archaeologist":return new String[]{toolChoice.equals("navigator_tools")?toolChoice:"cartographer_tools"};
            default:return new String[]{artisanChoice};
        }
    }
    String instrumentChoice="Lute", prayerChoice="prayer_book";
    private static String skillTag(String skill){
        return skill.trim().toLowerCase(java.util.Locale.ROOT).replace(' ','_');
    }
    static final String[] ALL_SKILLS={"Acrobatics","Animal Handling","Arcana","Athletics","Deception","History","Insight","Intimidation","Investigation","Medicine","Nature","Perception","Performance","Persuasion","Religion","Sleight of Hand","Stealth","Survival"};
    String replacementOne="",replacementTwo="";
    java.util.Set<String> baseSkillTags(){
        java.util.Set<String> tags=new java.util.TreeSet<>();
        tags.addAll(race.skills);
        tags.add(skillTag(skillOne));tags.add(skillTag(skillTwo));
        for(String skill:backgroundSkills())tags.add(skillTag(skill));
        return tags;
    }
    int replacementCount(){return race.skills.size()+2+backgroundSkills().length-baseSkillTags().size();}
    final List<String> chosenLanguages=new ArrayList<>(java.util.Arrays.asList("Dwarvish","Sylvan","Draconic"));
    final List<String> chosenReplacements=new ArrayList<>();
    int languageCount(){return race.bonusLanguages+backgroundLanguageCount();}
    String languageAt(int slot){return chosenLanguages.get(slot);}
    void setLanguage(int slot,String value){chosenLanguages.set(slot,value);}
    String replacementAt(int slot){return chosenReplacements.get(slot);}
    void setReplacement(int slot,String value){chosenReplacements.set(slot,value);}
    private void normalizeChoices(List<String> choices,int count,String[] options,java.util.Collection<String> granted){
        java.util.Set<String> used=new java.util.HashSet<>(granted);
        while(choices.size()<count)choices.add("");
        for(int i=0;i<count;i++){
            String value=choices.get(i);
            if(!java.util.Arrays.asList(options).contains(value)||used.contains(skillTag(value))){
                value="";
                for(String option:options)if(!used.contains(skillTag(option))){value=option;break;}
                choices.set(i,value);
            }
            used.add(skillTag(value));
        }
    }
    void normalizeProficiencies(){
        normalizeChoices(chosenReplacements,replacementCount(),ALL_SKILLS,baseSkillTags());
        normalizeChoices(chosenLanguages,languageCount(),LANGUAGES,race.languages);
    }
    private String[] availableChoices(List<String> choices,int count,int slot,String[] options,java.util.Collection<String> granted){
        java.util.Set<String> used=new java.util.HashSet<>(granted);
        for(int i=0;i<count;i++)if(i!=slot)used.add(skillTag(choices.get(i)));
        List<String> result=new ArrayList<>();
        for(String option:options)if(!used.contains(skillTag(option)))result.add(option);
        return result.toArray(new String[0]);
    }
    String[] replacementOptions(int slot){normalizeProficiencies();return availableChoices(chosenReplacements,replacementCount(),slot,ALL_SKILLS,baseSkillTags());}
    String[] languageOptions(int slot){normalizeProficiencies();return availableChoices(chosenLanguages,languageCount(),slot,LANGUAGES,race.languages);}
    java.util.Set<String> languageTags(){
        normalizeProficiencies();
        java.util.Set<String> tags=new java.util.TreeSet<>(race.languages);
        for(int i=0;i<languageCount();i++)tags.add(skillTag(chosenLanguages.get(i)));
        return java.util.Collections.unmodifiableSet(tags);
    }
    boolean knowsLanguage(String language){return languageTags().contains(skillTag(language));}
    private static String toolTag(String tool){
        String tag=skillTag(tool);
        return tag.endsWith("_artisan")?tag.substring(0,tag.length()-8):tag;
    }
    java.util.Set<String> toolTags(){
        java.util.Set<String> tags=new java.util.TreeSet<>();
        for(String tool:backgroundTools())tags.add(toolTag(tool));
        return java.util.Collections.unmodifiableSet(tags);
    }
    boolean toolProficient(String tool){return toolTags().contains(toolTag(tool));}
    java.util.Set<String> skillTags(){
        normalizeProficiencies();
        java.util.Set<String> tags=baseSkillTags();
        for(int i=0;i<replacementCount();i++)tags.add(skillTag(chosenReplacements.get(i)));
        return java.util.Collections.unmodifiableSet(tags);
    }
    boolean skillProficient(String skill){
        return skillTags().contains(skillTag(skill));
    }
    int skillBonus(String skill,int ability){
        return modifier(score(ability))+(skillProficient(skill)?proficiency():0);
    }
    int skillBonus(String skill){
        int ability;
        switch(skillTag(skill)){
            case "athletics":ability=0;break;
            case "acrobatics":case "sleight_of_hand":case "stealth":ability=1;break;
            case "arcana":case "history":case "investigation":case "nature":case "religion":ability=3;break;
            case "animal_handling":case "insight":case "medicine":case "perception":case "survival":ability=4;break;
            case "deception":case "intimidation":case "performance":case "persuasion":ability=5;break;
            default:throw new IllegalArgumentException("Unknown skill: "+skill);
        }
        return skillBonus(skill,ability);
    }
    int rollSkillCheck(String skill,Random rng){
        return rng.nextInt(20)+1+skillBonus(skill);
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
        switch(background){
            case "Acolyte":
                bag.add("holy_symbol",1);bag.add(prayerChoice.equals("prayer_wheel")?"prayer_wheel":"prayer_book",1);
                bag.add("incense",5);bag.add("vestments",1);bag.add("common_clothes",1);bag.gold=15;break;
            case "Outlander":
                bag.add("quarterstaff",1);bag.add("hunting_trap",1);bag.add("animal_trophy",1);
                bag.add("travelers_clothes",1);bag.gold=10;break;
            case "Soldier":
                bag.add(gamingChoice,1);bag.add("rank_insignia",1);bag.add("enemy_trophy",1);
                bag.add("common_clothes",1);bag.gold=10;break;
            case "Archaeologist":
                bag.add("map_case",1);bag.add("bullseye_lantern",1);bag.add("miners_pick",1);
                bag.add("travelers_clothes",1);bag.add("shovel",1);bag.add("two_person_tent",1);
                bag.add("excavation_trinket",1);bag.gold=25;break;
            default:
                bag.add(artisanChoice,1);bag.add("guild_letter",1);bag.add("travelers_clothes",1);bag.gold=15;
        }
        bag.add("pouch",1);
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
            o.put("race",race.id);o.put("racialTraitTags",new JSONArray(race.traits));o.put("name",name);o.put("appearance",appearance.id);
            o.put("petiteEars",petite.ears);o.put("petiteHair",petite.hair);o.put("petiteSkin",petite.skin);
            o.put("class",startingClass);o.put("background",background);
            o.put("backgroundFeatureTags",new JSONArray(backgroundFeatureTags()));
            normalizeProficiencies();
            o.put("skillTags",new JSONArray(skillTags()));
            o.put("toolTags",new JSONArray(toolTags()));
            o.put("languageTags",new JSONArray(languageTags()));
            o.put("replacementChoices",new JSONArray(chosenReplacements));
            o.put("style",style);o.put("cantrip",cantrip);o.put("skillOne",skillOne);o.put("skillTwo",skillTwo);
            o.put("languageChoices",new JSONArray(chosenLanguages));
            o.put("instrumentChoice",instrumentChoice);o.put("prayerChoice",prayerChoice);o.put("toolChoice",toolChoice);o.put("gamingChoice",gamingChoice);o.put("artisanChoice",artisanChoice);
            o.put("armorChoice",armorChoice);o.put("martialOne",martialOne);o.put("martialTwo",martialTwo);
            o.put("rangedChoice",rangedChoice);o.put("packChoice",packChoice);
            o.put("age",age);o.put("alignment",alignment);
            o.put("completed",completed);o.put("experience",experience);o.put("level",level);
            JSONArray scores=new JSONArray();for(int x:base)scores.put(x);o.put("base",scores);
            JSONArray history=new JSONArray();for(int[] set:rolls){JSONArray a=new JSONArray();for(int n:set)a.put(n);history.put(a);}o.put("rolls",history);
        }catch(Exception e){throw new IllegalStateException(e);}
        return o;
    }
    static PlayerCharacter fromJson(JSONObject o){
        PlayerCharacter p=new PlayerCharacter();
        p.race=RaceDefinition.fromId(o.optString("race","high_elf"));
        p.name=o.optString("name",p.name);
        p.petite=new PetiteAppearance(o.optString("petiteEars"),o.optString("petiteHair"),o.optString("petiteSkin"));
        String savedClass=o.optString("class");
        for(String option:STARTING_CLASSES){
            if(option.equals(savedClass))p.startingClass=option;
        }
        p.background=o.optString("background",p.background);
        switch(p.background){case "Wilderness Pathfinder":p.background="Outlander";break;case "Caravan Guard":p.background="Soldier";break;case "Relic Seeker":p.background="Archaeologist";break;case "Guild Apprentice":p.background="Guild Artisan";break;}
        p.instrumentChoice=o.optString("instrumentChoice",p.instrumentChoice);p.prayerChoice=o.optString("prayerChoice",p.prayerChoice);p.style=o.optString("style",p.style);
        p.cantrip=o.optString("cantrip",p.cantrip);p.skillOne=o.optString("skillOne",p.skillOne);
        p.skillTwo=o.optString("skillTwo",p.skillTwo);p.languageOne=o.optString("languageOne",p.languageOne);
        p.languageTwo=o.optString("languageTwo",p.languageTwo);p.languageThree=o.optString("languageThree",p.languageThree);
        p.toolChoice=o.optString("toolChoice",p.toolChoice);p.gamingChoice=o.optString("gamingChoice",p.gamingChoice);
        p.artisanChoice=o.optString("artisanChoice",p.artisanChoice);
        p.armorChoice=o.optString("armorChoice",p.armorChoice);
        p.martialOne=o.optString("martialOne",p.martialOne);p.martialTwo=o.optString("martialTwo",p.martialTwo);
        p.rangedChoice=o.optString("rangedChoice",p.rangedChoice);p.packChoice=o.optString("packChoice",p.packChoice);
        p.age=o.optInt("age",p.age);
        p.alignment=o.optString("alignment",p.alignment);
        p.completed=o.optBoolean("completed",false);p.experience=o.optInt("experience",0);p.level=o.optInt("level",1);
        JSONArray a=o.optJSONArray("base");
        if(a!=null&&a.length()==6)for(int i=0;i<6;i++)p.base[i]=a.optInt(i,p.base[i]);
        JSONArray history=o.optJSONArray("rolls");
        if(history!=null)for(int j=0;j<history.length();j++){
            JSONArray set=history.optJSONArray(j);if(set==null||set.length()!=6)continue;
            int[] values=new int[6];for(int i=0;i<6;i++)values[i]=set.optInt(i);p.rolls.add(values);
        }
        p.replacementOne=o.optString("replacementOne","");p.replacementTwo=o.optString("replacementTwo","");
        p.chosenLanguages.clear();
        p.chosenLanguages.addAll(java.util.Arrays.asList(p.languageOne,p.languageTwo,p.languageThree));
        p.chosenReplacements.addAll(java.util.Arrays.asList(p.replacementOne,p.replacementTwo));
        readChoices(o.optJSONArray("languageChoices"),p.chosenLanguages);
        readChoices(o.optJSONArray("replacementChoices"),p.chosenReplacements);
        p.normalizeProficiencies();
        return p;
    }
    private static void readChoices(JSONArray array,List<String> choices){
        if(array==null)return;
        choices.clear();for(int i=0;i<array.length();i++)choices.add(array.optString(i,""));
    }
    void save(Context context){
        byte[] data=json().toString().getBytes(StandardCharsets.UTF_8);
        AtomicFile target=file(context,completed?FILE:DRAFT_FILE);
        FileOutputStream out=null;
        try{
            out=target.startWrite();
            out.write(data);
            target.finishWrite(out);
        }catch(IOException e){
            target.failWrite(out);
            throw new IllegalStateException("Character save failed",e);
        }
        if(completed)file(context,DRAFT_FILE).delete();
    }

    static PlayerCharacter loadCompleted(Context context){
        PlayerCharacter saved=read(file(context,FILE));
        return saved!=null&&saved.completed?saved:null;
    }

    static PlayerCharacter loadDraft(Context context){
        PlayerCharacter draft=read(file(context,DRAFT_FILE));
        if(draft!=null)return draft;
        PlayerCharacter legacy=read(file(context,FILE));
        return legacy!=null&&!legacy.completed?legacy:null;
    }

    private static AtomicFile file(Context context,String filename){
        return new AtomicFile(new File(context.getFilesDir(),filename));
    }

    private static PlayerCharacter read(AtomicFile file){
        try{
            return fromJson(new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8)));
        }catch(IOException|org.json.JSONException ignored){return null;}
    }
}
