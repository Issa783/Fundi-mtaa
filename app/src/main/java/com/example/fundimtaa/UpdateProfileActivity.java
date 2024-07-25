package com.example.fundimtaa;

import android.app.Activity;
import android.app.ProgressDialog;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageViewProfilePicture;
    private Button btnUploadProfilePicture;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private EditText editTextName, editTextEmail, editTextPhoneNumber,editTextAbout, editTextLocation, editTextExperience, editTextSpecialization;
    private Button btnSaveProfile;
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize UI elements
        imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);
        btnUploadProfilePicture = findViewById(R.id.btnUploadProfilePicture);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextAbout = findViewById(R.id.editTextAbout);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextExperience = findViewById(R.id.editTextExperience);
        editTextSpecialization = findViewById(R.id.editTextSpecialization);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity
            }
        });
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");

        // Set click listener for upload profile picture button
        btnUploadProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        // Set click listener for save profile button
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        // Fetch user details
        fetchUserDetails();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageViewProfilePicture.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveProfile() {
        progressDialog.show();

        if (imageUri != null) {
            StorageReference fileReference = storageReference.child("profile_pictures/" + mAuth.getCurrentUser().getUid() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    saveProfileData(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UpdateProfileActivity.this, "Failed to upload profile picture. Please try again.", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    });
        } else {
            saveProfileData(null);
        }
    }

    private void saveProfileData(String imageUrl) {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String about = editTextAbout.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String experience = editTextExperience.getText().toString().trim();
        String specialization = editTextSpecialization.getText().toString().trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String workerId = currentUser.getUid();

            Map<String, Object> updatedProfile = new HashMap<>();
            updatedProfile.put("name", name);
            updatedProfile.put("email", email);
            updatedProfile.put("phoneNumber", phoneNumber);
            updatedProfile.put("about", about);
            updatedProfile.put("location", location);
            updatedProfile.put("experience", experience);
            updatedProfile.put("specialization", specialization);

            if (imageUrl != null) {
                updatedProfile.put("profilePictureUrl", imageUrl);
            }

            db.collection("users").document(workerId)
                    .update(updatedProfile)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(UpdateProfileActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UpdateProfileActivity.this, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    });
        } else {
            Toast.makeText(UpdateProfileActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    private void fetchUserDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                editTextName.setText(documentSnapshot.getString("name"));
                                editTextEmail.setText(documentSnapshot.getString("email"));
                                editTextPhoneNumber.setText(documentSnapshot.getString("phoneNumber"));
                                editTextAbout.setText(documentSnapshot.getString("about"));
                                editTextLocation.setText(documentSnapshot.getString("location"));
                                editTextExperience.setText(documentSnapshot.getString("experience"));
                                editTextSpecialization.setText(documentSnapshot.getString("specialization"));

                                String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                                if (profilePictureUrl != null) {
                                    Glide.with(UpdateProfileActivity.this).load(profilePictureUrl).into(imageViewProfilePicture);
                                }
                            } else {
                                Toast.makeText(UpdateProfileActivity.this, "User details not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UpdateProfileActivity.this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(UpdateProfileActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
