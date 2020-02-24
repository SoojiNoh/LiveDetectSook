package com.n0xx1.livedetect.text2speech;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class Text2Speech implements TextToSpeech.OnInitListener{

    TextToSpeech tts;

    public Text2Speech(Context context, Activity activity) {


        tts = new TextToSpeech(context, this);

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
        } else {
            tts = null;
            Log.e("MainActivity", "Failed to initialize the TextToSpeech engine");
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
//        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }


}
