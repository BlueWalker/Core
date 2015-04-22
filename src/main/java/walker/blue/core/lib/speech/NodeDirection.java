package walker.blue.core.lib.speech;

/**
 * Enum that represents the different directions allowed at a noce
 */
public enum NodeDirection {
    RIGHT("right"),
    LEFT("left"),
    AHEAD("ahead"),
    BEHIND("behind");

    private String msg;

    NodeDirection(final String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }
}
