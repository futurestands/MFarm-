package dev.mfarm.com.mfarm;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import dev.mfarm.com.mfarm.dao.DatabaseHelper;

public class PhotoIntentActivity extends AppCompatActivity {
    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;

    private String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    Button btnSave;

    private String animalName = "";
    private String breedId = "1";
    private String bodyConf = "";
    private String gender = "Female";
    private String bodyColor = "";
    private String dob = "";
    private String damId = "1";
    private String sireId = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_intent);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if (intent != null) {
            animalName = intent.getStringExtra("animal_name");
            breedId = intent.getStringExtra("breed_id");
            bodyConf = intent.getStringExtra("body_conf");
            gender = intent.getStringExtra("gender");
            bodyColor = intent.getStringExtra("body_color");
            dob = intent.getStringExtra("dob");
            damId = intent.getStringExtra("dam_id");
            sireId = intent.getStringExtra("sire_id");
        }
        if (animalName == null) animalName = GlobalVariables.animal_name;
        if (breedId == null) breedId = GlobalVariables.breed_id;
        if (bodyConf == null) bodyConf = GlobalVariables.body_conf;
        if (gender == null) gender = GlobalVariables.gender;
        if (bodyColor == null) bodyColor = GlobalVariables.body_color;
        if (dob == null) dob = GlobalVariables.dob;
        if (damId == null) damId = GlobalVariables.dam_id;
        if (sireId == null) sireId = GlobalVariables.sire_id;

        mImageView = findViewById(R.id.imageView1);
        mImageBitmap = null;

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (animalName == null || animalName.trim().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Start registration again", Toast.LENGTH_LONG).show();
                    return;
                }

                ContentValues collect = new ContentValues();
                collect.put("name", animalName.trim());
                collect.put("breed_id", breedId);
                collect.put("gender", gender);
                collect.put("dob", dob);
                collect.put("body_conf", bodyConf);
                collect.put("body_color", bodyColor);
                collect.put("dam_id", damId);
                collect.put("sire_id", sireId);
                collect.put("photo_path", mCurrentPhotoPath);
                collect.put("lifecycle_status", "Active");
                collect.put("repro_status", "Open");
                collect.put("lactation_status", "Dry");
                collect.put("health_status", "Healthy");

                MainActivity.database.beginTransaction();
                try {
                    long rowId = MainActivity.database.insert("animas", null, collect);
                    if (rowId == -1) {
                        Toast.makeText(getApplicationContext(), "Could not save animal", Toast.LENGTH_LONG).show();
                        return;
                    }
                    MainActivity.database.setTransactionSuccessful();
                    DatabaseHelper.logAudit(MainActivity.database, "CREATE_ANIMAL", "ANIMALS", rowId, "Registered animal: " + animalName);
                    Toast.makeText(getApplicationContext(), "Animal Registered Successfully", Toast.LENGTH_LONG).show();

                    Intent x = new Intent(getApplicationContext(), MainActivity.class);
                    x.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(x);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    MainActivity.database.endTransaction();
                }
            }
        });

        Button picBtn = findViewById(R.id.btnIntend);
        setBtnListenerOrDisable(picBtn, mTakePicOnClickListener, MediaStore.ACTION_IMAGE_CAPTURE);

        Button picSBtn = findViewById(R.id.btnIntendS);
        setBtnListenerOrDisable(picSBtn, mTakePicSOnClickListener, MediaStore.ACTION_IMAGE_CAPTURE);

        dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
    }

    private File getAlbumDir() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        return File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
    }

    private File setUpPhotoFile() throws IOException {
        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();
        return f;
    }

    private void setPic() {
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(View.VISIBLE);
    }

    private void dispatchTakePictureIntent(int actionCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app available. You can still save without a photo.", Toast.LENGTH_LONG).show();
            return;
        }

        if (actionCode == ACTION_TAKE_PHOTO_B) {
            try {
                File f = setUpPhotoFile();
                mCurrentPhotoPath = f.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(this, "dev.mfarm.com.mfarm.fileprovider", f);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (IOException e) {
                e.printStackTrace();
                mCurrentPhotoPath = null;
                Toast.makeText(this, "Could not create image file. Save without a photo.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void handleSmallCameraPhoto(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            mImageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(mImageBitmap);
            mImageView.setVisibility(View.VISIBLE);
        }
    }

    private void handleBigCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            setPic();
        }
    }

    Button.OnClickListener mTakePicOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
        }
    };

    Button.OnClickListener mTakePicSOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTION_TAKE_PHOTO_B) {
                handleBigCameraPhoto();
            } else if (requestCode == ACTION_TAKE_PHOTO_S) {
                handleSmallCameraPhoto(data);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        if (mImageBitmap != null) {
            mImageView.setImageBitmap(mImageBitmap);
            mImageView.setVisibility(savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(Button btn, Button.OnClickListener onClickListener, String intentName) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }
}
