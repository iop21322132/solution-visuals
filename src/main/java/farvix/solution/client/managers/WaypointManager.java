package farvix.solution.client.managers;

import lombok.Getter;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WaypointManager {

    @Getter
    public static final class Waypoint {
        private final String name;
        private final double x, y, z;

        public Waypoint(String name, double x, double y, double z) {
            this.name = name;
            this.x    = x;
            this.y    = y;
            this.z    = z;
        }

        public Vec3d getPos() { return new Vec3d(x, y, z); }
    }

    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    public static synchronized void add(String name, double x, double y, double z) {
        WAYPOINTS.add(new Waypoint(name, x, y, z));
    }

    public static synchronized boolean remove(int index) {
        if (index >= 0 && index < WAYPOINTS.size()) {
            WAYPOINTS.remove(index);
            return true;
        }
        return false;
    }

    public static synchronized void clear() { WAYPOINTS.clear(); }

    public static synchronized List<Waypoint> list() {
        return Collections.unmodifiableList(new ArrayList<>(WAYPOINTS));
    }
}
