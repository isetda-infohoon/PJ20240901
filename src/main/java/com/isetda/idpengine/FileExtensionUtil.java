package com.isetda.idpengine;

import java.util.Set;

public class FileExtensionUtil {
    public static final Set<String> DA_SUPPORTED_EXT = Set.of(
            "pdf",
            // Word
            "doc","dot","dotx","docx","docm","dotm",
            // PPT
            "ppt","pot","pps","pptx","pptm","potx","potm","ppsx",
            // Excel
            "xls","xlt","xlsx","xlsm","xltx","xltm","xlsb",
            // HWP
            "hwp","hwt","hml","hwpx"
    );

    public static final Set<String> AIVISION_SUPPORTED_EXT = Set.of(
            "pdf", "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    public static final Set<String> AIVISION_OFFICE_SUPPORTED_EXT = Set.of(
            "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    public static String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx > -1) ? filename.substring(idx + 1).toLowerCase() : "";
    }
}