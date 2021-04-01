package phamthuc.android.babershop.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.HorizontalCalendarView;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import dmax.dialog.SpotsDialog;
import phamthuc.android.babershop.Adapter.MyBarberAdapter;
import phamthuc.android.babershop.Adapter.MyTimeSlotAdapter;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Common.SpacesItemDecoration;
import phamthuc.android.babershop.Interface.ITimeSlotLoadListener;
import phamthuc.android.babershop.Model.EventBus.BarberDoneEvent;
import phamthuc.android.babershop.Model.EventBus.DisplayTimeSlotEvent;
import phamthuc.android.babershop.Model.TimeSlot;
import phamthuc.android.babershop.R;

public class BookingStep3Fragment extends Fragment implements ITimeSlotLoadListener {

    //Variable
    DocumentReference barberDoc;
    ITimeSlotLoadListener iTimeSlotLoadListener;
    AlertDialog dialog;

    Unbinder unbinder;

    @BindView( R.id.recycler_time_slot )
    RecyclerView recycler_time_slot;
    @BindView( R.id.calendarView )
    HorizontalCalendarView calendarView;
    SimpleDateFormat simpleDateFormat;

    //===================================================================================
    // EventBus
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register( this );

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister( this );
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void loadAllTimeSlotAvaiable(DisplayTimeSlotEvent event){
        if(event.isDisplay()){
            Calendar date = Calendar.getInstance();
            date.add( Calendar.DATE, 0 ); // Add current dat
            loadAvailableTimeSlotOfBarber( Common.currentBarber.getBarberId(),
                    simpleDateFormat.format( date.getTime() ));
        }
    }

    //===================================================================================

    private void loadAvailableTimeSlotOfBarber(String barberId, String bookDate) {
        dialog.show();

        // /AllSalon/Haiduong/Branches/AjTxEkjY8Ah0s3OgJEnk/Babers/CRsAdamgYBx9ztLNaoZE
        barberDoc = FirebaseFirestore.getInstance()
                .collection( "AllSalon" )
                .document(Common.city)
                .collection( "Branches" )
                .document(Common.currentSalon.getSalonId())
                .collection( "Babers" )
                .document(Common.currentBarber.getBarberId());

        // Get information of this barber
        barberDoc.get().addOnCompleteListener( new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if(documentSnapshot.exists()){ // If barber available
                        //Get information of booking
                        // If not created, return empty
                        CollectionReference date = FirebaseFirestore.getInstance()
                                .collection( "AllSalon" )
                                .document( Common.city )
                                .collection("Branches")
                                .document( Common.currentSalon.getSalonId() )
                                .collection( "Babers" )
                                .document(Common.currentBarber.getBarberId())
                                .collection( bookDate );

                        date.get().addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()){
                                    QuerySnapshot querySnapshot = task.getResult();
                                    if(querySnapshot.isEmpty()){ // If dont have any appointment
                                        iTimeSlotLoadListener.onTimeSlotLoadEmpty();
                                    }else{
                                        // If have appointments
                                        List<TimeSlot> timeSlots = new ArrayList<>(  );
                                        for(QueryDocumentSnapshot document: task.getResult())
                                            timeSlots.add( document.toObject(TimeSlot.class ) );
                                        iTimeSlotLoadListener.onTimeSlotLoadSuccess( timeSlots );
                                    }
                                }
                            }
                        } ).addOnFailureListener( new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                iTimeSlotLoadListener.onTimeSlotLoadFailed( e.getMessage() );
                            }
                        } );

                    }
                }
            }
        } );
    }

    static BookingStep3Fragment instance;
    public static BookingStep3Fragment getInstance(){
        if(instance == null)
            instance = new BookingStep3Fragment();
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        iTimeSlotLoadListener = this;
        simpleDateFormat = new SimpleDateFormat( "dd_MM_yyyy" );

        dialog = new SpotsDialog.Builder().setContext( getContext() ).setCancelable( false ).build();

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView( inflater, container, savedInstanceState );

        View itemView = inflater.inflate( R.layout.fragment_booking_step_three, container, false);
        unbinder = ButterKnife.bind( this, itemView );

        init(itemView);
        return itemView;
    }

    private void init(View itemView) {
        recycler_time_slot.setHasFixedSize( true );
        recycler_time_slot.addItemDecoration( new SpacesItemDecoration( 8 ) );

        //Calendar
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DATE, 0);
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DATE, 2);  // 2 Day left

        HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder( itemView, R.id.calendarView )
                .range( startDate, endDate )
                .datesNumberOnScreen( 1 )
                .mode( HorizontalCalendar.Mode.DAYS )
                .defaultSelectedDate( startDate )
                .build();
        horizontalCalendar.setCalendarListener( new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar date, int position) {

                if(Common.bookingDate.getTimeInMillis() != date.getTimeInMillis()){
                    Common.bookingDate = date; // This code will not load again if you select new day same with day selected
                    loadAvailableTimeSlotOfBarber(Common.currentBarber.getBarberId(),
                            simpleDateFormat.format( date.getTime() ));
                }
            }
        } );
    }

    @Override
    public void onTimeSlotLoadSuccess(List<TimeSlot> timeSlotList) {
        MyTimeSlotAdapter adapter = new MyTimeSlotAdapter( getContext() , timeSlotList);
        recycler_time_slot.setAdapter( adapter );
        GridLayoutManager gridLayoutManager = new GridLayoutManager( getActivity(), 3 );
        recycler_time_slot.setLayoutManager( gridLayoutManager );
        dialog.dismiss();

    }

    @Override
    public void onTimeSlotLoadFailed(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();

    }

    @Override
    public void onTimeSlotLoadEmpty() {
        MyTimeSlotAdapter adapter = new MyTimeSlotAdapter( getContext() );
        recycler_time_slot.setAdapter( adapter );
        GridLayoutManager gridLayoutManager = new GridLayoutManager( getActivity(), 3 );
        recycler_time_slot.setLayoutManager( gridLayoutManager );
        dialog.dismiss();

    }
}
