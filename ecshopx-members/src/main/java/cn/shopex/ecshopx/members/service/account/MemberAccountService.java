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

package cn.shopex.ecshopx.members.service.account;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersDeleteRecord;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.ShopRelMember;
import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.h5.bind.ShoppingGuideForH5BindLookup;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class MemberAccountService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final Pattern H5_RESET_PASSWORD_ALNUM = Pattern.compile("[a-zA-Z0-9]+");

	private static final String WECHAT_RANDOM_PASSWORD_CHARSET =
			"QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";

	private final MembersMapper membersMapper;

	private final MembersInfoMapper membersInfoMapper;

	private final WechatUsersMapper wechatUsersMapper;

	private final MembersAssociationsMapper membersAssociationsMapper;

	private final MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;

	private final BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

	private final StringRedisTemplate sharedStringRedisTemplate;

	private final ShopProtocolSetService shopProtocolSetService;

	private final MembersDeleteRecordMapper membersDeleteRecordMapper;

	private final AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort;

	private final AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort;

	private final ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup;

	private final WorkWechatRelMapper workWechatRelMapper;

	private final ShopRelMemberMapper shopRelMemberMapper;

	private final MemberRegSettingService memberRegSettingService;

	private final DmCrmSettingReadPort dmCrmSettingReadPort;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Value("${ecshopx.h5.encrypt-sensitive-data:false}")
	private boolean encryptSensitiveData;

	public MemberAccountService(
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			WechatUsersMapper wechatUsersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher,
			BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ShopProtocolSetService shopProtocolSetService,
			MembersDeleteRecordMapper membersDeleteRecordMapper,
			@Lazy AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort,
			AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort,
			ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup,
			WorkWechatRelMapper workWechatRelMapper,
			ShopRelMemberMapper shopRelMemberMapper,
			MemberRegSettingService memberRegSettingService,
			DmCrmSettingReadPort dmCrmSettingReadPort) {
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.wechatUsersMapper = wechatUsersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.membersCreateMemberSuccessDispatchPublisher = membersCreateMemberSuccessDispatchPublisher;
		this.bindSalsepersonJobDispatchPublisher = bindSalsepersonJobDispatchPublisher;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.shopProtocolSetService = shopProtocolSetService;
		this.membersDeleteRecordMapper = membersDeleteRecordMapper;
		this.adminMemberCreatePromoterSideEffectPort = adminMemberCreatePromoterSideEffectPort;
		this.adminMemberCreateDistributorUserSideEffectPort = adminMemberCreateDistributorUserSideEffectPort;
		this.shoppingGuideForH5BindLookup = shoppingGuideForH5BindLookup;
		this.workWechatRelMapper = workWechatRelMapper;
		this.shopRelMemberMapper = shopRelMemberMapper;
		this.memberRegSettingService = memberRegSettingService;
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
	}

	public Map<String, Object> getTokenData(Map<String, Object> userInfo) {
		Object uid = userInfo.get("user_id");
		Object cid = userInfo.get("company_id");
		Map<String, Object> m = new HashMap<>();
		m.put("id", String.valueOf(uid) + "_espier_companyid_espier_" + String.valueOf(cid));
		m.put("user_id", uid);
		m.put("company_id", cid);
		m.put("operator_type", "user");
		m.put("unionid", userInfo.get("unionid"));
		m.put("openid", userInfo.get("open_id"));
		m.put("is_new", userInfo.getOrDefault("is_new", 0));
		Object mobileClaim = userInfo.get("mobile");
		if (mobileClaim != null) {
			String mobileTrim = String.valueOf(mobileClaim).trim();
			if (StringUtils.hasText(mobileTrim)) {
				m.put("mobile", mobileTrim);
			}
		}
		return m;
	}

	/**
	 * Resolves stored member mobile to plaintext for H5 flows when the JWT has no {@code mobile} claim.
	 */
	public String resolvePlainMobileForMember(long companyId, long userId) {
		String stored = findMobileStored(companyId, userId);
		if (!StringUtils.hasText(stored)) {
			return "";
		}
		return LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(stored);
	}

	public String encodeMobileForStorage(String mobile) {
		if (!encryptSensitiveData) {
			return mobile;
		}
		return mobile;
	}

	public Members findMemberByCompanyAndMobile(long companyId, String mobilePlain) {
		String stored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain);
		LambdaQueryWrapper<Members> q = new LambdaQueryWrapper<>();
		q.eq(Members::getCompanyId, companyId).eq(Members::getMobile, stored).last("LIMIT 1");
		return membersMapper.selectOne(q);
	}

	public Members findMemberByCompanyAndThirdData(long companyId, String thirdData) {
		if (!StringUtils.hasText(thirdData)) {
			return null;
		}
		LambdaQueryWrapper<Members> q = new LambdaQueryWrapper<>();
		q.eq(Members::getCompanyId, companyId)
				.eq(Members::getThirdData, thirdData.trim())
				.last("LIMIT 1");
		return membersMapper.selectOne(q);
	}

	public long requireUserIdByMobileInCompany(long companyId, String mobilePlain) {
		Members m = findMemberByCompanyAndMobile(companyId, mobilePlain);
		if (m == null) {
			throw new ResourceException("当前手机号还不是会员");
		}
		return m.getUserId();
	}

	public Long findInviterUserId(long companyId, long userId) {
		Members m = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.select(Members::getInviterId)
				.last("LIMIT 1"));
		if (m == null || m.getInviterId() == null || m.getInviterId() <= 0L) {
			return null;
		}
		return m.getInviterId();
	}

	public String findMobileStored(long companyId, long userId) {
		Members m = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.select(Members::getMobile)
				.last("LIMIT 1"));
		if (m == null) {
			return null;
		}
		return m.getMobile();
	}

	public boolean passwordMatches(String rawPassword, String storedHash) {
		if (!StringUtils.hasText(storedHash)) {
			return false;
		}
		return passwordEncoder.matches(rawPassword, storedHash);
	}

	public void createMemberWechatAssociation(long companyId, long userId, String unionId) {
		createMemberPlatformAssociation(companyId, userId, unionId, "wechat");
	}

	public void createMemberPlatformAssociation(
			long companyId, long userId, String unionId, String userType) {
		if (!StringUtils.hasText(unionId)) {
			throw new ResourceException("参数有误！");
		}
		String type = StringUtils.hasText(userType) ? userType.trim() : "wechat";
		MembersAssociations existing =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserType, type)
								.eq(MembersAssociations::getUnionid, unionId)
								.last("LIMIT 1"));
		if (existing != null) {
			if (existing.getUserId() != null && existing.getUserId() == userId) {
				return;
			}
			throw new ResourceException("绑定失败！该微信信息已与其他用户做了绑定！");
		}
		MembersAssociations assoc = new MembersAssociations();
		assoc.setUserId(userId);
		assoc.setUnionid(unionId);
		assoc.setCompanyId(companyId);
		assoc.setUserType(type);
		membersAssociationsMapper.insert(assoc);
	}

	/**
	 * Creates a member for WeChat mini-program bind; does not enqueue {@code BindSalseperson} when
	 * salesperson linkage ({@code work_user_id}, sales channel) is not provided on the request.
	 * After commit, publishes the create-member success dispatch payload for the wxapp bind new-member path.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMemberForWxappBind(
			long companyId,
			String mobilePlain,
			String passwordPlain,
			Map<String, Object> wechatUserRow,
			Map<String, Object> requestExtras) {
		String pwd = passwordPlain == null ? "" : passwordPlain;
		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeId == null) {
			throw new ResourceException("缺少默认等级");
		}
		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain);
		String authorizerAppid = stringVal(wechatUserRow.get("authorizer_appid")).trim();
		String wxaAppid = stringVal(requestExtras.getOrDefault("wxa_appid", "")).trim();
		long inviterId = longParam(requestExtras.get("inviter_id"));
		long sourceId = longParam(requestExtras.get("source_id"));
		long monitorId = longParam(requestExtras.get("monitor_id"));
		long latestSourceId = longParam(requestExtras.get("latest_source_id"));
		long latestMonitorId = longParam(requestExtras.get("latest_monitor_id"));
		String unionid = stringVal(wechatUserRow.get("unionid")).trim();
		String openId = stringVal(wechatUserRow.get("open_id")).trim();

		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());
		Members m = new Members();
		m.setCompanyId(companyId);
		m.setGradeId(gradeId);
		m.setMobile(mobileStored);
		m.setRegionMobile(mobilePlain);
		m.setMobileCountryCode("86");
		m.setPassword(passwordEncoder.encode(pwd));
		m.setUserCardCode(randomCardCode());
		m.setCreated(now);
		m.setUpdated(now);
		m.setDisabled(false);
		m.setCreatedYear(z.getYear());
		m.setCreatedMonth(z.getMonthValue());
		m.setCreatedDay(z.getDayOfMonth());
		if (StringUtils.hasText(authorizerAppid)) {
			m.setAuthorizerAppid(authorizerAppid);
		}
		if (StringUtils.hasText(wxaAppid)) {
			m.setWxaAppid(wxaAppid);
		}
		if (inviterId > 0L) {
			m.setInviterId(inviterId);
		}
		if (sourceId > 0L) {
			m.setSourceId(sourceId);
		}
		if (monitorId > 0L) {
			m.setMonitorId(monitorId);
		}
		if (latestSourceId > 0L) {
			m.setLatestSourceId(latestSourceId);
		}
		if (latestMonitorId > 0L) {
			m.setLatestMonitorId(latestMonitorId);
		}
		membersMapper.insert(m);
		long userId = m.getUserId();

		String nick = stringVal(wechatUserRow.get("nickname")).trim();
		if (!StringUtils.hasText(nick)) {
			nick = randomUsername(8);
		}
		MembersInfo info = new MembersInfo();
		info.setUserId(userId);
		info.setCompanyId(companyId);
		info.setUsername(nick);
		info.setAvatar(stringVal(wechatUserRow.get("headimgurl")).trim());
		info.setSex(0);
		info.setOtherParams("{}");
		info.setCreated(now);
		info.setUpdated(now);
		membersInfoMapper.insert(info);

		boolean ifRegisterPromotion = resolveIfRegisterPromotionForNewMember(companyId, mobilePlain);
		long distributorIdForEvent = 0L;
		Object distRaw = requestExtras.get("distributor_id");
		if (distRaw != null && stringVal(distRaw).trim().length() > 0) {
			long d = longParam(distRaw);
			if (d > 0L) {
				distributorIdForEvent = d;
			}
		}
		long salespersonIdForEvent = longParam(requestExtras.get("salesperson_id"));

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("user_id", userId);
		eventData.put("company_id", companyId);
		eventData.put("mobile", mobilePlain);
		eventData.put("openid", openId);
		eventData.put("wxa_appid", wxaAppid);
		eventData.put("inviter_id", inviterId);
		eventData.put("distributor_id", distributorIdForEvent);
		eventData.put("source_id", sourceId);
		eventData.put("monitor_id", monitorId);
		eventData.put("salesperson_id", salespersonIdForEvent);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("createMemberForWxappBind requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
				String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
				sharedStringRedisTemplate.opsForSet().add("Member:" + companyId + ":" + ymd, String.valueOf(userId));
			}
		});

		createMemberWechatAssociation(companyId, userId, unionid);

		Map<String, Object> row = new HashMap<>();
		row.put("user_id", userId);
		row.put("company_id", companyId);
		return row;
	}

	private boolean resolveIfRegisterPromotionForNewMember(long companyId, String mobilePlain) {
		boolean ifRegisterPromotion = true;
		Map<String, Object> proto = shopProtocolSetService.get(companyId, "member_logout_config", "zh-CN");
		Object block = proto.get("member_logout_config");
		String newRights = "";
		if (block instanceof Map<?, ?> mb) {
			Object nr = mb.get("new_rights");
			newRights = nr == null ? "" : String.valueOf(nr).trim();
		}
		if (newRights.isEmpty() || "0".equals(newRights)) {
			MembersDeleteRecord dr =
					membersDeleteRecordMapper.selectOne(
							new LambdaQueryWrapper<MembersDeleteRecord>()
									.eq(MembersDeleteRecord::getCompanyId, companyId)
									.eq(MembersDeleteRecord::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain))
									.last("LIMIT 1"));
			if (dr != null) {
				ifRegisterPromotion = false;
			}
		}
		return ifRegisterPromotion;
	}

	private static long longParam(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMemberLocalAutoRegister(
			long companyId,
			String mobilePlain,
			String passwordPlain) {
		return createMemberLocalAutoRegister(companyId, mobilePlain, passwordPlain, Collections.emptyMap());
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMemberLocalAutoRegister(
			long companyId,
			String mobilePlain,
			String passwordPlain,
			Map<String, Object> requestExtras) {
		Map<String, Object> ext = requestExtras == null ? Collections.emptyMap() : requestExtras;
		String pwd = passwordPlain == null ? "" : passwordPlain;
		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeId == null) {
			throw new ResourceException("缺少默认等级");
		}
		String wxaAppid = stringVal(ext.getOrDefault("wxa_appid", "")).trim();
		long inviterId = longParam(ext.get("inviter_id"));
		long sourceId = longParam(ext.get("source_id"));
		long monitorId = longParam(ext.get("monitor_id"));
		long latestSourceId = longParam(ext.get("latest_source_id"));
		long latestMonitorId = longParam(ext.get("latest_monitor_id"));
		String authorizerAppid = stringVal(ext.get("authorizer_appid")).trim();
		String alipayAppidForMember = stringVal(ext.get("alipay_appid")).trim();

		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());

		Members existing = findMemberByCompanyAndMobile(companyId, mobilePlain);
		if (existing != null) {
			boolean isNew = false;
			MembersInfo infoBefore =
					membersInfoMapper.selectOne(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getUserId, existing.getUserId())
									.eq(MembersInfo::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (infoBefore != null && otherParamsHasUploadMemberFlag(infoBefore.getOtherParams())) {
				isNew = true;
			}

			Members m = existing;
			long userId = m.getUserId();
			if (!StringUtils.hasText(m.getUserCardCode())) {
				m.setUserCardCode(randomCardCode());
			}
			if (m.getGradeId() == null || m.getGradeId() <= 0L || m.getGradeId() == -1L) {
				m.setGradeId(gradeId);
			}
			m.setRegionMobile(mobilePlain);
			m.setUpdated(now);
			if (StringUtils.hasText(authorizerAppid)) {
				m.setAuthorizerAppid(authorizerAppid);
			}
			if (StringUtils.hasText(wxaAppid)) {
				m.setWxaAppid(wxaAppid);
			}
			if (StringUtils.hasText(alipayAppidForMember)) {
				m.setAlipayAppid(alipayAppidForMember);
			}
			membersMapper.updateById(m);

			MembersInfo info = infoBefore;
			if (info == null) {
				info = new MembersInfo();
				info.setUserId(userId);
				info.setCompanyId(companyId);
				info.setUsername(randomUsername(8));
				info.setOtherParams("[]");
				info.setCreated(now);
				info.setUpdated(now);
				membersInfoMapper.insert(info);
			} else {
				info.setUpdated(now);
				membersInfoMapper.updateById(info);
			}

			long effectiveGradeId = m.getGradeId() != null ? m.getGradeId() : gradeId;
			Map<String, Object> row = new HashMap<>();
			row.put("user_id", userId);
			row.put("company_id", companyId);
			row.put("grade_id", effectiveGradeId);
			row.put("is_new", isNew ? 1 : 0);
			return row;
		}

		Members m = new Members();
		m.setCompanyId(companyId);
		m.setGradeId(gradeId);
		m.setMobile(encodeMobileForStorage(mobilePlain));
		m.setRegionMobile(mobilePlain);
		m.setMobileCountryCode("86");
		m.setPassword(passwordEncoder.encode(pwd));
		m.setUserCardCode(randomCardCode());
		m.setCreated(now);
		m.setUpdated(now);
		m.setDisabled(false);
		m.setCreatedYear(z.getYear());
		m.setCreatedMonth(z.getMonthValue());
		m.setCreatedDay(z.getDayOfMonth());
		if (StringUtils.hasText(authorizerAppid)) {
			m.setAuthorizerAppid(authorizerAppid);
		}
		if (StringUtils.hasText(wxaAppid)) {
			m.setWxaAppid(wxaAppid);
		}
		if (StringUtils.hasText(alipayAppidForMember)) {
			m.setAlipayAppid(alipayAppidForMember);
		}
		if (inviterId > 0L) {
			m.setInviterId(inviterId);
		}
		if (sourceId > 0L) {
			m.setSourceId(sourceId);
		}
		if (monitorId > 0L) {
			m.setMonitorId(monitorId);
		}
		if (latestSourceId > 0L) {
			m.setLatestSourceId(latestSourceId);
		}
		if (latestMonitorId > 0L) {
			m.setLatestMonitorId(latestMonitorId);
		}
		membersMapper.insert(m);
		long userId = m.getUserId();

		MembersInfo info = new MembersInfo();
		info.setUserId(userId);
		info.setCompanyId(companyId);
		info.setUsername(randomUsername(8));
		info.setOtherParams("[]");
		info.setCreated(now);
		info.setUpdated(now);
		membersInfoMapper.insert(info);

		String openIdForEvent = stringVal(ext.getOrDefault("open_id", "")).trim();
		boolean ifRegisterPromotion = resolveIfRegisterPromotionForNewMember(companyId, mobilePlain);
		long distributorIdForEvent = 0L;
		Object distRaw = ext.get("distributor_id");
		if (distRaw != null && stringVal(distRaw).trim().length() > 0) {
			long d = longParam(distRaw);
			if (d > 0L) {
				distributorIdForEvent = d;
			}
		}
		long salespersonIdForEvent = longParam(ext.get("salesperson_id"));

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("user_id", userId);
		eventData.put("company_id", companyId);
		eventData.put("mobile", mobilePlain);
		eventData.put("openid", openIdForEvent);
		eventData.put("wxa_appid", wxaAppid);
		eventData.put("inviter_id", inviterId);
		eventData.put("distributor_id", distributorIdForEvent);
		eventData.put("source_id", sourceId);
		eventData.put("monitor_id", monitorId);
		eventData.put("salesperson_id", salespersonIdForEvent);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("createMemberLocalAutoRegister requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
				String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
				sharedStringRedisTemplate.opsForSet().add("Member:" + companyId + ":" + ymd, String.valueOf(userId));
				String workUseridRaw = stringVal(ext.get("work_userid"));
				if (StringUtils.hasText(workUseridRaw.trim()) && numberOrZero(ext.get("channel")) == 1) {
					// work_userid + channel 1: enqueue bind-salesperson; DM-after-member-create vs standard path per point-integration gate.
					if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
						bindSalsepersonJobDispatchPublisher.enqueueBindSalsepersonAfterDmMemberCreate(
								companyId,
								stringVal(ext.get("unionid")).trim(),
								workUseridRaw.trim(),
								1,
								mobilePlain);
					} else {
						bindSalsepersonJobDispatchPublisher.enqueueBindSalseperson(
								companyId,
								stringVal(ext.get("unionid")).trim(),
								workUseridRaw.trim(),
								1,
								mobilePlain,
								userId);
					}
				}
			}
		});

		Map<String, Object> row = new HashMap<>();
		row.put("user_id", userId);
		row.put("company_id", companyId);
		row.put("grade_id", gradeId);
		row.put("is_new", 1);
		return row;
	}

	public List<Long> listUserIdsByCompanyAndMobile(long companyId, String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return List.of();
		}
		String stored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile.trim());
		Page<Members> mp = new Page<>(1, 100);
		LambdaQueryWrapper<Members> w = new LambdaQueryWrapper<>();
		w.eq(Members::getCompanyId, companyId)
				.eq(Members::getMobile, stored)
				.select(Members::getUserId);
		membersMapper.selectPage(mp, w);
		return mp.getRecords().stream().map(Members::getUserId).toList();
	}

	public List<Long> listUserIdsByUsername(long companyId, String username) {
		if (!StringUtils.hasText(username)) {
			return List.of();
		}
		LambdaQueryWrapper<MembersInfo> w = new LambdaQueryWrapper<>();
		w.eq(MembersInfo::getCompanyId, companyId)
				.eq(MembersInfo::getUsername, username.trim())
				.select(MembersInfo::getUserId);
		return membersInfoMapper.selectList(w).stream().map(MembersInfo::getUserId).distinct().toList();
	}

	public List<Long> listUserIdsByNameContains(long companyId, String name) {
		if (!StringUtils.hasText(name)) {
			return List.of();
		}
		LambdaQueryWrapper<MembersInfo> w = new LambdaQueryWrapper<>();
		w.eq(MembersInfo::getCompanyId, companyId)
				.like(MembersInfo::getName, name.trim())
				.select(MembersInfo::getUserId);
		return membersInfoMapper.selectList(w).stream().map(MembersInfo::getUserId).distinct().toList();
	}

	/**
	 * Password hash column for wide-table merge into promoter payloads (read-only).
	 */
	public String readMemberPasswordStored(long companyId, long userId) {
		if (companyId <= 0L || userId <= 0L) {
			return null;
		}
		Members m =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.select(Members::getPassword)
								.last("LIMIT 1"));
		return m == null ? null : m.getPassword();
	}

	/**
	 * Returns an ordered snake_case map of member columns intended to be merged before promoter fields: keys are
	 * inserted in member-table order first so a later promoter slice can overwrite duplicate keys in place when
	 * the caller assembles the combined row.
	 */
	public LinkedHashMap<String, Object> readMemberWideMergeSlice(long companyId, long userId) {
		if (companyId <= 0L || userId <= 0L) {
			return new LinkedHashMap<>();
		}
		Members mb =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (mb == null) {
			return new LinkedHashMap<>();
		}
		MembersInfo mi =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));
		ZoneId shanghai = ZoneId.of("Asia/Shanghai");
		DateTimeFormatter createdFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", String.valueOf(mb.getUserId()));
		m.put("company_id", String.valueOf(mb.getCompanyId()));
		m.put("grade_id", mb.getGradeId() == null ? "0" : String.valueOf(mb.getGradeId()));
		m.put("mobile", nzEmpty(mb.getMobile()));
		m.put("region_mobile", nzEmpty(mb.getRegionMobile()));
		m.put("mobile_country_code", nzEmpty(mb.getMobileCountryCode()));
		m.put("password", mb.getPassword());
		m.put("user_card_code", nzEmpty(mb.getUserCardCode()));
		m.put("offline_card_code", mb.getOfflineCardCode());
		m.put("authorizer_appid", mb.getAuthorizerAppid());
		m.put("wxa_appid", nzEmpty(mb.getWxaAppid()));
		m.put("inviter_id", String.valueOf(mb.getInviterId() == null ? 0L : mb.getInviterId()));
		m.put("source_from", nzEmpty(mb.getSourceFrom()));
		m.put("source_id", String.valueOf(mb.getSourceId() == null ? 0L : mb.getSourceId()));
		m.put("monitor_id", String.valueOf(mb.getMonitorId() == null ? 0L : mb.getMonitorId()));
		m.put("latest_source_id", String.valueOf(mb.getLatestSourceId() == null ? 0L : mb.getLatestSourceId()));
		m.put("latest_monitor_id", String.valueOf(mb.getLatestMonitorId() == null ? 0L : mb.getLatestMonitorId()));
		m.put("remarks", nzEmpty(mb.getRemarks()));
		m.put("created", mb.getCreated());
		m.put("created_year", mb.getCreatedYear() == null ? "0" : String.valueOf(mb.getCreatedYear()));
		m.put("created_month", mb.getCreatedMonth() == null ? "0" : String.valueOf(mb.getCreatedMonth()));
		m.put("created_day", mb.getCreatedDay() == null ? "0" : String.valueOf(mb.getCreatedDay()));
		m.put("updated", mb.getUpdated() == null ? null : String.valueOf(mb.getUpdated()));
		m.put("disabled", Boolean.TRUE.equals(mb.getDisabled()) ? 1 : 0);
		m.put("use_point", Boolean.TRUE.equals(mb.getUsePoint()) ? "1" : "0");
		m.put("third_data", parseThirdDataLoose(mb.getThirdData()));
		m.put("alipay_appid", nzEmpty(mb.getAlipayAppid()));
		m.put("fp_salesperson", mb.getFpSalesperson());
		m.put("has_fp", Boolean.TRUE.equals(mb.getHasFp()) ? "1" : "0");
		m.put("reg_distributor", String.valueOf(mb.getRegDistributor() == null ? 0 : mb.getRegDistributor()));
		m.put("reg_salesperson", nzEmpty(mb.getRegSalesperson()));
		m.put(
				"is_become_friend",
				String.valueOf(Boolean.TRUE.equals(mb.getIsBecomeFriend()) ? 1 : 0));
		m.put("op_distributor", String.valueOf(mb.getOpDistributor() == null ? 0 : mb.getOpDistributor()));
		m.put("username", mi != null && mi.getUsername() != null ? mi.getUsername() : "");
		m.put("sex", mi != null && mi.getSex() != null ? String.valueOf(mi.getSex()) : "0");
		Long cr = mb.getCreated();
		if (cr != null && cr > 0L) {
			m.put("created_date", Instant.ofEpochSecond(cr).atZone(shanghai).format(createdFmt));
		} else {
			m.put("created_date", "");
		}
		return m;
	}

	private static String nzEmpty(String s) {
		return s == null ? "" : s;
	}

	private static Object parseThirdDataLoose(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return JSON.readValue(raw, Object.class);
		} catch (Exception e) {
			return null;
		}
	}

	public List<Map<String, Object>> listMemberSummariesByUserIds(long companyId, Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		List<Long> ids = userIds.stream().distinct().toList();
		LambdaQueryWrapper<Members> wm = new LambdaQueryWrapper<>();
		wm.eq(Members::getCompanyId, companyId).in(Members::getUserId, ids);
		Map<Long, Members> byUser = membersMapper.selectList(wm).stream()
				.collect(Collectors.toMap(Members::getUserId, m -> m, (a, b) -> a));

		LambdaQueryWrapper<MembersInfo> wi = new LambdaQueryWrapper<>();
		wi.eq(MembersInfo::getCompanyId, companyId).in(MembersInfo::getUserId, ids);
		Map<Long, MembersInfo> infoByUser = membersInfoMapper.selectList(wi).stream()
				.collect(Collectors.toMap(MembersInfo::getUserId, i -> i, (a, b) -> a));

		List<Map<String, Object>> out = new ArrayList<>();
		for (Long uid : ids) {
			Members mb = byUser.get(uid);
			MembersInfo info = infoByUser.get(uid);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("user_id", uid);
			row.put("username", info != null && info.getUsername() != null ? info.getUsername() : "");
			row.put("name", info != null && info.getName() != null ? info.getName() : "");
			row.put("mobile", mb != null && mb.getMobile() != null ? mb.getMobile() : "");
			row.put(
					"user_card_code",
					mb != null && mb.getUserCardCode() != null ? mb.getUserCardCode() : "");
			out.add(row);
		}
		return out;
	}

	public Map<Long, Map<String, String>> batchWechatNicknameHeadByUserIds(
			long companyId, Collection<Long> userIds) {
		LinkedHashMap<Long, Map<String, String>> out = new LinkedHashMap<>();
		if (userIds == null || userIds.isEmpty()) {
			return out;
		}
		for (Long uid : userIds.stream().filter(Objects::nonNull).filter(id -> id > 0L).distinct().toList()) {
			out.put(uid, resolveWechatNicknameHead(companyId, uid));
		}
		return out;
	}

	private Map<String, String> resolveWechatNicknameHead(long companyId, long userId) {
		LinkedHashMap<String, String> pair = new LinkedHashMap<>();
		pair.put("nickname", "");
		pair.put("headimgurl", "");
		Map<String, Object> assoc = getMembersAssociationByUserId(companyId, "wechat", userId);
		if (assoc.isEmpty()) {
			return pair;
		}
		String unionid = stringVal(assoc.get("unionid"));
		if (!StringUtils.hasText(unionid)) {
			return pair;
		}
		Map<String, Object> wxFilter = new HashMap<>();
		wxFilter.put("company_id", companyId);
		wxFilter.put("unionid", unionid);
		Map<String, Object> wx = getWechatSimpleUser(wxFilter);
		if (!wx.isEmpty()) {
			pair.put("nickname", stringVal(wx.get("nickname")));
			pair.put("headimgurl", stringVal(wx.get("headimgurl")));
		}
		return pair;
	}

	/**
	 * Plain mobile string for H5 contexts (link encryption, auth echo): prefers {@code region_mobile},
	 * otherwise the stored {@code mobile} column.
	 */
	public String resolveMemberMobileForH5Context(long userId, long companyId) {
		if (userId <= 0L || companyId <= 0L) {
			return "";
		}
		return effectiveMobileFromMemberMap(getMemberInfo(userId, companyId));
	}

	private static String effectiveMobileFromMemberMap(Map<String, Object> member) {
		if (member == null || member.isEmpty()) {
			return "";
		}
		Object reg = member.get("region_mobile");
		if (reg != null && StringUtils.hasText(reg.toString().trim())) {
			return reg.toString().trim();
		}
		Object m = member.get("mobile");
		if (m != null) {
			return m.toString().trim();
		}
		return "";
	}

	public Map<String, Object> getMemberRowForAdminFilter(long companyId, Map<String, Object> filter) {
		if (filter == null) {
			return Collections.emptyMap();
		}
		boolean hasUser = filter.containsKey("user_id") && filter.get("user_id") instanceof Long;
		Object mobileObj = filter.get("mobile");
		boolean hasMobile =
				mobileObj instanceof String ms && StringUtils.hasText(ms.trim());
		String mobileEnc = null;
		if (hasMobile) {
			mobileEnc = LegacyFixedMobileEncrypt.fixedEncryptMobile(((String) mobileObj).trim());
		}
		Members mb = null;
		if (hasUser && hasMobile) {
			mb = membersMapper.selectMemberRowForAdminByCompanyUserIdAndMobileEnc(
					companyId, (Long) filter.get("user_id"), mobileEnc);
		} else if (hasUser) {
			mb = membersMapper.selectMemberRowForAdminByCompanyAndUserId(companyId, (Long) filter.get("user_id"));
		} else if (hasMobile) {
			mb = membersMapper.selectMemberRowForAdminByCompanyAndMobileEnc(companyId, mobileEnc);
		}
		if (mb == null) {
			return Collections.emptyMap();
		}
		LinkedHashMap<String, Object> out = mapMembersTable(mb);
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getUserId, mb.getUserId())
								.eq(MembersInfo::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (info != null) {
			out.putAll(toMembersInfoApiMap(info, false));
		}
		return out;
	}

	public Map<String, Object> getMemberInfoForOpenapiFilter(
			long companyId, String mobilePlain, String externalMemberIdRaw) {
		boolean hasMobile = mobilePlain != null && !mobilePlain.isEmpty() && !"0".equals(mobilePlain);
		Long userId = parseExternalMemberIdForOpenapi(externalMemberIdRaw);
		Members mb = null;
		String mobileEnc = null;
		if (hasMobile) {
			mobileEnc = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain.trim());
		}
		if (hasMobile && userId != null) {
			mb = membersMapper.selectMemberRowForAdminByCompanyUserIdAndMobileEnc(
					companyId, userId, mobileEnc);
		} else if (userId != null) {
			mb = membersMapper.selectMemberRowForAdminByCompanyAndUserId(companyId, userId);
		} else if (hasMobile) {
			mb = membersMapper.selectMemberRowForAdminByCompanyAndMobileEnc(companyId, mobileEnc);
		}
		if (mb == null) {
			return Collections.emptyMap();
		}
		LinkedHashMap<String, Object> out = mapMembersTable(mb);
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getUserId, mb.getUserId())
								.eq(MembersInfo::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (info != null) {
			out.putAll(toMembersInfoApiMap(info, false));
		}
		return out;
	}

	public Map<String, Object> getMemberInfo(long userId, long companyId) {
		Members mb = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getUserId, userId)
				.eq(Members::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (mb == null) {
			return Map.of();
		}
		LinkedHashMap<String, Object> out = mapMembersTable(mb);
		MembersInfo info = membersInfoMapper.selectOne(new LambdaQueryWrapper<MembersInfo>()
				.eq(MembersInfo::getUserId, userId)
				.eq(MembersInfo::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (info != null) {
			out.putAll(toMembersInfoApiMap(info));
		}
		out.put("requestFields", new ArrayList<Object>());
		out.put("datapassRequestFields", new ArrayList<Object>());
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> resetMemberPassword(long companyId, long authUserId, Map<String, Object> merged) {
		String mobileRaw = stringVal(merged.get("mobile")).trim();
		if (!StringUtils.hasText(mobileRaw)) {
			throw new BadRequestException("手机号必填！");
		}
		String pwd = stringVal(merged.get("password")).trim();
		if (!StringUtils.hasText(pwd)) {
			throw new BadRequestException("密码必填！");
		}
		if (!H5_RESET_PASSWORD_ALNUM.matcher(pwd).matches()) {
			throw new BadRequestException("密码只能是字母和数字的组合！");
		}
		if (pwd.length() < 6 || pwd.length() > 16) {
			throw new BadRequestException("密码长度6～16个字符之间！");
		}

		Members m;
		String redisPhoneForVcode;
		if (authUserId > 0L) {
			m = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
					.eq(Members::getCompanyId, companyId)
					.eq(Members::getUserId, authUserId)
					.last("LIMIT 1"));
			if (m == null) {
				throw new ResourceException("该手机号还没有注册");
			}
			redisPhoneForVcode = m.getMobile();
		} else {
			String plainMobile = stringVal(merged.get("mobile")).trim();
			m = findMemberByCompanyAndMobile(companyId, plainMobile);
			if (m == null) {
				throw new ResourceException("该手机号还没有注册");
			}
			redisPhoneForVcode = plainMobile;
		}

		boolean shouldVerifySms = merged.containsKey("vcode") && merged.get("vcode") != null;
		if (shouldVerifySms) {
			String vcode = String.valueOf(merged.get("vcode")).trim();
			if (!memberRegSettingService.checkSmsVcode(redisPhoneForVcode, companyId, vcode, "forgot_password")) {
				throw new ResourceException("短信验证码错误");
			}
		}

		boolean wechatRandomPwd = merged.containsKey("api_from")
				&& merged.get("api_from") != null
				&& "wechat".equals(String.valueOf(merged.get("api_from")).trim());
		String pwdPlain = pwd;
		if (wechatRandomPwd) {
			pwdPlain = shuffleCharsetSubstringWechatStyle();
		}

		m.setPassword(passwordEncoder.encode(pwdPlain));
		long nowEpochSec = System.currentTimeMillis() / 1000L;
		m.setUpdated(nowEpochSec);
		int rows = membersMapper.updateById(m);
		if (rows == 0) {
			throw new ResourceException("更新的用户不存在！");
		}
		Map<String, Object> result = getMemberInfo(m.getUserId(), companyId);
		result.put("updated", m.getUpdated());
		return result;
	}

	private static String shuffleCharsetSubstringWechatStyle() {
		char[] arr = WECHAT_RANDOM_PASSWORD_CHARSET.toCharArray();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = arr.length - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			char tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
		}
		return new String(arr).substring(5, 15);
	}

	/**
	 * Member {@code authInfo} map echoed on H5 success payloads (compact guard shape).
	 */
	public Map<String, Object> buildH5AuthInfoForResponse(long userId, long companyId) {
		Map<String, Object> member = getMemberInfo(userId, companyId);
		String uidStr = String.valueOf(userId);
		String cidStr = String.valueOf(companyId);

		String wxappAppid = "";
		String woaAppid = "";
		String openId = "";
		String unionid = "";
		String nickname = "";
		String headimgurl = "";
		String gradeIdStr = "0";
		String mobile = "";
		String username = "";
		String userCardCode = "";
		Object offlineCardCode = null;
		boolean disabled = false;
		String inviterIdStr = "0";
		String sourceIdStr = "0";
		String monitorIdStr = "0";
		String latestSourceIdStr = "0";
		String latestMonitorIdStr = "0";
		String chiefIdStr = "0";
		String alipayAppid = "";
		String alipayUserId = "";

		if (!member.isEmpty()) {
			disabled = Boolean.TRUE.equals(member.get("disabled"));
			gradeIdStr = stringifyAuthNumeric(member.get("grade_id"));
			mobile = effectiveMobileFromMemberMap(member);
			String u = stringVal(member.get("username")).trim();
			if (StringUtils.hasText(u)) {
				username = u;
			} else {
				Object nameVal = member.get("name");
				if (nameVal != null && StringUtils.hasText(nameVal.toString().trim())) {
					username = nameVal.toString().trim();
				}
			}
			Object ucc = member.get("user_card_code");
			userCardCode = ucc != null ? String.valueOf(ucc) : "";
			offlineCardCode = member.get("offline_card_code");
			Object wxa = member.get("wxa_appid");
			wxappAppid = wxa != null ? String.valueOf(wxa) : "";
			Object authApp = member.get("authorizer_appid");
			woaAppid = authApp != null ? String.valueOf(authApp) : "";
			Object aliApp = member.get("alipay_appid");
			alipayAppid = aliApp != null ? String.valueOf(aliApp) : "";
			inviterIdStr = stringifyAuthNumeric(member.get("inviter_id"));
			sourceIdStr = stringifyAuthNumeric(member.get("source_id"));
			monitorIdStr = stringifyAuthNumeric(member.get("monitor_id"));
			latestSourceIdStr = stringifyAuthNumeric(member.get("latest_source_id"));
			latestMonitorIdStr = stringifyAuthNumeric(member.get("latest_monitor_id"));
			chiefIdStr = resolveChiefIdStringForAuthInfo(member);
		}

		Map<String, Object> assoc = getMembersAssociationByUserId(companyId, "wechat", userId);
		if (!assoc.isEmpty()) {
			unionid = stringVal(assoc.get("unionid"));
			if (StringUtils.hasText(unionid)) {
				Map<String, Object> wxFilter = new HashMap<>();
				wxFilter.put("company_id", companyId);
				wxFilter.put("unionid", unionid);
				Map<String, Object> wx = getWechatSimpleUser(wxFilter);
				if (!wx.isEmpty()) {
					openId = stringVal(wx.get("open_id"));
					nickname = stringVal(wx.get("nickname"));
					headimgurl = stringVal(wx.get("headimgurl"));
				}
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", uidStr);
		out.put("user_id", uidStr);
		out.put("disabled", disabled);
		out.put("company_id", cidStr);
		out.put("wxapp_appid", wxappAppid);
		out.put("woa_appid", woaAppid);
		out.put("open_id", openId);
		out.put("unionid", unionid);
		out.put("nickname", nickname);
		out.put("headimgurl", headimgurl);
		out.put("grade_id", gradeIdStr);
		out.put("mobile", mobile);
		out.put("username", username);
		out.put("user_card_code", userCardCode);
		out.put("offline_card_code", offlineCardCode);
		out.put("operator_type", "user");
		out.put("inviter_id", inviterIdStr);
		out.put("source_id", sourceIdStr);
		out.put("monitor_id", monitorIdStr);
		out.put("latest_source_id", latestSourceIdStr);
		out.put("latest_monitor_id", latestMonitorIdStr);
		out.put("chief_id", chiefIdStr);
		out.put("alipay_appid", alipayAppid);
		out.put("alipay_user_id", alipayUserId);
		out.put("api_from", "h5app");
		return out;
	}

	private static String stringifyAuthNumeric(Object v) {
		if (v == null) {
			return "0";
		}
		if (v instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		String s = String.valueOf(v).trim();
		return StringUtils.hasText(s) ? s : "0";
	}

	private static String resolveChiefIdStringForAuthInfo(Map<String, Object> member) {
		Object op = member.get("other_params");
		if (op instanceof Map<?, ?> om) {
			Object c = om.get("chief_id");
			if (c != null && StringUtils.hasText(String.valueOf(c).trim())) {
				return String.valueOf(c).trim();
			}
		}
		Object td = member.get("third_data");
		if (td instanceof String raw && StringUtils.hasText(raw)) {
			try {
				Map<String, Object> parsed = JSON.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
				Object c = parsed.get("chief_id");
				if (c != null && StringUtils.hasText(String.valueOf(c).trim())) {
					return String.valueOf(c).trim();
				}
			} catch (Exception ignored) {
				// ignore malformed third_data
			}
		}
		return "0";
	}

	public static LinkedHashMap<String, Object> mapMembersTable(Members mb) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", mb.getUserId());
		m.put("company_id", mb.getCompanyId());
		m.put("grade_id", mb.getGradeId());
		m.put("mobile", mb.getMobile());
		m.put("region_mobile", mb.getRegionMobile());
		m.put("mobile_country_code", mb.getMobileCountryCode());
		m.put("user_card_code", mb.getUserCardCode());
		m.put("offline_card_code", mb.getOfflineCardCode());
		m.put("inviter_id", longOrZero(mb.getInviterId()));
		m.put("source_from", stringOrDefault(mb.getSourceFrom(), "default"));
		m.put("source_id", mb.getSourceId());
		m.put("monitor_id", mb.getMonitorId());
		m.put("latest_source_id", mb.getLatestSourceId());
		m.put("latest_monitor_id", mb.getLatestMonitorId());
		m.put("authorizer_appid", mb.getAuthorizerAppid());
		m.put("use_point", Boolean.TRUE.equals(mb.getUsePoint()));
		m.put("wxa_appid", mb.getWxaAppid());
		m.put("alipay_appid", mb.getAlipayAppid());
		m.put("created", longOrZero(mb.getCreated()));
		m.put("updated", longOrZero(mb.getUpdated()));
		m.put("disabled", Boolean.TRUE.equals(mb.getDisabled()));
		m.put("remarks", mb.getRemarks());
		m.put("third_data", mb.getThirdData());
		m.put("reg_distributor", mb.getRegDistributor() != null ? mb.getRegDistributor() : 0);
		m.put("reg_salesperson", mb.getRegSalesperson());
		m.put("fp_salesperson", mb.getFpSalesperson());
		m.put("has_fp", Boolean.TRUE.equals(mb.getHasFp()));
		m.put("is_become_friend", Boolean.TRUE.equals(mb.getIsBecomeFriend()));
		m.put("op_distributor", mb.getOpDistributor() != null ? mb.getOpDistributor() : 0);
		return m;
	}

	/**
	 * @param structuredOtherParamsForApi {@code true}: {@code other_params} as nested map/list for API JSON;
	 *     {@code false}: {@code other_params} as the canonical JSON string stored on the member profile.
	 */
	public LinkedHashMap<String, Object> toMembersInfoApiMap(MembersInfo info, boolean structuredOtherParamsForApi) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", info.getUserId());
		m.put("company_id", info.getCompanyId());
		m.put("username", info.getUsername());
		m.put("name", info.getName());
		m.put("avatar", info.getAvatar());
		m.put("sex", info.getSex() != null ? info.getSex() : 0);
		m.put("birthday", info.getBirthday());
		m.put("address", info.getAddress());
		m.put("email", info.getEmail());
		m.put("industry", info.getIndustry());
		m.put("income", info.getIncome());
		m.put("edu_background", info.getEduBackground());
		m.put("habbit", parseHabbitList(info.getHabbit()));
		m.put("created", longOrZero(info.getCreated()));
		m.put("updated", longOrZero(info.getUpdated()));
		m.put("have_consume", Boolean.TRUE.equals(info.getHaveConsume()));
		String otherParamsRaw = canonicalEmptyOtherParamsAsBracketArray(info.getOtherParams());
		Object decoded = decodeOtherParams(otherParamsRaw);
		Object normalizedForWxFlag = normalizeOtherParamsForApi(decoded);
		if (structuredOtherParamsForApi) {
			m.put("other_params", normalizedForWxFlag);
		} else {
			m.put("other_params", otherParamsRaw);
		}
		m.put("isGetWxInfo", resolveIsGetWxInfoFlagFromNormalizedOtherParams(normalizedForWxFlag));
		m.put("dm_member_id", info.getDmMemberId());
		m.put("dm_card_no", info.getDmCardNo());
		return m;
	}

	public LinkedHashMap<String, Object> toMembersInfoApiMap(MembersInfo info) {
		return toMembersInfoApiMap(info, true);
	}

	public LinkedHashMap<String, Object> toMembersInfoOpenapiListMap(MembersInfo info) {
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
		m.put("habbit", parseHabbitList(info.getHabbit()));
		m.put("created", longOrZero(info.getCreated()));
		m.put("updated", longOrZero(info.getUpdated()));
		m.put("have_consume", Boolean.TRUE.equals(info.getHaveConsume()));
		m.put("other_params", canonicalEmptyOtherParamsAsBracketArray(info.getOtherParams()));
		m.put("dm_member_id", info.getDmMemberId());
		m.put("dm_card_no", info.getDmCardNo());
		return m;
	}

	public LinkedHashMap<String, Object> toMembersInfoForMemberEditInfoBaseMap(
			MembersInfo info,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", info.getUserId());
		m.put("company_id", info.getCompanyId());
		String rawUsername = info.getUsername();
		m.put(
				"username",
				rawUsername == null ? "" : sensitiveFieldEncryptor.decrypt(String.valueOf(rawUsername)));
		m.put("name", info.getName());
		m.put("avatar", info.getAvatar());
		m.put("sex", info.getSex() != null ? info.getSex() : 0);
		m.put("birthday", info.getBirthday());
		m.put("address", info.getAddress());
		m.put("email", info.getEmail());
		m.put("industry", info.getIndustry());
		m.put("income", info.getIncome());
		m.put("edu_background", info.getEduBackground());
		m.put("habbit", info.getHabbit());
		m.put("created", longOrZero(info.getCreated()));
		m.put("updated", longOrZero(info.getUpdated()));
		m.put("have_consume", Boolean.TRUE.equals(info.getHaveConsume()));
		String otherParamsRaw = canonicalEmptyOtherParamsAsBracketArray(info.getOtherParams());
		m.put("other_params", otherParamsRaw);
		m.put("dm_member_id", info.getDmMemberId());
		m.put("dm_card_no", info.getDmCardNo());
		return m;
	}

	private static boolean resolveIsGetWxInfoFlagFromNormalizedOtherParams(Object normalizedOtherParams) {
		if (normalizedOtherParams instanceof Map<?, ?> om) {
			Object v = om.get("isGetWxInfo");
			return Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
		}
		return false;
	}

	private static long longOrZero(Long v) {
		return v != null ? v : 0L;
	}

	/** Empty extended fields are stored and echoed as a JSON array literal. */
	private static String canonicalEmptyOtherParamsAsBracketArray(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "[]";
		}
		String t = raw.trim();
		if ("[]".equals(t) || "{}".equals(t)) {
			return "[]";
		}
		try {
			Object v = JSON.readValue(t, new TypeReference<Object>() {});
			if (v == null) {
				return "[]";
			}
			if (v instanceof Map<?, ?> m) {
				return m.isEmpty() ? "[]" : t;
			}
			if (v instanceof List<?> list) {
				return list.isEmpty() ? "[]" : t;
			}
			return "[]";
		} catch (Exception e) {
			return "[]";
		}
	}

	private static Object decodeOtherParams(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<Object>();
		}
		try {
			Object v = JSON.readValue(raw.trim(), new TypeReference<Object>() {});
			if (v == null) {
				return new ArrayList<Object>();
			}
			if (v instanceof Map<?, ?> || v instanceof List<?>) {
				return v;
			}
			return new ArrayList<Object>();
		} catch (Exception e) {
			return new ArrayList<Object>();
		}
	}

	/**
	 * Shapes stored {@code other_params} JSON for API responses: nested object/array values, not a JSON string.
	 * Empty associative {@code custom_data} is represented as an empty JSON array to match legacy list semantics.
	 */
	private static Object normalizeOtherParamsForApi(Object decoded) {
		if (decoded instanceof List<?> list) {
			if (!list.isEmpty()) {
				return decoded;
			}
			LinkedHashMap<String, Object> d = new LinkedHashMap<>();
			d.put("custom_data", new ArrayList<Object>());
			d.put("isGetWxInfo", Boolean.FALSE);
			return d;
		}
		if (!(decoded instanceof Map<?, ?> rawMap)) {
			LinkedHashMap<String, Object> d = new LinkedHashMap<>();
			d.put("custom_data", new ArrayList<Object>());
			d.put("isGetWxInfo", Boolean.FALSE);
			return d;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : rawMap.entrySet()) {
			if (e.getKey() == null) {
				continue;
			}
			String k = String.valueOf(e.getKey());
			if ("custom_data".equals(k)) {
				out.put(k, normalizeCustomDataForApi(e.getValue()));
			} else {
				out.put(k, e.getValue());
			}
		}
		if (!out.containsKey("custom_data")) {
			out.put("custom_data", new ArrayList<Object>());
		}
		return out;
	}

	private static Object normalizeCustomDataForApi(Object v) {
		if (v == null) {
			return new ArrayList<Object>();
		}
		if (v instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				return new ArrayList<Object>();
			}
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					copy.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return copy;
		}
		if (v instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		return new ArrayList<Object>();
	}

	private static List<Object> parseHabbitList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<>();
		}
		String s = raw.trim();
		try {
			Object v = JSON.readValue(s, new TypeReference<Object>() {});
			if (v instanceof List<?> list) {
				return new ArrayList<>(list);
			}
			if (v instanceof Map<?, ?>) {
				return new ArrayList<>();
			}
		} catch (Exception ignored) {
			// fall through
		}
		return new ArrayList<>();
	}

	public Map<String, Object> getWechatSimpleUser(Map<String, Object> filter) {
		WechatUsers u = findWechatUser(filter);
		if (u == null) {
			return Map.of();
		}
		return wechatUserToMap(u);
	}

	public Map<String, Object> getWechatUserInfo(Map<String, Object> filter) {
		Map<String, Object> userFilter = new HashMap<>(filter);
		if (filter.containsKey("user_id") && filter.get("user_id") != null
				&& toLong(filter.get("user_id")) > 0) {
			MembersAssociations assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
					.eq(MembersAssociations::getUserId, toLong(filter.get("user_id")))
					.eq(MembersAssociations::getUserType, "wechat")
					.last("LIMIT 1"));
			if (assoc == null) {
				return Map.of();
			}
			userFilter = new HashMap<>();
			userFilter.put("company_id", assoc.getCompanyId());
			userFilter.put("unionid", assoc.getUnionid());
			if (filter.get("authorizer_appid") != null && StringUtils.hasText(String.valueOf(filter.get("authorizer_appid")))) {
				userFilter.put("authorizer_appid", String.valueOf(filter.get("authorizer_appid")));
			}
		}
		WechatUsers entity = findWechatUser(userFilter);
		if (entity == null) {
			return Map.of();
		}
		Map<String, Object> result = wechatUserToMap(entity);
		MembersAssociations assoc2 = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getCompanyId, entity.getCompanyId())
				.eq(MembersAssociations::getUnionid, entity.getUnionid())
				.eq(MembersAssociations::getUserType, "wechat")
				.last("LIMIT 1"));
		if (assoc2 != null) {
			result.put("user_id", assoc2.getUserId());
		} else {
			result.put("user_id", "");
		}
		return result;
	}

	public void updateWechatUnionId(
			long companyId,
			String authorizerAppid,
			String openId,
			String oldUnionid,
			String newUnionid) {
		long now = System.currentTimeMillis() / 1000L;
		wechatUsersMapper.update(null, new LambdaUpdateWrapper<WechatUsers>()
				.eq(WechatUsers::getCompanyId, companyId)
				.eq(WechatUsers::getAuthorizerAppid, authorizerAppid)
				.eq(WechatUsers::getOpenId, openId)
				.set(WechatUsers::getUnionid, newUnionid)
				.set(WechatUsers::getNeedTransfer, false)
				.set(WechatUsers::getUpdated, now));
		membersAssociationsMapper.update(null, new LambdaUpdateWrapper<MembersAssociations>()
				.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUnionid, oldUnionid)
				.eq(MembersAssociations::getUserType, "wechat")
				.set(MembersAssociations::getUnionid, newUnionid));
	}

	public Map<String, Object> createOffiaccountFans(long companyId, String woaAppid, Map<String, Object> fanParams) {
		Map<String, Object> p = new HashMap<>();
		p.put("company_id", companyId);
		p.put("open_id", fanParams.get("openid"));
		p.put("unionid", fanParams.get("unionid"));
		p.put("nickname", fanParams.getOrDefault("nickname", "-"));
		p.put("headimgurl", fanParams.get("headimgurl"));
		p.put("inviter_id", numberOrZero(fanParams.get("inviter_id")));
		p.put("source_id", numberOrZero(fanParams.get("source_id")));
		p.put("monitor_id", numberOrZero(fanParams.get("monitor_id")));
		p.put("source_from", stringOrDefault(fanParams.get("source_from"), "default"));
		return createWxappFans(woaAppid, p);
	}

	public Map<String, Object> createWxappFans(String authorizerAppId, Map<String, Object> params) {
		long companyId = toLong(params.get("company_id"));
		String openId = stringVal(params.get("open_id"));
		String unionid = stringVal(params.get("unionid"));
		if (companyId <= 0 || !StringUtils.hasText(openId) || !StringUtils.hasText(unionid)) {
			throw new ResourceException("公司信息错误");
		}
		long now = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<WechatUsers> w = new LambdaQueryWrapper<>();
		w.eq(WechatUsers::getCompanyId, companyId)
				.eq(WechatUsers::getAuthorizerAppid, authorizerAppId)
				.eq(WechatUsers::getOpenId, openId)
				.eq(WechatUsers::getUnionid, unionid);
		WechatUsers existing = wechatUsersMapper.selectOne(w);
		WechatUsers u = existing != null ? existing : new WechatUsers();
		u.setCompanyId(companyId);
		u.setAuthorizerAppid(authorizerAppId);
		u.setOpenId(openId);
		u.setUnionid(unionid);
		u.setNickname(stringVal(params.getOrDefault("nickname", "-")));
		u.setHeadimgurl(stringVal(params.get("headimgurl")));
		u.setInviterId(numberOrZero(params.get("inviter_id")));
		u.setSourceFrom(stringOrDefault(params.get("source_from"), "default"));
		u.setNeedTransfer(false);
		u.setCreated(existing != null ? existing.getCreated() : now);
		u.setUpdated(now);
		if (existing == null) {
			wechatUsersMapper.insert(u);
		} else {
			wechatUsersMapper.update(null, new LambdaUpdateWrapper<WechatUsers>()
					.eq(WechatUsers::getCompanyId, companyId)
					.eq(WechatUsers::getAuthorizerAppid, authorizerAppId)
					.eq(WechatUsers::getOpenId, openId)
					.eq(WechatUsers::getUnionid, existing.getUnionid())
					.set(WechatUsers::getUnionid, unionid)
					.set(WechatUsers::getNickname, u.getNickname())
					.set(WechatUsers::getHeadimgurl, u.getHeadimgurl())
					.set(WechatUsers::getInviterId, u.getInviterId())
					.set(WechatUsers::getSourceFrom, u.getSourceFrom())
					.set(WechatUsers::getUpdated, now));
		}
		MembersAssociations assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUserType, "wechat")
				.eq(MembersAssociations::getUnionid, unionid)
				.last("LIMIT 1"));
		long bindMemberUserId = toLong(params.get("bind_member_user_id"));
		if (assoc == null && bindMemberUserId > 0L) {
			MembersAssociations na = new MembersAssociations();
			na.setUserId(bindMemberUserId);
			na.setUnionid(unionid);
			na.setCompanyId(companyId);
			na.setUserType("wechat");
			membersAssociationsMapper.insert(na);
			assoc = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
					.eq(MembersAssociations::getCompanyId, companyId)
					.eq(MembersAssociations::getUserType, "wechat")
					.eq(MembersAssociations::getUnionid, unionid)
					.last("LIMIT 1"));
		}
		Map<String, Object> memberInfo = null;
		if (assoc != null && assoc.getUserId() != null) {
			memberInfo = getMemberInfo(assoc.getUserId(), companyId);
		}
		Map<String, Object> out = new HashMap<>();
		out.put("wechatuser", wechatUserToMap(u));
		out.put("memberInfo", memberInfo);
		out.put("is_new", assoc == null ? 1 : 0);
		return out;
	}

	public Map<String, Object> getInfoByMobile(long companyId, String mobilePlain) {
		Members m = findMemberByCompanyAndMobile(companyId, mobilePlain);
		if (m == null) {
			return Map.of();
		}
		return getMemberInfo(m.getUserId(), companyId);
	}

	public Map<String, Object> getMembersAssociationByUserId(long companyId, String userType, long userId) {
		MembersAssociations a = membersAssociationsMapper.selectOne(new LambdaQueryWrapper<MembersAssociations>()
				.eq(MembersAssociations::getCompanyId, companyId)
				.eq(MembersAssociations::getUserType, userType)
				.eq(MembersAssociations::getUserId, userId)
				.last("LIMIT 1"));
		if (a == null) {
			return Map.of();
		}
		Map<String, Object> m = new HashMap<>();
		m.put("user_id", a.getUserId());
		m.put("company_id", a.getCompanyId());
		m.put("unionid", a.getUnionid());
		m.put("user_type", a.getUserType());
		return m;
	}

	@Transactional(rollbackFor = Exception.class)
	public void memberInfoUpdate(long userId, long companyId, Map<String, Object> postData) {
		if (postData == null) {
			throw new ResourceException("请填写数据!");
		}
		MembersInfo info =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getUserId, userId)
								.eq(MembersInfo::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (info == null) {
			throw new ResourceException("用户不存在");
		}
		if (!postData.containsKey("other_params")) {
			info.setOtherParams("[]");
		} else {
			mergeOtherParamsForMemberInfoUpdate(info, postData.get("other_params"));
		}
		long now = System.currentTimeMillis() / 1000L;
		applyMemberInfoScalarUpdatesFromPost(info, postData, now);
		int rows = membersInfoMapper.updateById(info);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	@SuppressWarnings("unchecked")
	private void mergeOtherParamsForMemberInfoUpdate(MembersInfo info, Object otherParamsRaw) {
		LinkedHashMap<String, Object> oldFull = readOtherParamsAsMap(info.getOtherParams());
		LinkedHashMap<String, Object> newOp = new LinkedHashMap<>();
		if (otherParamsRaw instanceof Map<?, ?> nm) {
			for (Map.Entry<?, ?> e : nm.entrySet()) {
				if (e.getKey() != null) {
					newOp.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
		}
		if (oldFull.containsKey("custom_data") && newOp.containsKey("custom_data")) {
			LinkedHashMap<String, Object> mergedCd = new LinkedHashMap<>();
			putAllStringKeyed(mergedCd, oldFull.get("custom_data"));
			LinkedHashMap<String, Object> overlay = new LinkedHashMap<>();
			putAllStringKeyed(overlay, newOp.get("custom_data"));
			mergedCd.putAll(overlay);
			newOp.put("custom_data", mergedCd);
			oldFull.remove("custom_data");
		}
		LinkedHashMap<String, Object> top = new LinkedHashMap<>(oldFull);
		top.putAll(newOp);
		info.setOtherParams(writeOtherParamsMap(top));
	}

	private static void putAllStringKeyed(LinkedHashMap<String, Object> target, Object raw) {
		if (!(raw instanceof Map<?, ?> m)) {
			return;
		}
		for (Map.Entry<?, ?> e : m.entrySet()) {
			if (e.getKey() != null) {
				target.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
	}

	private void applyMemberInfoScalarUpdatesFromPost(MembersInfo info, Map<String, Object> postData, long now) {
		if (postData.containsKey("username")) {
			info.setUsername(stringVal(postData.get("username")));
		}
		if (postData.containsKey("name")) {
			info.setName(stringVal(postData.get("name")));
		}
		if (postData.containsKey("avatar")) {
			info.setAvatar(stringVal(postData.get("avatar")));
		}
		if (postData.containsKey("sex")) {
			info.setSex(parseSexInt(postData.get("sex")));
		}
		if (postData.containsKey("birthday")) {
			Object b = postData.get("birthday");
			if (b != null && StringUtils.hasText(String.valueOf(b).trim())) {
				String bstr = String.valueOf(b).trim();
				info.setBirthday(bstr);
				String[] parts = bstr.split("-");
				if (parts.length >= 3) {
					try {
						info.setYear(Integer.parseInt(parts[0].trim()));
						info.setMonth(Integer.parseInt(parts[1].trim()));
						info.setDay(Integer.parseInt(parts[2].trim()));
					} catch (NumberFormatException ignored) {
						// keep birthday string only
					}
				}
			}
		}
		if (postData.containsKey("address")) {
			info.setAddress(stringVal(postData.get("address")));
		}
		if (postData.containsKey("email")) {
			info.setEmail(stringVal(postData.get("email")));
		}
		if (postData.containsKey("industry")) {
			info.setIndustry(stringVal(postData.get("industry")));
		}
		if (postData.containsKey("income")) {
			info.setIncome(stringVal(postData.get("income")));
		}
		if (postData.containsKey("edu_background")) {
			info.setEduBackground(stringVal(postData.get("edu_background")));
		}
		if (postData.containsKey("habbit")) {
			info.setHabbit(habbitToStoredString(postData.get("habbit")));
		}
		if (postData.containsKey("have_consume")) {
			info.setHaveConsume(toBooleanObject(postData.get("have_consume")));
		}
		info.setUpdated(now);
	}

	private static String habbitToStoredString(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		if (raw instanceof Collection<?> c) {
			try {
				return JSON.writeValueAsString(c);
			} catch (Exception e) {
				return String.valueOf(raw);
			}
		}
		try {
			return JSON.writeValueAsString(raw);
		} catch (Exception e) {
			return String.valueOf(raw);
		}
	}

	private static Boolean toBooleanObject(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		return false;
	}

	public void updateWechatUserProfileByOpenUnion(
			long companyId,
			Map<String, Object> authClaims,
			Map<String, Object> wechatPatch,
			String country,
			String province,
			String city,
			String language) {
		String unionid = stringVal(authClaims.get("unionid"));
		String openId = stringVal(authClaims.get("open_id"));
		String appid = stringVal(authClaims.get("wxapp_appid"));
		if (!StringUtils.hasText(unionid) || !StringUtils.hasText(openId) || !StringUtils.hasText(appid)) {
			return;
		}
		WechatUsers existing =
				wechatUsersMapper.selectOne(
						new LambdaQueryWrapper<WechatUsers>()
								.eq(WechatUsers::getCompanyId, companyId)
								.eq(WechatUsers::getUnionid, unionid)
								.eq(WechatUsers::getOpenId, openId)
								.eq(WechatUsers::getAuthorizerAppid, appid)
								.last("LIMIT 1"));
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		long now = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<WechatUsers> uw =
				new LambdaUpdateWrapper<WechatUsers>()
						.eq(WechatUsers::getCompanyId, companyId)
						.eq(WechatUsers::getAuthorizerAppid, appid)
						.eq(WechatUsers::getOpenId, openId)
						.eq(WechatUsers::getUnionid, unionid)
						.set(WechatUsers::getUpdated, now);
		if (wechatPatch != null && wechatPatch.containsKey("nickname")) {
			uw.set(WechatUsers::getNickname, String.valueOf(wechatPatch.get("nickname")));
		}
		if (wechatPatch != null
				&& wechatPatch.containsKey("headimgurl")
				&& wechatPatch.get("headimgurl") != null
				&& StringUtils.hasText(String.valueOf(wechatPatch.get("headimgurl")).trim())) {
			uw.set(WechatUsers::getHeadimgurl, String.valueOf(wechatPatch.get("headimgurl")).trim());
		}
		int n = wechatUsersMapper.update(null, uw);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> registerShuyunMember(Map<String, Object> params) {
		long companyId = toLong(params.get("company_id"));
		String mobile = stringVal(params.get("mobile"));
		if (companyId <= 0 || !StringUtils.hasText(mobile)) {
			throw new BadRequestException("注册参数不完整");
		}
		String userType = stringVal(params.get("user_type"));
		if (!StringUtils.hasText(userType)) {
			userType = "wechat";
		}
		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeId == null) {
			throw new ResourceException("缺少默认等级");
		}
		long now = System.currentTimeMillis() / 1000L;
		Members m = new Members();
		m.setCompanyId(companyId);
		m.setGradeId(gradeId);
		m.setMobile(encodeMobileForStorage(mobile));
		m.setRegionMobile(mobile);
		m.setMobileCountryCode("86");
		m.setPassword(passwordEncoder.encode(randomUsername(12)));
		m.setUserCardCode(randomCardCode());
		m.setCreated(now);
		m.setUpdated(now);
		m.setDisabled(false);
		if ("ali".equals(userType)) {
			String alipayAppid = stringVal(params.get("alipay_appid"));
			if (StringUtils.hasText(alipayAppid)) {
				m.setAlipayAppid(alipayAppid);
			}
		}
		membersMapper.insert(m);

		MembersInfo info = new MembersInfo();
		info.setUserId(m.getUserId());
		info.setCompanyId(companyId);
		info.setUsername(randomUsername(8));
		info.setCreated(now);
		info.setUpdated(now);
		membersInfoMapper.insert(info);

		String unionid = stringVal(params.get("unionid"));
		MembersAssociations assoc = new MembersAssociations();
		assoc.setUserId(m.getUserId());
		assoc.setUnionid(unionid);
		assoc.setCompanyId(companyId);
		assoc.setUserType(userType);
		membersAssociationsMapper.insert(assoc);

		long userId = m.getUserId();
		boolean ifRegisterPromotion = resolveIfRegisterPromotionForNewMember(companyId, mobile);
		String openIdEvent = stringVal(params.get("unionid")).trim();
		String wxaAppidEvent = stringVal(params.get("wxa_appid")).trim();
		if (!StringUtils.hasText(wxaAppidEvent)) {
			wxaAppidEvent = stringVal(params.get("alipay_appid")).trim();
		}
		long inviterIdEvent = longParam(params.get("inviter_id"));
		long sourceIdEvent = longParam(params.get("source_id"));
		long monitorIdEvent = longParam(params.get("monitor_id"));
		long distributorIdForEvent = 0L;
		Object distRaw = params.get("distributor_id");
		if (distRaw != null && stringVal(distRaw).trim().length() > 0) {
			long d = longParam(distRaw);
			if (d > 0L) {
				distributorIdForEvent = d;
			}
		}
		long salespersonIdForEvent = longParam(params.get("salesperson_id"));

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("user_id", userId);
		eventData.put("company_id", companyId);
		eventData.put("mobile", mobile);
		eventData.put("openid", openIdEvent);
		eventData.put("wxa_appid", wxaAppidEvent);
		eventData.put("inviter_id", inviterIdEvent);
		eventData.put("distributor_id", distributorIdForEvent);
		eventData.put("source_id", sourceIdEvent);
		eventData.put("monitor_id", monitorIdEvent);
		eventData.put("salesperson_id", salespersonIdForEvent);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("registerShuyunMember requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
				String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
				sharedStringRedisTemplate.opsForSet().add("Member:" + companyId + ":" + ymd, String.valueOf(userId));
			}
		});

		Map<String, Object> row = new HashMap<>();
		row.put("user_id", userId);
		row.put("company_id", companyId);
		return row;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> creatMemberForH5Post(Map<String, Object> postData, boolean isUpdatePassword) {
		long companyId = toLong(postData.get("company_id"));
		String mobilePlain = stringVal(postData.get("mobile")).trim();
		if (companyId <= 0L || !StringUtils.hasText(mobilePlain)) {
			throw new ResourceException("注册参数不完整");
		}
		String apiFrom = stringVal(postData.get("api_from"));
		String authType = stringVal(postData.get("auth_type"));
		int forcePassword = numberOrZero(postData.get("force_password"));

		Long gradeIdObj = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (gradeIdObj == null) {
			throw new ResourceException("缺少默认等级");
		}
		long defaultGradeId = gradeIdObj;

		Members existing = findMemberByCompanyAndMobile(companyId, mobilePlain);
		boolean isNew = false;
		boolean isUploadMember = false;
		long userId;
		MembersInfo infoForUploadCleanup = null;
		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());
		long effectiveGradeId = defaultGradeId;

		if (existing != null) {
			MembersInfo infoBefore =
					membersInfoMapper.selectOne(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getUserId, existing.getUserId())
									.eq(MembersInfo::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (infoBefore != null && otherParamsHasUploadMemberFlag(infoBefore.getOtherParams())) {
				isUploadMember = true;
				isNew = true;
			}

			Members m = existing;
			userId = m.getUserId();
			if (isUpdatePassword) {
				String pwdPlain = stringVal(postData.get("password"));
				if (StringUtils.hasText(pwdPlain)) {
					m.setPassword(passwordEncoder.encode(pwdPlain));
				}
			}
			if (!StringUtils.hasText(m.getUserCardCode())) {
				m.setUserCardCode(randomCardCode());
			}
			if (m.getGradeId() == null || m.getGradeId() <= 0L || m.getGradeId() == -1L) {
				m.setGradeId(defaultGradeId);
			}
			applyH5PostDataToExistingMember(m, postData, mobilePlain, now, z);
			membersMapper.updateById(m);
			effectiveGradeId = m.getGradeId() != null ? m.getGradeId() : defaultGradeId;

			MembersInfo info =
					membersInfoMapper.selectOne(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getUserId, userId)
									.eq(MembersInfo::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (info == null) {
				info = new MembersInfo();
				info.setUserId(userId);
				info.setCompanyId(companyId);
				info.setOtherParams("[]");
				info.setCreated(now);
				membersInfoMapper.insert(info);
			}
			applyH5PostDataToMembersInfo(info, postData, now);
			membersInfoMapper.updateById(info);
			if (isUploadMember) {
				infoForUploadCleanup = info;
			}
		} else {
			isNew = true;
			String passwordPlain = resolveH5NewMemberPasswordPlain(postData, apiFrom, authType, forcePassword);
			Members m = new Members();
			m.setCompanyId(companyId);
			m.setGradeId(defaultGradeId);
			m.setMobile(LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain));
			m.setRegionMobile(mobilePlain);
			m.setMobileCountryCode(stringOrDefault(postData.get("mobile_country_code"), "86"));
			m.setPassword(passwordEncoder.encode(passwordPlain));
			m.setUserCardCode(randomCardCode());
			m.setCreated(now);
			m.setUpdated(now);
			m.setDisabled(false);
			m.setCreatedYear(z.getYear());
			m.setCreatedMonth(z.getMonthValue());
			m.setCreatedDay(z.getDayOfMonth());
			applyH5PostDataToNewMember(m, postData, now, z);
			membersMapper.insert(m);
			userId = m.getUserId();
			effectiveGradeId = m.getGradeId() != null ? m.getGradeId() : defaultGradeId;

			MembersInfo info = new MembersInfo();
			info.setUserId(userId);
			info.setCompanyId(companyId);
			String uname = stringVal(postData.get("username")).trim();
			info.setUsername(StringUtils.hasText(uname) ? uname : randomUsername(8));
			info.setAvatar(stringVal(postData.get("avatar")).trim());
			info.setSex(parseSexInt(postData.get("sex")));
			info.setOtherParams("[]");
			info.setCreated(now);
			info.setUpdated(now);
			membersInfoMapper.insert(info);
		}

		boolean ifRegisterPromotion = resolveIfRegisterPromotionForNewMember(companyId, mobilePlain);

		if (isNew && userId > 0L) {
			resolveWorkUseridIntoPostData(companyId, postData);
			long distributorId = longParam(postData.get("distributor_id"));
			long salespersonId = longParam(postData.get("salesperson_id"));
			long inviterId = longParam(postData.get("inviter_id"));

			adminMemberCreatePromoterSideEffectPort.createForNewMember(
					companyId, userId, mobilePlain, effectiveGradeId);
			adminMemberCreateDistributorUserSideEffectPort.createData(
					companyId, userId, distributorId, salespersonId, inviterId);

			if (distributorId > 0L) {
				insertShopRelMemberIfAbsent(companyId, userId, distributorId);
			}
			if (salespersonId > 0L) {
				insertWorkWechatRelBindIfAbsent(
						companyId,
						userId,
						salespersonId,
						stringVal(postData.get("unionid")),
						stringVal(postData.get("work_userid")),
						now);
			}

			registerAfterCommitH5CreateMemberSideEffects(
					companyId,
					userId,
					mobilePlain,
					postData,
					ifRegisterPromotion,
					!isUploadMember);
		}

		if (shouldCreateMemberAssociationForH5(apiFrom, authType)) {
			String unionid = stringVal(postData.get("unionid")).trim();
			if (StringUtils.hasText(unionid)) {
				String userTypeAssoc = stringVal(postData.get("user_type"));
				if (!StringUtils.hasText(userTypeAssoc)) {
					userTypeAssoc = "wechat";
				}
				if ("ali".equals(userTypeAssoc) || "aliapp".equals(authType)) {
					String alipayUid = stringVal(postData.get("alipay_user_id")).trim();
					if (!StringUtils.hasText(alipayUid)) {
						alipayUid = unionid;
					}
					ensureAliMemberAssociation(companyId, userId, alipayUid, stringVal(postData.get("alipay_appid")));
				} else if ("social_oauth".equals(authType)) {
					createMemberPlatformAssociation(companyId, userId, unionid, userTypeAssoc);
				} else {
					createMemberWechatAssociation(companyId, userId, unionid);
				}
			}
		}

		if (isUploadMember && infoForUploadCleanup != null) {
			LinkedHashMap<String, Object> op = readOtherParamsAsMap(infoForUploadCleanup.getOtherParams());
			op.remove("is_upload_member");
			infoForUploadCleanup.setOtherParams(writeOtherParamsMap(op));
			infoForUploadCleanup.setUpdated(System.currentTimeMillis() / 1000L);
			membersInfoMapper.updateById(infoForUploadCleanup);
		}

		Members reloaded =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserId, userId)
								.last("LIMIT 1"));
		if (reloaded == null) {
			throw new ResourceException("会员数据异常");
		}
		return toCreatMemberH5ResultMap(reloaded);
	}

	private void applyH5PostDataToExistingMember(
			Members m, Map<String, Object> postData, String mobilePlain, long now, ZonedDateTime z) {
		m.setRegionMobile(mobilePlain);
		m.setUpdated(now);
		Long inv = longParamObj(postData.get("inviter_id"));
		if (inv != null) {
			m.setInviterId(inv);
		}
		String sf = stringVal(postData.get("source_from")).trim();
		if (StringUtils.hasText(sf)) {
			m.setSourceFrom(sf);
		}
		m.setSourceId(longParamObjOrNull(postData.get("source_id")));
		m.setMonitorId(longParamObjOrNull(postData.get("monitor_id")));
		m.setLatestSourceId(longParamObjOrNull(postData.get("latest_source_id")));
		m.setLatestMonitorId(longParamObjOrNull(postData.get("latest_monitor_id")));
		String wxa = stringVal(postData.get("wxa_appid")).trim();
		if (StringUtils.hasText(wxa)) {
			m.setWxaAppid(wxa);
		}
		String authApp = stringVal(postData.get("authorizer_appid")).trim();
		if (StringUtils.hasText(authApp)) {
			m.setAuthorizerAppid(authApp);
		}
		String ali = stringVal(postData.get("alipay_appid")).trim();
		if (StringUtils.hasText(ali)) {
			m.setAlipayAppid(ali);
		}
		int rd = numberOrZero(postData.get("reg_distributor"));
		if (rd > 0) {
			m.setRegDistributor(rd);
		}
		String rs = stringVal(postData.get("reg_salesperson")).trim();
		if (StringUtils.hasText(rs)) {
			m.setRegSalesperson(rs);
		}
		int od = numberOrZero(postData.get("op_distributor"));
		if (od > 0) {
			m.setOpDistributor(od);
		}
	}

	private void applyH5PostDataToNewMember(Members m, Map<String, Object> postData, long now, ZonedDateTime z) {
		Long inv = longParamObj(postData.get("inviter_id"));
		m.setInviterId(inv != null && inv > 0L ? inv : 0L);
		String sf = stringVal(postData.get("source_from")).trim();
		m.setSourceFrom(StringUtils.hasText(sf) ? sf : "default");
		m.setSourceId(longParamObjOrZero(postData.get("source_id")));
		m.setMonitorId(longParamObjOrZero(postData.get("monitor_id")));
		m.setLatestSourceId(longParamObjOrZero(postData.get("latest_source_id")));
		m.setLatestMonitorId(longParamObjOrZero(postData.get("latest_monitor_id")));
		String wxa = stringVal(postData.get("wxa_appid")).trim();
		if (StringUtils.hasText(wxa)) {
			m.setWxaAppid(wxa);
		}
		String authApp = stringVal(postData.get("authorizer_appid")).trim();
		if (StringUtils.hasText(authApp)) {
			m.setAuthorizerAppid(authApp);
		}
		String ali = stringVal(postData.get("alipay_appid")).trim();
		if (StringUtils.hasText(ali)) {
			m.setAlipayAppid(ali);
		}
		int rd = numberOrZero(postData.get("reg_distributor"));
		if (rd > 0) {
			m.setRegDistributor(rd);
		}
		String rs = stringVal(postData.get("reg_salesperson")).trim();
		if (StringUtils.hasText(rs)) {
			m.setRegSalesperson(rs);
		}
		String remarks = stringVal(postData.get("remarks")).trim();
		if (StringUtils.hasText(remarks)) {
			m.setRemarks(remarks);
		}
		String third = stringVal(postData.get("third_data")).trim();
		if (StringUtils.hasText(third)) {
			m.setThirdData(third);
		}
	}

	private void applyH5PostDataToMembersInfo(MembersInfo info, Map<String, Object> postData, long now) {
		String uname = stringVal(postData.get("username")).trim();
		if (StringUtils.hasText(uname)) {
			info.setUsername(uname);
		}
		String av = stringVal(postData.get("avatar")).trim();
		if (StringUtils.hasText(av)) {
			info.setAvatar(av);
		}
		if (postData.containsKey("sex")) {
			info.setSex(parseSexInt(postData.get("sex")));
		}
		info.setUpdated(now);
	}

	private static int parseSexInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long longParamObjOrNull(Object o) {
		if (o == null) {
			return null;
		}
		long v = longParam(o);
		return v == 0L ? null : v;
	}

	private static long longParamObjOrZero(Object o) {
		return longParam(o);
	}

	private static Long longParamObj(Object o) {
		if (o == null) {
			return null;
		}
		long v = longParam(o);
		return v;
	}

	private static Long parseExternalMemberIdForOpenapi(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String resolveH5NewMemberPasswordPlain(
			Map<String, Object> postData, String apiFrom, String authType, int forcePassword) {
		boolean wechatLike =
				"wechat".equals(apiFrom)
						|| "wxapp".equals(authType)
						|| "aliapp".equals(authType)
						|| "social_oauth".equals(authType);
		if (wechatLike && forcePassword == 0) {
			return randomAlphanumericPassword(10);
		}
		if ("social_oauth".equals(authType) && !StringUtils.hasText(stringVal(postData.get("password")))) {
			return randomAlphanumericPassword(10);
		}
		String p = stringVal(postData.get("password"));
		if (!StringUtils.hasText(p)) {
			return randomAlphanumericPassword(10);
		}
		return p;
	}

	private static String randomAlphanumericPassword(int len) {
		String chars = "QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";
		ThreadLocalRandom r = ThreadLocalRandom.current();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private void resolveWorkUseridIntoPostData(long companyId, Map<String, Object> postData) {
		String wu = stringVal(postData.get("work_userid")).trim();
		long sp = longParam(postData.get("salesperson_id"));
		long dist = longParam(postData.get("distributor_id"));
		if (!StringUtils.hasText(wu) || sp > 0L) {
			return;
		}
		Map<String, Object> guide = shoppingGuideForH5BindLookup.getShoppingGuideDetailForH5Bind(companyId, wu);
		if (guide == null || guide.isEmpty()) {
			return;
		}
		Object sid = guide.get("salesperson_id");
		if (sid != null) {
			try {
				sp = Long.parseLong(String.valueOf(sid).trim());
			} catch (NumberFormatException e) {
				sp = 0L;
			}
		}
		Object dRaw = guide.get("distributor_id");
		if (dRaw != null && !(dRaw instanceof Boolean)) {
			try {
				dist = Long.parseLong(String.valueOf(dRaw).trim());
			} catch (NumberFormatException e) {
				// keep dist
			}
		}
		if (sp > 0L) {
			postData.put("salesperson_id", sp);
		}
		if (dist > 0L) {
			postData.put("distributor_id", dist);
		}
	}

	/**
	 * Idempotent insert into {@code members_rel_shop} when no row exists for the tuple
	 * ({@code company_id}, {@code user_id}, {@code shop_id}, {@code shop_type}).
	 */
	public void ensureShopRelMemberIfAbsent(long companyId, long userId, long shopId, String shopType) {
		if (companyId <= 0L || userId <= 0L || shopId <= 0L || !StringUtils.hasText(shopType)) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		String st = shopType.trim();
		ShopRelMember ex =
				shopRelMemberMapper.selectOne(
						new LambdaQueryWrapper<ShopRelMember>()
								.eq(ShopRelMember::getCompanyId, companyId)
								.eq(ShopRelMember::getUserId, userId)
								.eq(ShopRelMember::getShopId, shopId)
								.eq(ShopRelMember::getShopType, st)
								.last("LIMIT 1"));
		if (ex != null) {
			return;
		}
		ShopRelMember row = new ShopRelMember();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setShopId(shopId);
		row.setShopType(st);
		row.setCreated(now);
		row.setUpdated(now);
		shopRelMemberMapper.insert(row);
	}

	private void insertShopRelMemberIfAbsent(long companyId, long userId, long distributorId) {
		ensureShopRelMemberIfAbsent(companyId, userId, distributorId, "distributor");
	}

	private void insertWorkWechatRelBindIfAbsent(
			long companyId,
			long userId,
			long salespersonId,
			String unionid,
			String workUserid,
			long now) {
		WorkWechatRel ex =
				workWechatRelMapper.selectOne(
						new LambdaQueryWrapper<WorkWechatRel>()
								.eq(WorkWechatRel::getCompanyId, companyId)
								.eq(WorkWechatRel::getUserId, userId)
								.eq(WorkWechatRel::getSalespersonId, salespersonId)
								.last("LIMIT 1"));
		if (ex != null) {
			return;
		}
		WorkWechatRel ins = new WorkWechatRel();
		ins.setCompanyId(companyId);
		ins.setSalespersonId(salespersonId);
		ins.setUserId(userId);
		ins.setUnionid(unionid == null ? "" : unionid);
		ins.setWorkUserid(workUserid == null ? "" : workUserid);
		ins.setExternalUserid("");
		ins.setIsFriend(false);
		ins.setIsBind(true);
		ins.setBoundTime(now);
		ins.setAddFriendTime(0L);
		workWechatRelMapper.insert(ins);
	}

	private void registerAfterCommitH5CreateMemberSideEffects(
			long companyId,
			long userId,
			String mobilePlain,
			Map<String, Object> postData,
			boolean ifRegisterPromotion,
			boolean includeDailyRedisSet) {
		long sourceId = longParam(postData.get("source_id"));
		long monitorId = longParam(postData.get("monitor_id"));
		long inviterId = longParam(postData.get("inviter_id"));
		long salespersonId = longParam(postData.get("salesperson_id"));
		long distributorId = longParam(postData.get("distributor_id"));
		String openId = stringVal(postData.get("open_id")).trim();
		String wxaAppid = stringVal(postData.get("wxa_appid")).trim();

		Map<String, Object> eventData = new LinkedHashMap<>();
		eventData.put("company_id", companyId);
		eventData.put("user_id", userId);
		eventData.put("mobile", mobilePlain);
		eventData.put("openid", openId);
		eventData.put("wxa_appid", wxaAppid);
		eventData.put("inviter_id", inviterId);
		eventData.put("distributor_id", distributorId);
		eventData.put("source_id", sourceId);
		eventData.put("monitor_id", monitorId);
		eventData.put("salesperson_id", salespersonId);
		eventData.put("if_register_promotion", ifRegisterPromotion);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new BadRequestException("creatMemberForH5Post requires active transaction synchronization");
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				membersCreateMemberSuccessDispatchPublisher.publish(eventData);
				if (includeDailyRedisSet) {
					String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
					sharedStringRedisTemplate.opsForSet().add("Member:" + companyId + ":" + ymd, String.valueOf(userId));
				}
				String workUseridRaw = stringVal(postData.get("work_userid"));
				if (StringUtils.hasText(workUseridRaw.trim()) && numberOrZero(postData.get("channel")) == 1) {
					bindSalsepersonJobDispatchPublisher.enqueueBindSalseperson(
							companyId,
							stringVal(postData.get("unionid")).trim(),
							workUseridRaw.trim(),
							1,
							mobilePlain,
							userId);
				}
			}
		});
	}

	private static boolean shouldCreateMemberAssociationForH5(String apiFrom, String authType) {
		return "wechat".equals(apiFrom)
				|| "wxapp".equals(authType)
				|| "wx_offiaccount".equals(authType)
				|| "aliapp".equals(authType)
				|| "social_oauth".equals(authType);
	}

	private LinkedHashMap<String, Object> toCreatMemberH5ResultMap(Members mb) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", mb.getUserId() == null ? 0 : mb.getUserId().intValue());
		m.put("company_id", mb.getCompanyId() == null ? 0 : mb.getCompanyId().intValue());
		m.put("grade_id", mb.getGradeId() == null ? 0 : mb.getGradeId().intValue());
		m.put("mobile", mb.getMobile());
		m.put("region_mobile", mb.getRegionMobile());
		m.put("mobile_country_code", mb.getMobileCountryCode() != null ? mb.getMobileCountryCode() : "");
		m.put("user_card_code", mb.getUserCardCode() != null ? mb.getUserCardCode() : "");
		m.put("offline_card_code", mb.getOfflineCardCode());
		m.put("inviter_id", mb.getInviterId() == null ? 0 : mb.getInviterId().intValue());
		m.put("source_from", stringOrDefault(mb.getSourceFrom(), "default"));
		m.put("source_id", mb.getSourceId() == null ? 0 : mb.getSourceId().intValue());
		m.put("monitor_id", mb.getMonitorId() == null ? 0 : mb.getMonitorId().intValue());
		m.put("latest_source_id", mb.getLatestSourceId() == null ? 0 : mb.getLatestSourceId().intValue());
		m.put("latest_monitor_id", mb.getLatestMonitorId() == null ? 0 : mb.getLatestMonitorId().intValue());
		m.put("authorizer_appid", mb.getAuthorizerAppid() != null ? mb.getAuthorizerAppid() : "");
		m.put("use_point", Boolean.TRUE.equals(mb.getUsePoint()));
		m.put("wxa_appid", mb.getWxaAppid() != null ? mb.getWxaAppid() : "");
		m.put("alipay_appid", mb.getAlipayAppid() != null ? mb.getAlipayAppid() : "");
		m.put("created", mb.getCreated() == null ? 0L : mb.getCreated());
		m.put("updated", mb.getUpdated() == null ? 0L : mb.getUpdated());
		m.put("disabled", Boolean.TRUE.equals(mb.getDisabled()));
		m.put("remarks", mb.getRemarks() != null ? mb.getRemarks() : "");
		m.put("third_data", mb.getThirdData() != null ? mb.getThirdData() : "");
		m.put("reg_distributor", mb.getRegDistributor() == null ? 0 : mb.getRegDistributor());
		m.put("reg_salesperson", mb.getRegSalesperson() != null ? mb.getRegSalesperson() : "");
		m.put("fp_salesperson", mb.getFpSalesperson());
		m.put("has_fp", Boolean.TRUE.equals(mb.getHasFp()));
		m.put("is_become_friend", Boolean.TRUE.equals(mb.getIsBecomeFriend()));
		m.put("op_distributor", mb.getOpDistributor() == null ? 0 : mb.getOpDistributor());
		return m;
	}

	private static boolean otherParamsHasUploadMemberFlag(String raw) {
		LinkedHashMap<String, Object> op = readOtherParamsAsMap(raw);
		Object v = op.get("is_upload_member");
		return Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
	}

	private static LinkedHashMap<String, Object> readOtherParamsAsMap(String raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (!StringUtils.hasText(raw)) {
			return out;
		}
		String t = raw.trim();
		if ("[]".equals(t) || "{}".equals(t)) {
			return out;
		}
		try {
			Map<String, Object> parsed = JSON.readValue(t, new TypeReference<Map<String, Object>>() {});
			if (parsed != null) {
				out.putAll(parsed);
			}
		} catch (Exception ignored) {
			// keep empty
		}
		return out;
	}

	private static String writeOtherParamsMap(Map<String, Object> op) {
		try {
			return JSON.writeValueAsString(op == null ? Map.of() : op);
		} catch (Exception e) {
			return "{}";
		}
	}

	/**
	 * Persists Alipay third-party identity for local (non-数云主链) mini-program registration: optional {@code alipay_appid}
	 * on {@code members}, and a {@code members_associations} row with {@code user_type} {@code ali} and {@code unionid}
	 * equal to the Alipay user id when absent.
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ensureAliMemberAssociation(long companyId, long userId, String alipayUserId, String alipayAppid) {
		if (companyId <= 0L || userId <= 0L || !StringUtils.hasText(alipayUserId)) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		if (StringUtils.hasText(alipayAppid)) {
			membersMapper.update(
					null,
					new LambdaUpdateWrapper<Members>()
							.eq(Members::getCompanyId, companyId)
							.eq(Members::getUserId, userId)
							.set(Members::getAlipayAppid, alipayAppid.trim()));
		}
		MembersAssociations existing =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserType, "ali")
								.eq(MembersAssociations::getUnionid, alipayUserId)
								.last("LIMIT 1"));
		if (existing != null) {
			return;
		}
		MembersAssociations assoc = new MembersAssociations();
		assoc.setUserId(userId);
		assoc.setUnionid(alipayUserId);
		assoc.setCompanyId(companyId);
		assoc.setUserType("ali");
		membersAssociationsMapper.insert(assoc);
	}

	private WechatUsers findWechatUser(Map<String, Object> filter) {
		LambdaQueryWrapper<WechatUsers> q = new LambdaQueryWrapper<>();
		if (filter.get("company_id") != null) {
			q.eq(WechatUsers::getCompanyId, toLong(filter.get("company_id")));
		}
		if (filter.get("authorizer_appid") != null) {
			q.eq(WechatUsers::getAuthorizerAppid, String.valueOf(filter.get("authorizer_appid")));
		}
		if (filter.get("open_id") != null) {
			q.eq(WechatUsers::getOpenId, String.valueOf(filter.get("open_id")));
		}
		if (filter.containsKey("unionid")) {
			Object u = filter.get("unionid");
			if (u == null || (u instanceof String s && !StringUtils.hasText(s))) {
				q.isNull(WechatUsers::getUnionid);
			} else {
				q.eq(WechatUsers::getUnionid, String.valueOf(u));
			}
		}
		q.last("LIMIT 1");
		return wechatUsersMapper.selectOne(q);
	}

	private static Map<String, Object> wechatUserToMap(WechatUsers u) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", u.getCompanyId());
		m.put("authorizer_appid", u.getAuthorizerAppid());
		m.put("open_id", u.getOpenId());
		m.put("unionid", u.getUnionid());
		m.put("nickname", u.getNickname());
		m.put("headimgurl", u.getHeadimgurl());
		m.put("inviter_id", u.getInviterId());
		m.put("source_from", u.getSourceFrom());
		m.put("need_transfer", u.getNeedTransfer());
		m.put("created", u.getCreated());
		m.put("updated", u.getUpdated());
		return m;
	}

	private static String randomCardCode() {
		return "M" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
	}

	private static String randomUsername(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = 0; i < len; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String stringOrDefault(Object o, String def) {
		String s = stringVal(o);
		return StringUtils.hasText(s) ? s : def;
	}

	private static int numberOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
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
		return Long.parseLong(String.valueOf(o));
	}
}
