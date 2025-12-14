package com.hugoof06.pokedex.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

    private final int hp;
    private final int attack;
    private final int defense;
    private final int spAttack;
    private final int spDefense;
    private final int speed;

    @JsonCreator
    public Stats(
            @JsonProperty("hp") int hp,
            @JsonProperty("attack") int attack,
            @JsonProperty("defense") int defense,
            @JsonProperty("spAttack") int spAttack,
            @JsonProperty("spDefense") int spDefense,
            @JsonProperty("speed") int speed
    ) {
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.spAttack = spAttack;
        this.spDefense = spDefense;
        this.speed = speed;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpAttack() {
        return spAttack;
    }

    public int getSpDefense() {
        return spDefense;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public String toString() {
        return String.format(
            "HP:%d ATK:%d DEF:%d SpA:%d SpD:%d SPE:%d",
            hp, attack, defense, spAttack, spDefense, speed
        );
    }
}

