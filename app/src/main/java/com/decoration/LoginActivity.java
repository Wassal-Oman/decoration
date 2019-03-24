package com.decoration;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.admin.AdminHomeActivity;
import com.decoration.admin.UserDetailsActivity;
import com.decoration.customer.CustomerHomeActivity;
import com.decoration.engineer.EngineerHomeActivity;
import com.decoration.models.User;
import com.decoration.seller.SellerHomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class LoginActivity extends AppCompatActivity {

    // widgets
    EditText etEmail;
    EditText etPassword;
    TextView tvForgetPassword;

    // firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // crypto
    private static final String ALGORITHM = "AES";
    private static final String KEY = "1Hbfh667adfDEJ78";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // initialize widgets
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvForgetPassword = findViewById(R.id.tv_forget_password);

        // initialize firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // method to trigger sign in button click event
    public void signIn(View view) {
        // get user inputs
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // validate user inputs
        if(email.isEmpty()) {
          //  Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
            etEmail.setError("Please enter email address");
            return;
        }

        if(password.isEmpty()) {
           // Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            etPassword.setError("Please enter password");
            return;
        }

        if(!isValidEmail(email)) {
           // Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            etEmail.setError("Please enter a valid email address");
            return;
        }

        if(password.length() < 6) {
           // Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        // sign in user through firebase authentication
        signInUser(email, password);
    }

    // method to authenticate user through "Firebase Authentication Service" using email and password
    private void signInUser(String email, final String password) {

        // sign in user
        final ProgressDialog dialog = ProgressDialog.show(this, "Login In Progress...", "please wait...", false);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    // get logged in user
                    FirebaseUser loggedUser = auth.getCurrentUser();
                    if(loggedUser != null) {
                        // check if email has been verified
                        if(loggedUser.isEmailVerified()) {
                            // get user data from firebase database
                            db.collection("users").document(loggedUser.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    // retrieve user data
                                    User user = documentSnapshot.toObject(User.class);

                                    // get  password
                                    String originalPassword = user.getPassword();
                                    try {
                                        String decryptedPassword = decrypt(originalPassword);

                                        // check if password is correct
                                        if(password.equals(decryptedPassword)) {

                                            // check status
                                            if(user.getStatus().equals("Accept")) {
                                                Toast.makeText(LoginActivity.this, "Welcome " + user.getName(), Toast.LENGTH_LONG).show();

                                                // go to user page
                                                switch (user.getType()) {
                                                    case "Admin":
                                                        startActivity(new Intent(LoginActivity.this, AdminHomeActivity.class));
                                                        break;
                                                    case "Seller":
                                                        startActivity(new Intent(LoginActivity.this, SellerHomeActivity.class));
                                                        break;
                                                    case "Decoration Engineer":
                                                        startActivity(new Intent(LoginActivity.this, EngineerHomeActivity.class));
                                                        break;
                                                    case "Customer":
                                                        startActivity(new Intent(LoginActivity.this, CustomerHomeActivity.class));
                                                        break;
                                                }

                                            } else if(user.getStatus().equals("Reject"))  {
                                                // show alert dialog
                                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoginActivity.this);
                                                alertDialogBuilder.setMessage("Your registration request has been rejected");
                                                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                });
                                                AlertDialog alertDialog = alertDialogBuilder.create();
                                                alertDialog.show();
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Wait for admin to accept your registration request", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Cannot retrieve user data", Toast.LENGTH_LONG).show();
                                            auth.signOut();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(LoginActivity.this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e("Database Error", e.getMessage());
                                    Toast.makeText(LoginActivity.this, "Cannot retrieve user data", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            Toast.makeText(LoginActivity.this, "Email is not verified!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User does not exist", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Wrong email or password", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // method to trigger sign up button click event
    public void signUp(View view) {
        // go to register activity
        startActivity(new Intent(this, RegisterActivity.class));
    }

    // method to trigger forget password button click event
    public void forgetPassword(View view) {
        // go to forget password activity
        startActivity(new Intent(this, ForgetPasswordActivity.class));
    }

    // method to validate email address
    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public String decrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(LoginActivity.ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedValue64 = Base64.decode(value, Base64.DEFAULT);
        byte[] decryptedByteValue = cipher.doFinal(decryptedValue64);
        return new String(decryptedByteValue, StandardCharsets.UTF_8);
    }

    private Key generateKey() throws Exception {
        return new SecretKeySpec(LoginActivity.KEY.getBytes(), LoginActivity.ALGORITHM);
    }
}