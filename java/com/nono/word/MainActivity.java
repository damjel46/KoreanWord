package com.nono.word;

import android.content.SharedPreferences;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI 변수 선언
    private TextView tvInitial, tvMean, tvExample, tvStreak, tvFeedback, tvCount;
    private EditText etAnswer;
    private Button btnBack, btnHint, btnReveal, btnSubmit, btnRestart;
    private ImageButton btnBookmark, btnExclude;
    private FrameLayout layoutCard;
    private AdView adViewTop;

    // 데이터 관리 리스트
    private List<WordItem> wordList = new ArrayList<>();       // 전체 단어장
    private List<WordItem> bookmarkedList = new ArrayList<>(); // 즐겨찾기 단어장
    private List<WordItem> quizList = new ArrayList<>();       // 현재 진행 중인 퀴즈 큐 (중복 방지용)

    // 상태 및 설정 변수
    private WordItem currentItem;
    private SharedPreferences prefs;
    private Set<String> excludedSet = new HashSet<>();

    private int selectedGroup = 0;
    private boolean isBookmarkMode = false;
    private boolean isAnswerRevealed = false;
    private boolean isHintUsed = false;
    private int streakCount = 0;
    private TextView tvTimer; // 타이머 텍스트뷰
    private int secondsElapsed = 0; // 지난 시간 (초)
    private boolean isTimerRunning = false;
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 구글 애드몹 초기화
        MobileAds.initialize(this, initializationStatus -> {});

        // 2. Intent 데이터 수신 (그룹 정보, 즐겨찾기 모드 여부)
        selectedGroup = getIntent().getIntExtra("selected_group", 0);
        isBookmarkMode = getIntent().getBooleanExtra("bookmark_mode", false);

        // 3. 내부 저장소 연결
        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);

        // 4. UI 연결
        initViews();
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewTop.loadAd(adRequest);

        // 5. 데이터 로딩 (CSV 읽기 & 필터링)
        readCsvFile();

        // 6. 첫 문제 로드
        loadRandomQuestion();

        // 7. 버튼 리스너 설정
        setupListeners();
        // 8. 타이머 시작
        startStudyTimer();
    }

    // UI 구성요소 연결 메소드
    private void initViews() {
        tvCount = findViewById(R.id.tv_count);
        tvInitial = findViewById(R.id.tv_initial);
        tvMean = findViewById(R.id.tv_mean);
        tvExample = findViewById(R.id.tv_example);
        tvStreak = findViewById(R.id.tv_streak);
        tvFeedback = findViewById(R.id.tv_feedback);

        etAnswer = findViewById(R.id.et_answer);
        layoutCard = findViewById(R.id.layout_card);

        btnBack = findViewById(R.id.btn_back);
        btnHint = findViewById(R.id.btn_hint);
        btnReveal = findViewById(R.id.btn_reveal);
        btnSubmit = findViewById(R.id.btn_submit);
        btnRestart = findViewById(R.id.btn_restart);

        btnBookmark = findViewById(R.id.btn_bookmark);
        btnExclude = findViewById(R.id.btn_exclude);

        adViewTop = findViewById(R.id.adViewTop);
        tvTimer = findViewById(R.id.tv_timer);
    }

    // 버튼 클릭 이벤트 설정 메소드
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnHint.setOnClickListener(v -> {
            tvExample.setVisibility(View.VISIBLE);
            tvFeedback.setText("");
            isHintUsed = true;
        });

        btnReveal.setOnClickListener(v -> showCorrectAnswer());
        btnSubmit.setOnClickListener(v -> checkAnswer());

        btnBookmark.setOnClickListener(v -> toggleBookmark());
        btnExclude.setOnClickListener(v -> excludeCurrentWord());
        btnRestart.setOnClickListener(v -> resetGame());

        etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                checkAnswer();
                return true;
            }
            return false;
        });
    }

    // CSV 파일 읽기 및 데이터 세팅
    private void readCsvFile() {
        wordList.clear();
        bookmarkedList.clear();
        quizList.clear();

        // 저장소 데이터 불러오기
        excludedSet = new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
        Set<String> savedBookmarks = prefs.getStringSet("bookmarks", new HashSet<>());

        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("word.csv"); // 파일명 확인
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            reader.readLine(); // 헤더 건너뛰기

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    String initial = tokens[0].trim();
                    String wordName = tokens[1].trim();

                    // 제외된 단어면 건너뛰기
                    if (excludedSet.contains(wordName)) continue;

                    // 그룹 필터링
                    if (!isWordInGroup(initial)) continue;

                    WordItem item = new WordItem(tokens[0], wordName, tokens[2], tokens[3]);

                    // 즐겨찾기 상태 복구
                    if (savedBookmarks.contains(wordName)) {
                        item.setBookmarked(true);
                        bookmarkedList.add(item);
                    }

                    wordList.add(item);
                }
            }
            // 강력한 랜덤 셔플
            Collections.shuffle(wordList, new Random(System.nanoTime()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 현재 모드에 맞는 리스트 반환
    private List<WordItem> getCurrentTargetList() {
        return isBookmarkMode ? bookmarkedList : wordList;
    }

    // 랜덤 문제 출제 로직
    private void loadRandomQuestion() {
        List<WordItem> originalTargetList = getCurrentTargetList();

        // 퀴즈 큐가 비었을 때 처리
        if (quizList.isEmpty()) {
            // 원본도 비어있음 (데이터 없음)
            if (originalTargetList.isEmpty()) {
                if (isBookmarkMode) {
                    tvInitial.setText("텅");
                    tvMean.setText("즐겨찾기한 단어가 없습니다.");
                    tvFeedback.setText("다른 모드에서 별표를 추가해보세요!");
                } else {
                    tvInitial.setText("끝");
                    tvMean.setText("해당 그룹의 단어를 모두 마스터했습니다!");
                    tvFeedback.setText("정말 대단해요! 🥳");
                }

                // UI 초기화
                tvCount.setText("- / -");
                tvExample.setVisibility(View.GONE);
                setGameUIEnabled(false);
                btnRestart.setVisibility(View.GONE);
                currentItem = null;
                return;
            }

            // 처음 시작하거나 리필할 때
            if (currentItem == null) {
                quizList.addAll(originalTargetList);
                Collections.shuffle(quizList, new Random(System.nanoTime()));
            } else {
                // 문제를 다 푼 경우
                tvInitial.setText("완료");
                tvMean.setText("학습 가능한 단어를 모두 풀었습니다!");
                tvFeedback.setText("대단해요! 🔄 버튼을 눌러 다시 시작하세요.");

                tvCount.setText("완료!");
                tvExample.setVisibility(View.GONE);
                setGameUIEnabled(false);
                btnRestart.setVisibility(View.VISIBLE);
                return;
            }
        }

        // 정상 게임 진행
        setGameUIEnabled(true);
        btnRestart.setVisibility(View.GONE);

        // 상태 초기화
        isAnswerRevealed = false;
        isHintUsed = false;

        // 문제 수 계산 (전체 - 남은거 + 1)
        int totalCount = originalTargetList.size();
        int remainCount = quizList.size();
        int currentNum = totalCount - remainCount + 1;
        tvCount.setText(currentNum + " / " + totalCount);

        // 문제 가져오기 (0번째)
        currentItem = quizList.get(0);

        // 화면 표시
        tvStreak.setText("연속 : " + streakCount+ " 🔥");
        tvInitial.setText(currentItem.getInitial());
        tvMean.setText(currentItem.getMean());
        tvExample.setText(currentItem.getExample());

        updateBookmarkIcon();

        tvExample.setVisibility(View.GONE);
        etAnswer.setText("");
        tvFeedback.setText("");
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        layoutCard.setBackgroundResource(R.drawable.bg_border_purple);
    }

    private void checkAnswer() {
        if (currentItem == null) return;
        String userAnswer = etAnswer.getText().toString().trim();
        String correctAnswer = currentItem.getWord();

        if (userAnswer.equals(correctAnswer)) {
            // [정답]
            String message;
            int color;

            if (isAnswerRevealed || isHintUsed) {
                message = "도움 받고 정답! (다음에 다시 나옵니다)";
                color = ContextCompat.getColor(this, R.color.feedback_pass);

                // 도움 받았으면 맨 뒤로 보내기 (삭제X)
                quizList.remove(currentItem);
                quizList.add(currentItem);
            } else {
                message = "스스로 정답! 👏";
                color = ContextCompat.getColor(this, R.color.feedback_success);

                streakCount++;
                updateBestScore(streakCount); // 최고점수 갱신

                // 스스로 맞췄으면 삭제 (이번 판 안 나옴)
                quizList.remove(currentItem);
            }

            loadRandomQuestion(); // 다음 문제 로드

            if (currentItem != null || !quizList.isEmpty()) {
                tvFeedback.setText(message);
                tvFeedback.setTextColor(color);
                tvFeedback.postDelayed(() -> tvFeedback.setText(""), 1000);
            }

        } else {
            // [오답]
            streakCount = 0;
            tvStreak.setText("연속 : " + streakCount+ " 🔥");
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

    private void showCorrectAnswer() {
        if (currentItem == null) return;
        isAnswerRevealed = true;

        streakCount = 0;
        tvStreak.setText("연속 : " + streakCount);

        etAnswer.setText(currentItem.getWord());
        tvFeedback.setText("정답 확인 (점수 없음)");
        tvFeedback.setTextColor(Color.parseColor("#FF6F61"));
    }

    // 그룹 필터링 로직
    private boolean isWordInGroup(String initial) {
        if (selectedGroup == 0) return true;
        if (initial == null || initial.isEmpty()) return false;

        char firstChar = initial.charAt(0);
        String checkString = String.valueOf(firstChar);

        if (selectedGroup == 1) return "ㄱㄴㄷㄹㅁ".contains(checkString);
        else if (selectedGroup == 2) return "ㅂㅅㅇㅈㅊ".contains(checkString);
        else if (selectedGroup == 3) return "ㅋㅌㅍㅎ".contains(checkString);

        return false;
    }

    // 게임 재시작
    private void resetGame() {
        quizList.clear();
        List<WordItem> target = getCurrentTargetList();

        if (!target.isEmpty()) {
            quizList.addAll(target);
            Collections.shuffle(quizList, new Random(System.nanoTime()));
        }

        streakCount = 0;
        tvStreak.setText("연속 : 0");
        tvFeedback.setText("순서를 섞어서 다시 시작합니다! 🔄");
        tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_info));

        currentItem = null;
        loadRandomQuestion();
    }

    // 단어 제외 (휴지통)
    private void excludeCurrentWord() {
        if (currentItem == null) return;

        String wordToExclude = currentItem.getWord();
        excludedSet.add(wordToExclude); // 제외 목록에 추가

        wordList.remove(currentItem);
        bookmarkedList.remove(currentItem);
        quizList.remove(currentItem);

        saveData(); // 저장
        loadRandomQuestion(); // 다음 문제

        if (currentItem != null) {
            tvFeedback.setText("단어장에서 제외했습니다. 👋");
            tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.feedback_pass));
        }
    }

    // 즐겨찾기 토글
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

    // 데이터 저장
    private void saveData() {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> bookmarkSet = new HashSet<>();
        for (WordItem item : bookmarkedList) {
            bookmarkSet.add(item.getWord());
        }
        editor.putStringSet("bookmarks", bookmarkSet);
        editor.putStringSet("excluded", excludedSet);
        editor.apply();
    }

    // 최고 점수 갱신
    private void updateBestScore(int currentScore) {
        int bestScore = prefs.getInt("bestStreak", 0);
        if (currentScore > bestScore) {
            prefs.edit().putInt("bestStreak", currentScore).apply();
        }
    }

    // 아이콘 업데이트
    private void updateBookmarkIcon() {
        if (currentItem != null) {
            if (currentItem.isBookmarked()) btnBookmark.setImageResource(R.drawable.ic_star_filled);
            else btnBookmark.setImageResource(R.drawable.ic_star_border);
        }
    }

    // UI 활성/비활성 처리 헬퍼
    private void setGameUIEnabled(boolean enabled) {
        etAnswer.setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
        btnReveal.setEnabled(enabled);
        btnHint.setEnabled(enabled);
        btnExclude.setEnabled(enabled);
        btnBookmark.setEnabled(enabled);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStudyTimer();
    }
    private void startStudyTimer() {
        secondsElapsed = 0; // 0초로 초기화
        isTimerRunning = true;

        // 타이머 로직 정의
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTimerRunning) return;

                // 시간 포맷 (분:초)
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                String timeString = String.format("%02d:%02d", minutes, seconds);

                // 화면 업데이트
                if (tvTimer != null) {
                    tvTimer.setText(timeString);
                }

                secondsElapsed++; // 1초 증가

                // 1초 뒤에 다시 이 코드를 실행 (무한 반복)
                timerHandler.postDelayed(this, 1000);
            }
        };

        // 타이머 가동
        timerHandler.post(timerRunnable);
    }

    private void stopStudyTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }
}