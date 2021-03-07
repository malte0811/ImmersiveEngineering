package blusunrize.immersiveengineering.data;

import blusunrize.immersiveengineering.data.render.ModelLoader;
import blusunrize.immersiveengineering.data.render.ModelRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ItemRenderProvider implements IDataProvider
{
	private final ExistingFileHelper helper;
	private final GLFWErrorCallback loggingErrorCallback = GLFWErrorCallback.create((error, description) -> {
		System.err.println("Error "+error+": "+description);
	});

	public ItemRenderProvider(ExistingFileHelper helper)
	{
		this.helper = helper;
	}

	@Override
	public void act(@Nonnull DirectoryCache cache) throws IOException
	{
		// Hack together something that may work?
		if(!glfwInit())
		{
			throw new RuntimeException("Failed to initialize GLFW???");
		}
		RenderSystem.initRenderThread();
		glfwSetErrorCallback(loggingErrorCallback);
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		long window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		ModelLoader loader = new ModelLoader(helper, cache);
		ModelResourceLocation tcLoc = new ModelResourceLocation(
				Blocks.DIRT.asItem().getRegistryName().toString(), "inventory"
		);
		loader.add(tcLoc);
		loader.add(new ModelResourceLocation(
				Blocks.COBBLESTONE.asItem().getRegistryName().toString(), "inventory"
		));
		loader.bake();
		ModelRenderer renderer = new ModelRenderer(512, 512, loader.getAtlas());
		renderer.renderModel(loader.getModel(tcLoc), "teslacoil.png");
		glfwTerminate();
	}

	@Nonnull
	@Override
	public String getName()
	{
		return "Item Renderer";
	}
}
