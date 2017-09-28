package net.dcgoodridge.nmearecord;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener, GpsStatus.Listener, GpsStatus.NmeaListener {

    protected static final String[] ACTIVITY_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);
    private static final int REQUEST_ACTIVITY_PERMISSIONS = 112;
    private ListView listview;
    private NmeaListAdapter adapter;
    //private TextView editText;
    private Button buttonRecordStart;
    private Button buttonRecordStop;
    private Button gpsisoffButton;
    private LinearLayout gpsisoffLayer;

    private int nmeaCount = 0;


    private Intent starterIntent;

    /**
     * Has GPS Permissions
     */
    private boolean hasPermissions = false;

    private LocationManager lm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar supportToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(supportToolbar);
        getSupportActionBar().setTitle(null);

        starterIntent = getIntent();
        hasPermissions = hasPermissions();

        if (!hasPermissions) {
            askForPermissions();
            return;
        }

        initGui();
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            Toast.makeText(MainActivity.this, "LOCATION_SERVICE NO DISPONIBLE", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private boolean isGpsEnabled() {
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void setRecordButtonsStyle(boolean isRecording) {

        if (isRecording) {
            buttonRecordStart.setEnabled(false);
            buttonRecordStop.setEnabled(true);
        } else {
            buttonRecordStart.setEnabled(true);
            buttonRecordStop.setEnabled(false);
        }
    }


    private boolean hasPermissions() {
        return ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                 PackageManager.PERMISSION_GRANTED));
    }

    private void askForPermissions() {
        ActivityCompat.requestPermissions(this, ACTIVITY_PERMISSIONS, REQUEST_ACTIVITY_PERMISSIONS);
    }

    private void initGui() {
        gpsisoffButton = (Button) findViewById(R.id.gps_is_off_button);
        gpsisoffLayer = (LinearLayout) findViewById(R.id.gps_is_off_layer);

        listview = (ListView) findViewById(R.id.listview_nmea);
        adapter = new NmeaListAdapter(this, new ArrayList<String>());
        listview.setAdapter(adapter);


        buttonRecordStart = (Button) findViewById(R.id.button_start);
        buttonRecordStop = (Button) findViewById(R.id.button_stop);

        gpsisoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });

        buttonRecordStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    eventRecordStart();
                } catch (Exception e) {
                    LOG.error("Error iniciando grabacion", e);
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        buttonRecordStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventRecordStop();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ACTIVITY_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finish();
                    startActivity(starterIntent);
                } else {
                    Toast.makeText(this, "Permisos no concedidos", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void eventRecordStart() {
        Intent startIntent = new Intent(MainActivity.this, RecorderService.class);
        startIntent.setAction(Constants.ACTION.RECORD_START_ACTION);
        startService(startIntent);
        setRecordButtonsStyle(true);
    }

    private void eventRecordStop() {
        Intent stopIntent = new Intent(MainActivity.this, RecorderService.class);
        stopIntent.setAction(Constants.ACTION.RECORD_STOP_ACTION);
        startService(stopIntent);
        setRecordButtonsStyle(false);
    }


    private boolean isRecording() {
        return RecorderService.isRecording();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_recordedfiles:
                if (isRecording()) {
                    Toast.makeText(MainActivity.this, "Detener grabación primero", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, FileListActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.menu_config:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermissions) return;

        setGpsDisabledButtonsStyle(isGpsEnabled());
        setRecordButtonsStyle(RecorderService.isRecording());

        locationUpdateStart();
    }

    private void setGpsDisabledButtonsStyle(boolean gpsEnabled) {
        if (gpsEnabled) {
            gpsisoffLayer.setVisibility(View.GONE);
        } else {
            gpsisoffLayer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        if (!hasPermissions) {
            super.onPause();
            return;
        }

        locationUpdateStop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (!hasPermissions) {
            super.onDestroy();
            return;
        }

        super.onDestroy();
    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        setGpsDisabledButtonsStyle(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        setGpsDisabledButtonsStyle(false);
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        nmeaCount++;
        adapter.add(nmea);
        adapter.notifyDataSetChanged();
    }


    @SuppressWarnings({"MissingPermission"})
    protected void locationUpdateStart() {
        lm.addGpsStatusListener(this);
        lm.addNmeaListener(this);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, RecorderService.MIN_TIME_MILLIS, RecorderService.MIN_DISTANCE_METERS, this);
    }

    @SuppressWarnings({"MissingPermission"})
    protected void locationUpdateStop() {
        if (lm != null) {
            lm.removeUpdates(this); // Siempre primero???
            lm.removeGpsStatusListener(this);
            lm.removeNmeaListener(this);

        }
    }
}
