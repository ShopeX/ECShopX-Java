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

package cn.shopex.ecshopx.systemlink.wdterp;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpItemStructAssembler;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpItemStructAssembler.WdtErpItemStruct;
import cn.shopex.ecshopx.goods.integration.wdterp.WdtErpUploadItemsBatchHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WdtErpUploadItemsBatchHandlerImpl implements WdtErpUploadItemsBatchHandler {

	private static final Logger log = LoggerFactory.getLogger(WdtErpUploadItemsBatchHandlerImpl.class);

	private static final String METHOD_ITEM_ADD = "goods.Goods.push";
	private static final String METHOD_ITEM_API_ADD = "goods.ApiGoods.upload";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WdtErpItemStructAssembler wdtErpItemStructAssembler;
	private final WdtErpOpenApiClient wdtErpOpenApiClient;
	private final DistributorListQueryService distributorListQueryService;

	public WdtErpUploadItemsBatchHandlerImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			WdtErpItemStructAssembler wdtErpItemStructAssembler,
			WdtErpOpenApiClient wdtErpOpenApiClient,
			DistributorListQueryService distributorListQueryService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.wdtErpItemStructAssembler = wdtErpItemStructAssembler;
		this.wdtErpOpenApiClient = wdtErpOpenApiClient;
		this.distributorListQueryService = distributorListQueryService;
	}

	@Override
	public void handleBatch(long companyId, List<Long> itemIds, long distributorId) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		log.debug("wdterp upload batch companyId={} count={} distributorId={}", companyId, itemIds.size(), distributorId);
		Map<String, Object> setting = readWdtErpSetting(companyId);
		Object open = setting.get("is_open");
		boolean enabled = open instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(open));
		if (!enabled) {
			log.debug("wdterp upload skipped: ERP not enabled companyId={}", companyId);
			return;
		}
		Object shopNoObj = setting.get("shop_no");
		String shopNo = shopNoObj != null ? shopNoObj.toString() : "";
		if (distributorId > 0) {
			List<Distributor> dist = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (dist == null || dist.isEmpty()) {
				log.debug("wdterp upload skipped: distributor missing companyId={} distributorId={}", companyId, distributorId);
				return;
			}
			String wdt = dist.get(0).getWdtShopNo();
			if (!StringUtils.hasText(wdt)) {
				log.debug("wdterp upload skipped: no wdt_shop_no companyId={} distributorId={}", companyId, distributorId);
				return;
			}
			shopNo = wdt;
		}

		String sid = setting.get("sid") != null ? setting.get("sid").toString() : "";
		String appKey = setting.get("app_key") != null ? setting.get("app_key").toString() : "";
		String appSecret = setting.get("app_secret") != null ? setting.get("app_secret").toString() : "";

		for (Long itemId : itemIds) {
			if (itemId == null || itemId <= 0) {
				continue;
			}
			try {
				WdtErpItemStruct struct = wdtErpItemStructAssembler.getItemStruct(companyId, itemId, distributorId);
				if (struct == null) {
					log.debug("wdterp upload skip item: struct empty companyId={} itemId={}", companyId, itemId);
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> goods = (Map<String, Object>) struct.goodsPush().get("goods");
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specList = (List<Map<String, Object>>) struct.goodsPush().get("specList");
				if (goods == null || specList == null) {
					continue;
				}
				Object r1 = wdtErpOpenApiClient.call(companyId, METHOD_ITEM_ADD, List.of(goods, specList), sid, appKey, appSecret);
				log.debug("wdterp goods push result companyId={} itemId={} body={}", companyId, itemId, r1);

				Map<String, Object> param = new LinkedHashMap<>();
				param.put("shop_no", shopNo);
				param.put("goods_list", struct.apiGoodsUpload());
				Object r2 = wdtErpOpenApiClient.call(companyId, METHOD_ITEM_API_ADD, List.of(param), sid, appKey, appSecret);
				log.debug("wdterp api goods upload result companyId={} itemId={} body={}", companyId, itemId, r2);
			} catch (Exception e) {
				log.debug("wdterp upload item failed companyId={} itemId={} msg={}", companyId, itemId, e.getMessage());
			}
		}
	}

	private Map<String, Object> readWdtErpSetting(long companyId) {
		String key = "WdtErpSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
