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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberListCustomerUnionidsFailException;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.integration.distribution.OpenapiMemberListDistributorByShopCodePort;
import cn.shopex.ecshopx.members.integration.orders.OpenapiMemberOrderListOrdersPort;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberListQueryFilter;
import cn.shopex.ecshopx.members.service.browse.MemberBrowseHistoryListService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1MemberListService {

	private static final Logger log = LoggerFactory.getLogger(OpenapiThirdApiV1MemberListService.class);
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MembersMapper membersMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort;
	private final MemberBrowseHistoryListService memberBrowseHistoryListService;
	private final LangueProperties langueProperties;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final OpenapiMemberListDistributorByShopCodePort distributorByShopCodePort;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV1MemberListService(
			MembersMapper membersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OpenapiMemberOrderListOrdersPort openapiMemberOrderListOrdersPort,
			MemberBrowseHistoryListService memberBrowseHistoryListService,
			LangueProperties langueProperties,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			OpenapiMemberListDistributorByShopCodePort distributorByShopCodePort,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.openapiMemberOrderListOrdersPort = openapiMemberOrderListOrdersPort;
		this.memberBrowseHistoryListService = memberBrowseHistoryListService;
		this.langueProperties = langueProperties;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.distributorByShopCodePort = distributorByShopCodePort;
		this.objectMapper = objectMapper;
	}

	public Object executeOpenapiMemberList(long companyId, Map<String, Object> mergedParams) {
		validateMemberListParams(mergedParams);
		String scope = stringOrDefault(mergedParams.get("scope"), "fp");
		if ("customer".equals(scope)) {
			return executeCustomerUnionids(companyId, mergedParams);
		}
		return executeFpMemberList(companyId, mergedParams);
	}

	private void validateMemberListParams(Map<String, Object> mergedParams) {
		if (mergedParams.containsKey("scope")) {
			String scope = stringValue(mergedParams.get("scope"));
			if (scope != null && !"fp".equals(scope) && !"customer".equals(scope)) {
				throw new ResourceException("scope参数错误，应为fp或customer");
			}
		}
		if (mergedParams.containsKey("page")) {
			Integer page = parseIntegerOrNull(mergedParams.get("page"));
			if (page == null || page < 1) {
				throw new ResourceException("页码必须为正整数");
			}
		}
		if (mergedParams.containsKey("page_size")) {
			if (parseIntegerOrNull(mergedParams.get("page_size")) == null) {
				throw new ResourceException("每页数量必须在1-500之间");
			}
		}
		if (mergedParams.containsKey("birthday_start")) {
			String v = stringValue(mergedParams.get("birthday_start"));
			if (v != null && !DATE_PATTERN.matcher(v).matches()) {
				throw new ResourceException("生日开始日期格式错误，应为YYYY-MM-DD");
			}
		}
		if (mergedParams.containsKey("birthday_end")) {
			String v = stringValue(mergedParams.get("birthday_end"));
			if (v != null && !DATE_PATTERN.matcher(v).matches()) {
				throw new ResourceException("生日结束日期格式错误，应为YYYY-MM-DD");
			}
		}
		if (mergedParams.containsKey("tag_id")) {
			Object tagId = mergedParams.get("tag_id");
			if (!(tagId instanceof List<?>)) {
				throw new ResourceException("标签ID必须为数组");
			}
			for (Object item : (List<?>) tagId) {
				if (parseIntegerOrNull(item) == null) {
					throw new ResourceException("标签ID数组中的每个元素必须为整数");
				}
			}
		}
		if (mergedParams.containsKey("type")) {
			String type = stringValue(mergedParams.get("type"));
			if (type != null && !"noassign".equals(type) && !"assign".equals(type)) {
				throw new ResourceException("分配状态类型错误，应为noassign或assign");
			}
		}
		if (mergedParams.containsKey("salesperson_code") && !(mergedParams.get("salesperson_code") instanceof String)
				&& mergedParams.get("salesperson_code") != null
				&& !(mergedParams.get("salesperson_code") instanceof Number)) {
			throw new ResourceException("导购编号必须为字符串");
		}
		if (mergedParams.containsKey("store_bn") && !(mergedParams.get("store_bn") instanceof String)
				&& mergedParams.get("store_bn") != null
				&& !(mergedParams.get("store_bn") instanceof Number)) {
			throw new ResourceException("门店编号必须为字符串");
		}
		validateNonNegativeIntIfPresent(mergedParams, "point_start", "积分开始值必须为非负整数");
		validateNonNegativeIntIfPresent(mergedParams, "point_end", "积分结束值必须为非负整数");
		if (mergedParams.containsKey("grade_id")) {
			Integer gradeId = parseIntegerOrNull(mergedParams.get("grade_id"));
			if (gradeId == null || gradeId < 1) {
				throw new ResourceException("等级ID必须为正整数");
			}
		}
		validateNonNegativeIntIfPresent(mergedParams, "buy_start", "购买金额开始值必须为非负整数");
		validateNonNegativeIntIfPresent(mergedParams, "buy_end", "购买金额结束值必须为非负整数");
		if (mergedParams.containsKey("keyword") && !(mergedParams.get("keyword") instanceof String)
				&& mergedParams.get("keyword") != null
				&& !(mergedParams.get("keyword") instanceof Number)) {
			throw new ResourceException("关键词（用于按手机号查询）");
		}
	}

	private static void validateNonNegativeIntIfPresent(
			Map<String, Object> mergedParams, String key, String message) {
		if (!mergedParams.containsKey(key)) {
			return;
		}
		Integer v = parseIntegerOrNull(mergedParams.get(key));
		if (v == null || v < 0) {
			throw new ResourceException(message);
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> executeCustomerUnionids(long companyId, Map<String, Object> mergedParams) {
		String salespersonCode = stringValue(mergedParams.get("salesperson_code"));
		if (isPhpEmptyInline(salespersonCode)) {
			throw new ResourceException("scope为customer时，salesperson_code必填");
		}
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("employee_number", salespersonCode);
			Map<String, Object> root = marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(
					companyId, "basics.salesperson.getBindMemberUnionids", payload);

			log.info(
					"获取导购客户unionid列表：导购端返回结果 company_id={} salesperson_code={} result={}",
					companyId,
					salespersonCode,
					root);

			if (root == null || root.isEmpty()) {
				return List.of();
			}
			Object errcode = root.get("errcode");
			if (errcode == null || !"0".equals(String.valueOf(errcode))) {
				String errorMsg = stringOrDefault(root.get("errmsg"), "获取导购客户unionid列表失败");
				log.warn(
						"获取导购客户unionid列表：接口调用失败 company_id={} salesperson_code={} error={} result={}",
						companyId,
						salespersonCode,
						errorMsg,
						root);
				return List.of();
			}

			List<String> unionids = parseUnionidsFromRoot(root);
			if (unionids.isEmpty()) {
				return List.of();
			}

			List<MembersAssociations> associations =
					membersAssociationsMapper.selectList(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUserType, "wechat")
									.in(MembersAssociations::getUnionid, unionids));
			if (associations == null || associations.isEmpty()) {
				return List.of();
			}

			List<Long> userIdList = new ArrayList<>();
			Map<Long, String> unionidMap = new LinkedHashMap<>();
			for (MembersAssociations assoc : associations) {
				userIdList.add(assoc.getUserId());
				unionidMap.put(assoc.getUserId(), assoc.getUnionid());
			}
			if (userIdList.isEmpty()) {
				return List.of();
			}

			OpenapiMemberListQueryFilter filter = new OpenapiMemberListQueryFilter();
			filter.setCompanyId(companyId);
			filter.setUserIdIn(userIdList);
			applyOptionalCustomerFilters(filter, mergedParams);

			List<Map<String, Object>> memberList = membersMapper.selectMemberListForOpenapiAll(filter);
			List<Long> filteredUserIds = new ArrayList<>();
			for (Map<String, Object> row : memberList) {
				Long uid = longObject(row.get("user_id"));
				if (uid != null && uid > 0L) {
					filteredUserIds.add(uid);
				}
			}

			Integer buyStart = phpNonEmptyInt(mergedParams.get("buy_start"));
			Integer buyEnd = phpNonEmptyInt(mergedParams.get("buy_end"));
			if (buyStart != null || buyEnd != null) {
				List<Long> finalUserIds = new ArrayList<>();
				for (Long userId : filteredUserIds) {
					long totalAmount = lookupSingleUserTotalFee(companyId, userId);
					if (buyStart != null && totalAmount < buyStart) {
						continue;
					}
					if (buyEnd != null && totalAmount > buyEnd) {
						continue;
					}
					finalUserIds.add(userId);
				}
				filteredUserIds = finalUserIds;
			}

			List<String> finalUnionids = new ArrayList<>();
			for (Long userId : filteredUserIds) {
				String unionid = unionidMap.get(userId);
				if (StringUtils.hasText(unionid)) {
					finalUnionids.add(unionid);
				}
			}
			return finalUnionids;
		} catch (OpenapiMemberListCustomerUnionidsFailException e) {
			throw e;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error(
					"获取导购客户unionid列表：系统异常 company_id={} salesperson_code={} error={}",
					companyId,
					salespersonCode,
					e.getMessage(),
					e);
			throw new OpenapiMemberListCustomerUnionidsFailException(
					"获取导购客户unionid列表失败：" + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> executeFpMemberList(long companyId, Map<String, Object> mergedParams) {
		int page = resolvePage(mergedParams);
		int pageSize = resolvePageSize(mergedParams);

		OpenapiMemberListQueryFilter filter = buildFpFilter(companyId, mergedParams);

		String storeBn = stringValue(mergedParams.get("store_bn"));
		if (!isPhpEmptyInline(storeBn)) {
			Optional<Long> distId = distributorByShopCodePort.resolveDistributorIdByShopCode(companyId, storeBn);
			if (distId.isEmpty()) {
				return Map.of("count", 0L, "list", List.of());
			}
			filter.setOpDistributorEq(distId.get());
		}

		long totalCount = membersMapper.countMemberListForOpenapi(filter);
		Page<Map<String, Object>> mpPage = new Page<>(page, pageSize, false);
		List<Map<String, Object>> list = membersMapper.selectMemberListForOpenapi(mpPage, filter);

		List<Long> pageUserIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Long uid = longObject(row.get("user_id"));
			if (uid != null && uid > 0L) {
				pageUserIds.add(uid);
			}
		}

		Map<Long, String> unionidMap = loadUnionidMap(companyId, pageUserIds);
		String languageTag = langueProperties.getDefaultLang();

		for (Map<String, Object> row : list) {
			Long userId = longObject(row.get("user_id"));
			Object unionid = row.get("unionid");
			if (isPhpEmptyInline(unionid) && userId != null && unionidMap.containsKey(userId)) {
				row.put("unionid", unionidMap.get(userId));
			}
			decryptField(row, "mobile");
			decryptField(row, "username");
			row.put("nickname", "");

			if (userId != null && userId > 0L) {
				row.put("total_amount", lookupSingleUserTotalFee(companyId, userId));
				enrichBrowseFields(companyId, userId, row, languageTag);
			} else {
				row.put("total_amount", 0L);
				setEmptyBrowseFields(row);
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("count", totalCount);
		result.put("list", list);
		logMemberListResult(result);
		return result;
	}

	private void logMemberListResult(Map<String, Object> result) {
		try {
			log.info("david-memberList--->{}", objectMapper.writeValueAsString(result));
		} catch (JsonProcessingException e) {
			log.info("david-memberList--->{}", result);
		}
	}

	private OpenapiMemberListQueryFilter buildFpFilter(long companyId, Map<String, Object> mergedParams) {
		OpenapiMemberListQueryFilter filter = new OpenapiMemberListQueryFilter();
		filter.setCompanyId(companyId);

		String birthdayStart = stringValue(mergedParams.get("birthday_start"));
		if (!isPhpEmptyInline(birthdayStart)) {
			filter.setBirthdayStart(birthdayStart);
		}
		String birthdayEnd = stringValue(mergedParams.get("birthday_end"));
		if (!isPhpEmptyInline(birthdayEnd)) {
			filter.setBirthdayEnd(birthdayEnd);
		}

		Object tagIdRaw = mergedParams.get("tag_id");
		if (tagIdRaw instanceof List<?> tagIds && !tagIds.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (Object item : tagIds) {
				Integer id = parseIntegerOrNull(item);
				if (id != null) {
					ids.add(id.longValue());
				}
			}
			if (!ids.isEmpty()) {
				filter.setTagIdIn(ids);
				filter.setJoinRelTags(true);
			}
		}

		String type = stringValue(mergedParams.get("type"));
		if (!isPhpEmptyInline(type)) {
			if ("noassign".equals(type)) {
				filter.setHasFpEq(0);
			} else if ("assign".equals(type)) {
				filter.setHasFpEq(1);
				filter.setIsBecomeFriendEq(0);
			}
		}

		String salespersonCode = stringValue(mergedParams.get("salesperson_code"));
		if (!isPhpEmptyInline(salespersonCode)) {
			filter.setFpSalespersonEq(salespersonCode);
		}

		Integer pointStart = phpNonEmptyInt(mergedParams.get("point_start"));
		Integer pointEnd = phpNonEmptyInt(mergedParams.get("point_end"));
		if (pointStart != null || pointEnd != null) {
			filter.setJoinPointMember(true);
			if (pointStart != null) {
				filter.setPointGte(pointStart);
			}
			if (pointEnd != null) {
				filter.setPointLte(pointEnd);
			}
		}

		Integer gradeId = phpNonEmptyInt(mergedParams.get("grade_id"));
		if (gradeId != null) {
			filter.setGradeIdEq(gradeId.longValue());
		}

		String keyword = stringValue(mergedParams.get("keyword"));
		if (!isPhpEmptyInline(keyword)) {
			filter.setMobileLike(keyword.trim());
		}
		return filter;
	}

	private void applyOptionalCustomerFilters(
			OpenapiMemberListQueryFilter filter, Map<String, Object> mergedParams) {
		Integer gradeId = phpNonEmptyInt(mergedParams.get("grade_id"));
		if (gradeId != null) {
			filter.setGradeIdEq(gradeId.longValue());
		}
		Integer pointStart = phpNonEmptyInt(mergedParams.get("point_start"));
		Integer pointEnd = phpNonEmptyInt(mergedParams.get("point_end"));
		if (pointStart != null || pointEnd != null) {
			filter.setJoinPointMember(true);
			if (pointStart != null) {
				filter.setPointGte(pointStart);
			}
			if (pointEnd != null) {
				filter.setPointLte(pointEnd);
			}
		}
	}

	private Map<Long, String> loadUnionidMap(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		List<MembersAssociations> associations =
				membersAssociationsMapper.selectList(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserType, "wechat")
								.in(MembersAssociations::getUserId, userIds));
		Map<Long, String> unionidMap = new LinkedHashMap<>();
		for (MembersAssociations assoc : associations) {
			unionidMap.put(assoc.getUserId(), assoc.getUnionid());
		}
		return unionidMap;
	}

	@SuppressWarnings("unchecked")
	private void enrichBrowseFields(
			long companyId, long userId, Map<String, Object> row, String languageTag) {
		Map<String, Object> browse =
				memberBrowseHistoryListService.getBrowseHistory(companyId, userId, 1, 1, languageTag);
		long browseCount = longVal(browse.get("total_count"));
		List<Map<String, Object>> browseList = (List<Map<String, Object>>) browse.get("list");
		if (browseCount > 0 && browseList != null && !browseList.isEmpty()) {
			Map<String, Object> latest = browseList.get(0);
			row.put("browse_count", browseCount);
			row.put("browse_time", formatEpoch(latest.get("updated")));
			Object itemData = latest.get("itemData");
			if (itemData instanceof Map<?, ?> itemMap) {
				row.put("browse_item_name", itemMap.get("item_name"));
			} else {
				row.put("browse_item_name", null);
			}
			row.put("browse_item_id", latest.get("item_id"));
		} else {
			setEmptyBrowseFields(row);
		}
	}

	private static void setEmptyBrowseFields(Map<String, Object> row) {
		row.put("browse_count", 0);
		row.put("browse_time", "");
		row.put("browse_item_name", null);
		row.put("browse_item_id", null);
	}

	private void decryptField(Map<String, Object> row, String key) {
		Object raw = row.get(key);
		if (raw == null) {
			return;
		}
		String decrypted = sensitiveFieldEncryptor.decrypt(String.valueOf(raw));
		row.put(key, decrypted);
	}

	private long lookupSingleUserTotalFee(long companyId, long userId) {
		Map<String, Object> orderFilter = new LinkedHashMap<>();
		orderFilter.put("company_id", companyId);
		orderFilter.put("user_id", userId);
		Map<Object, Long> totals = openapiMemberOrderListOrdersPort.sumTotalFeeByUserId(orderFilter);
		Long fee = totals.get(userId);
		if (fee == null) {
			fee = totals.get(String.valueOf(userId));
		}
		return fee != null ? fee : 0L;
	}

	@SuppressWarnings("unchecked")
	private static List<String> parseUnionidsFromRoot(Map<String, Object> root) {
		Object dataObj = root.get("data");
		if (!(dataObj instanceof Map<?, ?> dataMap)) {
			return List.of();
		}
		Object unionidsObj = dataMap.get("unionids");
		if (!(unionidsObj instanceof Collection<?> coll) || coll.isEmpty()) {
			return List.of();
		}
		List<String> unionids = new ArrayList<>();
		for (Object item : coll) {
			if (item != null) {
				String s = String.valueOf(item).trim();
				if (!s.isEmpty()) {
					unionids.add(s);
				}
			}
		}
		return unionids;
	}

	private static int resolvePage(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("page")) {
			return 1;
		}
		Integer page = parseIntegerOrNull(mergedParams.get("page"));
		return page != null && page >= 1 ? page : 1;
	}

	private static int resolvePageSize(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("page_size")) {
			return 10;
		}
		Integer pageSize = parseIntegerOrNull(mergedParams.get("page_size"));
		return pageSize != null && pageSize >= 1 ? pageSize : 10;
	}

	private static Integer phpNonEmptyInt(Object raw) {
		if (isPhpEmptyInline(raw)) {
			return null;
		}
		return parseIntegerOrNull(raw);
	}

	private static boolean isPhpEmptyInline(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static String stringOrDefault(Object raw, String defaultValue) {
		String v = stringValue(raw);
		return v != null ? v : defaultValue;
	}

	private static Integer parseIntegerOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long longObject(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longVal(Object raw) {
		Long v = longObject(raw);
		return v != null ? v : 0L;
	}

	private static String formatEpoch(Object raw) {
		long sec = longVal(raw);
		if (sec <= 0L) {
			return "";
		}
		return DATE_TIME_FMT.format(Instant.ofEpochSecond(sec));
	}
}
