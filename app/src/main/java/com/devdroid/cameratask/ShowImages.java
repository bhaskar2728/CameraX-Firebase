package com.devdroid.cameratask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class ShowImages extends AppCompatActivity {

    FirebaseStorage firebaseStorage;
    ArrayList<StorageReference> storageReferences;
    RecyclerView rclView;
    ImageView imgCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_images);

        rclView = findViewById(R.id.rclView);
        imgCamera = findViewById(R.id.imgCamera);

        imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShowImages.this,MainActivity.class));
                finishAffinity();
            }
        });

        firebaseStorage = FirebaseStorage.getInstance();
        StorageReference reference = firebaseStorage.getReference().child("images/"+getDeviceId());

        storageReferences = new ArrayList<>();

        reference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {

//                        Toast.makeText(ShowImages.this, "Success", Toast.LENGTH_SHORT).show();

                        storageReferences.clear();

                        for(StorageReference item: listResult.getItems()){
                            Log.d("item:",item.toString());

                            storageReferences.add(item);

                            /*item.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {

                                    Bitmap bm = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                                    imgView.setImageBitmap(Bitmap.createScaledBitmap(bm,imgView.getWidth(),imgView.getHeight(),false));

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    Toast.makeText(ShowImages.this, "Failed because of " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });*/

                        }

                        setAdapter();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Toast.makeText(ShowImages.this, "Failed because of " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private String getDeviceId() {

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("deviceId", deviceId);
        return deviceId;
    }


    public void setAdapter() {


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        rclView.setLayoutManager(linearLayoutManager);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this,storageReferences);
        rclView.setAdapter(adapter);



    }
}