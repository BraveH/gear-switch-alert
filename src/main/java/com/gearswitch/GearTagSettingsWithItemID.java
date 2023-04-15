package com.gearswitch;

import lombok.Data;
import net.runelite.client.game.ItemManager;

@Data
class GearTagSettingsWithItemID
{
    GearTagSettings origTag;
    Integer itemID;
    String name;

    GearTagSettingsWithItemID(GearTagSettings tag, String key, ItemManager itemManager) {
        String[] split = key.split("_");
        this.itemID = split.length > 0 ? Integer.parseInt(split[split.length-1]) : null;

        this.origTag = tag;
        if(itemID != null)
            this.name = itemManager.getItemComposition(itemID).getMembersName();
    }

    public int getWeight() {
        return origTag.getWeight();
    }
}