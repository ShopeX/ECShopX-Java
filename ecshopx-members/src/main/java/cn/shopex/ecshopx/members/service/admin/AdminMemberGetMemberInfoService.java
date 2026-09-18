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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoConfigRequestFieldsPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDepositTotalPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDistributorDisplayNamePort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoKaquanPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoPointPayloadPort;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberGetMemberInfoService {

	private static final Pattern INTEGER_USER_ID = Pattern.compile("-?(0|[1-9]\\d*)");

	private final MemberAccountService memberAccountService;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort;
	private final AdminMemberGetInfoKaquanPort adminMemberGetInfoKaquanPort;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final AdminMemberGetInfoDistributorDisplayNamePort adminMemberGetInfoDistributorDisplayNamePort;
	private final AdminMemberGetInfoDepositTotalPort adminMemberGetInfoDepositTotalPort;
	private final AdminMemberGetInfoPointPayloadPort adminMemberGetInfoPointPayloadPort;
	private final LangueProperties langueProperties;

	public AdminMemberGetMemberInfoService(
			MemberAccountService memberAccountService,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagsMapper memberTagsMapper,
			AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort,
			AdminMemberGetInfoKaquanPort adminMemberGetInfoKaquanPort,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			AdminMemberGetInfoDistributorDisplayNamePort adminMemberGetInfoDistributorDisplayNamePort,
			AdminMemberGetInfoDepositTotalPort adminMemberGetInfoDepositTotalPort,
			AdminMemberGetInfoPointPayloadPort adminMemberGetInfoPointPayloadPort,
			LangueProperties langueProperties) {
		this.memberAccountService = memberAccountService;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.adminMemberGetInfoConfigRequestFieldsPort = adminMemberGetInfoConfigRequestFieldsPort;
		this.adminMemberGetInfoKaquanPort = adminMemberGetInfoKaquanPort;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.adminMemberGetInfoDistributorDisplayNamePort = adminMemberGetInfoDistributorDisplayNamePort;
		this.adminMemberGetInfoDepositTotalPort = adminMemberGetInfoDepositTotalPort;
		this.adminMemberGetInfoPointPayloadPort = adminMemberGetInfoPointPayloadPort;
		this.langueProperties = langueProperties;
	}

	public Object getMemberInfo(long companyId, Map<String, Object> mergedParams, HttpServletRequest request) {
		if (isUnsetMemberLookupKey(mergedParams.get("user_id"))
				&& isUnsetMemberLookupKey(mergedParams.get("mobile"))) {
			LinkedHashMap<String, Object> placeholder = new LinkedHashMap<>();
			placeholder.put("username", "无");
			placeholder.put("mobile", "无");
			placeholder.put("gradeInfo", "");
			return placeholder;
		}
		if (!isUnsetMemberLookupKey(mergedParams.get("user_id"))
				&& resolveMemberQueryUserId(mergedParams.get("user_id")).isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if (!isUnsetMemberLookupKey(mergedParams.get("mobile"))) {
			filter.put("mobile", String.valueOf(mergedParams.get("mobile")).trim());
		}
		resolveMemberQueryUserId(mergedParams.get("user_id")).ifPresent(uid -> filter.put("user_id", uid));
		Map<String, Object> row = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		if (row == null || row.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> lh = new LinkedHashMap<>(row);
		adminMemberGetInfoConfigRequestFieldsPort.enrichMemberInfo(companyId, lh, request);
		long uid = extractUserId(lh.get("user_id"));
		applyTagList(companyId, uid, lh);
		String lang = RequestLangTag.current(langueProperties);
		lh.put("_acceptLanguageForGrade", lang);
		adminMemberGetInfoKaquanPort.applyMemberCardSnapshot(companyId, lh);
		adminMemberGetInfoKaquanPort.applyGradeInfo(companyId, lh);
		adminMemberGetInfoKaquanPort.applyVipGrade(companyId, lh);
		adminMemberGetInfoKaquanPort.applyCouponNum(companyId, lh);
		applyWechatUserInfo(companyId, uid, lh);
		applyMarketingCenter(companyId, lh);
		applyRegDistributorName(companyId, lh);
		applyDeposit(companyId, uid, lh);
		lh.put("point", extractPointScalar(adminMemberGetInfoPointPayloadPort.readPoint(companyId, uid)));
		applyDatapassMasking(request, lh);
		normalizeOtherParamsJsonStringsForThisEndpoint(lh);
		return lh;
	}

	private static long extractPointScalar(Map<String, Object> pointPayload) {
		if (pointPayload == null) {
			return 0L;
		}
		Object pv = pointPayload.get("point");
		if (pv instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	/**
	 * Echoes stored JSON literals as structured values for admin member detail responses.
	 */
	private static void normalizeOtherParamsJsonStringsForThisEndpoint(LinkedHashMap<String, Object> result) {
		Object op = result.get("other_params");
		if (!(op instanceof String raw)) {
			return;
		}
		String t = raw.trim();
		if ("[]".equals(t)) {
			result.put("other_params", Collections.emptyList());
			return;
		}
		if ("{}".equals(t)) {
			result.put("other_params", Collections.emptyMap());
		}
	}

	private void applyDatapassMasking(HttpServletRequest request, LinkedHashMap<String, Object> result) {
		int datapassBlock = parseDatapassBlock(request);
		if (datapassBlock == 0) {
			return;
		}
		Object mobile = result.get("mobile");
		if (mobile != null) {
			result.put(
					"mobile",
					DataMasking.maskMobileIfBlocked(String.valueOf(mobile), 1));
		}
		Object username = result.get("username");
		if (username != null) {
			result.put(
					"username",
					DataMasking.maskTruenameIfBlocked(String.valueOf(username), 1));
		}
		String birthday = Objects.toString(result.get("birthday"), "");
		result.put("birthday", DataMasking.maskBirthday(birthday));
		String address = Objects.toString(result.get("address"), "");
		result.put("address", DataMasking.maskDetailedAddress(address));
		Object rawSex = result.get("sex");
		if ((rawSex instanceof Number n && n.longValue() == 0L)
				|| Objects.toString(rawSex, "").trim().equals("0")) {
			result.put("sex", "-");
		} else {
			String t = Objects.toString(rawSex, "");
			result.put("sex", t.isBlank() ? t : DataMasking.maskSex(t));
		}
	}

	private void applyDeposit(long companyId, long uid, LinkedHashMap<String, Object> result) {
		long fen = adminMemberGetInfoDepositTotalPort.readTotalFen(companyId, uid);
		int depositJson;
		if (fen < 0L) {
			depositJson = 0;
		} else if (fen > Integer.MAX_VALUE) {
			depositJson = Integer.MAX_VALUE;
		} else {
			depositJson = (int) fen;
		}
		result.put("deposit", Integer.valueOf(depositJson));
	}

	private void applyRegDistributorName(long companyId, LinkedHashMap<String, Object> result) {
		Object rd = result.get("reg_distributor");
		long distributorId = 0L;
		if (rd instanceof Number n) {
			distributorId = n.longValue();
		} else if (rd != null) {
			try {
				distributorId = Long.parseLong(String.valueOf(rd).trim());
			} catch (NumberFormatException e) {
				distributorId = 0L;
			}
		}
		if (distributorId > 0L) {
			result.put(
					"reg_distributor",
					adminMemberGetInfoDistributorDisplayNamePort.resolve(companyId, distributorId));
		} else {
			result.put("reg_distributor", "");
		}
	}

	private void applyMarketingCenter(long companyId, LinkedHashMap<String, Object> result) {
		String unionid = resolveUnionidForMarketingCenter(result);
		if (isUnsetUnionidToken(unionid)) {
			applyEmptySalespersonAndStore(result);
			return;
		}
		try {
			Object ext = result.get("user_id");
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			payload.put("external_member_id", ext);
			Map<String, Object> data =
					marketingCenterOpenApiSignedFormClient.postReturningParsedData(
							companyId, "basics.member.getBindSalesperson", payload);
			if (data == null || data.isEmpty()) {
				applyEmptySalespersonAndStore(result);
				return;
			}
			LinkedHashMap<String, Object> salesperson = new LinkedHashMap<>();
			for (Map.Entry<String, Object> e : data.entrySet()) {
				if (e.getKey() != null) {
					salesperson.put(e.getKey(), e.getValue());
				}
			}
			normalizeSalespersonBranchC(salesperson);
			result.put("salesperson_info", salesperson);
			LinkedHashMap<String, Object> store = new LinkedHashMap<>();
			putStoreTwoKeys(store, data, "store_bn");
			putStoreTwoKeys(store, data, "store_name");
			result.put("store_info", store);
		} catch (Exception e) {
			applyEmptySalespersonAndStore(result);
		}
	}

	private static void putStoreTwoKeys(LinkedHashMap<String, Object> store, Map<String, Object> data, String key) {
		Object v = data.get(key);
		store.put(key, v == null ? "" : String.valueOf(v));
	}

	private static void normalizeSalespersonBranchC(LinkedHashMap<String, Object> m) {
		normalizeLongKey(m, "salesperson_id");
		normalizeStringKey(m, "work_userid");
		normalizeStringKey(m, "member_id");
		normalizeIntegerKey(m, "external_member_type");
		normalizeStringKey(m, "external_userid");
		normalizeStringKey(m, "suite_external_userid");
		normalizeStringKey(m, "unionid");
		normalizeIntegerKey(m, "member_status");
		normalizeIntegerKey(m, "bind_status");
		normalizeLongKey(m, "bind_time");
		normalizeLongKey(m, "bind_cancel_time");
		normalizeIntegerKey(m, "friend_status");
		normalizeLongKey(m, "become_friend_time");
		normalizeStringKey(m, "employee_number");
		normalizeStringKey(m, "store_bn");
		normalizeStringKey(m, "store_name");
	}

	private static void normalizeLongKey(LinkedHashMap<String, Object> m, String key) {
		if (!m.containsKey(key)) {
			return;
		}
		Object v = m.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof Number n) {
			m.put(key, n.longValue());
		}
	}

	private static void normalizeIntegerKey(LinkedHashMap<String, Object> m, String key) {
		if (!m.containsKey(key)) {
			return;
		}
		Object v = m.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof Number n) {
			m.put(key, n.intValue());
		}
	}

	private static void normalizeStringKey(LinkedHashMap<String, Object> m, String key) {
		if (!m.containsKey(key)) {
			return;
		}
		Object v = m.get(key);
		if (v == null) {
			m.put(key, null);
			return;
		}
		if (v instanceof String s) {
			m.put(key, s);
		} else {
			m.put(key, String.valueOf(v));
		}
	}

	private static void applyEmptySalespersonAndStore(Map<String, Object> result) {
		result.put("salesperson_info", Collections.emptyList());
		LinkedHashMap<String, Object> store = new LinkedHashMap<>();
		store.put("store_bn", "");
		store.put("store_name", "");
		result.put("store_info", store);
	}

	private static String resolveUnionidForMarketingCenter(Map<String, Object> result) {
		Object wx = result.get("wechatUserInfo");
		String unionidForMc = "";
		if (wx instanceof Map<?, ?> m) {
			Object u = m.get("unionid");
			if (u instanceof String s) {
				unionidForMc = s;
			} else if (u != null) {
				unionidForMc = String.valueOf(u);
			}
		}
		return unionidForMc;
	}

	private static boolean isUnsetUnionidToken(String unionidForMc) {
		if (unionidForMc == null) {
			return true;
		}
		String t = unionidForMc.trim();
		return t.isEmpty() || "0".equals(t);
	}

	private void applyWechatUserInfo(long companyId, long uid, LinkedHashMap<String, Object> result) {
		Map<String, Object> assoc = memberAccountService.getMembersAssociationByUserId(companyId, "wechat", uid);
		if (assoc == null || assoc.isEmpty()) {
			result.put("wechatUserInfo", Collections.emptyList());
			return;
		}
		LinkedHashMap<String, Object> wxFilter = new LinkedHashMap<>();
		wxFilter.put("company_id", companyId);
		Object u = assoc.get("unionid");
		if (u != null) {
			wxFilter.put("unionid", String.valueOf(u));
		}
		Map<String, Object> wxMap = memberAccountService.getWechatSimpleUser(wxFilter);
		if (wxMap != null && !wxMap.isEmpty()) {
			LinkedHashMap<String, Object> wxOut = new LinkedHashMap<>(wxMap);
			wxOut.put("user_id", String.valueOf(uid));
			result.put("wechatUserInfo", wxOut);
		} else {
			result.put("wechatUserInfo", Collections.emptyList());
		}
	}

	private void applyTagList(long companyId, long uid, LinkedHashMap<String, Object> result) {
		List<MemberRelTags> relRows =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>().eq(MemberRelTags::getUserId, uid));
		LinkedHashSet<Long> orderedTagIds = new LinkedHashSet<>();
		for (MemberRelTags rel : relRows) {
			if (rel.getTagId() != null) {
				orderedTagIds.add(rel.getTagId());
			}
		}
		if (orderedTagIds.isEmpty()) {
			result.put("tagList", Collections.emptyList());
			return;
		}
		List<MemberTags> tagEntities =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, orderedTagIds)
								.orderByDesc(MemberTags::getCreated));
		List<Map<String, Object>> tagList = new ArrayList<>();
		for (MemberTags t : tagEntities) {
			if (t.getTagId() != null) {
				tagList.add(tagRowToApiMap(t));
			}
		}
		result.put("tagList", tagList);
	}

	private static LinkedHashMap<String, Object> tagRowToApiMap(MemberTags t) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("tag_id", t.getTagId());
		m.put("company_id", t.getCompanyId());
		m.put("tag_name", t.getTagName());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("saleman_id", t.getSalemanId());
		m.put("tag_status", t.getTagStatus());
		m.put("category_id", t.getCategoryId());
		m.put("self_tag_count", t.getSelfTagCount());
		m.put("tag_color", t.getTagColor());
		m.put("font_color", t.getFontColor());
		m.put("distributor_id", t.getDistributorId());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		return m;
	}

	private static long extractUserId(Object userIdObj) {
		if (userIdObj instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static int parseDatapassBlock(HttpServletRequest request) {
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

	private static boolean isUnsetMemberLookupKey(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof Boolean b && !b.booleanValue()) {
			return true;
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		return false;
	}

	private static OptionalLong resolveMemberQueryUserId(Object raw) {
		if (isUnsetMemberLookupKey(raw)) {
			return OptionalLong.empty();
		}
		if (raw instanceof Number n) {
			if (n.doubleValue() == 0.0) {
				return OptionalLong.empty();
			}
			if ((n instanceof Double || n instanceof Float) && n.doubleValue() != Math.rint(n.doubleValue())) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(n.longValue());
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (!INTEGER_USER_ID.matcher(t).matches()) {
				return OptionalLong.empty();
			}
			try {
				return OptionalLong.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}
		return OptionalLong.empty();
	}
}
