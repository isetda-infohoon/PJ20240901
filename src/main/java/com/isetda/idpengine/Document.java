package com.isetda.idpengine;

public class Document {
    private int documentId;
    private int countryId;
    private String documentType;
    private String countryCode;

    public Document(int documentId, int countryId, String documentType, String countryCode) {
        this.documentId = documentId;
        this.countryId = countryId;
        this.documentType = documentType;
        this.countryCode = countryCode;
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getCountryId() {
        return countryId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
