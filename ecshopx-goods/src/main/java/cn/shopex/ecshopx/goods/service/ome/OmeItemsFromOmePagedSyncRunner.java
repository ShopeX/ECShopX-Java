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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.goods.dispatch.GetItemsFromOmeJobDispatchPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Fetches one page of OME goods for a company and persists it. Uses a fixed page size of 10 rows per OME request,
 * aligned with the OME API contract. After a successful page with a non-zero {@code count}, either advances the stored
 * cursor when this page is the last in the window, or schedules the next page by calling
 * {@link GetItemsFromOmeJobDispatchPublisher#enqueueGetItemsFromOme(long, int, long, String)} so the same job name is
 * dispatched again with {@code page} incremented (async self-dispatch on the bus after consume completes for this
 * message).
 * Migration artifact slug reference for traceability: entry-02.
 */
@Service
public class OmeItemsFromOmePagedSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(OmeItemsFromOmePagedSyncRunner.class);

	private static final int PAGE_SIZE = 10;

	private static final DateTimeFormatter OME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;
	private final OmeLastTimeRedisAccessor omeLastTimeRedisAccessor;
	private final ShopexErpOpenApiClient openApiClient;
	private final OmeItemsFromOmePersistService omeItemsFromOmePersistService;
	private final ObjectMapper objectMapper;
	private final GetItemsFromOmeJobDispatchPublisher getItemsFromOmeJobDispatchPublisher;

	public OmeItemsFromOmePagedSyncRunner(
			ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor,
			OmeLastTimeRedisAccessor omeLastTimeRedisAccessor,
			ShopexErpOpenApiClient openApiClient,
			OmeItemsFromOmePersistService omeItemsFromOmePersistService,
			ObjectMapper objectMapper,
			GetItemsFromOmeJobDispatchPublisher getItemsFromOmeJobDispatchPublisher) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
		this.omeLastTimeRedisAccessor = omeLastTimeRedisAccessor;
		this.openApiClient = openApiClient;
		this.omeItemsFromOmePersistService = omeItemsFromOmePersistService;
		this.objectMapper = objectMapper;
		this.getItemsFromOmeJobDispatchPublisher = getItemsFromOmeJobDispatchPublisher;
	}

	/**
	 * Loads and persists the page described by {@code payload}. When {@link #shouldEnqueueNextPage(int, int)} is
	 * true for the returned total count, enqueues the next page through {@link GetItemsFromOmeJobDispatchPublisher};
	 * otherwise updates the last-modify cursor for the company to close the window.
	 */
	public void consumeQueuedPage(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		int pageNo = toInt(payload.get("page"));
		long endLastmodifyUnix = toLong(payload.get("end_lastmodify_unix"));
		String goodsBn = goodsBn(payload.get("goods_bn"));

		Map<String, Object> setting = shopexErpSettingRedisAccessor.getParsedSetting(companyId);
		if (setting == null || !Boolean.TRUE.equals(setting.get("is_openapi_open"))) {
			log.debug("companyId:{},msg:未开启OME开放数据接口", companyId);
			return;
		}

		String method = "goods.getList";
		Map<String, Object> requestDataSnapshot = new LinkedHashMap<>();
		Map<String, Object> resultSnapshot = new LinkedHashMap<>();

		try {
			Map<String, Object> params = new LinkedHashMap<>();
			if (StringUtils.hasText(goodsBn)) {
				params.put("goods_bn", goodsBn);
			} else {
				long startUnix = omeLastTimeRedisAccessor.getItemsCursorUnix(companyId);
				ZoneId zone = ZoneId.systemDefault();
				String startTime = Instant.ofEpochSecond(startUnix).atZone(zone).format(OME_TIME);
				String endTime = Instant.ofEpochSecond(endLastmodifyUnix).atZone(zone).format(OME_TIME);
				params.put("page_no", pageNo);
				params.put("page_size", PAGE_SIZE);
				params.put("start_lastmodify", startTime);
				params.put("end_lastmodify", endTime);
			}

			Map<String, Object> result = openApiClient.call(companyId, method, params);
			resultSnapshot = result;

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) result.get("data");
			if (data == null || !"succ".equals(data.get("rsp"))) {
				log.debug("companyId:{},msg:OME批量获取商品信息请求失败", companyId);
				return;
			}

			int count = toInt(data.get("count"));
			List<Map<String, Object>> goodsList = coerceGoodsList(data.get("list"));

			if (count > 0) {
				omeItemsFromOmePersistService.persistOmeGoodsPage(companyId, goodsList, method, pageNo, endLastmodifyUnix, goodsBn);
				if (!shouldEnqueueNextPage(pageNo, count)) {
					omeLastTimeRedisAccessor.setItemsCursorUnix(companyId, endLastmodifyUnix);
				} else {
					getItemsFromOmeJobDispatchPublisher.enqueueGetItemsFromOme(
							companyId, pageNo + 1, endLastmodifyUnix, goodsBn);
				}
			}

			try {
				requestDataSnapshot.putAll(data);
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, objectMapper.writeValueAsString(data), objectMapper.writeValueAsString(result));
			} catch (Exception logEx) {
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, data, result);
			}
		} catch (cn.shopex.ecshopx.common.exception.ResourceException e) {
			throw e;
		} catch (Exception e) {
			try {
				log.debug("OME请求失败:{}=>method:{}=>requestData:{}=>result:{}",
						e.getMessage(), method, objectMapper.writeValueAsString(requestDataSnapshot), objectMapper.writeValueAsString(resultSnapshot));
			} catch (Exception jsonEx) {
				log.debug("OME请求失败:{}=>method:{}=>requestData:{}=>result:{}", e.getMessage(), method, requestDataSnapshot, resultSnapshot);
			}
		}
	}

	/**
	 * {@code true} when {@code pageNo * PAGE_SIZE &lt; totalCount}, meaning another page remains and the publisher
	 * should dispatch the same job with {@code pageNo + 1}; {@code false} on the final page (cursor update path).
	 */
	public static boolean shouldEnqueueNextPage(int pageNo, int totalCount) {
		return pageNo * PAGE_SIZE < totalCount;
	}

	private static String goodsBn(Object raw) {
		if (raw == null) {
			return "";
		}
		return raw.toString();
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> coerceGoodsList(Object listObj) {
		if (!(listObj instanceof List<?> raw)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : raw) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			}
		}
		return out;
	}
}
