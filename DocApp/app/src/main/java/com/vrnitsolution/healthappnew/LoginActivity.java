package com.vrnitsolution.healthappnew;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.vrnitsolution.healthappnew.Admin.AdminDashboard;
import com.vrnitsolution.healthappnew.Admin.AdminLogin;
import com.vrnitsolution.healthappnew.DoctorUI.DoctorDashboard;
import com.vrnitsolution.healthappnew.DoctorUI.DoctorFormPage;
import com.vrnitsolution.healthappnew.DoctorUI.IntroActivity;

import com.vrnitsolution.healthappnew.model.Coordinates;

public class LoginActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, LocationListener {

    GoogleSignInClient googleSignInClient;
    FirebaseAuth firebaseAuth;

    ImageView menu1;
    Coordinates coordinates;
    private boolean toggle = false;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String token = "";
    private LocationManager locationManager;
    ProgressDialog progressDialog;
    ProgressDialog progressDialog1;
    ImageView togglebutton;
    EditText userPassword, email;

    private static final String LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_REQ_CODE = 1005;
    private CollectionReference chatCollection = db.collection("chatusers");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        progressDialog1 = new ProgressDialog(this);
        progressDialog1.setMessage("Loading..");
        progressDialog1.setCancelable(false);

        togglebutton = findViewById(R.id.togglepassword);
        userPassword = findViewById(R.id.userPassword);
        email = findViewById(R.id.email);

        // Initialize firebase auth
        firebaseAuth = FirebaseAuth.getInstance();


        menu1 = findViewById(R.id.menu_1);
        registerForContextMenu(menu1);

        menu1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..."); // Set your message here
        progressDialog.setCancelable(false); // Set whether the dialog can be canceled by pressing back button


        togglebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggle == false) {
                    toggle = true;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_on);
                    userPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    toggle = false;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_off);
                    userPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });


    }


    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.menu1, popup.getMenu());
        popup.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menuForAdminLogin:
                startActivity(new Intent(this, AdminLogin.class));
                return true;

            case R.id.menuForDoctorLogin:
                finish();
                startActivity(new Intent(LoginActivity.this, IntroActivity.class));
                return true;
        }

        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, LOCATION_PERMISSION) && !(ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, LOCATION_COARSE)))) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setMessage("This feature is unavailabe because this feature requires permission that you have denied."
                                + "Please allow Location permission from settings to proceed further")
                        .setTitle("Permission Required")
                        .setCancelable(false)
                        .setNegativeButton("Cancel", (((dialogInterface, i) -> dialogInterface.dismiss())))
                        .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                dialogInterface.dismiss();
                            }
                        });

                builder.show();
            }
        } else {
            requestRuntimePermission();
        }
    }


    /**
     * When this method calls then get the coordinates
     */
    public void retriveLocation() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {        // Initialize location manager and request location updates
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null) {
                String lat = String.valueOf(location.getLatitude());
                String longit = String.valueOf(location.getLongitude());

                coordinates = new Coordinates(lat, longit);
            } else {
                coordinates = new Coordinates("0", "0");
            }
        } else {
            requestRuntimePermission();
        }
    }


    /**
     * Update the coordinates of that user according to userid if match found in collection
     *
     * @param uid
     * @param coordinates
     */

    private void updateTheCoordinates(String uid, Coordinates coordinates) {
        //Update Logic here
        db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // Update the coordinates field for the matching user
                                document.getReference().update("currentCoordinates", coordinates)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                displayToast("Location updated successfully");
                                                startActivity(new Intent(LoginActivity.this, DashboardHome.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(LoginActivity.this, "Failed to update coordinates", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            progressDialog.dismiss();
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }


    /**
     * Get the Coordinates of your current location when user Allow the Permission of Location
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    /**
     * Checking the Location permission and requesting location permission.
     */
    private void requestRuntimePermission() {
        if ((ActivityCompat.checkSelfPermission(LoginActivity.this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(LoginActivity.this, LOCATION_COARSE) == PackageManager.PERMISSION_GRANTED)) {

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, LOCATION_PERMISSION) && ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, LOCATION_COARSE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app requires LOCATION PERMISSION  for feature to work as expected.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{LOCATION_PERMISSION, LOCATION_COARSE}, PERMISSION_REQ_CODE);
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", (((dialogInterface, i) -> dialogInterface.dismiss())));

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION, LOCATION_COARSE}, PERMISSION_REQ_CODE);
        }
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        requestRuntimePermission();
        retriveLocation();
        checkUserLoggedIn(firebaseAuth.getCurrentUser());

    }

    private void checkUserLoggedIn(FirebaseUser currentUser) {
        progressDialog1.show();

        if (currentUser != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference doctorCollection = db.collection("doctors");
            CollectionReference userCollection = db.collection("users");

            String userId = currentUser.getUid();

            // Check if the user is a doctor
            doctorCollection.document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        progressDialog1.dismiss();
                        // Check if the required fields in the doctor document are not empty or null
                        DocumentSnapshot doctorSnapshot = task.getResult();
                        if (doctorSnapshot.contains("specialistIn")
                                && doctorSnapshot.contains("photoUrl")
                                && doctorSnapshot.contains("occupation")) {
                            String specialistIn = doctorSnapshot.getString("specialistIn");
                            String photoUrl = doctorSnapshot.getString("photoUrl");
                            String occupation = doctorSnapshot.getString("occupation");

                            if (specialistIn.isEmpty()
                                    && photoUrl.isEmpty() &&
                                    occupation.isEmpty()) {
                                startActivity(new Intent(LoginActivity.this, DoctorFormPage.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                finish();
                            } else {
                                startActivity(new Intent(LoginActivity.this, DoctorDashboard.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                finish();
                            }
                        }
                    } else {
                        userCollection.document(userId).get().addOnCompleteListener(userTask -> {
                            if (userTask.isSuccessful()) {
                                progressDialog1.dismiss();
                                if (userTask.getResult().exists()) {
                                    startActivity(new Intent(LoginActivity.this, DashboardHome.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    finish();
                                }
                            } else {
                                // Handle user task failure
                                progressDialog1.dismiss();
                            }
                        });
                    }
                } else {
                    // Handle doctor task failure
                    progressDialog1.dismiss();
                }
            });
        } else {
            progressDialog1.dismiss();
            SharedPreferences sharedPreferences = getSharedPreferences("adminData", MODE_PRIVATE);

            // Retrieve data from SharedPreferences
            String savedUsername = sharedPreferences.getString("username", "");
            String savedEmail = sharedPreferences.getString("email", "");
            String savedProfileUrl = sharedPreferences.getString("profileUrl", "");
            String savedAccountType = sharedPreferences.getString("accountType", "");


            if (sharedPreferences!=null)
            {
                if (!savedEmail.isEmpty())
                {
                    startActivity(new Intent(this,AdminDashboard.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }
            }
        }
    }


    public void loginUser(View view) {
        String useremail = email.getText().toString().trim();
        String password = userPassword.getText().toString().trim();

        if (useremail.isEmpty()) {
            displayToast("enter email");
        } else if (password.isEmpty()) {
            displayToast("enter password");
        } else {
            //login
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(useremail, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success");
                    progressDialog.dismiss();
                    displayToast("Login Successfull");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        updateTheCoordinates(user.getUid(), coordinates);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // If sign in fails, display a message to the user.
                    progressDialog.dismiss();
                    displayToast(e.getMessage());
                }
            });
//            firebaseAuth.signInWithEmailAndPassword(useremail, password)
//                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//                            if (task.isSuccessful()) {
//                                // Sign in success, update UI with the signed-in user's information
//                                Log.d(TAG, "signInWithEmail:success");
//                                progressDialog.dismiss();
//                                displayToast("Login Successfull");
//                                FirebaseUser user = firebaseAuth.getCurrentUser();
//
//                                if (user != null) {
//                                    updateTheCoordinates(user.getUid(), coordinates);
//                                }
//
//                            } else {
//                                // If sign in fails, display a message to the user.
//                                progressDialog.dismiss();
//                                displayToast("Authentication failed." + task.getException());
//
//                            }
//                        }
//                    });
        }
    }

    public void registerForUser(View view) {
        startActivity(new Intent(LoginActivity.this, UserRegistration.class));
    }
}