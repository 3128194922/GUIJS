package com.guijs.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * Script events for GUIJS.
 *
 * <pre>{@code
 * // server_scripts
 * GUIJSEvents.opened(event => {
 *   console.info(`${event.player.name} opened ${event.menuId}`)
 * })
 * }</pre>
 */
public interface GuiJSEvents {
    EventGroup GROUP = EventGroup.of("GUIJSEvents");

    /** Fired after a GUIJS-opened menu is successfully opened on the server. */
    EventHandler OPENED = GROUP.server("opened", () -> GuiMenuOpenedEventJS.class);

    /** Fired when a button of a custom GuiJS GUI is clicked (server scripts). */
    EventHandler BUTTON = GROUP.server("button", () -> ButtonClickedEventJS.class);
}
