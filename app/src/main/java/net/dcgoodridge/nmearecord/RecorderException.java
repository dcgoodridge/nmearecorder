package net.dcgoodridge.nmearecord;


public class RecorderException extends Exception {
    public RecorderException(String message) {
        super(message);
    }


    public RecorderException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
