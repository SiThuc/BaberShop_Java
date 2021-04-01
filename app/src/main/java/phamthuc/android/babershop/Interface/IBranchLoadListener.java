package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Model.Salon;

public interface IBranchLoadListener {
    void onBranchLoadSuccess(List<Salon> salonList);
    void onBranchLoadFailed(String message);
}
