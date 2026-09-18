/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.goods.service.recommend;

/** 商品推荐稳定错误码（SSOT §7.3） */
public final class GoodsRecommendErrorCodes {

	public static final String RULE_NOT_FOUND = "RECOMMEND_RULE_NOT_FOUND";

	public static final String RULE_NAME_INVALID = "RECOMMEND_RULE_NAME_INVALID";

	public static final String MAIN_ITEMS_EMPTY = "RECOMMEND_MAIN_ITEMS_EMPTY";

	public static final String RECOMMEND_ITEMS_EMPTY = "RECOMMEND_ITEMS_EMPTY";

	public static final String MAIN_RECOMMEND_INTERSECT = "RECOMMEND_MAIN_RECOMMEND_INTERSECT";

	public static final String MAIN_ITEM_CONFLICT = "RECOMMEND_MAIN_ITEM_CONFLICT";

	public static final String MAIN_ITEMS_EMPTY_AFTER_EXCLUDE = "RECOMMEND_MAIN_ITEMS_EMPTY_AFTER_EXCLUDE";

	public static final String DISPLAY_SETTING_INVALID = "RECOMMEND_DISPLAY_SETTING_INVALID";

	public static final String DISTRIBUTOR_REQUIRED = "RECOMMEND_DISTRIBUTOR_REQUIRED";

	public static final String DISTRIBUTOR_INVALID = "RECOMMEND_DISTRIBUTOR_INVALID";

	public static final String CHECKOUT_ADD_FORBIDDEN = "RECOMMEND_CHECKOUT_ADD_FORBIDDEN";

	public static final String CHECKOUT_CONTEXT_INVALID = "RECOMMEND_CHECKOUT_CONTEXT_INVALID";

	public static final String CHECKOUT_ORDER_TYPE_UNSUPPORTED = "RECOMMEND_CHECKOUT_ORDER_TYPE_UNSUPPORTED";

	public static final String ITEM_IDS_INVALID = "RECOMMEND_ITEM_IDS_INVALID";

	public static final String ITEM_NOT_FOUND = "RECOMMEND_ITEM_NOT_FOUND";

	private GoodsRecommendErrorCodes() {}
}
