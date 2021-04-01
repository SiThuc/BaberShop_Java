package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Model.Banner;

public interface ILookBookLoadListener {
    void onLookbookLoadSuccess(List<Banner> banners);
    void onLookbookLoadFailed(String message);
}
