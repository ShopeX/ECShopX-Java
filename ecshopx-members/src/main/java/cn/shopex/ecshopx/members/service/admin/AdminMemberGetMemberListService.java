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

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberVipGradeFilterUserIdsPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.integration.community.AdminMemberListChiefStoresPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoConfigRequestFieldsPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDistributorDisplayNamePort;
import cn.shopex.ecshopx.members.integration.kaquan.AdminMemberListVipGradeRowGetPort;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberListQueryFilter;
import cn.shopex.ecshopx.members.integration.popularize.AdminMemberListPromoterSidePort;
import cn.shopex.ecshopx.members.integration.salesperson.AdminMemberListSalesmanMobileResolvePort;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmMemberListTagClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberGetMemberListService {

	private static final Logger log = LoggerFactory.getLogger(AdminMemberGetMemberListService.class);

	/** Keys expected on each admin list row from members + members_info + open data (null allowed). */
	private static final List<String> ADMIN_MEMBER_LIST_BASE_ROW_KEYS =
			List.of(
					"address",
					"authorizer_appid",
					"avatar",
					"birthday",
					"company_id",
					"created",
					"created_day",
					"created_month",
					"created_year",
					"disabled",
					"edu_background",
					"email",
					"fp_salesperson",
					"grade_id",
					"vip_grade",
					"habbit",
					"has_fp",
					"income",
					"industry",
					"inviter_id",
					"latest_monitor_id",
					"latest_source_id",
					"mobile",
					"mobile_country_code",
					"monitor_id",
					"name",
					"nickname",
					"offline_card_code",
					"open_id",
					"op_distributor",
					"password",
					"reg_distributor",
					"reg_salesperson",
					"region_mobile",
					"remarks",
					"sex",
					"source_from",
					"source_id",
					"third_data",
					"unionid",
					"updated",
					"use_point",
					"user_card_code",
					"user_id",
					"username",
					"wxa_appid");

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final AdminMemberVipGradeFilterUserIdsPort adminMemberVipGradeFilterUserIdsPort;
	private final MemberAccountService memberAccountService;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final AdminMemberListVipGradeRowGetPort adminMemberListVipGradeRowGetPort;
	private final AdminMemberListSalesmanMobileResolvePort adminMemberListSalesmanMobileResolvePort;
	private final AdminMemberListPromoterSidePort adminMemberListPromoterSidePort;
	private final MembersMapper membersMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final AdminMemberListChiefStoresPort adminMemberListChiefStoresPort;
	private final AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort;
	private final AdminMemberGetInfoDistributorDisplayNamePort adminMemberGetInfoDistributorDisplayNamePort;
	private final ShopexCrmMemberListTagClient shopexCrmMemberListTagClient;
	private final ObjectMapper objectMapper;
	private final MembersInfoMapper membersInfoMapper;

	@Value("${common.oem-shuyun:false}")
	private boolean oemShuyun;

	public AdminMemberGetMemberListService(
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			@Qualifier("adminMemberVipGradeFilterUserIdsPortImpl")
					AdminMemberVipGradeFilterUserIdsPort adminMemberVipGradeFilterUserIdsPort,
			MemberAccountService memberAccountService,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			@Qualifier("adminMemberListVipGradeRowGetPortImpl")
					AdminMemberListVipGradeRowGetPort adminMemberListVipGradeRowGetPort,
			@Qualifier("adminMemberListSalesmanMobileResolvePortImpl")
					AdminMemberListSalesmanMobileResolvePort adminMemberListSalesmanMobileResolvePort,
			@Qualifier("adminMemberListPromoterSidePortImpl")
					AdminMemberListPromoterSidePort adminMemberListPromoterSidePort,
			MembersMapper membersMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagsMapper memberTagsMapper,
			AdminMemberListChiefStoresPort adminMemberListChiefStoresPort,
			AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort,
			AdminMemberGetInfoDistributorDisplayNamePort adminMemberGetInfoDistributorDisplayNamePort,
			ShopexCrmMemberListTagClient shopexCrmMemberListTagClient,
			ObjectMapper objectMapper,
			MembersInfoMapper membersInfoMapper) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.adminMemberVipGradeFilterUserIdsPort = adminMemberVipGradeFilterUserIdsPort;
		this.memberAccountService = memberAccountService;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.adminMemberListVipGradeRowGetPort = adminMemberListVipGradeRowGetPort;
		this.adminMemberListSalesmanMobileResolvePort = adminMemberListSalesmanMobileResolvePort;
		this.adminMemberListPromoterSidePort = adminMemberListPromoterSidePort;
		this.membersMapper = membersMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.adminMemberListChiefStoresPort = adminMemberListChiefStoresPort;
		this.adminMemberGetInfoConfigRequestFieldsPort = adminMemberGetInfoConfigRequestFieldsPort;
		this.adminMemberGetInfoDistributorDisplayNamePort = adminMemberGetInfoDistributorDisplayNamePort;
		this.shopexCrmMemberListTagClient = shopexCrmMemberListTagClient;
		this.objectMapper = objectMapper;
		this.membersInfoMapper = membersInfoMapper;
	}

	public Map<String, Object> getMemberList(long companyId, Map<String, Object> mergedParams, HttpServletRequest request) {
		applyPageDefaults(mergedParams);
		validateListParams(mergedParams);
		Map<?, ?> operatorJwtClaims = readOperatorJwtClaims(request);
		if (Long.parseLong(String.valueOf(operatorJwtClaims.get("company_id")).trim()) != companyId) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> internal = new LinkedHashMap<>();
		internal.put("company_id", companyId);
		boolean emptyUserIds = applyDataFilterCore(companyId, mergedParams, internal);
		if (emptyUserIds) {
			applyEmptyUserIdSentinel(internal);
		}
		// Align PHP Members@getMemberList: distributor JWT no longer scopes list by op_distributor.
		applyInviterMobileBranch(companyId, mergedParams, internal);
		applyWechatNicknamePrefilter(mergedParams, internal);
		applySalesmanMobileBranch(companyId, mergedParams, internal);
		applyPromoterMobileBranch(companyId, mergedParams, internal);
		applyEmployeeStoreBindBranch(companyId, mergedParams, internal);
		internal.remove("distributor_id");
		Long distributorIdForChiefBranch = parsePositiveLongOrNull(mergedParams.get("distributor_id"));
		AdminMemberListQueryFilter f = buildAdminMemberListQueryFilter(companyId, mergedParams, internal, distributorIdForChiefBranch, emptyUserIds);
		int page = parsePositiveInt(String.valueOf(mergedParams.get("page")), "分页参数错误");
		int pageSize = parsePositiveInt(String.valueOf(mergedParams.get("pageSize")), "每页显示数量最大100");
		if (pageSize > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		Page<Map<String, Object>> mpPage = new Page<>(page, pageSize);
		// Disable MP auto-COUNT: the list SELECT joins wechat assoc; count SQL must stay lean.
		mpPage.setSearchCount(false);
		List<Map<String, Object>> list = membersMapper.selectMemberListForAdmin(mpPage, f);
		long total = membersMapper.countMemberListForAdmin(f);
		for (Map<String, Object> row : list) {
			MemberAdminListJdbcMapSnakeCase.normalizeTopLevelKeys(row);
		}
		decryptMemberRows(list);
		for (Map<String, Object> row : list) {
			ensureAdminMemberListBaseKeys(row);
		}
		List<Long> pageUserIds =
				list.stream().map(r -> extractUserId(r.get("user_id"))).filter(u -> u != null && u > 0L).toList();
		if (!list.isEmpty()) {
			adminMemberListVipGradeRowGetPort.mergeVipGradeLabelsIntoRows(companyId, list);
			boolean wechatNicknameFilter =
					mergedParams.get("wechat_nickname") != null
							&& StringUtils.hasText(String.valueOf(mergedParams.get("wechat_nickname")).trim());
			Map<Long, Map<String, String>> wxHead =
					wechatNicknameFilter
							? memberAccountService.batchWechatNicknameHeadByUserIds(companyId, pageUserIds)
							: Map.of();
			for (Map<String, Object> row : list) {
				long uid = userIdLong(row.get("user_id"));
				if (wechatNicknameFilter) {
					Map<String, String> head = wxHead.get(uid);
					if (head != null) {
						Object nick = head.get("nickname");
						row.put("nickname", nick == null ? "" : String.valueOf(nick));
					} else {
						row.put("nickname", "");
					}
				} else {
					row.put("nickname", "");
				}
			}
		}
		Map<Long, String> inviterDisplay = buildInviterDisplayMap(companyId, list);
		Object datapassBlockRaw = resolveDatapassBlockRaw(request);
		if (!list.isEmpty() && !pageUserIds.isEmpty()) {
			applyLocalTags(companyId, list, pageUserIds);
			adminMemberListChiefStoresPort.applyChiefFlagsAndStoreInfo(companyId, list, pageUserIds);
			if (oemShuyun) {
				adminMemberListPromoterSidePort.applyOemShuyunExtensions(companyId, list, pageUserIds);
			}
			shopexCrmMemberListTagClient.mergeCrmTagsIntoRows(companyId, list);
			mergeOtherParamsForRequestFieldsEnrich(companyId, list, pageUserIds);
			List<LinkedHashMap<String, Object>> enrichRows = new ArrayList<>(list.size());
			for (Map<String, Object> row : list) {
				enrichRows.add(new LinkedHashMap<>(row));
			}
			adminMemberGetInfoConfigRequestFieldsPort.enrichMemberInfoList(companyId, enrichRows, request);
			for (int i = 0; i < list.size(); i++) {
				Map<String, Object> row = list.get(i);
				row.clear();
				row.putAll(enrichRows.get(i));
				row.remove("other_params");
			}
			applyBindSalespersonBatch(companyId, list, pageUserIds);
			applyDistributorDisplayFields(companyId, list);
		}
		if (!list.isEmpty()) {
			applyHabbitAndMasking(request, list, inviterDisplay);
		}
		int datapassBlock = parseDatapassBlock(request);
		if (datapassBlock == 0) {
			for (Map<String, Object> row : list) {
				Long invId = extractUserId(row.get("inviter_id"));
				row.put("inviter", inviterDisplay.getOrDefault(invId == null ? 0L : invId, "-"));
			}
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("list", list);
		payload.put("total_count", total);
		payload.put("datapass_block", datapassBlockRaw);
		if (oemShuyun && !list.isEmpty()) {
			Map<String, Object> first = list.get(0);
			if (!first.containsKey("promoter_info") || !(first.get("promoter_info") instanceof Map<?, ?>)) {
				first.put("promoter_info", new LinkedHashMap<String, Object>());
			}
		}
		if (!oemShuyun) {
			for (Map<String, Object> row : list) {
				row.remove("promoter_info");
			}
		}
		for (Map<String, Object> row : list) {
			stripAdminListHiddenKeys(row);
		}
		return payload;
	}

	private void mergeOtherParamsForRequestFieldsEnrich(
			long companyId, List<Map<String, Object>> list, List<Long> pageUserIds) {
		if (pageUserIds == null || pageUserIds.isEmpty()) {
			return;
		}
		List<MembersInfo> infos =
				membersInfoMapper.selectList(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.in(MembersInfo::getUserId, pageUserIds));
		Map<Long, String> otherParamsByUser = new LinkedHashMap<>();
		for (MembersInfo info : infos) {
			if (info.getUserId() != null
					&& info.getOtherParams() != null
					&& StringUtils.hasText(info.getOtherParams())) {
				otherParamsByUser.put(info.getUserId(), info.getOtherParams());
			}
		}
		for (Map<String, Object> row : list) {
			Long uid = extractUserId(row.get("user_id"));
			if (uid == null || uid <= 0L) {
				continue;
			}
			String op = otherParamsByUser.get(uid);
			if (op != null) {
				row.put("other_params", op);
			}
		}
	}

	private static void ensureAdminMemberListBaseKeys(Map<String, Object> row) {
		for (String k : ADMIN_MEMBER_LIST_BASE_ROW_KEYS) {
			if (!row.containsKey(k)) {
				row.put(k, null);
			}
		}
	}

	private static void stripAdminListHiddenKeys(Map<String, Object> row) {
		row.remove("headimgurl");
		row.remove("is_had_vip");
		row.remove("is_open");
		row.remove("is_vip");
		row.remove("other_params");
	}

	private static void applyPageDefaults(Map<String, Object> merged) {
		if (merged.get("page") == null || !StringUtils.hasText(String.valueOf(merged.get("page")).trim())) {
			merged.put("page", "1");
		}
		if (merged.get("pageSize") == null || !StringUtils.hasText(String.valueOf(merged.get("pageSize")).trim())) {
			merged.put("pageSize", "20");
		}
	}

	private void validateListParams(Map<String, Object> p) {
		firstIntMin(p.get("page"), 1, "分页参数错误");
		int ps = firstIntMin(p.get("pageSize"), 1, "每页显示数量最大100");
		if (ps > 100) {
			throw new BadRequestException("每页显示数量最大100");
		}
		validateOptionalMaxString(p.get("remarks"), 255, "最多输入255字");
		validateOptionalMaxString(p.get("username"), 50, "最多输入50字");
		validateOptionalMaxString(p.get("name"), 50, "最多输入50字");
		if (p.get("time_start_begin") != null && StringUtils.hasText(String.valueOf(p.get("time_start_begin")).trim())) {
			parseLongStrict(p.get("time_start_begin"), "请填写正确的开始日期");
		}
		if (p.get("time_start_end") != null && StringUtils.hasText(String.valueOf(p.get("time_start_end")).trim())) {
			parseLongStrict(p.get("time_start_end"), "请填写正确的结束日期");
		}
		Object hc = p.get("have_consume");
		if (hc != null && StringUtils.hasText(String.valueOf(hc).trim())) {
			String s = String.valueOf(hc).trim();
			if (!"true".equalsIgnoreCase(s) && !"false".equalsIgnoreCase(s)) {
				throw new BadRequestException("有无购买记录参数不正确");
			}
		}
		if (p.get("distributor_id") != null && StringUtils.hasText(String.valueOf(p.get("distributor_id")).trim())) {
			parseLongMin(p.get("distributor_id"), 1L, "请确认您选择的店铺是否存在");
		}
		if (p.get("shop_id") != null && StringUtils.hasText(String.valueOf(p.get("shop_id")).trim())) {
			parseLongMin(p.get("shop_id"), 1L, "请确认您选择的门店是否存在");
		}
		if (p.get("tag_id") != null && StringUtils.hasText(String.valueOf(p.get("tag_id")).trim())) {
			// presence validated; existence checked by query
		}
		if (p.get("grade_id") != null && StringUtils.hasText(String.valueOf(p.get("grade_id")).trim())) {
			// optional
		}
		Object vg = p.get("vip_grade");
		if (vg != null && StringUtils.hasText(String.valueOf(vg).trim())) {
			String s = String.valueOf(vg).trim();
			if (!"notvip".equalsIgnoreCase(s)
					&& !"svip".equalsIgnoreCase(s)
					&& !"vip".equalsIgnoreCase(s)
					&& !"vip,svip".equalsIgnoreCase(s)
					&& !"svip,vip".equalsIgnoreCase(s)) {
				throw new BadRequestException("付费会员类型参数不正确");
			}
		}
		Object pm = p.get("promoter_mobile");
		if (pm != null && StringUtils.hasText(String.valueOf(pm).trim())) {
			String mobile = String.valueOf(pm).trim();
			if (!mobile.matches("^1[3456789][0-9]{9}$")) {
				throw new BadRequestException("来源推广员请填写正确的手机号");
			}
		}
		validateOptionalString(p.get("employee_number"), "导购编号");
		validateOptionalString(p.get("store_bn"), "门店编号");
		validateOptionalDateYmd(p.get("birthday_start"), "请填写正确的生日开始日期");
		validateOptionalDateYmd(p.get("birthday_end"), "请填写正确的生日结束日期");
	}

	private static void validateOptionalString(Object v, String label) {
		if (v == null) {
			return;
		}
		String.valueOf(v);
	}

	private static void validateOptionalMaxString(Object v, int max, String err) {
		if (v == null) {
			return;
		}
		String s = String.valueOf(v);
		if (s.length() > max) {
			throw new BadRequestException(err);
		}
	}

	private static void validateOptionalDateYmd(Object v, String err) {
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new BadRequestException(err);
		}
	}

	private static int firstIntMin(Object raw, int min, String err) {
		int v = (int) parseLongStrict(raw, err);
		if (v < min) {
			throw new BadRequestException(err);
		}
		return v;
	}

	private static long parseLongStrict(Object raw, String err) {
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			throw new BadRequestException(err);
		}
	}

	private static void parseLongMin(Object raw, long min, String err) {
		long v = parseLongStrict(raw, err);
		if (v < min) {
			throw new BadRequestException(err);
		}
	}

	private static int parsePositiveInt(String raw, String err) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (Exception e) {
			throw new BadRequestException(err);
		}
	}

	private static Map<?, ?> readOperatorJwtClaims(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> operatorJwtClaims)) {
			throw new UnauthorizedException("未登录");
		}
		return operatorJwtClaims;
	}

	private boolean applyDataFilterCore(
			long companyId,
			Map<String, Object> merged,
			LinkedHashMap<String, Object> internal) {
		mapQueryParamsToInternalFilter(companyId, merged, internal);
		if (hasPositiveNumericGradeId(merged)) {
			applyPostUserIdListToInternal(merged, internal);
			return false;
		}
		boolean emptyUserIds = false;
		Object vgRaw = merged.get("vip_grade");
		if (vgRaw != null && StringUtils.hasText(String.valueOf(vgRaw).trim())) {
			String vipExpr = String.valueOf(vgRaw).trim();
			if ("notvip".equalsIgnoreCase(vipExpr)) {
				List<Long> ids =
						new ArrayList<>(
								adminMemberVipGradeFilterUserIdsPort
										.listUserIdsMatchingVipGradeFilter(companyId, "notvip")
										.stream()
										.filter(Objects::nonNull)
										.distinct()
										.toList());
				List<Long> postUserIds = extractUserIdListFromMerged(merged);
				List<Long> resolved;
				if (!ids.isEmpty() && !postUserIds.isEmpty()) {
					resolved = postUserIds.stream().filter(ids::contains).distinct().toList();
					if (resolved.isEmpty()) {
						emptyUserIds = true;
					}
				} else if (!ids.isEmpty()) {
					resolved = new ArrayList<>(ids);
				} else {
					resolved = List.of(0L);
				}
				if (!emptyUserIds) {
					internal.put("user_id|notIn", resolved);
					internal.remove("user_id|in");
					internal.remove("user_id");
				}
			} else {
				List<Long> ids =
						adminMemberVipGradeFilterUserIdsPort.listUserIdsMatchingVipGradeFilter(companyId, vgRaw);
				List<Long> vipUserIds =
						ids == null ? new ArrayList<>() : new ArrayList<>(ids.stream().filter(Objects::nonNull).distinct().toList());
				List<Long> postUserIds = extractUserIdListFromMerged(merged);
				if (!postUserIds.isEmpty()) {
					vipUserIds.retainAll(postUserIds);
				}
				if (vipUserIds.isEmpty()) {
					emptyUserIds = true;
				} else {
					internal.put("user_id|in", vipUserIds);
					internal.remove("user_id|notIn");
					internal.remove("user_id");
				}
			}
		} else {
			applyPostUserIdListToInternal(merged, internal);
		}
		return emptyUserIds;
	}

	private static void applyPostUserIdListToInternal(Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		List<Long> postUserIds = extractUserIdListFromMerged(merged);
		if (!postUserIds.isEmpty()) {
			internal.put("user_id|in", postUserIds);
			internal.remove("user_id|notIn");
			internal.remove("user_id");
		}
	}

	private static boolean hasPositiveNumericGradeId(Map<String, Object> merged) {
		Object gradeRaw = merged.get("grade_id");
		if (gradeRaw == null || !StringUtils.hasText(String.valueOf(gradeRaw).trim())) {
			return false;
		}
		String gs = String.valueOf(gradeRaw).trim();
		if (!gs.chars().allMatch(Character::isDigit)) {
			return false;
		}
		try {
			return Long.parseLong(gs) > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static long parseLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void applyInviterMobileBranch(long companyId, Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		Object raw = merged.get("inviter_mobile");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return;
		}
		String mobile = String.valueOf(raw).trim();
		List<Long> ids = memberAccountService.listUserIdsByCompanyAndMobile(companyId, mobile);
		if (ids.isEmpty()) {
			internal.put("inviter_id", -1L);
		} else {
			internal.put("inviter_id", ids.get(0));
		}
	}

	private void applyWechatNicknamePrefilter(Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		Object raw = merged.get("wechat_nickname");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return;
		}
		internal.put("wechat_nickname", String.valueOf(raw).trim());
	}

	private void applySalesmanMobileBranch(long companyId, Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		Object raw = merged.get("salesman_mobile");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return;
		}
		String mobile = String.valueOf(raw).trim();
		Long bound =
				adminMemberListSalesmanMobileResolvePort.resolveMemberUserIdForSalesmanMobile(companyId, mobile);
		if (bound == null) {
			internal.put("user_id", -1L);
		} else {
			internal.put("user_id", bound);
		}
	}

	private void applyPromoterMobileBranch(long companyId, Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		Object raw = merged.get("promoter_mobile");
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return;
		}
		String mobileEnc = LegacyFixedMobileEncrypt.fixedEncryptMobile(String.valueOf(raw).trim());
		List<Long> promoterUserIds =
				adminMemberListPromoterSidePort.listDownstreamUserIdsByPromoterMobileEnc(companyId, mobileEnc);
		promoterUserIds = promoterUserIds.stream().filter(Objects::nonNull).filter(u -> u != 0L).distinct().sorted().toList();
		if (promoterUserIds.isEmpty()) {
			applyEmptyUserIdSentinel(internal);
			return;
		}
		intersectOrReplaceUserIds(internal, promoterUserIds);
	}

	private void applyEmployeeStoreBindBranch(long companyId, Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		String en = merged.get("employee_number") == null ? "" : String.valueOf(merged.get("employee_number")).trim();
		String sn = merged.get("store_name") == null ? "" : String.valueOf(merged.get("store_name")).trim();
		if (!StringUtils.hasText(en) && !StringUtils.hasText(sn)) {
			return;
		}
		LinkedHashMap<String, Object> dataPayload = new LinkedHashMap<>();
		if (StringUtils.hasText(en)) {
			dataPayload.put("employee_number", en);
		}
		if (StringUtils.hasText(sn)) {
			dataPayload.put("store_bn", sn);
		}
		Map<String, Object> parsed =
				marketingCenterOpenApiSignedFormClient.postReturningParsedData(
						companyId, "basics.salesperson.getBindMembers", dataPayload);
		List<Long> bindIds = parseMemberIdsFromMarketingCenter(parsed);
		if (bindIds.isEmpty()) {
			applyEmptyUserIdSentinel(internal);
			return;
		}
		intersectOrReplaceUserIds(internal, bindIds);
	}

	private static List<Long> parseMemberIdsFromMarketingCenter(Map<String, Object> parsed) {
		if (parsed == null) {
			return List.of();
		}
		Object raw = parsed.get("member_ids");
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		Set<Long> out = new LinkedHashSet<>();
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			long v;
			if (o instanceof Number n) {
				v = n.longValue();
			} else {
				try {
					v = Long.parseLong(String.valueOf(o).trim());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			if (v != 0L) {
				out.add(v);
			}
		}
		return out.stream().sorted().toList();
	}

	private static void intersectOrReplaceUserIds(LinkedHashMap<String, Object> internal, List<Long> rhs) {
		Object scalar = internal.get("user_id");
		Object inObj = internal.get("user_id|in");
		Object notInObj = internal.get("user_id|notIn");
		if (scalar != null) {
			long uid = parseLongOrZero(scalar);
			List<Long> cur = uid == 0L ? List.of() : List.of(uid);
			List<Long> inter = intersectSorted(cur, rhs);
			internal.remove("user_id");
			if (inter.isEmpty()) {
				applyEmptyUserIdSentinel(internal);
			} else {
				internal.put("user_id|in", inter);
			}
			return;
		}
		if (inObj instanceof List<?> curList) {
			List<Long> cur = curList.stream().map(x -> x instanceof Number n ? n.longValue() : parseLongOrZero(x)).toList();
			List<Long> inter = intersectSorted(cur, rhs);
			internal.remove("user_id|in");
			if (inter.isEmpty()) {
				applyEmptyUserIdSentinel(internal);
			} else {
				internal.put("user_id|in", inter);
			}
			return;
		}
		if (notInObj instanceof List<?> notList) {
			Set<Long> notSet =
					notList.stream()
							.map(x -> x instanceof Number n ? n.longValue() : parseLongOrZero(x))
							.collect(Collectors.toSet());
			List<Long> bindFiltered = rhs.stream().filter(u -> !notSet.contains(u)).distinct().sorted().toList();
			internal.remove("user_id|notIn");
			if (bindFiltered.isEmpty()) {
				applyEmptyUserIdSentinel(internal);
			} else {
				internal.put("user_id|in", bindFiltered);
			}
			return;
		}
		internal.put("user_id|in", rhs);
	}

	private static List<Long> intersectSorted(List<Long> a, List<Long> b) {
		Set<Long> bs = new LinkedHashSet<>(b);
		return a.stream().filter(bs::contains).distinct().sorted().toList();
	}

	private static void applyEmptyUserIdSentinel(LinkedHashMap<String, Object> internal) {
		internal.remove("user_id");
		internal.remove("user_id|in");
		internal.remove("user_id|notIn");
		internal.put("user_id|in", List.of(-1L));
	}

	private AdminMemberListQueryFilter buildAdminMemberListQueryFilter(
			long companyId,
			Map<String, Object> merged,
			LinkedHashMap<String, Object> internal,
			Long distributorIdForChiefBranch,
			boolean emptyUserIds) {
		AdminMemberListQueryFilter f = new AdminMemberListQueryFilter();
		f.setCompanyId(companyId);
		if (emptyUserIds) {
			f.setUserIdIn(List.of(-1L));
			return f;
		}
		Object uidIn = internal.get("user_id|in");
		if (uidIn instanceof List<?> l && !l.isEmpty()) {
			f.setUserIdIn(l.stream().map(x -> ((Number) x).longValue()).toList());
		}
		Object uidNot = internal.get("user_id|notIn");
		if (uidNot instanceof List<?> l2 && !l2.isEmpty()) {
			f.setUserIdNotIn(l2.stream().map(x -> ((Number) x).longValue()).toList());
		}
		Object uidEq = internal.get("user_id");
		if (uidEq != null) {
			f.setUserIdEq(parseLongOrZero(uidEq));
		}
		Object inv = internal.get("inviter_id");
		if (inv != null) {
			f.setInviterId(parseLongOrZero(inv));
		}
		Object gradeRaw = merged.get("grade_id");
		if (gradeRaw != null && StringUtils.hasText(String.valueOf(gradeRaw).trim())) {
			String gs = String.valueOf(gradeRaw).trim();
			if (gs.chars().allMatch(Character::isDigit)) {
				try {
					f.setGradeIdEq(Long.parseLong(gs));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		Object mob = merged.get("mobile");
		if (mob != null && StringUtils.hasText(String.valueOf(mob).trim())) {
			f.setMobileEncEq(sensitiveFieldEncryptor.encrypt(String.valueOf(mob).trim()));
		}
		Object remarks = merged.get("remarks");
		if (remarks != null && StringUtils.hasText(String.valueOf(remarks).trim())) {
			f.setRemarksLike(String.valueOf(remarks).trim());
		}
		Object username = merged.get("username");
		if (username != null && StringUtils.hasText(String.valueOf(username).trim())) {
			f.setUsernameLike(sensitiveFieldEncryptor.encrypt(String.valueOf(username).trim()));
		}
		Object name = merged.get("name");
		if (name != null && StringUtils.hasText(String.valueOf(name).trim())) {
			f.setNameLike(String.valueOf(name).trim());
		}
		Object source = merged.get("source");
		if (source != null && StringUtils.hasText(String.valueOf(source).trim())) {
			f.setSourceEq(String.valueOf(source).trim());
		}
		Object wn = internal.get("wechat_nickname");
		if (wn != null && StringUtils.hasText(String.valueOf(wn).trim())) {
			f.setWechatNicknameLike(String.valueOf(wn).trim());
		}
		Object hc = merged.get("have_consume");
		if (hc != null && StringUtils.hasText(String.valueOf(hc).trim())) {
			f.setHaveConsume(String.valueOf(hc).trim().toLowerCase());
		}
		Object bs = merged.get("birthday_start");
		if (bs != null && StringUtils.hasText(String.valueOf(bs).trim())) {
			f.setBirthdayStart(String.valueOf(bs).trim());
		}
		Object be = merged.get("birthday_end");
		if (be != null && StringUtils.hasText(String.valueOf(be).trim())) {
			f.setBirthdayEnd(String.valueOf(be).trim());
		}
		Object tsb = merged.get("time_start_begin");
		if (tsb != null && StringUtils.hasText(String.valueOf(tsb).trim())) {
			f.setTimeStartBegin(parseLongOrZero(tsb));
		}
		Object tse = merged.get("time_start_end");
		if (tse != null && StringUtils.hasText(String.valueOf(tse).trim())) {
			f.setTimeStartEnd(parseLongOrZero(tse));
		}
		Object sbn = merged.get("store_bn");
		if (sbn != null && StringUtils.hasText(String.valueOf(sbn).trim())) {
			f.setStoreBnEq(String.valueOf(sbn).trim());
		}
		f.setDistributorIdForChiefBranch(distributorIdForChiefBranch);
		Object opd = internal.get("op_distributor");
		if (opd != null && StringUtils.hasText(String.valueOf(opd).trim())) {
			long op = parseLongOrZero(opd);
			if (op > 0L) {
				f.setOpDistributorEq(op);
			}
		}
		List<Long> shopIds = normalizeIdList(internal.get("shop_id"));
		if (!shopIds.isEmpty()) {
			f.setShopIds(shopIds);
		}
		Object tagRaw = merged.get("tag_id");
		if (tagRaw != null && StringUtils.hasText(String.valueOf(tagRaw).trim())) {
			String ts = String.valueOf(tagRaw).trim();
			if (ts.contains(",")) {
				List<Long> tids = new ArrayList<>();
				for (String p : ts.split(",")) {
					String x = p.trim();
					if (x.isEmpty()) {
						continue;
					}
					try {
						tids.add(Long.parseLong(x));
					} catch (NumberFormatException ignored) {
					}
				}
				if (!tids.isEmpty()) {
					f.setTagIdIn(tids);
				}
			} else {
				try {
					f.setTagIdEq(Long.parseLong(ts));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return f;
	}

	private void mapQueryParamsToInternalFilter(long companyId, Map<String, Object> merged, LinkedHashMap<String, Object> internal) {
		internal.put("company_id", companyId);
		putIfEffective(merged, "shop_id", internal, "shop_id");
		putIfEffective(merged, "distributor_id", internal, "distributor_id");
		putIfEffective(merged, "user_id", internal, "user_id");
		putIfEffective(merged, "inviter_id", internal, "inviter_id");
		putIfEffective(merged, "user_card_code", internal, "user_card_code");
	}

	private static void putIfEffective(Map<String, Object> merged, String key, LinkedHashMap<String, Object> internal, String dest) {
		if (!merged.containsKey(key)) {
			return;
		}
		Object v = merged.get(key);
		if (v == null) {
			return;
		}
		internal.put(dest, v);
	}

	private static List<Long> extractUserIdListFromMerged(Map<String, Object> merged) {
		Object raw = merged.get("user_id");
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Iterable<?> it && !(raw instanceof String)) {
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			for (String p : s.split(",")) {
				String t = p.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			out.add(n.longValue());
		}
		return out;
	}

	private static List<Long> normalizeIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> l) {
			return l.stream()
					.filter(Objects::nonNull)
					.map(x -> x instanceof Number n ? n.longValue() : parseLongOrZero(x))
					.filter(v -> v != 0L)
					.distinct()
					.sorted()
					.toList();
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v == 0L ? List.of() : List.of(v);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return List.of();
			}
			if (t.contains(",")) {
				List<Long> out = new ArrayList<>();
				for (String p : t.split(",")) {
					try {
						long v = Long.parseLong(p.trim());
						if (v != 0L) {
							out.add(v);
						}
					} catch (NumberFormatException ignored) {
					}
				}
				return out.stream().distinct().sorted().toList();
			}
			try {
				long v = Long.parseLong(t);
				return v == 0L ? List.of() : List.of(v);
			} catch (NumberFormatException e) {
				return List.of();
			}
		}
		return List.of();
	}

	private void decryptMemberRows(List<Map<String, Object>> list) {
		for (Map<String, Object> row : list) {
			Object mob = row.get("mobile");
			if (mob != null) {
				row.put("mobile", sensitiveFieldEncryptor.decrypt(String.valueOf(mob)));
			}
			Object un = row.get("username");
			if (un != null) {
				row.put("username", sensitiveFieldEncryptor.decrypt(String.valueOf(un)));
			}
		}
	}

	private Map<Long, String> buildInviterDisplayMap(long companyId, List<Map<String, Object>> list) {
		Set<Long> inviterIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			Object i = row.get("inviter_id");
			long id = i instanceof Number n ? n.longValue() : parseLongOrZero(i);
			if (id > 0L) {
				inviterIds.add(id);
			}
		}
		if (inviterIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = new ArrayList<>(inviterIds);
		List<MembersContactByUserIdsLookupService.MemberContactRow> rows =
				membersMapper.selectInviterMobileRowsByUserIds(companyId, ids);
		Map<Long, String> out = new LinkedHashMap<>();
		for (MembersContactByUserIdsLookupService.MemberContactRow r : rows) {
			if (r.getUserId() == null || r.getMobileEnc() == null) {
				continue;
			}
			out.put(r.getUserId(), sensitiveFieldEncryptor.decrypt(r.getMobileEnc()));
		}
		return out;
	}

	private void applyLocalTags(long companyId, List<Map<String, Object>> list, List<Long> pageUserIds) {
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.in(MemberRelTags::getUserId, pageUserIds));
		if (rels.isEmpty()) {
			for (Map<String, Object> row : list) {
				row.put("tagList", new ArrayList<Map<String, Object>>());
			}
			return;
		}
		Set<Long> tagIds = rels.stream().map(MemberRelTags::getTagId).filter(Objects::nonNull).collect(Collectors.toSet());
		List<MemberTags> tags =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, tagIds));
		Map<Long, MemberTags> tagById = tags.stream().collect(Collectors.toMap(MemberTags::getTagId, t -> t, (a, b) -> a));
		Map<Long, List<Map<String, Object>>> byUser = new LinkedHashMap<>();
		for (MemberRelTags rel : rels) {
			Long uid = rel.getUserId();
			if (uid == null) {
				continue;
			}
			MemberTags meta = rel.getTagId() == null ? null : tagById.get(rel.getTagId());
			byUser.computeIfAbsent(uid, k -> new ArrayList<>()).add(buildLegacyTagListRow(uid, rel, meta));
		}
		for (Map<String, Object> row : list) {
			long uid = userIdLong(row.get("user_id"));
			row.put("tagList", new ArrayList<>(byUser.getOrDefault(uid, List.of())));
		}
	}

	private void applyBindSalespersonBatch(long companyId, List<Map<String, Object>> list, List<Long> pageUserIds) {
		List<Map<String, Object>> batchData = new ArrayList<>();
		for (Map<String, Object> m : list) {
			Long uid = extractUserId(m.get("user_id"));
			if (uid == null || uid <= 0L) {
				continue;
			}
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			Object uniObj = m.get("unionid");
			if (uniObj != null && StringUtils.hasText(String.valueOf(uniObj))) {
				row.put("unionid", String.valueOf(uniObj).trim());
			} else {
				row.put("external_member_id", String.valueOf(uid));
			}
			batchData.add(row);
		}
		if (batchData.isEmpty()) {
			log.info("[MemberService] 批量查询绑定导购：无有效数据 company_id={} member_count={}", companyId, list.size());
			return;
		}
		try {
			log.info("[MemberService] 批量查询绑定导购：开始请求 company_id={} batch_count={} page_user_ids_count={} request_data={}",
					companyId, batchData.size(), pageUserIds.size(), batchData);
			Map<String, Object> parsed =
					marketingCenterOpenApiSignedFormClient.postReturningParsedDataWithJsonArrayData(
							companyId, "basics.member.getBindSalespersonBatch", batchData);
			Object resultsObj = parsed.get("results");
			LinkedHashMap<String, Object> resultStructure = new LinkedHashMap<>();
			boolean hasResults = resultsObj instanceof List<?> rl && !rl.isEmpty();
			resultStructure.put("has_results", hasResults);
			if (resultsObj instanceof List<?> rl) {
				resultStructure.put("results_count", rl.size());
				if (!rl.isEmpty() && rl.get(0) instanceof Map<?, ?> first) {
					resultStructure.put("first_result_keys", new ArrayList<>(stringKeys(first)));
				} else {
					resultStructure.put("first_result_keys", List.of());
				}
			} else {
				resultStructure.put("results_count", 0);
				resultStructure.put("first_result_keys", List.of());
			}
			log.info("[MemberService] 批量查询绑定导购：返回结果 company_id={} result_structure={}",
					companyId, resultStructure);
			if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
				log.info("[MemberService] 批量查询绑定导购：无返回结果 company_id={}", companyId);
				return;
			}
			int matched = 0;
			for (Object o : results) {
				if (!(o instanceof Map<?, ?> raw)) {
					continue;
				}
				int idx = parseRequestIndex(raw.get("request_index"));
				if (idx < 0 || idx >= list.size()) {
					log.warn("[MemberService] 批量查询绑定导购：无法匹配会员 company_id={} request_index={} member_list_count={}",
							companyId, idx, list.size());
					continue;
				}
				matched++;
				Map<String, Object> target = list.get(idx);
				Object spi = raw.get("salesperson_info");
				if (spi instanceof Map<?, ?> sm) {
					target.put("salesperson_info", shallowStringObjectMap(sm));
				}
			}
			log.info("[MemberService] 批量查询绑定导购：匹配完成 company_id={} matched_count={} total_results={}",
					companyId, matched, results.size());
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log.error("[MemberService] 批量查询绑定导购失败 company_id={} error={} trace={}",
					companyId, e.toString(), sw.toString());
		}
	}

	private static List<String> stringKeys(Map<?, ?> m) {
		List<String> keys = new ArrayList<>();
		for (Object k : m.keySet()) {
			keys.add(String.valueOf(k));
		}
		return keys;
	}

	private static int parseRequestIndex(Object raw) {
		if (raw == null) {
			return -1;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static Map<String, Object> shallowStringObjectMap(Map<?, ?> in) {
		LinkedHashMap<String, Object> o = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : in.entrySet()) {
			o.put(String.valueOf(e.getKey()), e.getValue());
		}
		return o;
	}

	private void applyDistributorDisplayFields(long companyId, List<Map<String, Object>> list) {
		LinkedHashSet<Long> distributorIds = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			long regId = parseLongOrZero(row.get("reg_distributor"));
			if (regId > 0L) {
				distributorIds.add(regId);
			}
			long opId = parseLongOrZero(row.get("op_distributor"));
			if (opId > 0L) {
				distributorIds.add(opId);
			}
		}
		Map<Long, String> nameById =
				distributorIds.isEmpty()
						? Map.of()
						: adminMemberGetInfoDistributorDisplayNamePort.resolveBatch(companyId, distributorIds);
		for (Map<String, Object> row : list) {
			long regId = parseLongOrZero(row.get("reg_distributor"));
			row.put("reg_distributor_name", regId > 0L ? nameById.getOrDefault(regId, "") : "");
			long opId = parseLongOrZero(row.get("op_distributor"));
			row.put("maintain_store", opId > 0L ? nameById.getOrDefault(opId, "") : "");
		}
	}

	private void applyHabbitAndMasking(
			HttpServletRequest request, List<Map<String, Object>> list, Map<Long, String> inviterDisplay) {
		int block = parseDatapassBlock(request);
		for (Map<String, Object> row : list) {
			Object habbit = row.get("habbit");
			row.put("habbit", parseHabbitValue(habbit));
			if (block != 0) {
				Object mobile = row.get("mobile");
				if (mobile != null) {
					row.put("mobile", DataMasking.maskMobileIfBlocked(String.valueOf(mobile), 1));
				}
				Object username = row.get("username");
				if (username != null) {
					row.put("username", DataMasking.maskTruenameIfBlocked(String.valueOf(username), 1));
				}
				Object invKey = row.get("inviter_id");
				long invId = invKey instanceof Number n ? n.longValue() : parseLongOrZero(invKey);
				String invText = inviterDisplay.getOrDefault(invId, "-");
				if (!"-".equals(invText)) {
					invText = DataMasking.maskMobileIfBlocked(invText, 1);
				}
				row.put("inviter", invText);
			}
		}
	}

	private Object parseHabbitValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Map<?, ?> m) {
			return shallowStringObjectMap(m);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return new LinkedHashMap<>();
			}
			try {
				JsonNode n = objectMapper.readTree(t);
				if (n.isNull()) {
					return new LinkedHashMap<>();
				}
				if (n.isObject()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> cast = objectMapper.convertValue(n, Map.class);
					return new LinkedHashMap<>(cast);
				}
				if (n.isArray()) {
					return objectMapper.convertValue(n, List.class);
				}
				return new LinkedHashMap<>();
			} catch (JsonProcessingException e) {
				return new LinkedHashMap<>();
			}
		}
		return null;
	}

	private static LinkedHashMap<String, Object> buildLegacyTagListRow(long userId, MemberRelTags rel, MemberTags meta) {
		LinkedHashMap<String, Object> tagRow = new LinkedHashMap<>();
		tagRow.put("user_id", userId);
		tagRow.put("tag_id", rel.getTagId());
		if (rel.getCompanyId() != null) {
			tagRow.put("company_id", rel.getCompanyId());
		} else if (meta != null && meta.getCompanyId() != null) {
			tagRow.put("company_id", meta.getCompanyId());
		} else {
			tagRow.put("company_id", null);
		}
		if (meta != null) {
			tagRow.put("tag_name", meta.getTagName());
			tagRow.put("description", meta.getDescription());
			tagRow.put("tag_icon", meta.getTagIcon());
			tagRow.put("saleman_id", meta.getSalemanId());
			tagRow.put("tag_status", meta.getTagStatus());
			tagRow.put("category_id", meta.getCategoryId());
			tagRow.put("self_tag_count", meta.getSelfTagCount());
			tagRow.put("tag_color", meta.getTagColor());
			tagRow.put("font_color", meta.getFontColor());
			tagRow.put("distributor_id", meta.getDistributorId());
			tagRow.put("created", meta.getCreated());
			tagRow.put("updated", meta.getUpdated());
			tagRow.put("source", meta.getSource());
			tagRow.put("wechat_tag_id", meta.getWechatTagId());
		} else {
			tagRow.put("tag_name", null);
			tagRow.put("description", null);
			tagRow.put("tag_icon", null);
			tagRow.put("saleman_id", null);
			tagRow.put("tag_status", null);
			tagRow.put("category_id", null);
			tagRow.put("self_tag_count", null);
			tagRow.put("tag_color", null);
			tagRow.put("font_color", null);
			tagRow.put("distributor_id", null);
			tagRow.put("created", null);
			tagRow.put("updated", null);
			tagRow.put("source", null);
			tagRow.put("wechat_tag_id", null);
		}
		return tagRow;
	}

	private static int parseDatapassBlock(HttpServletRequest request) {
		String header = request.getHeader("x-datapass-block");
		if (StringUtils.hasText(header)
				&& !"0".equals(header.trim())
				&& !"false".equalsIgnoreCase(header.trim())) {
			return 1;
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Number n && n.intValue() != 0) {
			return 1;
		}
		if (Boolean.TRUE.equals(attr)) {
			return 1;
		}
		if (attr != null) {
			String t = attr.toString().trim();
			if (!t.isEmpty() && !"0".equals(t) && !"false".equalsIgnoreCase(t)) {
				return 1;
			}
		}
		String p = request.getParameter("x-datapass-block");
		if (p == null || p.trim().isEmpty() || "0".equals(p.trim()) || "false".equalsIgnoreCase(p.trim())) {
			return 0;
		}
		return 1;
	}

	private static Object resolveDatapassBlockRaw(HttpServletRequest request) {
		String header = request.getHeader("x-datapass-block");
		if (StringUtils.hasText(header)) {
			return header.trim();
		}
		String p = request.getParameter("x-datapass-block");
		if (p != null && !p.trim().isEmpty()) {
			return p.trim();
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr != null) {
			return attr;
		}
		return Integer.valueOf(0);
	}

	private static Long extractUserId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long userIdLong(Object v) {
		Long x = extractUserId(v);
		return x == null ? 0L : x.longValue();
	}

	private static Long parsePositiveLongOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
