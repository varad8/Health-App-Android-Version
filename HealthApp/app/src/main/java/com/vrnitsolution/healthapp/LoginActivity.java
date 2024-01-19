package com.vrnitsolution.healthapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
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
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vrnitsolution.healthapp.Admin.AdminLogin;
import com.vrnitsolution.healthapp.DoctorUI.DoctorDashboard;
import com.vrnitsolution.healthapp.DoctorUI.DoctorLogin;
import com.vrnitsolution.healthapp.DoctorUI.IntroActivity;

import com.vrnitsolution.healthapp.Message.model.UserModel;
import com.vrnitsolution.healthapp.model.Coordinates;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, LocationListener {

    GoogleSignInClient googleSignInClient;
    FirebaseAuth firebaseAuth;

    AppCompatButton googleButton;
    ImageView menu1;
    Coordinates coordinates;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String token = "";
    private LocationManager locationManager;
    ProgressDialog progressDialog;

    private static final String LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_REQ_CODE = 1005;
    private CollectionReference chatCollection = db.collection("chatusers");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Window window = this.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        googleButton = findViewById(R.id.sign_in_button);


        // Initialize sign in options the client-id is copied form google-services.json file
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("479825602375-a5dikea3cdprdoo82a6rilpc0ihuqdlm.apps.googleusercontent.com")
                .requestEmail()
                .build();


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


        getFcmToken();


        // Initialize sign in client
        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, googleSignInOptions);

        googleButton.setOnClickListener((View.OnClickListener) view -> {
            retriveLocation();
        });

    }


    /**
     * Get Device token for the Message
     */
    private void getFcmToken() {
        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get the FCM token
                    token = task.getResult();
                    Log.d(TAG, "FCM token: " + token);

                    // Now you can save this token to your Firestore database or handle it as needed
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        progressDialog.show();

        // Check condition
        if (requestCode == 100) {
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (signInAccountTask.isSuccessful()) {
                String s = "Google sign in successful";
                displayToast(s);
                try {
                    GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);
                    if (googleSignInAccount != null) {
                        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
                        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String uid = firebaseAuth.getCurrentUser().getUid();
                                    String userName = firebaseAuth.getCurrentUser().getDisplayName();
                                    String email = firebaseAuth.getCurrentUser().getEmail();
                                    String phoneNo = firebaseAuth.getCurrentUser().getPhoneNumber();
                                    String profileUrl = String.valueOf(firebaseAuth.getCurrentUser().getPhotoUrl());

                                    //When Sign In Success then save the data to the FireStore Collection
                                    createNewUser(uid, userName, email, phoneNo, profileUrl, coordinates);

                                } else {
                                    progressDialog.dismiss();
                                    displayToast("Authentication Failed :" + task.getException().getMessage());
                                }
                            }
                        });
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * When Login Success then save that data in firestore collection [Users]
     * But First check the data is present or not in that Collection users otherwise create new users
     * collection
     */
    public void createNewUser(String uid, String userName, String email, String phoneNo, String profileUrl, Coordinates coordinates) {
        // Query Firestore to check if user with the given UID already exists
        db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                //if the data is not empty then update the coordinates
                                updateTheCoordinates(uid, coordinates);
                            } else {
                                // User with the given UID does not exist, save the data
                                Map<String, Object> user = new HashMap<>();
                                user.put("uid", uid);
                                user.put("username", userName);
                                user.put("email", email);
                                user.put("messageToken", token);
                                user.put("phoneNo", phoneNo);
                                user.put("profileUrl", profileUrl);
                                user.put("currentCoordinates", coordinates);


                                db.collection("users")
                                        .add(user)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                registerToChat(user, uid);
                                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                                displayToast("Firebase authentication successful");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                Log.w(TAG, "Error adding document", e);
                                            }
                                        });
                            }

                        } else {
                            progressDialog.dismiss();
                            displayToast("Error checking user existence");
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
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
                                                Toast.makeText(LoginActivity.this, "Location updated successfully", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(LoginActivity.this,DashboardHome.class));
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
     * For saving the data in chatCollection we passed the data in UserModel object that set in collection
     *
     * @param user
     * @param uid
     */
    private void registerToChat(Map<String, Object> user, String uid) {
        String userid = (String) user.get("uid");
        String username = (String) user.get("username");
        String email = (String) user.get("email");
        String messageToken = (String) user.get("messageToken");
        String profileUrl = (String) user.get("profileUrl");


        UserModel userModel = new UserModel(userid, profileUrl, username, "user", email, messageToken);


        chatCollection.document(uid).set(userModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    progressDialog.dismiss();
                    // When task is successful redirect to profile activity display Toast
                    displayToast("Activated chat feature");
                    finish();
                    startActivity(new Intent(LoginActivity.this, DashboardHome.class));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                displayToast("Failed to activating chat feature");
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted.", Toast.LENGTH_SHORT).show();
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

                if (coordinates != null) {
                    Intent intent = googleSignInClient.getSignInIntent();
                    startActivityForResult(intent, 100);
                }
            }
        } else {
            requestRuntimePermission();
        }
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

        // Initialize firebase user
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        SharedPreferences preferences = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);

        // Check condition
        if (firebaseUser != null) {
            // When user already sign in redirect to profile activity
            finish();
            finishAffinity();
            startActivity(new Intent(LoginActivity.this, DashboardHome.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (preferences != null) {
            // Retrieve doctor data from SharedPreferences
            String doctorId = preferences.getString("doctorId", "");
            String email = preferences.getString("email", "");
            if (!doctorId.isEmpty() && !email.isEmpty()) {
                finish();
                finishAffinity();
                startActivity(new Intent(LoginActivity.this, DoctorDashboard.class));
            }
        }


    }

}