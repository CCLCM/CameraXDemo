package com.chencl.camerax;

import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class LuminosityAnalyzer implements ImageAnalysis.Analyzer {
    private long lastAnalyzedTimestamp = 0L;
    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        long currentTimestamp = System.currentTimeMillis();
        // Calculate the average luma no more often than every second
        if (currentTimestamp - lastAnalyzedTimestamp >=
                TimeUnit.SECONDS.toMillis(1)) {

            // Since format in ImageAnalysis is YUV, image.planes[0]
            // contains the Y (luminance) plane
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            // Extract image data from callback object

            byte[] data = buffer.array();


            String pixels = bytes2HexString(data);


            // Compute average luminance for the image

            // Log the new luma value
            Log.d("CameraXApp", "Average luminosity:" + pixels);
            // Update timestamp of last analyzed frame
            lastAnalyzedTimestamp = currentTimestamp;
        }
    }

    public static String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[ i ] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }




}
