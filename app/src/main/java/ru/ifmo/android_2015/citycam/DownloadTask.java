package ru.ifmo.android_2015.citycam;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import ru.ifmo.android_2015.citycam.model.City;
import ru.ifmo.android_2015.citycam.webcams.Webcams;

public class DownloadTask extends AsyncTask<City, Void, Camera> {
    private static final String TAG = "DownloadTask";
    private Activity activity;
    private DownloadState state;
    private Camera cam = null;

    enum DownloadState {
        COMPLETED,
        IN_PROGRESS,
        NO_CAMERAS,
        ERROR
    }

    DownloadTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Camera doInBackground(City... params) {
        HttpURLConnection conn = null;
        InputStream in = null;
        JsonReader jsonReader;
        state = DownloadState.IN_PROGRESS;

        try {
            URL uri = Webcams.createNearbyUrl(params[0].latitude, params[0].longitude);
            conn = (HttpURLConnection) uri.openConnection();
            in = conn.getInputStream();
            jsonReader = new JsonReader(new InputStreamReader(in));
            cam = parseCam(jsonReader);
            in.close();

            if (cam.getPreviewUrl() == null) {
                state = DownloadState.NO_CAMERAS;
            } else {
                in = new java.net.URL(cam.getPreviewUrl()).openStream();
                cam.setBitmap(BitmapFactory.decodeStream(in));
                state = DownloadState.COMPLETED;
            }
        } catch (IOException e) {
            state = DownloadState.ERROR;
            Log.e(TAG, "Error while executing task -> " + e, e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error while closing input stream -> " + e, e);
                }

            if (conn != null)
                conn.disconnect();
        }
        return cam;
    }

    @Override
    protected void onPostExecute(Camera result) {
        updateView();
        activity.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
    }

    public void attachActivity(Activity activity) {
        this.activity = activity;
        updateView();
    }

    private void updateView() {
        ImageView ivCam = (ImageView) activity.findViewById(R.id.cam_image);
        TextView tvTitle = (TextView) activity.findViewById(R.id.tvTitle);
        ProgressBar pbProgress = (ProgressBar) activity.findViewById(R.id.progress);

        if (state == DownloadState.IN_PROGRESS)
            pbProgress.setVisibility(View.VISIBLE);
        else
            pbProgress.setVisibility(View.INVISIBLE);

        if (state == DownloadState.ERROR) {
            ivCam.setImageResource(R.drawable.error);
            tvTitle.setText(R.string.error);
        } else if (state == DownloadState.NO_CAMERAS) {
            ivCam.setImageResource(R.drawable.no_camera);
            tvTitle.setText(R.string.no_cameras);
        } else if (state == DownloadState.COMPLETED) {
            ivCam.setImageBitmap(cam.getBitmap());
            tvTitle.setText(cam.getTitle());
        }
    }

    private Camera parseCam(JsonReader jsonReader) throws IOException {
        Camera result = new Camera();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String key = jsonReader.nextName();
            if (key.equals("webcams")) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    key = jsonReader.nextName();
                    if (key.equals("webcam")) {
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            jsonReader.beginObject();
                            while (jsonReader.hasNext()) {
                                key = jsonReader.nextName();
                                switch (key) {
                                    case "preview_url":
                                        result.setPreviewUrl(jsonReader.nextString());
                                        break;
                                    case "title":
                                        result.setTitle(jsonReader.nextString());
                                        break;
                                    default:
                                        jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        }
                        jsonReader.endArray();
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();
        return result;
    }
}
