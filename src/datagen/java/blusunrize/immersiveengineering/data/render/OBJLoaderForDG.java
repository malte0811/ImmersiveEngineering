package blusunrize.immersiveengineering.data.render;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.obj.LineReader;
import net.minecraftforge.client.model.obj.MaterialLibrary;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.client.model.obj.OBJModel.ModelSettings;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.BiFunction;

public class OBJLoaderForDG implements IModelLoader<OBJModel>
{
	private static final BiFunction<LineReader, ModelSettings, OBJModel> MAKE_OBJ_MODEL = Util.make(() -> {
		Constructor<OBJModel> construct;
		try
		{
			construct = OBJModel.class.getDeclaredConstructor(LineReader.class, ModelSettings.class);
		} catch(NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
		construct.setAccessible(true);
		return (reader, settings) -> {
			try
			{
				return construct.newInstance(reader, settings);
			} catch(IllegalAccessException|InstantiationException|InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}
		};
	});

	private final Map<ModelSettings, OBJModel> modelCache = Maps.newHashMap();
	private final Map<ResourceLocation, MaterialLibrary> materialCache = Maps.newHashMap();
	private final IResourceManager manager;

	public OBJLoaderForDG(IResourceManager manager)
	{
		this.manager = manager;
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager)
	{
	}

	@Override
	public OBJModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
	{
		if(!modelContents.has("model"))
			throw new RuntimeException("OBJ Loader requires a 'model' key that points to a valid .OBJ model.");

		String modelLocation = modelContents.get("model").getAsString();

		boolean detectCullableFaces = JSONUtils.getBoolean(modelContents, "detectCullableFaces", true);
		boolean diffuseLighting = JSONUtils.getBoolean(modelContents, "diffuseLighting", false);
		boolean flipV = JSONUtils.getBoolean(modelContents, "flip-v", false);
		boolean ambientToFullbright = JSONUtils.getBoolean(modelContents, "ambientToFullbright", true);
		@Nullable
		String materialLibraryOverrideLocation = modelContents.has("materialLibraryOverride")?JSONUtils.getString(modelContents, "materialLibraryOverride"): null;

		return loadModel(new OBJModel.ModelSettings(new ResourceLocation(modelLocation), detectCullableFaces, diffuseLighting, flipV, ambientToFullbright, materialLibraryOverrideLocation));
	}

	public OBJModel loadModel(OBJModel.ModelSettings settings)
	{
		return modelCache.computeIfAbsent(settings, (data) -> {

			try(IResource resource = manager.getResource(settings.modelLocation);
				LineReader rdr = new LineReader(resource))
			{
				return MAKE_OBJ_MODEL.apply(rdr, settings);
			} catch(FileNotFoundException e)
			{
				throw new RuntimeException("Could not find OBJ model", e);
			} catch(Exception e)
			{
				throw new RuntimeException("Could not read OBJ model", e);
			}
		});
	}

	public MaterialLibrary loadMaterialLibrary(ResourceLocation materialLocation)
	{
		return materialCache.computeIfAbsent(materialLocation, (location) -> {
			try(IResource resource = manager.getResource(location);
				LineReader rdr = new LineReader(resource))
			{
				return new MaterialLibrary(rdr);
			} catch(FileNotFoundException e)
			{
				throw new RuntimeException("Could not find OBJ material library", e);
			} catch(Exception e)
			{
				throw new RuntimeException("Could not read OBJ material library", e);
			}
		});
	}
}
