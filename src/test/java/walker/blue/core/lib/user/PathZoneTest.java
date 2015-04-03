package walker.blue.core.lib.user;

import org.junit.Test;

import walker.blue.path.lib.RectCoordinates;

public class PathZoneTest {

    @Test
    public void test() {
        RectCoordinates start = new RectCoordinates(1, 36, 0);
        RectCoordinates end = new RectCoordinates(1, 6, 0);
        PathZone zone = new PathZone(start, end, 3, 3);
    }

    @Test
    public void test1() {
        RectCoordinates start = new RectCoordinates(21, 37, 0);
        RectCoordinates end = new RectCoordinates(1, 36, 0);
        PathZone zone = new PathZone(start, end, 3, 3);
    }
}
