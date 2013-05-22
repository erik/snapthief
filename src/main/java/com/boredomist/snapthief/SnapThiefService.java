package com.boredomist.snapthief;

import android.app.IntentService;
import android.content.Intent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SnapThiefService extends IntentService {
    public static final String UPDATE_ACTION = "com.boredomist.snapthief.UPDATE_ACTION";
    public static final String KILLED_ACTION = "com.boredomist.snapthief.KILLED_ACTION";
    private static boolean established = false;
    private static Process rootProc;
    private static DataOutputStream os;
    private boolean active = false;
    private Timer timer = new Timer(true);
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                synchronized (this) {
                    os.flush();

                    os.writeBytes("cd /sdcard/snapthief\n");

                    os.writeBytes("cp -r /data/data/com.snapchat.android/cache/received_image_snaps/*"
                            + " /sdcard/snapthief/\n");
                    os.writeBytes("cp -r /sdcard/Android/data/com.snapchat.android/cache/received_video_snaps/*"
                            + " /sdcard/snapthief/\n");
                    os.writeBytes("for file in *.nomedia; do mv $file `sha512sum $file | cut -d ' ' "
                            + "-f 1`.`expr \"$file\" : '[^\\.]*\\.\\(.*\\)\\.nomedia`; done\n");
                    os.writeBytes("sync\n");

                    os.flush();
                }

                File dir = new File("/sdcard/snapthief/");
                File[] files = dir.listFiles();

                ArrayList<File> fileResult = new ArrayList<File>();

                if (files != null) {
                    for (File f : files) {
                        if (!f.getName().contains(".nomedia"))
                            fileResult.add(f);
                    }
                }

                Intent i = new Intent(UPDATE_ACTION);
                i.putExtra("files", fileResult);
                sendBroadcast(i);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public SnapThiefService() {
        super("SnapThiefService");
        if (rootProc == null) {
            try {
                rootProc = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(rootProc.getOutputStream());
            } catch (IOException e) {
                rootProc = null;
                e.printStackTrace();
                Intent i = new Intent(KILLED_ACTION);
                sendBroadcast(i);
                stopSelf();
            }
        }
    }

    public void toggleStealing() {
        active = !active;
        if (active)
            timerTask.run();
    }

    public void stopStealing() {
        active = false;
    }

    public void startStealing() {
        active = true;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!established) {
            File dataDir = new File("/sdcard/snapthief");
            if (!dataDir.isDirectory() && !dataDir.mkdir()) {
                Intent i = new Intent(KILLED_ACTION);
                sendBroadcast(i);

                stopSelf();
                return;
            }

            timer.scheduleAtFixedRate(timerTask, 0, 20 * 1000);
            established = true;
        }
    }
}