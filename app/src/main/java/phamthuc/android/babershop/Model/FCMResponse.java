package phamthuc.android.babershop.Model;

import java.util.List;

public class FCMResponse {
    private long multicast_id;
    private int success;
    private int failure;
    private int canonical_ids;
    private List<Result> results;

    public FCMResponse() {
    }

    public long getMulticast_id() {
        return multicast_id;
    }

    public void setMulticast_id(long multicast_id) {
        this.multicast_id = multicast_id;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailure() {
        return failure;
    }

    public void setFailure(int failure) {
        this.failure = failure;
    }

    public int getCanonical_ids() {
        return canonical_ids;
    }

    public void setCanonical_ids(int canonical_ids) {
        this.canonical_ids = canonical_ids;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}

class Result {
    private String message_i;

    public Result() {
    }

    public String getMessage_i() {
        return message_i;
    }

    public void setMessage_i(String message_i) {
        this.message_i = message_i;
    }
}
