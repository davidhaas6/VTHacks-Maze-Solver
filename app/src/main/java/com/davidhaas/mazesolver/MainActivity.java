package com.davidhaas.mazesolver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Name: MainActivity
 * Purpose: Provides an interface for the user to take a picture of a maze.
 * @author  David Haas
 * @since   2/16/18
 */

public class MainActivity extends Activity {

    // Starts OpenCV
    static {
        OpenCVLoader.initDebug();
    }

    public static final String IMAGE_URI = "com.davidhaas.mazesolver.IMAGE_URI";
    static final int REQUEST_TAKE_PHOTO = 1, REQUEST_LOAD_PHOTO = 2;

    private static final String TAG = "MyActivity";

    private Uri mImageURI;

    /**
     * Instantiates the UI elements.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.activity_main);

        //Bitmap backgBMP = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.maze_background));
        //Bitmap logoBMP = Bitmap.createBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo));

//        //fill the background ImageView with the resized image
//        ImageView background = findViewById(R.id.imgBackground);
//        background.setScaleType(ImageView.ScaleType.CENTER);
//        background.setImageBitmap(backgBMP);

        //ImageView logo = findViewById(R.id.logo);
        //logo.setImageBitmap(logoBMP);


        // Creates the buttons with their listeners
        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        Button loadPhotoButton = findViewById(R.id.loadPhotoButton);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/press_start_2p.ttf");
        loadPhotoButton.setTypeface(font);
        takePhotoButton.setTypeface(font);

        TextView txt = findViewById(R.id.textView);
        txt.setTypeface(font);


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

    /**
     * Starts the intent to take a photo of the maze.
     */
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

    /**
     * Starts the intent to load a photo from the gallery.
     */
    private void dispatchLoadPhotoIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Picture"), REQUEST_LOAD_PHOTO);
    }

    /**
     * Creates space to take an image of the maze.
     * @return The file that the image will occupy
     * @throws IOException If the file cannot be created
     */
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

    /**
     * Starts CornerSelectActivity with the image data grabbed from this activity.
     * @param requestCode Describes whether the user loaded or captured a photo
     * @param resultCode Successful or unsuccessful
     * @param data The intent storing the image URI
     */
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
