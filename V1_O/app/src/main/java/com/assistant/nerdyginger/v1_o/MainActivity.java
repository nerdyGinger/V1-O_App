package com.assistant.nerdyginger.v1_o;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import ai.api.android.AIConfiguration;
import ai.api.AIListener;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.api.ui.AIButton;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements RecognitionListener, AIListener {
    private SpeechRecognizer recognizer;
    private AIService aiService;
    private Synthesizer mSynth;
    private String KEYPHRASE = "v one oh";
    private String KW_SEARCH = "wakeup";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textBox);

        //Permission check
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        //Setup
        //new SetupTask(this).execute(); //sets up wake-word listening
        if (mSynth == null) {
            mSynth = new Synthesizer(getString(R.string.bingSpeechApiKey));
        }
        mSynth.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
        Voice v = new Voice("en-us", "Microsoft Server Speech Text to Speech Voice (en-US, BenjaminRUS)",
                Voice.Gender.Male, true);
        mSynth.SetVoice(v, null);
        final AIConfiguration config = new AIConfiguration(getString(R.string.dialogflowApiKey),
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        //aiService = AIService.getService(getApplicationContext(), config);
        //aiService.setListener(this);

        AIButton aiButton = findViewById(R.id.mic);
        aiButton.initialize(config);
        aiButton.setResultsListener(new AIButton.AIButtonListener() {
            @Override
            public void onResult(final AIResponse result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Result res = result.getResult();
                        Fulfillment fulfillment = res.getFulfillment();
                        String speech = fulfillment.getSpeech();
                        textView.setText(speech);
                        mSynth.SpeakToAudio(speech);
                        Log.d("HEY_ITS_ME", speech);
                        //aiService.startListening();s
                        //recognizer.startListening(KW_SEARCH, 8000);
                    }
                });
            }

            @Override
            public void onError(final AIError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("HEY_ITS_ME", error.toString());
                    }
                });
            }

            @Override
            public void onCancelled() {        }
        });
}

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;

        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (Exception ex) {
                return ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                Log.e("HEY_ITS_ME", result.toString());
            } else {
                activityReference.get().recognizer.startListening("wakeup");
            }
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .getRecognizer();
        recognizer.addListener(this);
        recognizer.addKeyphraseSearch(KW_SEARCH, KEYPHRASE);
    }

    //RecognizerListener Overrides -----------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            //initiate aiListener
            aiService.startListening();
            recognizer.stop();
        }
    }

    @Override
    public void onError(Exception error) {
        Log.e("HEY_ITS_ME", error.toString());
    }

    @Override
    public void onResult(Hypothesis hypothesis) {    }

    @Override
    public void onBeginningOfSpeech() {     }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KW_SEARCH)) {
            recognizer.startListening(KW_SEARCH);
        }
    }

    @Override
    public void onTimeout() {   }

    //AIListener Overrides -------------------------------------------------------------------------
    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Result result = response.getResult();
                Fulfillment fulfillment = result.getFulfillment();
                String speech = fulfillment.getSpeech();
                textView.setText(speech);
                mSynth.SpeakToAudio(speech);
                Log.d("HEY_ITS_ME", speech);
                //aiService.startListening();
				recognizer.startListening(KW_SEARCH, 8000);
            }
        });
    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("HEY_ITS_ME", error.toString());
            }
        });
        recognizer.startListening(KW_SEARCH);
    }

    @Override
    public void onListeningCanceled() {     }

    @Override
    public void onListeningStarted() {     }

    @Override
    public void onListeningFinished() {     }

    @Override
    public void onAudioLevel(final float level) {    }
}
