package blusunrize.immersiveengineering.data.render;

import net.minecraft.data.DirectoryCache;
import net.minecraft.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ExistingResourceManager implements IResourceManager
{
	private final ExistingFileHelper helper;
	private final Path outputPath;

	public ExistingResourceManager(ExistingFileHelper helper, DirectoryCache generatedFiles)
	{
		this.helper = helper;
		try
		{
			Field field = DirectoryCache.class.getDeclaredField("outputFolder");
			field.setAccessible(true);
			Path outPath = (Path)field.get(generatedFiles);
			this.outputPath = outPath.resolve(ResourcePackType.CLIENT_RESOURCES.getDirectoryName());
		} catch(IllegalAccessException|NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
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
		try
		{
			// EFH is not really designed for this, "exists" also returns true for generated files, so exceptions for
			// control flow it is!
			return helper.getResource(resourceLocationIn, ResourcePackType.CLIENT_RESOURCES);
		} catch(IOException ignored)
		{
			return new SimpleResource(
					"generated", resourceLocationIn, Files.newInputStream(getGeneratedPath(resourceLocationIn)), null
			);
		}
	}

	@Override
	public boolean hasResource(@Nonnull ResourceLocation path)
	{
		return helper.exists(path, ResourcePackType.CLIENT_RESOURCES)||Files.exists(getGeneratedPath(path));
	}

	private Path getGeneratedPath(ResourceLocation file)
	{
		return this.outputPath.resolve(file.getNamespace()).resolve(file.getPath());
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
