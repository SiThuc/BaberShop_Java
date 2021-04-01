package phamthuc.android.babershop.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import phamthuc.android.babershop.Adapter.MyBarberAdapter;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Common.SpacesItemDecoration;
import phamthuc.android.babershop.Model.Barber;
import phamthuc.android.babershop.Model.EventBus.BarberDoneEvent;
import phamthuc.android.babershop.R;

public class BookingStep2Fragment extends Fragment {

    Unbinder unbinder;

    @BindView( R.id.recycler_barber )
    RecyclerView recycler_barber;

    //EVENTBUS START
    //==============================================================
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
    public void setBarberAdapter(BarberDoneEvent event){
        MyBarberAdapter adapter = new MyBarberAdapter( getContext(), event.getBarberList() );
        recycler_barber.setAdapter( adapter );
    }

    //===================================================================


    static BookingStep2Fragment instance;


    public static BookingStep2Fragment getInstance(){
        if(instance == null)
            instance = new BookingStep2Fragment();
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView( inflater, container, savedInstanceState );

        View itemView = inflater.inflate( R.layout.fragment_booking_step_two, container, false);
        unbinder = ButterKnife.bind( this, itemView );
        
        initView();
        
        return itemView;
    }

    private void initView() {
        recycler_barber.setHasFixedSize( true );
        recycler_barber.setLayoutManager( new GridLayoutManager( getActivity(), 2 ) );
        recycler_barber.addItemDecoration( new SpacesItemDecoration( 4 ) );
    }
}
