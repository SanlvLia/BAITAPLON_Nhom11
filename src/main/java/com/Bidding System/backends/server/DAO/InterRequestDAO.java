package DAO;

import models.core.Item;

import java.io.IOException;
import java.util.List;

public interface InterRequestDAO {

    // chưa cho save request vào đây vì hiên tại phương thức hiện tại của các database có tham số khác nhau
    default Object findById(String id) throws IOException {
        throw new UnsupportedOperationException("findById is not supported");
    }

    default List<?> getRequestsByType(String requestType) throws IOException {
        throw new UnsupportedOperationException("getRequestsByType is not supported");
    }

    default List<?> getRequestsByStatus(String status) throws IOException {
        throw new UnsupportedOperationException("getItemsByStatus is not supported");
    }

    default Item getRequesttoAuction(String status) throws IOException {
        throw new UnsupportedOperationException("getItemtoAuction is not supported");
    }

    default List<?> getRequestsByUserId(String userId) throws IOException {
        throw new UnsupportedOperationException("getItemsByUserId is not supported");
    }

    String getStatusById(String id);

    default void updateRequestStatus(String requestId, String status) throws IOException {
        throw new UnsupportedOperationException("updateRequestStatus is not supported");
    }

    default void updateRequestsStatus(List<String> itemIds, String status) throws IOException {
        throw new UnsupportedOperationException("updateItemStatus(List<String>, String) is not supported");
    }

    default void deleteRequest(String request_id) throws IOException {
        throw new UnsupportedOperationException("deleteRequests(List<String>) is not supported");
    }

    default void deleteRequests(List<Integer> requestIds) throws IOException {
        throw new UnsupportedOperationException("deleteMyRequests(List<Integer>) is not supported");
    }

    default void removeByRequestId(String requestId) throws IOException {};

    public  boolean existsByRequestId(String requestId) throws IOException ;

}
