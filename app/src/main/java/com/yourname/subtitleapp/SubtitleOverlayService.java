package com.yourname.subtitleapp;

import android.app.*;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.ArrayList;

public class SubtitleOverlayService extends Service {
    private WindowManager windowManager;
    private TextView subtitleView;
    private SpeechRecognizer speechRecognizer;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
        startSpeechRecognition();
    }

    private void createOverlay() {
        subtitleView = new TextView(this);
        subtitleView.setText("Ожидание аудио...");
        subtitleView.setTextSize(18);
        subtitleView.setTextColor(0xFFFFFFFF);
        subtitleView.setBackgroundColor(0x80000000);
        subtitleView.setPadding(20, 20, 20, 20);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        windowManager.addView(subtitleView, params);
    }

    private void startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateSubtitle("Распознавание недоступно");
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { updateSubtitle("Слушаю..."); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) updateSubtitle(matches.get(0));
                restartListening();
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) updateSubtitle(matches.get(0) + "...");
            }
            @Override public void onError(int error) {
                updateSubtitle("Ошибка: " + error);
                restartListening();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        restartListening();
    }

    private void restartListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
    }

    private void updateSubtitle(String text) {
        if (subtitleView != null) {
            subtitleView.post(() -> subtitleView.setText(text));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (subtitleView != null) windowManager.removeView(subtitleView);
    }
}
