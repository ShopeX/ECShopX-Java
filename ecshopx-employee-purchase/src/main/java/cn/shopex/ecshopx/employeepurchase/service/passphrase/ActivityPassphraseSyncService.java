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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityPassphraseSyncService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String CODE_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 8;
	private static final int MAX_COLLISION_RETRIES = 64;

	public enum PassphraseSyncMode {
		NONE,
		REPLACE,
		CLEAR
	}

	private final ActivitiesMapper activitiesMapper;
	private final ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	private final ActivityPassphraseService activityPassphraseService;
	private final PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;
	private final SecureRandom secureRandom = new SecureRandom();

	public ActivityPassphraseSyncService(
			ActivitiesMapper activitiesMapper,
			ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper,
			ActivityPassphraseService activityPassphraseService,
			PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService) {
		this.activitiesMapper = activitiesMapper;
		this.activityPassphraseEnterpriseMapper = activityPassphraseEnterpriseMapper;
		this.activityPassphraseService = activityPassphraseService;
		this.passphraseParticipateQuotaRedisService = passphraseParticipateQuotaRedisService;
	}

	public void applyOnCreate(
			long companyId, long activityId, Set<Long> activityEnterpriseIds, Map<String, Object> params) {
		Boolean enabled = parseEnabled(params.get("is_passphrase_enabled"));
		if (!Boolean.TRUE.equals(enabled)) {
			setPassphraseEnabled(companyId, activityId, false);
			return;
		}
		List<PassphraseEnterpriseInput> items = parsePassphraseEnterprises(params.get("passphrase_enterprises"));
		if (items.isEmpty()) {
			throw new ResourceException("开启口令通道时请配置口令企业信息");
		}
		validateAndReplace(companyId, activityId, activityEnterpriseIds, items, false);
		setPassphraseEnabled(companyId, activityId, true);
	}

	/**
	 * 新契约：从 enterprise_configs 写入口令；passphrase_limitfee = per_capita_limitfee；
	 * passphrase_code 为空时后端随机生成。
	 */
	public void applyFromEnterpriseConfigsOnCreate(
			long companyId,
			long activityId,
			Set<Long> activityEnterpriseIds,
			List<PassphraseEnterpriseInput> items) {
		if (items == null || items.isEmpty()) {
			throw new ResourceException("开启口令通道时请配置口令企业信息");
		}
		validateAndReplace(companyId, activityId, activityEnterpriseIds, items, true);
		setPassphraseEnabled(companyId, activityId, true);
	}

	public void applyOnUpdate(
			long companyId, long activityId, Set<Long> activityEnterpriseIds, Map<String, Object> params) {
		PassphraseSyncMode mode = resolveUpdateSyncMode(params);
		switch (mode) {
			case NONE -> {
				// 不改口令表
			}
			case CLEAR -> clearPassphraseConfig(companyId, activityId);
			case REPLACE -> {
				Boolean enabled = parseEnabled(params.get("is_passphrase_enabled"));
				if (enabled != null && !enabled) {
					clearPassphraseConfig(companyId, activityId);
					return;
				}
				// 新契约活动更新口令关键配置已锁定；此处仅兼容旧活动 passphrase_enterprises
				List<PassphraseEnterpriseInput> items = parsePassphraseEnterprises(params.get("passphrase_enterprises"));
				if (items.isEmpty()) {
					throw new ResourceException("开启口令通道时请配置口令企业信息");
				}
				validateAndReplace(companyId, activityId, activityEnterpriseIds, items, false);
				setPassphraseEnabled(companyId, activityId, true);
			}
		}
	}

	public PassphraseSyncMode resolveUpdateSyncMode(Map<String, Object> params) {
		if (params.containsKey("is_passphrase_enabled")) {
			Boolean enabled = parseEnabled(params.get("is_passphrase_enabled"));
			if (enabled != null && !enabled) {
				return PassphraseSyncMode.CLEAR;
			}
		}
		if (params.containsKey("passphrase_enterprises")) {
			return PassphraseSyncMode.REPLACE;
		}
		return PassphraseSyncMode.NONE;
	}

	public void clearPassphraseConfig(long companyId, long activityId) {
		List<ActivityPassphraseEnterprise> existing =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		activityPassphraseEnterpriseMapper.delete(
				Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
						.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
						.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		for (ActivityPassphraseEnterprise row : existing) {
			passphraseParticipateQuotaRedisService.removeKey(
					companyId, activityId, row.getEnterpriseId());
		}
		setPassphraseEnabled(companyId, activityId, false);
	}

	private void validateAndReplace(
			long companyId,
			long activityId,
			Set<Long> activityEnterpriseIds,
			List<PassphraseEnterpriseInput> items,
			boolean allowGenerateEmptyCode) {
		Set<Long> seenEnterprise = new HashSet<>();
		Set<String> seenCodeInActivity = new HashSet<>();
		Set<String> usedInCompany =
				activityPassphraseService.collectUsedCodesForCompany(companyId, activityId);
		List<PassphraseEnterpriseInput> normalized = new ArrayList<>(items.size());

		for (PassphraseEnterpriseInput item : items) {
			if (item.enterpriseId() == null || item.enterpriseId() <= 0L) {
				throw new ResourceException("口令企业配置格式错误");
			}
			if (!activityEnterpriseIds.contains(item.enterpriseId())) {
				throw new ResourceException("口令企业须为活动参与企业");
			}
			if (!seenEnterprise.add(item.enterpriseId())) {
				throw new ResourceException("同一活动下企业不能重复配置口令");
			}
			if (item.participateQuota() == null || item.participateQuota() <= 0) {
				throw new ResourceException("可参与名额须大于0");
			}
			if (item.passphraseLimitfee() == null || item.passphraseLimitfee() <= 0) {
				throw new ResourceException("口令通道额度须大于0（单位：分）");
			}
			String code = item.passphraseCode() == null ? "" : item.passphraseCode().trim();
			if (!StringUtils.hasText(code)) {
				if (!allowGenerateEmptyCode) {
					throw new ResourceException("口令编码为1-64个字符");
				}
				code = generateUniqueCode(usedInCompany, seenCodeInActivity);
			} else if (code.length() > 64) {
				throw new ResourceException("口令编码为1-64个字符");
			}
			if (!seenCodeInActivity.add(code)) {
				throw new ResourceException("同一活动下口令编码不能重复");
			}
			if (usedInCompany.contains(code)) {
				throw new ResourceException("口令编码已被其它活动占用：" + code);
			}
			normalized.add(
					new PassphraseEnterpriseInput(
							item.enterpriseId(), item.participateQuota(), item.passphraseLimitfee(), code));
		}

		List<ActivityPassphraseEnterprise> oldRows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		activityPassphraseEnterpriseMapper.delete(
				Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
						.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
						.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		for (ActivityPassphraseEnterprise old : oldRows) {
			passphraseParticipateQuotaRedisService.removeKey(
					companyId, activityId, old.getEnterpriseId());
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		for (PassphraseEnterpriseInput item : normalized) {
			ActivityPassphraseEnterprise row = new ActivityPassphraseEnterprise();
			row.setCompanyId(companyId);
			row.setActivityId(activityId);
			row.setEnterpriseId(item.enterpriseId());
			row.setParticipateQuota(item.participateQuota());
			row.setPassphraseLimitfee(item.passphraseLimitfee());
			row.setPassphraseCode(item.passphraseCode().trim());
			row.setCreated(now);
			row.setUpdated(now);
			activityPassphraseEnterpriseMapper.insert(row);
			passphraseParticipateQuotaRedisService.syncRemainingQuota(
					companyId, activityId, item.enterpriseId(), item.participateQuota());
		}
	}

	private String generateUniqueCode(Set<String> usedInCompany, Set<String> seenInActivity) {
		for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
			String code = randomCode();
			if (!usedInCompany.contains(code) && !seenInActivity.contains(code)) {
				return code;
			}
		}
		throw new ResourceException("生成唯一口令失败，请重试");
	}

	private String randomCode() {
		StringBuilder sb = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
		}
		return sb.toString();
	}

	private void setPassphraseEnabled(long companyId, long activityId, boolean enabled) {
		activitiesMapper.update(
				null,
				Wrappers.<Activities>lambdaUpdate()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, activityId)
						.set(Activities::getIsPassphraseEnabled, enabled));
	}

	@SuppressWarnings("unchecked")
	public static List<PassphraseEnterpriseInput> parsePassphraseEnterprises(Object raw) {
		raw = coercePassphraseEnterprisesRaw(raw);
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<PassphraseEnterpriseInput> out = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				throw new ResourceException("口令企业配置格式错误");
			}
			Map<String, Object> item = (Map<String, Object>) m;
			Long enterpriseId = readLong(item.get("enterprise_id"));
			Integer quota = readInt(item.get("participate_quota"));
			Integer limitFee = readInt(item.get("passphrase_limitfee"));
			String code = item.get("passphrase_code") == null ? null : item.get("passphrase_code").toString();
			out.add(new PassphraseEnterpriseInput(enterpriseId, quota, limitFee, code));
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Object coercePassphraseEnterprisesRaw(Object raw) {
		if (!(raw instanceof String s)) {
			return raw;
		}
		s = s.trim();
		if (!StringUtils.hasText(s)) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			throw new ResourceException("口令企业配置格式错误");
		}
	}

	public static Boolean parseEnabled(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return true;
		}
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return false;
		}
		return null;
	}

	private static Long readLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer readInt(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public record PassphraseEnterpriseInput(
			Long enterpriseId, Integer participateQuota, Integer passphraseLimitfee, String passphraseCode) {}
}
