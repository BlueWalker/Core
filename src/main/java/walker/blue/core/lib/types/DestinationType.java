package walker.blue.core.lib.types;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DestinationType {
    ROOM(false),
    BATHROOM(true),
    ELEVATOR(true),
    STAIRS(true);

    private static final String[] TYPES_ARRAY = new String[] { BATHROOM.name(),
            ELEVATOR.name(),
            ROOM.name(),
            STAIRS.name() };
    public static final Set<String> TYPES = new HashSet<>(Arrays.asList(TYPES_ARRAY));

    private boolean generic;

    private DestinationType(final boolean generic) {
        this.generic = generic;
    }

    public boolean isGeneric() {
        return this.generic;
    }

    public static boolean isType(final String name) {
        return TYPES.contains(name);
    }
}