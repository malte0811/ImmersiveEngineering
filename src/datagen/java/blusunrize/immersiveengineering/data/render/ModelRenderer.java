package blusunrize.immersiveengineering.data.render;

import blusunrize.immersiveengineering.common.util.DirectionUtils;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL40.*;

public class ModelRenderer
{
	private static final Direction[] DIRECTIONS_AND_NULL = Arrays.copyOf(DirectionUtils.VALUES, 7);
	private static final Random RANDOM = new Random(0);

	private final int width;
	private final int height;
	private final AtlasTexture texture;
	private final int framebufferID;
	private final int renderedTexture;
	private final int depthBuffer;

	public ModelRenderer(int width, int height, AtlasTexture texture)
	{
		this.width = width;
		this.height = height;
		this.texture = texture;
		this.framebufferID = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);

		this.renderedTexture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, renderedTexture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTexture, 0);

		this.depthBuffer = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
		//TODO do we need/want stencil?
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
	}

	// TODO free GL resources

	public void renderModel(IBakedModel model, String filename)
	{
		// Gather quads
		List<BakedQuad> quads = new ArrayList<>();
		for(Direction side : DIRECTIONS_AND_NULL)
			quads.addAll(model.getQuads(null, side, RANDOM, EmptyModelData.INSTANCE));
		// Set up GL
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		glViewport(0, 0, width, height/2);
		glActiveTexture(texture.getGlTextureId()+GL_TEXTURE0);

		glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0.0D, 854.0D, 480.0D, 0.0D, -1000.0D, 1000.0D);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glEnable(GL_BLEND);

		glBegin(GL_QUADS);
		glTexCoord2d(0, 0);
		glVertex2f(0.0F, 0.0F);
		glTexCoord2d(0, 1);
		glVertex2f(0.0F, 480.0F);
		glTexCoord2d(1, 1);
		glVertex2f(854.0F, 480.0F);
		glTexCoord2d(1, 0);
		glVertex2f(854.0F, 0.0F);
		glEnd();

		glBindTexture(GL_TEXTURE_2D, renderedTexture);
		exportTo("icons/", filename);
		glBindTexture(GL_TEXTURE_2D, texture.getGlTextureId());
		exportTo("icons/", "atlas.png");
		System.out.println("glGetError: "+glGetError());
	}

	private void exportTo(String outputFolder, String fileName)
	{
		//TODO glBindTexture(GL_TEXTURE_2D, renderedTexture);
		int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
		int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
		int size = width*height;
		BufferedImage bufferedimage = new BufferedImage(width, height, 2);

		File output = new File(outputFolder, fileName);
		IntBuffer buffer = BufferUtils.createIntBuffer(size);
		int[] data = new int[size];

		glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
		buffer.get(data);
		bufferedimage.setRGB(0, 0, width, height, data, 0, width);

		try
		{
			ImageIO.write(bufferedimage, "png", output);
		} catch(IOException xcp)
		{
			throw new RuntimeException(xcp);
		}
	}
}
