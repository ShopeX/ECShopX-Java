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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.MemberOperateLog;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MemberOperateLogMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberUpdateDetailService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final Set<Integer> EDU_BACKGROUND_KEYS = Set.of(0, 1, 2, 3, 4);
	private static final Set<Integer> INCOME_KEYS = Set.of(0, 1, 2, 3, 4);
	private static final Set<Integer> INDUSTRY_KEYS =
			Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	private static final Set<Integer> SEX_SERVICE_KEYS = Set.of(0, 1, 2);
	private static final Set<String> LOG_EXCLUDE_COLUMNS = Set.of("created", "updated");

	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberOperateLogMapper memberOperateLogMapper;
	private final MemberAccountService memberAccountService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberUpdateDetailService(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MemberOperateLogMapper memberOperateLogMapper,
			MemberAccountService memberAccountService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberOperateLogMapper = memberOperateLogMapper;
		this.memberAccountService = memberAccountService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiUpdateDetail(
			long companyId, Map<String, Object> mergedRaw, Map<String, Object> requestData) {
		try {
			MemberUpdateOperateLogCollector collector = new MemberUpdateOperateLogCollector();
			String mobile = validateMobile(mergedRaw);
			validateActionFields(mergedRaw);

			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
			if (member == null || member.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}
			long userId = member.getUserId();

			updateMembersMain(companyId, member, requestData, collector);
			updateMembersInfo(companyId, userId, requestData, collector);
			saveOperateInfoLog(companyId, userId, collector);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private void updateMembersMain(
			long companyId,
			Members member,
			Map<String, Object> requestData,
			MemberUpdateOperateLogCollector collector) {
		long userId = member.getUserId();
		Members before =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (before == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}

		String inviterMobile = String.valueOf(requestData.getOrDefault("inviter_mobile", "0"));
		long inviterId = 0L;
		if (StringUtils.hasText(inviterMobile) && !"0".equals(inviterMobile.trim())) {
			Members inviter =
					memberAccountService.findMemberByCompanyAndMobile(companyId, inviterMobile.trim());
			if (inviter == null || inviter.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_INVITER_NOT_FOUND, "会员的推荐人找不到");
			}
			inviterId = inviter.getUserId();
		}

		int status = parseIntFromRequestData(requestData.get("status"), 1);
		boolean disabled = status != 1;
		long nowSec = System.currentTimeMillis() / 1000L;

		LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
		uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
		uw.set(Members::getInviterId, inviterId);
		uw.set(Members::getDisabled, disabled);
		uw.set(Members::getUpdated, nowSec);
		membersMapper.update(null, uw);

		Members after =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (after == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}
		collector.register(
				MemberAccountService.mapMembersTable(after), MemberAccountService.mapMembersTable(before));
	}

	private void updateMembersInfo(
			long companyId,
			long userId,
			Map<String, Object> requestData,
			MemberUpdateOperateLogCollector collector) {
		MembersInfo before =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (before == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}

		int sex = parseSexService(requestData.get("sex"));
		String birthday = defaultString(stringValue(requestData.get("birthday")));
		validateBirthDayService(birthday);

		List<Map<String, Object>> habbitList = normalizeHabbitForService(requestData.get("habbit"));
		validateHabbitService(habbitList);

		int eduBackground = parseIntFromRequestData(requestData.get("edu_background"), 4);
		int income = parseIntFromRequestData(requestData.get("income"), 4);
		int industry = parseIntFromRequestData(requestData.get("industry"), 12);
		validateEduBackgroundService(eduBackground);
		validateIncomeService(income);
		validateIndustryService(industry);

		String username = defaultString(stringValue(requestData.get("username")));
		String avatar = defaultString(stringValue(requestData.get("avatar")));
		String email = defaultString(stringValue(requestData.get("email")));
		String address = defaultString(stringValue(requestData.get("address")));

		String habbitJson;
		try {
			habbitJson = objectMapper.writeValueAsString(habbitList);
		} catch (JsonProcessingException e) {
			throw missingParams("爱好填写错误");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<MembersInfo> uw = new LambdaUpdateWrapper<>();
		uw.eq(MembersInfo::getCompanyId, companyId).eq(MembersInfo::getUserId, userId);
		uw.set(MembersInfo::getUsername, sensitiveFieldEncryptor.encrypt(username));
		uw.set(MembersInfo::getAvatar, avatar);
		uw.set(MembersInfo::getSex, sex);
		uw.set(MembersInfo::getEmail, email);
		uw.set(MembersInfo::getAddress, address);
		uw.set(MembersInfo::getEduBackground, String.valueOf(eduBackground));
		uw.set(MembersInfo::getIncome, String.valueOf(income));
		uw.set(MembersInfo::getIndustry, String.valueOf(industry));
		uw.set(MembersInfo::getHabbit, habbitJson);
		uw.set(MembersInfo::getUpdated, nowSec);

		if (StringUtils.hasText(birthday)) {
			LocalDate parsedBirthday = parseBirthdayDate(birthday);
			uw.set(MembersInfo::getBirthday, parsedBirthday.toString());
			uw.set(MembersInfo::getYear, parsedBirthday.getYear());
			uw.set(MembersInfo::getMonth, parsedBirthday.getMonthValue());
			uw.set(MembersInfo::getDay, parsedBirthday.getDayOfMonth());
		}

		membersInfoMapper.update(null, uw);

		MembersInfo after =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		if (after == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}
		collector.register(toMemberInfoLogMap(after), toMemberInfoLogMap(before));
	}

	private void saveOperateInfoLog(
			long companyId, long userId, MemberUpdateOperateLogCollector collector) {
		if (collector.isEmpty()) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		try {
			MemberOperateLog log = new MemberOperateLog();
			log.setCompanyId(companyId);
			log.setUserId(userId);
			log.setOperateType("info");
			log.setRemarks("");
			log.setOldData(objectMapper.writeValueAsString(collector.oldData()));
			log.setNewData(objectMapper.writeValueAsString(collector.newData()));
			log.setOperater("外部开发者");
			log.setCreated(nowSec);
			log.setUpdated(nowSec);
			memberOperateLogMapper.insert(log);
		} catch (JsonProcessingException e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private String validateMobile(Map<String, Object> mergedRaw) {
		Map<String, Object> in = mergedRaw == null ? Map.of() : mergedRaw;
		String mobile = stringValue(in.get("mobile"));
		if (!StringUtils.hasText(mobile) || !MOBILE_PATTERN.matcher(mobile.trim()).matches()) {
			throw v2Fail(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}
		return mobile.trim();
	}

	private void validateActionFields(Map<String, Object> mergedRaw) {
		Map<String, Object> in = mergedRaw == null ? Map.of() : mergedRaw;

		if (in.containsKey("inviter_mobile")) {
			String inviterMobile = defaultString(stringValue(in.get("inviter_mobile")));
			if (StringUtils.hasText(inviterMobile)
					&& !MOBILE_PATTERN.matcher(inviterMobile.trim()).matches()) {
				throw missingParams("推荐人手机号填写错误");
			}
		}

		if (in.containsKey("status")) {
			parseStatusAction(in.get("status"));
		}

		validateNullableStringField(in, "remarks", "备注填写错误");
		validateNullableStringField(in, "username", "姓名/昵称填写错误");
		validateNullableStringField(in, "avatar", "头像url填写错误");
		validateNullableStringField(in, "address", "地址填写错误");

		if (in.containsKey("sex")) {
			parseSexAction(in.get("sex"));
		}

		if (in.containsKey("birthday")) {
			String birthday = defaultString(stringValue(in.get("birthday")));
			if (StringUtils.hasText(birthday) && !isValidBirthdayInput(birthday)) {
				throw missingParams("生日填写错误");
			}
		}

		if (in.containsKey("edu_background")) {
			int eduBackground = parseEnumKey(in.get("edu_background"), -1, "学历填写错误");
			if (!EDU_BACKGROUND_KEYS.contains(eduBackground)) {
				throw missingParams("学历填写错误");
			}
		}
		if (in.containsKey("income")) {
			int income = parseEnumKey(in.get("income"), -1, "年收入填写错误");
			if (!INCOME_KEYS.contains(income)) {
				throw missingParams("年收入填写错误");
			}
		}
		if (in.containsKey("industry")) {
			int industry = parseEnumKey(in.get("industry"), -1, "行业填写错误");
			if (!INDUSTRY_KEYS.contains(industry)) {
				throw missingParams("行业填写错误");
			}
		}

		if (in.containsKey("email")) {
			String email = defaultString(stringValue(in.get("email")));
			if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
				throw missingParams("email填写错误");
			}
		}
	}

	private static void validateNullableStringField(
			Map<String, Object> in, String key, String errorMessage) {
		if (!in.containsKey(key)) {
			return;
		}
		Object raw = in.get(key);
		if (raw != null && !(raw instanceof String)) {
			throw missingParams(errorMessage);
		}
	}

	private static int parseStatusAction(Object raw) {
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

	private static int parseSexAction(Object raw) {
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

	private static int parseSexService(Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return 0;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			if (!SEX_SERVICE_KEYS.contains(v)) {
				throw missingParams("性别填写错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw missingParams("性别填写错误");
		}
	}

	private static void validateBirthDayService(String birthday) {
		if (!StringUtils.hasText(birthday)) {
			throw missingParams("生日格式填写错误");
		}
		try {
			parseBirthdayDate(birthday);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw missingParams("生日格式填写错误");
		}
	}

	private static LocalDate parseBirthdayDate(String birthday) {
		try {
			return LocalDate.parse(birthday.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			try {
				return LocalDate.parse(birthday.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			} catch (DateTimeParseException ex) {
				throw missingParams("生日格式填写错误");
			}
		}
	}

	private static List<Map<String, Object>> normalizeHabbitForService(Object habbitRaw) {
		if (habbitRaw == null) {
			return List.of();
		}
		if (habbitRaw instanceof List<?> list) {
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
		return List.of();
	}

	private static void validateHabbitService(List<Map<String, Object>> habbitList) {
		for (Map<String, Object> item : habbitList) {
			Object name = item.get("name");
			Object ischecked = item.get("ischecked");
			if (!(name instanceof String) || !StringUtils.hasText((String) name)) {
				throw missingParams("爱好填写错误");
			}
			if (!isValidIschecked(ischecked)) {
				throw missingParams("爱好填写错误");
			}
			if (ischecked instanceof String s) {
				item.put("ischecked", "true".equalsIgnoreCase(s));
			} else {
				item.put("ischecked", Boolean.TRUE.equals(ischecked));
			}
		}
	}

	private static void validateEduBackgroundService(int eduBackground) {
		if (!EDU_BACKGROUND_KEYS.contains(eduBackground)) {
			throw missingParams("学历填写错误");
		}
	}

	private static void validateIncomeService(int income) {
		if (!INCOME_KEYS.contains(income)) {
			throw missingParams("年收入填写错误");
		}
	}

	private static void validateIndustryService(int industry) {
		if (!INDUSTRY_KEYS.contains(industry)) {
			throw missingParams("行业填写错误");
		}
	}

	private static int parseEnumKey(Object raw, int defaultValue, String errorMessage) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw missingParams(errorMessage);
		}
	}

	private static int parseIntFromRequestData(Object raw, int defaultValue) {
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

	private static boolean isValidIschecked(Object value) {
		if (value instanceof Boolean) {
			return true;
		}
		if (value instanceof String s) {
			return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
		}
		return false;
	}

	private static Map<String, Object> toMemberInfoLogMap(MembersInfo info) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", info.getUserId());
		m.put("company_id", info.getCompanyId());
		m.put("username", info.getUsername());
		m.put("name", info.getName());
		m.put("avatar", info.getAvatar());
		m.put("sex", info.getSex());
		m.put("birthday", info.getBirthday());
		m.put("address", info.getAddress());
		m.put("email", info.getEmail());
		m.put("industry", info.getIndustry());
		m.put("income", info.getIncome());
		m.put("edu_background", info.getEduBackground());
		m.put("habbit", info.getHabbit());
		m.put("have_consume", info.getHaveConsume());
		m.put("other_params", info.getOtherParams());
		m.put("dm_member_id", info.getDmMemberId());
		m.put("dm_card_no", info.getDmCardNo());
		return m;
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

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}

	private static final class MemberUpdateOperateLogCollector {

		private final Map<String, Object> oldData = new LinkedHashMap<>();
		private final Map<String, Object> newData = new LinkedHashMap<>();

		void register(Map<String, Object> newMap, Map<String, Object> oldMap) {
			for (Map.Entry<String, Object> entry : newMap.entrySet()) {
				String column = entry.getKey();
				if (LOG_EXCLUDE_COLUMNS.contains(column)) {
					continue;
				}
				Object newValue = entry.getValue();
				Object oldValue = oldMap.get(column);
				if (Objects.equals(newValue, oldValue)) {
					continue;
				}
				oldData.put(column, oldValue);
				newData.put(column, newValue);
			}
		}

		boolean isEmpty() {
			return oldData.isEmpty() && newData.isEmpty();
		}

		Map<String, Object> oldData() {
			return oldData;
		}

		Map<String, Object> newData() {
			return newData;
		}
	}
}
