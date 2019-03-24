package com.decoration.seller;

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

public class ItemDetailsActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivItem;
    EditText etName, etWidth, etHeight, etColor, etPrice, etCount;

    // attributes
    String id, name, width, height, color, price, count, image, user_id;
    private static final int GALLERY_REQUEST = 1111;
    InputStream imageStream;
    String imagePath = "";

    // firebase
    FirebaseFirestore db;
    FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        // initialize
        etName = findViewById(R.id.et_item_name);
        etWidth = findViewById(R.id.et_item_width);
        etHeight = findViewById(R.id.et_item_height);
        etColor = findViewById(R.id.et_item_location);
        etPrice = findViewById(R.id.et_item_price);
        etCount = findViewById(R.id.et_item_count);
        ivItem = findViewById(R.id.iv_service_image);

        etName.setEnabled(false);
        etWidth.setEnabled(false);
        etHeight.setEnabled(false);
        etColor.setEnabled(false);
        ivItem.setEnabled(false);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // check for passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            width = getIntent().getStringExtra("width");
            height = getIntent().getStringExtra("height");
            color = getIntent().getStringExtra("color");
            price = getIntent().getStringExtra("price");
            count = getIntent().getStringExtra("count");
            image = getIntent().getStringExtra("image");
            user_id = getIntent().getStringExtra("user_id");
        }

        // set widgets
        etName.setText(name);
        etWidth.setText(width);
        etHeight.setText(height);
        etColor.setText(color);
        etPrice.setText(price);
        etCount.setText(count);

        // show image
        Picasso.get()
                .load(image)
                .resize(200, 200)
                .centerCrop()
                .into(ivItem);
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
                            .into(ivItem);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // method to trigger back button click
    public void goBack(View view) {
        finish();
    }


    // method to trigger delete button click
    public void deleteItem(View view) {
        // create a storage reference
        StorageReference storageRef = storage.getReference();
        String imageName = id + ".jpg";

        // delete image file from storage
        final ProgressDialog dialog = ProgressDialog.show(this, "Removing Item", "Please wait...", false, false);
        storageRef.child(imageName).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    // delete item data from database
                    db.collection("items").document(id).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()) {
                                Toast.makeText(ItemDetailsActivity.this, "Item removed successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ItemDetailsActivity.this, "Cannot delete item data from database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(ItemDetailsActivity.this, "Cannot delete image file from storage", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to trigger update button click
    public void updateItem(View view) {
        // get user input
        String name = etName.getText().toString().trim();
        String width = etWidth.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String count = etCount.getText().toString().trim();

        // validation
        if(name.isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
            return;
        }

        if(width.isEmpty()) {
            Toast.makeText(this, "Please enter width", Toast.LENGTH_SHORT).show();
            return;
        }

        if(height.isEmpty()) {
            Toast.makeText(this, "Please enter height", Toast.LENGTH_SHORT).show();
            return;
        }

        if(price.isEmpty()) {
            Toast.makeText(this, "Please enter price", Toast.LENGTH_SHORT).show();
            return;
        }

        if(color.isEmpty()) {
            Toast.makeText(this, "Please enter color", Toast.LENGTH_SHORT).show();
            return;
        }

        if(count.isEmpty()) {
            Toast.makeText(this, "Please enter count", Toast.LENGTH_SHORT).show();
            return;
        }


        update(name, width, height, price, color, count, imagePath);
    }

    private void update(final String name, final String width, final String height, final String price, final String color, final String count, final String imagePath) {

        // check for new image
        if(imagePath.isEmpty()) {
            // prepare data to be updated
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", id);
            itemData.put("name", name);
            itemData.put("width", width);
            itemData.put("height", height);
            itemData.put("color", color);
            itemData.put("price", price);
            itemData.put("count", count);
            itemData.put("image", image);
            itemData.put("user_id", user_id);

            // update item in database
            final ProgressDialog dialog = ProgressDialog.show(this, "Updating Item", "Please wait...", false, false);
            db.collection("items").document(id).update(itemData).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        Toast.makeText(ItemDetailsActivity.this, "Item has been updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ItemDetailsActivity.this, "Cannot update item", Toast.LENGTH_SHORT).show();
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
                        updateItemWithNewImage(name, width, height, price, color, count, imagePath);
                    } else {
                        Toast.makeText(ItemDetailsActivity.this, "Cannot delete image file from storage", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // method to update item with new image
    private void updateItemWithNewImage(final String name, final String width, final String height, final String price, final String color, final String count, String imagePath) {
        // get current timestamp
        Long ts = System.currentTimeMillis() / 1000;
        final String timestamp = ts.toString();

        // create a dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Updating Item", "Please wait...", false, false);

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
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("id", timestamp);
                    itemData.put("name", name);
                    itemData.put("width", width);
                    itemData.put("height", height);
                    itemData.put("price", price);
                    itemData.put("color", color);
                    itemData.put("count", count);
                    itemData.put("image", downloadUri.toString());
                    itemData.put("user_id", user_id);

                    // add to database
                    db.collection("items").document(id).update(itemData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();

                            if(task.isSuccessful()) {
                                Toast.makeText(ItemDetailsActivity.this, "Item has been updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ItemDetailsActivity.this, "Cannot update item to database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(ItemDetailsActivity.this, "Cannot upload new image to storage", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // method to trigger clear button click
    public void clearFields(View view) {
        etName.setText("");
        etWidth.setText("");
        etHeight.setText("");
        etColor.setText("");
        etCount.setText("");
        etPrice.setText("");
    }
}
