package com.vrnitsolution.healthappnew;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vrnitsolution.healthappnew.DashboardProfile.DashboardProfile;
import com.vrnitsolution.healthappnew.GetNotification.AllNotification;
import com.vrnitsolution.healthappnew.Message.model.UserModel;
import com.vrnitsolution.healthappnew.adapter.DoctorsAdapter;
import com.vrnitsolution.healthappnew.bookappointment.adapter.HistoryAppointment;
import com.vrnitsolution.healthappnew.bookappointment.model.Patient;
import com.vrnitsolution.healthappnew.model.Doctors;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class DashboardHome extends AppCompatActivity implements HistoryAppointment.OnAppointmentItemClickListner {
    FirebaseAuth firebaseAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    TextView username, useremail;
    CircleImageView userProfile, profileUser;
    private DoctorsAdapter doctorAdapter;
    RecyclerView nearbyDoctorRecyclerView, recycler_activeapt;
    TextView notdata1;

    private HistoryAppointment appointmentAdapternew;
    ArrayList<Patient> patients;
    ArrayList<Doctors> doctors;
    TextView notdata;
    ProgressBar progressBar;

    FirebaseUser user;
    private CollectionReference chatCollection = db.collection("chatusers");
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 20 * 1024; // 20KB in bytes
    private Uri selectedImageUri;

    private ProgressDialog uploadProgressDialog;
    ProgressDialog progressDialog;
    private AlertDialog completeProfileDialog;

    private EditText dobText;
    private EditText heightText;
    private EditText weightText;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_home);


        profileUser = new CircleImageView(DashboardHome.this);
        bottomNavigationView=findViewById(R.id.bottomNavigationView);

        // Initialize firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();


        checkProfile(user);



        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);


        progressBar = findViewById(R.id.progressBar);
        notdata = findViewById(R.id.notdata);
        notdata1 = findViewById(R.id.notdata1);


        uploadProgressDialog = new ProgressDialog(this);
        uploadProgressDialog.setMessage("Uploading Image...");
        uploadProgressDialog.setCancelable(false);


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        // Handle home action
                        Toast.makeText(DashboardHome.this, "" + item.getTitle(), Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.navigation_dashboard:
                        // Handle dashboard action
                        startActivity(new Intent(DashboardHome.this, DashboardProfile.class));
                        return true;
                    case R.id.navigation_notifications:
                        // Handle notifications action
                        startActivity(new Intent(DashboardHome.this, AllNotification.class));
                        return true;
                    case R.id.search_bar:
                        startActivity(new Intent(DashboardHome.this, SearchDoctor.class));
                }
                return false;
            }
        });


        username = findViewById(R.id.displayname);
        useremail = findViewById(R.id.email);
        userProfile = findViewById(R.id.profileImage);


        //Set RecyclerView Active Appointments
        recycler_activeapt = findViewById(R.id.recycler_activeapt);
        recycler_activeapt.setHasFixedSize(true);
        recycler_activeapt.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        patients = new ArrayList<>();
        appointmentAdapternew = new HistoryAppointment(DashboardHome.this, patients, this);
        recycler_activeapt.setAdapter(appointmentAdapternew);


        //Nearby Doctor Adapter
        nearbyDoctorRecyclerView = findViewById(R.id.recycler_main);

        nearbyDoctorRecyclerView.setHasFixedSize(true);
        nearbyDoctorRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        doctors = new ArrayList<>();
        doctorAdapter = new DoctorsAdapter(DashboardHome.this, doctors);
        nearbyDoctorRecyclerView.setAdapter(doctorAdapter);


    }


    /**
     * Get All Active Appointments according that doctorId present in appointmentdata collection
     */
    private void getActiveAppointments(String userId) {
        patients.clear();
        db.collection("appointmentdata")
                .whereEqualTo("userId", userId)
                .whereEqualTo("visiting_status", "Not Visited")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        if (patients != null) {
                            patients.clear();
                        }

                        // Get current timestamp
                        Timestamp currentTime = new Timestamp(new Date());

                        for (DocumentSnapshot document : value.getDocuments()) {
                            Patient patient = document.toObject(Patient.class);
                            if (patient != null && patient.getScheduleTime() != null) {
                                // Compare scheduleTime with current time
                                if (patient.getScheduleTime().compareTo(currentTime) > 0) {
                                    // If scheduleTime is in the future, consider it an active appointment
                                    patients.add(patient);
                                }
                            }
                        }

                        if (patients.isEmpty()) {
                            notdata1.setVisibility(View.VISIBLE);
                            notdata1.setText("No Active Appointments");
                        } else {
                            notdata1.setVisibility(View.GONE);
                        }
                        // Notify the adapter that the data has changed
                        appointmentAdapternew.notifyDataSetChanged();
                    }
                });
    }


    /**
     * From this method calls it gets coordinates latitude and longitude from collection
     */
    private void getUserLocation(String uid) {
        // Query the "users" collection to get the current user's coordinates based on UID
        db.collection("users")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Assuming there is only one document for the given UID
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);


                        // Extract latitude and longitude from the coordinates map
                        Map<String, String> coordinates = (Map<String, String>) documentSnapshot.get("currentCoordinates");

                        if (coordinates != null && coordinates.containsKey("latitude") && coordinates.containsKey("longitude")) {
                            String latitude = coordinates.get("latitude");
                            String longitude = coordinates.get("longitude");
                            getNearbyDoctors(latitude, longitude);

                        }

                    } else {
                        // No user found for the given UID
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Toast.makeText(this, "Failed to get data", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * GetNearby Doctor according to the latitude and longitude and account is approved by the admin
     */
    private void getNearbyDoctors(String latitude, String longitude) {
        doctors.clear();
        double userLatitude = Double.parseDouble(latitude);
        double userLongitude = Double.parseDouble(longitude);
        double radius = 10.0; // 10 km radius

        db.collection("doctors")
                .whereEqualTo("account_status", "Approved")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            progressBar.setVisibility(View.GONE);
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }


                        List<Doctors> nearbyDoctorsList = new ArrayList<>();

                        for (QueryDocumentSnapshot document : snapshot) {
                            Map<String, String> coordinates = (Map<String, String>) document.get("coordinates");
                            if (coordinates != null && coordinates.containsKey("latitude") && coordinates.containsKey("longitude")) {
                                double doctorLatitude = Double.parseDouble(coordinates.get("latitude"));
                                double doctorLongitude = Double.parseDouble(coordinates.get("longitude"));

                                float[] results = new float[1];
                                Location.distanceBetween(userLatitude, userLongitude, doctorLatitude, doctorLongitude, results);
                                double distance = results[0] / 1000; // Convert meters to kilometers

                                if (distance <= radius) {
                                    Doctors doctor = document.toObject(Doctors.class);
                                    doctor.setDistance(String.valueOf(distance).substring(0, 3));
                                    nearbyDoctorsList.add(doctor);
                                }
                            }
                        }

                        // Set the nearby doctors list to your adapter
                        doctors.clear();
                        progressBar.setVisibility(View.GONE);
                        doctors.addAll(nearbyDoctorsList);
                        doctorAdapter.notifyDataSetChanged();

                        if (doctors.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            notdata.setVisibility(View.VISIBLE);
                            notdata.setText("No nearby doctors found");
                        } else {
                            notdata.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }


    @Override
    public void onAppintmentItemClick(int position) {
        //open the appointment history activity
        Patient patient = patients.get(position);
        // Format scheduleTime to the desired format
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy 'at' hh:mm a", Locale.getDefault());
        String formattedDate = dateFormat.format(patient.getScheduleTime().toDate());

        Intent appointmentDetailsPage = new Intent(DashboardHome.this, AppointmentDetailsPage.class);
        appointmentDetailsPage.putExtra("PatientName", patient.getPatientName());
        appointmentDetailsPage.putExtra("PatientMobileNo", patient.getPatientMobileNo());
        appointmentDetailsPage.putExtra("PatientProblem", patient.getPatientProblem());
        appointmentDetailsPage.putExtra("scheduleTime", formattedDate);
        appointmentDetailsPage.putExtra("AppointmentId", patient.getAppintmentId());
        appointmentDetailsPage.putExtra("userId", patient.getUserId());
        appointmentDetailsPage.putExtra("docId", patient.getDocId());
        appointmentDetailsPage.putExtra("visiting_status", patient.getVisiting_status());

        startActivity(appointmentDetailsPage);
    }

    public void viewProfile(View view) {
        startActivity(new Intent(DashboardHome.this, UpdateUserProfile.class));
    }



    private void showCompleteProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dilog_choose_image, null);
        builder.setView(dialogView);
        builder.setTitle("Complete Profile");

        AppCompatButton uploadButton = dialogView.findViewById(R.id.uploadButton);

        profileUser = dialogView.findViewById(R.id.profileImageUser);
        profileUser.setOnClickListener(view -> openGallery());

        dobText = dialogView.findViewById(R.id.dobtext);
        heightText = dialogView.findViewById(R.id.heighttext);
        weightText = dialogView.findViewById(R.id.weighttxt);

        uploadButton.setOnClickListener(view -> {
            String dob = dobText.getText().toString().trim();
            String height = heightText.getText().toString().trim();
            String weight = weightText.getText().toString().trim();

            if (TextUtils.isEmpty(dob) || !isValidDateFormat(dob)) {
                displayToast("Please enter a valid date of birth (DD/MM/YYYY)");
                return;
            }

            if (TextUtils.isEmpty(height)) {
                displayToast("Please enter your height");
                return;
            }

            if (TextUtils.isEmpty(weight)) {
                displayToast("Please enter your weight");
                return;
            }
            if (selectedImageUri == null) {
                displayToast("image not selected");
            }

            uploadProgressDialog.setMessage("Uploading Profile Image..");
            uploadProgressDialog.show();
            uploadProfileImage(user, dob, height, weight);
            completeProfileDialog.dismiss();
        });

        completeProfileDialog = builder.create();
        completeProfileDialog.setCanceledOnTouchOutside(false);
        completeProfileDialog.show();
    }

    private void uploadProfileImage(FirebaseUser user, String dob, String height, String weight) {
        long timestamp = System.currentTimeMillis();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profileUser/" + timestamp+user.getUid());

        storageRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                uploadProgressDialog.setMessage("Updating data please wait..");
                updateUsersData(user, dob, height, weight,uri);
            }).addOnFailureListener(e -> {
                displayToast("Failed to get image download URL.");
                uploadProgressDialog.dismiss();
            });
        }).addOnFailureListener(e -> {
            uploadProgressDialog.dismiss();
            displayToast("Failed to upload profile image.");

        });
    }

    private void updateUsersData(FirebaseUser user, String dob, String height, String weight,  Uri uri) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("users");

        String userUid = user.getUid();

        // Assuming the unique identifier is stored in a field named "uid" within the document
        usersCollection.whereEqualTo("uid", userUid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Get the document ID (unique identifier) for the found document
                        String documentId = document.getId();

                        Map<String,Object>userData=new HashMap<>();
                        userData.put("uid",document.get("uid"));
                        userData.put("email",document.get("email"));
                        userData.put("messageToken",document.get("messageToken"));
                        userData.put("profileUrl",uri.toString());
                        userData.put("username",document.get("username"));

                        // Create a map with the updated user data
                        Map<String, Object> updatedUserData = new HashMap<>();
                        updatedUserData.put("dob", dob);
                        updatedUserData.put("height", height);
                        updatedUserData.put("weight", weight);
                        updatedUserData.put("profileUrl",uri.toString());

                        // Update the user data in the Firestore collection using the document ID
                        usersCollection.document(documentId).update(updatedUserData)
                                .addOnSuccessListener(aVoid -> {
                                    uploadProgressDialog.setMessage("Activating chat feature");
                                    registerToChat(userData,userUid,selectedImageUri);
                                    displayToast("Saved User Profile");
                                })
                                .addOnFailureListener(e -> {
                                    uploadProgressDialog.dismiss();
                                    displayToast(e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    displayToast(e.getMessage());
                });
    }

    /**
     * For saving the data in chatCollection we passed the data in UserModel object that set in collection
     *
     * @param user
     * @param uid
     * @param selectedImageUri
     */
    private void registerToChat(Map<String, Object> user, String uid, Uri selectedImageUri) {
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
                    uploadProgressDialog.setMessage("Updating profile");
                    updateFirebaseAuthUserProfile(selectedImageUri);
                    displayToast("Activated chat feature");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                uploadProgressDialog.dismiss();
                displayToast("Failed to activating chat feature"+e.getMessage());
            }
        });
    }


    private boolean isImageSizeValid(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            int imageSize = inputStream.available();
            inputStream.close();

            return imageSize <= MAX_IMAGE_SIZE;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            if (isImageSizeValid(selectedImageUri)) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .into(profileUser);
            } else {
                displayToast("Image size exceeds 20KB");
            }
        }
    }


    private boolean isValidDateFormat(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date parsedDate = sdf.parse(date);
            return parsedDate != null;
        } catch (ParseException e) {
            return false;
        }
    }


    private void updateFirebaseAuthUserProfile(Uri photoUrl) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUrl)
                .build();

        if (user != null) {
            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkProfile(user);
                    uploadProgressDialog.dismiss();
                    displayToast("Profile information and image saved successfully.");
                } else {
                    uploadProgressDialog.dismiss();
                    displayToast("Failed to update user profile.");
                }
            });
        }
    }

    private void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }


    private void checkProfile(FirebaseUser user) {
        if (user!=null)
        {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference usersCollection = db.collection("users");

            String userUid = user.getUid();

            // Assuming the unique identifier is stored in a field named "uid" within the document
            usersCollection.whereEqualTo("uid", userUid).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Get the document ID (unique identifier) for the found document
                            String documentId = document.getId();
                            String profilepic= document.get("profileUrl").toString();
                            String fullname=document.get("username").toString();
                            String email=document.get("email").toString();

                            if (!profilepic.isEmpty()&&!fullname.isEmpty()&&!email.isEmpty())
                            {
                                if (email.length() > 20) {
                                   useremail.setText(email.substring(0, 20) + "...");
                                } else {
                                   useremail.setText(email);
                                }

                                username.setText(fullname);
                                Glide.with(userProfile).load(profilepic).into(userProfile);
                            }else {
                                showCompleteProfileDialog();
                            }

                        }
                    })
                    .addOnFailureListener(e -> {
                        displayToast(e.getMessage());
                    });


        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (user!=null)
        {
            getUserLocation(user.getUid());
            getActiveAppointments(user.getUid());
        }
    }
}


