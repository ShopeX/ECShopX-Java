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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeMultiLangReadService;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivityRelShop;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityDataInfoService {

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final RegistrationActivityRelShopMapper registrationActivityRelShopMapper;
	private final AddressMapper addressMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final DistributorMapper distributorMapper;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final VipGradeMapper vipGradeMapper;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;

	public RegistrationActivityDataInfoService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			RegistrationActivityRelShopMapper registrationActivityRelShopMapper,
			AddressMapper addressMapper,
			EnterprisesMapper enterprisesMapper,
			DistributorMapper distributorMapper,
			DistributorSelfMetaService distributorSelfMetaService,
			MemberCardGradeMapper memberCardGradeMapper,
			VipGradeMapper vipGradeMapper,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.registrationActivityRelShopMapper = registrationActivityRelShopMapper;
		this.addressMapper = addressMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.distributorMapper = distributorMapper;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
	}

	public Object getDataInfo(String activityIdRaw, long jwtCompanyId, String requestLangTag) {
		long id = parseActivityIdIntvalStyle(activityIdRaw);
		if (id == 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("enterprise_list", new ArrayList<>());
			empty.put("distributor_list", new ArrayList<>());
			return empty;
		}

		RegistrationActivity entity = registrationActivityMapper.selectById(id);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> result = toDetailMap(entity);

		Long ownerCid = entity.getCompanyId();
		if (ownerCid != null) {
			registrationActivityOutsideMultiLangReadService.applyDetailOutsideLangOverrides(
					ownerCid, id, result, requestLangTag);
		}

		buildMemberLevelList(entity, result, requestLangTag);
		buildAreaName(result);
		buildEnterpriseList(result, jwtCompanyId);
		buildDistributorList(id, result);

		return result;
	}

	private void buildMemberLevelList(
			RegistrationActivity entity, Map<String, Object> result, String requestLangTag) {
		Object mlObj = result.get("member_level");
		String ml = mlObj == null ? "" : String.valueOf(mlObj);
		if (!StringUtils.hasText(ml) || "0".equals(ml.trim())) {
			return;
		}
		List<String> tokens = splitCommaPreserveOrder(ml);
		if (tokens.isEmpty()) {
			result.put("member_level_list", new ArrayList<>());
			return;
		}

		List<Map<String, Object>> memberLevelList = new ArrayList<>();
		List<Long> gradeIdIn = parseLongTokensWherePossible(tokens);
		if (!gradeIdIn.isEmpty()) {
			List<MemberCardGrade> cardGrades =
					memberCardGradeMapper.selectList(new LambdaQueryWrapper<MemberCardGrade>()
							.in(MemberCardGrade::getGradeId, gradeIdIn));
			for (MemberCardGrade g : cardGrades) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("grade_id", g.getGradeId());
				row.put("grade_name", g.getGradeName());
				memberLevelList.add(row);
			}
		}

		List<VipGrade> vipGrades = vipGradeMapper.selectList(
				new LambdaQueryWrapper<VipGrade>().in(VipGrade::getLvType, tokens));
		for (VipGrade v : vipGrades) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("lv_type", v.getLvType());
			row.put("grade_name", v.getGradeName());
			memberLevelList.add(row);
		}

		long ownerCidForLang = entity.getCompanyId() != null ? entity.getCompanyId() : 0L;
		List<Map<String, Object>> cardOnly = new ArrayList<>();
		for (Map<String, Object> row : memberLevelList) {
			if (row.containsKey("grade_id")) {
				cardOnly.add(row);
			}
		}
		memberCardGradeMultiLangReadService.applyOverlays(ownerCidForLang, cardOnly, requestLangTag);
		result.put("member_level_list", memberLevelList);
	}

	private void buildAreaName(Map<String, Object> result) {
		Object areaObj = result.get("area");
		String areaRaw = areaObj == null ? "" : String.valueOf(areaObj);
		if (!StringUtils.hasText(areaRaw)) {
			result.put("area_name", "");
			return;
		}
		List<String> idTokens = splitCommaPreserveOrder(areaRaw);
		List<Long> ids = new ArrayList<>();
		for (String t : idTokens) {
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (ids.isEmpty()) {
			result.put("area_name", "");
			return;
		}
		List<Address> rows = addressMapper.selectList(new LambdaQueryWrapper<Address>().in(Address::getId, ids));
		List<String> labels = new ArrayList<>();
		for (Address a : rows) {
			labels.add(a.getLabel() != null ? a.getLabel() : "");
		}
		result.put("area_name", String.join("", labels));
	}

	private void buildEnterpriseList(Map<String, Object> result, long jwtCompanyId) {
		result.put("enterprise_list", new ArrayList<>());
		Object entObj = result.get("enterprise_ids");
		String entRaw = entObj == null ? "" : String.valueOf(entObj);
		if (!StringUtils.hasText(entRaw) || "0".equals(entRaw.trim())) {
			return;
		}
		List<String> tokens = splitCommaPreserveOrder(entRaw);
		List<Long> eids = new ArrayList<>();
		for (String t : tokens) {
			try {
				eids.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (eids.isEmpty()) {
			return;
		}
		List<Enterprises> enterprises =
				enterprisesMapper.selectList(new LambdaQueryWrapper<Enterprises>().in(Enterprises::getId, eids));
		List<Map<String, Object>> list = new ArrayList<>();
		for (Enterprises e : enterprises) {
			list.add(enterpriseToRow(e));
		}

		Set<Long> storeIds = new LinkedHashSet<>();
		for (Enterprises e : enterprises) {
			Integer d = e.getDistributorId();
			if (d != null && d >= 0) {
				storeIds.add(d.longValue());
			}
		}

		Map<Long, Map<String, Object>> storeDataById = new LinkedHashMap<>();
		if (!storeIds.isEmpty()) {
			List<Distributor> distRows = distributorMapper.selectList(
					new LambdaQueryWrapper<Distributor>()
							.in(Distributor::getDistributorId, storeIds)
							.select(Distributor::getDistributorId, Distributor::getName, Distributor::getAddress));
			for (Distributor d : distRows) {
				if (d.getDistributorId() == null) {
					continue;
				}
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("distributor_id", d.getDistributorId());
				m.put("name", d.getName() != null ? d.getName() : "");
				m.put("address", d.getAddress() != null ? d.getAddress() : "");
				storeDataById.put(d.getDistributorId(), m);
			}
			storeDataById.put(0L, new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(jwtCompanyId)));
		}

		if (!storeDataById.isEmpty()) {
			for (Map<String, Object> row : list) {
				Object didObj = row.get("distributor_id");
				long distKey = 0L;
				if (didObj instanceof Number n) {
					distKey = n.longValue();
				}
				Map<String, Object> store = storeDataById.get(distKey);
				String distributorName = "";
				if (store != null) {
					Object nm = store.get("name");
					distributorName = nm != null ? Objects.toString(nm, "") : "";
				}
				row.put("distributor_name", distributorName);
			}
		}

		result.put("enterprise_list", list);
	}

	private void buildDistributorList(long activityId, Map<String, Object> result) {
		List<RegistrationActivityRelShop> rels = registrationActivityRelShopMapper.selectList(
				new LambdaQueryWrapper<RegistrationActivityRelShop>()
						.eq(RegistrationActivityRelShop::getActivityId, activityId));
		if (rels == null || rels.isEmpty()) {
			result.put("distributor_list", List.of());
			return;
		}
		RegistrationActivityRelShop first = rels.get(0);
		Long firstDid = first.getDistributorId();
		if (firstDid == null || firstDid == 0L) {
			result.put("distributor_list", List.of());
			return;
		}
		List<Long> relDistributorIds = new ArrayList<>();
		for (RegistrationActivityRelShop r : rels) {
			Long d = r.getDistributorId();
			if (d != null) {
				relDistributorIds.add(d);
			}
		}
		if (relDistributorIds.isEmpty()) {
			result.put("distributor_list", List.of());
			return;
		}
		List<Distributor> distRows = distributorMapper.selectList(
				new LambdaQueryWrapper<Distributor>()
						.in(Distributor::getDistributorId, relDistributorIds)
						.select(Distributor::getDistributorId, Distributor::getName, Distributor::getAddress));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Distributor d : distRows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("distributor_id", d.getDistributorId());
			m.put("name", d.getName() != null ? d.getName() : "");
			m.put("address", d.getAddress() != null ? d.getAddress() : "");
			rows.add(m);
		}
		result.put("distributor_list", rows);
	}

	private static Map<String, Object> enterpriseToRow(Enterprises e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("name", e.getName());
		m.put("enterprise_sn", e.getEnterpriseSn());
		m.put("logo", e.getLogo());
		m.put("qr_code_bg_image", e.getQrCodeBgImage());
		m.put("is_employee_check_enabled", e.getIsEmployeeCheckEnabled());
		m.put("auth_type", e.getAuthType());
		m.put("disabled", e.getDisabled());
		m.put("sort", e.getSort());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private static Map<String, Object> toDetailMap(RegistrationActivity entity) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("activity_id", entity.getActivityId());
		out.put("temp_id", entity.getTempId());
		out.put("activity_name", entity.getActivityName());
		out.put("start_time", entity.getStartTime());
		out.put("end_time", entity.getEndTime());
		out.put("join_limit", entity.getJoinLimit());
		out.put("is_sms_notice", entity.getIsSmsNotice());
		out.put("is_wxapp_notice", entity.getIsWxappNotice());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("company_id", entity.getCompanyId());
		out.put("area", entity.getArea());
		out.put("place", entity.getPlace());
		out.put("address", entity.getAddress());
		out.put("intro", entity.getIntro());
		out.put("show_fields", entity.getShowFields());
		out.put("pics", entity.getPics());
		out.put("gift_points", entity.getGiftPoints());
		out.put("is_allow_duplicate", entity.getIsAllowDuplicate());
		out.put("is_allow_cancel", entity.getIsAllowCancel());
		out.put("is_offline_verify", entity.getIsOfflineVerify());
		out.put("is_need_check", entity.getIsNeedCheck());
		out.put("is_white_list", entity.getIsWhiteList());
		out.put("enterprise_ids", entity.getEnterpriseIds());
		out.put("group_no", entity.getGroupNo());
		out.put("member_level", entity.getMemberLevel());
		out.put("distributor_ids", entity.getDistributorIds());
		out.put("join_tips", entity.getJoinTips());
		out.put("submit_form_tips", entity.getSubmitFormTips());
		out.put("content", entity.getContent());
		out.put("distributor_id", entity.getDistributorId());
		return out;
	}

	private static List<String> splitCommaPreserveOrder(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String p : raw.split(",")) {
			String t = p.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
	}

	private static List<Long> parseLongTokensWherePossible(List<String> tokens) {
		List<Long> out = new ArrayList<>();
		for (String t : tokens) {
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	/** 前导空白、可选符号后读取连续十进制数字；空串或无数字段返回 0；超出 {@code long} 范围时钳位。 */
	private static long parseActivityIdIntvalStyle(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw;
		int len = s.length();
		int i = 0;
		while (i < len && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		boolean negative = false;
		char c = s.charAt(i);
		if (c == '+') {
			i++;
		} else if (c == '-') {
			negative = true;
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		int startDigits = i;
		while (i < len && Character.isDigit(s.charAt(i))) {
			i++;
		}
		if (i == startDigits) {
			return 0L;
		}
		String digitStr = s.substring(startDigits, i);
		try {
			BigInteger bi = new BigInteger(negative ? "-" + digitStr : digitStr);
			if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
				return Long.MAX_VALUE;
			}
			if (bi.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
				return Long.MIN_VALUE;
			}
			return bi.longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
