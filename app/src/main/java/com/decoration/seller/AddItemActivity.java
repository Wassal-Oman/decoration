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

public class AddItemActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivItem;
    EditText etName, etWidth, etHeight, etColor, etPrice, etCount;

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
        setContentView(R.layout.activity_add_item);

        // initialize
        etName = findViewById(R.id.et_item_name);
        etWidth = findViewById(R.id.et_item_width);
        etHeight = findViewById(R.id.et_item_height);
        etColor = findViewById(R.id.et_item_location);
        etPrice = findViewById(R.id.et_item_price);
        etCount = findViewById(R.id.et_item_count);
        ivItem = findViewById(R.id.iv_item_image);

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
                            .into(ivItem);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // method to trigger add item button click
    public void addItem(View view) {
        // get user input
        String name = etName.getText().toString().trim();
        String width = etWidth.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String count = etCount.getText().toString().trim();
        String user_id = auth.getCurrentUser().getUid();

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
        if(count.equals("0")){
            if(count.length() != 1){
                etCount.setError("you can enter only one zero");
                return;
            }
        }

        if(imagePath.isEmpty()) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        addNewItem(name, width, height, price, color, count, user_id, imagePath);
    }

    // method to add new item
    private void addNewItem(final String name, final String width, final String height, final String price, final String color, final String count, final String user_id, String imagePath) {

        // get current timestamp
        Long ts = System.currentTimeMillis() / 1000;
        final String timestamp = ts.toString();

        // create a dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Adding New Item", "Please wait...", false, false);

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
                    db.collection("items").document(timestamp).set(itemData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            dialog.dismiss();

                            if(task.isSuccessful()) {
                                Toast.makeText(AddItemActivity.this, "Item added successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddItemActivity.this, "Cannot add item to database", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(AddItemActivity.this, "Cannot upload image to storage", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // method to trigger back button
    public void goBack(View view) {
        finish();
    }
}
