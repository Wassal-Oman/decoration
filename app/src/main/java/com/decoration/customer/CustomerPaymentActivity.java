package com.decoration.customer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.decoration.LoginActivity;
import com.decoration.MailSender;
import com.decoration.R;
import com.decoration.models.Item;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomerPaymentActivity extends AppCompatActivity {

    // widgets
    EditText etCardNumber, etCardYear, etCVV;
    Spinner spMonths;
    LinearLayout linearCardDetails;

    // attributes
    String productId, sellerId;
    float lat, lng;
    Calendar calendar;
    String paymentType = "";
    String name = "", email = "", phone = "";
    String paymentStatus = "Unpaid";
    int count;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_payment);

        // initialize
        etCardNumber = findViewById(R.id.et_card_number);
        etCardYear = findViewById(R.id.et_card_years);
        etCVV = findViewById(R.id.et_cvv);
        spMonths = findViewById(R.id.sp_card_months);
        linearCardDetails = findViewById(R.id.linear_card_details);
        calendar = Calendar.getInstance();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // load months
        String[] months = getResources().getStringArray(R.array.months);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months);
        spMonths.setAdapter(adapter);

        // get passed parameters
        if(getIntent() != null) {
            productId = getIntent().getStringExtra("id");
            sellerId = getIntent().getStringExtra("user_id");
            count = getIntent().getIntExtra("count", 0);
            lat = getIntent().getFloatExtra("latitude", 0.0f);
            lng = getIntent().getFloatExtra("longitude", 0.0f);
        }

        // get default action bar
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // set payment type
        paymentType = "CASH";
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
                    Toast.makeText(CustomerPaymentActivity.this, "Cannot fetch user data from database", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(CustomerPaymentActivity.this, LoginActivity.class));
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

    // method to trigger radio button selection
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rb_cash:
                if (checked)
                    linearCardDetails.setVisibility(View.GONE);
                    paymentType = "CASH";
                    break;
            case R.id.rb_bank_card:
                if (checked)
                    linearCardDetails.setVisibility(View.VISIBLE);
                    paymentType = "CARD";
                    break;
        }
    }

    // method to trigger finish order button
    public void finishOrder(View view) {
        // get user inputs
        String cardNumber = etCardNumber.getText().toString().trim();
        String expiryMonth = spMonths.getSelectedItem().toString();
        String expiryYear = etCardYear.getText().toString();
        String cvv = etCVV.getText().toString().trim();

        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String currentDate = df.format(d);

        String[] currentDateArray = currentDate.split("-");

        String userId = auth.getCurrentUser().getUid();

        // check for payment type
        if(paymentType.equals("CARD")) {
            // validation
            if (cardNumber.isEmpty()) {
                etCardNumber.setError("Please enter card number");
                return;
            }

            if (expiryMonth.isEmpty()) {
                Toast.makeText(this, "Please select a month", Toast.LENGTH_SHORT).show();
                return;
            }

            if (expiryYear.isEmpty()) {
                etCardYear.setError("Please enter expiry year");
                return;
            }

            if (cvv.isEmpty()) {
                etCVV.setError("Please enter CVV / CVC");
                return;
            }

            if(cardNumber.length() != 16) {
                etCardNumber.setError("Card number must be 16 digits");
                return;
            }

            if (Integer.parseInt(expiryMonth) <= Integer.parseInt(currentDateArray[1])) {
                Toast.makeText(this, "Invalid expiry month", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Integer.parseInt(expiryYear) < Integer.parseInt(currentDateArray[0])) {
                Toast.makeText(this, "Invalid expiry year", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cvv.length() != 3) {
                etCVV.setError("CVV / CVC must be 3 digits");
            }

            // set payment status
            paymentStatus = "Paid";

            // make order
            makeOrder(userId, productId, sellerId, count, lat, lng, paymentStatus);
        } else {

            // make order
            makeOrder(userId, productId, sellerId, count, lat, lng, paymentStatus);
        }
    }

    // method to make order
    private void makeOrder(final String userId, final String productId, final String sellerId, final int count, final float lat, final float lng, final String paymentStatus) {

        db.collection("items").document(productId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Item item = documentSnapshot.toObject(Item.class);

                if(item != null) {
                    int currentCount = Integer.parseInt(item.getCount());
                    Log.d("Current Count", String.valueOf(currentCount));
                    Log.d("Customer Count", String.valueOf(count));

                    if(currentCount >= count) {
                        // prepare data
                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("user_id", userId);
                        orderData.put("product_id", productId);
                        orderData.put("seller_id", sellerId);
                        orderData.put("count", String.valueOf(count));
                        orderData.put("latitude", String.valueOf(lat));
                        orderData.put("longitude", String.valueOf(lng));
                        orderData.put("payment_status", paymentStatus);

                        // dialog
                        final ProgressDialog dialog = ProgressDialog.show(CustomerPaymentActivity.this, "Making Order", "Please wait...", false, false);

                        // store inside database
                        db.collection("orders").document().set(orderData).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                if(task.isSuccessful()) {
                                    // send an email to seller
                                    sendEmailToUser(sellerId, "New order by " + name + " - phone: " + phone + " - email: " + email);
                                    sendEmailToUser(userId, "Your order has been made successfully");
                                    subtractFromProductCount(productId);
                                    Toast.makeText(CustomerPaymentActivity.this, "Thank you for shopping through our app", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(CustomerPaymentActivity.this, "Cannot make order", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(CustomerPaymentActivity.this, "Cannot order items more than what is available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CustomerPaymentActivity.this, "Cannot get item details", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CustomerPaymentActivity.this, "Item not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void subtractFromProductCount(final String productId) {
        // get product data
        db.collection("items").document(productId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Item item = documentSnapshot.toObject(Item.class);

                if(item != null) {
                    int itemCount = Integer.parseInt(item.getCount());

                    // update count
                    int newCount = itemCount - count;

                    db.collection("items").document(productId).update("count", String.valueOf(newCount)).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                Log.d("Product Count", "Count has been updated");
                            } else {
                                Log.d("Product Count", "Cannot update count");
                            }
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CustomerPaymentActivity.this, "Cannot get item details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // method to handle sending email to seller
    private void sendEmailToUser(String id, final String message) {
        // get seller details from database
        db.collection("users").document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get seller data
                User user = documentSnapshot.toObject(User.class);

                if(user != null) {
                    new SendMail().execute(user.getEmail(), message);
                } else {
                    Toast.makeText(CustomerPaymentActivity.this, "No user details", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CustomerPaymentActivity.this, "Cannot get user data from database", Toast.LENGTH_SHORT).show();
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
            sender.setSubject("New Order");
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
                Toast.makeText(CustomerPaymentActivity.this, "Email has been sent", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(CustomerPaymentActivity.this, "Cannot send email", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
