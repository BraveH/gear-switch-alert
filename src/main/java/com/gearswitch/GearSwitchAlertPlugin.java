package com.gearswitch;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gearswitch.AttackStyle.*;

@Slf4j
@PluginDescriptor(
		name = "Dynamic Inventory Tags"
)
public class GearSwitchAlertPlugin extends Plugin
{
	public static Map<String, String> profiles = new HashMap<>();

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GearSwitchAlertConfig config;

	@Inject
	public GearsInventoryTagsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ClientThread clientThread;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	@Getter
	private ChatboxItemSearch itemSearch;

	private AttackType attackType;
	private boolean isCurrentTwoHanded;
	private static final String TAG_KEY_PREFIX = "gear_tag_";
	private static final String PROFILES_PREFIX = "gear_profiles";
	private static final String SELECTED_PROFILE_PREFIX = "gear_selected_profile";
	private static final String CUSTOM_ID = "customId";
	private static final String ID_PREFIX = "ID";

	private GearSwitchAlertPanel panel;
	private NavigationButton navButton;

	public AttackType getAttackType() {
		return attackType;
	}

	private final Map<Prayer, BufferedImage> prayerSprites = new HashMap<>();

	@Override
	protected void startUp() throws Exception {
		clientThread.invoke(this::loadSprites);
		loadProfiles();
		overlayManager.add(overlay);

		panel = new GearSwitchAlertPanel(clientThread, this, itemManager, config);
		final BufferedImage icon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "dynamic_tags_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Dynamic Inventory Tags")
				.icon(icon)
				.panel(panel)
				.build();

		if (!config.hidePlugin()) {
			clientToolbar.addNavigation(navButton);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);

		if (!config.hidePlugin()) {
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged event) {
		if (event.getGroup().equals(GearSwitchAlertConfig.GROUP)) {
			if (event.getKey().equals("hidePlugin")) {
				if (config.hidePlugin()) {
					clientToolbar.removeNavigation(navButton);
				} else {
					clientToolbar.addNavigation(navButton);
				}
			}
		}
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

	private AttackStyle[] getWeaponTypeStyles(int weaponType)
	{
		// from script4525
		int weaponStyleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
		int[] weaponStyleStructs = client.getEnum(weaponStyleEnum).getIntVals();

		AttackStyle[] styles = new AttackStyle[weaponStyleStructs.length];
		int i = 0;
		for (int style : weaponStyleStructs)
		{
			StructComposition attackStyleStruct = client.getStructComposition(style);
			String attackStyleName = attackStyleStruct.getStringValue(ParamID.ATTACK_STYLE_NAME);

			AttackStyle attackStyle = AttackStyle.valueOf(attackStyleName.toUpperCase());
			if (attackStyle == OTHER)
			{
				// "Other" is used for no style
				++i;
				continue;
			}

			// "Defensive" is used for Defensive and also Defensive casting
			if (i == 5 && attackStyle == DEFENSIVE)
			{
				attackStyle = DEFENSIVE_CASTING;
			}

			styles[i++] = attackStyle;
		}
		return styles;
	}

	private void UpdateEquippedWeaponInfo(boolean forceInvalidate) {
		final int attackStyleIndex = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		final int currentEquippedWeaponTypeVarbit = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
		final int castingMode = client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);

		AttackStyle newAttackStyle = OTHER;
		AttackStyle[] attackStyles = getWeaponTypeStyles(currentEquippedWeaponTypeVarbit);
		if (attackStyleIndex < attackStyles.length) {
			newAttackStyle = attackStyles[attackStyleIndex];
			if (newAttackStyle == null) {
				newAttackStyle = OTHER;
			} else if ((newAttackStyle == DEFENSIVE) && (attackStyles[0] == CASTING)) {
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

		overlay.resetDelayTimer();
	}

	GearTagSettings getTag(int itemId) {
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

		String profile = loadSelectedProfile();
		String profilePrefix = profile.equals("0") ? "" : profile + "_";
		String tag = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + profilePrefix + itemId);
		if (tag == null || tag.isEmpty()) {
			return null;
		}

		return gson.fromJson(tag, GearTagSettings.class);
	}

	ArrayList<GearTagSettingsWithItemID> getTagsForProfile(String profileID) {
		ArrayList<GearTagSettingsWithItemID> result = new ArrayList<>();
		String profilePrefix = profileID.equals("0") ? "" : profileID + "_";

		List<String> keys = configManager.getConfigurationKeys(GearSwitchAlertConfig.GROUP + "." + TAG_KEY_PREFIX + profilePrefix);

		if (keys == null || keys.isEmpty()) {
			return result;
		}

		if(profileID.equals("0")) {
			for (String key : keys) {
				String[] split = key.split(TAG_KEY_PREFIX + profilePrefix);
				boolean isNotDefaultProfileKey = split[1].startsWith(ID_PREFIX);
				if(!isNotDefaultProfileKey) {
					String tag = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + profilePrefix + split[1]);
					if(tag != null && !tag.isEmpty()) {
						GearTagSettings gearTag = gson.fromJson(tag, GearTagSettings.class);
//						if(gearTag.isMeleeGear || gearTag.isRangeGear || gearTag.isMagicGear) {
						GearTagSettingsWithItemID tagWithID = new GearTagSettingsWithItemID(gearTag, key, itemManager);
						result.add(tagWithID);
//						}
					}
				}
			}
		} else {
			for (String key : keys) {
				String[] split = key.split(TAG_KEY_PREFIX + profilePrefix);
				String tag = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + profilePrefix + split[1]);
				if(tag != null && !tag.isEmpty()) {
					GearTagSettings gearTag = gson.fromJson(tag, GearTagSettings.class);
//					if(gearTag.isMeleeGear || gearTag.isRangeGear || gearTag.isMagicGear) {
					GearTagSettingsWithItemID tagWithID = new GearTagSettingsWithItemID(gearTag, key, itemManager);
					result.add(tagWithID);
//					}
				}
			}
		}

