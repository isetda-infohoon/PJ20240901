package com.isetda.idpengine;

public class FileInfo {
    private String filename;
    private String userId;
    private int pageNum;
    private String serviceType;
    private String groupUID;
    private String language;
    private String lClassification;
    private String mClassification;
    private String sClassification;
    private String classificationStatus;
    private String jobType;
    private String classificationStartDateTime;
    private String classificationEndDateTime;
    private String createDateTime;
    private String requestId;
    private String receiveData;
    private String urlData;
    private String taskName;

    public FileInfo() {}

    public String getFilename() {return filename;}
    public String getUserId() {return userId;}
    public int getPageNum() {return pageNum;}
    public String getServiceType() {return serviceType;}
    public String getGroupUID() {return groupUID;}
    public String getLanguage() {return language;}
    public String getLClassification() {return lClassification;}
    public String getMClassification() {return mClassification;}
    public String getSClassification() {return sClassification;}
    public String getClassificationStatus() {return classificationStatus;}
    public String getJobType() {return jobType;}
    public String getClassificationStartDateTime() {return classificationStartDateTime;}
    public String getClassificationEndDateTime() {return classificationEndDateTime;}
    public String getCreateDateTime() {return createDateTime;}
    public String getRequestId() {return requestId;}
    public String getReceiveData() {return receiveData;}
    public String getUrlData() {return urlData;}
    public String getTaskName() {return taskName; }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setGroupUID(String groupUID) { this.groupUID = groupUID; }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLClassification(String lClassification) {
        this.lClassification = lClassification;
    }

    public void setMClassification(String mClassification) {
        this.mClassification = mClassification;
    }

    public void setSClassification(String sClassification) {
        this.sClassification = sClassification;
    }

    public void setClassificationStatus(String classificationStatus) {
        this.classificationStatus = classificationStatus;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public void setClassificationStartDateTime(String classificationStartDateTime) {
        this.classificationStartDateTime = classificationStartDateTime;
    }

    public void setClassificationEndDateTime(String classificationEndDateTime) {
        this.classificationEndDateTime = classificationEndDateTime;
    }

    public void setCreateDateTime(String createDateTime) {
        this.createDateTime = createDateTime;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setReceiveData(String receiveData) {
        this.receiveData = receiveData;
    }

    public void setUrlData(String urlData) {
        this.urlData = urlData;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}
