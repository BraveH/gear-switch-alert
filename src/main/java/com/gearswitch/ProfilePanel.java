package com.gearswitch;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

@Slf4j
class ProfilePanel extends JPanel {

    private static final int ROW_WIDTH = PluginPanel.PANEL_WIDTH - 10;
    private static final int ROW_HEIGHT = 30;
    private static final int RIGHT_PANEL_WIDTH = 60;
    private static final int ITEMS_PER_ROW = 5;

    static final ImageIcon UNTICKED_ICON;
    static final ImageIcon UNTICKED_ICON_HOVER;
    private static final ImageIcon TICKED_ICON;
    static final ImageIcon TICKED_ICON_HOVER;
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon DELETE_ICON_HOVER;
    static final ImageIcon COLLAPSED_ICON;
    static final ImageIcon COLLAPSED_ICON_HOVER;
    static final ImageIcon EXPANDED_ICON;
    static final ImageIcon EXPANDED_ICON_HOVER;
    static final ImageIcon PLUS_ICON;
    static final ImageIcon PLUS_ICON_HOVER;
    private static final int PRAYER_SIZE = 12;

    final JPanel rowContainer = new JPanel();
    final JPanel rightPanel = new JPanel();
    final JLabel profileName;
    final JLabel addProfile;
    final JLabel removeProfile;
    private final GearSwitchAlertPlugin plugin;
    private final GearSwitchAlertPanel panel;
    private final ItemManager itemManager;
    private JLabel deleteCustomProfile;

    JLabel collapseBtn;

    private final JPanel tagsContainer = new JPanel();
    private final String profileUUID;
    private final Cache<Long, Image> fillCache;

