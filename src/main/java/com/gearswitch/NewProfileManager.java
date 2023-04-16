package com.gearswitch;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

@Slf4j
public class NewProfileManager extends PluginPanel {
    private final JLabel inputLabel;
    private final FlatTextField nameInput;

    NewProfileManager(GearSwitchAlertPlugin plugin, GearSwitchAlertPanel panel) {
        super();

        this.inputLabel = new JLabel("Profile Name");
        add(inputLabel);

        this.nameInput = new FlatTextField();
        nameInput.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        nameInput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        nameInput.setMinimumSize(new Dimension(0, 30));
        add(nameInput);

        JButton addProfileButton = new JButton();
        addProfileButton.setText("Add Profile");
        addProfileButton.setHorizontalAlignment(JLabel.CENTER);
        addProfileButton.setFocusable(false);
        addProfileButton.setPreferredSize((new Dimension(PluginPanel.PANEL_WIDTH - 10, 30)));
        addProfileButton.addActionListener(e ->
        {
            if (nameInput.getText().isEmpty()) {
                JOptionPane.showMessageDialog(inputLabel, "Must add a profile name!", null, JOptionPane.ERROR_MESSAGE);
                return;
            }
            plugin.addProfile(nameInput.getText());
            plugin.loadProfiles();
            panel.loadProfiles();
        });
        add(addProfileButton);

        JButton importProfileButton = new JButton();
        importProfileButton.setText("Import Profile From Clipboard");
        importProfileButton.setHorizontalAlignment(JLabel.CENTER);
        importProfileButton.setFocusable(false);
        importProfileButton.setPreferredSize((new Dimension(PluginPanel.PANEL_WIDTH - 10, 30)));
        importProfileButton.addActionListener(e ->
        {
            final String clipboardText;
            try
            {
                clipboardText = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
            }
            catch (IOException | UnsupportedFlavorException ignore)
            {
                JOptionPane.showMessageDialog(importProfileButton, "Nothing in clipboard!", null, JOptionPane.ERROR_MESSAGE);
                return;
            }
            plugin.importProfileFromClipboard(clipboardText, importProfileButton);
        });
        add(importProfileButton);
    }
}