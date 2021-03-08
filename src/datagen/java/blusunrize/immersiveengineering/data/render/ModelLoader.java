package blusunrize.immersiveengineering.data.render;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.AtlasTexture.SheetData;
import net.minecraft.profiler.EmptyProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ModelLoader
{
	private final IResourceManager resourceManager;
	private final ModelBakery bakery;
	private final List<ResourceLocation> modelLocations = new ArrayList<>();
	private final Map<ResourceLocation, IBakedModel> bakedModels = new Object2ObjectOpenHashMap<>();
	private final AtlasTexture atlas = new AtlasTexture(ImmersiveEngineering.rl("datagen"));

	public ModelLoader(IResourceManager manager)
	{
		this.resourceManager = manager;
		this.bakery = new DummyModelBakery(this.resourceManager, BlockColors.init());
	}

	public void add(ResourceLocation location)
	{
		modelLocations.add(location);
	}

	public void bake()
	{
		Map<ResourceLocation, IUnbakedModel> unbaked = new Object2ObjectOpenHashMap<>();
		for(ResourceLocation modelLocation : modelLocations)
		{
			IUnbakedModel unbakedModel = bakery.getUnbakedModel(modelLocation);
			unbaked.put(modelLocation, unbakedModel);
		}
		HashSet<Pair<String, String>> missing = new HashSet<>();
		final SheetData sheetData = atlas.stitch(
				resourceManager,
				unbaked.values().stream()
						.flatMap(u -> u.getTextures(bakery::getUnbakedModel, missing).stream())
						.map(RenderMaterial::getTextureLocation),
				EmptyProfiler.INSTANCE,
				0
		);
		atlas.upload(sheetData);
		for(Entry<ResourceLocation, IUnbakedModel> entry : unbaked.entrySet())
		{
			IBakedModel baked = entry.getValue().bakeModel(
					bakery, mat -> atlas.getSprite(mat.getTextureLocation()), ModelRotation.X0_Y0, entry.getKey()
			);
			bakedModels.put(entry.getKey(), baked);
		}
	}

	public AtlasTexture getAtlas()
	{
		return atlas;
	}

	public IBakedModel getModel(ResourceLocation loc)
	{
		return bakedModels.get(loc);
	}
}
