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

package cn.shopex.ecshopx.systemlink.jushuitan;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.integration.jushuitan.JushuitanUploadItemsBatchHandler;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStructAssembler;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStructAssembler.JushuitanItemStructBundle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
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
public class JushuitanUploadItemsBatchHandlerImpl implements JushuitanUploadItemsBatchHandler {

	private static final Logger log = LoggerFactory.getLogger(JushuitanUploadItemsBatchHandlerImpl.class);

	private static final String FLOW_LOCK_KEY = "JushuitanApiFlowControlLock";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final JushuitanItemStructAssembler jushuitanItemStructAssembler;
	private final JushuitanOpenApiClient jushuitanOpenApiClient;
	private final DistributorListQueryService distributorListQueryService;

	public JushuitanUploadItemsBatchHandlerImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			JushuitanItemStructAssembler jushuitanItemStructAssembler,
			JushuitanOpenApiClient jushuitanOpenApiClient,
			DistributorListQueryService distributorListQueryService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.jushuitanItemStructAssembler = jushuitanItemStructAssembler;
		this.jushuitanOpenApiClient = jushuitanOpenApiClient;
		this.distributorListQueryService = distributorListQueryService;
	}

	@Override
	public void handleBatch(long companyId, List<Long> itemIds, long distributorId, String itemType) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		log.debug("jushuitan upload batch companyId={} count={} distributorId={} itemType={}", companyId, itemIds.size(), distributorId, itemType);
		Map<String, Object> setting = readJushuitanSetting(companyId);
		Object open = setting.get("is_open");
		boolean enabled = open instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(open));
		if (!enabled) {
			log.debug("jushuitan upload skipped: ERP not enabled companyId={}", companyId);
			return;
		}
		String accessToken = setting.get("access_token") != null ? setting.get("access_token").toString() : "";
		Object shopIdObj = setting.get("shop_id");
		String shopId = shopIdObj != null ? shopIdObj.toString() : "";
		if (distributorId > 0) {
			List<Distributor> dist = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (dist == null || dist.isEmpty()) {
				log.debug("jushuitan upload skipped: distributor missing companyId={} distributorId={}", companyId, distributorId);
				return;
			}
			Long jst = dist.get(0).getJstShopId();
			if (jst == null || jst <= 0) {
				log.debug("jushuitan upload skipped: no jst_shop_id companyId={} distributorId={}", companyId, distributorId);
				return;
			}
			shopId = jst.toString();
		}

		int chunk = 2;
		for (int i = 0; i < itemIds.size(); i += chunk) {
			int end = Math.min(i + chunk, itemIds.size());
			List<Long> slice = itemIds.subList(i, end);
			List<Map<String, Object>> itemStructs = new ArrayList<>();
			List<List<Map<String, Object>>> shopItemStructs = new ArrayList<>();
			for (Long itemId : slice) {
				if (itemId == null || itemId <= 0) {
					continue;
				}
				JushuitanItemStructBundle built = jushuitanItemStructAssembler.build(companyId, itemId, distributorId, shopId, itemType);
				if (built == null || built.itemSkus().isEmpty()) {
					log.debug("jushuitan upload skip item: struct empty companyId={} itemId={}", companyId, itemId);
					continue;
				}
				itemStructs.addAll(built.itemSkus());
				shopItemStructs.addAll(built.shopItemChunks());
			}
			if (itemStructs.isEmpty() || shopItemStructs.isEmpty()) {
				continue;
			}
			acquireFlowLock();
			try {
				Map<String, Object> r1 = jushuitanOpenApiClient.call(companyId, "item_add", Map.of("items", itemStructs), accessToken);
				log.debug("jushuitan item_add result companyId={} body={}", companyId, r1);
				for (List<Map<String, Object>> shopBatch : shopItemStructs) {
					Map<String, Object> r2 = jushuitanOpenApiClient.call(companyId, "shop_item_add", Map.of("items", shopBatch), accessToken);
					log.debug("jushuitan shop_item_add result companyId={} body={}", companyId, r2);
				}
			} catch (Exception e) {
				log.debug("jushuitan upload batch failed companyId={} msg={}", companyId, e.getMessage());
			}
		}
	}

	private void acquireFlowLock() {
		for (;;) {
			Boolean ok = companysRedisTemplate.opsForValue().setIfAbsent(FLOW_LOCK_KEY, "1", Duration.ofSeconds(1));
			if (Boolean.TRUE.equals(ok)) {
				return;
			}
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private Map<String, Object> readJushuitanSetting(long companyId) {
		String key = "JushuitanSetting:" + sha1Hex(String.valueOf(companyId));
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
