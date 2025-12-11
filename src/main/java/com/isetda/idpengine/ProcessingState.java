package com.isetda.idpengine;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessingState {
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    public void setProcessing(boolean value) {
        isProcessing.set(value);
    }
}
