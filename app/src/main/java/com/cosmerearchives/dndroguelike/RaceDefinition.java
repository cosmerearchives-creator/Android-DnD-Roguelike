package com.cosmerearchives.dndroguelike;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable 2014 racial rules, independent of avatar artwork. */
final class RaceDefinition {
    static final RaceDefinition HIGH_ELF=new RaceDefinition("high_elf","elf",new int[]{0,2,0,1,0,0},30,1,60,"medium","intelligence",Arrays.asList("longsword","shortsword","shortbow","longbow"),
        Arrays.asList("perception"),Arrays.asList("common","elvish"),
        Arrays.asList("darkvision","keen_senses","fey_ancestry","trance","elf_weapon_training","high_elf_cantrip","extra_language"));
    final String id, family;
    private final int[] bonuses;
    final int speedFeet;
    final List<String> skills,languages,traits;
    private RaceDefinition(String id,String family,int[] bonuses,int speedFeet,int bonusLanguages,int darkvisionFeet,String size,String cantripAbility,List<String> weapons,List<String> skills,List<String> languages,List<String> traits){
        this.id=id;this.family=family;this.bonuses=bonuses.clone();this.speedFeet=speedFeet;this.bonusLanguages=bonusLanguages;this.darkvisionFeet=darkvisionFeet;this.size=size;this.cantripAbility=cantripAbility;this.weaponProficiencies=Collections.unmodifiableList(new java.util.ArrayList<>(weapons));
        this.skills=Collections.unmodifiableList(new java.util.ArrayList<>(skills));
        this.languages=Collections.unmodifiableList(new java.util.ArrayList<>(languages));
        this.traits=Collections.unmodifiableList(new java.util.ArrayList<>(traits));
    }
    final int bonusLanguages, darkvisionFeet;
    final String size, cantripAbility;
    final List<String> weaponProficiencies;
    static RaceDefinition fromId(String id){return HIGH_ELF;}
    float movementFactor(){return speedFeet/30f;}
    int bonus(int index){return bonuses[index];}
}
