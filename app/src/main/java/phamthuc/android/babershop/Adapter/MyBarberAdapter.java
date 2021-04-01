package phamthuc.android.babershop.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Interface.IRecyclerItemSelectedListener;
import phamthuc.android.babershop.Model.Barber;
import phamthuc.android.babershop.Model.EventBus.EnableNextButton;
import phamthuc.android.babershop.R;

public class MyBarberAdapter extends RecyclerView.Adapter<MyBarberAdapter.MyViewHolder> {

    Context context;
    List<Barber> barberList;
    List<CardView> cardViewList;

    public MyBarberAdapter(Context context, List<Barber> barberList) {
        this.context = context;
        this.barberList = barberList;
        cardViewList = new ArrayList<>(  );
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txt_barber_name;
        RatingBar ratingBar;
        CardView card_barber;

        IRecyclerItemSelectedListener iRecyclerItemSelectedListener;

        public void setiRecyclerItemSelectedListener(IRecyclerItemSelectedListener iRecyclerItemSelectedListener) {
            this.iRecyclerItemSelectedListener = iRecyclerItemSelectedListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super( itemView );

            card_barber = (CardView)itemView.findViewById( R.id.card_barber );
            txt_barber_name = (TextView)itemView.findViewById( R.id.txt_barber_name );
            ratingBar = (RatingBar)itemView.findViewById( R.id.rtb_barber );

            itemView.setOnClickListener( this );
        }

        @Override
        public void onClick(View v) {
            iRecyclerItemSelectedListener.onItemSelectedListener( v, getAdapterPosition() );
        }
    }


    @NonNull
    @Override
    public MyBarberAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from( context )
                .inflate( R.layout.layout_barber, parent, false );
        return new MyViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull MyBarberAdapter.MyViewHolder holder, int position) {
        holder.txt_barber_name.setText( barberList.get( position ).getName() );
        if(barberList.get( position ).getRatingTimes() != null)
            holder.ratingBar.setRating(barberList.get( position ).getRating().floatValue() / barberList.get( position ).getRatingTimes());
        else
            holder.ratingBar.setRating( 0 );
        if(!cardViewList.contains( holder.card_barber ))
            cardViewList.add( holder.card_barber );

        holder.setiRecyclerItemSelectedListener( new IRecyclerItemSelectedListener() {
            @Override
            public void onItemSelectedListener(View view, int pos) {
                // Set background for all item not choice
                for(CardView cardView: cardViewList){
                    cardView.setCardBackgroundColor( context.getResources()
                    .getColor( android.R.color.white ));
                }

                // Set background for choice
                holder.card_barber.setCardBackgroundColor(
                        context.getResources()
                        .getColor( android.R.color.holo_orange_dark )
                );

                //EventBus
                EventBus.getDefault().postSticky( new EnableNextButton( 2, barberList.get( position ) ) );
            }
        } );

    }

    @Override
    public int getItemCount() {
        return barberList.size();
    }


}
