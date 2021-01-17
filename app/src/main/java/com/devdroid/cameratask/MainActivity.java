package com.devdroid.cameratask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    Executor executor = Executors.newSingleThreadExecutor();

    PreviewView cameraView;
    ImageView imgCapture,imgGallery;

    FirebaseStorage firebaseStorage;
    StorageReference images;

    ProgressDialog progressDialog;


    static final int REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseStorage = FirebaseStorage.getInstance();



        images = firebaseStorage.getReference().child("images/"+getDeviceId());


        imgCapture = findViewById(R.id.imgCapture);
        imgGallery = findViewById(R.id.imgGallery);
        cameraView = findViewById(R.id.cameraView);

        imgGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ShowImages.class));
            }
        });

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) +
        ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)||
            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Grant those permissions");
                builder.setMessage("Camera and Storage");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        },REQUEST_CODE);

                    }
                });
                builder.setNegativeButton("Cancel",null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            }
            else{

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                },REQUEST_CODE);

            }
        }
        else{
//            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            startCamera();
        }



    }

    private String getDeviceId() {

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("deviceId", deviceId);
        return deviceId;
    }


    private void startCamera() {

        ListenableFuture<ProcessCameraProvider>
                cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                ProcessCameraProvider
                        cameraProvider = null;
                try {
                    cameraProvider = cameraProviderListenableFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        },ContextCompat.getMainExecutor(this));

    }

    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).build();

        preview.setSurfaceProvider(cameraView.createSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner)this,cameraSelector,imageCapture,imageAnalysis,preview);

        imgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Uploading...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.show();

                SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

                File file = new File(getBatchDirectoryName(),simpleDateFormat.format(new Date()) + ".jpg");


                ImageCapture.OutputFileOptions outputFileOptions =
                        new ImageCapture.OutputFileOptions.Builder(file).build();


                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                //Toast.makeText(MainActivity.this, "Image Saved successfully at "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                uploadImage(file.getAbsolutePath());
                            }
                        });

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {

                        Toast.makeText(MainActivity.this, "Image not Saved Successfully", Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();

                    }
                });

            }
        });


    }

    public void uploadImage(String path) {



        Uri file = Uri.fromFile(new File(path));
        StorageReference reference = images.child(file.getLastPathSegment());
        UploadTask uploadTask = reference.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Failed because of "+e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Image Uploaded successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this,ShowImages.class));

            }
        });

    }

    public String getBatchDirectoryName() {

        String app_folder_path = "";

        app_folder_path =
                Environment.getExternalStorageDirectory().toString() + "/images";

        File dir = new File(app_folder_path);

        if(!dir.exists() && !dir.mkdirs()){

        }

        return app_folder_path;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE){
            if(grantResults.length>0 && (grantResults[0] + grantResults[1] == PERMISSION_GRANTED)){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                startCamera();
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}