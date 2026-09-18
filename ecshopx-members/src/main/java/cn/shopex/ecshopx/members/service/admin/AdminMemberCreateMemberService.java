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

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePostCommitDataPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberCreateMemberService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private static final String PASSWORD_POOL =
			"QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";

	/** Members table payload keys aligned with admin create-member success body. */
	private static final List<String> CREATE_MEMBER_BODY_MEMBER_KEYS = List.of(
			"user_id",
			"company_id",
			"grade_id",
			"mobile",
			"region_mobile",
			"mobile_country_code",
			"user_card_code",
			"offline_card_code",
			"inviter_id",
			"source_from",
			"source_id",
			"monitor_id",
			"latest_source_id",
			"latest_monitor_id",
			"authorizer_appid",
			"use_point",
			"wxa_appid",
			"alipay_appid",
			"created",
			"updated",
			"disabled",
			"remarks",
			"third_data",
			"reg_distributor",
			"reg_salesperson",
			"fp_salesperson",
			"has_fp",
			"is_become_friend",
			"op_distributor");

	private final MemberAccountService memberAccountService;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MembersDeleteRecordMapper membersDeleteRecordMapper;
	private final ShopProtocolSetService shopProtocolSetService;
	private final MemberUserCardCodeAllocateService memberUserCardCodeAllocateService;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;
	private final AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort;
	private final AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort;
	private final AdminMemberCreatePostCommitDataPort adminMemberCreatePostCommitDataPort;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AdminMemberCreateMemberService(
			MemberAccountService memberAccountService,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MembersDeleteRecordMapper membersDeleteRecordMapper,
			ShopProtocolSetService shopProtocolSetService,
			MemberUserCardCodeAllocateService memberUserCardCodeAllocateService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher,
			@Qualifier("adminMemberCreatePromoterSideEffectPortImpl")
					AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort,
			@Qualifier("adminMemberCreateDistributorUserSideEffectPortImpl")
					AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort,
			@Qualifier("adminMemberCreatePostCommitDataPortImpl")
					AdminMemberCreatePostCommitDataPort adminMemberCreatePostCommitDataPort) {
		this.memberAccountService = memberAccountService;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
		this.shopProtocolSetService = shopProtocolSetService;
		this.memberUserCardCodeAllocateService = memberUserCardCodeAllocateService;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.membersCreateMemberSuccessDispatchPublisher =
				Objects.requireNonNull(membersCreateMemberSuccessDispatchPublisher, "membersCreateMemberSuccessDispatchPublisher");
		this.adminMemberCreatePromoterSideEffectPort = adminMemberCreatePromoterSideEffectPort;
		this.adminMemberCreateDistributorUserSideEffectPort = adminMemberCreateDistributorUserSideEffectPort;
		this.adminMemberCreatePostCommitDataPort = adminMemberCreatePostCommitDataPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMember(long companyId, Map<String, Object> postData) {
		Object mobileRaw = postData.get("mobile");
		String mobile = mobileRaw == null ? "" : String.valueOf(mobileRaw).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("手机号必填");
		}
		if (!MOBILE_PATTERN.matcher(mobile).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
		Map<String, Object> existing = memberAccountService.getInfoByMobile(companyId, mobile);
		if (existing != null && !existing.isEmpty()) {
			throw new ResourceException("手机号已存在");
		}
		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeId == null) {
			throw new ResourceException("缺少默认等级");
		}
		long distributorId = parseDistributorIdFromPostData(postData);
		String code = memberUserCardCodeAllocateService.allocateCode();
		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile);
		String randomUsername = randomUsername8();
		String plainPassword10 = randomPassword10();

		long userId;
		boolean ifRegisterPromotion = true;
		try {
			long now = System.currentTimeMillis() / 1000L;
			ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());
			Members m = new Members();
			m.setCompanyId(companyId);
			m.setGradeId(gradeId);
			m.setMobile(mobileStored);
			m.setRegionMobile(mobile);
			m.setMobileCountryCode("86");
			m.setPassword(passwordEncoder.encode(plainPassword10));
			m.setUserCardCode(code);
			m.setCreated(now);
			m.setUpdated(now);
			m.setDisabled(false);
			m.setCreatedYear(z.getYear());
			m.setCreatedMonth(z.getMonthValue());
			m.setCreatedDay(z.getDayOfMonth());
			membersMapper.insert(m);
			userId = m.getUserId();

			MembersInfo info = new MembersInfo();
			info.setUserId(userId);
			info.setCompanyId(companyId);
			info.setUsername(randomUsername);
			info.setOtherParams("{}");
			info.setCreated(now);
			info.setUpdated(now);
			membersInfoMapper.insert(info);
			Map<String, Object> proto =
					shopProtocolSetService.get(companyId, "member_logout_config", "zh-CN");
			Object block = proto.get("member_logout_config");
			String newRights = "";
			if (block instanceof Map<?, ?> mb) {
				Object nr = mb.get("new_rights");
				newRights = nr == null ? "" : String.valueOf(nr).trim();
			}
			if (newRights.isEmpty() || "0".equals(newRights)) {
				MembersDeleteRecord dr = membersDeleteRecordMapper.selectOne(
						new LambdaQueryWrapper<MembersDeleteRecord>()
								.eq(MembersDeleteRecord::getCompanyId, companyId)
								.eq(MembersDeleteRecord::getMobile, mobileStored)
								.last("LIMIT 1"));
				if (dr != null) {
					ifRegisterPromotion = false;
				}
			}

			adminMemberCreatePromoterSideEffectPort.createForNewMember(companyId, userId, mobile, gradeId);
			adminMemberCreateDistributorUserSideEffectPort.createData(
					companyId, userId, distributorId, 0L, 0L);
			addDailyMemberActiveRedisSet(companyId, userId);
		} catch (Exception ignored) {
			throw new ResourceException("会员添加失败");
		}

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("user_id", userId);
		eventData.put("company_id", companyId);
		eventData.put("mobile", mobile);
		eventData.put("openid", "");
		eventData.put("wxa_appid", "");
		eventData.put("source_id", 0L);
		eventData.put("monitor_id", 0L);
		eventData.put("inviter_id", 0L);
		eventData.put("salesperson_id", 0L);
		eventData.put("distributor_id", distributorId);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("createMember requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
			}
		});

		Map<String, Object> rawMember = memberAccountService.getMemberInfo(userId, companyId);
		Map<String, Object> body = buildCreateMemberSuccessBody(rawMember);
		Map<String, Object> gradeInfo = adminMemberCreatePostCommitDataPort.loadGradeInfo(companyId, gradeId);
		Map<String, Object> shapedGrade = shapeGradeInfoForCreateMember(gradeInfo);
		Map<String, Object> vipgrade = adminMemberCreatePostCommitDataPort.loadVipGrade(companyId, userId);
		if (vipgrade == null) {
			vipgrade = new LinkedHashMap<>(Map.of("is_vip", false));
		}
		body.put("gradeInfo", shapedGrade);
		body.put("vipgrade", vipgrade);
		return body;
	}

	/**
	 * 邮箱注册场景建会员（对齐 PHP {@code MemberService::createMember} 的本地注册分支）：
	 * 使用合成手机号（不做前台手机号校验）、写入 {@code login_email}、{@code email_verified_at=NULL}、
	 * 使用调用方提供的密码；其余（默认等级、卡号、注销会员营销、推广员/门店、每日活跃、建会员事件）与
	 * {@link #createMember} 一致。调用方须处于事务中。
	 *
	 * @param createParams keys: mobile, login_email, password, username, avatar, sex,
	 *     wxa_appid, authorizer_appid, inviter_id, source_from, source_id, monitor_id
	 * @return 含 user_id / company_id / mobile 的最小结果（供注册接口返回）
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMemberWithEmail(long companyId, Map<String, Object> createParams) {
		String mobile = createParams.get("mobile") == null
				? ""
				: String.valueOf(createParams.get("mobile")).trim();
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("手机号必填");
		}
		String email = createParams.get("login_email") == null
				? ""
				: String.valueOf(createParams.get("login_email")).trim();
		if (!StringUtils.hasText(email)) {
			throw new BadRequestException("邮箱必填");
		}
		String plainPassword =
				createParams.get("password") == null ? "" : String.valueOf(createParams.get("password"));

		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeId == null) {
			throw new ResourceException("缺少默认等级");
		}

		String code = memberUserCardCodeAllocateService.allocateCode();
		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile);
		String username = String.valueOf(createParams.getOrDefault("username", "")).trim();
		if (username.isEmpty()) {
			username = randomUsername8();
		}
		String avatar = String.valueOf(createParams.getOrDefault("avatar", "")).trim();
		int sex = intOrZero(createParams.get("sex"));
		String sourceFrom = String.valueOf(createParams.getOrDefault("source_from", "default")).trim();
		if (sourceFrom.isEmpty()) {
			sourceFrom = "default";
		}
		long sourceId = longOrZero(createParams.get("source_id"));
		long monitorId = longOrZero(createParams.get("monitor_id"));
		long inviterId = longOrZero(createParams.get("inviter_id"));
		String wxaAppid = String.valueOf(createParams.getOrDefault("wxa_appid", "")).trim();
		String authorizerAppid = String.valueOf(createParams.getOrDefault("authorizer_appid", "")).trim();

		long userId;
		boolean ifRegisterPromotion = true;
		try {
			long now = System.currentTimeMillis() / 1000L;
			ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());
			Members m = new Members();
			m.setCompanyId(companyId);
			m.setGradeId(gradeId);
			m.setMobile(mobileStored);
			m.setRegionMobile(mobile);
			m.setMobileCountryCode("86");
			m.setLoginEmail(email);
			m.setEmailVerifiedAt(null);
			m.setPassword(passwordEncoder.encode(plainPassword));
			m.setUserCardCode(code);
			m.setWxaAppid(wxaAppid.isEmpty() ? null : wxaAppid);
			m.setAuthorizerAppid(authorizerAppid.isEmpty() ? null : authorizerAppid);
			m.setInviterId(inviterId);
			m.setSourceFrom(sourceFrom);
			m.setSourceId(sourceId);
			m.setMonitorId(monitorId);
			m.setCreated(now);
			m.setUpdated(now);
			m.setDisabled(false);
			m.setCreatedYear(z.getYear());
			m.setCreatedMonth(z.getMonthValue());
			m.setCreatedDay(z.getDayOfMonth());
			membersMapper.insert(m);
			userId = m.getUserId();

			MembersInfo info = new MembersInfo();
			info.setUserId(userId);
			info.setCompanyId(companyId);
			info.setUsername(username);
			info.setAvatar(avatar.isEmpty() ? null : avatar);
			info.setEmail(email);
			info.setSex(sex);
			info.setOtherParams("{}");
			info.setCreated(now);
			info.setUpdated(now);
			membersInfoMapper.insert(info);

			Map<String, Object> proto =
					shopProtocolSetService.get(companyId, "member_logout_config", "zh-CN");
			Object block = proto.get("member_logout_config");
			String newRights = "";
			if (block instanceof Map<?, ?> mb) {
				Object nr = mb.get("new_rights");
				newRights = nr == null ? "" : String.valueOf(nr).trim();
			}
			if (newRights.isEmpty() || "0".equals(newRights)) {
				MembersDeleteRecord dr = membersDeleteRecordMapper.selectOne(
						new LambdaQueryWrapper<MembersDeleteRecord>()
								.eq(MembersDeleteRecord::getCompanyId, companyId)
								.eq(MembersDeleteRecord::getMobile, mobileStored)
								.last("LIMIT 1"));
				if (dr != null) {
					ifRegisterPromotion = false;
				}
			}

			adminMemberCreatePromoterSideEffectPort.createForNewMember(companyId, userId, mobile, gradeId);
			adminMemberCreateDistributorUserSideEffectPort.createData(companyId, userId, 0L, 0L, 0L);
			addDailyMemberActiveRedisSet(companyId, userId);
		} catch (Exception ignored) {
			throw new ResourceException("会员添加失败");
		}

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("user_id", userId);
		eventData.put("company_id", companyId);
		eventData.put("mobile", mobile);
		eventData.put("openid", "");
		eventData.put("wxa_appid", wxaAppid);
		eventData.put("source_id", sourceId);
		eventData.put("monitor_id", monitorId);
		eventData.put("inviter_id", inviterId);
		eventData.put("salesperson_id", 0L);
		eventData.put("distributor_id", 0L);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("createMemberWithEmail requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
			}
		});

		Map<String, Object> out = new LinkedHashMap<>(4);
		out.put("user_id", userId);
		out.put("company_id", companyId);
		out.put("mobile", mobile);
		return out;
	}

	private static int intOrZero(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(value).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longOrZero(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(value).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Map<String, Object> buildCreateMemberSuccessBody(Map<String, Object> rawMember) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String key : CREATE_MEMBER_BODY_MEMBER_KEYS) {
			Object v = rawMember.get(key);
			if ("user_id".equals(key)) {
				if (v != null) {
					out.put(key, String.valueOf(v).trim());
				}
				continue;
			}
			if ("source_id".equals(key)
					|| "monitor_id".equals(key)
					|| "latest_source_id".equals(key)
					|| "latest_monitor_id".equals(key)) {
				out.put(key, jsonNullIfUnsetId(v));
				continue;
			}
			out.put(key, v);
		}
		return out;
	}

	private static Object jsonNullIfUnsetId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L ? null : n;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return null;
		}
		return v;
	}

	private Map<String, Object> shapeGradeInfoForCreateMember(Map<String, Object> gradeInfo) {
		if (gradeInfo == null || gradeInfo.isEmpty()) {
			return Collections.emptyMap();
		}
		Object gid = gradeInfo.get("grade_id");
		Object cid = gradeInfo.get("company_id");
		Object gradeName = gradeInfo.get("grade_name");
		Object backgroundPicUrl = gradeInfo.get("background_pic_url");
		Object description = gradeInfo.get("description");
		Object gradeBackground = gradeInfo.get("grade_background");
		LinkedHashMap<String, Object> shaped = new LinkedHashMap<>();
		shaped.put("grade_id", gid);
		shaped.put("company_id", cid);
		shaped.put("grade_name", gradeName);
		shaped.put("default_grade", Boolean.TRUE.equals(gradeInfo.get("default_grade")));
		shaped.put("background_pic_url", backgroundPicUrl);
		shaped.put("promotion_condition", parsePromotionConditionForGrade(gradeInfo.get("promotion_condition")));
		shaped.put("privileges", parsePrivilegesObject(gradeInfo.get("privileges")));
		shaped.put("description", description);
		shaped.put("grade_background", gradeBackground);
		return shaped;
	}

	private static List<Object> parsePromotionConditionForGrade(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		if (!(raw instanceof String s) || !StringUtils.hasText(s.trim())) {
			return new ArrayList<>();
		}
		String t = s.trim();
		try {
			Object parsed = JSON.readValue(t, new TypeReference<Object>() {});
			if (parsed instanceof List<?> list) {
				return new ArrayList<>(list);
			}
			if (parsed == null) {
				return new ArrayList<>();
			}
			if (parsed instanceof Map<?, ?> map && map.isEmpty()) {
				return new ArrayList<>();
			}
			if (parsed instanceof Map<?, ?> map) {
				return new ArrayList<>(List.of(map));
			}
			List<Object> single = new ArrayList<>();
			single.add(parsed);
			return single;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static Map<String, Object> parsePrivilegesObject(Object raw) {
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		if (raw instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					out.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				Map<String, Object> parsed =
						JSON.readValue(s.trim(), new TypeReference<Map<String, Object>>() {});
				return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
			} catch (Exception e) {
				return new LinkedHashMap<>();
			}
		}
		return new LinkedHashMap<>();
	}

	private void addDailyMemberActiveRedisSet(long companyId, long userId) {
		String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		sharedStringRedisTemplate
				.opsForSet()
				.add("Member:" + companyId + ":" + ymd, String.valueOf(userId));
	}

	private long parseDistributorIdFromPostData(Map<String, Object> postData) {
		Object raw = postData.get("distributor_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String randomUsername8() {
		String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = 0; i < 8; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private static String randomPassword10() {
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
}
