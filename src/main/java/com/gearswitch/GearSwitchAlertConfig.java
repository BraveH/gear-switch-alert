package com.gearswitch;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(GearSwitchAlertConfig.GROUP)
public interface GearSwitchAlertConfig extends Config
{
	String GROUP = "gearswitchalert";
	@ConfigSection(
			name = "Default Tag Colours",
			description = "Colours used to represent each attack style",
			position = 0
	)
	String defaultColourSection = "defaultColourSection";
	@Alpha
	@ConfigItem(
			keyName = "defaultColourMelee",
			name = "Melee Tag",
			description = "Default melee tag colour",
			position = 0,
			section = defaultColourSection
	)
	default Color defaultColourMelee()
	{
		return Color.RED;
	}
	@Alpha
	@ConfigItem(
			keyName = "defaultColourRanged",
			name = "Ranged Tag",
			description = "Default ranged tag colour",
			position = 1,
			section = defaultColourSection
	)
	default Color defaultColourRanged()
	{
		return Color.GREEN;
	}
	@Alpha
	@ConfigItem(
			keyName = "defaultColourMagic",
			name = "Magic Tag",
			description = "Default magic tag colour",
			position = 2,
			section = defaultColourSection
	)
	default Color defaultColourMagic()
	{
		return Color.BLUE;
	}
	@ConfigSection(
			name = "Tag display mode",
			description = "How tags are displayed in the inventory",
			position = 3
	)
	String tagStyleSection = "tagStyleSection";

	@ConfigItem(
			position = 3,
			keyName = "showTagOutline",
			name = "Outline",
			description = "Configures whether or not item tags show be outlined",
			section = tagStyleSection
	)
	default boolean showTagOutline()
	{
		return true;
	}

	@ConfigItem(
			position = 4,
			keyName = "tagUnderline",
			name = "Underline",
			description = "Configures whether or not item tags should be underlined",
			section = tagStyleSection
	)
	default boolean showTagUnderline()
	{
		return false;
	}

	@ConfigItem(
			position = 5,
			keyName = "tagBox",
			name = "Box",
			description = "Configures whether or not item tags should have a box around it",
			section = tagStyleSection
	)
	default boolean showBoxAround()
	{
		return false;
	}

	@ConfigItem(
			position = 6,
			keyName = "tagFill",
			name = "Fill",
			description = "Configures whether or not item tags should be filled",
			section = tagStyleSection
	)
	default boolean showTagFill()
	{
		return false;
	}

	@Range(
			max = 255
	)
	@ConfigItem(
			position = 7,
			keyName = "fillOpacity",
			name = "Fill opacity",
			description = "Configures the opacity of the tag \"Fill\"",
			section = tagStyleSection
	)
	default int fillOpacity()
	{
		return 50;
	}
}
