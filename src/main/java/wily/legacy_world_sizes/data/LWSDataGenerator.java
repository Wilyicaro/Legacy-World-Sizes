package wily.legacy_world_sizes.data;

//? if neoforge {
/*import wily.legacy_world_sizes.LegacyWorldSizes;
import net.neoforged.bus.api.SubscribeEvent;
//? if >=1.20.6 {
import net.neoforged.fml.common.EventBusSubscriber;
//?} else {
/^import net.neoforged.fml.common.Mod.EventBusSubscriber;
^///?}
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataGenerator;

import java.io.IOException;
*///?} else if fabric {
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
//?}

//? if forge {
/*import wily.legacy_world_sizes.LegacyWorldSizes;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
*///?}

//? if forge || neoforge {
/*@EventBusSubscriber(modid = LegacyWorldSizes.MOD_ID/^? if !(neoforge && >=1.21.6) {^/, bus = EventBusSubscriber.Bus.MOD/^?}^/)
*///?}
public class LWSDataGenerator {
	//? if fabric {
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
	}
	//?} else if neoforge || forge {
	/*@SubscribeEvent
	public static void event(GatherDataEvent event) throws IOException {
		DataGenerator generator = event.getGenerator();
	}
	*///?}
}