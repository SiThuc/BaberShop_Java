package phamthuc.android.babershop.Fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Common.MyToken;
import phamthuc.android.babershop.Database.CartDataSource;
import phamthuc.android.babershop.Database.CartDatabase;
import phamthuc.android.babershop.Database.CartItem;
import phamthuc.android.babershop.Database.LocalCartDataSource;
import phamthuc.android.babershop.Model.BookingInformation;
import phamthuc.android.babershop.Model.EventBus.ConfirmBookingEvent;
import phamthuc.android.babershop.Model.FCMResponse;
import phamthuc.android.babershop.Model.FCMSendData;
import phamthuc.android.babershop.Model.MyNotification;
import phamthuc.android.babershop.R;
import phamthuc.android.babershop.Retrofit.IFCMApi;
import phamthuc.android.babershop.Retrofit.RetrofitClient;

public class BookingStep4Fragment extends Fragment{

    CartDataSource cartDataSource;

    CompositeDisposable compositeDisposable = new CompositeDisposable(  );

    SimpleDateFormat simpleDateFormat;
    Unbinder unbinder;

    IFCMApi ifcmApi;

    AlertDialog dialog;

    @BindView( R.id.txt_booking_barber_text )
    TextView txt_booking_barber_text;
    @BindView( R.id.txt_booking_time_text )
    TextView txt_booking_time_text;
    @BindView( R.id.txt_salon_address )
    TextView txt_salon_address;
    @BindView( R.id.txt_salon_name )
    TextView txt_salon_name;
    @BindView( R.id.txt_open_time )
    TextView txt_open_time;
    @BindView( R.id.txt_phone_number )
    TextView txt_salon_phone;
    @BindView( R.id.txt_salon_website )
    TextView txt_salon_website;

