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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchDiscountCardInventoryPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchVipDelayPort;
import cn.shopex.ecshopx.common.members.admin.VipGradeRow;
import cn.shopex.ecshopx.members.dispatch.BatchActionMembersJobDispatchPublisher;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orchestrates admin batch member actions against a fixed threshold of 100 matching members.
 * <p>
 * When the count is at most 100, processing runs synchronously on the current request thread:
 * {@link AdminMemberBatchOperatingChunkExecutor#executeChunk} is invoked once with {@code page=1} and
 * {@code pageSize=100}. The {@link BatchActionMembersJobDispatchPublisher} is not used on this path (no
 * {@code BatchActionMembers} chunk is enqueued for the overall batch).
 * <p>
 * When the count is greater than 100, members are sharded and each chunk is enqueued on the {@code slow} dispatch
 * queue through {@link BatchActionMembersJobDispatchPublisher}; the synchronous single-chunk path above is not used
 * for that flow.
 * <p>
 * For {@code action_type=send_sms}, the small-batch path may still dispatch a nested group-SMS job through the SMS
 * port; that is separate from the chunk publisher used only when the member count is above the threshold.
 */
@Service
public class AdminMemberBatchOperatingService {

	/** Upper bound (inclusive) for the synchronous single-chunk path; above this, chunks are enqueued asynchronously. */
	private static final int SMALL_BATCH_MEMBER_THRESHOLD = 100;

	private final AdminMemberBatchOperatingFilterService adminMemberBatchOperatingFilterService;
	private final AdminMemberRelTagsUserIdsQueryService adminMemberRelTagsUserIdsQueryService;
	private final AdminMemberBatchDiscountCardInventoryPort adminMemberBatchDiscountCardInventoryPort;
	private final AdminMemberBatchVipDelayPort adminMemberBatchVipDelayPort;
	private final AdminMemberBatchOperatingCountService adminMemberBatchOperatingCountService;
	private final AdminMemberBatchOperatingUserIdsPageService adminMemberBatchOperatingUserIdsPageService;
	private final AdminMemberBatchOperatingChunkExecutor adminMemberBatchOperatingChunkExecutor;
	private final BatchActionMembersJobDispatchPublisher batchActionMembersJobDispatchPublisher;
	private final ObjectMapper objectMapper;

	public AdminMemberBatchOperatingService(
			AdminMemberBatchOperatingFilterService adminMemberBatchOperatingFilterService,
			AdminMemberRelTagsUserIdsQueryService adminMemberRelTagsUserIdsQueryService,
			@Qualifier("adminMemberBatchDiscountCardInventoryPortImpl")
					AdminMemberBatchDiscountCardInventoryPort adminMemberBatchDiscountCardInventoryPort,
			@Qualifier("adminMemberBatchVipDelayPortImpl") AdminMemberBatchVipDelayPort adminMemberBatchVipDelayPort,
			AdminMemberBatchOperatingCountService adminMemberBatchOperatingCountService,
			AdminMemberBatchOperatingUserIdsPageService adminMemberBatchOperatingUserIdsPageService,
			AdminMemberBatchOperatingChunkExecutor adminMemberBatchOperatingChunkExecutor,
			BatchActionMembersJobDispatchPublisher batchActionMembersJobDispatchPublisher,
			ObjectMapper objectMapper) {
		this.adminMemberBatchOperatingFilterService = adminMemberBatchOperatingFilterService;
		this.adminMemberRelTagsUserIdsQueryService = adminMemberRelTagsUserIdsQueryService;
		this.adminMemberBatchDiscountCardInventoryPort = adminMemberBatchDiscountCardInventoryPort;
		this.adminMemberBatchVipDelayPort = adminMemberBatchVipDelayPort;
		this.adminMemberBatchOperatingCountService = adminMemberBatchOperatingCountService;
		this.adminMemberBatchOperatingUserIdsPageService = adminMemberBatchOperatingUserIdsPageService;
		this.adminMemberBatchOperatingChunkExecutor = adminMemberBatchOperatingChunkExecutor;
		this.batchActionMembersJobDispatchPublisher = batchActionMembersJobDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	/**
	 * Runs a batch member action for the resolved selection. When the member count is at most 100, delegates once to
	 * {@link AdminMemberBatchOperatingChunkExecutor#executeChunk} with {@code page=1} and {@code pageSize=100}. When
	 * the count is greater than 100, enqueues per-shard work only through {@link BatchActionMembersJobDispatchPublisher}.
	 */
	public Map<String, Object> batchProcessMemberData(long companyId, Map<?, ?> operatorJwt, Map<String, Object> merged) {
		UserIdSelection sel = parseUserIdSelection(merged);
		AdminMemberBatchOperatingMemberQueryFilter queryFilter;
		if (sel.useDataFilter()) {
			AdminMemberBatchOperatingMemberQueryFilter built =
					adminMemberBatchOperatingFilterService.buildFilter(companyId, merged, operatorJwt);
			if (built == null) {
				throw new ResourceException("没有内容可被操作");
			}
			queryFilter = built;
		} else {
			queryFilter = new AdminMemberBatchOperatingMemberQueryFilter();
			queryFilter.setCompanyId(companyId);
			queryFilter.setUserIdsIn(sel.ids());
		}
		Map<String, Object> filterWrapped = AdminMemberBatchOperatingMemberQueryFilterSupport.wrapFilter(queryFilter);

		Map<String, Object> params = new LinkedHashMap<>();
		Object dist = operatorJwt.get("distributor_id");
		params.put("distributor_id", parseLongOrZero(dist));
		params.put("sender", buildSender(operatorJwt));

		String actionType = merged.get("action_type") == null ? null : String.valueOf(merged.get("action_type")).trim();

		if ("rel_tag".equals(actionType)) {
			params.put("tag_ids", merged.get("tag_ids"));
		} else if ("give_coupon".equals(actionType)) {
			params.put("couponsids", merged.get("couponsids"));
			params.put("source_from", "商城后台发放");
			params.put(
					"trigger_time",
					(int) Math.min(Instant.now().getEpochSecond(), Integer.MAX_VALUE));
			List<Long> tagIds = AdminMemberBatchOperatingChunkExecutor.parseLongListFlexible(merged.get("tag_ids"));
			if (!tagIds.isEmpty()) {
				List<Long> tagUserIds = adminMemberRelTagsUserIdsQueryService.listUserIdsByTagIds(companyId, tagIds);
				AdminMemberBatchOperatingMemberQueryFilter inner =
						AdminMemberBatchOperatingMemberQueryFilterSupport.unwrap(filterWrapped);
				if (inner.getUserIdsIn() != null && !inner.getUserIdsIn().isEmpty()) {
					List<Long> inter = new ArrayList<>(inner.getUserIdsIn());
					inter.retainAll(tagUserIds);
					inner.setUserIdsIn(inter);
				} else {
					inner.setUserIdsIn(new ArrayList<>(tagUserIds));
				}
				if (inner.getUserIdsIn() == null || inner.getUserIdsIn().isEmpty()) {
					return Map.of("status", true, "msg", "根据标签未找到符合条件的会员");
				}
			}
			List<Long> couponIds = AdminMemberBatchOperatingChunkExecutor.parseLongListFlexible(merged.get("couponsids"));
			adminMemberBatchDiscountCardInventoryPort.assertCouponCardsNotFullyIssued(companyId, couponIds);
		} else if ("send_sms".equals(actionType)) {
			params.put("sms_content", merged.get("sms_content"));
		} else if ("vip_delay".equals(actionType)) {
			JsonNode form = readJsonObjectOrThrow(objectMapper, merged.get("vip_grade_form"), "vip_grade_form 格式错误");
			long vipGradeId = form.path("vip_grade_id").asLong(0L);
			int addDay = form.path("add_day").asInt(0);
			if (addDay <= 0) {
				throw new BadRequestException("请填写正确的延期天数");
			}
			VipGradeRow grade = adminMemberBatchVipDelayPort.loadVipGrade(companyId, vipGradeId);
			if (grade == null) {
				throw new ResourceException("无效的付费会员等级");
			}
			params.put("vip_grade_id", vipGradeId);
			params.put("add_day", addDay);
			if ("expired".equals(merged.get("filter"))) {
				adminMemberBatchVipDelayPort.processExpiredMemberExtension(companyId, vipGradeId, addDay);
				return Map.of("status", true, "msg", "已经处理成功");
			}
		} else if ("set_grade".equals(actionType)) {
			JsonNode form = readJsonObjectOrThrow(objectMapper, merged.get("grade_form"), "grade_form 格式错误");
			params.put("grade_id", parseJsonLong(form.get("grade_id"), 0L));
			if (form.has("remarks") && !form.get("remarks").isNull()) {
				params.put("remarks", form.get("remarks").asText(""));
			} else {
				params.put("remarks", "");
			}
		}

		long count = adminMemberBatchOperatingCountService.countMembersForBatchOperating(companyId, filterWrapped);
		if (count > SMALL_BATCH_MEMBER_THRESHOLD) {
			int pageSize = 50;
			int pages = (int) Math.ceil(count / (double) pageSize);
			for (int page = 1; page <= pages; page++) {
				List<Long> userIds =
						adminMemberBatchOperatingUserIdsPageService.listUserIdsPage(
								companyId, filterWrapped, page, pageSize);
				if (userIds.isEmpty()) {
					continue;
				}
				AdminMemberBatchOperatingMemberQueryFilter base =
						AdminMemberBatchOperatingMemberQueryFilterSupport.unwrap(filterWrapped);
				Map<String, Object> chunkFilter =
						AdminMemberBatchOperatingMemberQueryFilterSupport.wrapFilter(
								AdminMemberBatchOperatingMemberQueryFilterSupport.copyReplacingUserIds(base, userIds));
				Map<String, Object> operatorParams = new LinkedHashMap<>(params);
				operatorParams.put("__batch_operator_jwt__", operatorJwt);
				batchActionMembersJobDispatchPublisher.enqueueBatchActionMembersChunk(
						companyId, operatorParams, actionType, chunkFilter, 1, pageSize);
			}
			return Map.of("status", true, "msg", "由于数据较多，已经加入队列处理");
		}
		Map<String, Object> operatorParams = new LinkedHashMap<>(params);
		operatorParams.put("__batch_operator_jwt__", operatorJwt);
		Map<String, Object> chunkFilter = filterWrapped;
		try {
			adminMemberBatchOperatingChunkExecutor.executeChunk(
					companyId, operatorParams, actionType, chunkFilter, 1, SMALL_BATCH_MEMBER_THRESHOLD);
		} catch (RuntimeException e) {
			throw new ResourceException(e.getMessage() == null ? "" : e.getMessage());
		}
		return Map.of("status", true, "msg", "已经处理成功");
	}

	private static long parseJsonLong(JsonNode n, long defaultVal) {
		if (n == null || n.isNull()) {
			return defaultVal;
		}
		if (n.isNumber()) {
			return n.longValue();
		}
		String t = n.asText("").trim();
		if (!StringUtils.hasText(t)) {
			return defaultVal;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static JsonNode readJsonObjectOrThrow(ObjectMapper mapper, Object raw, String badMessage) {
		if (raw == null) {
			throw new BadRequestException(badMessage);
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(badMessage);
		}
		try {
			JsonNode n = mapper.readTree(s);
			if (!n.isObject()) {
				throw new BadRequestException(badMessage);
			}
			return n;
		} catch (JsonProcessingException e) {
			throw new BadRequestException(badMessage);
		}
	}

	private record UserIdSelection(boolean dataFilterPath, List<Long> ids) {
		boolean useDataFilter() {
			return dataFilterPath;
		}
	}

	private static UserIdSelection parseUserIdSelection(Map<String, Object> merged) {
		if (!merged.containsKey("user_id")) {
			return new UserIdSelection(true, List.of());
		}
		Object raw = merged.get("user_id");
		if (raw == null) {
			throw new BadRequestException("会员ID必填");
		}
		List<Long> ids = new ArrayList<>();
		if (raw instanceof Iterable<?> it && !(raw instanceof String)) {
			for (Object el : it) {
				if (el == null || (el instanceof String es && !StringUtils.hasText(es.trim()))) {
					throw new BadRequestException("会员ID必填");
				}
				try {
					ids.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("会员ID必须为整数");
				}
			}
		} else if (raw instanceof String[] arr) {
			for (String el : arr) {
				if (el == null || !StringUtils.hasText(el.trim())) {
					throw new BadRequestException("会员ID必填");
				}
				try {
					ids.add(Long.parseLong(el.trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("会员ID必须为整数");
				}
			}
		} else if (raw instanceof Long[] arr) {
			for (Long el : arr) {
				if (el == null) {
					throw new BadRequestException("会员ID必填");
				}
				ids.add(el);
			}
		} else if (raw instanceof long[] arr) {
			for (long el : arr) {
				ids.add(el);
			}
		} else if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return new UserIdSelection(true, List.of());
			}
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException("会员ID必须为整数");
			}
		} else if (raw instanceof Number n) {
			ids.add(n.longValue());
		} else {
			throw new BadRequestException("会员ID必须为整数");
		}
		if (ids.isEmpty()) {
			return new UserIdSelection(true, List.of());
		}
		return new UserIdSelection(false, ids);
	}

	private static String buildSender(Map<?, ?> operatorJwt) {
		String opType = String.valueOf(operatorJwt.get("operator_type")).trim();
		if ("staff".equalsIgnoreCase(opType)) {
			return "员工-"
					+ Objects.toString(operatorJwt.get("username"), "")
					+ "-"
					+ Objects.toString(operatorJwt.get("mobile"), "");
		}
		return Objects.toString(operatorJwt.get("username"), "");
	}

	private static long parseLongOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
