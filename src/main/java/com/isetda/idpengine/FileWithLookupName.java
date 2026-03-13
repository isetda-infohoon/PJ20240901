package com.isetda.idpengine;

import java.io.File;

public class FileWithLookupName {
    public final File file;
    public final String apiLookupName;

    public FileWithLookupName(File file, String apiLookupName) {
        this.file = file;
        this.apiLookupName = apiLookupName;
    }
}
