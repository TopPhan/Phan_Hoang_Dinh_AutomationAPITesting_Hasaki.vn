package com.pojoModel;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * POJO Model cho Update Product In Cart API.
 *
 * Endpoint: POST /mobile/v2/checkout/cart/update-product
 * Request body (flat - khác với AddToCart):
 * {
 *   "product_id": 99791,
 *   "quantity_selected": 2
 * }
 */
@Data
@Builder
public class UpdateCartModel {

    @SerializedName("product_id")
    private int productId;

    @SerializedName("quantity_selected")
    private int quantitySelected;
}
