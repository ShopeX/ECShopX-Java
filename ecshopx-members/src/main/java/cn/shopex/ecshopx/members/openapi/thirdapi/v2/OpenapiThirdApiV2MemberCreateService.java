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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.common.salesperson.port.OpenapiSalespersonCompleteNewUserPort;
import cn.shopex.ecshopx.common.salesperson.port.OpenapiSalespersonMemberNumIncreasePort;
import cn.shopex.ecshopx.common.salesperson.port.OpenapiShopSalespersonByMobilePort;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.MemberUserCardCodeAllocateService;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRelLogs;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelLogsMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCreateService {

	private static final String PASSWORD_POOL =
			"QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";
	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final Set<String> SOURCE_FROM_ALLOWED = Set.of("default", "openapi");
	private static final Set<Integer> EDU_BACKGROUND_KEYS = Set.of(0, 1, 2, 3, 4);
	private static final Set<Integer> INCOME_KEYS = Set.of(0, 1, 2, 3, 4);
	private static final Set<Integer> INDUSTRY_KEYS =
			Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	private static final Set<Integer> SEX_SERVICE_KEYS = Set.of(0, 1, 2);
	private static final ObjectMapper JSON = new ObjectMapper();

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MemberUserCardCodeAllocateService memberUserCardCodeAllocateService;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final MemberAccountService memberAccountService;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatRelLogsMapper workWechatRelLogsMapper;
	private final OpenapiShopSalespersonByMobilePort openapiShopSalespersonByMobilePort;
	private final OpenapiSalespersonCompleteNewUserPort openapiSalespersonCompleteNewUserPort;
	private final OpenapiSalespersonMemberNumIncreasePort openapiSalespersonMemberNumIncreasePort;

	public OpenapiThirdApiV2MemberCreateService(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MemberTagsMapper memberTagsMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			MemberUserCardCodeAllocateService memberUserCardCodeAllocateService,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			MemberAccountService memberAccountService,
			WorkWechatRelMapper workWechatRelMapper,
			WorkWechatRelLogsMapper workWechatRelLogsMapper,
			OpenapiShopSalespersonByMobilePort openapiShopSalespersonByMobilePort,
			OpenapiSalespersonCompleteNewUserPort openapiSalespersonCompleteNewUserPort,
			OpenapiSalespersonMemberNumIncreasePort openapiSalespersonMemberNumIncreasePort) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.memberUserCardCodeAllocateService = memberUserCardCodeAllocateService;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.memberAccountService = memberAccountService;
		this.workWechatRelMapper = workWechatRelMapper;
		this.workWechatRelLogsMapper = workWechatRelLogsMapper;
		this.openapiShopSalespersonByMobilePort = openapiShopSalespersonByMobilePort;
		this.openapiSalespersonCompleteNewUserPort = openapiSalespersonCompleteNewUserPort;
		this.openapiSalespersonMemberNumIncreasePort = openapiSalespersonMemberNumIncreasePort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiCreateDetail(long companyId, Map<String, Object> rawParams) {
		try {
			CreateDetailParams params = validateAndNormalize(companyId, rawParams);
			Members member = executeCreateDetailChain(companyId, params);
			return toUserDataMap(member);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	public Map<String, Object> validateDatumAndToPhpFormData(long companyId, Map<String, Object> datum) {
		CreateDetailParams params = validateAndNormalize(companyId, datum);
		return toPhpFormData(params);
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiCreateDetailFromPhpFormData(long companyId, Map<String, Object> formData) {
		try {
			CreateDetailParams params = createDetailParamsFromPhpFormData(companyId, formData);
			executeCreateDetailChain(companyId, params);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private Members executeCreateDetailChain(long companyId, CreateDetailParams params) {
		Members member = createMemberMain(companyId, params);
		long userId = member.getUserId() == null ? 0L : member.getUserId();
		if (userId <= 0L) {
			throw v2Fail(OpenapiErrorCode.MEMBER_ERROR, "会员错误");
		}
		params.userId = userId;
		params.companyId = companyId;
		createMemberInfo(params);
		createMemberTags(companyId, params);
		if (StringUtils.hasText(params.salespersonMobile)) {
			bindSalesperson(companyId, params);
		}
		if (StringUtils.hasText(params.unionid)) {
			insertMembersAssociation(companyId, userId, params.unionid);
		}
		Members reloaded = membersMapper.selectById(userId);
		if (reloaded == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_ERROR, "会员错误");
		}
		return reloaded;
	}

	private static Map<String, Object> toPhpFormData(CreateDetailParams params) {
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("mobile", params.mobile);
		form.put("source_from", params.sourceFrom);
		form.put("inviter_mobile", params.inviterMobile);
		form.put("salesperson_mobile", params.salespersonMobile);
		form.put("union_id", params.unionid);
		form.put("status", params.status);
		form.put("tag_name", params.tagNames == null ? List.of() : new ArrayList<>(params.tagNames));
		form.put("tag_id", List.of());
		form.put("card_code", params.cardCode);
		form.put("grade_id", params.gradeId);
		form.put("username", params.username);
		form.put("avatar", params.avatar);
		form.put("sex", String.valueOf(params.sex));
		form.put("birthday", params.birthday);
		form.put("habbit", params.habbit == null ? List.of() : new ArrayList<>(params.habbit));
		form.put("edu_background", params.eduBackground);
		form.put("income", params.income);
		form.put("industry", params.industry);
		form.put("email", params.email);
		form.put("address", params.address);
		form.put("remarks", params.remarks);
		return form;
	}

	private static CreateDetailParams createDetailParamsFromPhpFormData(
			long companyId, Map<String, Object> formData) {
		Map<String, Object> in = formData == null ? Map.of() : formData;

		CreateDetailParams params = new CreateDetailParams();
		params.mobile = defaultString(stringValue(in.get("mobile")));
		params.sourceFrom = defaultString(stringValue(in.get("source_from")));
		if (!StringUtils.hasText(params.sourceFrom)) {
			params.sourceFrom = "openapi";
		}
		params.inviterMobile = defaultString(stringValue(in.get("inviter_mobile")));
		params.salespersonMobile = defaultString(stringValue(in.get("salesperson_mobile")));
		params.unionid = defaultString(stringValue(in.get("union_id")));
		params.status = parseStatusFromPhpForm(in.get("status"));
		params.tagNames = readStringList(in.get("tag_name"));
		params.tagIds = List.of();
		params.cardCode = defaultString(stringValue(in.get("card_code")));
		params.gradeId = parseGradeIdFromPhpForm(in.get("grade_id"));
		params.username = defaultString(stringValue(in.get("username")));
		params.avatar = defaultString(stringValue(in.get("avatar")));
		params.sex = parseSexFromPhpForm(in.get("sex"));
		params.birthday = defaultString(stringValue(in.get("birthday")));
		params.habbit = readHabbitList(in.get("habbit"));
		params.eduBackground = parseIntFromPhpForm(in.get("edu_background"), 4);
		params.income = parseIntFromPhpForm(in.get("income"), 4);
		params.industry = parseIntFromPhpForm(in.get("industry"), 12);
		params.email = defaultString(stringValue(in.get("email")));
		params.address = defaultString(stringValue(in.get("address")));
		params.remarks = defaultString(stringValue(in.get("remarks")));
		params.companyId = companyId;
		return params;
	}

	private static int parseStatusFromPhpForm(Object raw) {
		if (raw == null) {
			return 1;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseGradeIdFromPhpForm(Object raw) {
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

	private static int parseSexFromPhpForm(Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseIntFromPhpForm(Object raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static List<String> readStringList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object item : list) {
				if (item != null) {
					out.add(String.valueOf(item));
				}
			}
			return out;
		}
		return List.of();
	}

	private static List<Map<String, Object>> readHabbitList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				Map<String, Object> copy = new HashMap<>();
				copy.put("name", map.get("name"));
				copy.put("ischecked", map.get("ischecked"));
				out.add(copy);
			}
		}
		return out;
	}

	private CreateDetailParams validateAndNormalize(long companyId, Map<String, Object> rawParams) {
		Map<String, Object> in = rawParams == null ? Map.of() : rawParams;

		List<String> tagNames = null;
		if (in.containsKey("tag_names")) {
			Object tagNamesRaw = in.get("tag_names");
			if (tagNamesRaw instanceof String s) {
				tagNames = splitCommaTrim(s);
			}
		}

		Object habbitRaw = in.get("habbit");
		if (habbitRaw instanceof String s && StringUtils.hasText(s)) {
			try {
				habbitRaw = JSON.readValue(s, List.class);
			} catch (JsonProcessingException e) {
				throw missingParams("爱好填写错误");
			}
		}

		String mobile = stringValue(in.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw missingParams("手机号必填");
		}

		String sourceFrom = stringValue(in.get("source_from"));
		if (!StringUtils.hasText(sourceFrom)) {
			sourceFrom = "openapi";
		} else if (!SOURCE_FROM_ALLOWED.contains(sourceFrom.trim())) {
			throw missingParams("来源渠道填写错误");
		}

		String inviterMobile = defaultString(stringValue(in.get("inviter_mobile")));
		if (StringUtils.hasText(inviterMobile) && !MOBILE_PATTERN.matcher(inviterMobile.trim()).matches()) {
			throw missingParams("推荐人手机号填写错误");
		}

		String salespersonMobile = defaultString(stringValue(in.get("salesperson_mobile")));
		if (StringUtils.hasText(salespersonMobile)
				&& !MOBILE_PATTERN.matcher(salespersonMobile.trim()).matches()) {
			throw missingParams("绑定导购手机号填写错误");
		}

		Object unionIdRaw = in.get("union_id");
		if (unionIdRaw != null && !(unionIdRaw instanceof String)) {
			throw missingParams("微信unionid填写错误");
		}
		String unionId = defaultString(stringValue(unionIdRaw));

		int status = parseStatus(in.get("status"));
		int gradeId = parseGradeId(in.get("grade_id"));
		int sex = parseSex(in.get("sex"));

		String birthday = defaultString(stringValue(in.get("birthday")));
		if (StringUtils.hasText(birthday) && !isValidBirthdayInput(birthday)) {
			throw missingParams("生日填写错误");
		}

		if (habbitRaw != null) {
			validateHabbitStructure(habbitRaw);
		}

		int eduBackground = parseEnumKey(in.get("edu_background"), 4, "学历填写错误");
		if (!EDU_BACKGROUND_KEYS.contains(eduBackground)) {
			throw missingParams("学历填写错误");
		}
		int income = parseEnumKey(in.get("income"), 4, "年收入填写错误");
		if (!INCOME_KEYS.contains(income)) {
			throw missingParams("年收入填写错误");
		}
		int industry = parseEnumKey(in.get("industry"), 12, "行业填写错误");
		if (!INDUSTRY_KEYS.contains(industry)) {
			throw missingParams("行业填写错误");
		}

		String email = defaultString(stringValue(in.get("email")));
		if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
			throw missingParams("email填写错误");
		}

		String mobilePlain = mobile.trim();
		if (!MOBILE_PATTERN.matcher(mobilePlain).matches()) {
			throw v2Fail(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}

		CreateDetailParams params = new CreateDetailParams();
		params.mobile = mobilePlain;
		params.sourceFrom = sourceFrom.trim();
		params.inviterMobile = inviterMobile.trim();
		params.salespersonMobile = salespersonMobile.trim();
		params.unionid = unionId.trim();
		params.status = status;
		params.tagNames = tagNames == null ? List.of() : tagNames;
		params.tagIds = List.of();
		params.cardCode = defaultString(stringValue(in.get("card_code")));
		params.gradeId = gradeId;
		params.username = defaultString(stringValue(in.get("username")));
		params.avatar = defaultString(stringValue(in.get("avatar")));
		params.sex = sex;
		params.birthday = birthday.trim();
		params.habbit = normalizeHabbit(habbitRaw);
		params.eduBackground = eduBackground;
		params.income = income;
		params.industry = industry;
		params.email = email.trim();
		params.address = defaultString(stringValue(in.get("address")));
		params.remarks = defaultString(stringValue(in.get("remarks")));
		params.companyId = companyId;
		return params;
	}

	private Members createMemberMain(long companyId, CreateDetailParams params) {
		long resolvedGradeId = resolveGradeId(companyId, params.gradeId);
		String userCardCode = resolveUserCardCode(companyId, params.cardCode);

		if (memberAccountService.findMemberByCompanyAndMobile(companyId, params.mobile) != null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}

		long inviterId = 0L;
		if (StringUtils.hasText(params.inviterMobile)) {
			Members inviter = memberAccountService.findMemberByCompanyAndMobile(companyId, params.inviterMobile);
			if (inviter == null || inviter.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_INVITER_NOT_FOUND, "会员的推荐人找不到");
			}
			inviterId = inviter.getUserId();
		}

		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(params.mobile);
		String plainPassword10 = randomPassword10Plain();
		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());

		Members m = new Members();
		m.setCompanyId(companyId);
		m.setGradeId(resolvedGradeId);
		m.setMobile(mobileStored);
		m.setRegionMobile(params.mobile);
		m.setMobileCountryCode("86");
		m.setPassword(plainPassword10);
		m.setUserCardCode(userCardCode);
		m.setOfflineCardCode("");
		m.setAuthorizerAppid("");
		m.setWxaAppid("");
		m.setAlipayAppid("");
		m.setInviterId(inviterId);
		m.setSourceFrom(params.sourceFrom);
		m.setSourceId(0L);
		m.setMonitorId(0L);
		m.setLatestSourceId(0L);
		m.setLatestMonitorId(0L);
		m.setRemarks(params.remarks);
		m.setDisabled(params.status != 1);
		m.setUsePoint(false);
		m.setThirdData(null);
		m.setRegDistributor(0);
		m.setRegSalesperson(null);
		m.setFpSalesperson(null);
		m.setHasFp(false);
		m.setIsBecomeFriend(false);
		m.setOpDistributor(0);
		m.setCreated(now);
		m.setUpdated(now);
		m.setCreatedYear(z.getYear());
		m.setCreatedMonth(z.getMonthValue());
		m.setCreatedDay(z.getDayOfMonth());

		membersMapper.insert(m);
		params.inviterId = inviterId;
		return m;
	}

	private void createMemberInfo(CreateDetailParams params) {
		int sex = params.sex;
		if (!SEX_SERVICE_KEYS.contains(sex)) {
			throw missingParams("性别填写错误");
		}

		String birthday = params.birthday;
		if (StringUtils.hasText(birthday)) {
			try {
				birthday = LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE).toString();
			} catch (DateTimeParseException e) {
				try {
					birthday =
							LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
									.toString();
				} catch (DateTimeParseException ex) {
					throw missingParams("生日格式填写错误");
				}
			}
		} else {
			birthday = "";
		}

		List<Map<String, Object>> habbitList = params.habbit;
		for (Map<String, Object> item : habbitList) {
			Object ischecked = item.get("ischecked");
			if (ischecked instanceof String s) {
				item.put("ischecked", "true".equalsIgnoreCase(s));
			} else {
				item.put("ischecked", Boolean.TRUE.equals(ischecked));
			}
		}

		if (!EDU_BACKGROUND_KEYS.contains(params.eduBackground)) {
			throw missingParams("学历填写错误");
		}
		if (!INCOME_KEYS.contains(params.income)) {
			throw missingParams("年收入填写错误");
		}
		if (!INDUSTRY_KEYS.contains(params.industry)) {
			throw missingParams("行业填写错误");
		}

		String habbitJson;
		try {
			habbitJson = JSON.writeValueAsString(habbitList);
		} catch (JsonProcessingException e) {
			throw missingParams("爱好填写错误");
		}

		long now = System.currentTimeMillis() / 1000L;
		MembersInfo info = new MembersInfo();
		info.setUserId(params.userId);
		info.setCompanyId(params.companyId);
		info.setUsername(params.username);
		info.setAvatar(params.avatar);
		info.setSex(sex);
		info.setBirthday(birthday);
		info.setAddress(params.address);
		info.setEmail(params.email);
		info.setIndustry(String.valueOf(params.industry));
		info.setIncome(String.valueOf(params.income));
		info.setEduBackground(String.valueOf(params.eduBackground));
		info.setHabbit(habbitJson);
		info.setHaveConsume(false);
		info.setOtherParams("[]");
		info.setCreated(now);
		info.setUpdated(now);
		membersInfoMapper.insert(info);
	}

	private void createMemberTags(long companyId, CreateDetailParams params) {
		List<Long> tagIdsResolved = checkTags(companyId, params.tagNames, params.tagIds);
		if (tagIdsResolved.isEmpty()) {
			return;
		}
		memberRelTagsBatchCreateService.createRelTags(List.of(params.userId), tagIdsResolved, companyId);
	}

	private List<Long> checkTags(long companyId, List<String> tagNames, List<Long> tagIds) {
		List<String> names = tagNames == null ? List.of() : tagNames.stream().filter(StringUtils::hasText).toList();
		List<Long> ids = tagIds == null ? List.of() : tagIds;
		if (names.isEmpty() && ids.isEmpty()) {
			return List.of();
		}

		Map<Object, Boolean> transformTagsName = new LinkedHashMap<>();
		Map<Object, Boolean> transformTagsId = new LinkedHashMap<>();
		for (String tagName : names) {
			transformTagsName.put(tagName, true);
		}
		for (Long tagId : ids) {
			transformTagsId.put(tagId, true);
		}

		LambdaQueryWrapper<MemberTags> w = new LambdaQueryWrapper<MemberTags>().eq(MemberTags::getCompanyId, companyId);
		if (!names.isEmpty() && !ids.isEmpty()) {
			w.and(
					q ->
							q.in(MemberTags::getTagName, names)
									.or()
									.in(MemberTags::getTagId, ids));
		} else if (!names.isEmpty()) {
			w.in(MemberTags::getTagName, names);
		} else {
			w.in(MemberTags::getTagId, ids);
		}

		List<MemberTags> data = memberTagsMapper.selectList(w);
		for (MemberTags datum : data) {
			transformTagsName.remove(datum.getTagId());
			transformTagsId.remove(datum.getTagName());
		}

		for (Object tagName : transformTagsName.keySet()) {
			throw v2Fail(
					OpenapiErrorCode.MEMBER_TAG_NOT_FOUND,
					"标签名为:" + tagName + ", 标签不存在！");
		}
		for (Object tagId : transformTagsId.keySet()) {
			throw v2Fail(
					OpenapiErrorCode.MEMBER_TAG_NOT_FOUND,
					"标签id为:" + tagId + ", 标签不存在！");
		}

		return data.stream().map(MemberTags::getTagId).filter(id -> id != null).toList();
	}

	private void bindSalesperson(long companyId, CreateDetailParams params) {
		Long salespersonId =
				openapiShopSalespersonByMobilePort.findSalespersonIdByMobile(companyId, params.salespersonMobile);
		if (salespersonId == null || salespersonId <= 0L) {
			throw v2Fail(OpenapiErrorCode.SALESPERSON_NOT_FOUND, "导购找不到");
		}

		WorkWechatRel boundInfo =
				workWechatRelMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatRel>()
								.eq(WorkWechatRel::getCompanyId, companyId)
								.eq(WorkWechatRel::getUserId, params.userId)
								.eq(WorkWechatRel::getIsBind, true)
								.last("LIMIT 1"));
		if (boundInfo != null) {
			long bindSalespersonId = boundInfo.getSalespersonId() == null ? 0L : boundInfo.getSalespersonId();
			if (bindSalespersonId == salespersonId) {
				throw v2Fail(OpenapiErrorCode.SALESPERSON_RELATION_MEMBER_EXIST, "会员已与该导购绑定");
			}
			throw v2Fail(OpenapiErrorCode.SALESPERSON_RELATION_MEMBER_EXIST, "会员已与其他导购绑定");
		}

		WorkWechatRel existing =
				workWechatRelMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatRel>()
								.eq(WorkWechatRel::getUserId, params.userId)
								.eq(WorkWechatRel::getCompanyId, companyId)
								.eq(WorkWechatRel::getSalespersonId, salespersonId)
								.last("LIMIT 1"));
		long boundEpoch = System.currentTimeMillis() / 1000L;
		if (existing != null) {
			LambdaUpdateWrapper<WorkWechatRel> uw = new LambdaUpdateWrapper<>();
			uw.eq(WorkWechatRel::getUserId, params.userId)
					.eq(WorkWechatRel::getCompanyId, companyId)
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.set(WorkWechatRel::getIsBind, true)
					.set(WorkWechatRel::getBoundTime, boundEpoch);
			workWechatRelMapper.update(null, uw);
		} else {
			WorkWechatRel row = new WorkWechatRel();
			row.setCompanyId(companyId);
			row.setSalespersonId(salespersonId);
			row.setUnionid(params.unionid);
			row.setUserId(params.userId);
			row.setWorkUserid("");
			row.setExternalUserid("");
			row.setIsFriend(false);
			row.setIsBind(true);
			row.setBoundTime(boundEpoch);
			row.setAddFriendTime(0L);
			workWechatRelMapper.insert(row);
		}

		WorkWechatRelLogs log = new WorkWechatRelLogs();
		log.setCompanyId(companyId);
		log.setSalespersonId(salespersonId);
		log.setUnionid(params.unionid);
		log.setUserId(params.userId);
		log.setWorkUserid("");
		log.setExternalUserid("");
		log.setIsFriend(false);
		log.setRemarks("");
		int ts = (int) boundEpoch;
		log.setCreated(ts);
		log.setUpdated(ts);
		workWechatRelLogsMapper.insert(log);

		openapiSalespersonCompleteNewUserPort.completeNewUser(companyId, salespersonId, params.userId);
		openapiSalespersonMemberNumIncreasePort.increaseSalespersonMemberNum(
				companyId, params.inviterId, params.userId);
	}

	private void insertMembersAssociation(long companyId, long userId, String unionid) {
		MembersAssociations assoc = new MembersAssociations();
		assoc.setUserId(userId);
		assoc.setUnionid(unionid);
		assoc.setCompanyId(companyId);
		assoc.setUserType("wechat");
		membersAssociationsMapper.insert(assoc);
	}

	private Map<String, Object> toUserDataMap(Members member) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("user_id", nzLong(member.getUserId()));
		row.put("company_id", nzLong(member.getCompanyId()));
		row.put("grade_id", nzLong(member.getGradeId()));
		row.put("mobile", resolvePlainMobile(member));
		row.put("region_mobile", defaultString(member.getRegionMobile()));
		row.put("mobile_country_code", defaultString(member.getMobileCountryCode()));
		row.put("user_card_code", defaultString(member.getUserCardCode()));
		row.put("offline_card_code", defaultString(member.getOfflineCardCode()));
		row.put("inviter_id", nzLong(member.getInviterId()));
		row.put("source_from", defaultString(member.getSourceFrom()));
		row.put("source_id", nzLong(member.getSourceId()));
		row.put("monitor_id", nzLong(member.getMonitorId()));
		row.put("latest_source_id", nzLong(member.getLatestSourceId()));
		row.put("latest_monitor_id", nzLong(member.getLatestMonitorId()));
		row.put("authorizer_appid", defaultString(member.getAuthorizerAppid()));
		row.put("use_point", boolToInt(member.getUsePoint()));
		row.put("wxa_appid", defaultString(member.getWxaAppid()));
		row.put("alipay_appid", defaultString(member.getAlipayAppid()));
		row.put("created", nzLong(member.getCreated()));
		row.put("updated", nzLong(member.getUpdated()));
		row.put("disabled", boolToInt(member.getDisabled()));
		row.put("remarks", defaultString(member.getRemarks()));
		row.put("third_data", member.getThirdData());
		row.put("reg_distributor", nzInt(member.getRegDistributor()));
		row.put("reg_salesperson", member.getRegSalesperson());
		row.put("fp_salesperson", defaultString(member.getFpSalesperson()));
		row.put("has_fp", boolToInt(member.getHasFp()));
		row.put("is_become_friend", boolToInt(member.getIsBecomeFriend()));
		row.put("op_distributor", nzInt(member.getOpDistributor()));
		return row;
	}

	private long resolveGradeId(long companyId, int gradeId) {
		if (gradeId > 0) {
			Long found = membersMapper.selectGradeIdIfExists(String.valueOf(companyId), gradeId);
			if (found == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_GRADE_NOT_FOUND, "会员等级找不到");
			}
			return found;
		}
		Long defaultGrade = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		return defaultGrade == null ? 0L : defaultGrade;
	}

	private String resolveUserCardCode(long companyId, String cardCode) {
		String userCardCode = StringUtils.hasText(cardCode) ? cardCode.trim() : "";
		if (!StringUtils.hasText(userCardCode)) {
			userCardCode = memberUserCardCodeAllocateService.allocateCode();
		}
		Members existing =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserCardCode, userCardCode)
								.last("LIMIT 1"));
		if (existing != null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_CARD_EXIST, "会员卡号已存在");
		}
		return userCardCode;
	}

	private static String resolvePlainMobile(Members member) {
		if (StringUtils.hasText(member.getRegionMobile())) {
			return member.getRegionMobile();
		}
		if (StringUtils.hasText(member.getMobile())) {
			return member.getMobile();
		}
		return "";
	}

	private static int parseStatus(Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return 1;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			if (v != 0 && v != 1) {
				throw missingParams("会员状态填写错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw missingParams("会员状态填写错误");
		}
	}

	private static int parseGradeId(Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return -1;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			if (v < 0) {
				throw missingParams("会员等级ID填写错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw missingParams("会员等级ID填写错误");
		}
	}

	private static int parseSex(Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return 0;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			if (v != 0) {
				throw missingParams("性别填写错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw missingParams("性别填写错误");
		}
	}

	private static int parseEnumKey(Object raw, int defaultValue, String errorMessage) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v;
		} catch (NumberFormatException e) {
			throw missingParams(errorMessage);
		}
	}

	private static void validateHabbitStructure(Object habbitRaw) {
		if (!(habbitRaw instanceof List<?> list)) {
			throw missingParams("爱好填写错误");
		}
		for (Object item : list) {
			if (!(item instanceof Map<?, ?> map)) {
				throw missingParams("爱好填写错误");
			}
			Object name = map.get("name");
			Object ischecked = map.get("ischecked");
			if (!(name instanceof String) || !StringUtils.hasText((String) name)) {
				throw missingParams("爱好填写错误");
			}
			if (!isValidIschecked(ischecked)) {
				throw missingParams("爱好填写错误");
			}
		}
	}

	private static List<Map<String, Object>> normalizeHabbit(Object habbitRaw) {
		if (habbitRaw == null) {
			return List.of();
		}
		if (!(habbitRaw instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				Map<String, Object> copy = new HashMap<>();
				copy.put("name", map.get("name"));
				copy.put("ischecked", map.get("ischecked"));
				out.add(copy);
			}
		}
		return out;
	}

	private static boolean isValidIschecked(Object value) {
		if (value instanceof Boolean) {
			return true;
		}
		if (value instanceof String s) {
			return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
		}
		return false;
	}

	private static boolean isValidBirthdayInput(String birthday) {
		try {
			LocalDate.parse(birthday.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
			return true;
		} catch (DateTimeParseException e) {
			try {
				LocalDate.parse(birthday.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				return true;
			} catch (DateTimeParseException ex) {
				return false;
			}
		}
	}

	private static List<String> splitCommaTrim(String raw) {
		String[] parts = raw.split(",");
		List<String> out = new ArrayList<>();
		for (String part : parts) {
			String t = part.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
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

	private static String defaultString(String raw) {
		return raw == null ? "" : raw;
	}

	private static long nzLong(Long value) {
		return value == null ? 0L : value;
	}

	private static int nzInt(Integer value) {
		return value == null ? 0 : value;
	}

	private static int boolToInt(Boolean value) {
		return Boolean.TRUE.equals(value) ? 1 : 0;
	}

	private static String randomPassword10Plain() {
		char[] pool = PASSWORD_POOL.toCharArray();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = pool.length - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			char t = pool[i];
			pool[i] = pool[j];
			pool[j] = t;
		}
		String shuffled = new String(pool);
		int start = 5;
		int len = 10;
		if (shuffled.length() < start + len) {
			return shuffled;
		}
		return shuffled.substring(start, start + len);
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}

	private static final class CreateDetailParams {
		long companyId;
		long userId;
		long inviterId;
		String mobile;
		String sourceFrom;
		String inviterMobile;
		String salespersonMobile;
		String unionid;
		int status;
		List<String> tagNames;
		List<Long> tagIds;
		String cardCode;
		int gradeId;
		String username;
		String avatar;
		int sex;
		String birthday;
		List<Map<String, Object>> habbit;
		int eduBackground;
		int income;
		int industry;
		String email;
		String address;
		String remarks;
	}
}
