package backends.common.messages.MsgData;

import java.util.List;

public class RequestListDataResponse {
    public String type = "REQUEST_LIST_DATA";
    public List<RequestRecordDto> requests;

    public RequestListDataResponse() {}
}
