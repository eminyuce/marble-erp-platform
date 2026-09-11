package com.ozerler.marble.util;

public final class Filenames {

    private Filenames() {
    }

    public static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public static boolean containsPathTraversal(String path) {
        return path != null && path.contains("..");
    }
}
