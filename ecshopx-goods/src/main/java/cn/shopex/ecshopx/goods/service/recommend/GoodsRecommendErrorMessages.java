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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public final class GoodsRecommendErrorMessages {

	private static final Map<String, String> I18N_KEYS =
			Map.ofEntries(
					Map.entry(GoodsRecommendErrorCodes.RULE_NOT_FOUND, "goods.recommend.rule_not_found"),
					Map.entry(GoodsRecommendErrorCodes.RULE_NAME_INVALID, "goods.recommend.rule_name_invalid"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY, "goods.recommend.main_items_empty"),
					Map.entry(GoodsRecommendErrorCodes.RECOMMEND_ITEMS_EMPTY, "goods.recommend.recommend_items_empty"),
					Map.entry(
							GoodsRecommendErrorCodes.MAIN_RECOMMEND_INTERSECT,
							"goods.recommend.main_recommend_intersect"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT, "goods.recommend.main_item_conflict"),
					Map.entry(
							GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY_AFTER_EXCLUDE,
							"goods.recommend.main_items_empty_after_exclude"),
					Map.entry(
							GoodsRecommendErrorCodes.DISPLAY_SETTING_INVALID,
							"goods.recommend.display_setting_invalid"),
					Map.entry(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED, "goods.recommend.distributor_required"),
					Map.entry(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID, "goods.recommend.distributor_invalid"),
					Map.entry(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN, "goods.recommend.checkout_add_forbidden"),
					Map.entry(
							GoodsRecommendErrorCodes.CHECKOUT_CONTEXT_INVALID,
							"goods.recommend.checkout_context_invalid"),
					Map.entry(
							GoodsRecommendErrorCodes.CHECKOUT_ORDER_TYPE_UNSUPPORTED,
							"goods.recommend.checkout_order_type_unsupported"),
					Map.entry(GoodsRecommendErrorCodes.ITEM_IDS_INVALID, "goods.recommend.item_ids_invalid"),
					Map.entry(GoodsRecommendErrorCodes.ITEM_NOT_FOUND, "goods.recommend.item_not_found"));

	private static final Map<String, String> ZH_CN_FALLBACKS =
			Map.ofEntries(
					Map.entry(GoodsRecommendErrorCodes.RULE_NOT_FOUND, "规则不存在"),
					Map.entry(GoodsRecommendErrorCodes.RULE_NAME_INVALID, "规则名称不符合要求"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY, "请至少选择 1 个主商品"),
					Map.entry(GoodsRecommendErrorCodes.RECOMMEND_ITEMS_EMPTY, "请至少选择 1 个推荐商品"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_RECOMMEND_INTERSECT, "主商品与推荐商品不能重复"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_ITEM_CONFLICT, "主商品已存在于其他规则"),
					Map.entry(GoodsRecommendErrorCodes.MAIN_ITEMS_EMPTY_AFTER_EXCLUDE, "请至少保留 1 个主商品"),
					Map.entry(GoodsRecommendErrorCodes.DISPLAY_SETTING_INVALID, "推荐展示设置不符合要求"),
					Map.entry(GoodsRecommendErrorCodes.DISTRIBUTOR_REQUIRED, "缺少店铺信息"),
					Map.entry(GoodsRecommendErrorCodes.DISTRIBUTOR_INVALID, "店铺信息无效"),
					Map.entry(GoodsRecommendErrorCodes.CHECKOUT_ADD_FORBIDDEN, "无法加入结算，请稍后重试"),
					Map.entry(GoodsRecommendErrorCodes.CHECKOUT_CONTEXT_INVALID, "结算信息已失效，请刷新后重试"),
					Map.entry(GoodsRecommendErrorCodes.CHECKOUT_ORDER_TYPE_UNSUPPORTED, "当前订单类型暂不支持加购推荐商品"),
					Map.entry(GoodsRecommendErrorCodes.ITEM_IDS_INVALID, "商品 ID 无效"),
					Map.entry(GoodsRecommendErrorCodes.ITEM_NOT_FOUND, "所选商品不存在或无效"));

	private GoodsRecommendErrorMessages() {}

	public static String message(MessageSource messageSource, String errorCode) {
		return message(messageSource, errorCode, LocaleContextHolder.getLocale());
	}

	public static String message(MessageSource messageSource, String errorCode, Locale locale) {
		if (messageSource == null || errorCode == null) {
			return errorCode;
		}
		String key = I18N_KEYS.get(errorCode);
		if (key == null) {
			return errorCode;
		}
		Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
		String fallback = ZH_CN_FALLBACKS.getOrDefault(errorCode, errorCode);
		return messageSource.getMessage(key, null, fallback, effective);
	}
}
