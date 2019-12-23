/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.gui.elements;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.client.ClientUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class GuiButtonState extends GuiButtonIE
{
	public boolean state;
	protected final int offsetDir;
	public int[] textOffset = {0, 0};

	public GuiButtonState(int x, int y, int w, int h, String name, boolean state, String texture, int u,
						  int v, int offsetDir, IPressable handler)
	{
		super(x, y, w, h, name, texture, u, v, handler);
		this.state = state;
		this.offsetDir = offsetDir;
		textOffset = new int[]{width+1, height/2-3};
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		Minecraft mc = Minecraft.getInstance();
		if(this.visible)
		{
			ClientUtils.bindTexture(texture);
			FontRenderer fontrenderer = mc.fontRenderer;
			GlStateManager.color3f(1.0F, 1.0F, 1.0F);
			this.isHovered = mouseX >= this.x&&mouseY >= this.y&&mouseX < this.x+this.width&&mouseY < this.y+this.height;
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			GlStateManager.blendFunc(770, 771);
			int u = texU+(!state?0: offsetDir==0?width: offsetDir==2?-width: 0);
			int v = texV+(!state?0: offsetDir==1?height: offsetDir==3?-height: 0);
			this.blit(x, y, u, v, width, height);
			if(!getMessage().isEmpty())
			{
				int txtCol = 0xE0E0E0;
				if(!this.active)
					txtCol = 0xA0A0A0;
				else if(this.isHovered)
					txtCol = Lib.COLOUR_I_ImmersiveOrange;
				this.drawString(fontrenderer, getMessage(), x+textOffset[0], y+textOffset[1], txtCol);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int key)
	{
		boolean b = super.mouseClicked(mouseX, mouseY, key);
		if(b)
			this.state = !state;
		return b;
	}
}
