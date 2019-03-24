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

import com.decoration.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class ServiceDetailsActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivService;
    EditText etName, etDesc, etLocation, etPrice;

    // attributes
    String id, name, desc, location, price, image, user_id;
    private static final int GALLERY_REQUEST = 1111;
    InputStream imageStream;
    String imagePath = "";

    // firebase
    FirebaseFirestore db;
    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_details);

        // initialize
        etName = findViewById(R.id.et_item_name);
        etDesc = findViewById(R.id.et_service_desc);
        etLocation = findViewById(R.id.et_item_location);
        etPrice = findViewById(R.id.et_item_price);
        ivService = findViewById(R.id.iv_service_image);

        etName.setEnabled(false);
        ivService.setEnabled(false);


        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // check for passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            desc = getIntent().getStringExtra("description");
            price = getIntent().getStringExtra("price");
            location = getIntent().getStringExtra("location");
            image = getIntent().getStringExtra("image");
            user_id = getIntent().getStringExtra("user_id");
        }

        // set widgets
        etName.setText(name);
        etDesc.setText(desc);
        etLocation.setText(location);
        etPrice.setText(price);

        // show image
        Picasso.get()
                .load(image)
                .resize(200, 200)
                .centerCrop()
                .into(ivService);
    }

    // method to trigger back button click
    public void goBack(View view) {
        finish();
    }

    // method to trigger image view click
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

    // method to delete service
    public void deleteService(View view) {
        // create a storage reference
        StorageReference storageRef = storage.getReference();
        String imageName = id + ".jpg";

        // delete image file from storage
        final ProgressDialog dialog = ProgressDialog.show(this, "Removing Service", "Please wait...", false, false);
        storageRef.child(imageName).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    // delete item data from database
                    db.collection("services").document(id).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()) {
                                Toast.makeText(ServiceDetailsActivity.this, "Service removed successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ServiceDetailsActivity.this, "Cannot delete service data from database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(ServiceDetailsActivity.this, "Cannot delete image file from storage", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to trigger update button
    public void updateService(View view) {
        // get user input
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        // validation
        if(name.isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(desc.isEmpty()) {
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
            return;
        }

        if(location.isEmpty()) {
            Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show();
            return;
        }

        if(price.isEmpty()) {
            Toast.makeText(this, "Please enter price", Toast.LENGTH_SHORT).show();
            return;
        }

        update(name, desc, location, price, imagePath);
    }

    // method to update service
    private void update(final String name, final String desc, final String location, final String price, final String imagePath) {
        // check for new image
        if(imagePath.isEmpty()) {
            // prepare data to be updated
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("id", id);
            serviceData.put("name", name);
            serviceData.put("description", desc);
            serviceData.put("location", location);
            serviceData.put("price", price);
            serviceData.put("image", image);
            serviceData.put("user_id", user_id);

            // update item in database
            final ProgressDialog dialog = ProgressDialog.show(this, "Updating Service", "Please wait...", false, false);
            db.collection("services").document(id).update(serviceData).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        Toast.makeText(ServiceDetailsActivity.this, "Service has been updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ServiceDetailsActivity.this, "Cannot update item", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // create a storage reference
            StorageReference storageRef = storage.getReference();
            String imageName = id + ".jpg";

            // delete image file from storage
            final ProgressDialog dialog = ProgressDialog.show(this, "Deleting old image", "Please wait...", false, false);
            storageRef.child(imageName).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        updateServiceWithNewImage(name, desc, location, price, imagePath);
                    } else {
                        Toast.makeText(ServiceDetailsActivity.this, "Cannot delete image file from storage", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // method to update service with new image
    private void updateServiceWithNewImage(final String name, final String desc, final String location, final String price, String imagePath) {

        // get current timestamp
        Long ts = System.currentTimeMillis() / 1000;
        final String timestamp = ts.toString();

        // create a dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Updating Service", "Please wait...", false, false);

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
                    serviceData.put("description", desc);
                    serviceData.put("location", location);
                    serviceData.put("price", price);
                    serviceData.put("image", downloadUri.toString());
                    serviceData.put("user_id", user_id);

                    // add to database
                    db.collection("services").document(id).update(serviceData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();

                            if(task.isSuccessful()) {
                                Toast.makeText(ServiceDetailsActivity.this, "Service has been updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ServiceDetailsActivity.this, "Cannot update Service to database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(ServiceDetailsActivity.this, "Cannot upload new image to storage", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to clear EditTexts
    public void clearFields(View view) {
        etName.setText("");
        etDesc.setText("");
        etLocation.setText("");
        etPrice.setText("");
    }
}
