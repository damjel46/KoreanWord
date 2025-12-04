package com.nono.word;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.games.PlayGames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TimeAttackActivity extends AppCompatActivity {

    // UI 변수
    private TextView tvInitial, tvMean, tvExample, tvTimer, tvCurrentScore, tvFeedback;
    private EditText etAnswer;
    private Button btnHint, btnPass, btnSubmit, btnRestart, btnBack; // btnReveal -> btnPass 변경
    private FrameLayout layoutCard;
    private AdView adViewTop;

    // 데이터 변수
    private List<WordItem> allWordList = new ArrayList<>();
    private List<WordItem> quizList = new ArrayList<>();
    private WordItem currentItem;

    // 게임 상태 변수
    private int currentScore = 0;
    private boolean isHintUsed = false;
    private CountDownTimer timer;
    private SharedPreferences prefs;

    private long timeLimit = 180000; // 초기 설정 시간
    private long timeLeftInMillis;   // 남은 시간 (실시간 추적용)
    private String scoreKey = "challengeBest_180000";
    private static final String LEADERBOARD_ID_3MIN = "CgkI-s7_zpQSEAIQAQ"; // 본인 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time);

        MobileAds.initialize(this, initializationStatus -> {});
        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);

        // Intent 데이터 수신
        timeLimit = getIntent().getLongExtra("TIME_LIMIT", 180000);
        scoreKey = "challengeBest_" + timeLimit;

        timeLeftInMillis = timeLimit;

        initUI();
        readCsvFile();
        startGame();
    }

    private void initUI() {
        tvTimer = findViewById(R.id.tv_timer);
        tvCurrentScore = findViewById(R.id.tv_current_score);
        tvInitial = findViewById(R.id.tv_initial);
        tvMean = findViewById(R.id.tv_mean);
        tvExample = findViewById(R.id.tv_example);
        tvFeedback = findViewById(R.id.tv_feedback);
        layoutCard = findViewById(R.id.layout_card);
        etAnswer = findViewById(R.id.et_answer);

        btnHint = findViewById(R.id.btn_hint);
        btnPass = findViewById(R.id.btn_pass); // ★ 연결 변경
        btnSubmit = findViewById(R.id.btn_submit);
        btnRestart = findViewById(R.id.btn_restart);
        btnBack = findViewById(R.id.btn_back);

        adViewTop = findViewById(R.id.adViewTop);

        btnBack.setOnClickListener(v -> finish());

        btnHint.setOnClickListener(v -> {
            tvExample.setVisibility(View.VISIBLE);
            isHintUsed = true;
        });

        btnPass.setOnClickListener(v -> passQuestion());

        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnRestart.setOnClickListener(v -> startGame());

        etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                checkAnswer();
                return true;
            }
            return false;
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTop.loadAd(adRequest);
    }

    private void readCsvFile() {
        allWordList.clear();
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("word.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    allWordList.add(new WordItem(tokens[0], tokens[1], tokens[2], tokens[3]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startGame() {
        currentScore = 0;
        tvCurrentScore.setText("0점");
        tvFeedback.setText("");

        enableGameUI(true);
        btnRestart.setVisibility(View.GONE);

        quizList.clear();
        quizList.addAll(allWordList);
        Collections.shuffle(quizList, new Random(System.nanoTime()));

        loadNextQuestion();

        timeLeftInMillis = timeLimit;
        startTimer(timeLeftInMillis);
    }

    private void startTimer(long duration) {
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;

                long seconds = millisUntilFinished / 1000;
                long min = seconds / 60;
                long sec = seconds % 60;
                tvTimer.setText(String.format("%02d:%02d", min, sec));

                if (seconds <= 10) tvTimer.setTextColor(Color.RED);
                else tvTimer.setTextColor(Color.parseColor("#F44336"));
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                tvTimer.setText("00:00");
                finishGame();
            }
        }.start();
    }

    // ★ [신규] 시간 차감(패널티) 메소드
    private void applyPenalty(long penaltyMillis, String reason) {
        if (timer != null) timer.cancel(); // 기존 타이머 중지

        timeLeftInMillis -= penaltyMillis; // 시간 차감

        if (timeLeftInMillis <= 0) {
            // 시간이 0 이하가 되면 바로 종료
            timeLeftInMillis = 0;
            tvTimer.setText("00:00");
            finishGame();
        } else {
            // 남은 시간으로 타이머 재시작
            startTimer(timeLeftInMillis);

            // 피드백 메시지 (빨간색)
            tvFeedback.setText(reason);
            tvFeedback.setTextColor(Color.RED);

            // 화면 흔들기 효과
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            tvTimer.startAnimation(shake); // 타이머가 흔들림
        }
    }

    private void passQuestion() {
        applyPenalty(5000, "PASS! (-5초)");
        // 문제 넘기기 (퀴즈 리스트에서 삭제하지 않고 뒤로 보낼 수도 있고, 삭제할 수도 있음. 여기선 삭제)
        if (currentItem != null) quizList.remove(currentItem);
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (quizList.isEmpty()) {
            quizList.addAll(allWordList);
            Collections.shuffle(quizList, new Random(System.nanoTime()));
        }

        currentItem = quizList.get(0);
        isHintUsed = false;

        tvInitial.setText(currentItem.getInitial());
        tvMean.setText(currentItem.getMean());
        tvExample.setText(currentItem.getExample());
        tvExample.setVisibility(View.GONE);
        etAnswer.setText("");

        // (정답 맞췄을 때만 초기화하거나 1초 뒤 사라지게 처리함)
    }

    private void checkAnswer() {
        if (currentItem == null) return;
        String userAnswer = etAnswer.getText().toString().trim();

        if (userAnswer.equals(currentItem.getWord())) {
            // [정답]
            if (!isHintUsed) {
                currentScore++;
                tvCurrentScore.setText(currentScore + "점");
                tvFeedback.setText("정답! (+1점)");
                tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_success));
            } else {
                tvFeedback.setText("힌트 사용 (점수 없음)");
                tvFeedback.setTextColor(Color.GRAY);
            }

            quizList.remove(currentItem);

            // 텍스트 초기화 방지하며 다음 문제 로드
            loadNextQuestion();

            // 정답 메시지는 1초 뒤 사라짐
            tvFeedback.postDelayed(() -> tvFeedback.setText(""), 1000);

            layoutCard.setBackgroundResource(R.drawable.bg_border_purple);

        } else {
            //  오답 시 패널티 (-2초)
            applyPenalty(2000, "틀렸습니다! (-2초)");

            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            layoutCard.startAnimation(shake);
            layoutCard.setBackgroundResource(R.drawable.bg_border_red);
            etAnswer.setText("");

            layoutCard.postDelayed(() -> layoutCard.setBackgroundResource(R.drawable.bg_border_purple), 500);
        }
    }

    private void finishGame() {
        enableGameUI(false);
        tvFeedback.setText("⏰ 시간 종료! 최종 점수: " + currentScore + "점");
        tvTimer.setText("00:00");

        checkAndSaveBestScore();

        if (timeLimit == 180000) {
            submitLeaderboardScore();
        }

        btnRestart.setVisibility(View.VISIBLE);
    }

    private void checkAndSaveBestScore() {
        int bestScore = prefs.getInt(scoreKey, 0);
        if (currentScore > bestScore) {
            prefs.edit().putInt(scoreKey, currentScore).apply();
            if (!btnSubmit.isEnabled()) {
                tvFeedback.append("\n🎉 신기록 달성!");
            }
        }
    }

    private void submitLeaderboardScore() {
        try {
            PlayGames.getLeaderboardsClient(this)
                    .submitScoreImmediate(LEADERBOARD_ID_3MIN, currentScore)
                    .addOnFailureListener(e -> Toast.makeText(this, "전송 실패", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableGameUI(boolean enable) {
        etAnswer.setEnabled(enable);
        btnSubmit.setEnabled(enable);
        btnHint.setEnabled(enable);
        btnPass.setEnabled(enable); // Pass 버튼 제어
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        checkAndSaveBestScore();
    }
}
