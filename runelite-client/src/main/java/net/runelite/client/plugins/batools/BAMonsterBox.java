package net.runelite.client.plugins.batools;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BAMonsterBox extends Counter
{
	private final String name;
	private final Color color;

	public BAMonsterBox(BufferedImage img, Plugin plugin, int seconds, String name, Color color)
	{
		super(img, plugin, seconds);
		this.name = name;
		this.color = color;
	}

	@Override
	public String getTooltip()
	{
		return name;
	}

	@Override
	public Color getTextColor()
	{
		return color;
	}
}