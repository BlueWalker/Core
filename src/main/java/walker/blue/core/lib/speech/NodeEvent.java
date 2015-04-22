package walker.blue.core.lib.speech;

/**
 * Enum that represents the different node events
 */
public enum NodeEvent {
    REACHING_DESTINATION("reaching destination to the %s in %.1f meters", "reached destination on the %s"),
    REACHING_DESTINATION_AHEAD("reaching destination ahead in %.1f meters", "reached destination %s"),
    TURN("turn %s in %.1f meters", "turn %s now");

    private String format;
    private String actionFormat;

    NodeEvent(final String format, final String actionFormat) {
        this.format = format;
        this.actionFormat = actionFormat;
    }

    public String getFormat() {
        return this.format;
    }

    public String getActionFormat() {
        return this.actionFormat;
    }
}
