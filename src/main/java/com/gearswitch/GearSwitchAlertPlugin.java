package com.gearswitch;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

import javax.inject.Inject;

import static com.gearswitch.AttackStyle.*;

@Slf4j
@PluginDescriptor(
		name = "Dynamic Inventory Tags"
)
public class GearSwitchAlertPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GearSwitchAlertConfig config;

	@Inject
	private GearsInventoryTagsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;


	private AttackType attackType;
	private boolean isCurrentTwoHanded;
	private static final String TAG_KEY_PREFIX = "gear_tag_";

	public AttackType getAttackType() {
		return attackType;
	}

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		ItemContainer itemContainer = event.getItemContainer();
		if (itemContainer != client.getItemContainer(InventoryID.EQUIPMENT)) {
			return;
		}

		UpdateEquippedWeaponInfo(true);
	}

	@Provides
    GearSwitchAlertConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(GearSwitchAlertConfig.class);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (event.getVarpId() == VarPlayer.ATTACK_STYLE
				|| event.getVarbitId() == Varbits.EQUIPPED_WEAPON_TYPE
				|| event.getVarbitId() == Varbits.DEFENSIVE_CASTING_MODE) {
			UpdateEquippedWeaponInfo(false);
		}
	}

	private void UpdateEquippedWeaponInfo(boolean forceInvalidate) {
		final int attackStyleIndex = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		final int currentEquippedWeaponTypeVarbit = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
		final int castingMode = client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);

		AttackStyle newAttackStyle = OTHER;
		AttackStyle[] attackStyles = WeaponType.getWeaponType(currentEquippedWeaponTypeVarbit).getAttackStyles();
		if (attackStyleIndex < attackStyles.length) {
			newAttackStyle = attackStyles[attackStyleIndex];
			if (newAttackStyle == null) {
				newAttackStyle = OTHER;
			} else if ((newAttackStyle == CASTING) && (castingMode == 1)) {
				newAttackStyle = DEFENSIVE_CASTING;
			}
		}

		AttackType newAttackType = AttackType.OTHER;
		switch (newAttackStyle) {
			case ACCURATE:
			case DEFENSIVE:
			case AGGRESSIVE:
			case CONTROLLED:
				newAttackType = AttackType.MELEE;
				break;
			case RANGING:
			case LONGRANGE:
				newAttackType = AttackType.RANGE;
				break;
			case CASTING:
			case DEFENSIVE_CASTING:
				newAttackType = AttackType.MAGIC;
				break;
		}

		if(newAttackType != attackType || forceInvalidate) {
			overlay.invalidateCache();
		}
		attackType = newAttackType;

		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);
		if(equipmentContainer != null) {
			Item weapon = equipmentContainer.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
			if(weapon != null) {
				int weaponId = weapon.getId();
				isCurrentTwoHanded = getItemStats(weaponId).getEquipment().isTwoHanded();
			}
		}
	}

	GearTagSettings getTag(int itemId)
	{
		ItemStats weaponStats = getItemStats(itemId);

		if (weaponStats != null) {
			ItemEquipmentStats eStats = weaponStats.getEquipment();
			if (eStats != null) {
				if (eStats.getSlot() != EquipmentInventorySlot.WEAPON.getSlotIdx()) {
					if (eStats.getSlot() == EquipmentInventorySlot.SHIELD.getSlotIdx() && isCurrentTwoHanded) {
						return null;
					}
				}
			}
		}

		String tag = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + itemId);
		if (tag == null || tag.isEmpty())
		{
			return null;
		}

		return gson.fromJson(tag, GearTagSettings.class);
	}

	void setTag(int itemId, GearTagSettings gearTagSettings)
	{
		String json = gson.toJson(gearTagSettings);
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + itemId, json);
		overlay.invalidateCache();
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event)
	{
		if (!client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();
		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();

			if (w != null && (WidgetInfo.TO_GROUP(w.getId()) == WidgetID.INVENTORY_GROUP_ID || WidgetInfo.TO_GROUP(w.getId()) == WidgetID.EQUIPMENT_GROUP_ID)
					&& "Examine".equals(entry.getOption()) && entry.getIdentifier() == 10)
			{
				int itemId = w.getItemId();
				if(itemId == -1 && w.getChildren() != null) {
					for (Widget child : w.getChildren()) {
						itemId = child.getItemId();
						if(itemId != -1)
							break;
					}
				}

				if(itemId == -1)
					return;

				final GearTagSettings gearTagSettings = getTag(itemId);

				final MenuEntry parent = client.createMenuEntry(idx)
						.setOption("Gear Switch Alert Tagging")
						.setTarget(entry.getTarget())
						.setType(MenuAction.RUNELITE_SUBMENU);

				boolean isMeleeEnabled, isRangeEnabled, isMagicEnabled;
				if(gearTagSettings != null) {
					isMeleeEnabled = gearTagSettings.isMeleeGear;
					isRangeEnabled = gearTagSettings.isRangeGear;
					isMagicEnabled = gearTagSettings.isMagicGear;
				} else {
					isMagicEnabled = false;
					isMeleeEnabled = false;
					isRangeEnabled = false;
				}

				for (int i = 0; i < 3; i++) {
					// 0 = melee,
					// 1 = range,
					// 2 = magic


					int finalItemId = itemId;
					if (i == 0) {
						client.createMenuEntry(idx)
								.setOption(ColorUtil.prependColorTag(isMeleeEnabled ? "Unset Melee Gear" : "Set Melee Gear", config.defaultColourMelee()))
								.setType(MenuAction.RUNELITE)
								.setParent(parent)
								.onClick(e ->
								{
									GearTagSettings newGearTagSettings = gearTagSettings;
									if (newGearTagSettings == null)
										newGearTagSettings = new GearTagSettings();

									newGearTagSettings.isMeleeGear = !isMeleeEnabled;
									newGearTagSettings.isRangeGear = isRangeEnabled;
									newGearTagSettings.isMagicGear = isMagicEnabled;
									setTag(finalItemId, newGearTagSettings);
								});
					} else if (i == 1) {
						client.createMenuEntry(idx)
								.setOption(ColorUtil.prependColorTag(isRangeEnabled ? "Unset Range Gear" : "Set Range Gear", config.defaultColourRanged()))
								.setType(MenuAction.RUNELITE)
								.setParent(parent)
								.onClick(e ->
								{
									GearTagSettings newGearTagSettings = gearTagSettings;
									if (newGearTagSettings == null)
										newGearTagSettings = new GearTagSettings();

									newGearTagSettings.isMeleeGear = isMeleeEnabled;
									newGearTagSettings.isRangeGear = !isRangeEnabled;
									newGearTagSettings.isMagicGear = isMagicEnabled;
									setTag(finalItemId, newGearTagSettings);
								});
					} else {
						client.createMenuEntry(idx)
								.setOption(ColorUtil.prependColorTag(isMagicEnabled ? "Unset Magic Gear" : "Set Magic Gear", config.defaultColourMagic()))
								.setType(MenuAction.RUNELITE)
								.setParent(parent)
								.onClick(e ->
								{
									GearTagSettings newGearTagSettings = gearTagSettings;
									if (newGearTagSettings == null)
										newGearTagSettings = new GearTagSettings();

									newGearTagSettings.isMeleeGear = isMeleeEnabled;
									newGearTagSettings.isRangeGear = isRangeEnabled;
									newGearTagSettings.isMagicGear = !isMagicEnabled;
									setTag(finalItemId, newGearTagSettings);
								});
					}
				}
			}
		}
	}

	private ItemStats getItemStats(int itemId) {
		return itemManager.getItemStats(itemId, false);
	}
}
