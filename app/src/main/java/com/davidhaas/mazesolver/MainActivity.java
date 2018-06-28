package com.davidhaas.mazesolver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    public static final String IMAGE_FILE = "com.davidhaas.mazesolver.IMAGE_FILE";
    static final int REQUEST_TAKE_PHOTO = 1;

    private static final String TAG = "MyActivity";

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creates the button with its listener
        Button takePhotoButton = findViewById(R.id.takePhotoButton);

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
    }

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "dispatchTakePictureIntent: " + "Error Creating file");
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.davidhaas.mazesolver",
                        photoFile);

                // Takes the picture
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
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

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        // Uri uri = Uri.parse("file://" + mCurrentPhotoPath);

        Log.i(TAG, "mCurrentPhotoPath: " + mCurrentPhotoPath);
        // Log.i(TAG, "uri: " + uri);

        //InputStream imageStream = getContentResolver().openInputStream(uri);
        //Bitmap myBitmap = BitmapFactory.decodeStream(imageStream);

        return image;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {

                // Starts the CornerSelectActivity
                Intent intent = new Intent(this, CornerSelectActivity.class);
                intent.putExtra(IMAGE_FILE, mCurrentPhotoPath);

                startActivity(intent);
            }
        }
    }
}
