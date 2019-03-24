package com.decoration;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // widgets
    EditText etName;
    EditText etEmail;
    EditText etPhone;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;
    FirebaseStorage storage;

    String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // initialize
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);

        // initialize firebase auth and database
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @Override
    protected void onStart() {
        // get logged user
        FirebaseUser loggedUser = auth.getCurrentUser();

        if(loggedUser != null) {

            // get user details
            final ProgressDialog dialog = ProgressDialog.show(this, "Loading user", "Please wait...", false, false);
            db.collection("users").document(loggedUser.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    dialog.dismiss();
                    // retrieve user data
                    User user = documentSnapshot.toObject(User.class);

                    // check if user exists
                    if(user != null) {
                        etName.setText(user.getName());
                        etEmail.setText(user.getEmail());
                        etPhone.setText(user.getPhone());
                        type = user.getType();
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
        super.onStart();
    }

    public void back(View view) {
        finish();
    }

    // method to trigger update button click
    public void updateAccount(View view) {
        // get user input
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // validation
        if (name.isEmpty()) {
            Toast.makeText(this, "please enter name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(phone.isEmpty()) {
            Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if(phone.length() != 8) {
            Toast.makeText(this, "Phone number must be 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!phone.substring(0, 1).equals("9") && !phone.substring(0, 1).equals("7")) {
            Toast.makeText(this, "Phone should start with 9 or 7", Toast.LENGTH_SHORT).show();
            return;
        }

        // prepare user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("phone", phone);

        // update user data
        final ProgressDialog dialog = ProgressDialog.show(this, "Updating account", "Please wait...", false, false);
        db.collection("users").document(auth.getCurrentUser().getUid()).update(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dialog.dismiss();
                if(task.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Account updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Account cannot be updated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to trigger delete account button click
    public void deleteAccount(View view) {

        // show alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ProfileActivity.this);
        alertDialogBuilder.setMessage("Are you sure to remove account?");
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                delete();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // method to remove user account
    void delete() {
        // check for user type
        if(!type.equals("Admin")) {
            final String user_id = auth.getCurrentUser().getUid();

            // check for user type
            switch (type) {
                case "Seller":
                    deleteItems(user_id);
                    break;
            }

            final ProgressDialog dialog = ProgressDialog.show(this, "Deleting account", "Please wait...", false, false);
            // remove from auth
            auth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()) {
                        // remove from database
                        db.collection("users").document(user_id).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();

                                if(task.isSuccessful()) {
                                    auth.signOut();
                                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(ProfileActivity.this, "Cannot delete data from database", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(ProfileActivity.this, "Cannot delete account from auth", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Admin account cannot be removed", Toast.LENGTH_SHORT).show();
        }
    }

    // method to delete items from database based on user id
    private void deleteItems(String user_id) {

        // delete items related to user id
        db.collection("items").whereEqualTo("user_id", user_id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for (final QueryDocumentSnapshot document : task.getResult()) {

                        // create a storage reference
                        StorageReference storageRef = storage.getReference();
                        final String imageName = document.getId() + ".jpg";

                        Log.d("IMAGE", imageName);

                        // delete image file from storage
                        storageRef.child(imageName).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()) {
                                    Log.d("Storage Deleted", imageName);
                                    // delete item data from database
                                    db.collection("items").document(document.getId()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                Log.d("DB deleted", imageName);
                                            } else {
                                                Log.d("DB not deleted", imageName);
                                            }
                                        }
                                    });
                                } else {
                                    Log.d("Storage not Delete", imageName);
                                }
                            }
                        });
                    }
                }
            }
        });
    }
}
