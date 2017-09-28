package net.dcgoodridge.nmearecord;

import java.io.File;
import java.text.DecimalFormat;

public class NmeaFile implements Comparable<NmeaFile> {

    private File file;

    public NmeaFile(File file) {
        this.file = file;
        readableSize = readableFileSize(this.file.length());
    }

    private String readableSize;

    public File getFile() {
        return file;
    }

    @Override
    public int compareTo(NmeaFile another) {
        long lastModified = file.lastModified();
        long anotherLastModified = another.getFile().lastModified();
        if (lastModified > anotherLastModified) {
            return -1;
        } else if (lastModified < anotherLastModified) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Se ha metido automaticamente..(para eliminar warning sonarqube)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NmeaFile nmeaFile = (NmeaFile) o;

        return file != null ? file.equals(nmeaFile.file) : nmeaFile.file == null;

    }

    public String getReadableFileSize(){
        return readableSize;
    }

    /**
     * Se ha metido automaticamente..(para eliminar warning sonarqube)
     */
    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }


    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
