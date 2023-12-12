package com.example.galeria;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.net.Uri;


public class MainActivity extends AppCompatActivity {
    String currentPhotoPath;
    private ImageView imageView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenGallery = findViewById(R.id.btnOpenGallery);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imageView = findViewById(R.id.imageView);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            handleImageResult(result.getData().getData());
                        }
                    }
                });

        // Inicializar el ActivityResultLauncher para la cámara
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            galleryAddPic();
                            //handleImageResult(result.getData().getExtras().get("data"));
                            setPic();
                            System.out.println("Correcto");
                        }
                    }
                });

        File file = new File(getFilesDir(), "pepe.jpg");
        System.out.println(file.exists());

        if (file.exists()){
            String imagePath = file.getAbsolutePath();
            imageView.setImageURI(Uri.parse(imagePath));
        }

        btnOpenGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
        // Ensure that there's a camera activity to handle the intent

        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                System.out.println("entra");
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.galeria.fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(cameraIntent);
            }
        }

    }

    private void handleImageResult(Object data) {
        if (data instanceof Uri) {
            // Galería
            Uri imageUri = (Uri) data;
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                saveImageToInternalStorage(bitmap); // Guardar la imagen en almacenamiento interno
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (data instanceof Bitmap) {
            // Cámara
            Bitmap bitmap = (Bitmap) data;
            saveImageToInternalStorage(bitmap); // Guardar la imagen en almacenamiento interno
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        }
    }
    private void saveImageToInternalStorage(Bitmap bitmap) {
        try {
            // Obtener el directorio de almacenamiento interno de la aplicación
            File internalStorageDir = getFilesDir();

            // Crear un nombre de archivo único (puedes mejorar esto según tus necesidades)
            String fileName = "pepe.jpg";

            // Crear un nuevo archivo en el directorio de almacenamiento interno
            File imageFile = new File(internalStorageDir, fileName);

            // Crear un flujo de salida para escribir la imagen en el archivo
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            // Comprimir y escribir la imagen en el archivo
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            // Cerrar el flujo de salida
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW/targetW, photoH/targetH));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }
}
