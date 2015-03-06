package walker.blue.core.lib.indicator;

import android.graphics.Paint;

/**
 * Factory class to build paint objects for the indicator view
 */
public class PaintFactory {

    /**
     * private constructor
     */
    private PaintFactory() {};

    /**
     * Builds the paint for the line using the given color
     *
     * @param color Color of the line
     * @return Paint class for the line
     */
    public static Paint buildLinePaint(final int color) {
        final Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(color);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(15);
        return linePaint;
    }

    /**
     * Builds the paint class for the point using the given color
     *
     * @param color Color of the point
     * @return Paint class for the point
     */
    public static Paint buildPointPaint(final int color) {
        final Paint pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setColor(color);
        pointPaint.setAntiAlias(true);
        return pointPaint;
    }
}
