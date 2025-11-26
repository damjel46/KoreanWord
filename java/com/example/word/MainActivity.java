package com.example.word;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
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
    private int selectedGroup = 0;
    private Button btnHint, btnReveal, btnSubmit, btnRestart; // btnRestart 추가
    private ImageButton btnBookmark, btnExclude;
    private Button btnBack; // 뒤로가기
    private SwitchMaterial switchMode;

    private FrameLayout layoutCard;

    private List<WordItem> wordList = new ArrayList<>();
    private List<WordItem> bookmarkedList = new ArrayList<>();

    private WordItem currentItem;
    private boolean isAnswerRevealed = false;
    private int streakCount = 0;
    private Random random = new Random();
    private SharedPreferences prefs;
    private boolean isBookmarkMode = false;
    private Set<String> excludedSet = new HashSet<>();
    private AdView adViewTop, adViewBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, initializationStatus -> {});
        selectedGroup = getIntent().getIntExtra("selected_group", 0);
        isBookmarkMode = getIntent().getBooleanExtra("bookmark_mode", false);
        // UI 연결
        adViewTop = findViewById(R.id.adViewTop);
        adViewBottom = findViewById(R.id.adViewBottom);
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
        btnBack = findViewById(R.id.btn_back);
        btnExclude = findViewById(R.id.btn_exclude);

        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
        readCsvFile();
        loadRandomQuestion();
        btnBack.setOnClickListener(v -> finish());
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
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTop.loadAd(adRequest);
        adViewBottom.loadAd(adRequest);

    }

    private List<WordItem> getCurrentTargetList() {
        if (isBookmarkMode) {
            return bookmarkedList;
        } else {
            return wordList;
        }
    }
    private void loadRandomQuestion() {
        List<WordItem> targetList = getCurrentTargetList();

        if (targetList.isEmpty()) {
            if (isBookmarkMode) {
                tvInitial.setText("텅");
                tvMean.setText("즐겨찾기한 단어가 없습니다.");
                tvFeedback.setText("다른 모드에서 별표를 추가해보세요!");
                btnRestart.setVisibility(View.GONE);
            } else {
                tvInitial.setText("끝");
                tvMean.setText("해당 그룹의 단어를 모두 마스터했습니다!");
                tvFeedback.setText("정말 대단해요! 🥳");
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

        tvFeedback.setText("순서를 섞어서 다시 시작합니다! 🔄");
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_info));

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
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_pass));
        }
    }

    private void readCsvFile() { //파일 읽어오기
        wordList.clear();
        bookmarkedList.clear();
        excludedSet = new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
        Set<String> savedBookmarks = prefs.getStringSet("bookmarks", new HashSet<>());
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("word.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    String initial = tokens[0];
                    String wordName = tokens[1]; // 정답 단어 (고유 키로 사용)

                    if (excludedSet.contains(wordName)) {
                        continue;
                    }
                    if (!isWordInGroup(initial)) {
                        continue; // 그룹에 안 맞으면 건너뜀
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

    private boolean isWordInGroup(String initial) {
        if (selectedGroup == 0) return true; // 0이면 전체(혹은 에러 방지용)
        if (initial == null || initial.isEmpty()) return false;

        // 초성의 '첫 글자'만 확인 (예: "ㄱㅁ" -> 'ㄱ')
        char firstChar = initial.charAt(0);
        String checkString = String.valueOf(firstChar);

        if (selectedGroup == 1) {
            // 그룹 1: ㄱ ㄴ ㄷ ㄹ ㅁ
            return "ㄱㄴㄷㄹㅁ".contains(checkString);
        } else if (selectedGroup == 2) {
            // 그룹 2: ㅂ ㅅ ㅇ ㅈ ㅊ
            return "ㅂㅅㅇㅈㅊ".contains(checkString);
        } else if (selectedGroup == 3) {
            // 그룹 3: ㅋ ㅌ ㅍ ㅎ
            return "ㅋㅌㅍㅎ".contains(checkString);
        }
        return false;
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
                color = ContextCompat.getColor(this, R.color.feedback_pass);
            } else {
                streakCount++;
                message = "정답입니다! 👏";
                color = ContextCompat.getColor(this, R.color.feedback_success);
            }
            if (!isAnswerRevealed) { // 스스로 맞춘 경우만
                updateBestScore(streakCount);
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
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_error));

            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            layoutCard.startAnimation(shake);
            layoutCard.setBackgroundResource(R.drawable.bg_border_red);
            etAnswer.setText("");
            etAnswer.requestFocus();
            layoutCard.postDelayed(() -> layoutCard.setBackgroundResource(R.drawable.bg_border_purple), 500);
        }
    }
    private void updateBestScore(int currentScore) {
        int bestScore = prefs.getInt("bestStreak", 0); // 기존 최고점 가져오기

        if (currentScore > bestScore) {
            // 신기록이면 저장!
            prefs.edit().putInt("bestStreak", currentScore).apply();
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