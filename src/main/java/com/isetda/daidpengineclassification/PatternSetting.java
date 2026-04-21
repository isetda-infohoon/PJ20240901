package com.isetda.daidpengineclassification;

public class PatternSetting {
    private String mainCategory;
    private String subCategory;

    public PatternSetting(String mainCategory, String subCategory) {
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public String getSubCategory() {
        return subCategory;
    }
}
