package com.decoration.engineer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.LoginActivity;
import com.decoration.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddServiceActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivService;
    EditText etName, etDesc, etLocation, etPrice;

    // attributes
    private static final int GALLERY_REQUEST = 1111;
    InputStream imageStream;
    String imagePath = "";

    // firebase
    FirebaseAuth auth;
    FirebaseStorage storage;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_service);

        // initialize
        ivService = findViewById(R.id.iv_service_image);
        etName = findViewById(R.id.et_service_name);
        etDesc = findViewById(R.id.et_service_desc);
        etLocation = findViewById(R.id.et_service_location);
        etPrice = findViewById(R.id.et_service_price);

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(auth.getCurrentUser() == null) {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    public void goBack(View view) {
        finish();
    }

    // method to load image from gallery
    public void selectImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_REQUEST) {

                try {
                    // get file stream
                    final Uri imageUri = data.getData();
                    imageStream = getContentResolver().openInputStream(imageUri);
                    imagePath = imageUri.getPath();

                    // load image into ImageView
                    Picasso.get()
                            .load(data.getData())
                            .noPlaceholder()
                            .centerCrop()
                            .fit()
                            .into(ivService);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // method to trigger add service button click
    public void addService(View view) {
        // get user inputs
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String user_id = auth.getCurrentUser().getUid();

        // validation
        if(name.isEmpty()) {
            Toast.makeText(this, "Please enter service name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(desc.isEmpty()) {
            Toast.makeText(this, "Please enter service description", Toast.LENGTH_SHORT).show();
            return;
        }

        if(price.isEmpty()) {
            Toast.makeText(this, "Please enter service price", Toast.LENGTH_SHORT).show();
            return;
        }

        if(location.isEmpty()) {
            Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show();
            return;
        }

        addNewService(name, desc, price, location, user_id, imagePath);
    }

    // method to add service
    private void addNewService(final String name, final String desc, final String price, final String location, final String user_id, String imagePath) {

        // get current timestamp
        Long ts = System.currentTimeMillis() / 1000;
        final String timestamp = ts.toString();

        // create a dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Adding New Service", "Please wait...", false, false);

        // upload image to Firebase Storage
        final StorageReference storageRef = storage.getReference();
        UploadTask uploadTask = storageRef.child(timestamp + ".jpg").putStream(imageStream);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return storageRef.child(timestamp + ".jpg").getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();

                    // prepare data
                    Map<String, Object> serviceData = new HashMap<>();
                    serviceData.put("id", timestamp);
                    serviceData.put("name", name);
                    serviceData.put("location", location);
                    serviceData.put("description", desc);
                    serviceData.put("price", price);
                    serviceData.put("image", downloadUri.toString());
                    serviceData.put("user_id", user_id);

                    // add to database
                    db.collection("services").document(timestamp).set(serviceData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();

                            if(task.isSuccessful()) {
                                Toast.makeText(AddServiceActivity.this, "Service added successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddServiceActivity.this, "Cannot add service to database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(AddServiceActivity.this, "Cannot upload image to storage", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
