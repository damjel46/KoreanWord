package com.nono.word;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

public class HomeActivity extends AppCompatActivity {

    // UI 변수
    private TextView tvBestScore, tvBest1Min, tvBest3Min;
    private Button btnGroup1, btnGroup2, btnGroup3, btnRandomAll, btnChallenge1Min, btnChallenge3Min;
    private ImageButton btnOnlyBookmark, btnTrash, btnRanking;

    private WordRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Play Games SDK 초기화
        PlayGamesSdk.initialize(this);

        setContentView(R.layout.activity_home);

        repository = new WordRepository(this);

        // 2. UI 연결
        initViews();

        // 3. 버튼 기능 설정
        setupListeners();

        // 4. Play Games 자동 로그인 시도
        PlayGames.getGamesSignInClient(this).isAuthenticated().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().isAuthenticated()) {
                // 로그인 성공
            } else {
                // 로그인 실패 (설정 확인 필요)
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScores();
    }

    private void initViews() {
        tvBestScore = findViewById(R.id.tv_best_score);
        tvBest1Min = findViewById(R.id.tv_best_1min);
        tvBest3Min = findViewById(R.id.tv_best_3min);
        btnChallenge1Min = findViewById(R.id.btn_challenge_1min);
        btnChallenge3Min = findViewById(R.id.btn_challenge_3min);

        btnGroup1 = findViewById(R.id.btn_group1);
        btnGroup2 = findViewById(R.id.btn_group2);
        btnGroup3 = findViewById(R.id.btn_group3);
        btnRandomAll = findViewById(R.id.btn_random_all);
        btnChallenge3Min = findViewById(R.id.btn_challenge_3min);

        btnOnlyBookmark = findViewById(R.id.btn_only_bookmark);
        btnTrash = findViewById(R.id.btn_trash);

        // ★ 랭킹 버튼 연결
        btnRanking = findViewById(R.id.btn_ranking);
    }

    private void setupListeners() {
        // 그룹별 학습
        btnGroup1.setOnClickListener(v -> startMainActivity(1, false));
        btnGroup2.setOnClickListener(v -> startMainActivity(2, false));
        btnGroup3.setOnClickListener(v -> startMainActivity(3, false));

        // 전체 랜덤
        btnRandomAll.setOnClickListener(v -> startMainActivity(0, false));

        // 즐겨찾기만
        btnOnlyBookmark.setOnClickListener(v -> startMainActivity(0, true));
        // 1분 챌린지
        btnChallenge1Min.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TimeAttackActivity.class);
            intent.putExtra("TIME_LIMIT", Constants.TIME_LIMIT_1MIN);
            startActivity(intent);
        });
        // 3분 챌린지
        btnChallenge3Min.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TimeAttackActivity.class);
            intent.putExtra("TIME_LIMIT", Constants.TIME_LIMIT_3MIN);
            startActivity(intent);
        });

        // 휴지통
        btnTrash.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TrashActivity.class);
            startActivity(intent);
        });

        btnRanking.setOnClickListener(v -> showLeaderboard());
    }

    private void showLeaderboard() {
        try {
            PlayGames.getLeaderboardsClient(this)
                    .getLeaderboardIntent(Constants.LEADERBOARD_ID)
                    .addOnSuccessListener(intent -> startActivityForResult(intent, 9004))
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadScores() {
        tvBestScore.setText("연속 정답 기록 : " + repository.getBestStreak() + " 🔥");
        tvBest1Min.setText("Best: " + repository.getChallengeScore(Constants.TIME_LIMIT_1MIN));
        tvBest3Min.setText("Best: " + repository.getChallengeScore(Constants.TIME_LIMIT_3MIN));
    }

    private void startMainActivity(int groupNumber, boolean isBookmarkMode) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("selected_group", groupNumber);
        intent.putExtra("bookmark_mode", isBookmarkMode);
        startActivity(intent);
    }
}