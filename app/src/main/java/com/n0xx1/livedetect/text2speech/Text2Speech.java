package com.n0xx1.livedetect.text2speech;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.n0xx1.livedetect.MainActivity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Text2Speech implements TextToSpeech.OnInitListener{

    private static final String TAG = "Text2Speech";
    private TextToSpeech tts;
    private final Context context;
    private final Activity MainActivity;

    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public Text2Speech(Context context, Activity activity) {

        this.context = context;
        MainActivity = activity;
        tts = new TextToSpeech(context, this);
        tts.setLanguage(Locale.US);


//        service.scheduleAtFixedRate(compressBuffer, 10, 2, TimeUnit.SECONDS);
//        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
//            ttsGreater21(text);
//        } else {
//            ttsUnder20(text);
//        }

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    ((MainActivity)MainActivity).getWorkflowModel().setTtsAvailable(true);
                    Log.i(TAG, "***TtsAvailable: "+((MainActivity)MainActivity).getWorkflowModel().getTtsAvailable());
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "Failed to initialize the TextToSpeech engine");
                }

                @Override
                public void onStart(String utteranceId) {
                    ((MainActivity)MainActivity).getWorkflowModel().setTtsAvailable(false);
                    Log.i(TAG, "***TtsAvailable: "+((MainActivity)MainActivity).getWorkflowModel().getTtsAvailable());
                }


            });

        } else {
            tts = null;
            Log.e(TAG, "Failed to initialize the TextToSpeech engine");
        }


    }

    public void speech(String text){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
            ttsGreater21(text);
        } else {
            ttsUnder20(text);
        }
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        Bundle bundle = new Bundle();
        String utteranceId=this.hashCode() + "";
        bundle.putString("utteranceId", utteranceId);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
    }




//    Runnable compressBuffer = new Runnable() {
//        @Override
//        public void run() {
//
//            printBuffer();
//            for (int i = 0; i < ttsBuffer.size(); i++)
//                for (int j = 1; j < ttsBuffer.size(); j++)
//                    if (ttsBuffer.get(i).indexOf(ttsBuffer.get(j)) != -1) {
//                        ttsBuffer.remove((ttsBuffer.get(i).length() > ttsBuffer.get(j).length()) ? j : i);
//                        printBuffer();
//                    }
//        }
//    };




}
