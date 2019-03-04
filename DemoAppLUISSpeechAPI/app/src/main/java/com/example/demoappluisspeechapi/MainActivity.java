package com.example.demoappluisspeechapi;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    private Button speechButton;
    private TextView recognizedTextView;

    // Replace below with your own Language Understanding subscription key
    // The intent recognition service calls the required key 'endpoint key'.
    private static final String LanguageUnderstandingSubscriptionKey = "your language understanding key key";
    // Replace below with the deployment region of your Language Understanding application
    private static final String LanguageUnderstandingServiceRegion = "westus";
    // Replace below with the application ID of your Language Understanding application
    private static final String LanguageUnderstandingAppId = "you language understanding app id";

    // Replace below with your own subscription key
    private static final String SpeechSubscriptionKey = "your speech key";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "westus";

    // Arjun Added
    private Synthesizer m_syn;

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (m_syn == null) {
            // Create Text To Speech Synthesizer.
            m_syn = new Synthesizer(SpeechSubscriptionKey);
        }

        m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
        Voice v = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
        //Voice v = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, HuihuiRUS)", Voice.Gender.Female, true);
        m_syn.SetVoice(v, null);
        m_syn.SpeakToAudio("Arjun testing");

        speechButton = findViewById(R.id.speechButton);
        recognizedTextView =findViewById(R.id.recognizedText);

        ///////////////////////////////////////////////////
        // recognize speech intent
        ///////////////////////////////////////////////////
        speechButton.setOnClickListener(view -> {
            final String logTag = "intent";
            final ArrayList<String> content = new ArrayList<>();

            final HashMap<String, String> intentIdMap = new HashMap<>();
            intentIdMap.put("1", "HomeAutomation.TurnOff");
            intentIdMap.put("2", "HomeAutomation.TurnOn");

            try {
                final SpeechConfig intentConfig = SpeechConfig.fromSubscription(LanguageUnderstandingSubscriptionKey, LanguageUnderstandingServiceRegion);

                // final AudioConfig audioInput = AudioConfig.fromDefaultMicrophoneInput();
                final AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                final IntentRecognizer reco = new IntentRecognizer(intentConfig, audioInput);

                LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LanguageUnderstandingAppId);
                for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
                    reco.addIntent(intentModel, entry.getValue(), entry.getKey());
                }

                final Future<IntentRecognitionResult> task = reco.recognizeOnceAsync();
                setOnTaskCompletedListener(task, result -> {
                    String s = result.getText();

                    if (result.getReason() != ResultReason.RecognizedIntent) {
                        String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                        s = "Intent failed with " + result.getReason() + ". Did you enter your Language Understanding subscription?" + System.lineSeparator() + errorDetails;
                    }

                    String intentId = result.getIntentId();
                    String intent = "";
                    if (intentIdMap.containsKey(intentId)) {
                        intent = intentIdMap.get(intentId);
                    }

                    content.add(0, s);
                    content.add(1, " [intent: " + intent + "]");
                    recognizedTextView.setText(TextUtils.join(System.lineSeparator(), content));

                    if(intentId!=null && intentId.equals("1")){
                        m_syn.SpeakToAudio("Successfully switched off the light");
                    }else{
                        m_syn.SpeakToAudio("Successfully switched on the light");
                    }
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        });
    }

    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