    @OnClick( R.id.btn_confirm )
    void confirmBooking(){
        dialog.show();

        //DatabaseUtils.getAllCart( CartDatabase.getInstance( getContext() ), this );
        compositeDisposable.add( cartDataSource.getAllItemFromCart( Common.currentUser.getPhoneNumber() )
        .subscribeOn( Schedulers.io() )
        .observeOn( AndroidSchedulers.mainThread() )
        .subscribe( cartItemList -> {
            //Move to here
            // Here, we will have all item on Cart
            //Process Timestamp
            // We will use Timestamp to filter all booking with date is greater today
            // For only display all future booking
            String startTime = Common.convertTimeSlotToString( Common.currentTimeSlot );
            String[] convertTime = startTime.split( "-" );
            //Get start time: get 9:00
            String[] startTimeConvert = convertTime[0].split( ":" );
            int startHourInt = Integer.parseInt( startTimeConvert[0].trim() ); // Gotten 9
            int startMinInt = Integer.parseInt( startTimeConvert[1].trim() ); // Gotten 0

            Calendar bookingDateWithOurHour = Calendar.getInstance();
            bookingDateWithOurHour.setTimeInMillis( Common.bookingDate.getTimeInMillis() );
            bookingDateWithOurHour.set(Calendar.HOUR_OF_DAY, startHourInt);
            bookingDateWithOurHour.set(Calendar.MINUTE, startMinInt);

            // Create timestamp object and apply to BookingInformation
            Timestamp timestamp = new Timestamp( bookingDateWithOurHour.getTime() );

            //Create booking information
            BookingInformation bookingInformation = new BookingInformation(  );

            bookingInformation.setCityBook( Common.city );
            bookingInformation.setTimestamp( timestamp );
            bookingInformation.setDone( false );
            bookingInformation.setBarberId( Common.currentBarber.getBarberId() );
            bookingInformation.setBarberName( Common.currentBarber.getName() );
            bookingInformation.setCustomerName( Common.currentUser.getName() );
            bookingInformation.setCustomerPhone( Common.currentUser.getPhoneNumber() );
            bookingInformation.setSalonId( Common.currentSalon.getSalonId() );
            bookingInformation.setSalonAddress( Common.currentSalon.getAddress() );
            bookingInformation.setSalonName( Common.currentSalon.getName() );
            bookingInformation.setTime( new StringBuilder( Common.convertTimeSlotToString( Common.currentTimeSlot ) )
                    .append(" at ")
                    .append(simpleDateFormat.format( bookingDateWithOurHour.getTime() )).toString());
            bookingInformation.setSlot( Long.valueOf( Common.currentTimeSlot ) );
            bookingInformation.setCartItemList( cartItemList );

            //Submit to Barber document
            DocumentReference bookingDate = FirebaseFirestore.getInstance()
                    .collection( "AllSalon" )
                    .document( Common.city )
                    .collection("Branches")
                    .document( Common.currentSalon.getSalonId() )
                    .collection( "Babers" )
                    .document(Common.currentBarber.getBarberId())
                    .collection( Common.simpleDateFormat.format( Common.bookingDate.getTime() ) )
                    .document(String.valueOf( Common.currentTimeSlot ));

            // Write data
            bookingDate.set( bookingInformation )
                    .addOnSuccessListener( new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Here we can write an function to check,
                            // If already exist an booking, we wil prevent new booking

                            //After add success booking information, just clear car
                            //DatabaseUtils.clearCart( CartDatabase.getInstance( getContext() ) );
                            cartDataSource.clearCart( Common.currentUser.getPhoneNumber() )
                                    .subscribeOn( Schedulers.io() )
                                    .observeOn( AndroidSchedulers.mainThread() )
                                    .subscribe( new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            addToUserBooking(bookingInformation);
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText( getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                                        }
                                    } );

                        }
                    } ).addOnFailureListener( new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText( getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                }
            } );

        }, throwable -> {
            Toast.makeText( getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT ).show();
        } ));

    }

    private void addToUserBooking(BookingInformation bookingInformation) {
        // First, create new Collection
        final CollectionReference userBooking = FirebaseFirestore.getInstance()
                .collection( "User" )
                .document(Common.currentUser.getPhoneNumber())
                .collection( "Booking" );

        //Check if exist document in this collection
        //Get current date
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.DATE, 0 );
        calendar.add( Calendar.HOUR_OF_DAY, 0 );
        calendar.add( Calendar.MINUTE, 0 );

        Timestamp toDayTimestamp = new Timestamp( calendar.getTime() );

        userBooking
                .whereGreaterThanOrEqualTo( "timestamp", toDayTimestamp )
                .whereEqualTo( "done", false )  // If have any document with field done == false
                .limit(1)
                .get()
                .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.getResult().isEmpty()){
                            //Set data
                            userBooking.document()
                                    .set( bookingInformation )
                                    .addOnSuccessListener( new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //Creat notification
                                            MyNotification myNotification = new MyNotification();
                                            myNotification.setUid( UUID.randomUUID().toString() );
                                            myNotification.setTitle( "New Booking" );
                                            myNotification.setContent( "You have a new appointment for customer hair care with "+ Common.currentUser.getName() );
                                            myNotification.setRead( false ); // We will only filter norification with read is false on Barber Staff app
                                            myNotification.setServerTimestamp( FieldValue.serverTimestamp() );

                                            //Submit Notification to "Notification" collection of Barber
                                            FirebaseFirestore.getInstance()
                                                    .collection( "AllSalon" )
                                                    .document(Common.city)
                                                    .collection( "Branches" )
                                                    .document(Common.currentSalon.getSalonId())
                                                    .collection( "Babers" )
                                                    .document(Common.currentBarber.getBarberId())
                                                    .collection( "Notification" ) //If if not available, it will be created automatically
                                                    .document(myNotification.getUid())
                                                    .set( myNotification )
                                                    .addOnSuccessListener( new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            // First, get Token base on Barber id
                                                            FirebaseFirestore.getInstance()
                                                                    .collection( "Tokens" )
                                                                    .whereEqualTo( "userPhone", Common.currentBarber.getUsername() )
                                                                    .limit( 1 )
                                                                    .get()
                                                                    .addOnCompleteListener( new OnCompleteListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                            if(task.isSuccessful() && task.getResult().size() >0){
                                                                                MyToken myToken = new MyToken();
                                                                                for(DocumentSnapshot tokenSnapShot: task.getResult())
                                                                                    myToken = tokenSnapShot.toObject( MyToken.class );


                                                                                // Create data to send
                                                                                FCMSendData sendRequest = new FCMSendData();
                                                                                Map<String, String> dataSend = new HashMap<>(  );
                                                                                dataSend.put( Common.TITLE_KEY, "New Booking" );
                                                                                dataSend.put( Common.CONTENT_KEY, "You have new booking from user"+Common.currentUser.getName() );

                                                                                sendRequest.setTo( myToken.getToken() );
                                                                                sendRequest.setData( dataSend );

                                                                                compositeDisposable.add(
                                                                                        ifcmApi.sendNotification( sendRequest )
                                                                                                .subscribeOn( Schedulers.io() )
                                                                                                .observeOn( AndroidSchedulers.mainThread() )
                                                                                                .subscribe( new Consumer<FCMResponse>() {
                                                                                                    @Override
                                                                                                    public void accept(FCMResponse fcmResponse) throws Exception {
                                                                                                        if(dialog.isShowing())
                                                                                                            dialog.dismiss();

                                                                                                        addToCalendar(Common.bookingDate, Common.convertTimeSlotToString( Common.currentTimeSlot ));
                                                                                                        resetStaticData();
                                                                                                        getActivity().finish();
                                                                                                        Toast.makeText( getContext(), "Success!", Toast.LENGTH_SHORT ).show();

                                                                                                    }
                                                                                                }, new Consumer<Throwable>() {
                                                                                                    @Override
                                                                                                    public void accept(Throwable throwable) throws Exception {
                                                                                                        if(dialog.isShowing())
                                                                                                            dialog.dismiss();

                                                                                                        Log.d("NOTIFICATION_ERROR", throwable.getMessage());
                                                                                                        addToCalendar(Common.bookingDate, Common.convertTimeSlotToString( Common.currentTimeSlot ));
                                                                                                        resetStaticData();
                                                                                                        getActivity().finish();
                                                                                                        Toast.makeText( getContext(), "Success!", Toast.LENGTH_SHORT ).show();
                                                                                                    }
                                                                                                } ) );
                                                                            }
                                                                        }
                                                                    } );
                                                        }
