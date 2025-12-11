package com.isetda.idpengine;

public class FolderMapping {
    private String imageFolderPath;
    private String resultFilePath;

    public FolderMapping(String imageFolderPath, String resultFilePath) {
        this.imageFolderPath = imageFolderPath;
        this.resultFilePath = resultFilePath;
    }

    public String getImageFolderPath() {
        return imageFolderPath;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }
}
