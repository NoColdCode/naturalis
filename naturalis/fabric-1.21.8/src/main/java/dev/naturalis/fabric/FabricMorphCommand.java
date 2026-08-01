package dev.naturalis.fabric;

import dev.naturalis.command.MorphCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class FabricMorphCommand {

    private FabricMorphCommand() {
    }

    public static void register() {
        // Fabric command-api v2: second parameter is CommandBuildContext (not registry + enabledFeatures like vanilla Commands ctor).
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) ->
            MorphCommand.register(dispatcher, commandBuildContext));
    }
}
