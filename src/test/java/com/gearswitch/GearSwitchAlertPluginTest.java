package com.gearswitch;

import com.gearhighlighter.GearHighlighterPlugin;
import com.toasolvers.ZebakJugSolverPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GearSwitchAlertPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GearSwitchAlertPlugin.class, GearHighlighterPlugin.class,
				ZebakJugSolverPlugin.class);//, HetSolverPlugin.class);
		RuneLite.main(args);
	}
}