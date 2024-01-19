package com.vrnitsolution.healthappnew;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vrnitsolution.healthappnew.model.Coordinates;

import java.util.HashMap;

public class UserRegistration extends AppCompatActivity implements LocationListener {
    EditText userfullname, useremail, usermobileno, userpassword;
    String name, email, mobile, password;
    ImageView togglebutton,backBtnImageview;
    private boolean toggle = false;
    String token = "";
    FirebaseAuth firebaseAuth;
    Coordinates coordinates;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LocationManager locationManager;
    ProgressDialog progressDialog;

    private static final String LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_REQ_CODE = 1005;

    private boolean locationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        retriveLocation();
        getFcmToken();

        userfullname = findViewById(R.id.doctorName);
        useremail = findViewById(R.id.doctorEmail);
        usermobileno = findViewById(R.id.doctorMobile);
        userpassword = findViewById(R.id.doctorPassword);
        togglebutton = findViewById(R.id.togglepassword);
        backBtnImageview=findViewById(R.id.backBtnImageview);

        backBtnImageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        togglebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggle == false) {
                    toggle = true;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_on);
                    userpassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    toggle = false;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_off);
                    userpassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });
    }

    private void getFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    token = task.getResult();
                    Log.d(TAG, "FCM token: " + token);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();
            } else if ((!ActivityCompat.shouldShowRequestPermissionRationale(UserRegistration.this, LOCATION_PERMISSION)
                    && !(ActivityCompat.shouldShowRequestPermissionRationale(UserRegistration.this, LOCATION_COARSE)))) {
                showPermissionAlertDialog();
            }
        } else {
            requestRuntimePermission();
        }
    }

    public void retriveLocation() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null) {
                String lat = String.valueOf(location.getLatitude());
                String longit = String.valueOf(location.getLongitude());

                coordinates = new Coordinates(lat, longit);

                if (coordinates == null) {
                    displayToast("Coordinates not getting. Please make sure connected to the internet and location on");
                }
            }
        } else {
            requestRuntimePermission();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        String lat = String.valueOf(location.getLatitude());
        String longit = String.valueOf(location.getLongitude());

        coordinates = new Coordinates(lat, longit);
    }

    private void requestRuntimePermission() {
        if ((ActivityCompat.checkSelfPermission(UserRegistration.this, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(UserRegistration.this, LOCATION_COARSE) == PackageManager.PERMISSION_GRANTED)) {
            locationPermissionGranted = true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(UserRegistration.this, LOCATION_PERMISSION)
                && ActivityCompat.shouldShowRequestPermissionRationale(UserRegistration.this, LOCATION_COARSE)) {
            showPermissionAlertDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION, LOCATION_COARSE}, PERMISSION_REQ_CODE);
        }
    }

    private void showPermissionAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(UserRegistration.this);
        builder.setMessage("This feature is unavailable because it requires permission that you have denied."
                        + " Please allow Location permission from settings to proceed further")
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

    public void registerUser(View view) {
        name = userfullname.getText().toString().trim();
        email = useremail.getText().toString().trim();
        mobile = usermobileno.getText().toString().trim();
        password = userpassword.getText().toString().trim();

        if (coordinates == null && locationPermissionGranted) {
            displayToast("Waiting for location information. Please try again.");
        } else if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || password.isEmpty()) {
            displayToast("Enter all data correctly");
        } else if (!(password.length() >= 6)) {
            displayToast("Enter at least 6 characters password");
        } else if (mobile.length() != 10) {
            displayToast("Mobile number must be 10 digits");
        } else {
            sendToRegisterDetails(name, email, mobile, password, coordinates, "");
            progressDialog.setMessage("Wait while registering a new account");
            progressDialog.show();
        }
    }

    private void sendToRegisterDetails(String name, String email, String mobile, String password, Coordinates coordinates, String imageUrl) {
        HashMap<String, Object> userdata = new HashMap<>();
        userdata.put("username", name);
        userdata.put("email", email);
        userdata.put("phoneNo", mobile);
        userdata.put("currentCoordinates", coordinates);
        userdata.put("messageToken", token);
        userdata.put("profileUrl", "");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user != null) {
                            userdata.put("uid", user.getUid());

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            displayToast("User registration successful!");
                                            createNewUserInFirestore(userdata,user.getUid());
                                        } else {
                                            displayToast("Failed to update user profile information.");
                                        }
                                    });
                        }
                    } else {
                        progressDialog.dismiss();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        displayToast("Authentication failed." + task.getException().toString());
                    }
                });
    }

//    private void createNewUserIntheFireStore(HashMap<String, Object> userdata, String uid) {
//        db.collection("users").add(userdata).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentReference> task) {
//                if (task.isSuccessful()) {
//                    displayToast("User account created");
//
//                    FirebaseUser user = firebaseAuth.getCurrentUser();
//
//                    if (user != null) {
//                        progressDialog.dismiss();
//                        startActivity(new Intent(UserRegistration.this, DashboardHome.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//                        finish();
//                    }
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                progressDialog.dismiss();
//                displayToast("" + e.getMessage().toString());
//            }
//        });
//    }


    private void createNewUserInFirestore(HashMap<String, Object> userdata, String uid) {
        db.collection("users").document(uid)
                .set(userdata)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            displayToast("User account created");

                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            if (user != null) {
                                progressDialog.dismiss();
                                startActivity(new Intent(UserRegistration.this, DashboardHome.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                finish();
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        displayToast("" + e.getMessage().toString());
                    }
                });
    }


    public void loginUser(View view) {
        startActivity(new Intent(UserRegistration.this, LoginActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestRuntimePermission();
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
}
