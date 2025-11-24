package com.chosun.word;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvInitial, tvMean, tvExample, tvStreak, tvFeedback;
    private EditText etAnswer;
    private Button btnHint, btnReveal, btnSubmit, btnRestart; // btnRestart 추가
    private ImageButton btnBookmark, btnExclude;
    private SwitchMaterial switchMode;

    private FrameLayout layoutCard;

    private List<WordItem> wordList = new ArrayList<>();
    private List<WordItem> bookmarkedList = new ArrayList<>();

    private WordItem currentItem;
    private boolean isAnswerRevealed = false;
    private int streakCount = 0;
    private Random random = new Random();
    private SharedPreferences prefs;
    private Set<String> excludedSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 연결
        tvInitial = findViewById(R.id.tv_initial);
        tvMean = findViewById(R.id.tv_mean);
        tvExample = findViewById(R.id.tv_example);
        tvStreak = findViewById(R.id.tv_streak);
        tvFeedback = findViewById(R.id.tv_feedback);
        layoutCard = findViewById(R.id.layout_card);
        etAnswer = findViewById(R.id.et_answer);

        btnHint = findViewById(R.id.btn_hint);
        btnReveal = findViewById(R.id.btn_reveal);
        btnSubmit = findViewById(R.id.btn_submit);
        btnRestart = findViewById(R.id.btn_restart); // 연결
        btnBookmark = findViewById(R.id.btn_bookmark);
        btnExclude = findViewById(R.id.btn_exclude);
        switchMode = findViewById(R.id.switch_mode);
        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
        readCsvFile();
        loadRandomQuestion();

        btnHint.setOnClickListener(v -> {
            tvExample.setVisibility(View.VISIBLE);
            tvFeedback.setText("");
        });

        btnReveal.setOnClickListener(v -> showCorrectAnswer());
        btnSubmit.setOnClickListener(v -> checkAnswer());

        etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                checkAnswer();
                return true;
            }
            return false;
        });

        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnExclude.setOnClickListener(v -> excludeCurrentWord());

        btnRestart.setOnClickListener(v -> resetGame());

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            streakCount = 0;
            tvStreak.setText("연속 : 0");
            loadRandomQuestion();

        });
    }

    private List<WordItem> getCurrentTargetList() {
        return switchMode.isChecked() ? bookmarkedList : wordList;
    }

    private void loadRandomQuestion() {
        List<WordItem> targetList = getCurrentTargetList();

        if (targetList.isEmpty()) {
            // 텍스트 설정
            if (switchMode.isChecked()) {
                tvInitial.setText("텅");
                tvMean.setText("즐겨찾기한 단어가 없습니다.");
                tvFeedback.setText("별표를 눌러 단어를 추가해보세요!");
                // 즐겨찾기 모드일 땐 다시 시작 버튼 안 보여줌 (전체로 가서 추가해야 하니까)
                btnRestart.setVisibility(View.GONE);
            } else {
                tvInitial.setText("끝");
                tvMean.setText("모든 단어를 마스터했습니다!");
                tvFeedback.setText("정말 대단해요! 🥳");

                // ★ 전체 모드가 끝났을 때만 다시 시작 버튼 보이기
                btnRestart.setVisibility(View.VISIBLE);
            }

            tvExample.setText("");
            tvExample.setVisibility(View.GONE);

            // 입력 및 버튼 잠금
            etAnswer.setEnabled(false);
            btnSubmit.setEnabled(false);
            btnReveal.setEnabled(false);
            btnHint.setEnabled(false); // ★ 힌트 버튼도 잠금
            btnExclude.setEnabled(false);
            btnBookmark.setEnabled(false);

            currentItem = null;
            return;
        }

        // 정상 게임 진행 상태 (버튼들 다시 활성화)
        etAnswer.setEnabled(true);
        btnSubmit.setEnabled(true);
        btnReveal.setEnabled(true);
        btnHint.setEnabled(true);
        btnExclude.setEnabled(true);
        btnBookmark.setEnabled(true);
        btnRestart.setVisibility(View.GONE); // 게임 중엔 숨김

        isAnswerRevealed = false;

        int randomIndex = random.nextInt(targetList.size());
        currentItem = targetList.get(randomIndex);

        tvStreak.setText("연속 : " + streakCount + " 🔥");
        tvInitial.setText(currentItem.getInitial());
        tvMean.setText(currentItem.getMean());
        tvExample.setText(currentItem.getExample());

        updateBookmarkIcon();

        tvExample.setVisibility(View.GONE);
        etAnswer.setText("");
        tvFeedback.setText("");
        tvFeedback.setTextColor(Color.BLACK);
        layoutCard.setBackgroundResource(R.drawable.bg_border_purple);
    }

    private void resetGame() {


        // 1. 현재 남아있는 단어들만 다시 섞기
        if (!wordList.isEmpty()) {
            Collections.shuffle(wordList);
        }

        // 2. 점수 및 상태 초기화
        streakCount = 0;
        tvStreak.setText("연속 : 0");

        // 3. 토스트 대신 피드백 텍스트로 안내
        tvFeedback.setText("순서를 섞어서 다시 시작합니다! 🔄");
        tvFeedback.setTextColor(Color.BLUE);

        // 4. 첫 문제 로드
        loadRandomQuestion();
    }

    private void excludeCurrentWord() {
        if (currentItem == null) return;

        // 리스트에서 영구 삭제
        wordList.remove(currentItem);
        bookmarkedList.remove(currentItem);
        saveData();
        // 다음 문제 로드
        loadRandomQuestion();

        // 피드백 텍스트로 안내 (null 체크: 마지막 문제 삭제 시 에러 방지)
        if (currentItem != null) {
            tvFeedback.setText("단어장에서 제외했습니다. 👋");
            tvFeedback.setTextColor(Color.DKGRAY);
        }
    }

    private void readCsvFile() { //파일 읽어오기
        wordList.clear();
        bookmarkedList.clear();
        excludedSet = new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
        Set<String> savedBookmarks = prefs.getStringSet("bookmarks", new HashSet<>());
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("test.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    String wordName = tokens[1]; // 정답 단어 (고유 키로 사용)

                    if (excludedSet.contains(wordName)) {
                        continue;
                    }
                    WordItem item = new WordItem(tokens[0], wordName, tokens[2], tokens[3]);
                    if (savedBookmarks.contains(wordName)) {
                        item.setBookmarked(true);
                        bookmarkedList.add(item);
                    }

                    wordList.add(item);
                }
            }
            // 남은 단어들만 섞기
            Collections.shuffle(wordList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleBookmark() {
        if (currentItem == null) return;
        boolean newState = !currentItem.isBookmarked();
        currentItem.setBookmarked(newState);
        if (newState) {
            if (!bookmarkedList.contains(currentItem)) bookmarkedList.add(currentItem);
        } else {
            bookmarkedList.remove(currentItem);
        }
        updateBookmarkIcon();
        saveData();
    }

    private void updateBookmarkIcon() {
        if (currentItem != null) {
            if (currentItem.isBookmarked()) btnBookmark.setImageResource(R.drawable.ic_star_filled);
            else btnBookmark.setImageResource(R.drawable.ic_star_border);
        }
    }

    private void showCorrectAnswer() {
        if (currentItem == null) return;
        isAnswerRevealed = true;
        streakCount = 0;
        tvStreak.setText("연속 : " + streakCount);
        etAnswer.setText(currentItem.getWord());
        tvFeedback.setText("정답 확인 (점수 미인정)");
        tvFeedback.setTextColor(Color.parseColor("#FF6F61"));
    }

    private void checkAnswer() {
        if (currentItem == null) return;

        String userAnswer = etAnswer.getText().toString().trim();
        String correctAnswer = currentItem.getWord();

        if (userAnswer.equals(correctAnswer)) {

            // 1. 띄워줄 메시지와 색상을 미리 결정합니다.
            String message;
            int color;

            if (isAnswerRevealed) {
                message = "정답 확인 후 패스!";
                color = Color.GRAY;
            } else {
                streakCount++;
                message = "정답입니다! 👏";
                color = Color.parseColor("#4CAF50");
            }

            loadRandomQuestion();

            if (currentItem != null) {
                tvFeedback.setText(message);
                tvFeedback.setTextColor(color);
                tvFeedback.postDelayed(() -> tvFeedback.setText(""), 500);
            }

        } else {
            streakCount = 0;
            tvStreak.setText("연속 : " + streakCount);
            tvFeedback.setText("틀렸습니다 😢");
            tvFeedback.setTextColor(Color.RED);

            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            layoutCard.startAnimation(shake);
            layoutCard.setBackgroundResource(R.drawable.bg_border_red);
            etAnswer.setText("");
            etAnswer.requestFocus();
            layoutCard.postDelayed(() -> layoutCard.setBackgroundResource(R.drawable.bg_border_purple), 500);
        }
    }
    private void saveData() {
        SharedPreferences.Editor editor = prefs.edit();

        // 1. 현재 즐겨찾기 목록을 Set<String>으로 변환
        Set<String> bookmarkSet = new HashSet<>();
        for (WordItem item : bookmarkedList) {
            bookmarkSet.add(item.getWord());
        }

        // 2. 저장 (즐겨찾기 목록, 제외 목록)
        editor.putStringSet("bookmarks", bookmarkSet);
        editor.putStringSet("excluded", excludedSet);

        // 3. 완료
        editor.apply();
    }
}