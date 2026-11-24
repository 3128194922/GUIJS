package com.guijs.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link ContainerLevelAccess} that can be used to open "floating" GUIs
 * without a real backing block.
 *
 * <p>Vanilla menus validate their backing block inside {@code stillValid(...)}
 * via {@code access.evaluate((level, pos) -> level.getBlockState(pos).is(block) ...)}.
 * {@link ContainerLevelAccess#NULL} would fail that check and close the menu on
 * the first tick, so this access force-fails boolean (validation) queries while
 * still executing non-boolean queries (bookshelf power, rename context, ...)
 * against the real level, keeping those features functional.</p>
 */
public class OpenAccess implements ContainerLevelAccess {
    private final Level level;
    private final BlockPos pos;
    private final boolean forceValid;

    public OpenAccess(Level level, BlockPos pos, boolean forceValid) {
        this.level = level;
        this.pos = pos;
        this.forceValid = forceValid;
    }

    /** Validation queries (boolean results) always pass; other queries run for real. */
    @Override
    public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> func) {
        try {
            T result = func.apply(level, pos);
            if (forceValid && result instanceof Boolean) {
                return Optional.of(castTrue());
            }
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return forceValid ? Optional.of(castTrue()) : Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T castTrue() {
        return (T) Boolean.TRUE;
    }
}
