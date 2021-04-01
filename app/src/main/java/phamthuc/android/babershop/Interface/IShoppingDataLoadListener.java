package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Model.ShoppingItem;

public interface IShoppingDataLoadListener {
    void onShoppingDataLoadSuccess(List<ShoppingItem> shoppingItemList);
    void onShoppingDataLoadFailed(String message);
}
