package com.boredomist.snapthief;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends Activity {
    SnapThiefService mService;
    boolean mBound = false;
    ToggleButton toggleButton = null;
    GridView gridView = null;
    TextView numberSnaps = null;
    int lastNum = -1;
    ImageAdapter adapter = null;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            String type = intent.getAction();

            if (type.equals(SnapThiefService.UPDATE_ACTION)) {
                ArrayList<File> files = (ArrayList<File>) extras.get("files");

                if (lastNum != -1 && files.size() > lastNum) {
                    Toast.makeText(getApplicationContext(), "Grabbed " + (files.size() - lastNum) + "images!",
                            Toast.LENGTH_LONG).show();
                }

                lastNum = files.size();

                for (File f : files) {
                    adapter.addFile(f);
                }

                adapter.sortFiles();

                adapter.notifyDataSetChanged();
                gridView.setAdapter(adapter);

                numberSnaps.setText("" + adapter.getCount());
            } else if (type.equals(SnapThiefService.KILLED_ACTION)) {
                toggleButton.setEnabled(false);

                Toast.makeText(getApplicationContext(), "Service crashed! Check the logs...", Toast.LENGTH_LONG).show();
                if (mBound) {
                    unbindService(connection);
                    mBound = false;
                }
            }
        }
    };
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            SnapThiefService.SnapThiefBinder binder = (SnapThiefService.SnapThiefBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numberSnaps = (TextView) findViewById(R.id.numImages);
        gridView = (GridView) findViewById(R.id.gridView);

        if (savedInstanceState != null) {
            adapter = savedInstanceState.getParcelable("adapter");
        } else {
            adapter = new ImageAdapter(this);
        }

        numberSnaps.setText("" + adapter.getCount());

        adapter.notifyDataSetChanged();
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                File file = (File) gridView.getAdapter().getItem(position);

                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromFile(file)));
            }
        });

        toggleButton = (ToggleButton) findViewById(R.id.activeToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mService.isActive()) {
                    mService.stopStealing();

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(0);
                } else {
                    if (!mService.getThread().isAlive())
                        mService.getThread().start();

                    mService.startStealing();

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setContentTitle("SnapThief running!")
                            .setContentText("Capturing images/videos every now and then.")
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setOngoing(true)
                            .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                                    new Intent(getApplicationContext(), MainActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                    PendingIntent.FLAG_CANCEL_CURRENT))
                            .build();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(0, notification);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("adapter", adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, SnapThiefService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        registerReceiver(receiver, new IntentFilter(SnapThiefService.UPDATE_ACTION));
        registerReceiver(receiver, new IntentFilter(SnapThiefService.KILLED_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBound) {
            unbindService(connection);
            mBound = false;
        }

        unregisterReceiver(receiver);
    }

    public class ImageAdapter extends BaseAdapter implements Parcelable {
        private final ArrayList<File> files;
        private HashMap<String, Bitmap> thumbMap;
        private Context context;

        public ImageAdapter(Context context) {
            this.context = context;
            this.files = new ArrayList<File>();
            this.thumbMap = new HashMap<String, Bitmap>();
        }

        private Bitmap getBitmap(File f) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), bounds);
            if ((bounds.outWidth == -1) || (bounds.outHeight == -1))
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            return BitmapFactory.decodeFile(f.getPath(), opts);
        }

        public void addFile(File f) {
            if (!thumbMap.containsKey(f.getName())) {
                Bitmap bitmap = getBitmap(f);
                thumbMap.put(f.getName(), bitmap);
                files.add(0, f);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = convertView;
            if (convertView == null) {
                final File file = files.get(position);
                view = inflater.inflate(R.layout.gridviewitem, null);
                ImageView imageView = (ImageView) view.findViewById(R.id.thumbnail);
                imageView.setMaxHeight(128);
                imageView.setMaxWidth(128);
                imageView.setHapticFeedbackEnabled(true);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageBitmap(thumbMap.get(file.getName()));
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        System.out.println(Uri.fromFile(file).toString());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        String type = "image/*";
                        if (file.getName().contains(".mp4"))
                            type = "video/*";

                        i.setDataAndType(Uri.fromFile(file), type);
                        startActivity(i);
                    }
                });
            }

            return view;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void sortFiles() {
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    return (int) (file2.lastModified() - file.lastModified());
                }
            });

            this.notifyDataSetChanged();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeList(files);
            parcel.writeMap(thumbMap);
        }
    }
}
