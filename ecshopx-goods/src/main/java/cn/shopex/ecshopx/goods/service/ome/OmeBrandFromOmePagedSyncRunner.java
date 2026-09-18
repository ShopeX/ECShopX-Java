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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.dispatch.GetBrandFromOmeJobDispatchPublisher;
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

/**
 * Consumes one OME {@code goodsbrand.getList} page for a fixed {@code end_lastmodify_unix} window, persists rows,
 * then either writes the brand cursor (final page) or enqueues the next page on the slow queue via
 * {@link GetBrandFromOmeJobDispatchPublisher#enqueueGetBrandFromOme} and {@code DispatchFacade#dispatchJob}.
 * <p>
 * Page size is {@value #PAGE_SIZE}, matching the inventory job {@code page_size}.
 * Inventory anchor {@code entry-02-jc-getbrandfromome-handle}.
 */
@Service
public class OmeBrandFromOmePagedSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(OmeBrandFromOmePagedSyncRunner.class);

	private static final int PAGE_SIZE = 10;

	private static final DateTimeFormatter OME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;
	private final OmeLastTimeRedisAccessor omeLastTimeRedisAccessor;
	private final ShopexErpOpenApiClient openApiClient;
	private final OmeBrandBatchPersistService omeBrandBatchPersistService;
	private final ObjectMapper objectMapper;
	private final GetBrandFromOmeJobDispatchPublisher getBrandFromOmeJobDispatchPublisher;

	public OmeBrandFromOmePagedSyncRunner(
			ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor,
			OmeLastTimeRedisAccessor omeLastTimeRedisAccessor,
			ShopexErpOpenApiClient openApiClient,
			OmeBrandBatchPersistService omeBrandBatchPersistService,
			ObjectMapper objectMapper,
			GetBrandFromOmeJobDispatchPublisher getBrandFromOmeJobDispatchPublisher) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
		this.omeLastTimeRedisAccessor = omeLastTimeRedisAccessor;
		this.openApiClient = openApiClient;
		this.omeBrandBatchPersistService = omeBrandBatchPersistService;
		this.objectMapper = objectMapper;
		this.getBrandFromOmeJobDispatchPublisher = getBrandFromOmeJobDispatchPublisher;
	}

	/**
	 * Fetches one page, persists non-empty lists when {@code count > 0}, then if
	 * {@code pageNo * pageSize >= totalCount} updates {@link OmeLastTimeRedisAccessor#setBrandCursorUnix}; otherwise
	 * calls {@link GetBrandFromOmeJobDispatchPublisher#enqueueGetBrandFromOme} so the next page is scheduled through
	 * {@code dispatchJob} on the same job metadata as the initial enqueue. Anchor {@code entry-02-jc-getbrandfromome-handle}.
	 */
	public void consumeQueuedPage(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		int pageNo = toInt(payload.get("page"));
		long endLastmodifyUnix = toLong(payload.get("end_lastmodify_unix"));

		Map<String, Object> setting = shopexErpSettingRedisAccessor.getParsedSetting(companyId);
		if (setting == null || !Boolean.TRUE.equals(setting.get("is_openapi_open"))) {
			log.debug("companyId:{},msg:未开启OME开放数据接口", companyId);
			return;
		}

		String method = "goodsbrand.getList";
		Map<String, Object> requestDataSnapshot = new LinkedHashMap<>();
		Map<String, Object> resultSnapshot = new LinkedHashMap<>();

		try {
			long startUnix = omeLastTimeRedisAccessor.getBrandCursorUnix(companyId);
			ZoneId zone = ZoneId.systemDefault();
			String startTime = Instant.ofEpochSecond(startUnix).atZone(zone).format(OME_TIME);
			String endTime = Instant.ofEpochSecond(endLastmodifyUnix).atZone(zone).format(OME_TIME);

			Map<String, Object> params = new LinkedHashMap<>();
			params.put("start_time", startTime);
			params.put("end_time", endTime);
			params.put("page_no", pageNo);
			params.put("page_size", PAGE_SIZE);

			Map<String, Object> result = openApiClient.call(companyId, method, params);
			resultSnapshot = result;

			if (result == null || !"succ".equals(result.get("rsp"))) {
				log.debug("companyId:{},msg:OME批量获取商品规格信息请求失败", companyId);
				return;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) result.get("data");
			if (data == null) {
				data = Map.of();
			}
			int count = toInt(data.get("count"));
			List<Map<String, Object>> lists = coerceBrandLists(data.get("lists"));

			if (count > 0 && !lists.isEmpty()) {
				omeBrandBatchPersistService.saveBrands(companyId, lists);
			}

			if (!shouldEnqueueNextPage(pageNo, PAGE_SIZE, count)) {
				omeLastTimeRedisAccessor.setBrandCursorUnix(companyId, endLastmodifyUnix);
			} else {
				getBrandFromOmeJobDispatchPublisher.enqueueGetBrandFromOme(companyId, pageNo + 1, endLastmodifyUnix);
			}

			try {
				requestDataSnapshot.putAll(data);
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, objectMapper.writeValueAsString(data), objectMapper.writeValueAsString(result));
			} catch (Exception logEx) {
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, data, result);
			}
		} catch (ResourceException e) {
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
	 * Returns whether another job should be queued for the same window. Equivalent to
	 * {@code pageNo * pageSize < totalCount} for non-negative inputs. When {@code false}, the caller treats the window
	 * as complete and writes the brand cursor. Anchor {@code entry-02-jc-getbrandfromome-handle}.
	 */
	public static boolean shouldEnqueueNextPage(int pageNo, int pageSize, int totalCount) {
		return pageNo * pageSize < totalCount;
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
	private static List<Map<String, Object>> coerceBrandLists(Object listsObj) {
		if (!(listsObj instanceof List<?> raw)) {
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
