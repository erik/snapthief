package com.boredomist.snapthief;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SnapThiefService extends IntentService {
    public static final String UPDATE_ACTION = "com.boredomist.snapthief.UPDATE_ACTION";
    public static final String KILLED_ACTION = "com.boredomist.snapthief.KILLED_ACTION";
    private final IBinder binder = new SnapThiefBinder();
    private boolean active = false;
    private Thread thread;

    public SnapThiefService() {
        super("SnapThiefService");
    }

    public void stopStealing() {
        if (active) {
            Toast.makeText(this, "Stopping SnapThief thread...", Toast.LENGTH_SHORT).show();
            active = false;
        }
    }

    public void startStealing() {
        if (!active) {
            Toast.makeText(this, "Starting SnapThief thread...", Toast.LENGTH_SHORT).show();
            active = true;
        }
    }

    public boolean isActive() {
        return active;
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        File dataDir = new File("/sdcard/snapthief");
        if (!dataDir.isDirectory() && !dataDir.mkdir()) {
            Intent i = new Intent(KILLED_ACTION);
            sendBroadcast(i);

            stopSelf();
        }
        final Toast toaster = Toast.makeText(getApplicationContext(), "Starting snapthief!", Toast.LENGTH_SHORT);

        thread = new Thread() {
            @Override
            public void run() {
                toaster.show();
                try {
                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());

                    os.writeBytes("cd /sdcard/snapthief\n");

                    while (true) {
                        if (active) {
                            os.writeBytes("cp -r /data/data/com.snapchat.android/cache/received_image_snaps/*"
                                    + " /sdcard/snapthief/\n");
                            os.writeBytes("cp -r /sdcard/Android/data/com.snapchat.android/cache/received_video_snaps/*"
                                    + " /sdcard/snapthief/\n");
                            os.writeBytes("for file in *.nomedia; do mv $file `sha512sum $file | cut -d ' ' "
                                    + "-f 1`.`expr \"$file\" : '[^\\.]*\\.\\(.*\\)\\.nomedia`; done\n");
                            os.writeBytes("sync\n");

                            os.flush();

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
                        }

                        try {
                            Thread.sleep(20 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Intent i = new Intent(KILLED_ACTION);
                    sendBroadcast(i);
                    SnapThiefService.this.stopSelf();
                }
            }
        };

        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    public class SnapThiefBinder extends Binder {
        SnapThiefService getService() {
            return SnapThiefService.this;
        }
    }
}