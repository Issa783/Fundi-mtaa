package com.example.fundimtaa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView textViewRegisteredUsers, textViewPostedJobs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewRegisteredUsers = findViewById(R.id.textViewRegisteredUsers);
        textViewPostedJobs = findViewById(R.id.textViewPostedJobs);
        Button buttonManageUsers = findViewById(R.id.buttonUserManagement);
        Button buttonManageJobs = findViewById(R.id.buttonJobManagement);
        buttonManageUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class));
            }
        });
        buttonManageJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AdminDashboardActivity.this, ManageJobsActivity.class));
            }
        });


        loadStatistics();
    }

    private void loadStatistics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch number of registered users excluding admins
        db.collection("users")
                .whereNotEqualTo("role", "admin")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int userCount = task.getResult().size();
                        textViewRegisteredUsers.setText(String.valueOf(userCount));
                    } else {
                        Toast.makeText(this, "Failed to load registered users", Toast.LENGTH_SHORT).show();
                    }
                });

        // Fetch number of posted jobs
        db.collection("jobs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int jobCount = task.getResult().size();
                        textViewPostedJobs.setText(String.valueOf(jobCount));
                    } else {
                        Toast.makeText(this, "Failed to load posted jobs", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
