package walker.blue.core.lib.indicator;

import android.graphics.Color;

/**
 * Themes for the indicator view
 */
public enum IndicatorTheme {
    LIGHT(Color.WHITE, Color.RED),
    DARK(Color.BLACK, Color.RED),
    GRAYSCALE_LIGHT(Color.WHITE, Color.LTGRAY),
    GRAYSCALE_DARK(Color.BLACK, Color.GRAY);

    /**
     * Color of the line
     */
    private int lineColor;
    /**
     * Color of the point
     */
    private int pointColor;

    /**
     * Constructor sets the line and point color to the given colors
     *
     * @param lineColor color for the line
     * @param pointColor color for the point
     */
    private IndicatorTheme(final int lineColor,final int pointColor) {
        this.lineColor = lineColor;
        this.pointColor = pointColor;
    }

    /**
     * Returns the line color
     *
     * @return color of the line
     */
    public int getLineColor() {
        return this.lineColor;
    }

    /**
     * Returns the point color
     *
     * @return color of the point
     */
    public int getPointColor() {
        return this.pointColor;
    }
}
