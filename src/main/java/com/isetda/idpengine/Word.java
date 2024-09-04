package com.isetda.idpengine;

public class Word {
    private int wordId;
    private int documentId;
    private String word;
    private double wordWeight;
    private String documentType;
    private String countryCode;

    public Word(int wordId, int documentId, String word, double wordWeight, String documentType, String countryCode) {
        this.wordId = wordId;
        this.documentId = documentId;
        this.word = word;
        this.wordWeight = wordWeight;
        this.documentType = documentType;
        this.countryCode = countryCode;
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

    public String getDocumentType() {
        return documentType;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
