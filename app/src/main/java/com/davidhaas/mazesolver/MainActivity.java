package com.davidhaas.mazesolver;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Name: CornerSelectActivity
 * Purpose: Provides an interface for the user to take a picture of a maze.
 * Author: David Haas
 * Last updated: 6/28/18
 */

public class MainActivity extends AppCompatActivity {

    static {
        OpenCVLoader.initDebug();
    }

    public static final String IMAGE_URI = "com.davidhaas.mazesolver.IMAGE_URI";
    static final int REQUEST_TAKE_PHOTO = 1, REQUEST_LOAD_PHOTO = 2;

    private static final String TAG = "MyActivity";

    private Uri mImageURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creates the button with its listener
        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        Button loadPhotoButton = findViewById(R.id.loadPhotoButton);

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePhotoIntent();
            }
        });

        loadPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchLoadPhotoIntent();
            }
        });
    }

    private void dispatchTakePhotoIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "dispatchTakePhotoIntent: " + "Error Creating file");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mImageURI = FileProvider.getUriForFile(this,
                        "com.davidhaas.mazesolver",
                        photoFile);

                // Takes the picture
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void dispatchLoadPhotoIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Picture"), REQUEST_LOAD_PHOTO);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "MAZE_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (resultCode == RESULT_OK) {
            Intent cornerIntent = new Intent(this, CornerSelectActivity.class);
            if (requestCode == REQUEST_LOAD_PHOTO || requestCode == REQUEST_TAKE_PHOTO) {
                if(requestCode == REQUEST_LOAD_PHOTO)
                     mImageURI = data.getData();

                cornerIntent.putExtra(IMAGE_URI, mImageURI.toString());
                Log.i(TAG, "onActivityResult: data extras " + mImageURI);
                startActivity(cornerIntent);
            }
        }
    }
}
