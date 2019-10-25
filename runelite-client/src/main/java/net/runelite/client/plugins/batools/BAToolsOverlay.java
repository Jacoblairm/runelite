/*
 * Copyright (c) 2018, Cameron <https://github.com/noremac201>
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


import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Perspective;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Polygon;
import javax.inject.Inject;
import java.util.Collection;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.Point;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayUtil;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

class BAToolsOverlay extends Overlay
{
	private final Client client;
	private final BAToolsPlugin plugin;
	private final BAToolsConfig config;
	private static final int MAX_DISTANCE = 2500;
	private final TextComponent textComponent = new TextComponent();

	@Getter
	@Setter
	private String eggColour;

	@Inject
	private BAToolsOverlay(Client client, BAToolsPlugin plugin, BAToolsConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "B.A. overlay"));

	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN || eggColour == null)
		{
			return null;
		}

		final Player player = client.getLocalPlayer();

		if (player == null || client.getViewportWidget() == null)
		{
			return null;
		}

		Collection<GroundItem> groundItemList = plugin.getCollectedGroundItems().values();
		final LocalPoint localLocation = player.getLocalLocation();


		for (GroundItem item : groundItemList)
		{
			if(!(item.getName().toLowerCase().equals(eggColour + " egg")))
			{
				continue;
			}

			Color colorEgg = Color.YELLOW;

			switch (eggColour)
			{
				case "blood":
					colorEgg = Color.RED;
					break;
				case "green":
					colorEgg = Color.GREEN;
					break;
				case "blue":
					colorEgg = Color.BLUE;
					break;
			}

			final LocalPoint groundPoint = LocalPoint.fromWorld(client, item.getLocation());

			if (groundPoint == null || localLocation.distanceTo(groundPoint) > MAX_DISTANCE)
			{
				continue;
			}

			final Polygon poly = Perspective.getCanvasTilePoly(client, groundPoint);

			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, colorEgg);
			}

			final Point textPoint = Perspective.getCanvasTextLocation(client,
					graphics,
					groundPoint,
					""+item.getQuantity(),
					item.getHeight() + 2);

			textComponent.setText(item.getQuantity()+"");
			textComponent.setColor(colorEgg);
			textComponent.setPosition(new java.awt.Point(textPoint.getX(), textPoint.getY()));
			textComponent.render(graphics);
		}

		return null;
	}
}
