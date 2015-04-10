package walker.blue.core.lib.speech;

/**
 * Enum that represents the different node events
 */
public enum NodeEvent {
    REACHING_DESTINATION("reaching destionation to the %s in %f meters"),
    REACHING_DESTINATION_AHEAD("reaching destionation ahead in %f meters"),
    TURN("turn %s in %f meters");

    private String format;

    NodeEvent(final String format) {
        this.format = format;
    }

    public String getFormat() {
        return this.format;
    }
}
