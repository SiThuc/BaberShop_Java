package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Model.Banner;

public interface IBannerLoadListener {
    void onBannerLoadSuccess(List<Banner> banners);
    void onBannerLoadFailed(String message);
}
