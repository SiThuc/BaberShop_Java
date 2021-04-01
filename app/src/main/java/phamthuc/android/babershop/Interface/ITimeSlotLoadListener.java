package phamthuc.android.babershop.Interface;

import java.util.List;

import phamthuc.android.babershop.Model.TimeSlot;

public interface ITimeSlotLoadListener {
    void onTimeSlotLoadSuccess(List<TimeSlot> timeSlotList);
    void onTimeSlotLoadFailed(String message);
    void onTimeSlotLoadEmpty();
}
