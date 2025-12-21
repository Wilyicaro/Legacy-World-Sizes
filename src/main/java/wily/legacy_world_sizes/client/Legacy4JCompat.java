package wily.legacy_world_sizes.client;

import wily.legacy.client.screen.LegacyConfigWidgets;
import wily.legacy.client.screen.OptionsScreen;
import wily.legacy_world_sizes.LegacyWorldSizesClient;
import wily.legacy_world_sizes.util.LWSComponents;

public class Legacy4JCompat {
    public static void init() {
        OptionsScreen.Section.ADVANCED_GAME_OPTIONS.elements().add(o -> {
            o.getRenderableVList().addCategory(LWSComponents.OPTIONS_TITLE);
            LegacyWorldSizesClient.DISPLAYED_CONFIGS.forEach(c -> o.getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c)));
        });
    }
}
