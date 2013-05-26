package com.boredomist.snapthief;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SnapThiefService extends Service {
    private static boolean running = false;
    private Process rootProc;
    private DataOutputStream os;
    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private Timer timer = new Timer(true);
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                synchronized (this) {
                    os.flush();

                    os.writeBytes("\ncd /sdcard/snapthief\n"
                            + "cp -r /data/data/com.snapchat.android/cache/received_image_snaps/*"
                            + "   /sdcard/snapthief/\n"
                            + "cp -r /sdcard/Android/data/com.snapchat.android/cache/received_video_snaps/*"
                            + "   /sdcard/snapthief/\n"
                            + "for file in *.nomedia; do mv $file `sha512sum $file | cut -d ' ' "
                            + "   -f 1`.`expr \"$file\" : '[^\\.]*\\.\\(.*\\)\\.nomedia'`; done\n"
                            + "sync\n");

                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public SnapThiefService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.w(getClass().getName(), "Stopping service");
        running = false;
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.w(getClass().getName(), "Starting service");
        if (running) {
            Log.w(getClass().getName(), "Tried to duplicate, ignoring");
            stopSelf(startId);
            return Service.START_NOT_STICKY;
        }

        running = true;

        try {
            rootProc = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(rootProc.getOutputStream());
        } catch (IOException e) {
            rootProc = null;
            e.printStackTrace();
            stopSelf(startId);
            return Service.START_NOT_STICKY;
        }

        File dataDir = new File("/sdcard/snapthief");
        if (!dataDir.isDirectory() && !dataDir.mkdir()) {
            stopSelf(startId);
            return Service.START_NOT_STICKY;
        }

        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        builder = new NotificationCompat.Builder(this)
                .setContentTitle("SnapThief running!")
                .setContentText("Will capture data every 20 seconds.")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pi);

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(999, notification);

        timer.scheduleAtFixedRate(timerTask, 0, 20 * 1000);

        return START_NOT_STICKY;
    }
}