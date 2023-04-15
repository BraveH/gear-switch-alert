package com.gearswitch;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gearswitch.GearSwitchAlertPlugin.profiles;
import static com.gearswitch.ProfilePanel.*;

public class GearSwitchAlertPanel extends PluginPanel {

    private final GearSwitchAlertPlugin plugin;
    private final ItemManager itemManager;

    private final IconTextField searchBar;
    private final JPanel listContainer = new JPanel();
    public final GearSwitchAlertConfig config;
    private final ClientThread clientThread;

    @Setter
    @Getter
    private ProfilePanel enabledProfileTile = null;

    GearSwitchAlertPanel(ClientThread clientThread, GearSwitchAlertPlugin plugin, ItemManager itemManager, GearSwitchAlertConfig config) {
        super();
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.config = config;

        add(Box.createRigidArea(new Dimension(PluginPanel.PANEL_WIDTH - 10, 10)));

        Font font = FontManager.getRunescapeFont();
        Map<TextAttribute, Integer> attributes = new HashMap<>();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

        JLabel titleLabel = new JLabel("Dynamic Inventory Tags");
        titleLabel.setFont(font.deriveFont(Font.BOLD, 20).deriveFont(attributes));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel);

        add(Box.createRigidArea(new Dimension(PluginPanel.PANEL_WIDTH - 10, 10)));

        this.searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.setMinimumSize(new Dimension(0, 30));
        searchBar.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                loadProfiles();
            }
        });
        searchBar.addClearListener(this::loadProfiles);
        add(searchBar);

        add(listContainer);
//        listContainer.setLayout(new GridLayout(0, 1, 0, 0));
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        NewProfileManager newProfileManager = new NewProfileManager(plugin, this);

        add(newProfileManager);

        loadProfiles();
    }

    private int sortProfiles(Map.Entry<String, String> profile1, Map.Entry<String, String> profile2) {
        if(profile1.getKey().equals("0"))
            return -1;
        if(profile2.getKey().equals("0"))
            return 1;

        return profile1.getValue().compareTo(profile2.getValue());
    }


    void loadProfiles() {
        listContainer.removeAll();
        String search = searchBar.getText();
        String selectedProfile = plugin.loadSelectedProfile();

        for (Map.Entry<String, String> profile : profiles.entrySet().stream().sorted(this::sortProfiles).collect(Collectors.toList())) {
            if (Strings.isNullOrEmpty(search) || profile.getValue().toLowerCase().contains(search.toLowerCase())) {
                String key = profile.getKey();
                ProfilePanel tile = new ProfilePanel(clientThread, plugin, itemManager, this, key, profile.getValue());
                if(!key.equals(selectedProfile)) {
                    addMouseListener(key, tile);
                }
                else {
                    setEnabledProfileTile(tile);
                    tile.rowContainer.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                tile.toggle();
                            }
                        }
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            tile.applyDimmer(false, tile.rowContainer);
                            if(tile.isCollapsed()) {
                                tile.collapseBtn.setIcon(COLLAPSED_ICON_HOVER);
                            } else {
                                tile.collapseBtn.setIcon(EXPANDED_ICON_HOVER);
                            }
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            tile.applyDimmer(true, tile.rowContainer);
                            if(tile.isCollapsed()) {
                                tile.collapseBtn.setIcon(COLLAPSED_ICON);
                            } else {
                                tile.collapseBtn.setIcon(EXPANDED_ICON);
                            }
                        }
                    });
                }
                listContainer.add(tile);
                listContainer.add(Box.createRigidArea(new Dimension(0, 3)));
            }
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private void addMouseListener(String key, ProfilePanel tile) {
        tile.rowContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    plugin.setEnabledProfile(key);
                    tile.removeProfile.setIcon(TICKED_ICON_HOVER);
                    tile.rightPanel.add(tile.removeProfile, BorderLayout.EAST);
                    tile.rightPanel.remove(tile.addProfile);
                    tile.rightPanel.revalidate();
                    tile.rightPanel.repaint();

                    ProfilePanel enabledTile = getEnabledProfileTile();
                    if(enabledTile != null) {
                        enabledTile.rightPanel.add(enabledTile.addProfile, BorderLayout.EAST);
                        enabledTile.rightPanel.remove(enabledTile.removeProfile);
                        enabledTile.rightPanel.revalidate();
                        enabledTile.rightPanel.repaint();
                    }
                    setEnabledProfileTile(tile);

                    loadProfiles();
                    plugin.overlay.invalidateCache();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                tile.addProfile.setIcon(UNTICKED_ICON_HOVER);
                tile.applyDimmer(false, tile.rowContainer);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tile.addProfile.setIcon(UNTICKED_ICON);
                tile.applyDimmer(true, tile.rowContainer);
            }
        });
    }
}