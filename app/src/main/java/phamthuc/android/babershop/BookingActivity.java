package phamthuc.android.babershop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.shuhart.stepview.StepView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import phamthuc.android.babershop.Adapter.MyViewPagerAdapter;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Common.NonSwipeViewPager;
import phamthuc.android.babershop.Model.Barber;
import phamthuc.android.babershop.Model.EventBus.BarberDoneEvent;
import phamthuc.android.babershop.Model.EventBus.ConfirmBookingEvent;
import phamthuc.android.babershop.Model.EventBus.DisplayTimeSlotEvent;
import phamthuc.android.babershop.Model.EventBus.EnableNextButton;

public class BookingActivity extends AppCompatActivity {

    AlertDialog dialog;
    CollectionReference barberRef;

    @BindView( R.id.step_view )
    StepView stepView;
    @BindView( R.id.view_pager )
    NonSwipeViewPager viewpager;
    @BindView( R.id.btn_previous_step )
    Button btn_previous_step;
    @BindView( R.id.btn_next_step )
    Button btn_next_step;

    //Event
    @OnClick(R.id.btn_previous_step)
    void previousStep(){
        if(Common.step == 3 || Common.step > 0){
            Common.step--;
            viewpager.setCurrentItem( Common.step );
            if(Common.step < 3){ // Always enable Next button when step < 3
                btn_next_step.setEnabled( true );
                setColorButton();

            }
        }
    }

    @OnClick(R.id.btn_next_step)
    void nextClick(){
        if(Common.step < 3 || Common.step == 0){
            Common.step++;
            if(Common.step == 1){ // After choose salon
                if(Common.currentSalon != null)
                    loadBarberBySalon(Common.currentSalon.getSalonId());
            } else if(Common.step == 2){ //Pick time slot
                if(Common.currentBarber != null){
                    loadTimeslotOfBarber(Common.currentBarber.getBarberId());
                }
            }else if (Common.step == 3){    // Confirm
                if(Common.currentTimeSlot != -1){
                    confirmBooking();
                }
            }
            viewpager.setCurrentItem( Common.step );
        }
    }

    private void confirmBooking() {
        EventBus.getDefault().postSticky( new ConfirmBookingEvent(true) );
    }

    private void loadTimeslotOfBarber(String barberId) {

        EventBus.getDefault().postSticky( new DisplayTimeSlotEvent(true) );
    }

    private void loadBarberBySalon(String salonId) {
        dialog.show();

        //Select all barber of Salon
        //
        // /AllSalon/Haiduong/Branches/AjTxEkjY8Ah0s3OgJEnk/Babers/TM3Ad4MLB2dHsb1FQBWe
        if(!TextUtils.isEmpty( Common.city )){
            barberRef = FirebaseFirestore.getInstance()
                    .collection( "AllSalon" )
                    .document(Common.city)
                    .collection( "Branches" )
                    .document(salonId)
                    .collection( "Babers" );

            barberRef.get()
                    .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            ArrayList<Barber> barbers = new ArrayList<>(  );
                            for(QueryDocumentSnapshot barberSnapShot: task.getResult()){
                                Barber barber = barberSnapShot.toObject( Barber.class );
                                barber.setPassword( "" ); // Remove password because in client app
                                barber.setBarberId( barberSnapShot.getId() );

                                barbers.add( barber );
                            }
                            EventBus.getDefault()
                                    .postSticky( new BarberDoneEvent(barbers) );

                            dialog.dismiss();
                        }
                    } ).addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    dialog.dismiss();
                }
            } );
        }
    }

    //EventBus convert
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void buttonNextReceiver(EnableNextButton event){
        int step = event.getStep();
        if(step == 1)
            Common.currentSalon = event.getSalon();
        else if (step == 2)
            Common.currentBarber = event.getBarber();
        else if (step == 3)
            Common.currentTimeSlot = event.getTimeSlot();

        btn_next_step.setEnabled( true );
        setColorButton();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_booking );
        ButterKnife.bind( BookingActivity.this );

        dialog = new SpotsDialog.Builder().setContext( this ).setCancelable( false).build();

        setupStepView();
        setColorButton();

        //View
        viewpager.setAdapter( new MyViewPagerAdapter(getSupportFragmentManager()) );
        viewpager.setOffscreenPageLimit( 4 ); // We have 4 fragment so we need keep state of this 4 screen page
        viewpager.addOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //Show step
                stepView.go(position, true);
                if(position == 0)
                    btn_previous_step.setEnabled( false );
                else
                    btn_previous_step.setEnabled( true );
                //Set disable button next here
                btn_next_step.setEnabled( false );
                setColorButton();

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        } );
    }

    private void setColorButton() {
        if(btn_next_step.isEnabled()){
            btn_next_step.setBackgroundResource( R.color.colorButton );
        }else{
            btn_next_step.setBackgroundResource( android.R.color.darker_gray );
        }

        if(btn_previous_step.isEnabled()){
            btn_previous_step.setBackgroundResource( R.color.colorButton );
        }else{
            btn_previous_step.setBackgroundResource( android.R.color.darker_gray );
        }
    }

    private void setupStepView(){
        List<String> stepList = new ArrayList<>(  );
        stepList.add( "Salon" );
        stepList.add( "Barber" );
        stepList.add( "Time" );
        stepList.add( "Confirm" );
        stepView.setSteps( stepList );

    }

    //EventBuss

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register( this );
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister( this );
        super.onStop();
    }
}
