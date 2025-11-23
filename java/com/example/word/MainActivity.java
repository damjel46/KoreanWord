package com.example.word;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI 변수
    private TextView tvInitial, tvMean, tvExample, tvProgress, tvFeedback; // tvFeedback 추가
    private EditText etAnswer;
    private Button btnHint, btnReveal, btnSubmit;
    private LinearLayout layoutCard; // 카드 레이아웃 (테두리 색상 변경용)

    private List<WordItem> wordList = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 연결
        tvInitial = findViewById(R.id.tv_initial);
        tvMean = findViewById(R.id.tv_mean);
        tvExample = findViewById(R.id.tv_example);
        tvProgress = findViewById(R.id.tv_progress);
        tvFeedback = findViewById(R.id.tv_feedback); // 연결
        layoutCard = findViewById(R.id.layout_card); // 연결
        etAnswer = findViewById(R.id.et_answer);

        btnHint = findViewById(R.id.btn_hint);
        btnReveal = findViewById(R.id.btn_reveal);
        btnSubmit = findViewById(R.id.btn_submit);

        // 파일 읽기 및 초기화
        readCsvFile();
        loadQuestion();

        // 1. 힌트 버튼
        btnHint.setOnClickListener(v -> {
            tvExample.setVisibility(View.VISIBLE);
            tvFeedback.setText(""); // 메시지 초기화
        });

        // 2. 정답 보기 버튼
        btnReveal.setOnClickListener(v -> showCorrectAnswer());

        // 3. 제출 버튼
        btnSubmit.setOnClickListener(v -> checkAnswer());

        // 4. 엔터키 입력 처리
        etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                checkAnswer();
                return true;
            }
            return false;
        });
    }

    private void readCsvFile() {
        // ... (기존과 동일하여 생략, 구분자 | 사용) ...
        // 만약 필요하시면 다시 적어드리겠습니다.
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("test.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    wordList.add(new WordItem(tokens[0], tokens[1], tokens[2], tokens[3]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadQuestion() {
        if (currentIndex < wordList.size()) {
            WordItem item = wordList.get(currentIndex);
            tvProgress.setText((currentIndex + 1) + " / " + wordList.size());
            tvInitial.setText(item.getInitial());
            tvMean.setText(item.getMean());
            tvExample.setText(item.getExample());

            // 상태 초기화
            tvExample.setVisibility(View.GONE);
            etAnswer.setText("");
            tvFeedback.setText(""); // 피드백 텍스트 지우기
            tvFeedback.setTextColor(Color.BLACK);
            layoutCard.setBackgroundResource(R.drawable.bg_border_purple); // 보라색 테두리 복구
        } else {
            tvFeedback.setText("모든 문제를 완료했습니다!");
            tvFeedback.setTextColor(Color.BLUE);
            btnSubmit.setEnabled(false);
        }
    }

    private void showCorrectAnswer() {
        if (currentIndex >= wordList.size()) return;
        String correct = wordList.get(currentIndex).getWord();

        etAnswer.setText(correct);
        tvFeedback.setText("정답을 확인하고 제출하세요.");
        tvFeedback.setTextColor(Color.parseColor("#FF6F61")); // 주황색
    }

    private void checkAnswer() {
        if (currentIndex >= wordList.size()) return;

        String userAnswer = etAnswer.getText().toString().trim();
        String correctAnswer = wordList.get(currentIndex).getWord();

        if (userAnswer.equals(correctAnswer)) {
            // [정답]
            tvFeedback.setText("정답입니다! 👏");
            tvFeedback.setTextColor(Color.parseColor("#4CAF50")); // 초록색

            // 잠시 후 다음 문제로 (0.5초 딜레이)
            new Handler(Looper.getMainLooper()).postDelayed(this::nextQuestion, 500);

        } else {
            // [오답] -> 애니메이션 & 빨간 테두리 효과
            tvFeedback.setText("틀렸습니다. 다시 시도해보세요.");
            tvFeedback.setTextColor(Color.RED);

            // 1. 흔들림 애니메이션 실행
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            layoutCard.startAnimation(shake); // 카드 전체를 흔듭니다

            // 2. 테두리 빨간색으로 변경
            layoutCard.setBackgroundResource(R.drawable.bg_border_red);

            // 3. 입력값 지우기
            etAnswer.setText("");
            etAnswer.requestFocus();

            // 4. 0.5초 뒤에 다시 보라색 테두리로 복구
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                layoutCard.setBackgroundResource(R.drawable.bg_border_purple);
            }, 500);
        }
    }

    private void nextQuestion() {
        currentIndex++;
        loadQuestion();
    }
}