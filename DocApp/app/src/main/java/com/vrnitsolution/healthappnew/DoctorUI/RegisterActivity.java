package com.vrnitsolution.healthappnew.DoctorUI;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vrnitsolution.healthappnew.R;
import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    EditText doctorName, doctorEmail, doctorMobileNo, doctorPassword;

    String dcName, dcEmail, dcMobile, dcPassword;
    ImageView backBtnImageview;

    ImageView togglebutton;
    private boolean toggle = false;
    String token = "";
    FirebaseAuth mauth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);

        doctorName = findViewById(R.id.doctorName);
        doctorEmail = findViewById(R.id.doctorEmail);
        doctorMobileNo = findViewById(R.id.doctorMobile);
        doctorPassword = findViewById(R.id.doctorPassword);
        togglebutton = findViewById(R.id.togglepassword);

        mauth=FirebaseAuth.getInstance();


        togglebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggle == false) {
                    toggle = true;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_on);
                    doctorPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    toggle = false;
                    togglebutton.setImageResource(R.drawable.baseline_visibility_off);
                    doctorPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        backBtnImageview=findViewById(R.id.backBtnImageview);
        backBtnImageview.setOnClickListener(view -> finish());

    }


    /**
     * Getting data from edittext and check is not empty
     * data is not empty then send that to DoctorFormPage
     * In that the passeord is encrypt and send
     * */

    public void registerDoctor(View view) {
        dcName = doctorName.getText().toString().trim();
        dcEmail = doctorEmail.getText().toString().trim();
        dcMobile = doctorMobileNo.getText().toString().trim();
        dcPassword = doctorPassword.getText().toString().trim();


        if (dcName.isEmpty()) {
    displayToast("Enter doctor name");
        } else if (dcEmail.isEmpty()) {
            displayToast("Enter email");
        } else if (dcMobile.isEmpty()) {
            displayToast("Enter Mobile no");
        } else if (dcMobile.length()!=10) {
            displayToast("mobile no must be 10 digit");
        } else if (doctorPassword.getText().toString().length() != 6) {
            Toast.makeText(this, "Password must be 6 charachter or greater", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.show();

            //send data to Doctor Form Details Page
            HashMap<String, Object> doctorData = new HashMap<>();
            doctorData.put("doctorName", dcName);
            doctorData.put("email", dcEmail);
            doctorData.put("mobileNo", dcMobile);
            doctorData.put("messageToken", token);
            doctorData.put("accountType","doctor");
            doctorData.put("account_status","Not Approved");
            doctorData.put("specialistIn","");
            doctorData.put("photoUrl","");
            doctorData.put("occupation","");

            mauth.createUserWithEmailAndPassword(dcEmail,dcPassword).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                   displayToast("Doctor registration successfully");
                    saveDataInDoctorCollection(doctorData);
                    updateAuthName(dcName);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    displayToast(""+e.getMessage());
                }
            });


        }
    }

    private void updateAuthName(String dcName) {
        FirebaseAuth mauth=FirebaseAuth.getInstance();
        FirebaseUser user=mauth.getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(dcName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        displayToast("Doctor data saved.!");
                    } else {
                        displayToast("Failed to update user profile information.");
                    }
                });
    }

    private void saveDataInDoctorCollection(HashMap<String, Object> doctorData) {
        progressDialog.setMessage("Saving data..");
        doctorData.put("doctorId",mauth.getCurrentUser().getUid());

        // Get a reference to your Firestore collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference doctorCollection = db.collection("doctors");


        doctorCollection.document(mauth.getCurrentUser().getUid()).set(doctorData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                // Handle success
                progressDialog.dismiss();
                startActivity(new Intent(RegisterActivity.this,DoctorFormPage.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                progressDialog.dismiss();
                // Add any error handling here
                displayToast(""+e.getMessage());
            }
        });


    }


    /**
     * This method generate firebase message token accordin that token we perform the POP Notification recived and send for that perticular device
     * */
    private void generateMessageToken() {
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


    public void loginDoctor(View view) {
        startActivity(new Intent(this, DoctorLogin.class));
    }


    public void displayToast(String txt){
        Toast.makeText(this,""+txt,Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        generateMessageToken();
    }


}