package com.nono.word;

public final class Constants {
    private Constants() {}

    static final String PREFS_NAME = "MyWordApp";

    // SharedPreferences 키
    static final String KEY_BOOKMARKS = "bookmarks";
    static final String KEY_EXCLUDED = "excluded";
    static final String KEY_BEST_STREAK_CHOSEONG = "bestStreak_choseong";
    static final String KEY_BEST_STREAK_LITERACY = "bestStreak_literacy";
    static final String KEY_CHALLENGE_SCORE_PREFIX = "challengeBest_";

    // Google Play Games 리더보드 ID
    static final String LEADERBOARD_ID = "CgkI-s7_zpQSEAIQAQ";

    // 타임어택 시간 제한
    static final long TIME_LIMIT_1MIN = 60_000L;
    static final long TIME_LIMIT_3MIN = 180_000L;

    // 리워드 광고 ID
    static final String REWARDED_AD_ID = "ca-app-pub-5497677824149260/6804698298";

    // Firestore 컬렉션명
    static final String COLLECTION_LEADERBOARD_CHOSEONG_1MIN = "leaderboard_choseong_1min";
    static final String COLLECTION_LEADERBOARD_CHOSEONG_3MIN = "leaderboard_choseong_3min";
    static final String COLLECTION_LEADERBOARD_LITERACY_1MIN = "leaderboard_literacy_1min";
    static final String COLLECTION_LEADERBOARD_LITERACY_3MIN = "leaderboard_literacy_3min";

    // 문해력 테스트
    static final String KEY_LITERACY_BEST_SCORE = "literacyBestScore";
    static final String KEY_LITERACY_CHALLENGE_SCORE_PREFIX = "literacyChallengeBest_";
    static final int LITERACY_QUIZ_COUNT = 20;

    // 인앱 결제
    static final String PRODUCT_REMOVE_ADS = "remove_ads";
    static final String KEY_ADS_REMOVED = "ads_removed";
}
