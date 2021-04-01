package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Database.CartItem;

public interface ICartItemLoadListener {
    void onGetAllItemFromCartSuccess(List<CartItem> cartItemList);
}
