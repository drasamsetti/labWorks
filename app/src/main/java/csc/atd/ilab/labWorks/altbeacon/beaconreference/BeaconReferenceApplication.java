package csc.atd.ilab.labWorks.altbeacon.beaconreference;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
//import android.widget.EditText;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import java.util.Collection;

import csc.atd.ilab.labWorks.MainActivity;
import csc.atd.ilab.labWorks.PosterScanOcrActivity;
import csc.atd.ilab.labWorks.R;
import csc.atd.ilab.labWorks.core.SpeechPlayer;


/**
 * Created by Durga Prasad R on 12/13/13.
 */
public class BeaconReferenceApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "BeaconReferenceApplication";
    private final Identifier uuid = Identifier.parse("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEFFFFFF");
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MonitoringActivity monitoringActivity = null;
    private RangingActivity rangingActivity = null;
    BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

    public void onCreate() {
        super.onCreate();


        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        //

        Log.d(TAG, "setting up background monitoring for beacons and power saving");
        // wake up the app when a beacon is seen
        Region region = new Region("com.example.backgroundRegion",uuid, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
        //BeaconManager.debug = true;

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        beaconManager.setBackgroundBetweenScanPeriod(0l);
        beaconManager.setBackgroundScanPeriod(1100l);
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
    }

    @Override
    public void didEnterRegion(Region arg0) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        //RangeNotifier rangeNotifier;
        Log.d(TAG, "did enter region.");
        try {
            // start ranging for beacons.  This will provide an update once per second with the estimated
            // distance to the beacon in the didRAngeBeaconsInRegion method.
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", uuid, null, null));
            beaconManager.setRangeNotifier((RangeNotifier) this);
        } catch (RemoteException e) {   }

        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;
        } else {
            if (rangingActivity != null) {
                // If the Beacon is in with in range, we log info about the beacons we have
                // seen on its display
                rangingActivity.logToDisplay("I see a beacon again");
            } else {
                // If we have already seen beacons before, but the monitoring activity is not in
                // the foreground, we send a notification to the user on subsequent detections.
                Log.d(TAG, "Sending notification.");
                sendNotification();
            }
        }


    }

    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    //EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
                    Beacon firstBeacon = beacons.iterator().next();
                    if (firstBeacon.getDistance() < 1.0) {
                        Log.d(TAG, "I see a beacon that is less than 1 meters away.");
                        // Perform distance-specific action here
                        SpeechPlayer.getInstance(getApplicationContext()).play("Welcome to CSC TechLabs");

                        Intent intent = new Intent(String.valueOf(PosterScanOcrActivity.class));
                        startActivity(intent);
                        // The very first time since boot that we detect an beacon, we launch the
                        // MainActivity
                       // Intent intent = new Intent(this, PosterScanOcrActivity.class);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // Important:  make sure to add android:launchMode="singleInstance" in the manifest
                        // to keep multiple copies of this activity from getting created if the user has
                        // already manually launched the app.
                        //this.startActivity(intent);
                        haveDetectedBeaconsSinceBoot = true;
                    }
                    Log.d(TAG, "The first beacon "+firstBeacon.toString()+" is about "+firstBeacon.getDistance()+" meters away.");            }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }






    @Override
    public void didExitRegion(Region region) {
        if (monitoringActivity != null) {
            monitoringActivity.logToDisplay("I no longer see a beacon.");
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        if (monitoringActivity != null) {
            monitoringActivity.logToDisplay("I have just switched from seeing/not seeing beacons: " + state);
        }
    }

    private void sendNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MonitoringActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setMonitoringActivity(MonitoringActivity activity) {
        this.monitoringActivity = activity;
    }

}