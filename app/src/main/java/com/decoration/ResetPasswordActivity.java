package com.decoration;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ResetPasswordActivity extends AppCompatActivity {

    // widgets
    EditText etOldPassword;
    EditText etNewPassword;
    EditText etConfirmPassword;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    // crypto
    private static final String ALGORITHM = "AES";
    private static final String KEY = "1Hbfh667adfDEJ78";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // initialize
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        // initialize firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // method to handle button click
    public void resetPassword(View view) {
        // get inputs
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // validation
        if(oldPassword.isEmpty()) {
            Toast.makeText(this, "Please enter old password", Toast.LENGTH_SHORT).show();
            return;
        }

        if(newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if(confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm password", Toast.LENGTH_SHORT).show();
            return;
        }


        if(oldPassword.length() < 6) {
            Toast.makeText(this, "Old password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if(newPassword.length() < 6) {
            Toast.makeText(this, "Old password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // reset
        reset(oldPassword, newPassword);
    }
    // method to fetch user data and compare old password
    private void reset(final String oldPassword, final String newPassword) {
        final ProgressDialog dialog = ProgressDialog.show(this, "Fetching user data...", "Please wait...", false, false);
        if(auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    dialog.dismiss();
                    // retrieve user data
                    User user = documentSnapshot.toObject(User.class);

                    // check if user exists
                    if(user != null) {
                        // compare between old password and new password
                        try {
                            String encryptedOldPassword = encrypt(oldPassword);
                            if(user.getPassword().equals(encryptedOldPassword)) {
                                // reset password
                                updateUserPassword(newPassword);
                            } else {
                                Toast.makeText(ResetPasswordActivity.this, "Old password is not correct", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    dialog.dismiss();
                    auth.signOut();
                    finish();
                }
            });
        }
    }

    // method to update password in auth and database
    private void updateUserPassword(final String newPassword) {
        final ProgressDialog dialog = ProgressDialog.show(this, "Updating password...", "Please wait...", false, false);
        if(auth.getCurrentUser() != null) {
            auth.getCurrentUser().updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        // get encrypted password
                        try {
                            String encryptedNewPassword = encrypt(newPassword);
                            db.collection("users").document(auth.getCurrentUser().getUid()).update("password", encryptedNewPassword).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(ResetPasswordActivity.this, "Password has been reset successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ResetPasswordActivity.this, "Cannot reset password", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Cannot update password in auth", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // method to go back
    public void back(View view) {
        finish();
    }

    // encrypt password
    public String encrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(ResetPasswordActivity.ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedByteValue, Base64.DEFAULT);
    }

    // generate key
    private Key generateKey() throws Exception {
        return new SecretKeySpec(ResetPasswordActivity.KEY.getBytes(), ResetPasswordActivity.ALGORITHM);
    }
}
