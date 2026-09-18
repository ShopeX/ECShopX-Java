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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayWxBusinessCategory;
import cn.shopex.ecshopx.adapay.mapper.AdapayWxBusinessCategoryMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayMerchantResidentCreateService {

	private final AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper;
	private final AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final ObjectMapper objectMapper;
	private final String adapayNotifyUrl;

	public AdapayMerchantResidentCreateService(
			AdapayWxBusinessCategoryMapper adapayWxBusinessCategoryMapper,
			AdapayMerchantEntryInfoLoader adapayMerchantEntryInfoLoader,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			ObjectMapper objectMapper,
			@Value("${adapay.notify-url:}") String adapayNotifyUrl) {
		this.adapayWxBusinessCategoryMapper = adapayWxBusinessCategoryMapper;
		this.adapayMerchantEntryInfoLoader = adapayMerchantEntryInfoLoader;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.objectMapper = objectMapper;
		this.adapayNotifyUrl = adapayNotifyUrl == null ? "" : adapayNotifyUrl;
	}

	public void apply(long companyId, Map<String, Object> body) {
		String feeType = requireBodyText(body, "fee_type", "费率类型必填");
		String wxCategoryName = requireBodyText(body, "wx_category", "微信经营类目必填");
		String modelType = requireBodyText(body, "model_type", "入驻模式必填");
		String merType = requireBodyText(body, "mer_type", "商户种类必填");
		String provinceCode = requireBodyText(body, "province_code", "省份必填");
		String cityCode = requireBodyText(body, "city_code", "城市必填");
		String districtCode = requireBodyText(body, "district_code", "区县必填");
		String adapayFeeMode = requireBodyText(body, "adapay_fee_mode", "手续费扣除方式必填");

		Map<String, Object> addValueMap = parseAndNormalizeAddValueList(body.get("add_value_list"));

		AdapayWxBusinessCategory wxRow =
				adapayWxBusinessCategoryMapper.selectOne(
						new LambdaQueryWrapper<AdapayWxBusinessCategory>()
								.eq(AdapayWxBusinessCategory::getFeeType, feeType)
								.eq(AdapayWxBusinessCategory::getMerchantTypeName, wxCategoryName)
								.last("LIMIT 1"));
		if (wxRow == null || !StringUtils.hasText(wxRow.getBusinessCategoryId())) {
			throw new ResourceException("微信经营类目不存在");
		}
		String wxCategoryResolved = wxRow.getBusinessCategoryId();

		AdapayMerchantEntryInfoContext context =
				adapayMerchantEntryInfoLoader
						.load(companyId)
						.orElseThrow(() -> new ResourceException("请先完成开户进件"));

		String appIdListRaw = context.entry().getAppIdList();
		if (!StringUtils.hasText(appIdListRaw)) {
			throw new ResourceException("应用ID列表不能为空");
		}
		JsonNode appIdArray;
		try {
			appIdArray = objectMapper.readTree(appIdListRaw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("app_id_list JSON格式错误");
		}
		if (!appIdArray.isArray() || appIdArray.isEmpty()) {
			throw new ResourceException("应用ID列表不能为空");
		}
		JsonNode first = appIdArray.get(0);
		if (first == null || !first.isObject()) {
			throw new ResourceException("应用ID列表数据不完整");
		}
		JsonNode appIdNode = first.get("app_id");
		String firstAppId =
				appIdNode == null || appIdNode.isNull() ? "" : appIdNode.asText();
		if (!StringUtils.hasText(firstAppId)) {
			throw new ResourceException("应用ID列表数据不完整");
		}

		String liveApiKey =
				context.entry().getLiveApiKey() == null ? "" : context.entry().getLiveApiKey();

		long epochSeconds = Instant.now().getEpochSecond();
		String requestId = "merchant_resident_" + companyId + "_" + epochSeconds;

		String addValueJson;
		try {
			addValueJson = objectMapper.writeValueAsString(addValueMap);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("支付渠道配置无法序列化");
		}

		LinkedHashMap<String, Object> callParams = new LinkedHashMap<>();
		callParams.put("request_id", requestId);
		callParams.put("company_id", companyId);
		callParams.put("notify_url", adapayNotifyUrl);
		callParams.put("api_method", "MerchantConf.create");
		callParams.put("sub_api_key", liveApiKey);
		callParams.put("app_id", firstAppId);
		callParams.put("fee_type", feeType);
		callParams.put("wx_category", wxCategoryResolved);
		callParams.put("model_type", modelType);
		callParams.put("mer_type", merType);
		callParams.put("province_code", provinceCode);
		callParams.put("city_code", cityCode);
		callParams.put("district_code", districtCode);
		callParams.put("adapay_fee_mode", adapayFeeMode);
		callParams.put("add_value_list", addValueJson);

		unreachablePostOutboundNotes(callParams);

		adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		throw new ResourceException("暂不支持开户流程");
	}

	private static void unreachablePostOutboundNotes(@SuppressWarnings("unused") Map<String, Object> callParams) {
		/*
		 * Outbound success path is disabled: failure handling, merchant_resident persistence,
		 * and operation logging are not executed until MerchantConf.create is wired end-to-end.
		 */
	}

	private Map<String, Object> parseAndNormalizeAddValueList(Object raw) {
		if (raw == null) {
			throw new BadRequestException("支付渠道配置信息必填");
		}
		if (raw instanceof String str && !StringUtils.hasText(str)) {
			throw new BadRequestException("支付渠道配置信息必填");
		}
		Map<String, Object> map;
		if (raw instanceof List<?> list) {
			if (!list.isEmpty()) {
				throw new BadRequestException("未支持支付渠道");
			}
			map = new LinkedHashMap<>();
		} else if (raw instanceof Map<?, ?> rawMap) {
			map = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				if (e.getKey() != null) {
					map.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
		} else if (raw instanceof String str) {
			try {
				JsonNode node = objectMapper.readTree(str);
				if (node.isObject()) {
					map =
							objectMapper.convertValue(
									node, new TypeReference<LinkedHashMap<String, Object>>() {});
				} else if (node.isArray()) {
					if (node.size() > 0) {
						throw new BadRequestException("未支持支付渠道");
					}
					map = new LinkedHashMap<>();
				} else {
					throw new BadRequestException("支付渠道配置JSON格式错误");
				}
			} catch (BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("支付渠道配置JSON格式错误");
			}
		} else {
			throw new BadRequestException("支付渠道配置信息格式错误");
		}

		List<Map.Entry<String, Object>> snapshot = new ArrayList<>(map.entrySet());
		for (Map.Entry<String, Object> e : snapshot) {
			String payChannel = e.getKey();
			Object setting = e.getValue();
			switch (payChannel) {
				case "wx_lite" -> {
					Map<String, Object> set = asStringKeyMap(setting);
					String appid = mapText(set, "appid");
					if (!StringUtils.hasText(appid)) {
						throw new BadRequestException("小程序appi必填");
					}
					Map<String, Object> wxPub = new LinkedHashMap<>();
					wxPub.put("appid", appid);
					map.put("wx_pub", wxPub);
				}
				case "wx_pub" -> {
					Map<String, Object> set = asStringKeyMap(setting);
					String appid = mapText(set, "appid");
					if (!StringUtils.hasText(appid)) {
						throw new BadRequestException("公众号appi必填");
					}
					String path = mapText(set, "path");
					if (!StringUtils.hasText(path)) {
						throw new BadRequestException("授权目录必填");
					}
					Map<String, Object> wxPub = new LinkedHashMap<>();
					wxPub.put("appid", appid);
					wxPub.put("path", path);
					map.put("wx_pub", wxPub);
				}
				case "alipay", "alipay_wap", "alipay_qr" -> map.put(payChannel, "");
				default -> throw new BadRequestException("未支持支付渠道");
			}
		}
		return map;
	}

	private static Map<String, Object> asStringKeyMap(Object setting) {
		if (!(setting instanceof Map<?, ?> raw)) {
			throw new BadRequestException("支付渠道配置项格式错误");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static String mapText(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v == null) {
			return "";
		}
		if (v instanceof String s) {
			return s;
		}
		return String.valueOf(v);
	}

	private static String requireBodyText(Map<String, Object> body, String key, String message) {
		Object v = body.get(key);
		if (v == null) {
			throw new BadRequestException(message);
		}
		String s = v instanceof String ? (String) v : String.valueOf(v);
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(message);
		}
		return s.trim();
	}
}
