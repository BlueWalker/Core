package walker.blue.core.lib.main;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;

import walker.blue.core.lib.speech.GeneratedSpeech;
import walker.blue.core.lib.speech.SpeechGenerator;
import walker.blue.core.lib.types.Building;
import walker.blue.core.lib.user.UserTracker;
import walker.blue.path.lib.node.GridNode;
import walker.blue.path.lib.node.RectCoordinates;

/**
 * Runnable in charge of submitting what will be said to the user throughout
 * the main loop
 */
public class SpeechSubmitRunnable implements Runnable {

    /**
     * Tracker currently tracking the user
     */
    private UserTracker userTracker;
    /**
     * Building the user is currently in
     */
    private Building building;
    /**
     * TextToSpeech object used to speak to the user
     */
    private TextToSpeech textToSpeech;
    /**
     * SpeechGenerator in charge of generating what will be said to the user
     */
    private SpeechGenerator speechGenerator;

    /**
     * Constructor. Sets fields using the given values
     *
     * @param userTracker  Tracker currently tracking the user
     * @param building Building the user is currently in
     * @param path The generated path for the user
     * @param context Context the main loop is currently being run in
     */
    public SpeechSubmitRunnable(final UserTracker userTracker,
                                final Building building,
                                final List<GridNode> path,
                                final Context context) {
        this.userTracker = userTracker;
        this.building = building;
        this.speechGenerator = new SpeechGenerator(path);
        this.textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {}
        });
    }

    @Override
    public void run() {
        final RectCoordinates userCoordinates = this.userTracker.getLatestLocation();
        if (userCoordinates != null) {
            final GeneratedSpeech speech =
                    this.speechGenerator.getSpeechForNodes(userCoordinates, this.userTracker.getNextNode());
            Log.d(this.getClass().getName(), "Submitting the following string: " + speech.toString());
            this.textToSpeech.speak(speech.toString(), TextToSpeech.QUEUE_FLUSH, null);
        }
    }
}
