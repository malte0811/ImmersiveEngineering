package blusunrize.immersiveengineering.data.render;

import net.minecraft.data.DirectoryCache;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ExistingResourceManager implements IResourceManager
{
	private final ExistingFileHelper helper;

	public ExistingResourceManager(ExistingFileHelper helper, DirectoryCache generatedFiled)
	{
		this.helper = helper;
		//TODO also respect generated files!
	}

	@Nonnull
	@Override
	public Set<String> getResourceNamespaces()
	{
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public IResource getResource(@Nonnull ResourceLocation resourceLocationIn) throws IOException
	{
		return helper.getResource(resourceLocationIn, ResourcePackType.CLIENT_RESOURCES);
	}

	@Override
	public boolean hasResource(@Nonnull ResourceLocation path)
	{
		return helper.exists(path, ResourcePackType.CLIENT_RESOURCES);
	}

	@Nonnull
	@Override
	public List<IResource> getAllResources(@Nonnull ResourceLocation resourceLocationIn) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Collection<ResourceLocation> getAllResourceLocations(@Nonnull String pathIn, @Nonnull Predicate<String> filter)
	{
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public Stream<IResourcePack> getResourcePackStream()
	{
		throw new UnsupportedOperationException();
	}
}
