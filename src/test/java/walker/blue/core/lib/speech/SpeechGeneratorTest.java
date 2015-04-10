package walker.blue.core.lib.speech;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import walker.blue.path.lib.GridNode;

/**
 * Unit tests for the SpeechGenerator class
 */
public class SpeechGeneratorTest {

    private static List<GridNode> path;
    private static SpeechGenerator speechGenerator;

    @BeforeClass
    public static void classSetup() {
        path = new ArrayList<>();
        final GridNode node0 = new GridNode(0, 0, 0, true);
        final GridNode node1 = new GridNode(0, 10, 0, true);
        final GridNode node2 = new GridNode(7, 11, 0, true);
        final GridNode node3 = new GridNode(7, 15, 0, true);
        final GridNode node4 = new GridNode(2, 15, 0, true);
        final GridNode node5 = new GridNode(2, 16, 0, false);
        path.add(node0);
        path.add(node1);
        path.add(node2);
        path.add(node3);
        path.add(node4);
        path.add(node5);
        speechGenerator = new SpeechGenerator(path);
    }

    @Test
    public void testGetSpeechForNodes0and1() {
        final GeneratedSpeech generatedSpeech = speechGenerator.getSpeechForNodes(path.get(0), path.get(1));
        Assert.assertEquals(10.0f, generatedSpeech.getDistance(), .001);
        Assert.assertEquals(NodeEvent.TURN, generatedSpeech.getEvent());
        Assert.assertEquals(NodeDirection.RIGHT, generatedSpeech.getDirection());
        System.out.println(generatedSpeech.toString());
    }

    @Test
    public void testGetSpeechForNodes1and2() {
        final GeneratedSpeech generatedSpeech = speechGenerator.getSpeechForNodes(path.get(1), path.get(2));
        Assert.assertEquals(Math.sqrt(50), generatedSpeech.getDistance(), .001);
        Assert.assertEquals(NodeEvent.TURN, generatedSpeech.getEvent());
        Assert.assertEquals(NodeDirection.LEFT, generatedSpeech.getDirection());
        System.out.println(generatedSpeech.toString());
    }

    @Test
    public void testGetSpeechForNodes3and4() {
        final GeneratedSpeech generatedSpeech = speechGenerator.getSpeechForNodes(path.get(3), path.get(4));
        Assert.assertEquals(5.0f, generatedSpeech.getDistance(), .001);
        Assert.assertEquals(NodeEvent.REACHING_DESTINATION, generatedSpeech.getEvent());
        Assert.assertEquals(NodeDirection.RIGHT, generatedSpeech.getDirection());
        System.out.println(generatedSpeech.toString());
    }
}
