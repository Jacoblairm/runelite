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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("BATools")
public interface BAToolsConfig extends Config
{
	@ConfigItem(
		keyName = "defTimer",
		name = "Defender Tick Timer",
		description = "Shows the current cycle tick of runners."
	)
	default boolean defTimer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "calls",
		name = "Remove Incorrect Calls",
		description = "Remove incorrect calls."
	)
	default boolean calls()
	{
		return false;
	}

	@ConfigItem(
		keyName = "healerMenuOption",
		name = "Healer menu options",
		description = "Shows time since last food placed on healer"
	)
	default boolean healerMenuOption()
	{
		return false;
	}

	@ConfigItem(
		keyName = "antiDrag",
		name = "Anti Drag",
		description = "asd"
	)
	default boolean antiDrag()
	{
		return false;
	}

	@ConfigItem(
		keyName = "antiDragDelay",
		name = "Anti Drag Delay",
		description = "Similar to antidrag plugin but does not require shift to be help down"
	)
	default int antiDragDelay()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "eggBoi",
		name = "Collector helper",
		description = "asd"
	)
	default boolean eggBoi()
	{
		return false;
	}

	@ConfigItem(
		keyName = "osHelp",
		name = "Shift OS",
		description = "Hold shift to only pick up correct eggs"
	)
	default boolean osHelp()
	{
		return false;
	}


	@ConfigItem(
		keyName = "attackStyles",
		name = "Attack Styles",
		description = "Remove incorrect attack styles for hally/claws and if shift held down = all attack options are removed to help walk to the correct tiles"
	)
	default boolean attackStyles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tagging",
		name = "Attack Tags",
		description = "Highlights the menu entry of an attacker/ranger that has not been tagged."
	)
	default boolean tagging()
	{
		return false;
	}

	@ConfigItem(
			keyName = "ctrlHealer",
			name = "Control Healer",
			description = "Hold ctrl to put last healer clicked on top"
		)
	default boolean ctrlHealer()
	{
		return false;
	}

	@ConfigItem(
			keyName = "monsterDeathTimeChat",
			name = "Monster Death Time in Chat",
			description = "Shows time in seconds when a specific monster dies"
	)
	default boolean monsterDeathTimeChat()
	{
		return false;
	}

	@ConfigItem(
			keyName = "deathTimeBoxes",
			name = "Monster Death Boxes",
			description = "Shows info boxes when a specific monster dies and time"
	)
	default boolean deathTimeBoxes()
	{
		return false;
	}

	@ConfigItem(
			keyName = "atkShiftToWalk",
			name = "Attack shift to walk",
			description = "Hold shift to walk where you click"
	)
	default boolean atkShiftToWalk()
	{
		return false;
	}

	@ConfigItem(
			keyName = "announcePointBreakdown",
			name = "Show wave point breakdown",
			description = "Shows points for each role at the end of each wave"
	)
	default boolean announcePointBreakdown()
	{
		return false;
	}

	@ConfigItem(
			keyName = "announcePointBreakdownGame",
			name = "Show game point breakdown",
			description = "Show at the end of game"
	)
	default boolean announcePointBreakdownGame()
	{
		return true;
	}


}
