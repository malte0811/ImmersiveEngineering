package blusunrize.immersiveengineering.client;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import blusunrize.immersiveengineering.common.blocks.IEBlocks.*;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.MetalScaffoldingType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map.Entry;

@EventBusSubscriber(modid = ImmersiveEngineering.MODID, value = {Dist.CLIENT}, bus = Bus.MOD)
public class BlockRenderLayers
{
	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent ev)
	{
		RenderTypeLookup.setRenderLayer(StoneDecoration.insulatingGlass, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(StoneDecoration.concreteSprayed, RenderType.getCutout());
		for(Block b : IEContent.registeredIEBlocks)
			if(b instanceof ConnectorBlock)
				RenderTypeLookup.setRenderLayer(
						b, rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()
				);
		for(MetalScaffoldingType type : MetalScaffoldingType.values())
		{
			RenderTypeLookup.setRenderLayer(MetalDecoration.steelScaffoldingStair.get(type), RenderType.getCutout());
			RenderTypeLookup.setRenderLayer(MetalDecoration.aluScaffoldingStair.get(type), RenderType.getCutout());
			RenderTypeLookup.setRenderLayer(MetalDecoration.aluScaffolding.get(type), RenderType.getCutout());
			RenderTypeLookup.setRenderLayer(MetalDecoration.steelScaffolding.get(type), RenderType.getCutout());
		}
		RenderTypeLookup.setRenderLayer(WoodenDecoration.treatedScaffolding, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(
				Connectors.ENERGY_CONNECTORS.get(Pair.of(WireType.HV_CATEGORY, true)),
				rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()
		);
		RenderTypeLookup.setRenderLayer(
				Connectors.connectorBundled,
				rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()
		);
		RenderTypeLookup.setRenderLayer(
				Connectors.connectorProbe,
				rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()||rt==RenderType.getCutout()
		);
		RenderTypeLookup.setRenderLayer(
				Connectors.feedthrough,
				rt -> true
		);
		RenderTypeLookup.setRenderLayer(MetalDevices.fluidPlacer, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDevices.furnaceHeater, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDevices.fluidPipe, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDevices.cloche, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDevices.sampleDrill, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDecoration.slopeAlu, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(MetalDecoration.slopeSteel, RenderType.getCutout());
		for(Block b : MetalDevices.CONVEYORS.values())
			RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(
				MetalDevices.chargingStation,
				rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()
		);
		RenderTypeLookup.setRenderLayer(Multiblocks.tank, RenderType.getCutoutMipped());

		for(Entry<Block, SlabBlock> slab : IEBlocks.toSlab.entrySet())
			RenderTypeLookup.setRenderLayer(
					slab.getValue(),
					rt -> RenderTypeLookup.canRenderInLayer(slab.getKey().getDefaultState(), rt)
			);
		RenderTypeLookup.setRenderLayer(
				Cloth.balloon,
				rt -> rt==RenderType.getSolid()||rt==RenderType.getTranslucent()
		);

		RenderTypeLookup.setRenderLayer(IEContent.fluidPotion, RenderType.getTranslucent());
	}
}