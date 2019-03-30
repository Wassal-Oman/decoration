package com.decoration.engineer;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.MailSender;
import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EngineerAppointDetailsActivity extends AppCompatActivity {

    // widgets
    TextView tvDate, tvTime, tvCustomer, tvStatus;

    // firebase
    FirebaseFirestore db;

    // attributes
    String id, date, time, status, customer_id, service_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_appoint_details);

        // initialize
        tvDate = findViewById(R.id.tv_date);
        tvTime = findViewById(R.id.tv_time);
        tvCustomer = findViewById(R.id.tv_customer_name);
        tvStatus = findViewById(R.id.tv_status);

        db = FirebaseFirestore.getInstance();

        // get passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            date = getIntent().getStringExtra("appointment_date");
            time = getIntent().getStringExtra("appointment_time");
            customer_id = getIntent().getStringExtra("customer_id");
            status = getIntent().getStringExtra("appointment_status");
            service_id = getIntent().getStringExtra("service_id");
        }

        // set widgets
        tvDate.setText(date);
        tvTime.setText(time);
        tvStatus.setText(status);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Details", "Please wait...", false, false);

        // load customer data
        db.collection("users").document(customer_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                dialog.dismiss();
                User user = documentSnapshot.toObject(User.class);
                tvCustomer.setText(user.getName());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
            }
        });
    }

    // method to go back
    public void back(View view) {
        finish();
    }

    // method to accept appointment
    public void acceptAppointment(View view) {
        // progress dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Accepting Appointment", "Please wait...", false, false);

        // change appointment status
        db.collection("appointments").document(id).update("appointment_status", "Accept").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    sendEmailToUser(customer_id, "Your appointment has been accepted");
                    Toast.makeText(EngineerAppointDetailsActivity.this, "Appointment has been accepted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EngineerAppointDetailsActivity.this, "Cannot accept appointment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to reject appointment
    public void rejectAppointment(View view) {
        // progress dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Accepting Appointment", "Please wait...", false, false);

        // change appointment status
        db.collection("appointments").document(id).update("appointment_status", "Reject").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    sendEmailToUser(customer_id, "Your appointment has been rejected");
                    Toast.makeText(EngineerAppointDetailsActivity.this, "Appointment has been accepted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EngineerAppointDetailsActivity.this, "Cannot accept appointment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to handle sending email to user
    private void sendEmailToUser(String id, final String message) {
        // get seller details from database
        db.collection("users").document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get user data
                User user = documentSnapshot.toObject(User.class);

                // check id user exists
                if(user != null) {
                    new SendMail().execute(user.getEmail(), message);
                } else {
                    Toast.makeText(EngineerAppointDetailsActivity.this, "No user details", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EngineerAppointDetailsActivity.this, "Cannot get user data from database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // thread to send email
    private class SendMail extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... args) {

            // attributes
            int status = 0;

            // get values
            String email = args[0];
            String message = args[1];

            // initialize MailSender
            //email
            MailSender sender = new MailSender("aandhford123@gmail.com", "A123456H");

            // send email
            sender.setTo(new String[]{email});
            sender.setFrom("aandhford123@gmail.com");
            sender.setSubject("Your Appointment Status");
            sender.setBody(message);

            try {
                if (sender.send()) {
                    status = 1;
                }
            } catch (Exception e) {
                Log.e("SendMail", e.getMessage(), e);
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            // check status code
            if(integer == 1) {
                Toast.makeText(EngineerAppointDetailsActivity.this, "Email has been sent", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(EngineerAppointDetailsActivity.this, "Cannot send email", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
