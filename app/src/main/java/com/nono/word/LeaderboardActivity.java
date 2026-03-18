package com.nono.word;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private TextView tabChoseong, tabLiteracy, tab1Min, tab3Min;
    private LinearLayout layoutRankingList, layoutMyScore;
    private ProgressBar progressLoading;
    private TextView tvEmpty, tvMyNickname, tvMyScore;

    private FirebaseLeaderboard leaderboard;
    private boolean isChoseongTab = false;
    private boolean is1MinTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboard = new FirebaseLeaderboard();

        tabChoseong = findViewById(R.id.tab_choseong);
        tabLiteracy = findViewById(R.id.tab_literacy);
        tab1Min = findViewById(R.id.tab_1min);
        tab3Min = findViewById(R.id.tab_3min);
        layoutRankingList = findViewById(R.id.layout_ranking_list);
        progressLoading = findViewById(R.id.progress_loading);
        tvEmpty = findViewById(R.id.tv_empty);
        layoutMyScore = findViewById(R.id.layout_my_score);
        tvMyNickname = findViewById(R.id.tv_my_nickname);
        tvMyScore = findViewById(R.id.tv_my_score);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabChoseong.setOnClickListener(v -> switchModeTab(true));
        tabLiteracy.setOnClickListener(v -> switchModeTab(false));
        tab1Min.setOnClickListener(v -> switchTimeTab(true));
        tab3Min.setOnClickListener(v -> switchTimeTab(false));

        // 광고 로드
        AdView adViewBottom = findViewById(R.id.adViewBottom);
        if (BillingManager.isAdsRemoved(this)) {
            adViewBottom.setVisibility(View.GONE);
        } else {
            adViewBottom.loadAd(new AdRequest.Builder().build());
        }

        // 더미 데이터 삭제
        deleteDummyData();

        // 기본: 문해력 테스트 1분
        switchModeTab(false);
    }

    private void deleteDummyData() {
        String[] collections = {
                Constants.COLLECTION_LEADERBOARD_LITERACY_1MIN,
                Constants.COLLECTION_LEADERBOARD_LITERACY_3MIN,
                Constants.COLLECTION_LEADERBOARD_CHOSEONG_1MIN,
                Constants.COLLECTION_LEADERBOARD_CHOSEONG_3MIN
        };
        String[] names = {"테스트유저A", "테스트유저B", "테스트유저C"};
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (String col : collections) {
            for (String name : names) {
                db.collection(col).document("dummy_" + name).delete();
            }
        }
    }


    private void switchModeTab(boolean choseong) {
        isChoseongTab = choseong;

        tabChoseong.setBackgroundResource(choseong ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabChoseong.setTextColor(choseong ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabLiteracy.setBackgroundResource(choseong ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
        tabLiteracy.setTextColor(choseong ? Color.parseColor("#94A3B8") : Color.WHITE);

        loadCurrentRanking();
    }

    private void switchTimeTab(boolean oneMin) {
        is1MinTab = oneMin;

        tab1Min.setBackgroundColor(oneMin ? Color.parseColor("#E8E8EC") : Color.TRANSPARENT);
        tab1Min.setTextColor(oneMin ? Color.parseColor("#334155") : Color.parseColor("#94A3B8"));

        tab3Min.setBackgroundColor(oneMin ? Color.TRANSPARENT : Color.parseColor("#E8E8EC"));
        tab3Min.setTextColor(oneMin ? Color.parseColor("#94A3B8") : Color.parseColor("#334155"));

        loadCurrentRanking();
    }

    private String getCurrentCollection() {
        if (isChoseongTab) {
            return is1MinTab
                    ? Constants.COLLECTION_LEADERBOARD_CHOSEONG_1MIN
                    : Constants.COLLECTION_LEADERBOARD_CHOSEONG_3MIN;
        } else {
            return is1MinTab
                    ? Constants.COLLECTION_LEADERBOARD_LITERACY_1MIN
                    : Constants.COLLECTION_LEADERBOARD_LITERACY_3MIN;
        }
    }

    private void loadCurrentRanking() {
        String collection = getCurrentCollection();
        loadRanking(collection);
        loadMyScore(collection);
    }

    private void loadRanking(String collection) {
        layoutRankingList.removeAllViews();
        tvEmpty.setVisibility(View.GONE);
        progressLoading.setVisibility(View.VISIBLE);

        leaderboard.getTopScores(collection, 100, entries -> {
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

    private void loadMyScore(String collection) {
        leaderboard.getMyScore(collection, (nickname, score) -> {
            if (nickname != null && score > 0) {
                layoutMyScore.setVisibility(View.VISIBLE);
                tvMyNickname.setText(nickname);
                tvMyScore.setText(score + "점");
            } else {
                layoutMyScore.setVisibility(View.GONE);
            }
        });
    }
}
