package phamthuc.android.babershop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import phamthuc.android.babershop.Adapter.MyCartAdapter;
import phamthuc.android.babershop.Common.Common;
import phamthuc.android.babershop.Database.CartDataSource;
import phamthuc.android.babershop.Database.CartDatabase;
import phamthuc.android.babershop.Database.CartItem;
import phamthuc.android.babershop.Database.LocalCartDataSource;

public class CartActivity extends AppCompatActivity {

    MyCartAdapter adapter;
    CompositeDisposable compositeDisposable = new CompositeDisposable(  );
    CartDataSource cartDataSource;

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.btn_clear_cart)
    Button btn_submit_cart;

    @OnClick(R.id.btn_clear_cart)
    void clearCart(){
        AlertDialog.Builder builder = new AlertDialog.Builder( this )
                .setTitle( "Clear Cart" )
                .setMessage( "Do you really want to clear cart ?" )
                .setNegativeButton( "CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                } )
                .setPositiveButton( "CLEAR", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cartDataSource.clearCart( Common.currentUser.getPhoneNumber() )
                                .subscribeOn( Schedulers.io() )
                                .observeOn( AndroidSchedulers.mainThread() )
                                .subscribe( new SingleObserver<Integer>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {

                                    }

                                    @Override
                                    public void onSuccess(Integer integer) {
                                        //Here, after delete all item, we need update total price
                                        cartDataSource.sumPrice( Common.currentUser.getPhoneNumber() )
                                                .subscribeOn( Schedulers.io() )
                                                .observeOn( AndroidSchedulers.mainThread() )
                                                .subscribe( new SingleObserver<Long>() {
                                                    @Override
                                                    public void onSubscribe(Disposable d) {

                                                    }

                                                    @Override
                                                    public void onSuccess(Long aLong) {
                                                        // But we need load all cart again after we clear
                                                        Toast.makeText( CartActivity.this, "Cart has been deleted", Toast.LENGTH_SHORT ).show();
                                                        compositeDisposable.add(
                                                                cartDataSource.getAllItemFromCart( Common.currentUser.getPhoneNumber() )
                                                                        .subscribeOn( Schedulers.io() )
                                                                        .observeOn( AndroidSchedulers.mainThread() )
                                                                        .subscribe( cartItemList -> {
                                                                            //After done, we just sum
                                                                            cartDataSource.sumPrice( Common.currentUser.getPhoneNumber() )
                                                                                    .subscribeOn( Schedulers.io() )
                                                                                    .observeOn( AndroidSchedulers.mainThread() )
                                                                                    .subscribe(updatePrice());

                                                                        }, throwable -> {
                                                                            Toast.makeText( CartActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT ).show();
                                                                        } )
                                                        );
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        Toast.makeText( CartActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                                                    }
                                                } );

                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Toast.makeText( CartActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                                    }
                                } );

                        //Update adapter
                        getAllCart();
                    }
                } );

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private SingleObserver<? super Long> updatePrice() {
        return new SingleObserver<Long>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Long aLong) {
                txt_total_price.setText( new StringBuilder( "€" ).append( aLong ) );
            }

            @Override
            public void onError(Throwable e) {
                if(e.getMessage().contains( "Query returned empty" ))
                    txt_total_price.setText( "" );
                else
                    Toast.makeText( CartActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT ).show();
                //finish();  //Close activity
            }
        };
    }

    CartDatabase cartDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_cart );


        ButterKnife.bind( CartActivity.this );

        cartDataSource = new LocalCartDataSource( CartDatabase.getInstance( this ).cartDAO() );

        //DatabaseUtils.getAllCart(cartDatabase, this);
        getAllCart();

        //View
        recycler_cart.setHasFixedSize( true );
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager( this );
        recycler_cart.setLayoutManager( linearLayoutManager );
        recycler_cart.addItemDecoration( new DividerItemDecoration( this, linearLayoutManager.getOrientation()) );
    }

    private void getAllCart() {
        compositeDisposable.add(cartDataSource.getAllItemFromCart( Common.currentUser.getPhoneNumber() )
                .subscribeOn( Schedulers.io() )
                .observeOn( AndroidSchedulers.mainThread() )
                .subscribe( new Consumer<List<CartItem>>() {
                    @Override
                    public void accept(List<CartItem> cartItems) throws Exception {
                        adapter = new MyCartAdapter( CartActivity.this, cartItems );
                        recycler_cart.setAdapter( adapter );

                        //Update price
                        cartDataSource.sumPrice( Common.currentUser.getPhoneNumber() )
                                .subscribeOn( Schedulers.io() )
                                .observeOn( AndroidSchedulers.mainThread() )
                                .subscribe(updatePrice() );

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText( CartActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT ).show();
                    }
                } ));

    }

    @Override
    protected void onDestroy() {
        if(adapter!= null)
            adapter.onDestroy();
        compositeDisposable.clear();
        super.onDestroy();
    }
}
