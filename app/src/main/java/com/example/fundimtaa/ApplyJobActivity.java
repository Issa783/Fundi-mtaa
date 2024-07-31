package com.example.fundimtaa;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ApplyJobActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextPhoneNumber;
    private EditText editTextLocation;
    private EditText editTextDate;
    private EditText editTextExperience;
    private Button buttonApplyJob;
    private String jobId;
    private String jobName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);
        jobId = getIntent().getStringExtra("jobId");
        Log.d("ApplyJobActivity", "Received Job ID: " + jobId);
        jobName = getIntent().getStringExtra("jobName");
        Log.d("ApplyJobActivity", "Received Job Name: " + jobName);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextDate = findViewById(R.id.editTextDate);
        editTextExperience = findViewById(R.id.editTextExperience);
        buttonApplyJob = findViewById(R.id.buttonApplyJob);

        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(editTextDate);
            }
        });

        buttonApplyJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString().trim();
                String date = editTextDate.getText().toString().trim();
                String phoneNumber = editTextPhoneNumber.getText().toString().trim();
                String location = editTextLocation.getText().toString().trim();
                String experience = editTextExperience.getText().toString().trim();

                if (!isValidName(name)) {
                    editTextName.setError("Name must be at least 3 characters long");
                    editTextName.requestFocus();
                } else if (!isValidDate(date)) {
                    editTextDate.setError("Date must be today or in the future");
                    editTextDate.requestFocus();
                } else if (!isValidExperience(experience)) {
                    editTextExperience.setError("Experience must be in the format 'x years' or 'x year'");
                    editTextExperience.requestFocus();
                } else if (!isValidLocation(location)) {
                    editTextLocation.setError("Location should be in the format 'City, Street/Area'");
                    editTextLocation.requestFocus();
                } else if (!isValidPhoneNumber(phoneNumber)) {
                    editTextPhoneNumber.setError("Phone number must be exactly 10 digits");
                    editTextPhoneNumber.requestFocus();
                } else {
                    saveJobApplication(name, date, phoneNumber, location, experience);
                }
            }
        });
    }

    private void showDatePickerDialog(final EditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(ApplyJobActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                        String selectedDate = dateFormat.format(calendar.getTime());
                        editText.setText(selectedDate);
                    }
                }, year, month, dayOfMonth);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private boolean isValidName(String name) {
        return name.length() >= 3; // Adjust the minimum length as needed
    }

    private boolean isValidDate(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            dateFormat.setLenient(false);
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.setTime(dateFormat.parse(date));
            selectedDate.set(Calendar.HOUR_OF_DAY, 0);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            Calendar currentDate = Calendar.getInstance();
            currentDate.set(Calendar.HOUR_OF_DAY, 0);
            currentDate.set(Calendar.MINUTE, 0);
            currentDate.set(Calendar.SECOND, 0);
            currentDate.set(Calendar.MILLISECOND, 0);

            return !selectedDate.before(currentDate);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidExperience(String experience) {
        return Pattern.matches("\\d+\\s+years?", experience.toLowerCase());
    }

    private boolean isValidLocation(String location) {
        // Define a pattern for a valid location
        Pattern pattern = Pattern.compile("^[A-Za-z]+(,\\s*[A-Za-z0-9\\s]+)*$");
        Matcher matcher = pattern.matcher(location);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.length() == 10 && phoneNumber.matches("\\d+");
    }

    private void saveJobApplication(String name, String date, String phoneNumber, String location, String experience) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String workerId = currentUser != null ? currentUser.getUid() : null;
        if (workerId == null) {
            Toast.makeText(ApplyJobActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String documentId = workerId + "_" + jobId;

        db.collection("job_applications")
                .document(documentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            Toast.makeText(ApplyJobActivity.this, "You have already applied for this job", Toast.LENGTH_SHORT).show();
                            clearFields();
                        } else {
                            Map<String, Object> application = new HashMap<>();
                            application.put("name", name);
                            application.put("dateOfApplication", date);
                            application.put("phoneNumber", phoneNumber);
                            application.put("location", location);
                            application.put("experience", experience);
                            application.put("workerId", workerId);
                            application.put("jobId", jobId);
                            application.put("timestamp", Timestamp.now());
                            application.put("isCanceled", false);

                            db.collection("job_applications")
                                    .document(documentId)
                                    .set(application)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ApplyJobActivity.this, "Application submitted successfully", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                        fetchClientIdAndNotify(workerId, jobId, jobName);
                                        navigateToPreviousPage();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ApplyJobActivity.this, "Failed to submit application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(ApplyJobActivity.this, "Error checking application status: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearFields() {
        editTextName.setText("");
        editTextDate.setText("");
        editTextPhoneNumber.setText("");
        editTextLocation.setText("");
        editTextExperience.setText("");
    }

    private void fetchClientIdAndNotify(String workerId, String jobId, String jobName) {
        db.collection("jobs")
                .document(jobId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String clientId = documentSnapshot.getString("clientId");
                        if (clientId != null) {
                            notifyJobApplication(workerId, jobId, jobName, clientId);
                        } else {
                            Log.e("NotifyJobApplication", "clientId is null for jobId: " + jobId);
                        }
                    } else {
                        Log.e("NotifyJobApplication", "Job document not found for jobId: " + jobId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NotifyJobApplication", "Failed to fetch job document: " + e.getMessage());
                });
    }

    private void notifyJobApplication(String workerId, String jobId, String jobName, String clientId) {
        Log.d("NotifyJobApplication", "clientId: " + clientId);
        Log.d("NotifyJobApplication", "workerId: " + workerId);
        Log.d("NotifyJobApplication", "jobId: " + jobId);
        Log.d("NotifyJobApplication", "jobName: " + jobName);

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonBody = "{\"clientId\":\"" + clientId + "\", \"workerId\":\"" + workerId + "\", \"jobId\":\"" + jobId + "\", \"jobName\":\"" + jobName + "\"}";
        RequestBody body = RequestBody.create(jsonBody, JSON);

        String url = "https://notify-1-wk1o.onrender.com/notify-job-application";
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NotifyJobApplication", "Failed to send notification: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("NotifyJobApplication", "Unexpected code " + response);
                } else {
                    Log.d("NotifyJobApplication", "Notification sent successfully");
                }
            }
        });
    }

    private void navigateToPreviousPage() {
        Intent intent = new Intent(ApplyJobActivity.this, WorkerViewJobs.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
