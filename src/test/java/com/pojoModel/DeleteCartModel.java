package com.pojoModel;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * POJO Model cho Delete Item In Cart API.
 *
 * Endpoint: POST /mobile/v2/checkout/cart/delete-product
 * Request body (flat - giống UpdateCartModel nhưng tách riêng để rõ intent):
 * {
 *   "product_id": 183184,
 *   "quantity_selected": 2
 * }
 *
 * Lưu ý: cấu trúc body giống UpdateCartModel, nhưng tách riêng class
 * để phân biệt rõ intent (xóa khác với cập nhật).
 */
@Data
@Builder
public class DeleteCartModel {

    @SerializedName("product_id")
    private int productId;

    @SerializedName("quantity_selected")
    private int quantitySelected;
}
