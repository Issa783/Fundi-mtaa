package com.example.fundimtaa;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ClientActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri profileImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        profileImageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
                            // Resize the image
                            Bitmap resizedBitmap = resizeImage(bitmap, 500, 500); // Set the max width and height
                            // Set the resized bitmap to ImageView
                            ImageView imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);
                            imageViewProfilePicture.setImageBitmap(resizedBitmap);
                            // Convert the resized bitmap to URI
                            profileImageUri = getImageUri(resizedBitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Retrieve the data passed from RegistrationActivity
        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");

        // Initialize EditText fields
        EditText editTextAbout = findViewById(R.id.editTextAbout);
        EditText editTextLocation = findViewById(R.id.editTextLocation);
        ImageView imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);

        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(pickPhoto);
            }
        });

        Button buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String about = editTextAbout.getText().toString().trim();
                String location = editTextLocation.getText().toString().trim();

                if (about.isEmpty() || location.isEmpty()) {
                    Toast.makeText(ClientActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Upload profile picture if selected
                if (profileImageUri != null) {
                    uploadProfilePicture(userId, profileImageUri, about, location);
                } else {
                    // No image selected, proceed with other data
                    saveClientData(userId, null, about, location);
                }
            }
        });
    }

    private Bitmap resizeImage(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfileImage", null);
        return Uri.parse(path);
    }

    private void uploadProfilePicture(String userId, Uri imageUri, String about, String location) {
        StorageReference profilePicRef = storageRef.child("profile_pictures/" + userId + ".jpg");

        profilePicRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        profilePicRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Image uploaded successfully, save the URL
                                saveClientData(userId, uri.toString(), about, location);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ClientActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveClientData(String userId, String imageUrl, String about, String location) {
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("about", about);
        clientData.put("location", location);
        clientData.put("role", "client");
        if (imageUrl != null) {
            clientData.put("profilePicture", imageUrl);
        }

        db.collection("users").document(userId)
                .set(clientData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ClientActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ClientActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ClientActivity.this, "Failed to store additional data in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
