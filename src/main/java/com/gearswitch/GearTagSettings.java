package com.gearswitch;

import lombok.Data;

@Data
class GearTagSettings
{
    boolean isMeleeGear;
    boolean isRangeGear;
    boolean isMagicGear;

    public int getWeight() {
        return (isMeleeGear ? 100 : 0) + (isRangeGear ? 10 : 0) + (isMagicGear ? 1 : 0);
    }
}