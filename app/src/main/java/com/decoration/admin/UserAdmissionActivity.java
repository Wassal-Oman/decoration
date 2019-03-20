package com.decoration.admin;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserAdmissionActivity extends AppCompatActivity {

    // widgets
    TextView tvName, tvEmail, tvPhone, tvType, tvStatus;

    // attributes
    String id, name, email, phone, type, status;

    // firebase
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_admission);

        // initialize
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvType = findViewById(R.id.tv_type);
        tvStatus = findViewById(R.id.tv_status);

        db = FirebaseFirestore.getInstance();

        // enable home button
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // get passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            email = getIntent().getStringExtra("email");
            phone = getIntent().getStringExtra("phone");
            type = getIntent().getStringExtra("type");
            status = getIntent().getStringExtra("status");
        }

        // set TextView values
        tvName.setText(name);
        tvEmail.setText(email);
        tvPhone.setText(phone);
        tvType.setText(type);
        tvStatus.setText(status);
    }

    // method to handle accept button click event
    public void acceptUser(View view) {
        if(!id.isEmpty()) {
            db.collection("users").document(id).update("status", "Accept").addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(UserAdmissionActivity.this, "User's request has been accepted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(UserAdmissionActivity.this, "Error has occurred .. Cannot accept user's request", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Cannot accept request for this user!", Toast.LENGTH_SHORT).show();
        }
    }

    // method to handle reject button click event
    public void rejectUser(View view) {
        if(!id.isEmpty()) {
            db.collection("users").document(id).update("status", "Reject").addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(UserAdmissionActivity.this, "User's request has been rejected successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(UserAdmissionActivity.this, "Error has occurred .. Cannot accept user's request", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Cannot accept request for this user!", Toast.LENGTH_SHORT).show();
        }
    }
}
