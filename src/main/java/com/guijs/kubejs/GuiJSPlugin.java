package com.guijs.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import com.guijs.kubejs.event.GuiJSEvents;

/**
 * KubeJS plugin entrypoint. Registered via {@code kubejs.plugins.txt}
 * (loaded automatically by KubeJS when GUIJS is present).
 */
public class GuiJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        GuiJSEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("GuiJS", new GuiJSBinding());
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // Allow the GUIJS binding classes to be used from scripts.
        filter.allow("com.guijs.**");
    }
}