//
                                                } );



//                                            if(dialog.isShowing())
//                                                dialog.dismiss();
//
//                                            addToCalendar(Common.bookingDate, Common.convertTimeSlotToString( Common.currentTimeSlot ));
//
//                                            resetStaticData();
//                                            getActivity().finish();
//                                            Toast.makeText( getContext(), "Success", Toast.LENGTH_SHORT ).show();
                                        }
                                    } )
                                    .addOnFailureListener( new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if(dialog.isShowing())
                                                dialog.dismiss();
                                            Toast.makeText( getContext(), e.getMessage(), Toast.LENGTH_SHORT ).show();
                                        }
                                    } );
                        }else{
                            if(dialog.isShowing())
                                dialog.dismiss();

                            resetStaticData();
                            getActivity().finish();
                            Toast.makeText( getContext(), "Success", Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );



    }

    private void addToCalendar(Calendar bookingDate, String convertTimeSlotToString) {
        String startTime = Common.convertTimeSlotToString( Common.currentTimeSlot );
        String[] convertTime = startTime.split( "-" );
        //Get start time: get 9:00
        String[] startTimeConvert = convertTime[0].split( ":" );
        int startHourInt = Integer.parseInt( startTimeConvert[0].trim() ); // Gotten 9
        int startMinInt = Integer.parseInt( startTimeConvert[1].trim() ); // Gotten 0

        String[] endTimeConvert = convertTime[0].split( ":" );
        int endHourInt = Integer.parseInt( endTimeConvert[0].trim() ); // Gotten 9
        int endMinInt = Integer.parseInt( endTimeConvert[1].trim() ); // Gotten 30

        Calendar startEvent = Calendar.getInstance();
        startEvent.setTimeInMillis( bookingDate.getTimeInMillis() );
        startEvent.set(Calendar.HOUR_OF_DAY, startHourInt); // Set event start hour
        startEvent.set(Calendar.MINUTE, startMinInt);      // Set event start minute

        Calendar endEvent = Calendar.getInstance();
        endEvent.setTimeInMillis( bookingDate.getTimeInMillis() );
        endEvent.set(Calendar.HOUR_OF_DAY, endHourInt);     // Set event end hour
        endEvent.set(Calendar.MINUTE, endMinInt);      // Set event start minute

        //After we have startEvent and endEvent, convert it to format String
        SimpleDateFormat calendarDateFormat = new SimpleDateFormat( "dd-MM-yyyy:mm" );
        String startEventTime = calendarDateFormat.format( startEvent.getTime() );
        String endEventTime = calendarDateFormat.format( endEvent.getTime() );

        addToDeviceCalendar(startEventTime, endEventTime, "Haircut Booking",
                new StringBuilder( "Haircut from" )
        .append( startTime )
        .append("with")
        .append(Common.currentBarber.getName())
        .append( "at" )
        .append( Common.currentSalon.getName() ).toString(),
                new StringBuilder( "Address:" ).append( Common.currentSalon.getAddress() ).toString());

    }

    private void addToDeviceCalendar(String startEventTime, String endEventTime, String title, String description, String location) {
        SimpleDateFormat calendarDateFormat = new SimpleDateFormat( "dd-MM-yyyy HH:mm" );

        try {
            Date start = calendarDateFormat.parse( startEventTime );
            Date end = calendarDateFormat.parse( endEventTime );
            ContentValues event = new ContentValues(  );

            //Put
            event.put( CalendarContract.Events.CALENDAR_ID, getCalendar(getContext()) );
            event.put( CalendarContract.Events.TITLE, title );
            event.put( CalendarContract.Events.DESCRIPTION, description );
            event.put( CalendarContract.Events.EVENT_LOCATION, location );

            //Time
            event.put( CalendarContract.Events.DTSTART, start.getTime() );
            event.put( CalendarContract.Events.DTEND, end.getTime() );
            event.put( CalendarContract.Events.ALL_DAY, 0 );
            event.put( CalendarContract.Events.HAS_ALARM, 1 );

            String timeZone = TimeZone.getDefault().getID();
            event.put( CalendarContract.Events.EVENT_TIMEZONE, timeZone );

            Uri calendars;
            if(Build.VERSION.SDK_INT >= 8)
                calendars = Uri.parse("content://com.android.calendar/calendars");
            else
                calendars = Uri.parse( "content://calendar/events" );

            Uri uri_save = getActivity().getContentResolver().insert( calendars, event );
            // Save to Cache
            Paper.init(getActivity());
            Paper.book().write( Common.EVENT_URI_CACHE, uri_save.toString() );

            getActivity().getContentResolver().insert( calendars, event );


        }catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String getCalendar(Context context) {
        //Get default calendar ID of Calendar of Gmail
        String gmailIdCalendar = "";
        String projection[] = {"_id", "calendar_displayName"};
        Uri calendars = Uri.parse("content://com.android.calendar/calendars");

        ContentResolver contentResolver = context.getContentResolver();
        //Select all calendar
        Cursor managedCursor = contentResolver.query( calendars, projection, null, null, null );
        if(managedCursor.moveToFirst()){
            String calName;
            int nameCol = managedCursor.getColumnIndex( projection[1] );
            int idCol = managedCursor.getColumnIndex( projection[0] );
            do{
                calName = managedCursor.getString( nameCol );
                if(calName.contains( "@gmail.com" )){
                    gmailIdCalendar = managedCursor.getString( idCol );
                    break; // Exit as soon as have id
                }
            }while (managedCursor.moveToNext());
            managedCursor.close();
        }

        return gmailIdCalendar;
    }

    private void resetStaticData() {
        Common.step = 0;
        Common.currentTimeSlot = -1;
        Common.currentSalon = null;
        Common.currentBarber = null;
        Common.bookingDate.add( Calendar.DATE, 0); // Current date added
    }

    //=======================================================================
    //EventBus
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
    public void setDataBooking(ConfirmBookingEvent event){
        if(event.isConfirm()){
            setData();
        }
    }
    //=======================================================================

    private void setData() {
        txt_booking_barber_text.setText( Common.currentBarber.getName() );
        txt_booking_time_text.setText( new StringBuilder( Common.convertTimeSlotToString( Common.currentTimeSlot ) )
        .append(" at ")
        .append(simpleDateFormat.format( Common.bookingDate.getTime() )));

        txt_salon_address.setText( Common.currentSalon.getAddress() );
        txt_salon_website.setText( Common.currentSalon.getWebsite() );
        txt_salon_name.setText( Common.currentSalon.getName() );
        txt_open_time.setText( Common.currentSalon.getOpenTime() );
    }


    static BookingStep4Fragment instance;
    public static BookingStep4Fragment getInstance(){
        if(instance == null)
            instance = new BookingStep4Fragment();
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        ifcmApi = RetrofitClient.getInstance().create( IFCMApi.class );

        //Apply format for date display on confirm
        simpleDateFormat = new SimpleDateFormat( "dd/MM/yyyy" );
        dialog = new SpotsDialog.Builder().setContext( getContext() ).setCancelable( false ).build();
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView( inflater, container, savedInstanceState );
        View itemView = inflater.inflate( R.layout.fragment_booking_step_four, container, false);
        unbinder = ButterKnife.bind( this, itemView );

        cartDataSource = new LocalCartDataSource( CartDatabase.getInstance( getContext() ).cartDAO() );

        return itemView;
    }

}