		GearSwitchAlertConfig.SortMethod sortMethod = config.sortItems();
		if(sortMethod != GearSwitchAlertConfig.SortMethod.NONE) {
			result.sort((tag1, tag2) -> sortMethod == GearSwitchAlertConfig.SortMethod.ALL_FIRST ?
					tag2.getWeight() - tag1.getWeight() :
					tag1.getWeight() - tag2.getWeight() );
		}

		return result;
	}

	void setTag(int itemId, GearTagSettings gearTagSettings) {
		setTag(itemId, gearTagSettings, loadSelectedProfile());
	}

	void setTag(int itemId, GearTagSettings gearTagSettings, String profileUUID) {
		String json = gson.toJson(gearTagSettings);
		String profilePrefix = profileUUID.equals("0") ? "" : profileUUID + "_";
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + profilePrefix + itemId, json);
		overlay.invalidateCache();
		ProfilePanel tile = panel.getEnabledProfileTile();
		if(tile != null)
			clientThread.invokeLater(tile::rebuild);
	}

	void unsetTag(int itemId, String profileUUID) {
		String profilePrefix = profileUUID.equals("0") ? "" : profileUUID + "_";
		configManager.unsetConfiguration(GearSwitchAlertConfig.GROUP, TAG_KEY_PREFIX + profilePrefix + itemId);
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event) {
		if (!client.isKeyPressed(KeyCode.KC_SHIFT)) {
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();
		for (int idx = entries.length - 1; idx >= 0; --idx) {
			final MenuEntry entry = entries[idx];
			final Widget w = entry.getWidget();

			if(w != null) {
				final int group = WidgetUtil.componentToInterface(w.getId());
				if ((group == InterfaceID.INVENTORY || group == InterfaceID.EQUIPMENT)
						&& "Examine".equals(entry.getOption()) && entry.getIdentifier() == 10) {
					int itemId = w.getItemId();
					if (itemId == -1 && w.getChildren() != null) {
						for (Widget child : w.getChildren()) {
							itemId = child.getItemId();
							if (itemId != -1)
								break;
						}
					}

					if (itemId == -1)
						return;

					ItemStats itemStats = itemManager.getItemStats(itemId, false);
					if (!config.allowTaggingUnequipables() && (itemStats == null || !itemStats.isEquipable())) {
						return;
					}

					final GearTagSettings gearTagSettings = getTag(itemId);
					final Menu parent = client.getMenu().createMenuEntry(idx)
							.setOption("Gear Switch Alert Tagging")
							.createSubMenu();

					boolean isMeleeEnabled, isRangeEnabled, isMagicEnabled;
					if (gearTagSettings != null) {
						isMeleeEnabled = gearTagSettings.isMeleeGear;
						isRangeEnabled = gearTagSettings.isRangeGear;
						isMagicEnabled = gearTagSettings.isMagicGear;
					} else {
						isMagicEnabled = false;
						isMeleeEnabled = false;
						isRangeEnabled = false;
					}
					int finalItemId = itemId;

					parent.createMenuEntry(0)
							.setOption(ColorUtil.prependColorTag(isMeleeEnabled ? "Unset Melee Gear" : "Set Melee Gear", config.defaultColourMelee()))
							.setType(MenuAction.RUNELITE)
							.onClick(e ->
									toggleGearTag(gearTagSettings, finalItemId, true, false, false));

					parent.createMenuEntry(0)
							.setOption(ColorUtil.prependColorTag(isRangeEnabled ? "Unset Range Gear" : "Set Range Gear", config.defaultColourRanged()))
							.setType(MenuAction.RUNELITE)
							.onClick(e ->
									toggleGearTag(gearTagSettings, finalItemId, false, true, false));

					parent.createMenuEntry(0)
							.setOption(ColorUtil.prependColorTag(isMagicEnabled ? "Unset Magic Gear" : "Set Magic Gear", config.defaultColourMagic()))
							.setType(MenuAction.RUNELITE)
							.onClick(e ->
									toggleGearTag(gearTagSettings, finalItemId, false, false, true));

				}
			}
		}
	}

	public void toggleGearTag(GearTagSettingsWithItemID tag, boolean toggleMelee, boolean toggleRanged, boolean toggleMagic) {
		toggleGearTag(tag.origTag, tag.itemID, toggleMelee, toggleRanged, toggleMagic);
	}

	public void toggleGearTag(GearTagSettings tag, int itemId, boolean toggleMelee, boolean toggleRanged, boolean toggleMagic) {
		GearTagSettings newGearTagSettings = tag;
		if (newGearTagSettings == null)
			newGearTagSettings = new GearTagSettings();

		newGearTagSettings.isMeleeGear = toggleMelee != newGearTagSettings.isMeleeGear;
		newGearTagSettings.isRangeGear = toggleRanged != newGearTagSettings.isRangeGear;
		newGearTagSettings.isMagicGear = toggleMagic != newGearTagSettings.isMagicGear;
		setTag(itemId, newGearTagSettings);
	}

	private ItemStats getItemStats(int itemId) {
		return itemManager.getItemStats(itemId, false);
	}

	public String loadSelectedProfile() {
		String profileID = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, SELECTED_PROFILE_PREFIX);

		if (Strings.isNullOrEmpty(profileID)) {
			return "0";
		}

		String ID = gson.fromJson(profileID, new TypeToken<String>() {
		}.getType());

		if(profiles.containsKey(ID))
			return ID;
		else
			return "0";
	}

	public void loadProfiles() {
		Map<String, String> parsed = new HashMap<>();
		parsed.put("0", "Default");
		//merge in any custom profiles
		Map<String, String> customProfiles = loadCustomProfiles();
		parsed.putAll(customProfiles);

		profiles = parsed;
	}

	private Map<String, String> loadCustomProfiles() {
		String json = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, PROFILES_PREFIX);

		if (Strings.isNullOrEmpty(json)) {
			return new HashMap<>();
		}
		return gson.fromJson(json, new TypeToken<Map<String, String>>() {
		}.getType());
	}

	Integer loadCustomId() {
		String json = configManager.getConfiguration(GearSwitchAlertConfig.GROUP, CUSTOM_ID);

		if (Strings.isNullOrEmpty(json)) {
			//default to 9999 because we add 1, and I want it to start at an even 10k.
			return 0;
		}
		return gson.fromJson(json, new TypeToken<Integer>() {
		}.getType());
	}

	public String addProfile(String name) {
		int customId = loadCustomId() + 1;
		Map<String, String> customProfiles = loadCustomProfiles();
		String newID = ID_PREFIX + customId;
		customProfiles.put(newID, name);

		String json = gson.toJson(customProfiles);
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, PROFILES_PREFIX, json);
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, CUSTOM_ID, customId);

		return newID;
	}

	public void deleteProfile(String profileUUID) {
		if(profileUUID.equals("0"))
			return;

		String loadedProfile = loadSelectedProfile();
		if(loadedProfile.equals(profileUUID))
			setEnabledProfile("0");

		Map<String, String> customProfiles = loadCustomProfiles();
		customProfiles.remove(profileUUID);
		if (customProfiles.isEmpty()) {
			configManager.unsetConfiguration(GearSwitchAlertConfig.GROUP, PROFILES_PREFIX);
			return;
		}

		String json = gson.toJson(customProfiles);
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, PROFILES_PREFIX, json);
	}

	public void setEnabledProfile(String profileUUID) {
		configManager.setConfiguration(GearSwitchAlertConfig.GROUP, SELECTED_PROFILE_PREFIX, profileUUID);
	}

	private void loadSprites() {
		for (Prayer p : new Prayer[]{Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MAGIC}) {
			BufferedImage img = spriteManager.getSprite(p.getSpriteID(), 0);
			if (img != null) {
				BufferedImage norm = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
				Graphics g = norm.getGraphics();
				g.drawImage(img, norm.getWidth() / 2 - img.getWidth() / 2, norm.getHeight() / 2 - img.getHeight() / 2, null);
				prayerSprites.put(p, norm);
			}
		}

		if(panel != null)
			SwingUtilities.invokeLater(panel::loadProfiles);
	}

	BufferedImage getSprite(Prayer p) {
		return prayerSprites.get(p);
	}

	public void duplicateProfile(String profileUUID, String name) {
		clientThread.invokeLater(() -> {
			String newProfileID = addProfile(name);
			setEnabledProfile(newProfileID);
			for(GearTagSettingsWithItemID tag : getTagsForProfile(profileUUID)) {
				setTag(tag.itemID, tag.origTag, newProfileID);
			}

			loadProfiles();
			SwingUtilities.invokeLater(() -> {
				panel.reload();
			});
		});
	}

	public void addTagBySearch(String profileUDID) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			JOptionPane.showMessageDialog(panel,
					"You must be logged in to search.",
					"Cannot Search for Item",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		itemSearch
				.tooltipText("Add item tag")
				.onItemSelected((itemId) ->
						clientThread.invokeLater(() ->
						{
							int finalId = itemManager.canonicalize(itemId);
							ItemStats itemStats = itemManager.getItemStats(finalId, false);
							if(config.allowTaggingUnequipables() || (itemStats != null && itemStats.isEquipable())) {
								setTag(finalId, new GearTagSettings(), profileUDID);
							} else {
								SwingUtilities.invokeLater(() ->
										JOptionPane.showMessageDialog(panel,
												"Only equipable items can be tagged!",
												"Cannot Add Item Tag",
												JOptionPane.ERROR_MESSAGE));
							}
						}))
				.build();
	}

	public void removeTag(Integer itemID, String profileUUID) {
		unsetTag(itemID, profileUUID);
		loadProfiles();
		overlay.invalidateCache();
		panel.reload();
	}

	public void removeAllTags(String profileUUID) {
		clientThread.invokeLater(() -> {
			for(GearTagSettingsWithItemID tag : getTagsForProfile(profileUUID)) {
				unsetTag(tag.itemID, profileUUID);
			}
			loadProfiles();
			overlay.invalidateCache();

			SwingUtilities.invokeLater(() -> {
				panel.reload();
			});
		});
	}

	public void importProfileFromClipboard(String clipboardText, Component parent) {
		clientThread.invokeLater(() -> {
			try {
				ProfileSerialization profileSerialization = gson.fromJson(clipboardText, new TypeToken<ProfileSerialization>() {
				}.getType());
				String profileID = addProfile(profileSerialization.name);
				for (Map.Entry<Integer, GearTagSettings> set : profileSerialization.tags.entrySet()) {
					Integer itemId = set.getKey();
					GearTagSettings tag = set.getValue();

					setTag(itemId, tag, profileID);
				}
				loadProfiles();
				overlay.invalidateCache();
				SwingUtilities.invokeLater(() -> {
					panel.reload();
				});
			} catch (JsonSyntaxException e) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Nothing in clipboard!", null, JOptionPane.ERROR_MESSAGE));
			}
		});
	}

	public void exportProfileToClipboard(String profileID, Component parent) {
		if(profiles.containsKey(profileID)) {
			clientThread.invokeLater(() -> {
				ProfileSerialization profileSerialization = new ProfileSerialization();
				profileSerialization.name = profiles.get(profileID);

				ArrayList<GearTagSettingsWithItemID> tags = getTagsForProfile(profileID);
				for (GearTagSettingsWithItemID tag : tags) {
					profileSerialization.tags.put(tag.itemID, tag.origTag);
				}

				String json = gson.toJson(profileSerialization);

				try {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
				} catch (Exception e) {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Failed to export to clipboard!", null, JOptionPane.ERROR_MESSAGE));
				}
			});
		} else {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Profile Not Found!", null, JOptionPane.ERROR_MESSAGE));
		}
	}

	@Subscribe
	void onGameStateChanged(GameStateChanged event) {
		if(event.getGameState().equals(GameState.LOGGED_IN)) {
			overlay.resetDelayTimer();
		}
	}
}
