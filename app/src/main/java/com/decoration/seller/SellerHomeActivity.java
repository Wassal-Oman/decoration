package com.decoration.seller;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.HelpActivity;
import com.decoration.ProfileActivity;
import com.decoration.R;
import com.decoration.ResetPasswordActivity;
import com.decoration.models.Item;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SellerHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // widgets
    TextView drawer_user_name;
    TextView drawer_user_email;
    ListView lvItems;
    SearchView searchView;
    FloatingActionButton fab;

    // drawer
    DrawerLayout drawer;
    NavigationView navigation;

    // firebase authentication and database
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // data
    List<Item> items;
    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_home);

        // initialize
        drawer = findViewById(R.id.drawer_layout);
        navigation = findViewById(R.id.nav_view);
        lvItems = findViewById(R.id.lv_items);
        searchView = findViewById(R.id.search_view);
        fab = findViewById(R.id.fab);
        items = new ArrayList<>();


        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // show home button with title
        if(getSupportActionBar() != null){
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // initialize header widgets
        View header = navigation.getHeaderView(0);
        drawer_user_name = header.findViewById(R.id.drawer_user_name);
        drawer_user_email= header.findViewById(R.id.drawer_user_email);

        // set navigation listener
        navigation.setNavigationItemSelectedListener(this);

        // add fab click listener
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SellerHomeActivity.this, AddItemActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // get logged user
        FirebaseUser loggedUser = auth.getCurrentUser();

        if(loggedUser != null) {
            // get user details
            db.collection("users").document(loggedUser.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {

                    // retrieve user data
                    User user = documentSnapshot.toObject(User.class);

                    // check if user exists
                    if(user != null) {
                        drawer_user_name.setText(user.getName());
                        drawer_user_email.setText(user.getEmail());
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    auth.signOut();
                    finish();
                }
            });
        }

        // load list of items
        loadListOfItems();
    }

    // method to load list of items
    private void loadListOfItems() {

        // clear list
        items.clear();
        lvItems.setAdapter(null);

        // fetch all items from database
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Items", "Please wait...", false, false);
        db.collection("items").whereEqualTo("user_id", auth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                dialog.dismiss();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Item item = document.toObject(Item.class);
                    items.add(item);
                }

                // view list of items
                viewListOfItems(items);
            }
        });
    }

    // method to view list of items
    private void viewListOfItems(List<Item> items) {
        if(items.size() > 0) {
            adapter = new MyAdapter(this, 0, items);
            lvItems.setTextFilterEnabled(true);
            lvItems.setAdapter(adapter);
            lvItems.setDivider(null);

            // search through ListView
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    // filter
                    adapter.getFilter().filter(s);
                    return false;
                }
            });

            // detect changes in Array Adapter
            adapter.notifyDataSetChanged();

            // go to details page based on user selection
            lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // get selected item
                    int pos = (int) adapter.getItemId(position);
                    Item _item = (Item) adapter.getItem(pos);

                    // go to details activity
                    Intent intent = new Intent(SellerHomeActivity.this, ItemDetailsActivity.class);
                    intent.putExtra("id", _item.getId());
                    intent.putExtra("name", _item.getName());
                    intent.putExtra("width", _item.getWidth());
                    intent.putExtra("height", _item.getHeight());
                    intent.putExtra("color", _item.getColor());
                    intent.putExtra("price", _item.getPrice());
                    intent.putExtra("count", _item.getCount());
                    intent.putExtra("image", _item.getImage());
                    intent.putExtra("user_id", _item.getUser_id());
                    startActivity(intent);
                }
            });

        } else {
            Toast.makeText(this, "No Items Available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if(id == android.R.id.home) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            if(auth.getCurrentUser() != null) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else {
                Toast.makeText(this, "You are not logged in!", Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.nav_reset_password) {
            if (auth.getCurrentUser() != null) {
                startActivity(new Intent(this, ResetPasswordActivity.class));
            } else {
                Toast.makeText(this, "You are not logged in!", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, SellerOrderListActivity.class));
        } else if(id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if(id == R.id.nav_exit) {
            // sign out
            auth.signOut();
            finish();
        }

        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        items.clear();
    }

    // private class to view data in ListView as custom list items
    class MyAdapter extends ArrayAdapter implements Filterable {

        private List<Item> data;
        private LayoutInflater inflater = null;
        Item item = null;

        public MyAdapter(@NonNull Context context, int resource, @NonNull List list) {
            super(context, resource, list);
            this.data = list;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public long getItemId(int i) {
            int id;

            // original list will be null only if we haven't filtered yet
            if (items == null) {
                id = i;
            } else {
                id = items.indexOf(data.get(i));
            }
            return id;
        }

        public class ViewHolder{

            public ImageView image;
            public TextView name;
            public TextView color;
            public TextView price;
        }

        @NonNull
        @Override
        public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
            @SuppressLint("ViewHolder") View v = inflater.inflate(R.layout.custom_item_layout, null);
            MyAdapter.ViewHolder holder = new MyAdapter.ViewHolder();

            if(v != null){
                holder.image = v.findViewById(R.id.iv_service_image);
                holder.name = v.findViewById(R.id.tv_name);
                holder.color = v.findViewById(R.id.tv_color);
                holder.price = v.findViewById(R.id.tv_price);
            }

            if(data.size() <= 0){
                Toast.makeText(SellerHomeActivity.this, "No Data Available", Toast.LENGTH_LONG).show();
            } else {
                item = data.get(i);
                holder.name.setText(item.getName());
                holder.color.setText(item.getColor());
                holder.price.setText(item.getPrice() + " OMR");
                Picasso.get()
                        .load(item.getImage())
                        .resize(130, 130)
                        .centerCrop()
                        .into(holder.image);
            }

            return v;
        }

        @Override
        public Filter getFilter() {

            // return filtered list
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    data = (List<Item>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<Item> FilteredArrayNames = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase();

                    for (int i = 0; i < items.size(); i++) {
                        Item _item = items.get(i);
                        if (_item.getName().toLowerCase().startsWith(constraint.toString()))  {
                            FilteredArrayNames.add(_item);
                        }
                    }

                    results.count = FilteredArrayNames.size();
                    results.values = FilteredArrayNames;

                    return results;
                }
            };
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }
}
