package com.nono.word;

public final class Constants {
    private Constants() {}

    static final String PREFS_NAME = "MyWordApp";

    // SharedPreferences 키
    static final String KEY_BOOKMARKS = "bookmarks";
    static final String KEY_EXCLUDED = "excluded";
    static final String KEY_BEST_STREAK = "bestStreak";
    static final String KEY_CHALLENGE_SCORE_PREFIX = "challengeBest_";

    // Google Play Games 리더보드 ID
    static final String LEADERBOARD_ID = "CgkI-s7_zpQSEAIQAQ";

    // 타임어택 시간 제한
    static final long TIME_LIMIT_1MIN = 60_000L;
    static final long TIME_LIMIT_3MIN = 180_000L;

    // 문해력 테스트
    static final String KEY_LITERACY_BEST_SCORE = "literacyBestScore";
    static final int LITERACY_QUIZ_COUNT = 20;
}
