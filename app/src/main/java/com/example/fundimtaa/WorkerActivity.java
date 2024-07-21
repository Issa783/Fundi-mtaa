package com.example.fundimtaa;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.UUID;

public class WorkerActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private ImageView profilePicture;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Retrieve the data passed from RegistrationActivity
        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");

        // Initialize EditText fields
        EditText editTextAbout = findViewById(R.id.editTextAbout);
        EditText editTextExperience = findViewById(R.id.editTextExperience);
        EditText editTextLocation = findViewById(R.id.editTextLocation);
        EditText editTextSpecialization = findViewById(R.id.editTextSpecialization);
        profilePicture = findViewById(R.id.profilePicture);
        Button buttonSelectPicture = findViewById(R.id.buttonSelectPicture);
        Button buttonSubmit = findViewById(R.id.buttonSubmitRegistration);

        buttonSelectPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String about = editTextAbout.getText().toString().trim();
                String experience = editTextExperience.getText().toString().trim();
                String location = editTextLocation.getText().toString().trim();
                String specialization = editTextSpecialization.getText().toString().trim();

                if (about.isEmpty() || experience.isEmpty() || location.isEmpty() || specialization.isEmpty()) {
                    Toast.makeText(WorkerActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (imageUri != null) {
                    uploadImageToFirebase(imageUri, userId, about, experience, location, specialization);
                } else {
                    Toast.makeText(WorkerActivity.this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                // Resize the image
                Bitmap resizedBitmap = resizeImage(bitmap, 500, 500); // Set max width and height
                // Set the resized bitmap to ImageView
                profilePicture.setImageBitmap(resizedBitmap);
                // Convert the resized bitmap to URI
                imageUri = getImageUri(resizedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    private void uploadImageToFirebase(Uri uri, String userId, String about, String experience, String location, String specialization) {
        StorageReference fileReference = storageReference.child("profile_pictures/" + UUID.randomUUID().toString() + ".jpg");

        fileReference.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                saveWorkerData(userId, about, experience, location, specialization, imageUrl);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Upload Error", "Failed to upload image", e); // Use Log to get more details
                        Toast.makeText(WorkerActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void saveWorkerData(String userId, String about, String experience, String location, String specialization, String imageUrl) {
        Map<String, Object> workerData = new HashMap<>();
        workerData.put("about", about);
        workerData.put("experience", experience);
        workerData.put("location", location);
        workerData.put("specialization", specialization);
        workerData.put("numberOfAssignedJobs", 0);
        workerData.put("role", "worker");
        workerData.put("isAvailable", true);
        workerData.put("profilePictureUrl", imageUrl);

        db.collection("users").document(userId)
                .set(workerData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(WorkerActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(WorkerActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(WorkerActivity.this, "Failed to store additional data in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
