package phamthuc.android.babershop.Fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.nex3z.notificationbadge.NotificationBadge;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import phamthuc.android.babershop.Adapter.HomeSliderAdapter;
import phamthuc.android.babershop.Adapter.LookbookAdapter;
import phamthuc.android.babershop.BookingActivity;
import phamthuc.android.babershop.CartActivity;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Database.CartDataSource;
import phamthuc.android.babershop.Database.CartDatabase;
import phamthuc.android.babershop.Database.LocalCartDataSource;
import phamthuc.android.babershop.HistoryActivity;
import phamthuc.android.babershop.Interface.IBannerLoadListener;
import phamthuc.android.babershop.Interface.IBookingInfoLoadListener;
import phamthuc.android.babershop.Interface.IBookingInformationChangeListener;
import phamthuc.android.babershop.Interface.ICountItemInCartListener;
import phamthuc.android.babershop.Interface.ILookBookLoadListener;
import phamthuc.android.babershop.MainActivity;
import phamthuc.android.babershop.Model.Banner;
import phamthuc.android.babershop.Model.BookingInformation;
import phamthuc.android.babershop.R;
import phamthuc.android.babershop.Service.PicassoImageLoadingService;
import ss.com.bannerslider.Slider;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements IBannerLoadListener, ILookBookLoadListener, IBookingInfoLoadListener, IBookingInformationChangeListener{
    private Unbinder unbinder;

    CartDataSource cartDataSource;

    @BindView(R.id.notification_badge)
    NotificationBadge notificationBadge;

    @BindView( R.id.layout_user_information )
    LinearLayout layout_user_information;

    @OnClick(R.id.layout_user_information)
    void onLogOutDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder( getContext() );
        builder.setTitle( "Sign out?" )
                .setMessage( "Please confirm you really want to sign out" )
                .setNegativeButton( "CANCEL", ((dialogInterface, i) ->{
                    dialogInterface.dismiss();
                } ) )
                .setPositiveButton( "OK", ((dialogInterface, i) -> {

                    Common.currentBarber = null;
                    Common.currentBooking = null;
                    Common.currentSalon= null;
                    Common.currentTimeSlot= -1;
                    Common.currentBookingId="";
                    Common.currentUser=null;
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent( getContext(), MainActivity.class );
                    intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
                    startActivity( intent );
                    getActivity().finish();
                }) );

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @BindView( R.id.txt_user_name )
    TextView txt_user_name;

    @BindView( R.id.banner_slider )
    Slider banner_slider;

    @BindView( R.id.recycler_look_book )
    RecyclerView recycler_look_book;

    @BindView( R.id.card_booking_info )
    CardView card_booking_info;
    @BindView( R.id.txt_salon_address )
    TextView txt_salon_address;
    @BindView( R.id.txt_salon_barber )
    TextView txt_salon_barber;
    @BindView( R.id.txt_time )
    TextView txt_time;
    @BindView( R.id.txt_time_remain )
    TextView txt_time_remain;

    @OnClick( R.id.btn_delete_booking )
    void deleteBooking(){
        deleteBookingFromBarber(false);
    }

    @OnClick(R.id.btn_change_booking)
    void changeBooking(){
        changeBookingFromUser();
    }

    private void changeBookingFromUser() {
        //Show dialog confirm
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder( getActivity() )
                .setCancelable( false )
                .setTitle( "Hey!" )
                .setMessage( "You you really want change booking information?\n Because we will delete your old Booking" )
                .setNegativeButton( "CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                } ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        deleteBookingFromBarber( true );
                    }
                } );
        confirmDialog.show();
    }

    android.app.AlertDialog dialog;

    private void deleteBookingFromBarber(boolean isChange) {

        // We have to delete from Baber's collections first and then from User's collections
        // ---> delete event
        // We need Load Common.curentBooking because we need some date from BookingInformation
        if(Common.currentBooking != null){
            dialog.show();
            //Get current information in barber object
            DocumentReference barberBookingInfo = FirebaseFirestore.getInstance()
                    .collection( "AllSalon" )
                    .document(Common.currentBooking.getCityBook())
                    .collection( "Branches" )
                    .document(Common.currentBooking.getSalonId())
                    .collection( "Babers" )
                    .document(Common.currentBooking.getBarberId())
                    .collection( Common.convertTimeStampToStringKey(Common.currentBooking.getTimestamp()))
                    .document(Common.currentBooking.getSlot().toString());

            // When we have document, just delete it
            barberBookingInfo.delete().addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText( getContext(), e.getMessage(), Toast.LENGTH_SHORT ).show();
                }
            } ).addOnSuccessListener( new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // After delete on Barber done, we will start delete from User
                    deleteBookingFromUser(isChange);
                }
            } );

        }else{
            Toast.makeText( getContext(), "Current Booking must not be null", Toast.LENGTH_SHORT ).show();
        }
    }

    private void deleteBookingFromUser(boolean isChange) {
        //First, we need get information from user object
        if(!TextUtils.isEmpty( Common.currentBookingId )){
            DocumentReference userBookingInfo = FirebaseFirestore.getInstance()
                    .collection( "User" )
                    .document(Common.currentUser.getPhoneNumber())
                    .collection( "Booking" )
                    .document(Common.currentBookingId);

            //Delete
            userBookingInfo.delete().addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText( getContext(), e.getMessage(), Toast.LENGTH_SHORT ).show();
                }
            } ).addOnSuccessListener( new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                //First, we need get save Uri of event we just add
                    Paper.init( getActivity() );
                    if(Paper.book().read( Common.EVENT_URI_CACHE ) != null) {
                        String eventString = Paper.book().read( Common.EVENT_URI_CACHE ).toString();
                        Uri eventUri = null;
                        if (eventString != null && !TextUtils.isEmpty( eventString ))
                            eventUri = Uri.parse( eventString );
                        if (eventUri != null)
                            getActivity().getContentResolver().delete( eventUri, null, null );
                    }

                    //Uri eventUri = Uri.parse( Paper.book().read( Common.EVENT_URI_CACHE ).toString() );
                    //getActivity().getContentResolver().delete( eventUri, null, null);

                    Toast.makeText( getActivity(), "Success delete booking!", Toast.LENGTH_SHORT ).show();
                    //Refresh interface
                    loadUserBooking();

                    // Check if Change --> Call from change button, we will fired interface
                    if(isChange)
                        iBookingInformationChangeListener.onBookingInformationChange();

                    dialog.dismiss();
                }
            } );
        }else{
            dialog.dismiss();

            Toast.makeText( getContext(), "Booking information must not be empty", Toast.LENGTH_SHORT ).show();
        }
    }


    @OnClick(R.id.card_view_booking)
    void booking(){
        startActivity( new Intent( getActivity(), BookingActivity.class ));
    }

    @OnClick(R.id.card_view_cart)
    void openCartActivity(){
        startActivity( new Intent( getActivity(), CartActivity.class) );
    }

    @OnClick(R.id.card_view_history)
    void openHistoryActivity(){
        startActivity( new Intent( getActivity(), HistoryActivity.class) );
    }




    //FireStore
    CollectionReference bannerRef, lookbookRef;

    //Interface
    IBannerLoadListener iBannerLoadListener;
    ILookBookLoadListener iLookBookLoadListener;
    IBookingInfoLoadListener iBookingInfoLoadListener;
    IBookingInformationChangeListener iBookingInformationChangeListener;


    ListenerRegistration userBookingListener = null;
    EventListener<QuerySnapshot> userBookingEvent = null;

    FirebaseUser user;


    public HomeFragment() {
        bannerRef = FirebaseFirestore.getInstance().collection( "Banner" );
        lookbookRef = FirebaseFirestore.getInstance().collection( "Lookbook" );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserBooking();
        countCartItem(user);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        dialog = new SpotsDialog.Builder().setContext( getContext() ).setCancelable( false ).build();
    }

    private void loadUserBooking() {
        if (Common.currentUser != null){
            CollectionReference userBooking = FirebaseFirestore.getInstance()
                    .collection( "User" )
                    .document( Common.currentUser.getPhoneNumber() )
                    .collection( "Booking" );

            //Get current date
            Calendar calendar = Calendar.getInstance();
            calendar.add( Calendar.DATE, 0 );
            calendar.add( Calendar.HOUR_OF_DAY, 0 );
            calendar.add( Calendar.MINUTE, 0 );

            Timestamp toDayTimestamp = new Timestamp( calendar.getTime() );

            //Select booking information from Firebase with done=false and timestamp greater today
            userBooking
                    .whereGreaterThanOrEqualTo( "timestamp", toDayTimestamp )
                    .whereEqualTo( "done", false )
                    .limit( 1 )
                    .get()
                    .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                        BookingInformation bookingInformation = queryDocumentSnapshot.toObject( BookingInformation.class );
                                        iBookingInfoLoadListener.onBookingInfoLoadSuccess( bookingInformation, queryDocumentSnapshot.getId() );
                                        break; // Exit loop as soon as
                                    }
                                } else {
                                    iBookingInfoLoadListener.onBookingInfoLoadEmpty();
                                }
                            }

                        }
                    } ).addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    iBannerLoadListener.onBannerLoadFailed( e.getMessage() );
                }
            } );

            //Here, after userBooking has been assigned data(collections)
            // We will make realtime listen here
            if(userBookingEvent != null){ // If userBookingEvent already init
                if(userBookingListener == null){ // Only add if userBookingListener == null
                    // That mean we just add 1 time
                    userBookingListener = userBooking.addSnapshotListener( userBookingEvent );
                }
            }

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate( R.layout.fragment_home, container, false );
        unbinder = ButterKnife.bind( this, view );

        cartDataSource = new LocalCartDataSource( CartDatabase.getInstance( getContext() ).cartDAO() );

        //Init
        Slider.init(new PicassoImageLoadingService() );
        iBannerLoadListener = this;
        iLookBookLoadListener = this;
        iBookingInfoLoadListener = this;
        iBookingInformationChangeListener = this;


        // Check is logged?
        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            setUserInformation();
            loadBanner();
            loadLookBook();
            initRealtimeUserBooking(); // need declare above loadUserBooking
            loadUserBooking();
            countCartItem(user);

        }
        return view;
    }

    private void initRealtimeUserBooking() {
        if(userBookingEvent == null){
            userBookingEvent = new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    //In this event, when it fired, we will call loadUserBooking again
                    // to reload all booking information
                    loadUserBooking();
                }
            };
        }
    }

    private void countCartItem(FirebaseUser user) {
        //cartDataSource.countItemInCart( Common.currentUser.getPhoneNumber() )
        cartDataSource.countItemInCart( user.getPhoneNumber() )
                .subscribeOn( Schedulers.io() )
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        notificationBadge.setText( String.valueOf( integer ) );
                    }

                    @Override
                    public void onError(Throwable e) {
                        //Toast.makeText( getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                    }
                } );



    }

    private void setUserInformation(){
        layout_user_information.setVisibility( View.VISIBLE );
        if(Common.currentUser != null)
            txt_user_name.setText( Common.currentUser.getName() + " " + Common.currentUser.getPhoneNumber() );

    }

    private void loadBanner(){
        bannerRef.get()
                .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        List<Banner> banners = new ArrayList<>(  );
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot bannerSnapShot:task.getResult()){
                                Banner banner = bannerSnapShot.toObject( Banner.class );
                                banners.add( banner );
                            }
                            iBannerLoadListener.onBannerLoadSuccess( banners );
                        }

                    }
                } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                iBannerLoadListener.onBannerLoadFailed( e.getMessage() );

            }
        } );
    }
    private void loadLookBook(){
        lookbookRef.get()
                .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        List<Banner> lookbooks = new ArrayList<>(  );
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot bannerSnapShot:task.getResult()){
                                Banner lookbook = bannerSnapShot.toObject( Banner.class );
                                lookbooks.add( lookbook );
                            }
                            iLookBookLoadListener.onLookbookLoadSuccess( lookbooks );
                        }

                    }
                } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                iLookBookLoadListener.onLookbookLoadFailed( e.getMessage() );
            }
        } );

    }

    @Override
    public void onBannerLoadSuccess(List<Banner> banners) {
        banner_slider.setAdapter( new HomeSliderAdapter(banners) );
    }

    @Override
    public void onBannerLoadFailed(String message) {
        Toast.makeText(getActivity(), ""+message, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onLookbookLoadSuccess(List<Banner> banners) {
        recycler_look_book.setHasFixedSize( true );
        recycler_look_book.setLayoutManager( new LinearLayoutManager( getActivity() ) );
        recycler_look_book.setAdapter( new LookbookAdapter(getActivity(), banners) );
    }

    @Override
    public void onLookbookLoadFailed(String message) {
        Toast.makeText( getActivity(), message, Toast.LENGTH_SHORT ).show();

    }

    @Override
    public void onBookingInfoLoadEmpty() {
        card_booking_info.setVisibility( View.GONE );
    }

    @Override
    public void onBookingInfoLoadSuccess(BookingInformation bookingInformation, String bookingId) {

        //Save currentBooking
        Common.currentBooking = bookingInformation;
        Common.currentBookingId = bookingId;

        txt_salon_address.setText( bookingInformation.getSalonAddress() );
        txt_salon_barber.setText( bookingInformation.getBarberName() );
        txt_time.setText( bookingInformation.getTime());
        String timeRemain = DateUtils.getRelativeTimeSpanString(
                Long.valueOf( bookingInformation.getTimestamp().toDate().getTime() ),
                Calendar.getInstance().getTimeInMillis(), 0 ).toString();
        txt_time_remain.setText( timeRemain );


        card_booking_info.setVisibility( View.VISIBLE );

        dialog.dismiss();

    }

    @Override
    public void onBookingInfoLoadFailed(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBookingInformationChange() {
        startActivity( new Intent(getActivity(), BookingActivity.class) );
    }



    @Override
    public void onDestroy() {
        if(userBookingListener != null)
            userBookingListener.remove();
        super.onDestroy();
    }
}
