package walker.blue.core.lib.input;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import walker.blue.core.lib.types.DestinationTable;
import walker.blue.core.lib.types.DestinationType;
import walker.blue.path.lib.GridNode;

/**
 * Class in charge of parsing user input
 */
public class UserInputParser {

    /**
     * Regex used to remove all characters that arent a number from a string
     */
    private static final String EXCLUDE_NUMS_REGEX = "[^0-9]+";
    /**
     * Regex used to remove all characters that arent a uppercase letter from
     * a string
     */
    private static final String EXCLUDE_LETTERS_REGEX = "[^A-Z]+";
    /**
     * Separator used when removing sections of a string
     */
    private static final String SEPARATOR = " ";
    /**
     * Array of keywords
     */
    public static final String[] KEYWORDS_ARRAY = new String[] { DestinationType.BATHROOM.name(),
                                                                 DestinationType.ELEVATOR.name(),
                                                                 DestinationType.ROOM.name(),
                                                                 DestinationType.STAIRS.name() };
    /**
     * Set of keywords
     */
    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(KEYWORDS_ARRAY));

    /**
     * Stores the raw user input
     */
    private List<String> rawInput;

    /**
     * Constructor. Sets the rawInput field to the given List of Strings
     *
     * @param rawInput List of strings containing the users input
     */
    public UserInputParser(final List<String> rawInput) {
        this.rawInput = rawInput;
    }

    /**
     * Extracts all instances of numbers in the user input
     *
     * @return Set of strings containing the all numbers found in the user
     *         input
     */
    public Set<String> getNumbers() {
        return this.getNumbers(null);
    }

    public Set<String> getFilteredNumbers(final DestinationTable table, final DestinationType type) {
        if (type.isGeneric()) {
            return null;
        }
        final Map<String, GridNode> map = (Map<String, GridNode>) table.getImmediateValue(type);
        return this.getNumbers(map);
    }

    /**
     *  Extracts the keywords that appear in the user input
     *
     * @return Set of strings containing the all keywords found in the user
     *         input
     */
    public Set<String> getKeywords() {
        final Set<String> result = new HashSet<>();
        for (final String str : this.rawInput) {
            final String[] words = this.extractWordsFromString(str);
            for(final String word : words) {
                if (KEYWORDS.contains(word)) {
                    result.add(word);
                }
            }
        }
        return result;
    }

    private Set<String> getNumbers(final Map<String, GridNode> filter) {
        final Set<String> result = new HashSet<>();
        for (final String str : this.rawInput) {
            final String[] nums = this.extractNumbersFromString(str);
            for(final String num : nums) {
                if (num.length() > 0 && (filter == null || filter.containsKey(num))) {
                    result.add(num);
                }
            }
        }
        return result;
    }

    /**
     * Gets all numbers in the given string
     *
     * @param str String that will be parser
     * @return Array of Strings containing all numbers found
     */
    private String[] extractNumbersFromString(final String str) {
        final String numOnlyStr = str.replaceAll(EXCLUDE_NUMS_REGEX, SEPARATOR);
        return numOnlyStr.trim().split(SEPARATOR);
    }

    /**
     * Gets all words in the given string
     *
     * @param str String that will be parser
     * @return Array of Strings containing all words found
     */
    private String[] extractWordsFromString(final String str) {
        final String cleanStr = str.toUpperCase().replaceAll(EXCLUDE_LETTERS_REGEX, SEPARATOR);
        return cleanStr.trim().split(SEPARATOR);
    }
}