package com.example.flashquiz;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private TextView questionTextView;
    private Button optionButton1, optionButton2, optionButton3, optionButton4;
    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private RequestQueue requestQueue;
    private TextToSpeech tts;
    private Vibrator vibrator;
    private String fetchedDay = "";

    //********************************************************************
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastShakeTime = 0;
    //********************************************************************

    private static final String PREFS_NAME = "FlashQuizPrefs";
    private static final String KEY_FETCHED_DAY = "fetchedDay";
    private static final String KEY_QUESTIONS_JSON = "questionsJson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        questionTextView = findViewById(R.id.questionTextView);
        optionButton1 = findViewById(R.id.optionButton1);
        optionButton2 = findViewById(R.id.optionButton2);
        optionButton3 = findViewById(R.id.optionButton3);
        optionButton4 = findViewById(R.id.optionButton4);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        requestQueue = Volley.newRequestQueue(this);


        //********************************************************************
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        //********************************************************************


        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.FRENCH);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(QuizActivity.this, "Langue non supportée", Toast.LENGTH_SHORT).show();
                }
            }
        });

        String currentKey = getCurrentKey();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedKey = prefs.getString(KEY_FETCHED_DAY, "");
        String savedJson = prefs.getString(KEY_QUESTIONS_JSON, "");

        if (!savedKey.isEmpty() && savedKey.equals(currentKey) && !savedJson.isEmpty()) {
            parseQuestionsFromJson(savedJson);
        } else {
            fetchQuestions();
        }
    }

    private String getCurrentKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(new Date());
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String period = (hour < 12) ? "am" : "pm";
        return dateStr + " " + period;
    }

    private void fetchQuestions() {
        String url = "https://opentdb.com/api.php?amount=5&type=multiple";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        int responseCode = response.getInt("response_code");
                        if (responseCode == 0) {
                            String currentKey = getCurrentKey();
                            fetchedDay = currentKey;
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putString(KEY_FETCHED_DAY, currentKey)
                                    .putString(KEY_QUESTIONS_JSON, response.toString())
                                    .apply();
                            parseQuestionsFromResponse(response);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(QuizActivity.this, "Erreur lors du chargement des questions.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(QuizActivity.this, "Erreur de connexion.", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void parseQuestionsFromResponse(JSONObject response) throws JSONException {
        JSONArray results = response.getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            JSONObject obj = results.getJSONObject(i);
            String question = obj.getString("question");
            String correctAnswer = obj.getString("correct_answer");
            JSONArray incorrectArray = obj.getJSONArray("incorrect_answers");
            List<String> incorrectAnswers = new ArrayList<>();
            for (int j = 0; j < incorrectArray.length(); j++) {
                incorrectAnswers.add(incorrectArray.getString(j));
            }
            Question q = new Question(question, correctAnswer, incorrectAnswers);
            questionList.add(q);
        }
        currentQuestionIndex = 0;
        score = 0;
        showQuestion();
    }

    private void parseQuestionsFromJson(String jsonStr) {
        try {
            JSONObject response = new JSONObject(jsonStr);
            parseQuestionsFromResponse(response);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du chargement des questions sauvegardées.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            Question currentQuestion = questionList.get(currentQuestionIndex);
            questionTextView.setText(Html.fromHtml(currentQuestion.getQuestion()));
            List<String> options = currentQuestion.getOptions();
            optionButton1.setText(Html.fromHtml(options.get(0)));
            optionButton2.setText(Html.fromHtml(options.get(1)));
            optionButton3.setText(Html.fromHtml(options.get(2)));
            optionButton4.setText(Html.fromHtml(options.get(3)));

            optionButton1.setOnClickListener(v -> checkAnswer(options.get(0)));
            optionButton2.setOnClickListener(v -> checkAnswer(options.get(1)));
            optionButton3.setOnClickListener(v -> checkAnswer(options.get(2)));
            optionButton4.setOnClickListener(v -> checkAnswer(options.get(3)));

            if (currentQuestionIndex == 0) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::speakQuestion, 500);
            } else {
                speakQuestion();
            }
        } else {
            showScoreDialog();
        }
    }

    private void checkAnswer(String selectedAnswer) {
        Question currentQuestion = questionList.get(currentQuestionIndex);
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
        if (selectedAnswer.equals(currentQuestion.getCorrectAnswer())) {
            score++;
            Toast.makeText(this, "Correct !", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Mauvaise réponse !\nLa bonne réponse était : " + currentQuestion.getCorrectAnswer(), Toast.LENGTH_SHORT).show();
        }
        currentQuestionIndex++;
        speakQuestion();
        showQuestion();
    }

    private void speakQuestion() {
        if (tts != null && currentQuestionIndex < questionList.size()) {
            String questionText = questionList.get(currentQuestionIndex).getQuestion();
            tts.speak(questionText, TextToSpeech.QUEUE_FLUSH, null, "QuestionID");
        }
    }

    private void showScoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quiz terminé !");
        builder.setMessage("Votre score : " + score + "/" + questionList.size());
        builder.setPositiveButton("Partager", (dialog, which) -> shareScore());
        builder.setNegativeButton("Rejouer", (dialog, which) -> {
            String currentKey = getCurrentKey();
            if (currentKey.equals(fetchedDay)) {
                score = 0;
                currentQuestionIndex = 0;
                showQuestion();
            } else {
                questionList.clear();
                fetchQuestions();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void shareScore() {
        String shareText = "J'ai obtenu " + score + " points au FlashQuiz ! Pouvez-vous faire mieux ?";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Partager votre score"));
        finish();
    }



    //********************************************************************
    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = Math.abs(lastX - x);
            float deltaY = Math.abs(lastY - y);
            float deltaZ = Math.abs(lastZ - z);

            lastX = x;
            lastY = y;
            lastZ = z;

            float shakeThreshold = 12f;
            if ((deltaX > shakeThreshold || deltaY > shakeThreshold || deltaZ > shakeThreshold)
                    && System.currentTimeMillis() - lastShakeTime > 1000) {
                lastShakeTime = System.currentTimeMillis();
                handleShakeSkip();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    //********************************************************************

    //********************************************************************
    private void handleShakeSkip() {
        Toast.makeText(this, "Secousse détectée ! Question sautée", Toast.LENGTH_SHORT).show();
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
        currentQuestionIndex++;
        speakQuestion();
        showQuestion();
    }
    //********************************************************************


    //********************************************************************
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeListener);
    }
    //********************************************************************



    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
