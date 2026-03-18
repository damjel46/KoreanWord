package com.nono.word;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

public class HomeActivity extends AppCompatActivity {

    // UI 변수
    private TextView tvBestScore, tvBestScoreChoseong, tvBest1Min, tvBest3Min;
    private TextView tvBestLiteracy1Min, tvBestLiteracy3Min;
    private View btnGroup1, btnGroup2, btnGroup3, btnRandomAll, btnChallenge1Min, btnChallenge3Min;
    private View btnOnlyBookmark, btnTrash, btnRanking;
    private TextView btnRemoveAds;
    private AdView adViewHome;
    private BillingManager billingManager;

    // 탭 관련
    private TextView tabChoseong, tabLiteracy;
    private View containerChoseong, containerLiteracy;
    private View btnLiteracyRandom, btnLitVocab, btnLitSpelling, btnLitIdiom;
    private View btnLitReading, btnLitGrammar, btnLitPractical, btnLitTerms, btnLitProverb;
    private View btnLiteracyChallenge1Min, btnLiteracyChallenge3Min;
    private NestedScrollView scrollView;

    private boolean isChoseongTab = false; // 현재 탭: true = 초성, false = 문해력

    private WordRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PlayGamesSdk.initialize(this);
        setContentView(R.layout.activity_home);

        repository = new WordRepository(this);

        initViews();
        setupListeners();

        // 결제 매니저
        billingManager = new BillingManager(this);
        billingManager.setOnPurchaseListener(this::hideAllAds);

        if (billingManager.isAdsRemoved()) {
            hideAllAds();
        } else {
            adViewHome.loadAd(new AdRequest.Builder().build());
        }

        // 기본 탭을 문해력 테스트로 설정
        switchTab(false);

