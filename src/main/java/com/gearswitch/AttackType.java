package com.gearswitch;

import lombok.Getter;
import net.runelite.api.Skill;

enum AttackType {
    MELEE("MELEE", Skill.ATTACK),
    MAGIC("MAGIC", Skill.MAGIC),
    RANGE("RANGE", Skill.RANGED),
    OTHER("Other");

    @Getter
    private final String name;
    @Getter
    private final Skill[] skills;

    AttackType(String name, Skill... skills)
    {
        this.name = name;
        this.skills = skills;
    }
}