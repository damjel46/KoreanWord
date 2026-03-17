package com.nono.word;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.games.PlayGames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LiteracyChallengeActivity extends AppCompatActivity {

    private TextView tvTimer, tvScore, tvFeedback, tvQuestion, tvPassage;
    private Button btnChoice1, btnChoice2, btnChoice3, btnChoice4, btnPass;
    private Button btnRestart, btnBack;
    private FrameLayout layoutCard;
    private ScrollView scrollView;
    private ProgressBar pbTimer;
    private AdView adViewTop;

    private List<LiteracyItem> allLiteracyList = new ArrayList<>();
    private LiteracyItem currentItem;

    private int currentScore = 0;
    private boolean answered = false;
    private CountDownTimer timer;
    private WordRepository repository;

    private long timeLimit = Constants.TIME_LIMIT_3MIN;
    private long timeLeftInMillis;
    private Button[] choiceButtons;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_literacy_challenge);

        MobileAds.initialize(this, initializationStatus -> {});
        repository = new WordRepository(this);
        random = new Random(System.nanoTime());

        // Intent 데이터 수신
        timeLimit = getIntent().getLongExtra("TIME_LIMIT", Constants.TIME_LIMIT_3MIN);
        timeLeftInMillis = timeLimit;

        initUI();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        loadQuestions();
        startGame();
    }

    private void initUI() {
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        tvFeedback = findViewById(R.id.tv_feedback);
        tvQuestion = findViewById(R.id.tv_question);
        tvPassage = findViewById(R.id.tv_passage);
        layoutCard = findViewById(R.id.layout_card);
        scrollView = findViewById(R.id.scrollView);

        btnChoice1 = findViewById(R.id.btn_choice1);
        btnChoice2 = findViewById(R.id.btn_choice2);
        btnChoice3 = findViewById(R.id.btn_choice3);
        btnChoice4 = findViewById(R.id.btn_choice4);
        btnPass = findViewById(R.id.btn_pass);
        btnRestart = findViewById(R.id.btn_restart);
        btnBack = findViewById(R.id.btn_back);

        adViewTop = findViewById(R.id.adViewTop);
        pbTimer = findViewById(R.id.pb_timer);
        pbTimer.setMax((int) (timeLimit / 1000));
        pbTimer.setProgress((int) (timeLimit / 1000));

        btnBack.setOnClickListener(v -> finish());
        btnPass.setOnClickListener(v -> handlePass());

        choiceButtons = new Button[]{btnChoice1, btnChoice2, btnChoice3, btnChoice4};

        View.OnClickListener choiceListener = v -> {
            if (answered) return;
            int selected = -1;
            for (int i = 0; i < choiceButtons.length; i++) {
                if (v == choiceButtons[i]) { selected = i; break; }
            }
            checkAnswer(selected);
        };

        for (Button btn : choiceButtons) btn.setOnClickListener(choiceListener);

        btnRestart.setOnClickListener(v -> {
            currentScore = 0;
            timeLeftInMillis = timeLimit;
            startGame();
        });

        adViewTop.loadAd(new AdRequest.Builder().build());
    }

    private void loadQuestions() {
        allLiteracyList.clear();
        allLiteracyList.addAll(repository.loadLiteracyQuestions(this));
    }

    private void startGame() {
        currentScore = 0;
        answered = false;
        tvScore.setText("0점");
        tvFeedback.setText("");
        btnRestart.setVisibility(View.GONE);
        btnPass.setEnabled(true);

        enableGameUI(true);
        showQuestion();

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
                pbTimer.setProgress((int) seconds);

                if (seconds <= 10) tvTimer.setTextColor(Color.RED);
                else tvTimer.setTextColor(Color.parseColor("#F97316"));
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                tvTimer.setText("00:00");
                finishGame();
            }
        }.start();
    }

    private void applyPenalty(long penaltyMillis, String reason) {
        if (timer != null) timer.cancel();

        timeLeftInMillis -= penaltyMillis;

        if (timeLeftInMillis <= 0) {
            timeLeftInMillis = 0;
            tvTimer.setText("00:00");
            finishGame();
        } else {
            startTimer(timeLeftInMillis);
            tvFeedback.setText(reason);
            tvFeedback.setTextColor(Color.RED);
        }
    }

    private void showQuestion() {
        answered = false;
        btnRestart.setVisibility(View.GONE);
        layoutCard.setVisibility(View.VISIBLE);
        tvFeedback.setText("");

        if (allLiteracyList.isEmpty()) {
            finishGame();
            return;
        }

        // 전체 리스트에서 랜덤하게 문제 출제
        currentItem = allLiteracyList.get(random.nextInt(allLiteracyList.size()));
        tvScore.setText(currentScore + "점");
        tvQuestion.setText(currentItem.getQuestion());

        if (currentItem.getPassage() != null && !currentItem.getPassage().isEmpty()) {
            tvPassage.setText("\"" + currentItem.getPassage() + "\"");
            tvPassage.setVisibility(View.VISIBLE);
        } else {
            tvPassage.setVisibility(View.GONE);
        }

        String[] prefixes = {"① ", "② ", "③ ", "④ "};
        String[] choices = currentItem.getChoices();
        for (int i = 0; i < choiceButtons.length; i++) {
            choiceButtons[i].setText(prefixes[i] + choices[i]);
            choiceButtons[i].setBackgroundResource(R.drawable.bg_choice_btn);
            choiceButtons[i].setBackgroundTintList(null);
            choiceButtons[i].setTextColor(Color.parseColor("#334155"));
            choiceButtons[i].setEnabled(true);
        }

        scrollView.scrollTo(0, 0);
    }

    private void checkAnswer(int selected) {
        if (selected < 0 || answered) return;
        answered = true;

        int correct = currentItem.getCorrectIndex();

        // 모든 버튼 비활성화
        for (Button btn : choiceButtons) btn.setEnabled(false);

        // 정답은 표시하지 않음 (챌린지 모드 - 정답 힌트 없음)

        if (selected == correct) {
            currentScore++;
            tvScore.setText(currentScore + "점");
            tvFeedback.setText("정답! 🎉");
            tvFeedback.setTextColor(Color.parseColor("#16A34A"));
            layoutCard.postDelayed(this::showQuestion, 800);
        } else {
            choiceButtons[selected].setBackgroundResource(R.drawable.bg_choice_wrong);
            choiceButtons[selected].setBackgroundTintList(null);
            choiceButtons[selected].setTextColor(Color.parseColor("#EF4444"));
            tvFeedback.setText("오답! (-5초)");
            tvFeedback.setTextColor(Color.RED);
            applyPenalty(5000, "오답! (-5초)");
            layoutCard.postDelayed(this::showQuestion, 1000);
        }
    }

    private void handlePass() {
        if (answered) return;
        answered = true;

        for (Button btn : choiceButtons) btn.setEnabled(false);
        tvFeedback.setText("PASS (-2초)");
        tvFeedback.setTextColor(Color.RED);

        applyPenalty(2000, "PASS (-2초)");
        layoutCard.postDelayed(this::showQuestion, 600);
    }

    private void finishGame() {
        enableGameUI(false);
        btnPass.setEnabled(false);
        tvFeedback.setText("⏰ 시간 종료! 최종 점수: " + currentScore + "점");
        tvTimer.setText("00:00");

        checkAndSaveBestScore();

        if (timeLimit == Constants.TIME_LIMIT_3MIN) {
            submitLeaderboardScore();
        }

        btnRestart.setVisibility(View.VISIBLE);
    }

    private void checkAndSaveBestScore() {
        int bestScore = repository.getLiteracyChallengeBestScore(timeLimit);
        if (currentScore > bestScore) {
            repository.saveLiteracyChallengeBestScore(timeLimit, currentScore);
            tvFeedback.append("\n🎉 신기록 달성!");
        }
    }

    private void submitLeaderboardScore() {
        try {
            PlayGames.getLeaderboardsClient(this)
                    .submitScoreImmediate(Constants.LEADERBOARD_ID, currentScore)
                    .addOnFailureListener(e -> Toast.makeText(this, "전송 실패", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableGameUI(boolean enable) {
        for (Button btn : choiceButtons) btn.setEnabled(enable);
        btnPass.setEnabled(enable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        checkAndSaveBestScore();
    }
}
