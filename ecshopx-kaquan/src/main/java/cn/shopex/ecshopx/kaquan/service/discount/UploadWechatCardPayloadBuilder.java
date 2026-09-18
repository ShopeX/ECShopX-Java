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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformCardCreateClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UploadWechatCardPayloadBuilder {

	private static final Logger log = LoggerFactory.getLogger(UploadWechatCardPayloadBuilder.class);
	private static final ObjectMapper OM = new ObjectMapper();

	private final WechatOpenPlatformCardCreateClient wechatOpenPlatformCardCreateClient;

	public UploadWechatCardPayloadBuilder(WechatOpenPlatformCardCreateClient wechatOpenPlatformCardCreateClient) {
		this.wechatOpenPlatformCardCreateClient = wechatOpenPlatformCardCreateClient;
	}

	public String buildAndCreate(String authorizerAppId, DiscountCards cardData, Map<String, ?> shopdata) {
		String rawType = DiscountCardParamNormalize.stringVal(cardData.getCardType());
		String cardType;
		Map<String, Object> especial = new LinkedHashMap<>();
		switch (rawType == null ? "" : rawType) {
			case "discount" -> {
				cardType = "DISCOUNT";
				especial.put("discount", intOrZero(cardData.getDiscount()));
			}
			case "cash" -> {
				cardType = "CASH";
				especial.put("least_cost", intOrZero(cardData.getLeastCost()));
				especial.put("reduce_cost", intOrZero(cardData.getReduceCost()));
			}
			case "groupon" -> {
				cardType = "GROUPON";
				especial.put("deal_detail", nullToEmpty(DiscountCardParamNormalize.stringVal(cardData.getDealDetail())));
			}
			case "general_coupon" -> {
				cardType = "GENERAL_COUPON";
				especial.put("default_detail", nullToEmpty(DiscountCardParamNormalize.stringVal(cardData.getDefaultDetail())));
			}
			case "gift" -> {
				cardType = "GIFT";
				especial.put("gift", nullToEmpty(DiscountCardParamNormalize.stringVal(cardData.getGift())));
			}
			default -> {
				log.warn("upload wechat card: unsupported card_type {}", rawType);
				return null;
			}
		}

		Map<String, Object> baseInfo = buildBaseInfo(cardData, shopdata);
		Map<String, Object> advancedInfo = buildAdvancedInfo(cardData, cardType);

		return wechatOpenPlatformCardCreateClient.createCard(authorizerAppId, cardType, baseInfo, advancedInfo, especial);
	}

	private Map<String, Object> buildBaseInfo(DiscountCards cardData, Map<String, ?> shopdata) {
		Map<String, Object> base = new LinkedHashMap<>();
		base.put("logo_url", shopString(shopdata, "logo"));
		base.put("brand_name", shopString(shopdata, "brand_name"));
		String title = DiscountCardParamNormalize.stringVal(cardData.getTitle());
		base.put("title", title);
		base.put("description", title);
		base.put("code_type", "CODE_TYPE_QRCODE");
		int qty = intOrZero(cardData.getQuantity());
		base.put("sku", Map.of("quantity", qty));
		base.put("color", "Color060");

		applyUseScenesNotice(cardData, base);

		String dateType = DiscountCardParamNormalize.stringVal(cardData.getDateType());
		if ("DATE_TYPE_FIX_TERM".equals(dateType)) {
			Map<String, Object> dateInfo = new LinkedHashMap<>();
			dateInfo.put("type", "DATE_TYPE_FIX_TERM");
			dateInfo.put("fixed_term", intOrZero(cardData.getFixedTerm()));
			dateInfo.put("fixed_begin_term", intOrZero(cardData.getBeginDate()));
			if (cardData.getEndDate() != null) {
				dateInfo.put("end_timestamp", cardData.getEndDate());
			} else {
				dateInfo.put("end_timestamp", "");
			}
			base.put("date_info", dateInfo);
		} else if ("DATE_TYPE_FIX_TIME_RANGE".equals(dateType)) {
			Map<String, Object> dateInfo = new LinkedHashMap<>();
			dateInfo.put("type", "DATE_TYPE_FIX_TIME_RANGE");
			dateInfo.put("begin_timestamp", intOrZero(cardData.getBeginDate()));
			dateInfo.put("end_timestamp", intOrZero(cardData.getEndDate()));
			base.put("date_info", dateInfo);
		} else if ("DATE_TYPE_PERMANENT".equals(dateType)) {
			base.put("date_info", Map.of("type", "DATE_TYPE_PERMANENT"));
		}

		base.put("service_phone", DiscountCardParamNormalize.stringVal(cardData.getServicePhone()));

		boolean useAll = DiscountCardParamNormalize.isTruthyString(cardData.getUseAllShops());
		base.put("use_all_locations", useAll);
		if (!useAll) {
			String rel = DiscountCardParamNormalize.stringVal(cardData.getRelShopsIds());
			if (StringUtils.hasText(rel)) {
				List<String> locIds = new ArrayList<>();
				for (String p : rel.split(",")) {
					String t = p.trim();
					if (StringUtils.hasText(t)) {
						locIds.add(t);
					}
				}
				if (!locIds.isEmpty()) {
					base.put("location_id_list", locIds);
				}
			}
		}

		base.put("get_limit", intOrZero(cardData.getGetLimit()));
		base.put("use_limit", intOrZero(cardData.getUseLimit()));
		return base;
	}

	private static void applyUseScenesNotice(DiscountCards cardData, Map<String, Object> base) {
		String scenes = DiscountCardParamNormalize.stringVal(cardData.getUseScenes());
		if (!StringUtils.hasText(scenes)) {
			return;
		}
		String notice = switch (scenes) {
			case "ONLINE" -> "请在下单时选择优惠券";
			case "QUICK" -> "请在买单时选择优惠券";
			case "SWEEP" -> "到店请出示二维码";
			case "SELF" -> "到店请出示该卡券";
			default -> "请在规定时间内使用";
		};
		base.put("notice", notice);
	}

	private Map<String, Object> buildAdvancedInfo(DiscountCards cardData, String wxCardType) {
		Map<String, Object> advanced = new LinkedHashMap<>();
		advanced.put("text_image_list", parseTextImageList(cardData.getTextImageList()));

		Map<String, Object> useCondition = new LinkedHashMap<>();
		useCondition.put("can_use_with_other_discount", true);
		if ("CASH".equals(wxCardType)) {
			useCondition.put("least_cost", intOrZero(cardData.getLeastCost()));
		}
		advanced.put("use_condition", useCondition);

		List<Map<String, String>> timeLimit = new ArrayList<>();
		timeLimit.add(Map.of("type", "MONDAY"));
		timeLimit.add(Map.of("type", "TUESDAY"));
		timeLimit.add(Map.of("type", "WEDNESDAY"));
		timeLimit.add(Map.of("type", "THURSDAY"));
		timeLimit.add(Map.of("type", "FRIDAY"));
		timeLimit.add(Map.of("type", "SUNDAY"));
		timeLimit.add(Map.of("type", "SATURDAY"));
		advanced.put("time_limit", timeLimit);
		return advanced;
	}

	private List<Map<String, Object>> parseTextImageList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (!t.startsWith("[")) {
			return List.of();
		}
		try {
			return OM.readValue(t, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			log.debug("text_image_list parse skip: {}", e.getMessage());
			return List.of();
		}
	}

	private static String shopString(Map<String, ?> shopdata, String key) {
		if (shopdata == null) {
			return null;
		}
		return DiscountCardParamNormalize.stringVal(shopdata.get(key));
	}

	private static int intOrZero(Integer v) {
		return v == null ? 0 : v;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
