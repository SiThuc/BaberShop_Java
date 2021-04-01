package phamthuc.android.babershop.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Interface.IRecyclerItemSelectedListener;
import phamthuc.android.babershop.Model.EventBus.EnableNextButton;
import phamthuc.android.babershop.Model.TimeSlot;
import phamthuc.android.babershop.R;

public class MyTimeSlotAdapter extends RecyclerView.Adapter<MyTimeSlotAdapter.MyViewHolder> {

    Context context;
    List<TimeSlot> timeSlotList;
    List<CardView> cardViewList;

    public MyTimeSlotAdapter(Context context) {
        this.context = context;
        this.timeSlotList = new ArrayList<>(  );
        cardViewList = new ArrayList<>(  );
    }

    public MyTimeSlotAdapter(Context context, List<TimeSlot> timeSlotList) {
        this.context = context;
        this.timeSlotList = timeSlotList;
        cardViewList = new ArrayList<>(  );
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate( R.layout.layout_time_slot, parent, false );
        return new MyViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.txt_time_slot.setText( new StringBuilder( Common.convertTimeSlotToString(position) ).toString() );
        if(timeSlotList.size()==0){ //If all position is avaible, just show list

            //If ll time slot is empty, all card is enable
            holder.card_time_slot.setEnabled( true );

            holder.card_time_slot.setCardBackgroundColor( context.getResources().getColor( android.R.color.white ) );
            holder.txt_time_slot_description.setText( "Available" );
            holder.txt_time_slot_description.setTextColor( context.getResources().getColor( android.R.color.black ) );
            holder.txt_time_slot.setTextColor( context.getResources().getColor( android.R.color.black ) );


        }else{ // If have position is full(booked)
            for(TimeSlot slotValue: timeSlotList){
                //Loop all time slot from server and set diffrent color
                int slot = Integer.parseInt( slotValue.getSlot().toString() );
                if(slot == position){
                    //We will set tag for all time slot is full
                    //So base on tag, we can set all remain card background without change full time slot
                    holder.card_time_slot.setEnabled( false );
                    holder.card_time_slot.setTag( Common.DISABLE_TAG );
                    holder.card_time_slot.setCardBackgroundColor( context.getResources().getColor( android.R.color.darker_gray ) );
                    holder.txt_time_slot_description.setText( "Full" );
                    holder.txt_time_slot_description.setTextColor( context.getResources().getColor( android.R.color.black ) );
                }
            }
        }

        // Add all card to list(20 card because we have 20 time slot)
        // No add card already in cardViewList
        if(!cardViewList.contains( holder.card_time_slot ))
            cardViewList.add( holder.card_time_slot );

        //Check if card time slot is available
        holder.setiRecyclerItemSelectedListener( new IRecyclerItemSelectedListener() {
            @Override
            public void onItemSelectedListener(View view, int pos) {
                //Loop all card in card List
                for(CardView cardView:cardViewList){
                    if(cardView.getTag() == null){  // Only available card time slot be changed
                        cardView.setCardBackgroundColor( context.getResources()
                        .getColor( android.R.color.white ));
                    }
                    // Our selected card will be changed color
                    holder.card_time_slot.setCardBackgroundColor( context.getResources()
                    .getColor( android.R.color.holo_orange_dark ));

                    //EventBus
                    EventBus.getDefault().postSticky( new EnableNextButton( 3, pos ) );
                }
            }
        } );

    }

    @Override
    public int getItemCount() {
        return Common.TIME_SLOT_TOTAL;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txt_time_slot, txt_time_slot_description;
        CardView card_time_slot;

        IRecyclerItemSelectedListener iRecyclerItemSelectedListener;

        public void setiRecyclerItemSelectedListener(IRecyclerItemSelectedListener iRecyclerItemSelectedListener) {
            this.iRecyclerItemSelectedListener = iRecyclerItemSelectedListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super( itemView );
            card_time_slot = (CardView) itemView.findViewById( R.id.card_time_slot );
            txt_time_slot = (TextView)itemView.findViewById( R.id.txt_time_slot );
            txt_time_slot_description = (TextView)itemView.findViewById( R.id.txt_time_slot_description );

            itemView.setOnClickListener( this );

        }

        @Override
        public void onClick(View v) {
            iRecyclerItemSelectedListener.onItemSelectedListener( v, getAdapterPosition() );
        }
    }
}
