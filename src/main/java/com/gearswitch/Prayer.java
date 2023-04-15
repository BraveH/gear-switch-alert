package com.gearswitch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.SpriteID;

@Getter
@RequiredArgsConstructor
enum Prayer
{
    PROTECT_FROM_MELEE(SpriteID.SKILL_ATTACK),
    PROTECT_FROM_MISSILES(SpriteID.SKILL_RANGED),
    PROTECT_FROM_MAGIC(SpriteID.SKILL_MAGIC);

    private final int spriteID;
}