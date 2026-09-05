package wily.legacy_world_sizes.mixin.base.client.compat.legacy4j;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.FactoryConfigWidgets;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.*;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyWorldSize;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;

@Mixin(WorldMoreOptionsScreen.class)
public class WorldMoreOptionsScreenMixin extends PanelVListScreen {
    @Unique
    private static final Gson legacyWorldSizes$gson = new GsonBuilder().setPrettyPrinting().create();

    @Unique
    private boolean legacyWorldSizes$addedOptions;

    @Unique
    private JsonObject legacyWorldSizes$worldOptions;

    @Unique
    private Path legacyWorldSizes$configPath;

    @Unique
    private LegacyWorldSize legacyWorldSizes$appliedSize;

    @Unique
    private LegacyWorldSize legacyWorldSizes$selectedSize;

    @Unique
    private boolean legacyWorldSizes$overwriteEdge;

    @Unique
    private LegacySliderButton<LegacyWorldSize> legacyWorldSizes$sizeSlider;

    @Unique
    private TickBox legacyWorldSizes$overwriteBox;

    public WorldMoreOptionsScreenMixin(Panel.Constructor<PanelVListScreen> panelConstructor, Component component) {
        super(panelConstructor, component);
    }

    @Inject(method = "renderableVListInit", at = @At("HEAD"), remap = false)
    private void init(CallbackInfo ci) {
        if (legacyWorldSizes$addedOptions) return;
        legacyWorldSizes$addedOptions = true;
        if (parent instanceof LoadSaveScreen screen) {
            legacyWorldSizes$addWorldSizeUpgrade(screen);
            return;
        }
        if (!(parent instanceof CreateWorldScreen)) return;
        int index = Math.min(3, renderableVList.renderables.size());
        if (isLegacySettingsMenusEnabled()) {
            if (renderableVList.renderables.size() > 5) {
                renderableVList.renderables.remove(5);
            }
            renderableVList.renderables.add(3, LegacyConfigWidgets.createWidget(LWSWorldOptions.balancedSeed, 0, 0, 200, LWSWorldOptions.balancedSeed::setDefault));
            addLabeledConfigSlider(6, LWSWorldOptions.legacyBiomeScale);
            addLabeledConfigSlider(8, LWSWorldOptions.legacyWorldSize);
            return;
        }
        renderableVList.renderables.add(index++, LegacyConfigWidgets.createWidget(LWSWorldOptions.balancedSeed, 0, 0, 200, LWSWorldOptions.balancedSeed::setDefault));
        renderableVList.renderables.add(index++, SimpleLayoutRenderable.create(0, 9, r -> ((GuiGraphicsExtractor, i, j, f) -> {})));
        addLabeledConfigSlider(index++, LWSWorldOptions.legacyBiomeScale);
        addLabeledConfigSlider(index + 1, LWSWorldOptions.legacyWorldSize);
    }

    @Unique
    private <T> void addLabeledConfigSlider(int index, FactoryConfig<T> config) {
        FactoryConfigControl.FromInt<T> c = (FactoryConfigControl.FromInt<T>) config.control();
        Function<T, Tooltip> tooltipFunction = (v) -> FactoryConfigWidgets.getCachedTooltip(config.getDisplay().tooltip().apply(v));
        renderableVList.renderables.add(index, LegacySliderButton.createFromInt(0, 0, 200, 16, (s) -> config.getDisplay().valueToComponent().apply(s.getObjectValue()), (s) -> tooltipFunction.apply(s.getObjectValue()), config.get(), c.valueGetter(), c.valueSetter(), c.valuesSize(), (s) -> FactoryConfig.saveOptionAndConsume(config, s.getObjectValue(), config::setDefault), config));
        renderableVList.renderables.add(index, new RenderableVList.LayoutText(config.getDisplay().name(), CommonColor.GRAY_TEXT, () -> LegacyOptions.getUIMode().isSD() ? 9 : 13));
    }

