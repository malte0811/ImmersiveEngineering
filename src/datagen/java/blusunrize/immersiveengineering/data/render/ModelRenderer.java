package blusunrize.immersiveengineering.data.render;

import blusunrize.immersiveengineering.common.util.DirectionUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
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
		// Set up GL
		glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		glViewport(0, 0, width, height);
		glBindTexture(GL_TEXTURE_2D, texture.getGlTextureId());

		glClear(256);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		// TODO figure out where the .25 comes from. Maybe blocks always render too big?
		glOrtho(-1.25, .25, -1.25, .25, -1000, 1000);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		RenderHelper.enableStandardItemLighting();
		RenderHelper.setupGui3DDiffuseLighting();

		BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.enableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();

		// Actually render
		MatrixStack stack = new MatrixStack();
		model = model.handlePerspective(TransformType.GUI, stack);
		stack.scale(1.0F, -1.0F, 1.0F);
		stack.translate(0, 0.5, 0);
		bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.BLOCK);
		for(Direction side : DIRECTIONS_AND_NULL)
			for(BakedQuad quad : model.getQuads(null, side, RANDOM, EmptyModelData.INSTANCE))
				bufferbuilder.addQuad(
						stack.getLast(), quad, 1, 1, 1, LightTexture.packLight(15, 15), OverlayTexture.NO_OVERLAY
				);
		bufferbuilder.finishDrawing();
		WorldVertexBufferUploader.draw(bufferbuilder);

		exportTo("icons/", filename, renderedTexture);
		exportTo("icons/", "atlas.png", texture.getGlTextureId());
		System.out.println("glGetError: "+glGetError());
	}

	private void exportTo(String outputFolder, String fileName, int texture)
	{
		glBindTexture(GL_TEXTURE_2D, texture);
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
