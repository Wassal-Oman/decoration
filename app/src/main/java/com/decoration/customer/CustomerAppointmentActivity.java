package com.decoration.customer;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.decoration.HttpHelper;
import com.decoration.LoginActivity;
import com.decoration.MailSender;
import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomerAppointmentActivity extends AppCompatActivity implements OnMapReadyCallback {

    // attributes
    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    Calendar calendar;
    String serviceId, engineerId, name, email, phone;

    // widgets
    EditText etDate, etTime;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_appointment);

        // initialize
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        etDate = findViewById(R.id.et_date);
        etTime = findViewById(R.id.et_time);

        calendar = Calendar.getInstance();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // get passed data
        if(getIntent() != null) {
            serviceId = getIntent().getStringExtra("service_id");
            engineerId = getIntent().getStringExtra("engineer_id");
        }

        // get default toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // check if user logged in
        if(auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // fetch user data
            final ProgressDialog dialog = ProgressDialog.show(this, "Fetching User Data", "Please wait...", false, false);
            db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    dialog.dismiss();
                    User user = documentSnapshot.toObject(User.class);
                    name = user.getName();
                    email = user.getEmail();
                    phone = user.getPhone();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    dialog.dismiss();
                    Toast.makeText(CustomerAppointmentActivity.this, "Cannot fetch user data from database", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CustomerAppointmentActivity.this, LoginActivity.class));
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // add a marker
        LatLng latLng = new LatLng(HttpHelper.LATITUDE, HttpHelper.LONGITUDE);
        MarkerOptions marker = new MarkerOptions().position(latLng).title("My Location");

        // config map
        mMap.addMarker(marker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    // method to trigger date button click
    public void pickDate(View view) {
        // date picker
        DatePickerDialog.OnDateSetListener mDatePicker = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // get date
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String myFormat = "yyyy-MM-dd";
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
                etDate.setText(sdf.format(calendar.getTime()));
            }
        };

        new DatePickerDialog(this, mDatePicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // method to trigger time button click
    public void pickTime(View view) {

        // get hours and minutes
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // time picker
        TimePickerDialog mTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                etTime.setText( selectedHour + ":" + selectedMinute);
            }
        }, hour, minute, true);

        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    // method to trigger make appointment button
    public void makeAppointment(View view) {
        // get user inputs
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String userId = auth.getCurrentUser().getUid();

        // validation
        if(date.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if(time.isEmpty()) {
            Toast.makeText(this, "Please enter a time", Toast.LENGTH_SHORT).show();
            return;
        }

        // check for date
        try {
            if (new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).before(new Date())) {
                Toast.makeText(this, "Cannot choose an old date", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        confirmAppoinment(userId, serviceId, engineerId, date, time);
    }

    private void confirmAppoinment(final String userId, String serviceId, final String engineerId, String date, String time) {
        // prepare data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("user_id", userId);
        appointmentData.put("engineer_id", engineerId);
        appointmentData.put("service_id", serviceId);
        appointmentData.put("appointment_date", date);
        appointmentData.put("appointment_time", time);
        appointmentData.put("appointment_status", "Not Accept");

        final ProgressDialog dialog = ProgressDialog.show(this, "Making Appointment", "Please wait...", false, false);

        db.collection("appointments").document().set(appointmentData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    sendEmailToUser(engineerId, "New appointment by: " + name + " - email: " + email + " - phone: " + phone);
                    sendEmailToUser(userId, "Your appointment has been set for you");
                    Toast.makeText(CustomerAppointmentActivity.this, "Appointment has been set with engineer", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to handle sending email to user
    private void sendEmailToUser(String id, final String message) {
        // get user details from database
        db.collection("users").document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get user data
                User user = documentSnapshot.toObject(User.class);

                if(user != null) {
                    new SendMail().execute(user.getEmail(), message);
                } else {
                    Toast.makeText(CustomerAppointmentActivity.this, "No user details", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CustomerAppointmentActivity.this, "Cannot get user data from database", Toast.LENGTH_SHORT).show();
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
            MailSender sender = new MailSender("aandhford123@gmail.com", "A123456H");

            // send email
            sender.setTo(new String[]{email});
            sender.setFrom("aandhford123@gmail.com");
            sender.setSubject("New Appointment");
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
                Toast.makeText(CustomerAppointmentActivity.this, "Email has been sent", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(CustomerAppointmentActivity.this, "Cannot send email", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
