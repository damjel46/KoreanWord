package com.nono.word;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private TextView tabChoseong, tabLiteracy;
    private LinearLayout layoutRankingList;
    private ProgressBar progressLoading;
    private TextView tvEmpty;

    private FirebaseLeaderboard leaderboard;
    private boolean isChoseongTab = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboard = new FirebaseLeaderboard();

        tabChoseong = findViewById(R.id.tab_choseong);
        tabLiteracy = findViewById(R.id.tab_literacy);
        layoutRankingList = findViewById(R.id.layout_ranking_list);
        progressLoading = findViewById(R.id.progress_loading);
        tvEmpty = findViewById(R.id.tv_empty);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabChoseong.setOnClickListener(v -> switchTab(true));
        tabLiteracy.setOnClickListener(v -> switchTab(false));

        // 기본: 문해력 테스트 탭
        switchTab(false);
    }

    private void switchTab(boolean choseong) {
        isChoseongTab = choseong;

        tabChoseong.setBackgroundResource(choseong ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabChoseong.setTextColor(choseong ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabLiteracy.setBackgroundResource(choseong ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
        tabLiteracy.setTextColor(choseong ? Color.parseColor("#94A3B8") : Color.WHITE);

        String collection = choseong
                ? Constants.COLLECTION_LEADERBOARD_CHOSEONG
                : Constants.COLLECTION_LEADERBOARD_LITERACY;
        loadRanking(collection);
    }

    private void loadRanking(String collection) {
        layoutRankingList.removeAllViews();
        tvEmpty.setVisibility(View.GONE);
        progressLoading.setVisibility(View.VISIBLE);

        leaderboard.getTopScores(collection, 50, entries -> {
            progressLoading.setVisibility(View.GONE);

            if (entries.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }

            for (FirebaseLeaderboard.LeaderboardEntry entry : entries) {
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_leaderboard, layoutRankingList, false);

                TextView tvRank = itemView.findViewById(R.id.tv_rank);
                TextView tvNickname = itemView.findViewById(R.id.tv_nickname);
                TextView tvScore = itemView.findViewById(R.id.tv_score);

                // 상위 3등 특별 표시
                if (entry.rank == 1) {
                    tvRank.setText("🥇");
                } else if (entry.rank == 2) {
                    tvRank.setText("🥈");
                } else if (entry.rank == 3) {
                    tvRank.setText("🥉");
                } else {
                    tvRank.setText(String.valueOf(entry.rank));
                }

                tvNickname.setText(entry.nickname);
                tvScore.setText(entry.score + "점");

                layoutRankingList.addView(itemView);
            }
        });
    }
}
