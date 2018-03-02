package de.iteratec.slab.segway.remote.robot.service;

import android.content.Context;
import android.util.Log;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.voice.Recognizer;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.grammar.GrammarConstraint;
import com.segway.robot.sdk.voice.grammar.Slot;
import com.segway.robot.sdk.voice.recognition.RecognitionListener;
import com.segway.robot.sdk.voice.recognition.RecognitionResult;
import com.segway.robot.sdk.voice.recognition.WakeupListener;
import com.segway.robot.sdk.voice.recognition.WakeupResult;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by mss on 22.12.17.
 */

public class LoomoRecognitionService {

    private static final String TAG = "LoomoRecognitionService";
    public static final float MOVE_DELTA = 0.5f;
    private final Context context;

    private Recognizer recognizer;

    public static LoomoRecognitionService instance;

    public static LoomoRecognitionService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LoomoRecognitionService instance not initialized yet");
        }
        return instance;
    }

    public LoomoRecognitionService(Context context) {
        this.context = context;
        init();
        instance = this;
    }

    public void restartService() {
        init();
    }

    private void init() {
        this.recognizer = Recognizer.getInstance();
        this.recognizer.bindService(this.context, new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "Recognize service bind successfully");
                try {
                    recognizer.addGrammarConstraint(getSniJokeContraint());
                    recognizer.addGrammarConstraint(getMoveConstraint());

                    // listen by default and toggle it off via settings
                    LoomoRecognitionService.getInstance().startListening();
                } catch (VoiceException e) {
                    Log.e(TAG, "Could not add sni constraint", e);
                }
            }

            @Override
            public void onUnbind(String reason) {
                Log.d(TAG, "Recognize service unbound");
            }
        });
    }


    private static final GrammarConstraint getSniJokeContraint() {
        GrammarConstraint constraint = new GrammarConstraint();
        constraint.setName("sniJoke");

        constraint.addSlot(new Slot("joke", false, Arrays.asList("sni", "joke", "sebastian")));

        return constraint;
    }

    private static final GrammarConstraint getMoveConstraint() {
        GrammarConstraint constraint = new GrammarConstraint();
        constraint.setName("move");

        constraint.addSlot(new Slot("move", false, Arrays.asList("move", "turn", "walk")));
        constraint.addSlot(new Slot("direction", false, Arrays.asList("right", "left", "backward", "forward", "reset")));

        return constraint;
    }

    public void startListening() {
        Log.d(TAG, "startListening");
        try {
            recognizer.startWakeupAndRecognition(new RobotWakeupListener(), new RobotRecognitionListener());
        } catch (VoiceException e) {
            Log.e(TAG, "Got VoiceException", e);
        }
    }

    private class RobotWakeupListener implements WakeupListener {
        @Override
        public void onStandby() {
            Log.i(TAG, "In Standby");
        }

        @Override
        public void onWakeupResult(WakeupResult wakeupResult) {
            Log.i(TAG, "Got wakeupresult: " + wakeupResult);

        }

        @Override
        public void onWakeupError(String error) {
            Log.i(TAG, "Got wakeup error: " + error);
        }
    }

    private class RobotRecognitionListener implements RecognitionListener {
        @Override
        public void onRecognitionStart() {
            Log.i(TAG, "started recognition");
        }

        @Override
        public boolean onRecognitionResult(RecognitionResult recognitionResult) {
            Log.i(TAG, "recognition result: " + recognitionResult);
            if (isCommand(recognitionResult.getRecognitionResult(), jokeCommandList)) {
                String joke = jokes[new Random().nextInt(jokes.length)];
                LoomoSpeakService.getInstance().speak(joke);
                return true;
            } else if (isCommand(recognitionResult.getRecognitionResult(), moveCommandList)) {
                LoomoBaseService loomoBaseService = LoomoBaseService.getInstance();

                if (recognitionResult.getRecognitionResult().contains("left")) {
                    loomoBaseService.moveToCoordinate(loomoBaseService.getLastXPosition(), loomoBaseService.getLastYPosition() + MOVE_DELTA);
                } else if (recognitionResult.getRecognitionResult().contains("right")) {
                    loomoBaseService.moveToCoordinate(loomoBaseService.getLastXPosition(), loomoBaseService.getLastYPosition() - MOVE_DELTA);
                } else if (recognitionResult.getRecognitionResult().contains("backward")) {
                    loomoBaseService.moveToCoordinate(loomoBaseService.getLastXPosition() - MOVE_DELTA, loomoBaseService.getLastYPosition());

                } else if (recognitionResult.getRecognitionResult().contains("forward")) {
                    loomoBaseService.moveToCoordinate(loomoBaseService.getLastXPosition() + MOVE_DELTA, loomoBaseService.getLastYPosition());

                } else if (recognitionResult.getRecognitionResult().contains("reset")) {
                    loomoBaseService.resetPosition();

                }
            }
            return false;
        }

        @Override
        public boolean onRecognitionError(String error) {
            Log.i(TAG, "recognition error: " + error);
            return false;
        }
    }

    private static boolean isCommand(String recognition, List<String> commandList) {
        for (String value : commandList) {
            if (recognition.toLowerCase().contains(value.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private static final List<String> jokeCommandList = Arrays.asList("sni", "joke", "sebastian");
    private static final List<String> moveCommandList = Arrays.asList("move", "turn", "walk");

    private static final String[] jokes = {
            "There is only one real sni.",
            "Do you like snibowitz?",
            "Go back to Sniederlands",
            "There is no sni on Freiday",
    };

    public void stopListening() {
        Log.d(TAG, "stopListening");
        try {
            recognizer.stopRecognition();
        } catch (VoiceException e) {
            Log.e(TAG, "got VoiceException", e);
        }

    }

    public void disconnect() {
        this.recognizer.unbindService();
    }
}
