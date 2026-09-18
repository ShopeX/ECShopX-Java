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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterprises;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityPassphraseEnterpriseMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseSyncService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseSyncService.PassphraseEnterpriseInput;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseParticipateQuotaRedisService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 新契约企业配置：enterprise_configs ↔ ActivityEnterprises.perCapitaLimitfee / 口令同步。 */
@Service
public class EnterpriseConfigService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final ActivityEnterprisesMapper activityEnterprisesMapper;
	private final ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final ActivityPassphraseSyncService activityPassphraseSyncService;
	private final ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper;
	private final PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;

	public EnterpriseConfigService(
			ActivityEnterprisesMapper activityEnterprisesMapper,
			ActivityPassphraseEnterpriseMapper activityPassphraseEnterpriseMapper,
			EnterprisesMapper enterprisesMapper,
			ActivityPassphraseSyncService activityPassphraseSyncService,
			ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper,
			PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService) {
		this.activityEnterprisesMapper = activityEnterprisesMapper;
		this.activityPassphraseEnterpriseMapper = activityPassphraseEnterpriseMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.activityPassphraseSyncService = activityPassphraseSyncService;
		this.activityEnterpriseParticipateUserMapper = activityEnterpriseParticipateUserMapper;
		this.passphraseParticipateQuotaRedisService = passphraseParticipateQuotaRedisService;
	}

	public List<EnterpriseConfigInput> parseAndValidate(
			Object enterpriseConfigsRaw, List<Long> enterpriseIds, boolean passphraseEnabled) {
		if (enterpriseIds == null || enterpriseIds.isEmpty()) {
			throw new BadRequestException("请选择参与企业");
		}
		List<Map<String, Object>> rawList = coerceConfigList(enterpriseConfigsRaw);
		if (rawList.isEmpty()) {
			throw new BadRequestException("请配置企业额度信息");
		}

		Set<Long> expected = new LinkedHashSet<>(enterpriseIds);
		if (expected.size() != enterpriseIds.size()) {
			throw new BadRequestException("参与企业不能重复");
		}
		if (rawList.size() != expected.size()) {
			throw new BadRequestException("企业配置须与参与企业一一对应");
		}

		Set<Long> seen = new HashSet<>();
		List<EnterpriseConfigInput> out = new ArrayList<>(rawList.size());
		for (Map<String, Object> item : rawList) {
			Long enterpriseId = readLong(item.get("enterprise_id"));
			if (enterpriseId == null || enterpriseId <= 0L) {
				throw new BadRequestException("企业配置格式错误");
			}
			if (!expected.contains(enterpriseId)) {
				throw new BadRequestException("企业配置须与参与企业一一对应");
			}
			if (!seen.add(enterpriseId)) {
				throw new BadRequestException("企业配置不能重复");
			}
			Integer perCapita = readInt(item.get("per_capita_limitfee"));
			if (perCapita == null || perCapita <= 0) {
				throw new BadRequestException("人均额度须大于0");
			}
			Integer participateQuota = readInt(item.get("participate_quota"));
			String passphraseCode =
					item.get("passphrase_code") == null ? null : item.get("passphrase_code").toString();
			if (passphraseEnabled) {
				if (participateQuota == null || participateQuota <= 0) {
					throw new BadRequestException("开启口令通道时请填写可参与名额");
				}
			} else {
				participateQuota = null;
				passphraseCode = null;
			}
			out.add(new EnterpriseConfigInput(enterpriseId, perCapita, participateQuota, passphraseCode));
		}
		if (seen.size() != expected.size()) {
			throw new BadRequestException("企业配置须与参与企业一一对应");
		}
		return out;
	}

	/** 写入活动-企业关联（含 per_capita_limitfee）；开启口令时同步口令表。 */
	public void saveOnCreate(
			long companyId,
			long activityId,
			List<Long> enterpriseIds,
			List<EnterpriseConfigInput> configs,
			boolean passphraseEnabled) {
		writeActivityEnterprises(companyId, activityId, configs);
		if (!passphraseEnabled) {
			activityPassphraseSyncService.clearPassphraseConfig(companyId, activityId);
			return;
		}
		List<PassphraseEnterpriseInput> passphraseItems = new ArrayList<>(configs.size());
		for (EnterpriseConfigInput c : configs) {
			passphraseItems.add(
					new PassphraseEnterpriseInput(
							c.enterpriseId(),
							c.participateQuota(),
							c.perCapitaLimitfee(),
							c.passphraseCode()));
		}
		activityPassphraseSyncService.applyFromEnterpriseConfigsOnCreate(
				companyId, activityId, new HashSet<>(enterpriseIds), passphraseItems);
	}

	public void writeActivityEnterprises(
			long companyId, long activityId, List<EnterpriseConfigInput> configs) {
		activityEnterprisesMapper.delete(
				Wrappers.<ActivityEnterprises>lambdaQuery()
						.eq(ActivityEnterprises::getActivityId, activityId)
						.eq(ActivityEnterprises::getCompanyId, companyId));
		for (EnterpriseConfigInput c : configs) {
			ActivityEnterprises row = new ActivityEnterprises();
			row.setActivityId(activityId);
			row.setEnterpriseId(c.enterpriseId());
			row.setCompanyId(companyId);
			row.setPerCapitaLimitfee(c.perCapitaLimitfee());
			activityEnterprisesMapper.insert(row);
		}
	}

	public Integer getPerCapitaLimitfee(long companyId, long activityId, long enterpriseId) {
		ActivityEnterprises row =
				activityEnterprisesMapper.selectOne(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId)
								.eq(ActivityEnterprises::getEnterpriseId, enterpriseId)
								.last("LIMIT 1"));
		return row == null ? null : row.getPerCapitaLimitfee();
	}

	public int requirePerCapitaLimitfee(long companyId, long activityId, long enterpriseId) {
		Integer fee = getPerCapitaLimitfee(companyId, activityId, enterpriseId);
		return fee == null ? 0 : fee;
	}

	public Map<Long, List<Integer>> loadPerCapitaFeesByActivityIds(long companyId, List<Long> activityIds) {
		if (activityIds == null || activityIds.isEmpty()) {
			return Map.of();
		}
		List<ActivityEnterprises> rows =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.in(ActivityEnterprises::getActivityId, activityIds)
								.orderByAsc(ActivityEnterprises::getId));
		Map<Long, List<Integer>> out = new HashMap<>();
		for (ActivityEnterprises row : rows) {
			if (row.getActivityId() == null) {
				continue;
			}
			out.computeIfAbsent(row.getActivityId(), k -> new ArrayList<>())
					.add(row.getPerCapitaLimitfee() == null ? 0 : row.getPerCapitaLimitfee());
		}
		return out;
	}

	public List<Map<String, Object>> buildResponseList(
			long companyId, long activityId, boolean passphraseEnabled) {
		List<ActivityEnterprises> enterpriseRows =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId)
								.orderByAsc(ActivityEnterprises::getId));
		if (enterpriseRows.isEmpty()) {
			return List.of();
		}

		Map<Long, ActivityPassphraseEnterprise> passphraseByEnterprise = Map.of();
		if (passphraseEnabled) {
			List<ActivityPassphraseEnterprise> passphraseRows =
					activityPassphraseEnterpriseMapper.selectList(
							Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
									.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
									.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
			passphraseByEnterprise =
					passphraseRows.stream()
							.filter(r -> r.getEnterpriseId() != null)
							.collect(
									Collectors.toMap(
											ActivityPassphraseEnterprise::getEnterpriseId,
											r -> r,
											(a, b) -> a,
											LinkedHashMap::new));
		}

		Set<Long> enterpriseIds =
				enterpriseRows.stream()
						.map(ActivityEnterprises::getEnterpriseId)
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Enterprises> enterpriseById = loadEnterprises(companyId, enterpriseIds);

		List<Map<String, Object>> out = new ArrayList<>(enterpriseRows.size());
		for (ActivityEnterprises row : enterpriseRows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("enterprise_id", row.getEnterpriseId());
			Enterprises ent = enterpriseById.get(row.getEnterpriseId());
			m.put("enterprise_name", ent != null ? ent.getName() : null);
			m.put("enterprise_sn", ent != null ? ent.getEnterpriseSn() : null);
			m.put("per_capita_limitfee", row.getPerCapitaLimitfee());
			if (passphraseEnabled) {
				ActivityPassphraseEnterprise pass = passphraseByEnterprise.get(row.getEnterpriseId());
				m.put("participate_quota", pass != null ? pass.getParticipateQuota() : null);
				m.put("passphrase_code", pass != null ? pass.getPassphraseCode() : null);
			}
			out.add(m);
		}
		return out;
	}

	/** 校验更新请求中锁定的企业配置与库中一致；可参与名额允许修改。 */
	public List<EnterpriseConfigInput> assertConfigsUnchanged(
			long companyId,
			long activityId,
			List<Long> requestEnterpriseIds,
			Object enterpriseConfigsRaw,
			boolean passphraseEnabled) {
		List<ActivityEnterprises> existing =
				activityEnterprisesMapper.selectList(
						Wrappers.<ActivityEnterprises>lambdaQuery()
								.eq(ActivityEnterprises::getCompanyId, companyId)
								.eq(ActivityEnterprises::getActivityId, activityId)
								.orderByAsc(ActivityEnterprises::getId));
		List<Long> existingIds =
				existing.stream()
						.map(ActivityEnterprises::getEnterpriseId)
						.filter(Objects::nonNull)
						.toList();
		if (!new LinkedHashSet<>(existingIds).equals(new LinkedHashSet<>(requestEnterpriseIds))
				|| existingIds.size() != requestEnterpriseIds.size()) {
			throw new ResourceException("购买方式与企业额度配置创建后不可修改");
		}
		if (enterpriseConfigsRaw == null) {
			return List.of();
		}
		List<EnterpriseConfigInput> requestConfigs =
				parseAndValidate(enterpriseConfigsRaw, requestEnterpriseIds, passphraseEnabled);
		Map<Long, Integer> existingFee =
				existing.stream()
						.filter(r -> r.getEnterpriseId() != null)
						.collect(
								Collectors.toMap(
										ActivityEnterprises::getEnterpriseId,
										r -> r.getPerCapitaLimitfee() == null ? 0 : r.getPerCapitaLimitfee(),
										(a, b) -> a));
		Map<Long, ActivityPassphraseEnterprise> passphraseByEnterprise = Map.of();
		if (passphraseEnabled) {
			List<ActivityPassphraseEnterprise> passphraseRows =
					activityPassphraseEnterpriseMapper.selectList(
							Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
									.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
									.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
			passphraseByEnterprise =
					passphraseRows.stream()
							.filter(r -> r.getEnterpriseId() != null)
							.collect(
									Collectors.toMap(
											ActivityPassphraseEnterprise::getEnterpriseId,
											r -> r,
											(a, b) -> a));
		}
		for (EnterpriseConfigInput c : requestConfigs) {
			Integer dbFee = existingFee.get(c.enterpriseId());
			if (dbFee == null || !dbFee.equals(c.perCapitaLimitfee())) {
				throw new ResourceException("购买方式与企业额度配置创建后不可修改");
			}
			if (passphraseEnabled) {
				ActivityPassphraseEnterprise pass = passphraseByEnterprise.get(c.enterpriseId());
				if (pass == null) {
					throw new ResourceException("购买方式与企业额度配置创建后不可修改");
				}
				String reqCode = c.passphraseCode() == null ? "" : c.passphraseCode().trim();
				String dbCode = pass.getPassphraseCode() == null ? "" : pass.getPassphraseCode().trim();
				if (StringUtils.hasText(reqCode) && !reqCode.equals(dbCode)) {
					throw new ResourceException("购买方式与企业额度配置创建后不可修改");
				}
			}
		}
		return requestConfigs;
	}

	/** 更新时可改口令可参与名额；Redis 剩余 = 新名额 − 已占用人数。 */
	public void applyParticipateQuotaOnUpdate(
			long companyId,
			long activityId,
			List<EnterpriseConfigInput> requestConfigs,
			boolean passphraseEnabled) {
		if (!passphraseEnabled || requestConfigs == null || requestConfigs.isEmpty()) {
			return;
		}
		List<ActivityPassphraseEnterprise> passphraseRows =
				activityPassphraseEnterpriseMapper.selectList(
						Wrappers.<ActivityPassphraseEnterprise>lambdaQuery()
								.eq(ActivityPassphraseEnterprise::getCompanyId, companyId)
								.eq(ActivityPassphraseEnterprise::getActivityId, activityId));
		Map<Long, ActivityPassphraseEnterprise> passphraseByEnterprise =
				passphraseRows.stream()
						.filter(r -> r.getEnterpriseId() != null)
						.collect(
								Collectors.toMap(
										ActivityPassphraseEnterprise::getEnterpriseId,
										r -> r,
										(a, b) -> a));
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (EnterpriseConfigInput c : requestConfigs) {
			ActivityPassphraseEnterprise pass = passphraseByEnterprise.get(c.enterpriseId());
			if (pass == null) {
				throw new ResourceException("口令企业配置不存在");
			}
			if (Objects.equals(pass.getParticipateQuota(), c.participateQuota())) {
				continue;
			}
			if (c.participateQuota() == null || c.participateQuota() <= 0) {
				throw new BadRequestException("开启口令通道时请填写可参与名额");
			}
			long occupied =
					activityEnterpriseParticipateUserMapper.selectCount(
							Wrappers.<ActivityEnterpriseParticipateUser>lambdaQuery()
									.eq(ActivityEnterpriseParticipateUser::getCompanyId, companyId)
									.eq(ActivityEnterpriseParticipateUser::getActivityId, activityId)
									.eq(ActivityEnterpriseParticipateUser::getEnterpriseId, c.enterpriseId()));
			if (c.participateQuota() < occupied) {
				throw new ResourceException("可参与名额不能小于已占用人数");
			}
			pass.setParticipateQuota(c.participateQuota());
			pass.setUpdated(now);
			activityPassphraseEnterpriseMapper.updateById(pass);
			int remaining = c.participateQuota() - (int) occupied;
			passphraseParticipateQuotaRedisService.syncRemainingQuota(
					companyId, activityId, c.enterpriseId(), remaining);
		}
	}

	public static String formatQuotaDesc(List<Integer> feesFen) {
		if (feesFen == null || feesFen.isEmpty()) {
			return "";
		}
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Integer fen : feesFen) {
			int v = fen == null ? 0 : fen;
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		if (min == max) {
			return fenToDisplay(min);
		}
		return fenToDisplay(min) + "~" + fenToDisplay(max);
	}

	public static String fenToDisplay(int fen) {
		BigDecimal bd =
				BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return bd.stripTrailingZeros().toPlainString();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> coerceConfigList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			s = s.trim();
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				return OBJECT_MAPPER.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
			} catch (Exception e) {
				throw new BadRequestException("企业配置格式错误");
			}
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("企业配置格式错误");
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				throw new BadRequestException("企业配置格式错误");
			}
			out.add((Map<String, Object>) m);
		}
		return out;
	}

	private Map<Long, Enterprises> loadEnterprises(long companyId, Set<Long> enterpriseIds) {
		if (enterpriseIds.isEmpty()) {
			return Map.of();
		}
		List<Enterprises> list =
				enterprisesMapper.selectList(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.in(Enterprises::getId, enterpriseIds));
		LinkedHashMap<Long, Enterprises> out = new LinkedHashMap<>();
		for (Enterprises e : list) {
			if (e.getId() != null) {
				out.put(e.getId(), e);
			}
		}
		return out;
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
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public record EnterpriseConfigInput(
			Long enterpriseId, Integer perCapitaLimitfee, Integer participateQuota, String passphraseCode) {}
}
