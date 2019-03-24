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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class RegisterActivity extends AppCompatActivity {

    // widgets
    EditText etName;
    EditText etEmail;
    EditText etPhone;
    EditText etPassword;
    EditText etConfirmPassword;
    Spinner spUserType;

    // firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // crypto
    private static final String ALGORITHM = "AES";
    private static final String KEY = "1Hbfh667adfDEJ78";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // initialize widgets
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spUserType = findViewById(R.id.sp_user_type);

        // initialize firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // load user types from string array
        String[] userTypes = getResources().getStringArray(R.array.user_type);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, userTypes);
        spUserType.setAdapter(adapter);
    }

    // method to trigger sign up button click event
    public void signUp(View view) {
        // get user input
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String userType = spUserType.getSelectedItem().toString();

        // validate user input
        if(name.isEmpty()) {
           // Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
            etName.setError("Please enter name");
            return;
        }

        if(email.isEmpty()) {
           // Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
            etEmail.setError("Please enter email address");
            return;
        }

        if(phone.isEmpty()) {
            //Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            etPhone.setError("Please enter phone number");
            return;
        }

        if(password.isEmpty()) {
           // Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            etPassword.setError("Please enter password");
            return;
        }

        if(confirmPassword.isEmpty()) {
           // Toast.makeText(this, "Please enter confirm password", Toast.LENGTH_SHORT).show();
            etConfirmPassword.setError("Please enter confirm password");
            return;
        }

        if(!isValidEmail(email)) {
            //Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            etEmail.setError("Please enter a valid email address");
            return;
        }

        if(phone.length() != 8) {
           // Toast.makeText(this, "Phone number must be 8 characters", Toast.LENGTH_SHORT).show();
            etPhone.setError("Phone number must be 8 characters");
            return;
        }

        if(!phone.substring(0, 1).equals("9") && !phone.substring(0, 1).equals("7")) {
          //  Toast.makeText(this, "Phone should start with 9 or 7", Toast.LENGTH_SHORT).show();
            etPhone.setError("Phone should start with 9 or 7");
            return;
        }

        if(password.length() < 6) {
            //Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if(!password.equals(confirmPassword)) {
           // Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // register user through firebase authentication and database
        registerNewUser(name, email, phone, password, userType);
    }

    // method to sign up a new user through "Firebase Authentication Service and Firestore"
    private void registerNewUser(final String name, final String email, final String phone, final String password, final String userType) {

        try {
            // encrypt password
            final String encryptedPassword = encrypt(password);

            // create user
            final ProgressDialog dialog = ProgressDialog.show(this, "Registration In Progress...", "please wait...", false);
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        // get created user from firebase authentication
                        final FirebaseUser createdUser = auth.getCurrentUser();

                        if(createdUser != null) {
                            // store user data inside an object
                            final Map<String, String> userData = new HashMap<>();
                            userData.put("id", createdUser.getUid());
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("phone", phone);
                            userData.put("password", encryptedPassword);
                            userData.put("type", userType);

                            if(userType.equals("Customer")) {
                                userData.put("status", "Accept");
                            } else {
                                userData.put("status", "Not Accept");
                                //email from adman
                            }

                            // add user data through firebase database
                            db.collection("users").document(createdUser.getUid()).set(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        // send verification email
                                        createdUser.sendEmailVerification();
                                        auth.signOut();
                                        // go to login activity
                                        if(userType.equals("Customer")){
                                             Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                        }
                                        else if(!userType.equals("Customer")) {
                                            // send email to admin if user is Seller and Decoration Engineer
                                            sendEmailToAdmin(email, userType);
                                        }

                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Cannot store user data", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(RegisterActivity.this, "No user was created", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(RegisterActivity.this, "E-mail already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmailToAdmin(String userEmail, String userType) {
        new SendMail().execute("aandhford123@gmail.com", userEmail, userType);
    }

    // method to trigger back button click event
    public void back(View view) {
        finish();
    }

    // method to validate email address
    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    // encrypt password
    public String encrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(RegisterActivity.ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedByteValue, Base64.DEFAULT);
    }

    // generate key
    private Key generateKey() throws Exception {
        return new SecretKeySpec(RegisterActivity.KEY.getBytes(), RegisterActivity.ALGORITHM);
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
            String userEmail = args[1];
            String userType = args[2];

            // initialize MailSender
            //email
            MailSender sender = new MailSender("aandhford123@gmail.com", "A123456H");

            // send email
            sender.setTo(new String[]{email});
            sender.setFrom("aandhford123@gmail.com");
            sender.setSubject("New User Registration");
            sender.setBody("The " + userType + " who owns the email: " + userEmail + ", has a new registration request");

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
                Toast.makeText(RegisterActivity.this, "Please wait for admin to accept", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(RegisterActivity.this, "Cannot send email", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
