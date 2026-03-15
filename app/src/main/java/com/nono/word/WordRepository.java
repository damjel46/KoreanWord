package com.nono.word;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CSV 파일 읽기와 SharedPreferences 접근을 담당하는 데이터 계층.
 * 모든 Activity는 prefs에 직접 접근하지 않고 이 클래스를 통한다.
 */
public class WordRepository {

    private static final String CSV_FILE = "word.csv";

    private final SharedPreferences prefs;

    public WordRepository(Context context) {
        prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * assets/word.csv를 파싱해 전체 WordItem 리스트를 반환한다.
     * 제외 필터나 북마크 상태 복원은 호출부에서 처리한다.
     */
    public List<WordItem> loadWords(Context context) {
        List<WordItem> list = new ArrayList<>();
        try {
            AssetManager assets = context.getAssets();
            InputStream is = assets.open(CSV_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.readLine(); // 헤더 스킵
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\|");
                if (tokens.length >= 4) {
                    list.add(new WordItem(
                            tokens[0].trim(),
                            tokens[1].trim(),
                            tokens[2],
                            tokens[3]
                    ));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── 북마크 ──────────────────────────────────────────────

    public Set<String> getBookmarks() {
        return new HashSet<>(prefs.getStringSet(Constants.KEY_BOOKMARKS, new HashSet<>()));
    }

    public void saveBookmarks(Set<String> bookmarks) {
        prefs.edit().putStringSet(Constants.KEY_BOOKMARKS, bookmarks).apply();
    }

    // ── 제외(휴지통) ─────────────────────────────────────────

    public Set<String> getExcluded() {
        return new HashSet<>(prefs.getStringSet(Constants.KEY_EXCLUDED, new HashSet<>()));
    }

    public void saveExcluded(Set<String> excluded) {
        prefs.edit().putStringSet(Constants.KEY_EXCLUDED, excluded).apply();
    }

    // ── 점수 ─────────────────────────────────────────────────

    public int getBestStreak() {
        return prefs.getInt(Constants.KEY_BEST_STREAK, 0);
    }

    public void saveBestStreak(int score) {
        prefs.edit().putInt(Constants.KEY_BEST_STREAK, score).apply();
    }

    public int getChallengeScore(long timeLimit) {
        return prefs.getInt(Constants.KEY_CHALLENGE_SCORE_PREFIX + timeLimit, 0);
    }

    public void saveChallengeScore(long timeLimit, int score) {
        prefs.edit().putInt(Constants.KEY_CHALLENGE_SCORE_PREFIX + timeLimit, score).apply();
    }
}
