package walker.blue.core.lib.input;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import walker.blue.core.lib.types.DestinationType;

/**
 * Tests for the UserInputParser
 */
public class UserInputParserTest {

    private static final String INPUT_NO_NUMS = "there are no numbers here :(";
    private static final String INPUT_EMPTY = "";
    private static final String INPUT_NUMS = "stairs 117";
    private static final String INPUT_NUMS_2 = "Elevator bathROOM14**! @#$%^&*( 117 2133";
    private static final String NUM_117 = "117";
    private static final String NUM_14 = "14";
    private static final String NUM_2133 = "2133";

    private List<String> input;
    private Set<String> expected;

    @Before
    public void setup() {
        this.input = new ArrayList<>();
        this.expected = new HashSet<>();
    }

    @After
    public void cleanup() {
        this.input = null;
        this.expected = null;
    }

    @Test
    public void testGetNumbers() {
        this.expected.add(NUM_117);
        this.expected.add(NUM_14);
        this.expected.add(NUM_2133);

        this.input.add(INPUT_NUMS);
        this.input.add(INPUT_NUMS_2);

        final UserInputParser parser = new UserInputParser(this.input);
        Assert.assertEquals(expected, parser.getNumbers());
    }

    @Test
    public void testGetNumbersNoNumbers() {
        this.input.add(INPUT_NO_NUMS);
        this.input.add(INPUT_EMPTY);
        final UserInputParser parser = new UserInputParser(this.input);
        Assert.assertEquals(this.expected, parser.getNumbers());
    }

    @Test
    public void testGetNumbersEmptyInput() {
        final UserInputParser parser = new UserInputParser(this.input);
        Assert.assertEquals(this.expected, parser.getNumbers());
    }

    @Test
    public void testGetKeyWords() {
        this.expected.add(DestinationType.BATHROOM.name());
        this.expected.add(DestinationType.ELEVATOR.name());
        this.expected.add(DestinationType.STAIRS.name());

        this.input.add(INPUT_NUMS);
        this.input.add(INPUT_NUMS_2);

        final UserInputParser parser = new UserInputParser(this.input);
        Assert.assertEquals(this.expected, parser.getKeywords());
    }

    @Test
    public void testGetKeyWordsEmptyInput() {
        final UserInputParser parser = new UserInputParser(this.input);
        Assert.assertEquals(this.expected, parser.getKeywords());
    }
}