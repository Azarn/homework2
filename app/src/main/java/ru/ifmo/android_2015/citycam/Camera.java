package ru.ifmo.android_2015.citycam;

import android.graphics.Bitmap;

public class Camera {
    private String PreviewUrl;
    private String title;
    private Bitmap bitmap;

    public String getTitle() {
        return title;
    }

    public String getPreviewUrl() {
        return PreviewUrl;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPreviewUrl(String previewUrl) {
        PreviewUrl = previewUrl;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
