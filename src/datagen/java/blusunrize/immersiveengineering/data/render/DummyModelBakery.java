package blusunrize.immersiveengineering.data.render;

import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.resources.IResourceManager;

public class DummyModelBakery extends ModelBakery
{
	protected DummyModelBakery(IResourceManager resourceManagerIn, BlockColors blockColorsIn)
	{
		super(resourceManagerIn, blockColorsIn, true);
	}
}
