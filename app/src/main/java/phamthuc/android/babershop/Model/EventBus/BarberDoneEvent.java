package phamthuc.android.babershop.Model.EventBus;


import java.util.List;

import phamthuc.android.babershop.Model.Barber;

public class BarberDoneEvent {
    private List<Barber> barberList;

    public BarberDoneEvent(List<Barber> barberList) {
        this.barberList = barberList;
    }

    public List<Barber> getBarberList() {
        return barberList;
    }

    public void setBarberList(List<Barber> barberList) {
        this.barberList = barberList;
    }
}
