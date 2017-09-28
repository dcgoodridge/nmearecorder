package net.dcgoodridge.nmearecord;

public class Constants {
    public interface ACTION {
        String PKG = "net.dcgoodridge.nmearecord";
        String RECORD_START_ACTION = PKG+".recordStart";
        String RECORD_STOP_ACTION = PKG+".recordStop";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }
}
