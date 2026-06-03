package farvix.solution.api.util.render;

import meteordevelopment.orbit.EventHandler;
import farvix.solution.api.events.impl.render.EventRender3D;

public class RenderListener {

    @EventHandler
    public void onRender3D(EventRender3D e) {
        RenderUtility.onRender3D(e);
    }
}
