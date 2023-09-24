package com.gearswitch;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GearSwitchAlertPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GearSwitchAlertPlugin.class);
		RuneLite.main(args);
	}
}
