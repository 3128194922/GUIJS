package com.guijs.gui.custom;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime registry of GUI descriptions defined in startup scripts
 * ({@code GuiJS.createGui}) and block-to-GUI bindings
 * ({@code GuiJS.bindBlock}).
 */
public final class GuiRegistry {
    private GuiRegistry() {
    }

    private static final Map<String, GuiUIDescription> GUIS = new HashMap<>();
    private static final Map<String, String> BLOCK_TO_GUI = new HashMap<>();

    public static void register(String guiId, GuiUIDescription description) {
        GUIS.put(guiId, description);
    }

    public static void bindBlock(String blockId, String guiId) {
        BLOCK_TO_GUI.put(blockId, guiId);
    }

    @Nullable
    public static GuiUIDescription get(String guiId) {
        return GUIS.get(guiId);
    }

    @Nullable
    public static String guiForBlock(String blockId) {
        return BLOCK_TO_GUI.get(blockId);
    }

    public static boolean exists(String guiId) {
        return GUIS.containsKey(guiId);
    }
}
