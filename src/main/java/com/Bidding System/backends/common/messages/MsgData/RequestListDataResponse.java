package backends.common.messages.MsgData;
import backends.server.database.RequestLog;
import java.util.List;

public class RequestListDataResponse {
    public String type = "REQUEST_LIST_DATA";
    public List<RequestLog.RequestRecord> requests;

    public RequestListDataResponse() {}
}
