package net.dcgoodridge.nmearecord;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Servicio que se encarga de grabar las tramas NMEA a un fichero
 */
public class RecorderService extends Service implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

    public static final String FILE_EXT = "nmea";
    public static final String INTERNAL_FOLDER_NMEA = "nmea";
    public static final long MIN_TIME_MILLIS = 500;
    public static final float MIN_DISTANCE_METERS = 10;
    private static final Logger LOG = LoggerFactory.getLogger(RecorderService.class);
    private static boolean isRecording = false;
    private long nmeaStringCount = 0;
    private FileWriter recordFw;
    private BufferedWriter recordBw;
    private PrintWriter recordPw;
    private File recordFile;

    private boolean timestamp_append = false;

    /**
     * Handler que pasaremos al registrarnos a un Broadcast
     */
    private LocationManager lm;

    public static boolean isRecording() {
        return isRecording;
    }

    public static File getRecordFolder(Context context) {
        return new File(context.getFilesDir().getAbsolutePath() + File.separator + INTERNAL_FOLDER_NMEA);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        startLocationUpdates();
    }

    private void loadRecordingSettings() {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        timestamp_append = sharedPrefs.getBoolean("timestamp_append", false);
        System.currentTimeMillis();
    }

    @SuppressWarnings({"MissingPermission"})
    protected void startLocationUpdates() {
        lm.addGpsStatusListener(this);
        lm.addNmeaListener(this);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MILLIS, MIN_DISTANCE_METERS, this);
    }

    @SuppressWarnings({"MissingPermission"})
    protected void stopLocationUpdates() {
        if (lm != null) {
            lm.removeNmeaListener(this);
            lm.removeGpsStatusListener(this);
            lm.removeUpdates(this);
        }
    }

    private void recordStart() {
        if (isRecording) {
            Toast.makeText(this, "Grabación ya en curso", Toast.LENGTH_SHORT).show();
            return;
        }

        loadRecordingSettings();

        try {
            fileRecordOpen();
        } catch (Exception e) {
            LOG.error("Error iniciando grabacion a fichero", e);
            Toast.makeText(RecorderService.this, e.toString(), Toast.LENGTH_LONG).show();
            recordStop();
        }

        isRecording = true;
        nmeaStringCount = 0;
        startForegroundNotification();
        Toast.makeText(this, "Iniciada grabación", Toast.LENGTH_SHORT).show();
    }

    private void startForegroundNotification() {
        Intent buttonPrevIntent = new Intent(this, NotificationPrevButtonHandler.class);
        buttonPrevIntent.putExtra("action", "prev");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder.setContentIntent(intent).setContentTitle("NMEA Recorder").setSmallIcon(R.mipmap.micro_icon_white).setContentText("Grabando tramas a fichero").setOngoing(true);
        Notification notification = notificationBuilder.build();
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
    }

    private void recordStop() {
        if (isRecording) {
            fileRecordClose();
            isRecording = false;
            stopForeground(true);
            Toast.makeText(RecorderService.this, "Fichero generado: " + recordFile.getName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        //Caso en el que el sistema quiere matar el servicio prematuramente
        if (isRecording) {
            recordStop();
        }
        stopLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.RECORD_START_ACTION)) {
            recordStart();
        } else if (intent.getAction().equals(Constants.ACTION.RECORD_STOP_ACTION)) {
            recordStop();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        // Do nothing
    }

    @Override
    public void onLocationChanged(Location location) {
        // Do nothing
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Do nothing
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        nmeaStringCount++;
        String finalNmea;
        if (timestamp_append) {
            finalNmea = timestamp + ";" + nmea;
        } else {
            finalNmea = nmea;
        }

        if (isRecording) {
            recordPw.print(finalNmea);
        }
    }

    private String computeFileName(File directory) throws RecorderException {
        String timestamp = Long.toString(System.currentTimeMillis());
        String fileName = timestamp + "." + FILE_EXT;

        File file = new File(directory, fileName);
        if (file.exists()) {
            fileName = timestamp + "-2." + FILE_EXT;
            file = new File(directory, fileName);
        }
        if (file.exists()) {
            throw new RecorderException(fileName + " ya existe.");
        }

        return fileName;
    }

    private void fileRecordOpen() throws RecorderException {
        File recordFolder = getRecordFolder(this);
        if (!recordFolder.exists()) {
            recordFolder.mkdirs();
        }

        String fileName = computeFileName(recordFolder);

        try {
            recordFile = new File(recordFolder, fileName);
            recordFw = new FileWriter(recordFile, true);
            recordBw = new BufferedWriter(recordFw);
            recordPw = new PrintWriter(recordBw);
        } catch (IOException e) {
            throw new RecorderException("No se ha podido iniciar el fichero de grabacion", e);
        }

    }


    private void fileRecordClose() {
        if (recordPw != null) {
            recordPw.flush();
            recordPw.close();
        }
        try {
            if (recordFw != null) {
                recordFw.close();
            }
        } catch (IOException e) {
            LOG.error("Error cerrando fichero", e);
        }
        try {
            if (recordBw != null) {
                recordBw.close();
            }
        } catch (IOException e) {
            LOG.error("Error cerrando fichero", e);
        }
    }

    public static class NotificationPrevButtonHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, ".", Toast.LENGTH_SHORT).show();
        }
    }


}
