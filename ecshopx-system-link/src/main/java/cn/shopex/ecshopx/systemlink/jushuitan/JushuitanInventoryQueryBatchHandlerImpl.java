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

import cn.shopex.ecshopx.goods.integration.jushuitan.JushuitanInventoryQueryBatchHandler;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanInventoryPersistService;
import cn.shopex.ecshopx.goods.service.jushuitan.JushuitanItemStoreQueryStructService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JushuitanInventoryQueryBatchHandlerImpl implements JushuitanInventoryQueryBatchHandler {

	private static final Logger log = LoggerFactory.getLogger(JushuitanInventoryQueryBatchHandlerImpl.class);

	private static final int CHUNK = 20;

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final JushuitanOpenApiClient jushuitanOpenApiClient;
	private final JushuitanItemStoreQueryStructService jushuitanItemStoreQueryStructService;
	private final JushuitanInventoryPersistService jushuitanInventoryPersistService;

	public JushuitanInventoryQueryBatchHandlerImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			JushuitanOpenApiClient jushuitanOpenApiClient,
			JushuitanItemStoreQueryStructService jushuitanItemStoreQueryStructService,
			JushuitanInventoryPersistService jushuitanInventoryPersistService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.jushuitanOpenApiClient = jushuitanOpenApiClient;
		this.jushuitanItemStoreQueryStructService = jushuitanItemStoreQueryStructService;
		this.jushuitanInventoryPersistService = jushuitanInventoryPersistService;
	}

	@Override
	public void handleBatches(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		log.debug("jushuitan inventory query batch companyId={} count={}", companyId, itemIds.size());
		Map<String, Object> setting = readJushuitanSetting(companyId);
		Object open = setting.get("is_open");
		boolean enabled = open instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(open));
		if (!enabled) {
			log.debug("jushuitan inventory query skipped: ERP not enabled companyId={}", companyId);
			return;
		}
		String accessToken = setting.get("access_token") != null ? setting.get("access_token").toString() : "";

		for (int i = 0; i < itemIds.size(); i += CHUNK) {
			int end = Math.min(i + CHUNK, itemIds.size());
			List<Long> chunk = itemIds.subList(i, end);
			Map<String, Object> payload = jushuitanItemStoreQueryStructService.buildItemStoreQueryPayload(companyId, chunk);
			if (payload == null) {
				log.debug("jushuitan inventory query skip chunk: no payload companyId={} chunkSize={}", companyId, chunk.size());
				continue;
			}
			try {
				Map<String, Object> result = jushuitanOpenApiClient.call(companyId, "item_store_query", payload, accessToken);
				Object codeObj = result.get("code");
				String codeStr = codeObj == null ? "" : String.valueOf(codeObj);
				if (!"0".equals(codeStr)) {
					continue;
				}
				Object invObj = result.get("inventorys");
				if (!(invObj instanceof List<?> rawList) || rawList.isEmpty()) {
					continue;
				}
				List<Map<String, Object>> inventorys = new ArrayList<>();
				for (Object o : rawList) {
					if (o instanceof Map<?, ?> m) {
						@SuppressWarnings("unchecked")
						Map<String, Object> typed = (Map<String, Object>) m;
						inventorys.add(typed);
					}
				}
				if (!inventorys.isEmpty()) {
					jushuitanInventoryPersistService.persistInventoriesFromJushuitan(companyId, inventorys);
				}
			} catch (Exception e) {
				log.debug("jushuitan inventory query chunk failed companyId={} msg={}", companyId, e.getMessage());
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
