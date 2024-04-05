package com.gearswitch;

import lombok.Getter;
import net.runelite.api.Skill;

enum AttackStyle
{
    ACCURATE("Accurate", Skill.ATTACK),
    AGGRESSIVE("Aggressive", Skill.STRENGTH),
    DEFENSIVE("Defensive", Skill.DEFENCE),
    CONTROLLED("Controlled", Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
    RANGING("Ranging", Skill.RANGED),
    LONGRANGE("Longrange", Skill.RANGED, Skill.DEFENCE),
    CASTING("Casting", Skill.MAGIC),
    DEFENSIVE_CASTING("Defensive Casting", Skill.MAGIC, Skill.DEFENCE),
    LONGRANGE_CASTING("Longrange Casting", Skill.MAGIC, Skill.DEFENCE),
    OTHER("Other");

    @Getter
    private final String name;
    @Getter
    private final Skill[] skills;

    AttackStyle(String name, Skill... skills)
    {
        this.name = name;
        this.skills = skills;
    }
}