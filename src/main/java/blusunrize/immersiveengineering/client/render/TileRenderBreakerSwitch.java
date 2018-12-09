/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityBreakerSwitch;
import blusunrize.immersiveengineering.common.util.chickenbones.Matrix4;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static blusunrize.immersiveengineering.common.util.Utils.RAND;

public class TileRenderBreakerSwitch extends TileEntitySpecialRenderer<TileEntityBreakerSwitch>
{
	//TODO move everything to TE-level
	private Set<Spark> sparks = new HashSet<>();
	private long lastRenderedSwitch = 0;
	private int arcProgress;
	private static final Vector3f contactA = new Vector3f(5.5F/16, 10F/16, 6F/16);
	private static final Vector3f contactB = new Vector3f(1-5.5F/16, 10F/16, 6F/16);
	private static final int ARC_DURATION = 3;//TODO use real time rather than frame time

	@Override
	public void render(TileEntityBreakerSwitch te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(te.lastSwitchedAt!=lastRenderedSwitch)
		{
			lastRenderedSwitch = te.lastSwitchedAt;
			if(te.allowEnergyToPass(null))
			{
				final int numSpawn = 10;
				Matrix4 pointMat = te.getTransformation();
				Vector3f origin = pointMat.apply(contactA);
				for(int i = 0; i < numSpawn; ++i)
				{
					if(i==numSpawn/2)
						origin = pointMat.apply(contactB);
					Vector3f motion = new Vector3f((RAND.nextFloat()-.5F),
							(RAND.nextFloat()-.5F)*6,
							(RAND.nextFloat()-.1F)*3);
					sparks.add(new Spark(origin, pointMat.mult3x3(motion), 25+RAND.nextInt(50)));
				}
			}
			else
				arcProgress = 0;
		}
		if(!sparks.isEmpty())
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, z);
			setLightmapDisabled(true);
			GlStateManager.disableTexture2D();
			GlStateManager.enableBlend();
			GlStateManager.color(1F, 1F, 1F, 1F);
			Vector3f pos = null;
			Tessellator tes = ClientUtils.tes();
			BufferBuilder bb = tes.getBuffer();
			bb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			for(Iterator<Spark> it = sparks.iterator(); it.hasNext(); )
			{
				Spark s = it.next();
				if(s.shouldRemove())
				{
					it.remove();
					continue;
				}
				pos = s.getPos(pos);
				float r = 1-RAND.nextFloat()/5;
				float g = (RAND.nextFloat()-.5F)/5+.5F;
				float b = RAND.nextFloat()/5;
				bb.pos(pos.x, pos.y, pos.z).color(r, g, b, 1).endVertex();
				final double scale = .05;
				bb.pos(pos.x+s.motion.x*scale, pos.y+s.motion.y*scale, pos.z+s.motion.z*scale)
						.color(r, g, b, 1).endVertex();
			}
			tes.draw();
			GlStateManager.disableBlend();
			GlStateManager.enableTexture2D();
			setLightmapDisabled(false);
			GlStateManager.popMatrix();
		}
		if(arcProgress < ARC_DURATION)
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, z);
			setLightmapDisabled(true);
			GlStateManager.disableLighting();
			GlStateManager.shadeModel(GL11.GL_SMOOTH);
			GlStateManager.color(1, 1, 1, 1);
			GlStateManager.enableBlend();
			GlStateManager.disableTexture2D();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GlStateManager.disableCull();
			//TODO cleanup
			GlStateManager.color(1, 1, 1, 1);
			Matrix4 pointMat = te.getTransformation();
			Vector3f start = pointMat.apply(contactA);
			final float DELTA = .0625F;
			Vector3f deltaA = pointMat.mult3x3(new Vector3f(DELTA, 0, 0));
			Vector3f deltaB = pointMat.mult3x3(new Vector3f(0, DELTA, 0));
			Tessellator tes = ClientUtils.tes();
			BufferBuilder bb = tes.getBuffer();
			bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			for(int i = 0; i < 2; ++i)
			{
				Vector3f end = pointMat.mult3x3(new Vector3f(0, 0, arcProgress*.25F/ARC_DURATION));
				Vector3f.add(end, start, end);
				bb.pos(start.x, start.y, start.z).color(1F, 1F, 1F, 1F).endVertex();
				bb.pos(start.x+deltaA.x, start.y+deltaA.y, start.z+deltaA.z).color(62/255F, 229/255F, 163/255F, .5F).endVertex();
				bb.pos(end.x+deltaA.x, end.y+deltaA.y, end.z+deltaA.z).color(62/255F, 229/255F, 163/255F, .5F).endVertex();
				bb.pos(end.x, end.y, end.z).color(1F, 1F, 1F, 1F).endVertex();

				bb.pos(start.x, start.y, start.z).color(1F, 1F, 1F, 1F).endVertex();
				bb.pos(start.x-deltaA.x, start.y-deltaA.y, start.z-deltaA.z).color(62/255F, 229/255F, 163/255F, .5F).endVertex();
				bb.pos(end.x-deltaA.x, end.y-deltaA.y, end.z-deltaA.z).color(62/255F, 229/255F, 163/255F, .5F).endVertex();
				bb.pos(end.x, end.y, end.z).color(1F, 1F, 1F, 1F).endVertex();
				start = pointMat.apply(contactB);
			}
			tes.draw();
			GlStateManager.disableBlend();
			GlStateManager.enableTexture2D();
			setLightmapDisabled(false);
			GlStateManager.popMatrix();
			GlStateManager.enableCull();
			++arcProgress;
		}
		//Arc color: 62, 229, 163
	}

	private class Spark
	{
		@Nonnull
		private final Vector3f origin;
		@Nonnull
		private final Vector3f motion;
		private final long createdAt;
		private final long maxAge;

		private Spark(@Nonnull Vector3f origin, @Nonnull Vector3f motion, int maxAge)
		{
			this.origin = origin;
			this.motion = motion;
			this.createdAt = System.currentTimeMillis();
			this.maxAge = maxAge+createdAt;
		}

		public Vector3f getPos(Vector3f out)
		{
			if(out==null)
				out = new Vector3f();
			out.set(motion);
			out.scale(((float)(System.currentTimeMillis()-createdAt))/1000);
			return Vector3f.add(out, origin, out);
		}

		public boolean shouldRemove()
		{
			return System.currentTimeMillis() > maxAge;
		}

		@Override
		public boolean equals(Object o)
		{
			if(this==o) return true;
			if(o==null||getClass()!=o.getClass()) return false;

			Spark spark = (Spark)o;

			if(createdAt!=spark.createdAt) return false;
			if(maxAge!=spark.maxAge) return false;
			if(!origin.equals(spark.origin)) return false;
			return motion.equals(spark.motion);
		}

		@Override
		public int hashCode()
		{
			int result = origin.hashCode();
			result = 31*result+motion.hashCode();
			result = 31*result+(int)(createdAt^(createdAt >>> 32));
			result = 31*result+(int)(maxAge^(maxAge >>> 32));
			return result;
		}
	}
}
