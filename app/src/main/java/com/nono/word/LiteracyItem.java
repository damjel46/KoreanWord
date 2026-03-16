package com.nono.word;

public class LiteracyItem {
    private int id;
    private String category;
    private String question;
    private String passage;
    private String[] choices;
    private int correctIndex;

    public LiteracyItem(int id, String category, String question, String passage, String[] choices, int correctIndex) {
        this.id = id;
        this.category = category;
        this.question = question;
        this.passage = passage;
        this.choices = choices;
        this.correctIndex = correctIndex;
    }

    public int getId() { return id; }
    public String getCategory() { return category; }
    public String getQuestion() { return question; }
    public String getPassage() { return passage; }
    public String[] getChoices() { return choices; }
    public int getCorrectIndex() { return correctIndex; }
}
