package phamthuc.android.babershop;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Model.User;

public class MainActivity extends AppCompatActivity {
    private static final int APP_REQUEST_CODE = 1234;

    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    // Add Bindview hier
    @BindView(R.id.btn_login)
    Button btn_login;

    @BindView( R.id.txt_skip)
    TextView txt_skip;

    @OnClick(R.id.btn_login)
    void loginUser(){
        startActivityForResult( AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers).build(), APP_REQUEST_CODE);
    }

    @OnClick(R.id.txt_skip)
    void skipLoginJustGoHome(){
        Intent intent = new Intent( this, HomeActivity.class );
        intent.putExtra( Common.IS_LOGIN, false );
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener( authStateListener );
    }

    @Override
    protected void onStop() {
        if(authStateListener != null)
            firebaseAuth.removeAuthStateListener( authStateListener );
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );

        IdpResponse response = IdpResponse.fromResultIntent( data );
        if(resultCode == RESULT_OK){
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        }else{
            Toast.makeText( this, "Failed to Sign in", Toast.LENGTH_SHORT ).show();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        Dexter.withContext( this )
                .withPermissions(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                ).withListener( new MultiplePermissionsListener(){
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                FirebaseUser user = firebaseAuth.getInstance().getCurrentUser();
                if(user != null){ // If user is already logged

                    // GET TOKEN
                    FirebaseInstanceId.getInstance(  )
                            .getInstanceId()
                            .addOnCompleteListener( task ->{
                                if(task.isSuccessful()){
                                    Common.updateToken(getBaseContext(), task.getResult().getToken());
                                    Log.d("EDMTToken", task.getResult().getToken());
                                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                    intent.putExtra(Common.IS_LOGIN, true);
                                    startActivity( intent );
                                    finish();
                                }
                            } )
                            .addOnFailureListener( e -> {
                                Toast.makeText( MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT ).show();
                                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                intent.putExtra( Common.IS_LOGIN, true );
                                startActivity( intent );
                                finish();
                            } );

                }else{
                    setContentView( R.layout.activity_main );
                    ButterKnife.bind(MainActivity.this);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        } ).check();


        providers = Arrays.asList( new AuthUI.IdpConfig.PhoneBuilder().build() );
        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = firebaseAuth1 ->{
            FirebaseUser user = firebaseAuth1.getCurrentUser();
            if(user != null){
                checkUserFromFirebase(user);
            }
        };
    }

    private void checkUserFromFirebase(FirebaseUser user) {
        FirebaseInstanceId.getInstance(  )
                .getInstanceId()
                .addOnCompleteListener( task ->{
                    if(task.isSuccessful()){
                        Common.updateToken(getBaseContext(), task.getResult().getToken());
                        Log.d("EDMTToken", task.getResult().getToken());

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        intent.putExtra(Common.IS_LOGIN, true);
                        startActivity( intent );
                        finish();
                    }
                } )
                .addOnFailureListener( e -> {
                    Toast.makeText( MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT ).show();
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.putExtra( Common.IS_LOGIN, true );
                    startActivity( intent );
                    finish();
                } );
    }
}
