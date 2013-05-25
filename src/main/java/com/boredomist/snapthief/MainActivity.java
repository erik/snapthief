package com.boredomist.snapthief;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.*;

public class MainActivity extends Activity {
    ToggleButton toggleButton = null;
    GridView gridView = null;
    TextView numberSnaps = null;
    int lastNum = -1;
    ImageAdapter adapter = null;
    Timer updateTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numberSnaps = (TextView) findViewById(R.id.numImages);
        gridView = (GridView) findViewById(R.id.gridView);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);


        if (savedInstanceState != null) {
            toggleButton.setChecked(savedInstanceState.getBoolean("active"));
        } else {
            toggleButton.setChecked(true);
        }

        adapter = new ImageAdapter(this);

        numberSnaps.setText("" + adapter.getCount());

        adapter.notifyDataSetChanged();
        gridView.setAdapter(adapter);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SnapThiefService.class);

                if (isSnapThiefServiceRunning()) {
                    Toast.makeText(getApplicationContext(), "Stopping SnapThief...", Toast.LENGTH_SHORT).show();
                    stopService(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Starting SnapThief...", Toast.LENGTH_SHORT).show();
                    startService(intent);
                }
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                File file = (File) gridView.getAdapter().getItem(position);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromFile(file)));
            }
        });
    }

    private boolean isSnapThiefServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SnapThiefService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("active", isSnapThiefServiceRunning());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateTimer.cancel();
        updateTimer = new Timer();

        if (!isSnapThiefServiceRunning()) {
            Intent intent = new Intent(this, SnapThiefService.class);
            startService(intent);

            toggleButton.setChecked(true);
        }

        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File dir = new File("/sdcard/snapthief/");
                File[] files = dir.listFiles();

                ArrayList<File> fileResult = new ArrayList<File>();

                if (files != null) {
                    for (File f : files) {
                        if (!f.getName().contains(".nomedia"))
                            fileResult.add(f);
                    }
                }


                if (lastNum == -1) {
                    Collections.sort(fileResult, new Comparator<File>() {
                        @Override
                        public int compare(File file, File file2) {
                            int result = (int) (file.lastModified() - file2.lastModified());

                            if (result == 0) {
                                return file2.compareTo(file);
                            }

                            return result;
                        }
                    });
                }

                for (File f : fileResult) {
                    adapter.addFile(f);
                }

                lastNum = files.length;

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        numberSnaps.setText("" + adapter.getCount());
                    }
                });
            }
        }, 0, 20 * 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateTimer.cancel();
    }

    public class ImageAdapter extends BaseAdapter implements Parcelable {
        final HashMap<String, SoftReference<Bitmap>> thumbMap = new HashMap<String, SoftReference<Bitmap>>();
        private final ArrayList<File> files;
        private Context context;

        public ImageAdapter(Context context) {
            this.context = context;
            this.files = new ArrayList<File>();
        }

        private void loadBitmap(final ImageView view, final File f) {

            synchronized (view) {
                new Thread() {
                    @Override
                    public void run() {
                        SoftReference<Bitmap> ref = null;

                        synchronized (thumbMap) {
                            ref = thumbMap.get(f.getName());
                        }
                        Bitmap bitmap = null;

                        if (ref != null)
                            bitmap = ref.get();

                        if (bitmap == null) {
                            if (f.getName().contains(".mp4")) {
                                bitmap = ThumbnailUtils.createVideoThumbnail(f.getAbsolutePath(),
                                        MediaStore.Images.Thumbnails.MINI_KIND);
                            } else {
                                BitmapFactory.Options bounds = new BitmapFactory.Options();
                                bounds.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(f.getPath(), bounds);

                                if ((bounds.outWidth != -1) && (bounds.outHeight != -1)) {
                                    BitmapFactory.Options opts = new BitmapFactory.Options();
                                    opts.inSampleSize = 4;
                                    bitmap = BitmapFactory.decodeFile(f.getPath(), opts);
                                }
                            }

                            if (bitmap == null) {
                                bitmap = BitmapFactory.decodeResource(getResources(),
                                        android.R.drawable.ic_dialog_alert);
                            }

                            synchronized (thumbMap) {
                                thumbMap.put(f.getName(), new SoftReference<Bitmap>(bitmap));
                            }
                        }

                        final Bitmap finalBitmap = bitmap;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                view.setImageBitmap(finalBitmap);
                                view.invalidate();
                            }
                        });
                    }
                }.start();
            }
        }

        public void addFile(File f) {
            synchronized (thumbMap) {
                if (!thumbMap.containsKey(f.getName())) {
                    thumbMap.put(f.getName(), new SoftReference<Bitmap>(null));
                    files.add(0, f);
                }
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = convertView;
            if (convertView == null) {
                view = inflater.inflate(R.layout.gridviewitem, null);

            } else {
                view = convertView;
            }

            final File file = files.get(position);
            ImageView imageView = (ImageView) view.findViewById(R.id.thumbnail);
            imageView.setMaxHeight(128);
            imageView.setMaxWidth(128);
            imageView.setHapticFeedbackEnabled(true);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            loadBitmap(imageView, file);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    String type = "image/*";
                    if (file.getName().contains(".mp4"))
                        type = "video/*";

                    i.setDataAndType(Uri.fromFile(file), type);
                    startActivity(i);
                }
            });

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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
        }
    }
}
