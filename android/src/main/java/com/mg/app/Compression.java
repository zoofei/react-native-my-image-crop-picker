package com.mg.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;

/**
 * Created by ipusic on 12/27/16.
 */

public class Compression {

    public File compressImage(final Activity activity, final ReadableMap options, final String originalImagePath) throws IOException {
        Integer maxWidth = options.hasKey("compressImageMaxWidth") ? options.getInt("compressImageMaxWidth") : null;
        Integer maxHeight = options.hasKey("compressImageMaxHeight") ? options.getInt("compressImageMaxHeight") : null;
        Double quality = options.hasKey("compressImageQuality") ? options.getDouble("compressImageQuality") : null;

        if (maxWidth == null && maxHeight == null && quality == null) {
            return new File(originalImagePath);
        }

        Compressor compressor = new Compressor(activity)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath());



        if (quality == null) {
            compressor.setQuality(100);
        } else {
            compressor.setQuality((int) (quality * 100));
        }

        if (maxWidth != null) {
            compressor.setMaxWidth(maxWidth);
        }

        if (maxHeight != null) {
            compressor.setMaxHeight(maxHeight);
        }

        File image = new File(originalImagePath);

        String[] paths = image.getName().split("\\.(?=[^\\.]+$)");
        String compressedFileName = paths[0] + "-compressed";

        if(paths.length > 1)
            compressedFileName += "." + paths[1];


        return compressor
                .compressToFile(image, compressedFileName);
    }

    public synchronized void compressVideo(final Activity activity, final ReadableMap options, final String originalVideo, final String compressedVideo, final Promise promise) {
        // todo: video compression
        // failed attempt 1: ffmpeg => slow and licensing issues
        promise.resolve(originalVideo);
    }
}