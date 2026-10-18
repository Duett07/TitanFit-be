package com.app.be.constant;

public final class OrderMessages {

    private OrderMessages() {
    }

    public static final String LOGIN_REQUIRED = "Vui lòng đăng nhập để thực hiện đặt hàng";
    public static final String USER_NOT_FOUND_OR_LOCKED = "Tài khoản người dùng không tồn tại hoặc đã bị khoá/xoá";
    public static final String ORDER_CODE_EMPTY = "Mã đơn hàng không được để trống";
    public static final String PHONE_EMPTY = "Số điện thoại không được để trống";
    public static final String ORDER_NOT_FOUND_PREFIX = "Không tìm thấy đơn hàng với mã: ";
    public static final String ORDER_NOT_FOUND_BY_ID_PREFIX = "Không tìm thấy đơn hàng với ID: ";
    public static final String ORDER_OR_PHONE_MISMATCH = "Không tìm thấy đơn hàng hoặc số điện thoại không trùng khớp";
    public static final String USER_UNAUTHENTICATED = "Người dùng chưa được xác thực";
    public static final String STATUS_REQUIRED = "Trạng thái mới không được để trống";
    public static final String CURRENT_STATUS_INVALID = "Trạng thái hiện tại của đơn hàng không hợp lệ";
    public static final String ALREADY_IN_STATUS = "Đơn hàng đã ở trạng thái %s rồi";
    public static final String CANCELLED_CANNOT_CHANGE = "Đơn hàng đã bị HỦY trước đó, không thể thay đổi trạng thái";
    public static final String COMPLETED_CANNOT_CHANGE = "Đơn hàng đã HOÀN THÀNH, không thể thay đổi trạng thái";
    public static final String INVALID_STATUS_TRANSITION = "Không thể chuyển trạng thái đơn hàng từ %s sang %s. Các trạng thái tiếp theo hợp lệ: %s";
    public static final String PAYMENT_STATUS_REQUIRED = "Trạng thái thanh toán mới không được để trống";
    public static final String ALREADY_IN_PAYMENT_STATUS = "Đơn hàng đã ở trạng thái thanh toán %s rồi";
    public static final String VARIANT_NOT_FOUND_PREFIX = "Không tìm thấy biến thể sản phẩm với ID: ";
    public static final String PRODUCT_UNAVAILABLE = "Sản phẩm '%s' hiện không khả dụng hoặc đã ngừng kinh doanh!";
    public static final String INSUFFICIENT_STOCK = "Sản phẩm '%s' không đủ hàng tồn kho (yêu cầu: %d, còn lại: %d)!";
    public static final String INVALID_PRICE = "Sản phẩm '%s' chưa có giá hợp lệ, không thể đặt hàng!";
}
