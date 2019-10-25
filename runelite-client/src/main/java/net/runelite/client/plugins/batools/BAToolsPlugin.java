/*
 * Copyright (c) 2018, Cameron <https://github.com/noremac201>
 * Copyright (c) 2018, Jacob M <https://github.com/jacoblairm>
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
package net.runelite.client.plugins.batools;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "BA Tools",
	description = "Custom tools for Barbarian Assault",
	tags = {"minigame", "overlay", "timer"}
)
public class BAToolsPlugin extends Plugin implements KeyListener
{
	private BufferedImage fighterImage, rangerImage, healerImage, runnerImage;
	int inGameBit = 0;
	int tickNum;
	int pastCall = 0;
	private int lastHealer;
	private GameTimer gameTime;
	private static final String START_WAVE = "1";
	private String currentWave = START_WAVE;

	//base
	//[0] BA_BASE_POINTS,
	//att
	//[1] BA_FAILED_ATTACKER_ATTACKS_POINTS,
	//[2] BA_RANGERS_KILLED,
	//[3] BA_FIGHTERS_KILLED,
	//def
	//[4] BA_RUNNERS_PASSED_POINTS,
	//[5] BA_RUNNERS_KILLED,
	//coll
	//[6] BA_EGGS_COLLECTED_POINTS,
	//heal
	//[7] BA_HEALERS_KILLED,
	//[8] BA_HITPOINTS_REPLENISHED_POINTS,
	//[9] BA_WRONG_POISON_PACKS_POINTS
	//[10] BA_EGGS_COLLECTED
	//[11] BA_FAILED_ATTACKER_ATTACKS
	//[12] BA_HITPOINTS_REPLENISHED

	final int[] childIDsOfPointsWidgets = new int[]{33, 32, 25, 26, 24, 28, 31, 27, 29, 30, 21, 22, 19};

	private int pointsHealer, pointsDefender , pointsCollector, pointsAttacker, totalEggsCollected, totalIncorrectAttacks, totalHealthReplenished;

	private final List<MenuEntry> entries = new ArrayList<>();
	private HashMap<Integer, Instant> foodPressed = new HashMap<>();
	private CycleCounter counter;

	private BAMonsterBox[] monsterDeathInfoBox = new BAMonsterBox[4];

	private static final String ENDGAME_REWARD_NEEDLE_TEXT = "<br>5";

	private boolean shiftDown;
	private boolean ctrlDown;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BAToolsConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Getter
	private Instant wave_start;

	@Inject
	private KeyManager keyManager;

	@Inject
	private BAToolsOverlay overlay;

	@Getter
	private final Map<GroundItem.GroundItemKey, GroundItem> collectedGroundItems = new LinkedHashMap<>();


	@Provides
	BAToolsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BAToolsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		wave_start = Instant.now();
		foodPressed.clear();
		keyManager.registerKeyListener(this);
		lastHealer = 0;
		fighterImage = ImageUtil.getResourceStreamFromClass(getClass(), "fighter.png");
		rangerImage = ImageUtil.getResourceStreamFromClass(getClass(), "ranger.png");
		runnerImage = ImageUtil.getResourceStreamFromClass(getClass(), "runner.png");
		healerImage = ImageUtil.getResourceStreamFromClass(getClass(), "healer.png");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		removeCounter();
		inGameBit = 0;
		gameTime = null;
		currentWave = START_WAVE;
		client.setInventoryDragDelay(5);
		keyManager.unregisterKeyListener(this);
		shiftDown = false;
		collectedGroundItems.clear();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		switch (event.getGroupId())
		{
			case WidgetID.BA_REWARD_GROUP_ID:
			{
				Widget rewardWidget = client.getWidget(WidgetInfo.BA_REWARD_TEXT);
				Widget pointsWidget = client.getWidget(WidgetID.BA_REWARD_GROUP_ID, 14); //RUNNERS_PASSED

				if (rewardWidget != null && rewardWidget.getText().contains(ENDGAME_REWARD_NEEDLE_TEXT) && gameTime != null)
				{
					gameTime = null;

					ChatMessageBuilder message = new ChatMessageBuilder()
					.append("Attacker: ")
					.append(Color.red, pointsAttacker+"")
					.append(" |  Healer: ")
					.append(Color.GREEN, pointsHealer+"")
					.append(" | Defender: ")
					.append(Color.blue, pointsDefender+"")
					.append(" | Collector: ")
					.append(Color.yellow, pointsCollector+"")
					.append(System.getProperty("line.separator"))
					.append(totalEggsCollected + " eggs collected, "+ totalHealthReplenished + " HP vialed and " + totalIncorrectAttacks+" wrong attacks.");

					if(config.announcePointBreakdown())
					{
						chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message.build()).build());
					}
				}
				else if(pointsWidget != null && client.getVar(Varbits.IN_GAME_BA) == 0)
				{
					int wavePoints_Attacker, wavePoints_Defender, wavePoints_Healer, wavePoints_Collector, waveEggsCollected, waveHPReplenished, waveFailedAttacks;

					wavePoints_Attacker = wavePoints_Defender = wavePoints_Healer = wavePoints_Collector = Integer.parseInt(client.getWidget(WidgetID.BA_REWARD_GROUP_ID, childIDsOfPointsWidgets[0]).getText()); //set base pts to all roles
					waveEggsCollected = waveHPReplenished = waveFailedAttacks = 0;

					for (int i = 0; i < childIDsOfPointsWidgets.length; i++)
					{
						int value = Integer.parseInt(client.getWidget(WidgetID.BA_REWARD_GROUP_ID, childIDsOfPointsWidgets[i]).getText());

						switch (i)
						{
							case 1:
							case 2:
							case 3:
								wavePoints_Attacker += value;
								pointsAttacker += value;
								break;
							case 4:
							case 5:
								wavePoints_Defender += value;
								pointsDefender += value;
								break;
							case 6:
								wavePoints_Collector += value;
								pointsCollector += value;
								break;
							case 7:
							case 8:
							case 9:
								wavePoints_Healer += value;
								pointsHealer += value;
								break;
							case 10:
								waveEggsCollected = value;
								totalEggsCollected += value;
								break;
							case 11:
								waveFailedAttacks = value;
								totalIncorrectAttacks += value;
								break;
							case 12:
								waveHPReplenished = value;
								totalHealthReplenished += value;
								break;
						}
					}

					ChatMessageBuilder message = new ChatMessageBuilder()
					.append("Attacker: ")
					.append(Color.red, wavePoints_Attacker+"")
					.append(" |  Healer: ")
					.append(Color.GREEN, wavePoints_Healer+"")
					.append(" | Defender: ")
					.append(Color.blue, wavePoints_Defender+"")
					.append(" | Collector: ")
					.append(Color.yellow, wavePoints_Collector+"")
					.append(System.getProperty("line.separator"))
					.append(waveEggsCollected + " eggs eollected, "+ waveHPReplenished + "HP vialed and " + waveFailedAttacks+" wrong attacks.");

					if(config.announcePointBreakdown())
					{
						chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message.build()).build());
					}
				}
			}
		}
	}

	@Subscribe
	public void onWidgetHiddenChanged(WidgetHiddenChanged event)
	{

		//Attack Styles
		Widget weapon = client.getWidget(593, 1);

		if(config.attackStyles()
			&& weapon!=null
			&& inGameBit == 1
			&& (weapon.getText().contains("Crystal halberd") || weapon.getText().contains("Dragon claws") || weapon.getText().contains("Scythe"))
			&& client.getWidget(WidgetInfo.BA_ATK_LISTEN_TEXT)!=null)
		{
			String style = client.getWidget(WidgetInfo.BA_ATK_LISTEN_TEXT).getText();

			if(style.contains("Defensive"))
			{
				client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_TWO).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_FOUR).setHidden(false);
			}
			else if(style.contains("Aggressive"))
			{
				client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_TWO).setHidden(false);
				client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_FOUR).setHidden(true);
			}
			else if(style.contains("Controlled"))
			{
				if(weapon.getText().contains("Crystal halberd"))
				{
					client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(false);
					client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(true);
				}
				else if(weapon.getText().contains("Scythe"))
				{
					client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(true);
					client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(true);
				}
				else
				{
					client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(true);
					client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(false);
				}
				client.getWidget(WidgetInfo.COMBAT_STYLE_TWO).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_FOUR).setHidden(true);
			}
			else if(style.contains("Accurate") && (weapon.getText().contains("Dragon claws") || weapon.getText().contains("Scythe")))
			{
				client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(false);
				client.getWidget(WidgetInfo.COMBAT_STYLE_TWO).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(true);
				client.getWidget(WidgetInfo.COMBAT_STYLE_FOUR).setHidden(true);
			}
			else
			{
				client.getWidget(WidgetInfo.COMBAT_STYLE_ONE).setHidden(false);
				client.getWidget(WidgetInfo.COMBAT_STYLE_TWO).setHidden(false);
				client.getWidget(WidgetInfo.COMBAT_STYLE_THREE).setHidden(false);
				client.getWidget(WidgetInfo.COMBAT_STYLE_FOUR).setHidden(false);
			}

		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (config.antiDrag())
		{
			client.setInventoryDragDelay(config.antiDragDelay());
		}

		setColourEgg();

		Widget callWidget = getWidget();

		if (callWidget != null)
		{
			if (callWidget.getTextColor() != pastCall && callWidget.getTextColor() == 16316664)
			{
				tickNum = 0;
			}
			pastCall = callWidget.getTextColor();
		}
		if (config.defTimer() && inGameBit == 1)
		{
			if (tickNum > 9)
			{
				tickNum = 0;
			}
			if (counter == null)
			{
				addCounter();
			}
			counter.setCount(tickNum++);
		}

	}

	private Widget getWidget()
	{
		if (client.getWidget(WidgetInfo.BA_DEF_CALL_TEXT) != null)
		{
			return client.getWidget(WidgetInfo.BA_DEF_CALL_TEXT);
		}
		else if (client.getWidget(WidgetInfo.BA_ATK_CALL_TEXT) != null)
		{
			return client.getWidget(WidgetInfo.BA_ATK_CALL_TEXT);
		}
		else if (client.getWidget(WidgetInfo.BA_COLL_CALL_TEXT) != null)
		{
			return client.getWidget(WidgetInfo.BA_COLL_CALL_TEXT);
		}
		else if (client.getWidget(WidgetInfo.BA_HEAL_CALL_TEXT) != null)
		{
			return client.getWidget(WidgetInfo.BA_HEAL_CALL_TEXT);
		}
		return null;
	}

	private void setColourEgg()
	{
		if(inGameBit == 0 || client.getWidget(WidgetInfo.BA_COLL_LISTEN_TEXT) == null)
		{
			overlay.setEggColour(null);
			return;
		}

		String eggC = client.getWidget(WidgetInfo.BA_COLL_LISTEN_TEXT).getText().split(" ")[0].toLowerCase();

		if(eggC.equals("red")||eggC.equals("green")||eggC.equals("blue"))
		{
			overlay.setEggColour(eggC);
		}
		else
		{
			overlay.setEggColour(null);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int inGame = client.getVar(Varbits.IN_GAME_BA);

		if (inGameBit != inGame)
		{
			if (inGameBit == 1)
			{
				pastCall = 0;
				removeCounter();
				foodPressed.clear();
			}
			else
			{
				addCounter();
				lastHealer = 0;
			}
		}

		inGameBit = inGame;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		String option = Text.removeTags(event.getOption()).toLowerCase();
		String target = Text.removeTags(event.getTarget()).toLowerCase();

		//Incorrect call remover
		if (config.calls() && getWidget() != null && event.getTarget().endsWith("horn") && inGameBit==1)
		{
			MenuEntry[] menuEntries = client.getMenuEntries();
			Widget callWidget = getWidget();
			String call = callWidget.getText();
			MenuEntry correctCall = null;
			entries.clear();

			for (MenuEntry entry : menuEntries)
			{
				if (entry.getOption().contains("Tell-") && call.toLowerCase().contains(entry.getOption().substring(5)))
				{
					correctCall = entry;
				}
				else if (!entry.getOption().startsWith("Tell-"))
				{
					entries.add(entry);
				}
			}

			if (correctCall != null) //&& callWidget.getTextColor()==16316664)
			{
				entries.add(correctCall);
				client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
			}
		}

		//Ladder swap
		if (config.swapLadder() && option.equals("climb-down") && target.equals("ladder"))
		{
			swap("quick-start", option, target, true);
		}

		//Ctrl Healer
		if(client.getWidget(WidgetInfo.BA_HEAL_CALL_TEXT) == getWidget() && lastHealer != 0 && inGameBit == 1 && config.ctrlHealer() && ctrlDown)
		{
			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry correctHealer = null;
			entries.clear();

			for (MenuEntry entry : menuEntries)
			{

				if((entry.getIdentifier() == lastHealer  && entry.getOption().equals("Use"))
						||
						(
								(entry.getTarget().equals("<col=ff9040>Poisoned meat") || entry.getTarget().equals("<col=ff9040>Poisoned worms") || entry.getTarget().equals("<col=ff9040>Poisoned tofu"))
								&&
										(entry.getOption().equals("Use")||entry.getOption().equals("Cancel"))
						)
				)
				{
					correctHealer = entry;
				}
				else if (!option.startsWith("use"))
				{
					entries.add(entry);
				}
			}
			if (correctHealer != null)
			{
				entries.add(correctHealer);
			}
			client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
		}

		//Target menu times/colour
		if ((event.getTarget().contains("Penance Healer") || event.getTarget().contains("Penance Fighter") || event.getTarget().contains("Penance Ranger")))
		{

			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry lastEntry = menuEntries[menuEntries.length - 1];
			String targett = lastEntry.getTarget();

			if (foodPressed.containsKey(lastEntry.getIdentifier()))
			{
				lastEntry.setTarget(lastEntry.getTarget().split("\\(")[0] + "(" + Duration.between(foodPressed.get(lastEntry.getIdentifier()), Instant.now()).getSeconds() + ")");
				if (Duration.between(foodPressed.get(lastEntry.getIdentifier()), Instant.now()).getSeconds() > 20)
				{
					lastEntry.setTarget(lastEntry.getTarget().replace("<col=ffff00>", "<col=2bff63>"));
				}
			}
			else
			{
				lastEntry.setTarget(targett.replace("<col=ffff00>", "<col=2bff63>"));

			}

			client.setMenuEntries(menuEntries);
		}

		//Collector helper
		if (client.getWidget(WidgetInfo.BA_COLL_LISTEN_TEXT) != null && inGameBit == 1 && config.eggBoi() && event.getTarget().endsWith("egg") && shiftDown)
		{
			String[] currentCall = client.getWidget(WidgetInfo.BA_COLL_LISTEN_TEXT).getText().split(" ");
			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry correctEgg = null;
			entries.clear();

			for (MenuEntry entry : menuEntries)
			{
				if (entry.getTarget().contains(currentCall[0]) && entry.getOption().equals("Take"))
				{
					correctEgg = entry;
				}
				else if (!entry.getOption().startsWith("Take"))
				{
					entries.add(entry);
				}
			}
			if (correctEgg != null)
			{
				entries.add(correctEgg);
			}
			client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
		}

		//Attacker shift to walk here
		if (client.getWidget(WidgetInfo.BA_ATK_LISTEN_TEXT) != null && inGameBit == 1 && config.atkShiftToWalk() && shiftDown)
		{
			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry correctEgg = null;
			entries.clear();

			for (MenuEntry entry : menuEntries)
			{
				if (entry.getOption().contains("Walk here"))
				{
					entries.add(entry);
				}
			}
			client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
		}

		//Shift healer OS
		if (client.getWidget(WidgetInfo.BA_HEAL_LISTEN_TEXT) != null && inGameBit == 1 && config.osHelp() && event.getTarget().equals("<col=ffff>Healer item machine") && shiftDown)
		{
			String[] currentCall = client.getWidget(WidgetInfo.BA_HEAL_LISTEN_TEXT).getText().split(" ");

			if (!currentCall[0].contains("Pois."))
			{
				return;
			}

			MenuEntry[] menuEntries = client.getMenuEntries();
			MenuEntry correctEgg = null;
			entries.clear();

			for (MenuEntry entry : menuEntries)
			{
				if (entry.getOption().equals("Take-" + currentCall[1]))
				{
					correctEgg = entry;
				}
			}
			if (correctEgg != null)
			{
				entries.add(correctEgg);
				client.setMenuEntries(entries.toArray(new MenuEntry[entries.size()]));
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String target = event.getMenuTarget();

		if(config.tagging() && (event.getMenuTarget().contains("Penance Ranger") || event.getMenuTarget().contains("Penance Fighter")))
		{
			if (event.getMenuOption().contains("Attack"))
			{
				foodPressed.put(event.getId(), Instant.now());
			}
			log.info(target);
		}

		if (config.healerMenuOption() && target.contains("Penance Healer") && target.contains("<col=ff9040>Poisoned") && target.contains("->"))
		{
			foodPressed.put(event.getId(), Instant.now());
			lastHealer = event.getId();
			log.info("Last healer changed: " + lastHealer);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (config.antiDrag())
		{
			client.setInventoryDragDelay(config.antiDragDelay());
		}
		if(!config.deathTimeBoxes() || !config.defTimer())
		{
			removeCounter();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if(event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().startsWith("All of the Penance") && gameTime != null && inGameBit !=0)
		{
			String[] message = event.getMessage().split(" ");
			final int waveSeconds = (int)gameTime.getTimeInSeconds(true);

			if(config.deathTimeBoxes())
			{
				switch (message[4])
				{
					case "Healers":
						monsterDeathInfoBox[0] = new BAMonsterBox(healerImage, this, waveSeconds, message[4], Color.green);
						infoBoxManager.addInfoBox(monsterDeathInfoBox[0]);
						break;
					case "Runners":
						monsterDeathInfoBox[1] = new BAMonsterBox(runnerImage, this, waveSeconds, message[4], Color.blue);
						infoBoxManager.addInfoBox(monsterDeathInfoBox[1]);
						break;
					case "Fighters":
						monsterDeathInfoBox[2] = new BAMonsterBox(fighterImage, this, waveSeconds, message[4], Color.red);
						infoBoxManager.addInfoBox(monsterDeathInfoBox[2]);
						break;
					case "Rangers":
						monsterDeathInfoBox[3] = new BAMonsterBox(rangerImage, this, waveSeconds, message[4], Color.red);
						infoBoxManager.addInfoBox(monsterDeathInfoBox[3]);
						break;
				}
			}
			if(config.monsterDeathTimeChat())
			{
				final MessageNode node = event.getMessageNode();
				final String nodeValue = Text.removeTags(node.getValue());
				node.setValue(nodeValue + " (<col=ff0000>" + gameTime.getTimeInSeconds(true) + "s<col=ffffff>)");
				chatMessageManager.update(node);
			}
		}

		if (event.getMessage().startsWith("---- Wave:"))
		{
			String[] message = event.getMessage().split(" ");
			currentWave = message[2];

			if (currentWave.equals(START_WAVE))
			{
				gameTime = new GameTimer();
				pointsHealer = pointsDefender = pointsCollector = pointsAttacker = totalEggsCollected = totalIncorrectAttacks = totalHealthReplenished = 0;
			}
			else if (gameTime != null)
			{
				gameTime.setWaveStartTime();
			}
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		TileItem item = itemSpawned.getItem();
		Tile tile = itemSpawned.getTile();

		GroundItem groundItem = buildGroundItem(tile, item);

		GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(item.getId(), tile.getWorldLocation());
		GroundItem existing = collectedGroundItems.putIfAbsent(groundItemKey, groundItem);
		if (existing != null)
		{
			existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned itemDespawned)
	{
		TileItem item = itemDespawned.getItem();
		Tile tile = itemDespawned.getTile();

		GroundItem.GroundItemKey groundItemKey = new GroundItem.GroundItemKey(item.getId(), tile.getWorldLocation());
		GroundItem groundItem = collectedGroundItems.get(groundItemKey);
		if (groundItem == null)
		{
			return;
		}

		if (groundItem.getQuantity() <= item.getQuantity())
		{
			collectedGroundItems.remove(groundItemKey);
		}
		else
		{
			groundItem.setQuantity(groundItem.getQuantity() - item.getQuantity());
		}
	}

	private GroundItem buildGroundItem(final Tile tile, final TileItem item)
	{
		// Collect the data for the item
		final int itemId = item.getId();
		final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;

		final GroundItem groundItem = GroundItem.builder()
				.id(itemId)
				.location(tile.getWorldLocation())
				.itemId(realItemId)
				.quantity(item.getQuantity())
				.name(itemComposition.getName())
				.height(tile.getItemLayer().getHeight())
				.build();

		return groundItem;
	}

	private void addCounter()
	{
		if (!config.defTimer() || counter != null)
		{
			return;
		}

		int itemSpriteId = ItemID.FIGHTER_TORSO;

		BufferedImage taskImg = itemManager.getImage(itemSpriteId);
		counter = new CycleCounter(taskImg, this, tickNum);

		infoBoxManager.addInfoBox(counter);
	}

	private void removeCounter()
	{
		if (counter != null)
		{
			infoBoxManager.removeInfoBox(counter);
			counter = null;
		}
		for (int i=0;i<monsterDeathInfoBox.length;i++)
		{
			if(monsterDeathInfoBox[i]!=null)
			{
				infoBoxManager.removeInfoBox(monsterDeathInfoBox[i]);
				monsterDeathInfoBox[i] = null;
			}
		}
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

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			shiftDown = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			ctrlDown = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			shiftDown = false;
		}
		if (e.getKeyCode() == KeyEvent.VK_CONTROL)
		{
			ctrlDown = false;
		}
	}

}