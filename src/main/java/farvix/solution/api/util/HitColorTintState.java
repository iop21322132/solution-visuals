package farvix.solution.api.util;

/**
 * Thread-local flag: set to true during LivingEntityRenderer.render()
 * when the entity is hurt and HitColor module is active.
 * Cleared at RETURN of the same render call.
 */
public final class HitColorTintState {
    private HitColorTintState() {}

    public static final ThreadLocal<Boolean> SHOULD_TINT =
            ThreadLocal.withInitial(() -> false);
}
