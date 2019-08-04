/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Kamiel
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.rc;

import net.runelite.client.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostItemComposition;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@PluginDescriptor(
	name = "Runecrafting addons",
	description = "Change the default option that is displayed when hovering over objects",
	tags = {"npcs", "inventory", "items", "objects"},
	enabledByDefault = false
)
public class RCPlugin extends Plugin
{
	private final List<MenuEntry> entries = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private RCConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MenuManager menuManager;

	@Getter
	private boolean configuringShiftClick = false;

	@Setter
	private boolean shiftModifier = false;

	@Provides
	RCConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RCConfig.class);
	}


	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		int itemId = event.getIdentifier();
		String option = Text.removeTags(event.getOption()).toLowerCase();
		String target = Text.removeTags(event.getTarget()).toLowerCase();



		if (config.swapTeleports() && option.equals("remove") && target.startsWith("ring of dueling"))
		{
			swap("duel arena", option, target, true);
		}
		if (config.swapTeleports() && option.equals("remove") && target.startsWith("crafting cape"))
		{
			swap("teleport", option, target, true);
		}
		if (config.swapPouches() && (target.equals("giant pouch") ||
			target.equals("large pouch") ||
			target.equals("medium pouch") ||
			target.equals("small pouch")))
		{
			if(option.equals("fill"))
			{
				swap("empty", option, target, true);
			}
			else
				if(option.equals("deposit-all"))
				{
					swapWithdraw("fill", option, target, true);
				}
		}
		if (config.swapTrade() && option.equalsIgnoreCase("follow"))
		{
			List<MenuEntry> tradeFix = new ArrayList<>();
			MenuEntry[] menuEntries = client.getMenuEntries();
			int i = 0;
			for (MenuEntry m : menuEntries) {
				if (m.getOption().contains("Trade")) {
					tradeFix.add(m);
				}
			}
			client.setMenuEntries(tradeFix.toArray(new MenuEntry[] {}));
		}

		if (config.disableCraftAltar()) {

			MenuEntry[] menuEntries = client.getMenuEntries();
			entries.clear();
			for (MenuEntry entry : menuEntries)
			{
				if (!option.startsWith("Craft-rune"))
				{
					entries.add(entry);
				}
			}
			client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
		}
	}

	private int searchIndex(MenuEntry[] entries, String option, String target, boolean strict)
	{
		for (int i = entries.length - 1; i >= 0; i--)
		{
			MenuEntry entry = entries[i];
			String entryOption = Text.removeTags(entry.getOption()).toLowerCase();
			String entryTarget = Text.removeTags(entry.getTarget()).toLowerCase();

			if (strict)
			{
				if (entryOption.equals(option) && entryTarget.equals(target))
				{
					return i;
				}
			}
			else
			{
				if (entryOption.contains(option.toLowerCase()) && entryTarget.equals(target))
				{
					return i;
				}
			}
		}

		return -1;
	}

	private void swap(String optionA, String optionB, String target, boolean strict)
	{
		MenuEntry[] entries = client.getMenuEntries();

		int idxA = searchIndex(entries, optionA, target, strict);
		int idxB = searchIndex(entries, optionB, target, strict);

		if (idxA >= 0 && idxB >= 0)
		{
			MenuEntry entry = entries[idxA];
			entries[idxA] = entries[idxB];
			entries[idxB] = entry;

			client.setMenuEntries(entries);
		}
	}

	private void swapWithdraw(String optionA, String optionB, String target, boolean strict)
	{
		MenuEntry[] entries = client.getMenuEntries();
		MenuEntry fill = entries[2];

		entries[2] = entries[9];
		entries[9] = fill;
		client.setMenuEntries(entries);

	}
}
