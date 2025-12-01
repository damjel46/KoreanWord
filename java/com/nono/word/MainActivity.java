package com.nono.word;
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
    private List<WordItem> quizList = new ArrayList<>();
    private boolean isHintUsed = false;
    private TextView tvCount;


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
        tvCount = findViewById(R.id.tv_count);

        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
        readCsvFile();
        loadRandomQuestion();
        btnBack.setOnClickListener(v -> finish());
        btnHint.setOnClickListener(v -> {
            tvExample.setVisibility(View.VISIBLE);
            tvFeedback.setText("");
            isHintUsed = true; // 힌트 봤음 체크
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
        // 1. 현재 모드(전체/즐겨찾기)에 맞는 원본 리스트 가져오기
        List<WordItem> originalTargetList = getCurrentTargetList();

        // 2. 풀고 있는 문제 리스트(quizList)가 비었을 때 처리
        if (quizList.isEmpty()) {

            // 2-1. 애초에 원본 데이터가 없는 경우 (즐겨찾기 0개 등)
            if (originalTargetList.isEmpty()) {
                if (isBookmarkMode) {
                    tvInitial.setText("텅");
                    tvMean.setText("즐겨찾기한 단어가 없습니다.");
                    tvFeedback.setText("다른 모드에서 별표를 추가해보세요!");
                } else {
                    tvInitial.setText("끝");
                    tvMean.setText("해당 그룹의 단어가 없습니다.");
                    tvFeedback.setText("단어 데이터가 비어있습니다.");
                }

                // UI 잠금 및 초기화
                tvCount.setText("- / -");
                tvExample.setText("");
                tvExample.setVisibility(View.GONE);

                etAnswer.setEnabled(false);
                btnSubmit.setEnabled(false);
                btnReveal.setEnabled(false);
                btnHint.setEnabled(false);
                btnExclude.setEnabled(false);
                btnBookmark.setEnabled(false);
                btnRestart.setVisibility(View.GONE);

                currentItem = null;
                return;
            }

            // 2-2. 한 바퀴 다 돌아서 비게 된 경우 vs 처음 시작하는 경우
            if (currentItem == null) {
                // [처음 시작] 원본에서 복사해오고 강력하게 섞기
                quizList.addAll(originalTargetList);
                Collections.shuffle(quizList, new Random(System.nanoTime()));
            } else {
                // [완료] 문제를 다 푼 경우
                tvInitial.setText("완료");
                tvMean.setText("모든 단어를 마스터했습니다!");
                tvFeedback.setText("정말 대단해요! 🥳");

                tvCount.setText("완료!");
                tvExample.setText("");
                tvExample.setVisibility(View.GONE);

                etAnswer.setEnabled(false);
                btnSubmit.setEnabled(false);
                btnReveal.setEnabled(false);
                btnHint.setEnabled(false);
                btnExclude.setEnabled(false);
                btnBookmark.setEnabled(false);

                // 다시 시작 버튼 보여주기
                btnRestart.setVisibility(View.VISIBLE);
                return;
            }
        }

        // 3. 정상 진행: UI 활성화
        etAnswer.setEnabled(true);
        btnSubmit.setEnabled(true);
        btnReveal.setEnabled(true);
        btnHint.setEnabled(true);
        btnExclude.setEnabled(true);
        btnBookmark.setEnabled(true);
        btnRestart.setVisibility(View.GONE);

        // 4. 상태 플래그 초기화 (새 문제니까 안 본 상태로)
        isAnswerRevealed = false;
        isHintUsed = false;

        // 5. ★ [문제 수 계산 로직]
        // 전체 개수(원본 사이즈) - 남은 개수(퀴즈리스트 사이즈) + 1
        int totalCount = originalTargetList.size();
        int remainCount = quizList.size();
        int currentNum = totalCount - remainCount + 1;

        tvCount.setText(currentNum + " / " + totalCount);

        // 6. ★ [문제 출제] 섞인 리스트의 맨 앞(0번째) 문제를 가져옴
        currentItem = quizList.get(0);

        // 7. 화면에 표시
        tvStreak.setText("연속 : " + streakCount + " 🔥");
        tvInitial.setText(currentItem.getInitial());
        tvMean.setText(currentItem.getMean());
        tvExample.setText(currentItem.getExample());

        updateBookmarkIcon();

        // 기타 UI 정리
        tvExample.setVisibility(View.GONE);
        etAnswer.setText("");
        tvFeedback.setText("");

        // 다크모드 대응 텍스트 색상 및 테두리 초기화
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        layoutCard.setBackgroundResource(R.drawable.bg_border_purple);
    }
    private void resetGame() {
        // 기존: wordList 섞기 -> 변경: quizList 재충전

        quizList.clear(); // 현재 리스트 비우고
        List<WordItem> target = getCurrentTargetList();

        if (!target.isEmpty()) {
            quizList.addAll(target); // 다시 꽉 채우기
            Collections.shuffle(quizList, new Random(System.nanoTime()));
        }

        streakCount = 0;
        tvStreak.setText("연속 : 0");

        tvFeedback.setText("순서를 섞어서 다시 시작합니다! 🔄");
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_info));

        // currentItem을 null로 만들어서 loadRandomQuestion에서 튕기지 않게 함
        currentItem = null;
        loadRandomQuestion(); // 여기서 quizList가 찼으니까 정상 실행됨
    }

    private void excludeCurrentWord() {
        if (currentItem == null) return;
        String wordToExclude = currentItem.getWord();
        excludedSet.add(wordToExclude);
        // 리스트에서 영구 삭제
        wordList.remove(currentItem);
        bookmarkedList.remove(currentItem);
        quizList.remove(currentItem);
        // 핸드폰에 저장
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
        quizList.clear();
        // (저장된 게 없으면 빈 목록을 가져옵니다)
        if (prefs == null) prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
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
                    String initial = tokens[0].trim();
                    String wordName = tokens[1].trim(); // 정답 단어 (고유 키로 사용)

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
            Collections.shuffle(wordList, new Random(System.nanoTime()));

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
            String message;
            int color;

            // ★ [수정] 정답 보기(Reveal) OR 힌트(Hint)를 사용했는지 체크
            if (isAnswerRevealed || isHintUsed) {
                message = "도움 받고 정답! (다음에 다시 나옵니다)";
                color = ContextCompat.getColor(this, R.color.feedback_pass);

                // ★ 도움을 받았으므로 quizList에서 삭제하지 않음 -> 맨 뒤로 보내거나 섞어서 나중에 또 나오게 함
                quizList.remove(currentItem);
                quizList.add(currentItem); // 맨 뒤로 보내기 (혹은 shuffle)
                // Collections.shuffle(quizList); // 원하면 다시 섞어서 언제 나올지 모르게 함

            } else {
                streakCount++;
                updateBestScore(streakCount);
                message = "스스로 정답! 👏 (완벽하게 익혔네요)";
                color = ContextCompat.getColor(this, R.color.feedback_success);

                // ★ [핵심] 스스로 맞췄으므로 리스트에서 영구 제거 (이번 판에서 안 나옴)
                quizList.remove(currentItem);
            }
            // 다음 문제 로드 (quizList가 줄어든 상태로 로드됨)
            loadRandomQuestion();

            if (currentItem != null || !quizList.isEmpty()) {
                tvFeedback.setText(message);
                tvFeedback.setTextColor(color);
                tvFeedback.postDelayed(() -> tvFeedback.setText(""), 1000); // 1초 뒤 삭제
            }

        } else {
            // 오답 로직 (기존 동일)
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
    // 데이터를 폰 내부에 저장하는 함수
    private void saveData() {
        if (prefs == null) return;

        SharedPreferences.Editor editor = prefs.edit();

        // 1. 즐겨찾기 목록 저장
        Set<String> bookmarkSet = new HashSet<>();
        for (WordItem item : bookmarkedList) {
            bookmarkSet.add(item.getWord());
        }
        editor.putStringSet("bookmarks", bookmarkSet);

        // 2. 제외 목록 저장 \\
        // excludedSet은 excludeCurrentWord()에서 이미 추가되어 있음
        editor.putStringSet("excluded", excludedSet);

        // 3. 저장 실행
        editor.apply();
    }
}