    @Unique
    private void legacyWorldSizes$addWorldSizeUpgrade(LoadSaveScreen screen) {
        Path world = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(screen.summary.getLevelId());
        legacyWorldSizes$configPath = DimensionType.getStorageFolder(Level.OVERWORLD, world).resolve("config/legacy_world_sizes.json");
        if (Files.exists(legacyWorldSizes$configPath.resolveSibling("legacy_world_sizes_upgrade.json"))) {
            boolean sd = LegacyOptions.getUIMode().isSD();
            var message = Component.translatable("legacy_world_sizes.upgrade.pending");
            if (sd) message.withStyle(style -> style.withFont(LegacyFontUtil.MOJANGLES_11_FONT));
            renderableVList.renderables.add(0, new AdvancedTextWidget(accessor).lineSpacing(sd ? 8 : 12).withLines(message, panel.width - 20).withColor(CommonColor.GRAY_TEXT.get()).withShadow(false));
            return;
        }
        Path source = Files.isRegularFile(legacyWorldSizes$configPath) ? legacyWorldSizes$configPath : world.resolve("config/legacy_world_sizes.json");
        if (!Files.isRegularFile(source)) return;

        boolean savedOverwrite;
        try {
            legacyWorldSizes$worldOptions = legacyWorldSizes$readWorldOptions(source);
            legacyWorldSizes$selectedSize = legacyWorldSizes$readWorldSize(LWSWorldOptions.legacyWorldSize.getKey());
            legacyWorldSizes$appliedSize = legacyWorldSizes$worldOptions.has(LWSWorldOptions.appliedLegacyWorldSize.getKey()) ? legacyWorldSizes$readWorldSize(LWSWorldOptions.appliedLegacyWorldSize.getKey()) : legacyWorldSizes$selectedSize;
            JsonElement value = legacyWorldSizes$worldOptions.get(LWSWorldOptions.overwriteWorldEdge.getKey());
            savedOverwrite = value != null && Codec.BOOL.parse(JsonOps.INSTANCE, value).getOrThrow();
        } catch (IOException | RuntimeException err) {
            LegacyWorldSizes.LOGGER.warn("Cannot edit world size options for {}", screen.summary.getLevelId(), err);
            return;
        }

        List<LegacyWorldSize> sizes = List.of(LegacyWorldSize.CLASSIC, LegacyWorldSize.SMALL, LegacyWorldSize.MEDIUM, LegacyWorldSize.LARGE);
        int min = sizes.indexOf(legacyWorldSizes$appliedSize);
        if (min < 0 || min == sizes.size() - 1) return;
        List<LegacyWorldSize> upgradeSizes = sizes.subList(min, sizes.size());
        if (!upgradeSizes.contains(legacyWorldSizes$selectedSize)) {
            LegacyWorldSizes.LOGGER.warn("Cannot edit invalid world size transition for {}", screen.summary.getLevelId());
            return;
        }

        legacyWorldSizes$overwriteEdge = savedOverwrite && legacyWorldSizes$selectedSize != legacyWorldSizes$appliedSize;
        FactoryConfigDisplay<LegacyWorldSize> display = LWSWorldOptions.legacyWorldSize.getDisplay();
        legacyWorldSizes$sizeSlider = new LegacySliderButton<>(0, 0, 200, 16, s -> display.getMessage(s.getObjectValue()), s -> FactoryConfigWidgets.getCachedTooltip(display.tooltip().apply(s.getObjectValue())), legacyWorldSizes$selectedSize, () -> upgradeSizes, s -> {
            legacyWorldSizes$saveUpgrade(s.getObjectValue(), legacyWorldSizes$overwriteEdge);
            s.setObjectValue(legacyWorldSizes$selectedSize);
            s.updateMessage();
        }, () -> legacyWorldSizes$selectedSize);
        legacyWorldSizes$overwriteBox = new TickBox(0, 0, 200, legacyWorldSizes$overwriteEdge, value -> Component.translatable("legacy_world_sizes.options.overwriteWorldEdge"), value -> null, box -> {
            if (!box.selected) {
                legacyWorldSizes$saveUpgrade(legacyWorldSizes$selectedSize, false);
                return;
            }
            box.selected = false;
            box.updateMessage();
            minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy_world_sizes.options.overwriteWorldEdge"), Component.translatable("legacy_world_sizes.upgrade.overwrite_warning"), confirmation -> {
                if (legacyWorldSizes$saveUpgrade(legacyWorldSizes$selectedSize, true)) confirmation.onClose();
            }));
        }, () -> legacyWorldSizes$overwriteEdge);
        legacyWorldSizes$overwriteBox.active = legacyWorldSizes$selectedSize != legacyWorldSizes$appliedSize;
        renderableVList.renderables.add(0, legacyWorldSizes$sizeSlider);
        renderableVList.renderables.add(1, legacyWorldSizes$overwriteBox);
        if (savedOverwrite && !legacyWorldSizes$overwriteEdge) legacyWorldSizes$saveUpgrade(legacyWorldSizes$selectedSize, false);
    }

    @Unique
    private boolean legacyWorldSizes$saveUpgrade(LegacyWorldSize size, boolean overwrite) {
        overwrite &= size != legacyWorldSizes$appliedSize;
        try {
            JsonObject options = legacyWorldSizes$worldOptions.deepCopy();
            if (!options.has(LWSWorldOptions.appliedLegacyWorldSize.getKey())) options.add(LWSWorldOptions.appliedLegacyWorldSize.getKey(), LegacyWorldSize.CODEC.encodeStart(JsonOps.INSTANCE, legacyWorldSizes$appliedSize).getOrThrow());
            options.add(LWSWorldOptions.legacyWorldSize.getKey(), LegacyWorldSize.CODEC.encodeStart(JsonOps.INSTANCE, size).getOrThrow());
            options.addProperty(LWSWorldOptions.overwriteWorldEdge.getKey(), overwrite);
            legacyWorldSizes$writeWorldOptions(legacyWorldSizes$configPath, options);
            legacyWorldSizes$worldOptions = options;
            legacyWorldSizes$selectedSize = size;
            legacyWorldSizes$overwriteEdge = overwrite;
            legacyWorldSizes$overwriteBox.active = size != legacyWorldSizes$appliedSize;
            legacyWorldSizes$overwriteBox.selected = overwrite;
            legacyWorldSizes$overwriteBox.updateMessage();
            return true;
        } catch (IOException | RuntimeException err) {
            LegacyWorldSizes.LOGGER.warn("Failed to save world size upgrade options to {}", legacyWorldSizes$configPath, err);
            legacyWorldSizes$sizeSlider.active = false;
            legacyWorldSizes$overwriteBox.active = false;
            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.translatable("legacy_world_sizes.upgrade.save_failed"), Component.translatable("legacy_world_sizes.upgrade.save_failed_message")));
            return false;
        }
    }

    @Unique
    private LegacyWorldSize legacyWorldSizes$readWorldSize(String key) {
        JsonElement value = legacyWorldSizes$worldOptions.get(key);
        return value == null ? LegacyWorldSize.CUSTOM : LegacyWorldSize.CODEC.parse(JsonOps.INSTANCE, value).getOrThrow();
    }

    @Unique
    private JsonObject legacyWorldSizes$readWorldOptions(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Unique
    private void legacyWorldSizes$writeWorldOptions(Path path, JsonObject options) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "legacy_world_sizes-", ".json.tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                legacyWorldSizes$gson.toJson(options, writer);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Unique
    private boolean isLegacySettingsMenusEnabled() {
       return LegacyOptions.legacySettingsMenus.get();
    }
}
