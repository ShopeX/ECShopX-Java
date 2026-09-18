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

import cn.shopex.ecshopx.goods.dispatch.GetItemsSpecFromOmeJobDispatchPublisher;
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
 * Consumes one OME {@code goodsspec.getList} page for a fixed {@code end_lastmodify_unix} window, persists rows,
 * then either writes the spec cursor (final page) or enqueues the next page on the slow queue via
 * {@link GetItemsSpecFromOmeJobDispatchPublisher#enqueueGetItemsSpecFromOme} and {@code DispatchFacade#dispatchJob}.
 * <p>
 * Page size is {@value #PAGE_SIZE}, matching the inventory job {@code page_size}.
 * Inventory anchor {@code entry-02-jc-getitemsspecfromome-handle}.
 */
@Service
public class OmeItemSpecFromOmePagedSyncRunner {

	private static final Logger log = LoggerFactory.getLogger(OmeItemSpecFromOmePagedSyncRunner.class);

	private static final int PAGE_SIZE = 10;

	private static final DateTimeFormatter OME_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;
	private final OmeLastTimeRedisAccessor omeLastTimeRedisAccessor;
	private final ShopexErpOpenApiClient openApiClient;
	private final OmeItemSpecBatchPersistService omeItemSpecBatchPersistService;
	private final ObjectMapper objectMapper;
	private final GetItemsSpecFromOmeJobDispatchPublisher getItemsSpecFromOmeJobDispatchPublisher;

	public OmeItemSpecFromOmePagedSyncRunner(
			ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor,
			OmeLastTimeRedisAccessor omeLastTimeRedisAccessor,
			ShopexErpOpenApiClient openApiClient,
			OmeItemSpecBatchPersistService omeItemSpecBatchPersistService,
			ObjectMapper objectMapper,
			GetItemsSpecFromOmeJobDispatchPublisher getItemsSpecFromOmeJobDispatchPublisher) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
		this.omeLastTimeRedisAccessor = omeLastTimeRedisAccessor;
		this.openApiClient = openApiClient;
		this.omeItemSpecBatchPersistService = omeItemSpecBatchPersistService;
		this.objectMapper = objectMapper;
		this.getItemsSpecFromOmeJobDispatchPublisher = getItemsSpecFromOmeJobDispatchPublisher;
	}

	/**
	 * Runs one Bus-queued page for the OME item-spec list sync window.
	 * <p>
	 * Inventory anchor {@code entry-02-jc-getitemsspecfromome-handle}. Invoked from
	 * {@link cn.shopex.ecshopx.goods.dispatch.GetItemsSpecFromOmeJobHandler#handle(java.util.Map)} after
	 * {@code company_id}, {@code page}, and {@code end_lastmodify_unix} are read from {@code payload}.
	 * Loads settings, calls OME {@code goodsspec.getList} with {@value #PAGE_SIZE} as {@code page_size}, and on a
	 * successful {@code rsp=succ} response with parsed {@code data.count}: persists non-empty {@code lists}, then
	 * either writes the spec cursor or enqueues the next page. When the window is complete—equivalently when
	 * {@code page_no * page_size >= count} with this runner's fixed {@code page_size} of {@value #PAGE_SIZE}—calls
	 * {@link OmeLastTimeRedisAccessor#setSpecCursorUnix(long, long)} and does not re-enqueue; otherwise calls
	 * {@link GetItemsSpecFromOmeJobDispatchPublisher#enqueueGetItemsSpecFromOme(long, int, long)} with
	 * {@code companyId}, {@code pageNo + 1}, and the same {@code endLastmodifyUnix}, which reaches
	 * {@code DispatchFacade#dispatchJob} (slow queue, job-level {@code DispatchOptions}) for the same job name as
	 * the first hop.
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

		String method = "goodsspec.getList";
		Map<String, Object> requestDataSnapshot = new LinkedHashMap<>();
		Map<String, Object> resultSnapshot = new LinkedHashMap<>();

		try {
			long startUnix = omeLastTimeRedisAccessor.getSpecCursorUnix(companyId);
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

			if (count > 0) {
				List<Map<String, Object>> lists = coerceSpecLists(data.get("lists"));
				omeItemSpecBatchPersistService.saveSpecs(companyId, lists);
			}

			if (!shouldEnqueueNextPage(pageNo, count)) {
				omeLastTimeRedisAccessor.setSpecCursorUnix(companyId, endLastmodifyUnix);
			} else {
				getItemsSpecFromOmeJobDispatchPublisher.enqueueGetItemsSpecFromOme(
						companyId, pageNo + 1, endLastmodifyUnix);
			}

			try {
				requestDataSnapshot.putAll(data);
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, objectMapper.writeValueAsString(data), objectMapper.writeValueAsString(result));
			} catch (Exception logEx) {
				log.debug("{}=>requestData:{}==>result:\r\n{}", method, data, result);
			}
		} catch (Exception e) {
			try {
				log.debug("OMS请求失败:{}=>method:{}=>requestData:{}=>result:{}",
						e.getMessage(), method, objectMapper.writeValueAsString(requestDataSnapshot), objectMapper.writeValueAsString(resultSnapshot));
			} catch (Exception jsonEx) {
				log.debug("OMS请求失败:{}=>method:{}=>requestData:{}=>result:{}", e.getMessage(), method, requestDataSnapshot, resultSnapshot);
			}
		}
	}

	/**
	 * Whether another page for the same sync window should be enqueued after the current OME page.
	 * <p>
	 * Inventory anchor {@code entry-02-jc-getitemsspecfromome-handle}. Returns {@code true} iff
	 * {@code pageNo * PAGE_SIZE < totalCount}, i.e. the final-page predicate
	 * {@code page_no * page_size >= count} is <strong>not</strong> satisfied yet (with {@code page_size} fixed at
	 * {@value #PAGE_SIZE}). When {@code true}, {@link #consumeQueuedPage(java.util.Map)} calls
	 * {@link GetItemsSpecFromOmeJobDispatchPublisher#enqueueGetItemsSpecFromOme(long, int, long)} for
	 * {@code pageNo + 1}; when {@code false}, it updates the spec cursor instead and does not chain another job.
	 *
	 * @param pageNo one-based page number for the OME list request (same as payload {@code page})
	 * @param totalCount value of {@code data.count} from the OME response
	 * @return {@code true} if a follow-on job should be dispatched for the next page
	 */
	public static boolean shouldEnqueueNextPage(int pageNo, int totalCount) {
		return pageNo * PAGE_SIZE < totalCount;
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
	private static List<Map<String, Object>> coerceSpecLists(Object listsObj) {
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
