package com.decoration;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ForgetPasswordActivity extends AppCompatActivity {

    // widgets
    EditText etEmail;

    // firebase
    private FirebaseFirestore db;

    // attributes
    List<User> users;

    // crypto
    private static final String ALGORITHM = "AES";
    private static final String KEY = "1Hbfh667adfDEJ78";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // initialize
        etEmail = findViewById(R.id.et_email);
        users = new ArrayList<>();

        // initialize firebase
        db = FirebaseFirestore.getInstance();
    }

    // method to trigger send button click event
    public void send(View view) {
        // get user input
        String email = etEmail.getText().toString().trim();

        if(email.isEmpty()) {
            Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // send a reset password email
        sendResetPasswordEmail(email);
    }

    private void sendResetPasswordEmail(String email) {
        // check for email in database
        final ProgressDialog dialog = ProgressDialog.show(this, "Checking Email...", "Please wait...", false, false);
        db.collection("users").whereEqualTo("email", email).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                dialog.dismiss();
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        User user = document.toObject(User.class);
                        users.add(user);
                    }

                    if(users.size() > 0) {
                        User user = users.get(0);
                        try {
                            String decryptedPassword = decrypt(user.getPassword());
                            Log.d("Password", decryptedPassword);

                            sendMail(user.getEmail(), decryptedPassword);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(ForgetPasswordActivity.this, "This email is not registered", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ForgetPasswordActivity.this, "This email is not registered", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMail(String email, String decryptedPassword) {
        new SendMail().execute(email, decryptedPassword);
    }

    // method to trigger back button click event
    public void back(View view) {
        finish();
    }

    // method to validate email address
    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public String decrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(ForgetPasswordActivity.ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedValue64 = Base64.decode(value, Base64.DEFAULT);
        byte[] decryptedByteValue = cipher.doFinal(decryptedValue64);
        return new String(decryptedByteValue, StandardCharsets.UTF_8);
    }

    private Key generateKey() throws Exception {
        return new SecretKeySpec(ForgetPasswordActivity.KEY.getBytes(), ForgetPasswordActivity.ALGORITHM);
    }

    private class SendMail extends AsyncTask<String, Void, Integer> {

        // progress dialog
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(ForgetPasswordActivity.this, "Sending Email...", "Please wait...", false, false);
        }

        @Override
        protected Integer doInBackground(String... args) {

            // attributes
            int status = 0;

            // get values
            String email = args[0];
            String password = args[1];

            // initialize MailSender
            //email
            MailSender sender = new MailSender("aandhford123@gmail.com", "A123456H");

            // send email
            sender.setTo(new String[]{email});
            sender.setFrom("aandhford123@gmail.com");
            sender.setSubject("Forget Password");
            sender.setBody("Your Password is " + password);

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
            dialog.dismiss();

            // check status code
            if(integer == 1) {
                Toast.makeText(ForgetPasswordActivity.this, "Check your email...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ForgetPasswordActivity.this, "Cannot send email", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
