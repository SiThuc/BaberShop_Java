package phamthuc.android.babershop.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Database.CartDataSource;
import phamthuc.android.babershop.Database.CartDatabase;
import phamthuc.android.babershop.Database.CartItem;
import phamthuc.android.babershop.Database.LocalCartDataSource;
import phamthuc.android.babershop.Interface.IRecyclerItemSelectedListener;
import phamthuc.android.babershop.Model.ShoppingItem;
import phamthuc.android.babershop.R;

public class MyShoppingItemAdapter extends RecyclerView.Adapter<MyShoppingItemAdapter.MyViewHolder> {
    Context context;
    List<ShoppingItem> shoppingItemList;
    CartDataSource cartDataSource;
    CompositeDisposable compositeDisposable;

    public void onDestroy(){
        compositeDisposable.clear();
    }

    public MyShoppingItemAdapter(Context context, List<ShoppingItem> shoppingItemList) {
        this.context = context;
        this.shoppingItemList = shoppingItemList;
        cartDataSource = new LocalCartDataSource( CartDatabase.getInstance( context ).cartDAO() );
        compositeDisposable = new CompositeDisposable(  );
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from( context ).inflate( R.layout.layout_shopping_item, parent, false );
        return new MyViewHolder( itemView );
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Picasso.get().load( shoppingItemList.get( position ).getImage() ).into( holder.img_shopping_item );
        holder.txt_shopping_item_name.setText( Common.formatShoppingItemName(shoppingItemList.get( position ).getName()) );
        holder.txt_shopping_item_price.setText( new StringBuilder( "???" ).append( shoppingItemList.get( position ).getPrice()) );

        // Add to Cart
        holder.setiRecyclerItemSelectedListener( new IRecyclerItemSelectedListener() {
            @Override
            public void onItemSelectedListener(View view, int pos) {
                // Create cart Item
                CartItem cartItem = new CartItem();
                cartItem.setProductId( shoppingItemList.get( pos ).getId() );
                cartItem.setProductName( shoppingItemList.get( pos ).getName() );
                cartItem.setProductImage( shoppingItemList.get( pos ).getImage() );
                cartItem.setProductQuantity( (long) 1 );
                cartItem.setProductPrice( shoppingItemList.get( pos ).getPrice() );
                cartItem.setUserPhone( Common.currentUser.getPhoneNumber() );

                //Insert to dbDatabaseUtils.insertToCart( cartDatabase, cartItem );
                compositeDisposable.add( cartDataSource.insert( cartItem )
                .subscribeOn( Schedulers.io() )
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe(
                        ()-> Toast.makeText( context, "Added to Cart!", Toast.LENGTH_SHORT ).show(),
                        throwable -> Toast.makeText( context, ""+throwable.getMessage(), Toast.LENGTH_SHORT ).show() )
                );

            }
        } );
    }

    @Override
    public int getItemCount() {
        return shoppingItemList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txt_shopping_item_name, txt_shopping_item_price, txt_add_to_cart;
        ImageView img_shopping_item;

        IRecyclerItemSelectedListener iRecyclerItemSelectedListener;

//        public IRecyclerItemSelectedListener getiRecyclerItemSelectedListener() {
//            return iRecyclerItemSelectedListener;
//        }

        public void setiRecyclerItemSelectedListener(IRecyclerItemSelectedListener iRecyclerItemSelectedListener) {
            this.iRecyclerItemSelectedListener = iRecyclerItemSelectedListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super( itemView );
            img_shopping_item = (ImageView)itemView.findViewById( R.id.img_shopping_item );
            txt_shopping_item_name = (TextView) itemView.findViewById( R.id.txt_name_shopping_item );
            txt_shopping_item_price = (TextView) itemView.findViewById( R.id.txt_price_shopping_item);
            txt_add_to_cart = (TextView) itemView.findViewById( R.id.txt_ad_to_cart );

            txt_add_to_cart.setOnClickListener( this );
        }

        @Override
        public void onClick(View v) {
            iRecyclerItemSelectedListener.onItemSelectedListener( v, getAdapterPosition() );
        }
    }
}
