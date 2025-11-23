package com.chosun.word;

public class WordItem {
    private String initial; // 초성
    private String word;    // 정답 단어
    private String mean;    // 뜻
    private String example; // 예문
    private boolean isBookmarked; // 즐겨찾기 여부

    public WordItem(String initial, String word, String mean, String example) {
        this.initial = initial;
        this.word = word;
        this.mean = mean;
        this.example = example;
        this.isBookmarked = false;
    }

    public String getInitial() { return initial; }
    public String getWord() { return word; }
    public String getMean() { return mean; }
    public String getExample() { return example; }
    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
}
