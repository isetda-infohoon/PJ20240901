package com.isetda.idpengine;

public class Word {
    private int wordId;
    private int documentId;
    private String word;
    private double wordWeight;

    public Word(int wordId, int documentId, String word, double wordWeight) {
        this.wordId = wordId;
        this.documentId = documentId;
        this.word = word;
        this.wordWeight = wordWeight;
    }

    public int getWordId() {
        return wordId;
    }

    public int getDocumentId() {
        return documentId;
    }

    public String getWord() {
        return word;
    }

    public double getWordWeight() {
        return wordWeight;
    }
}
