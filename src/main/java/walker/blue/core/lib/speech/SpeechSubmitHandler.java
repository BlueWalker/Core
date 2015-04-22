package walker.blue.core.lib.speech;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

/**
 * Runnable in charge of submitting what will be said to the user throughout
 * the main loop
 */
public class SpeechSubmitHandler {

    private static final String WARN_USER_STRING = "please return to the route";
    /**
     * TextToSpeech object used to speak to the user
     */
    private TextToSpeech textToSpeech;
    /**
     * TextView which displays the next instruction
     */
    private TextView nextInstructionText;
    /**
     * Context under which the application is being run
     */
    private Context context;

    /**
     * Constructor. Sets fields using the given values
     *
     * @param textToSpeech object used to submit speech to thr user
     */
    public SpeechSubmitHandler(final TextToSpeech textToSpeech) {
        this(textToSpeech, null, null);
    }

    /**
     * Constructor. Sets fields using the given values
     *
     * @param textToSpeech object used to submit speech to thr user
     * @param nextInstructionText  TextView which displays the next instruction
     * @param context Context under which the application is being run
     */
    public SpeechSubmitHandler(final TextToSpeech textToSpeech,
                               final TextView nextInstructionText,
                               final Context context) {
        this.textToSpeech = textToSpeech;
        this.nextInstructionText = nextInstructionText;
        this.context = context;
    }

    /**
     * Warn user to get back on course
     */
    public void warnUser() {
        this.submitString(WARN_USER_STRING, TextToSpeech.QUEUE_FLUSH, false);
    }

    /**
     * Submit the given generated speech as an action
     *
     * @param speech generated speech
     */
    public void submitAction(final GeneratedSpeech speech) {
        this.submitString(speech.toActionString(), TextToSpeech.QUEUE_ADD, false);
    }

    /**
     * Submit the given generated speech with type TextToSpeech.QUEUE_FLUSH
     *
     * @param speech generated speech
     */
    public void submitQueueFlush(final GeneratedSpeech speech) {
        this.submit(speech, TextToSpeech.QUEUE_FLUSH);
    }

    /**
     * Submit the given generated speech with type TextToSpeech.QUEUE_ADD
     *
     * @param speech generated speech
     */
    public void submitQueueAdd(final GeneratedSpeech speech) {
        this.submit(speech, TextToSpeech.QUEUE_ADD);
    }

    /**
     * Submits the given speech using the given type
     *
     * @param speech generated speech
     * @param type submit type
     */
    public void submit(final GeneratedSpeech speech, final int type) {
        this.submitSpeech(speech, type, false);
    }

    /**
     * Submits the given speech without silently (writes to the
     * nextInstructionText if availible)
     *
     * @param speech generated speech
     */
    public void submitSilent(final GeneratedSpeech speech) {
        this.setNextInstructionText(speech.toString());
    }

    /**
     * Submits the given speech using the given type.
     *
     * @param speech generated speech
     * @param type submit type
     * @param write whether the submitted speech should be written to
     *              the nextInstructionText
     */
    public void submitSpeech(final GeneratedSpeech speech, final int type, final boolean write) {
        this.submitString(speech.toString(), type, write);
    }

    /**
     * Submits the given string using the given type.
     *
     * @param speech generated speech
     * @param type submit type
     * @param write whether the submitted speech should be written to
     *              the nextInstructionText
     */
    public void submitString(final String speech, final int type, final boolean write) {
        if (write) {
            this.setNextInstructionText(speech);
        }
        this.textToSpeech.speak(speech, type, null);
    }

    /**
     * Writes the given text to the nextInstructionText if possible
     *
     * @param text text which will be written to nextInstructionText
     */
    private void setNextInstructionText(final String text) {
        if (this.nextInstructionText != null && this.context != null) {
            ((Activity) this.context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    nextInstructionText.setText(text);
                }
            });
        }
    }
}
