package com.nono.word;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView tvBestScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvBestScore = findViewById(R.id.tv_best_score);

        Button btnRandomAll = findViewById(R.id.btn_random_all);
        Button btnOnlyBookmark = findViewById(R.id.btn_only_bookmark);
        Button btnGroup1 = findViewById(R.id.btn_group1);
        Button btnGroup2 = findViewById(R.id.btn_group2);
        Button btnGroup3 = findViewById(R.id.btn_group3);
        Button btnTrash = findViewById(R.id.btn_trash);

        // ★ [추가] 전체 랜덤 (그룹 0, 즐겨찾기 모드 false)
        btnRandomAll.setOnClickListener(v -> startGame(0, false));

        // ★ [추가] 즐겨찾기만 (그룹 0, 즐겨찾기 모드 true)
        btnOnlyBookmark.setOnClickListener(v -> startGame(0, true));

        // 그룹별 학습 (즐겨찾기 모드 false)
        btnGroup1.setOnClickListener(v -> startGame(1, false));
        btnGroup2.setOnClickListener(v -> startGame(2, false));
        btnGroup3.setOnClickListener(v -> startGame(3, false));
        btnTrash.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TrashActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBestScore();
    }

    private void loadBestScore() {
        SharedPreferences prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
        int bestScore = prefs.getInt("bestStreak", 0);
        tvBestScore.setText("내 최고 연속 점수 : " + bestScore + " 🔥");
    }

    // ★ [수정] 파라미터 추가: isBookmarkMode
    private void startGame(int groupNumber, boolean isBookmarkMode) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("selected_group", groupNumber);

        // 즐겨찾기 모드인지 여부를 같이 보냄
        intent.putExtra("bookmark_mode", isBookmarkMode);

        startActivity(intent);
    }
}