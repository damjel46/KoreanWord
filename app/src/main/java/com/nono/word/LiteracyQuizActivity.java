package com.nono.word;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LiteracyQuizActivity extends AppCompatActivity {

    private TextView tvCount, tvScore, tvFeedback, tvQuestion, tvPassage;
    private Button btnChoice1, btnChoice2, btnChoice3, btnChoice4;
    private Button btnNext, btnRestart, btnBack;
    private FrameLayout layoutCard;
    private ScrollView scrollView;
    private AdView adViewTop;

    private List<LiteracyItem> quizList = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private int currentStreak = 0;
    private int maxStreak = 0;
    private int totalQuestions;
    private boolean answered = false;
    private int shuffledCorrectIndex;

    private WordRepository repository;
    private Button[] choiceButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_literacy_quiz);

        MobileAds.initialize(this, s -> {});
        repository = new WordRepository(this);

        initUI();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        loadQuestions();
        showQuestion();
    }

    private void initUI() {
        tvCount = findViewById(R.id.tv_count);
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
        btnNext = findViewById(R.id.btn_next);
        btnRestart = findViewById(R.id.btn_restart);
        btnBack = findViewById(R.id.btn_back);

        adViewTop = findViewById(R.id.adViewTop);
        adViewTop.loadAd(new AdRequest.Builder().build());

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

        btnNext.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex < totalQuestions) {
                showQuestion();
            } else {
                showResult();
            }
        });

        btnRestart.setOnClickListener(v -> {
            currentIndex = 0;
            score = 0;
            currentStreak = 0;
            maxStreak = 0;
            Collections.shuffle(quizList, new Random(System.nanoTime()));
            if (quizList.size() > Constants.LITERACY_QUIZ_COUNT) {
                quizList = new ArrayList<>(quizList.subList(0, Constants.LITERACY_QUIZ_COUNT));
            }
            totalQuestions = quizList.size();
            showQuestion();
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadQuestions() {
        String category = getIntent().getStringExtra("category");
        List<LiteracyItem> all = repository.loadLiteracyQuestions(this);

        if (category != null && !category.equals("all")) {
            List<LiteracyItem> filtered = new ArrayList<>();
            for (LiteracyItem item : all) {
                if (item.getCategory().equals(category)) filtered.add(item);
            }
            quizList = filtered;
        } else {
            quizList = new ArrayList<>(all);
        }

        Collections.shuffle(quizList, new Random(System.nanoTime()));

        if (quizList.size() > Constants.LITERACY_QUIZ_COUNT) {
            quizList = new ArrayList<>(quizList.subList(0, Constants.LITERACY_QUIZ_COUNT));
        }
        totalQuestions = quizList.size();
    }

    private void showQuestion() {
        answered = false;
        btnNext.setVisibility(View.GONE);
        btnRestart.setVisibility(View.GONE);
        layoutCard.setVisibility(View.VISIBLE);
        tvFeedback.setText("");

        LiteracyItem item = quizList.get(currentIndex);
        tvCount.setText((currentIndex + 1) + " / " + totalQuestions);
        tvScore.setText(score + "점");
        tvQuestion.setText(item.getQuestion());

        if (item.getPassage() != null && !item.getPassage().isEmpty()) {
            tvPassage.setText("\"" + item.getPassage() + "\"");
            tvPassage.setVisibility(View.VISIBLE);
        } else {
            tvPassage.setVisibility(View.GONE);
        }

        // 보기 순서 랜덤 셔플
        String[] originalChoices = item.getChoices();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < originalChoices.length; i++) order.add(i);
        Collections.shuffle(order);
        shuffledCorrectIndex = order.indexOf(item.getCorrectIndex());

        String[] prefixes = {"① ", "② ", "③ ", "④ "};
        for (int i = 0; i < choiceButtons.length; i++) {
            choiceButtons[i].setText(prefixes[i] + originalChoices[order.get(i)]);
            choiceButtons[i].setBackgroundResource(R.drawable.bg_choice_btn);
            choiceButtons[i].setBackgroundTintList(null);
            choiceButtons[i].setTextColor(Color.parseColor("#334155"));
            choiceButtons[i].setEnabled(true);
        }

        scrollView.scrollTo(0, 0);
    }

    private void checkAnswer(int selected) {
        if (selected < 0) return;
        answered = true;

        int correct = shuffledCorrectIndex;

        // 모든 버튼 비활성화
        for (Button btn : choiceButtons) btn.setEnabled(false);

        // 정답 버튼 표시
        choiceButtons[correct].setBackgroundResource(R.drawable.bg_choice_correct);
        choiceButtons[correct].setBackgroundTintList(null);
        choiceButtons[correct].setTextColor(Color.parseColor("#16A34A"));

        if (selected == correct) {
            score++;
            currentStreak++;
            maxStreak = Math.max(maxStreak, currentStreak);
            tvScore.setText(score + "점");
            tvFeedback.setText("정답! 🎉");
            tvFeedback.setTextColor(Color.parseColor("#16A34A"));
        } else {
            currentStreak = 0;
            choiceButtons[selected].setBackgroundResource(R.drawable.bg_choice_wrong);
            choiceButtons[selected].setBackgroundTintList(null);
            choiceButtons[selected].setTextColor(Color.parseColor("#EF4444"));
            tvFeedback.setText("오답! 정답은 " + (correct + 1) + "번");
            tvFeedback.setTextColor(Color.parseColor("#EF4444"));
        }

        if (currentIndex < totalQuestions - 1) {
            btnNext.setVisibility(View.VISIBLE);
        } else {
            // 마지막 문제 → 1.5초 후 결과
            layoutCard.postDelayed(this::showResult, 1500);
        }
    }

    private void showResult() {
        layoutCard.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        tvQuestion.setVisibility(View.GONE);
        tvPassage.setVisibility(View.GONE);

        String resultText = totalQuestions + "문제 중 " + score + "개 정답!\n"
                + "정답률: " + (score * 100 / totalQuestions) + "%";

        tvFeedback.setText("📊 테스트 완료!\n\n" + resultText);
        tvFeedback.setTextColor(Color.parseColor("#5211D4"));
        tvFeedback.setTextSize(18);

        // 최고 점수 저장
        if (score > repository.getLiteracyBestScore()) {
            repository.saveLiteracyBestScore(score);
            tvFeedback.append("\n\n🏆 신기록 달성!");
        }

        // 연속 정답 기록 저장
        if (maxStreak > repository.getBestStreak("literacy")) {
            repository.saveBestStreak("literacy", maxStreak);
        }

        btnRestart.setVisibility(View.VISIBLE);
    }
}
