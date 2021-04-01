package phamthuc.android.babershop.Retrofit;


import io.reactivex.Observable;
import phamthuc.android.babershop.Model.FCMResponse;
import phamthuc.android.babershop.Model.FCMSendData;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {
    @Headers( {
            "Content-Type:application/json",
            "Authorization:key=AAAAuUTqvSw:APA91bFkntLeJ9EL1uqa5LlVOLoSsbnNmUXSHigEdg4ABZvaZLen4Ciqr4d2NZmkYabV5OuCpbaDsaB1ipW72jShPULO21DONBX107p6XCrods-ZOuFIjNkLgLYInXrVcC5PE0vfVD2n"
    } )

    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
