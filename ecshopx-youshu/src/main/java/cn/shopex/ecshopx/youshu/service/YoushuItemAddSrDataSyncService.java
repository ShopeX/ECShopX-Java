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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.integration.YoushuItemSkuPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class YoushuItemAddSrDataSyncService {

	private static final int DATA_SOURCE_TYPE_SKU = 3;

	private final YoushuSettingMapper youshuSettingMapper;
	private final YoushuDataSourceApiPort youshuDataSourceApiPort;
	private final YoushuItemSkuPushPort youshuItemSkuPushPort;
	private final ItemsMapper itemsMapper;
	private final ObjectMapper objectMapper;

	public YoushuItemAddSrDataSyncService(
			YoushuSettingMapper youshuSettingMapper,
			YoushuDataSourceApiPort youshuDataSourceApiPort,
			YoushuItemSkuPushPort youshuItemSkuPushPort,
			ItemsMapper itemsMapper,
			ObjectMapper objectMapper) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.youshuDataSourceApiPort = youshuDataSourceApiPort;
		this.youshuItemSkuPushPort = youshuItemSkuPushPort;
		this.itemsMapper = itemsMapper;
		this.objectMapper = objectMapper;
	}

	public void syncItemsSku(long companyId, long itemId) {
		YoushuSetting setting =
				youshuSettingMapper.selectOne(
						new LambdaQueryWrapper<YoushuSetting>()
								.eq(YoushuSetting::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (setting == null) {
			return;
		}
		String merchantId = setting.getMerchantId();
		if (!StringUtils.hasText(merchantId)) {
			return;
		}

		Items item = itemsMapper.selectById(itemId);
		if (item == null
				|| item.getCompanyId() == null
				|| item.getCompanyId().longValue() != companyId
				|| item.getItemId() == null
				|| item.getItemId() <= 0L) {
			return;
		}

		List<Map<String, Object>> skuRows = buildSkuRows(item);
		if (skuRows.isEmpty()) {
			return;
		}

		YoushuOpenApiCredentials credentials = toCredentials(setting);
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(merchantId.trim(), DATA_SOURCE_TYPE_SKU, credentials);
		youshuItemSkuPushPort.pushSkuRows(dataSourceId, skuRows, credentials);
	}

	private List<Map<String, Object>> buildSkuRows(Items item) {
		List<String> picUrls = extractPicUrls(item.getPics());
		List<Map<String, Object>> imgUrls = buildImgUrlsStructure(picUrls);

		boolean isAvailable = "onsale".equals(item.getApproveStatus());
		long externalSkuId = item.getItemId() != null ? item.getItemId() : 0L;
		long externalSpuId = item.getGoodsId() != null ? item.getGoodsId() : 0L;
		String barcode = item.getBarcode() != null ? item.getBarcode() : "";
		String productName = item.getItemName() != null ? item.getItemName() : "";
		String externalCreated =
				item.getCreated() != null ? String.valueOf(item.getCreated().longValue() * 1000L) : "0";
		String categoryLeaf = item.getItemCategory() != null ? item.getItemCategory() : "";

		Map<String, Object> categoryProps = new LinkedHashMap<>();
		categoryProps.put("external_category_id_leaf", categoryLeaf);
		categoryProps.put("category_type", 2);

		Map<String, Object> sku = new LinkedHashMap<>();
		sku.put("external_sku_id", externalSkuId);
		sku.put("external_spu_id", externalSpuId);
		sku.put("sku_barcode", barcode);
		sku.put("img_urls", imgUrls);
		sku.put("category_props", categoryProps);
		sku.put("sales_props", Map.of("is_available", isAvailable));
		sku.put("desc_props", Map.of("product_name_chinese", productName));
		sku.put("external_created_time", externalCreated);
		return List.of(sku);
	}

	private List<String> extractPicUrls(String picsJson) {
		if (picsJson == null || picsJson.isBlank()) {
			return List.of();
		}
		try {
			JsonNode root = objectMapper.readTree(picsJson);
			List<String> out = new ArrayList<>();
			if (root.isArray()) {
				for (JsonNode el : root) {
					if (el.isTextual()) {
						String t = el.asText();
						if (StringUtils.hasText(t)) {
							out.add(t);
						}
					} else if (el.isObject()) {
						JsonNode url = el.get("url");
						if (url != null && url.isTextual() && StringUtils.hasText(url.asText())) {
							out.add(url.asText());
						}
					}
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private static List<Map<String, Object>> buildImgUrlsStructure(List<String> picUrls) {
		List<Map<String, Object>> imageList = new ArrayList<>();
		for (String u : picUrls) {
			imageList.add(Map.of("img_url", u));
		}
		int cap = Math.min(imageList.size(), 10);
		List<Map<String, Object>> capped = imageList.subList(0, cap);
		List<Map<String, Object>> primary = capped.isEmpty() ? List.of() : List.of(capped.get(0));
		Map<String, Object> block = new LinkedHashMap<>();
		block.put("primary_imgs", primary);
		block.put("imgs", capped);
		block.put("detail_imgs", capped);
		return List.of(block);
	}

	private static YoushuOpenApiCredentials toCredentials(YoushuSetting v) {
		String base = firstNonBlank(v.getApiUrl(), v.getSandboxApiUrl());
		String appId = firstNonBlank(v.getAppId(), v.getSandboxAppId());
		String appSecret = firstNonBlank(v.getAppSecret(), v.getSandboxAppSecret());
		return new YoushuOpenApiCredentials(base, appId, appSecret);
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "";
	}
}
