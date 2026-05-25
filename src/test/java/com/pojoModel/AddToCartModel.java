package com.pojoModel;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * POJO Model cho Add To Cart API.
 *
 * Endpoint: POST /mobile/v1/checkout/cart/add-to-cart
 * Request body:
 * {
 *   "product": {
 *     "id": 6710,
 *     "quantity_selected": 1,
 *     "gift_group_id": ""
 *   }
 * }
 */
@Data
@Builder
public class AddToCartModel {

    private ProductItem product;

    @Data
    @Builder
    public static class ProductItem {
        private int id;

        @SerializedName("quantity_selected")
        private int quantitySelected;

        @SerializedName("gift_group_id")
        @Builder.Default
        private String giftGroupId = "";
    }
}