    static {
        final BufferedImage untickedIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "unticked_icon.png");
        UNTICKED_ICON = new ImageIcon(untickedIcon);
        UNTICKED_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(untickedIcon, 0.53f));
        final BufferedImage tickedIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "ticked_icon.png");
        TICKED_ICON = new ImageIcon(tickedIcon);
        TICKED_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(tickedIcon, 0.50f));
        final BufferedImage deleteIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "delete_icon.png");
        DELETE_ICON = new ImageIcon(deleteIcon);
        DELETE_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(deleteIcon, 0.50f));
        final BufferedImage collapsedIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "collapsed_icon.png");
        COLLAPSED_ICON = new ImageIcon(collapsedIcon);
        COLLAPSED_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(collapsedIcon, 0.50f));
        final BufferedImage expandedIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "expanded_icon.png");
        EXPANDED_ICON = new ImageIcon(expandedIcon);
        EXPANDED_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(expandedIcon, 0.50f));
        final BufferedImage plusIcon = ImageUtil.loadImageResource(GearSwitchAlertPlugin.class, "plus_icon.png");
        PLUS_ICON = new ImageIcon(plusIcon);
        PLUS_ICON_HOVER =  new ImageIcon(ImageUtil.alphaOffset(plusIcon, 0.50f));
    }

    ProfilePanel(ClientThread clientThread, GearSwitchAlertPlugin plugin, ItemManager itemManager, GearSwitchAlertPanel panel, String profileUUID, String profileName) {
        super();

        fillCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(32)
                .build();

        this.plugin = plugin;
        this.panel = panel;
        this.itemManager = itemManager;
        this.profileUUID = profileUUID;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        String selectedProfile = plugin.loadSelectedProfile();
        boolean enabled = profileUUID.equals(selectedProfile);

        rowContainer.setLayout(new BorderLayout());
        rowContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rowContainer.setPreferredSize(new Dimension(ROW_WIDTH, ROW_HEIGHT));
        rowContainer.setBorder(new EmptyBorder(8, 8, 6, 8));
        add(rowContainer);

        add(Box.createRigidArea(new Dimension(ROW_WIDTH, 1)));

        this.profileName = new JLabel(profileName);
        this.profileName.setFont(FontManager.getRunescapeFont());
        if(!profileUUID.equals("0"))
            this.profileName.setToolTipText(profileUUID);
        rowContainer.add(this.profileName, BorderLayout.WEST);

        rightPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.LINE_AXIS));
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightPanel.setPreferredSize(new Dimension(RIGHT_PANEL_WIDTH, ROW_HEIGHT));
        rowContainer.add(rightPanel, BorderLayout.EAST);

        addProfile = new JLabel();
        addProfile.setIcon(UNTICKED_ICON);

        removeProfile = new JLabel();
        removeProfile.setIcon(TICKED_ICON);

        collapseBtn = new JLabel();
        collapseBtn.setIcon(EXPANDED_ICON);

        if (enabled) {
            rightPanel.add(removeProfile, BorderLayout.EAST);
            rightPanel.add(Box.createHorizontalStrut(8));
            rightPanel.add(collapseBtn, BorderLayout.EAST);
        } else {
            rightPanel.add(addProfile, BorderLayout.EAST);
        }
        rightPanel.add(Box.createHorizontalStrut(8));

        if(!profileUUID.equals("0")) {
            deleteCustomProfile = new JLabel();
            deleteCustomProfile.setIcon(DELETE_ICON);
            deleteCustomProfile.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        final int result = JOptionPane.showOptionDialog(rowContainer,
                                "Are you sure you want to delete profile: "+ profileName +"?",
                                "Delete Profile?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                null, new String[]{"Yes", "No"}, "No");

                        if (result == JOptionPane.YES_OPTION)
                        {
                            plugin.deleteProfile(profileUUID);
                            plugin.loadProfiles();
                            panel.loadProfiles();
                            rightPanel.revalidate();
                            rightPanel.repaint();
                        }
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    deleteCustomProfile.setIcon(DELETE_ICON_HOVER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    deleteCustomProfile.setIcon(DELETE_ICON);
                }
            });
            rightPanel.add(deleteCustomProfile, BorderLayout.WEST);
        }

        final JMenuItem duplicate = new JMenuItem("Duplicate Profile");
        duplicate.addActionListener(e ->
        {
            // TODO
            // ALSO TODO, why do profiles get created but disappear?
            final String result = JOptionPane.showInputDialog(rowContainer,
                    "What's the name of the new profile?",
                    "Duplicate Profile?", JOptionPane.INFORMATION_MESSAGE);

            if (result != null && !Strings.isNullOrEmpty(result))
            {
                plugin.duplicateProfile(profileUUID, result);
            } else {
                JOptionPane.showMessageDialog(rowContainer, "Must add a profile name!", null, JOptionPane.ERROR_MESSAGE);
            }
        });


        final JMenuItem reset = new JMenuItem("Reset Profile");
        reset.addActionListener(e ->
        {
            final int result = JOptionPane.showOptionDialog(rowContainer,
                    "Are you sure you want to reset profile: "+ profileName +"?",
                    "Reset Profile?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, new String[]{"Yes", "No"}, "No");

            if (result == JOptionPane.YES_OPTION) {
                plugin.removeAllTags(profileUUID);
            }
        });

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        popupMenu.add(duplicate);
        popupMenu.add(reset);
        rowContainer.setComponentPopupMenu(popupMenu);

        if(enabled) {
            add(tagsContainer, BorderLayout.CENTER);

            clientThread.invokeLater(this::rebuild);
        }
    }

    private void buildItems(ArrayList<GearTagSettingsWithItemID> items)
    {
        // Calculates how many rows need to be display to fit all items
        int size = items.size();

        tagsContainer.removeAll();
        tagsContainer.setLayout(new GridLayout(0, ITEMS_PER_ROW, 1, 1));

        final EmptyBorder emptyBorder = new EmptyBorder(5, 5, 5, 5);
        for (int i = 0; i < size; i++)
        {
            final GearTagSettingsWithItemID item = items.get(i);
            GearTagSettings tag = item.origTag;

            final JPanel slotContainer = new JPanel();
            slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            final JLayeredPane layeredPanel = new JLayeredPane();

            final JLabel imageLabel = new JLabel();
            Integer itemID = item.itemID;
            imageLabel.setToolTipText(item.name);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

            AsyncBufferedImage itemImage = itemManager.getImage(itemID, 1, false);
            int width = itemImage.getWidth();
            int height = itemImage.getHeight();
            itemImage.addTo(imageLabel);

            imageLabel.setBounds( 0, 0, width, height );
            layeredPanel.add(imageLabel);
            layeredPanel.setLayer(imageLabel, 0);

            final JLabel imageLabel2 = new JLabel();
            imageLabel2.setVerticalAlignment(SwingConstants.BOTTOM);
            imageLabel2.setHorizontalAlignment(SwingConstants.CENTER);

            AsyncBufferedImage prayerImg = new AsyncBufferedImage(width, PRAYER_SIZE, TYPE_INT_ARGB);
            prayerImg = applyPrayerToImage(prayerImg, tag.isMeleeGear, tag.isRangeGear, tag.isMagicGear);
            prayerImg.addTo(imageLabel2);

            imageLabel2.setBounds( 0, height, width, PRAYER_SIZE );
            layeredPanel.add(imageLabel2);
            layeredPanel.setLayer(imageLabel2, 10);
            layeredPanel.setPreferredSize(new Dimension(width, height+PRAYER_SIZE));

            // Create popup menu
            final JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.setBorder(emptyBorder);
            layeredPanel.setComponentPopupMenu(popupMenu);

            final JMenuItem meleeToggle = new JMenuItem(tag.isMeleeGear ? "Unset Melee Gear" : "Set Melee Gear");
            meleeToggle.addActionListener(e ->
                    plugin.toggleGearTag(item, true, false, false));

            final JMenuItem rangeToggle = new JMenuItem(tag.isRangeGear ? "Unset Range Gear" : "Set Range Gear");
            rangeToggle.addActionListener(e ->
                    plugin.toggleGearTag(item, false, true, false));

            final JMenuItem magicToggle = new JMenuItem(tag.isMagicGear ? "Unset Magic Gear" : "Set Magic Gear");
            magicToggle.addActionListener(e ->
                    plugin.toggleGearTag(item, false, false, true));

            final JMenuItem removeTag = new JMenuItem("Remove Tag");
            removeTag.addActionListener(e ->
                    plugin.removeTag(item.itemID, profileUUID));

            popupMenu.add(meleeToggle);
            popupMenu.add(rangeToggle);
            popupMenu.add(magicToggle);
            popupMenu.add(removeTag);

            String enabledTagsText;
            if(!tag.isMeleeGear && !tag.isRangeGear && !tag.isMagicGear) {
                enabledTagsText = "No Tags Set";
            } else {
                StringBuilder str = new StringBuilder();
                String delimiter = " - ";
                if(tag.isMeleeGear)
                    str.append("Melee").append(delimiter);
                if(tag.isRangeGear)
                    str.append("Ranged").append(delimiter);
                if(tag.isMagicGear)
                    str.append("Magic").append(delimiter);

                String list = str.toString();
                enabledTagsText = list.substring(
                        0, list.length() - delimiter.length());
            }
            imageLabel2.setToolTipText(enabledTagsText);

            slotContainer.add(layeredPanel);

            tagsContainer.add(slotContainer);
            slotContainer.revalidate();
        }

        final JPanel slotContainer = new JPanel();
        slotContainer.setLayout(new BorderLayout());
        slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel addLbl = new JLabel();
        addLbl.setIcon(PLUS_ICON);
        addLbl.setVerticalAlignment(SwingConstants.CENTER);
        addLbl.setHorizontalAlignment(SwingConstants.CENTER);
        addLbl.setPreferredSize(new Dimension(36, 32));
        slotContainer.add(addLbl, BorderLayout.CENTER);

        JLabel addLblTxt = new JLabel();
        addLblTxt.setText("Add Item");
        addLblTxt.setFont(FontManager.getRunescapeSmallFont());
        addLblTxt.setHorizontalAlignment(SwingConstants.CENTER);
        addLblTxt.setPreferredSize(new Dimension(36, PRAYER_SIZE));
        slotContainer.add(addLblTxt, BorderLayout.SOUTH);

        slotContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    plugin.addTagBySearch(profileUUID);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                addLbl.setIcon(PLUS_ICON_HOVER);
                applyDimmer(false, slotContainer);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addLbl.setIcon(PLUS_ICON);
                applyDimmer(true, slotContainer);
            }
        });

        tagsContainer.add(slotContainer);
        slotContainer.revalidate();

        tagsContainer.revalidate();
    }

    public AsyncBufferedImage applyPrayerToImage(AsyncBufferedImage img, boolean melee, boolean range, boolean magic) {
        Graphics g = img.getGraphics();
        int imgWidth = img.getWidth();

        BufferedImage meleeSprite = plugin.getSprite(Prayer.PROTECT_FROM_MELEE);
        BufferedImage rangeSprite = plugin.getSprite(Prayer.PROTECT_FROM_MISSILES);
        BufferedImage magicSprite = plugin.getSprite(Prayer.PROTECT_FROM_MAGIC);
        g.drawImage(melee ? getFillImage(panel.config.defaultColourMelee(), meleeSprite, 0) : getFillImage(ColorUtil.colorWithAlpha(Color.GRAY, 30), meleeSprite, 4), 0, 0, PRAYER_SIZE, PRAYER_SIZE, null);
        g.drawImage(range ? getFillImage(panel.config.defaultColourRanged(), rangeSprite, 1) : getFillImage(ColorUtil.colorWithAlpha(Color.GRAY, 30), rangeSprite, 5), (imgWidth/2) - (PRAYER_SIZE/2), 0, PRAYER_SIZE, PRAYER_SIZE, null);
        g.drawImage(magic ? getFillImage(panel.config.defaultColourMagic(), magicSprite, 2) : getFillImage(ColorUtil.colorWithAlpha(Color.GRAY, 30), magicSprite, 6), imgWidth - PRAYER_SIZE, 0, PRAYER_SIZE, PRAYER_SIZE, null);

        return img;
    }

    void rebuild() {
        ArrayList<GearTagSettingsWithItemID> items = this.plugin.getTagsForProfile(profileUUID);

        SwingUtilities.invokeLater(() ->{
            buildItems(items);
            revalidate();
        });
    }

    void toggle()
    {
        if(tagsContainer == null)
            return;

        boolean collapsed = isCollapsed();
        tagsContainer.setVisible(collapsed);

        if(collapsed) {
            collapseBtn.setIcon(EXPANDED_ICON);
        } else {
            collapseBtn.setIcon(COLLAPSED_ICON);
        }
    }

    boolean isCollapsed()
    {
        return !tagsContainer.isVisible();
    }

    void applyDimmer(boolean brighten, JPanel panel)
    {
        for (Component component : panel.getComponents())
        {
            Color color = component.getForeground();

            component.setForeground(brighten ? color.brighter() : color.darker());
        }
    }

    private Image getFillImage(Color fillColor, BufferedImage img, long key)
    {
        Image image = fillCache.getIfPresent(key);
        if (image == null)
        {
            image = ImageUtil.fillImage(img, fillColor);
            fillCache.put(key, image);
        }
        return image;
    }
}