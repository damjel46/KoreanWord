package com.nono.word;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseLeaderboard {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseLeaderboard() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /** 익명 로그인 보장 */
    public void ensureSignedIn(Runnable onSuccess) {
        if (auth.getCurrentUser() != null) {
            onSuccess.run();
        } else {
            auth.signInAnonymously().addOnSuccessListener(result -> onSuccess.run());
        }
    }

    /** 점수 등록 */
    public void submitScore(String collection, String nickname, int score, OnResultListener listener) {
        ensureSignedIn(() -> {
            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("nickname", nickname);
            data.put("score", score);
            data.put("timestamp", com.google.firebase.Timestamp.now());

            // 기존 최고점보다 높을 때만 업데이트
            db.collection(collection).document(uid).get()
                    .addOnSuccessListener(doc -> {
                        long existingScore = 0;
                        if (doc.exists() && doc.getLong("score") != null) {
                            existingScore = doc.getLong("score");
                        }

                        if (score > existingScore) {
                            db.collection(collection).document(uid).set(data)
                                    .addOnSuccessListener(v -> listener.onSuccess())
                                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                        } else {
                            listener.onSuccess(); // 기존 기록이 더 높으면 그냥 성공 처리
                        }
                    })
                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
        });
    }

    /** 상위 랭킹 조회 */
    public void getTopScores(String collection, int limit, OnScoresLoadedListener listener) {
        db.collection(collection)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    int rank = 1;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String nickname = doc.getString("nickname");
                        Long score = doc.getLong("score");
                        if (nickname != null && score != null) {
                            entries.add(new LeaderboardEntry(rank++, nickname, score.intValue()));
                        }
                    }
                    listener.onLoaded(entries);
                })
                .addOnFailureListener(e -> listener.onLoaded(new ArrayList<>()));
    }

    /** 내 점수 조회 */
    public void getMyScore(String collection, OnMyScoreLoadedListener listener) {
        ensureSignedIn(() -> {
            String uid = auth.getCurrentUser().getUid();
            db.collection(collection).document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("nickname") != null && doc.getLong("score") != null) {
                            listener.onLoaded(doc.getString("nickname"), doc.getLong("score").intValue());
                        } else {
                            listener.onLoaded(null, 0);
                        }
                    })
                    .addOnFailureListener(e -> listener.onLoaded(null, 0));
        });
    }


    public interface OnMyScoreLoadedListener {
        void onLoaded(String nickname, int score);
    }

    public interface OnResultListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnScoresLoadedListener {
        void onLoaded(List<LeaderboardEntry> entries);
    }

    public static class LeaderboardEntry {
        public final int rank;
        public final String nickname;
        public final int score;

        public LeaderboardEntry(int rank, String nickname, int score) {
            this.rank = rank;
            this.nickname = nickname;
            this.score = score;
        }
    }
}
