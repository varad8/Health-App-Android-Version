package com.vrnitsolution.healthappnew.DoctorUI;

import static com.vrnitsolution.healthappnew.DoctorUI.AESCrypt.encrypt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vrnitsolution.healthappnew.R;

public class DoctorLogin extends AppCompatActivity implements LocationListener {
    EditText doctorEmail, doctorPassword;
    String dcEmail, dcPassword;
    ImageView togglebutton;
    private boolean toggle = false;
    FirebaseAuth mauth;
    FirebaseUser firebaseUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference doctorsCollection = db.collection("doctors");

    ImageView backBtnImageview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        mauth = FirebaseAuth.getInstance();
        firebaseUser = mauth.getCurrentUser();
        ;


//        Window window = this.getWindow();
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        doctorEmail = findViewById(R.id.doctorEmail);
        doctorPassword = findViewById(R.id.doctorPassword);
        togglebutton = findViewById(R.id.togglepassword);

        //password toggle
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

        backBtnImageview = findViewById(R.id.backBtnImageview);
        backBtnImageview.setOnClickListener(view -> finish());


    }

    /**
     * In this method check the data is not empty
     */
    public void loginDoctor(View view) {
        dcEmail = doctorEmail.getText().toString().trim();
        dcPassword = doctorPassword.getText().toString().trim();

        if (dcEmail.isEmpty() && dcPassword.isEmpty()) {
            Toast.makeText(this, "please enter credentials", Toast.LENGTH_SHORT).show();
        } else {
            doctorAuthCheck(dcEmail, dcPassword);
        }
    }

    /**
     * Using this method check that Email and password is present in Firestore collection Doctors
     */
    private void doctorAuthCheck(String dcEmail, String dcPassword) {
        doctorsCollection.whereEqualTo("email", dcEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doctorDocument = queryDocumentSnapshots.getDocuments().get(0);

                        String specialistIn = doctorDocument.getString("specialistIn");
                        String photoUrl = doctorDocument.getString("photoUrl");
                        String occupation = doctorDocument.getString("occupation");


                        if (specialistIn.isEmpty() && photoUrl.isEmpty() && occupation.isEmpty()) {
                            doctorAuthLogin(true, dcEmail, dcPassword);
                            displayToast("empty");
                        } else {
                            doctorAuthLogin(false, dcEmail, dcPassword);
                            displayToast("not empty");
                        }
                    } else {
                        displayToast("Unauthorized account");
                    }
                })
                .addOnFailureListener(e -> {
                    displayToast("Error checking authentication: " + e.getMessage());
                });
    }

    private void doctorAuthLogin(boolean isEmpty, String dcEmail, String dcPassword) {

        mauth.signInWithEmailAndPassword(dcEmail, dcPassword).addOnSuccessListener(authResult -> {
            if (isEmpty) {
                startActivity(new Intent(DoctorLogin.this, DoctorFormPage.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            } if (!isEmpty){
                startActivity(new Intent(DoctorLogin.this, DoctorDashboard.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        }).addOnFailureListener(e -> {
            displayToast("" + e.getMessage());
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    public void registerDoctor(View view) {
        startActivity(new Intent(DoctorLogin.this, RegisterActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    public void displayToast(String txt) {
        Toast.makeText(this, "" + txt, Toast.LENGTH_LONG).show();
    }
}