package com.chencl.camerax;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;

public class MainActivity extends AppCompatActivity implements LifecycleOwner {
    private static int REQUEST_CODE_PERMISSIONS = 10;


    private String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,

    };
    // Add this after onCreate

    private TextureView viewFinder;
    private MyOnLayoutChangeListener myOnLayoutChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.view_finder);

        // Request camera permissions
        if (allPermissionsGranted()) {
//            viewFinder.post {  }
            viewFinder.post(new Runnable() {
                @Override
                public void run() {
                    startCamera();
                }
            });

        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Every time the provided texture view changes, recompute layout
        myOnLayoutChangeListener = new MyOnLayoutChangeListener();
        viewFinder.addOnLayoutChangeListener(myOnLayoutChangeListener);
    }


    private void startCamera() {
        // Create configuration object for the viewfinder use case

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(new Rational(1, 1))
                .setTargetResolution(new Size(640, 640)).build();

        // Build the viewfinder use case
        Preview preview = new Preview(previewConfig);

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                // To update the SurfaceTexture, we have to remove it and re-add it
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);
                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                myOnLayoutChangeListener.onLayoutChange(viewFinder, 0, 0, 0, 0, 0, 0, 0, 0);
            }
        });



        // Create configuration object for the image capture use case
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setTargetAspectRatio(new Rational(1, 1))
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .build();


        // Build the image capture use case and attach button click listener
        final ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);
        findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                         "${System.currentTimeMillis()}.jpg");
                 imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                     @Override
                     public void onImageSaved(File file) {
                         String msg = "Photo capture succeeded: ${file.absolutePath}";
                         Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                         Log.d("CameraXApp", msg);
                     }

                     @Override
                     public void onError(ImageCapture.ImageCaptureError imageCaptureError, String message,Throwable cause) {
                         String msg = "Photo capture failed: $message";
                         Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                         Log.e("CameraXApp", msg);

                     }
                 });
             }
         });


        HandlerThread luminosityAnalysis = new HandlerThread("LuminosityAnalysis");
        luminosityAnalysis.start();
        Handler imageAnalysisConfigHander = new Handler(luminosityAnalysis.getLooper());

        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(imageAnalysisConfigHander)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();


        ImageAnalysis analyzerUseCase = new ImageAnalysis(analysisConfig);
        analyzerUseCase.setAnalyzer(new  LuminosityAnalyzer());


        CameraX.bindToLifecycle(this, preview, imageCapture,analyzerUseCase);
    }


    public class MyOnLayoutChangeListener implements View.OnLayoutChangeListener {


        @Override
        public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            Matrix matrix = new Matrix();

            // Compute the center of the view finder
            float centerX = viewFinder.getWidth() / 2f;
            float centerY = viewFinder.getHeight() / 2f;
            float rotationDegrees = 0f;
            switch (viewFinder.getDisplay().getRotation()) {
                case Surface.ROTATION_0:
                    rotationDegrees = 0;
                    break;
                case Surface.ROTATION_90:
                    rotationDegrees = 90;
                    break;
                case Surface.ROTATION_180:
                    rotationDegrees = 180;
                    break;
                case Surface.ROTATION_270:
                    rotationDegrees = 270;
                    break;

            }
            matrix.postRotate(-rotationDegrees, centerX, centerY);
            viewFinder.setTransform(matrix);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post(new Runnable() {
                    @Override
                    public void run() {
                        startCamera();
                    }
                });
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    /**
     * Check if all permission specified in the manifest have been granted
     */
    private boolean allPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

}