        PlayGames.getGamesSignInClient(this).isAuthenticated().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().isAuthenticated()) {
                // 로그인 성공
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
        tvBestScoreChoseong = findViewById(R.id.tv_best_score_choseong);
        tvBest1Min = findViewById(R.id.tv_best_1min);
        tvBest3Min = findViewById(R.id.tv_best_3min);
        btnChallenge1Min = findViewById(R.id.btn_challenge_1min);
        btnChallenge3Min = findViewById(R.id.btn_challenge_3min);

        btnGroup1 = findViewById(R.id.btn_group1);
        btnGroup2 = findViewById(R.id.btn_group2);
        btnGroup3 = findViewById(R.id.btn_group3);
        btnRandomAll = findViewById(R.id.btn_random_all);

        btnOnlyBookmark = findViewById(R.id.btn_only_bookmark);
        btnTrash = findViewById(R.id.btn_trash);
        btnRanking = findViewById(R.id.btn_ranking);

        // 탭 관련
        tabChoseong = findViewById(R.id.tab_choseong);
        tabLiteracy = findViewById(R.id.tab_literacy);
        containerChoseong = findViewById(R.id.container_choseong);
        containerLiteracy = findViewById(R.id.container_literacy);

        // 문해력 챌린지 버튼
        btnLiteracyChallenge1Min = findViewById(R.id.btn_literacy_challenge_1min);
        btnLiteracyChallenge3Min = findViewById(R.id.btn_literacy_challenge_3min);
        tvBestLiteracy1Min = findViewById(R.id.tv_best_literacy_1min);
        tvBestLiteracy3Min = findViewById(R.id.tv_best_literacy_3min);

        // 문해력 테스트 카테고리 버튼
        btnLiteracyRandom = findViewById(R.id.btn_literacy_random);
        btnLitVocab = findViewById(R.id.btn_lit_vocab);
        btnLitSpelling = findViewById(R.id.btn_lit_spelling);
        btnLitIdiom = findViewById(R.id.btn_lit_idiom);
        btnLitReading = findViewById(R.id.btn_lit_reading);
        btnLitGrammar = findViewById(R.id.btn_lit_grammar);
        btnLitPractical = findViewById(R.id.btn_lit_practical);
        btnLitTerms = findViewById(R.id.btn_lit_terms);
        btnLitProverb = findViewById(R.id.btn_lit_proverb);

        scrollView = findViewById(R.id.scrollView);
        adViewHome = findViewById(R.id.adViewHome);
        btnRemoveAds = findViewById(R.id.btn_remove_ads);
    }

    private void setupListeners() {
        // 초성 퀴즈 탭
        btnGroup1.setOnClickListener(v -> startMainActivity(1, false));
        btnGroup2.setOnClickListener(v -> startMainActivity(2, false));
        btnGroup3.setOnClickListener(v -> startMainActivity(3, false));
        btnRandomAll.setOnClickListener(v -> startMainActivity(0, false));
        btnOnlyBookmark.setOnClickListener(v -> startMainActivity(0, true));
        btnChallenge1Min.setOnClickListener(v -> showChallengeConfirmDialog(Constants.TIME_LIMIT_1MIN));
        btnChallenge3Min.setOnClickListener(v -> showChallengeConfirmDialog(Constants.TIME_LIMIT_3MIN));

        btnTrash.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TrashActivity.class);
            startActivity(intent);
        });

        btnRanking.setOnClickListener(v -> showLeaderboard());
        btnRemoveAds.setOnClickListener(v -> billingManager.launchPurchase());

        // 탭 전환
        tabChoseong.setOnClickListener(v -> switchTab(true));
        tabLiteracy.setOnClickListener(v -> switchTab(false));

        // 문해력 챌린지
        btnLiteracyChallenge1Min.setOnClickListener(v -> showLiteracyChallengeConfirmDialog(Constants.TIME_LIMIT_1MIN));
        btnLiteracyChallenge3Min.setOnClickListener(v -> showLiteracyChallengeConfirmDialog(Constants.TIME_LIMIT_3MIN));

        // 문해력 테스트 탭
        btnLiteracyRandom.setOnClickListener(v -> startLiteracyActivity("all"));
        btnLitVocab.setOnClickListener(v -> startLiteracyActivity("어휘·의미"));
        btnLitSpelling.setOnClickListener(v -> startLiteracyActivity("맞춤법"));
        btnLitIdiom.setOnClickListener(v -> startLiteracyActivity("사자성어"));
        btnLitReading.setOnClickListener(v -> startLiteracyActivity("독해"));
        btnLitGrammar.setOnClickListener(v -> startLiteracyActivity("문법"));
        btnLitPractical.setOnClickListener(v -> startLiteracyActivity("실생활"));
        btnLitTerms.setOnClickListener(v -> startLiteracyActivity("어휘·용어"));
        btnLitProverb.setOnClickListener(v -> startLiteracyActivity("속담·관용어"));
    }

    private void switchTab(boolean isChoseong) {
        isChoseongTab = isChoseong;
        containerChoseong.setVisibility(isChoseong ? View.VISIBLE : View.GONE);
        containerLiteracy.setVisibility(isChoseong ? View.GONE : View.VISIBLE);

        tabChoseong.setBackgroundResource(isChoseong ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        tabChoseong.setTextColor(isChoseong ? Color.WHITE : Color.parseColor("#94A3B8"));

        tabLiteracy.setBackgroundResource(isChoseong ? R.drawable.bg_tab_inactive : R.drawable.bg_tab_active);
        tabLiteracy.setTextColor(isChoseong ? Color.parseColor("#94A3B8") : Color.WHITE);

        if (scrollView != null) scrollView.scrollTo(0, 0);
        loadScores();
    }

    private void startLiteracyActivity(String category) {
        Intent intent = new Intent(this, LiteracyQuizActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    private void showChallengeConfirmDialog(long timeLimit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_challenge_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        android.widget.TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        android.widget.TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        android.widget.Button btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);

        if (timeLimit == Constants.TIME_LIMIT_1MIN) {
            tvTitle.setText("⚡ 1분 챌린지");
            tvTitle.setTextColor(Color.parseColor("#FFB74D"));
            btnConfirm.setBackgroundResource(R.drawable.bg_btn_dialog_confirm_orange);
            tvMessage.setText("⏱️ 1분 안에 최대한 많이 맞춰보세요!\n\n정답 힌트 없이 진행되는 순수 실력 측정 모드예요. PASS는 -2초, 틀리면 -5초 패널티가 있으니 신중하게!\n\n도전할 준비가 되셨나요? 💪");
        } else {
            tvTitle.setText("🔥 3분 챌린지");
            tvTitle.setTextColor(Color.parseColor("#FF6F61"));
            btnConfirm.setBackgroundResource(R.drawable.bg_btn_dialog_confirm);
            tvMessage.setText("🏆 3분 동안 실력을 마음껏 발휘해보세요!\n\n정답 힌트 없이 진행되며, 점수는 전 세계 리더보드에 기록돼요. PASS는 -2초, 틀리면 -5초 패널티!\n\n도전할 준비가 되셨나요? 🔥");
        }

        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_dialog_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(HomeActivity.this, TimeAttackActivity.class);
            intent.putExtra("TIME_LIMIT", timeLimit);
            startActivity(intent);
        });

        dialog.show();
    }

    private void showLeaderboard() {
        Intent intent = new Intent(this, LeaderboardActivity.class);
        startActivity(intent);
    }

    private void showLiteracyChallengeConfirmDialog(long timeLimit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_challenge_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        android.widget.TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        android.widget.TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        android.widget.Button btnConfirm = dialog.findViewById(R.id.btn_dialog_confirm);

        if (timeLimit == Constants.TIME_LIMIT_1MIN) {
            tvTitle.setText("⚡ 1분 챌린지");
            tvTitle.setTextColor(Color.parseColor("#FFB74D"));
            btnConfirm.setBackgroundResource(R.drawable.bg_btn_dialog_confirm_orange);
            tvMessage.setText("⏱️ 1분 안에 최대한 많이 맞춰보세요!\n\n정답 힌트 없이 진행되는 순수 실력 측정 모드예요. PASS는 -2초, 틀리면 -5초 패널티가 있으니 신중하게!\n\n도전할 준비가 되셨나요? 💪");
        } else {
            tvTitle.setText("🔥 3분 챌린지");
            tvTitle.setTextColor(Color.parseColor("#FF6F61"));
            btnConfirm.setBackgroundResource(R.drawable.bg_btn_dialog_confirm);
            tvMessage.setText("🏆 3분 동안 실력을 마음껏 발휘해보세요!\n\n정답 힌트 없이 진행되며, 점수는 전 세계 리더보드에 기록돼요. PASS는 -2초, 틀리면 -5초 패널티!\n\n도전할 준비가 되셨나요? 🔥");
        }

        dialog.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_dialog_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(HomeActivity.this, LiteracyChallengeActivity.class);
            intent.putExtra("TIME_LIMIT", timeLimit);
            startActivity(intent);
        });

        dialog.show();
    }

    private void loadScores() {
        tvBestScoreChoseong.setText(" 연속 정답 : " + repository.getBestStreak("choseong"));
        tvBestScore.setText(" 연속 정답 : " + repository.getBestStreak("literacy"));

        // 초성 탭 점수
        tvBest1Min.setText("Best: " + repository.getChallengeScore(Constants.TIME_LIMIT_1MIN));
        tvBest3Min.setText("Best: " + repository.getChallengeScore(Constants.TIME_LIMIT_3MIN));

        // 문해력 탭 점수
        tvBestLiteracy1Min.setText("Best: " + repository.getLiteracyChallengeBestScore(Constants.TIME_LIMIT_1MIN));
        tvBestLiteracy3Min.setText("Best: " + repository.getLiteracyChallengeBestScore(Constants.TIME_LIMIT_3MIN));
    }

    private void hideAllAds() {
        if (adViewHome != null) adViewHome.setVisibility(View.GONE);
        if (btnRemoveAds != null) btnRemoveAds.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) billingManager.destroy();
    }

    private void startMainActivity(int groupNumber, boolean isBookmarkMode) {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.putExtra("selected_group", groupNumber);
        intent.putExtra("bookmark_mode", isBookmarkMode);
        startActivity(intent);
    }
}
