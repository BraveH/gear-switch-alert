package com.gearswitch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
			name = "Tag Display Mode",
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


	@ConfigSection(
			name = "Panel Settings",
			description = "Settings specific to the side panel tool.",
			position = 8
	)
	String panelSection = "panelSection";

	@ConfigItem(
			keyName = "hidePlugin",
			name = "Hide on toolbar",
			description = "When checked, the plugin will not appear in the tool bar",
			position = 8,
			section = panelSection
	)
	default boolean hidePlugin() {
		return false;
	}

	@Getter
	@RequiredArgsConstructor
	enum SortMethod
	{
		NONE("None"),
		EMPTY_FIRST("Place non-tagged items first"),
		ALL_FIRST("Place items tagged with more styles first");

		final String name;
	}
	@ConfigItem(
			position = 9,
			keyName = "tagSortMethod",
			name = "Tags Sort Method",
			description = "Sorting method used on tagged item boxes in the panel. <br/>None = no sorting<br/> Empty First = Place non-tagged items first<br/> All First = Place items tagged with more styles first",
			section = panelSection
	)
	default SortMethod sortItems() { return SortMethod.ALL_FIRST; }


	@ConfigSection(
			name = "General Settings",
			description = "Settings plugin settings.",
			position = 10
	)
	String generalSection = "generalSection";

	@ConfigItem(
			keyName = "allowTaggingUnequipables",
			name = "Allow tagging unequipable items",
			description = "Should unequipable items be allowed to be tagged as melee/range/magic gear?",
			position = 10,
			section = generalSection
	)
	default boolean allowTaggingUnequipables() {
		return false;
	}

	@ConfigItem(
			keyName = "secondsBeforeTagging",
			name = "Tagging Delay (ms)",
			description = "Delay (in milliseconds) before tagging if the gear is still not switched?",
			position = 11,
			section = generalSection
	)
	default int millisecondsBeforeTagging() {
		return 0;
	}
}
