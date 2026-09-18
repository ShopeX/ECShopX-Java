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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PromoterChangePromoterService {

	private final PromoterMapper promoterMapper;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService;
	private final PromoterUserChangeEligibilityService eligibilityService;

	public PromoterChangePromoterService(
			PromoterMapper promoterMapper,
			PromoterIdentityMapper promoterIdentityMapper,
			MemberAccountService memberAccountService,
			PromoterGradeUpgradeTriggerService promoterGradeUpgradeTriggerService,
			PromoterUserChangeEligibilityService eligibilityService) {
		this.promoterMapper = promoterMapper;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.memberAccountService = memberAccountService;
		this.promoterGradeUpgradeTriggerService = promoterGradeUpgradeTriggerService;
		this.eligibilityService = eligibilityService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> changePromoter(long companyId, long userId, boolean force, Map<String, Object> params) {
		if (!force) {
			if (!eligibilityService.userIsChangePromoter(companyId, userId, false)) {
				throw new ResourceException("不满足条件");
			}
		}
		Promoter info = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getUserId, userId)
				.last("LIMIT 1"));

		Map<String, Object> rowData = new LinkedHashMap<>();
		Object promoterNameCell = 0;
		Object regionsIdCell = 0;
		Object addressCell = 0;
		if (params != null) {
			promoterNameCell = params.getOrDefault("promoter_name", 0);
			regionsIdCell = params.getOrDefault("regions_id", 0);
			addressCell = params.getOrDefault("address", 0);
		}
		rowData.put("promoter_name", promoterNameCell);
		rowData.put("regions_id", regionsIdCell);
		rowData.put("address", addressCell);
		if (params != null && toLongParam(params.get("identity_id")) > 0L) {
			mergePromoterIdentity(companyId, params, rowData);
		}

		if (info == null) {
			Long inviterUserId = memberAccountService.findInviterUserId(companyId, userId);
			Promoter insert = buildInsertRow(companyId, userId, inviterUserId);
			promoterMapper.insert(insert);
			if (insert.getPid() != null
					&& inviterUserId != null
					&& inviterUserId > 0L) {
				promoterGradeUpgradeTriggerService.upgradeGrade(companyId, inviterUserId);
			}
			info = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
					.eq(Promoter::getUserId, userId)
					.last("LIMIT 1"));
		}

		if (info == null) {
			throw new ResourceException("更新失败");
		}
		if (!Objects.equals(info.getCompanyId(), companyId)) {
			throw new ResourceException("数据异常");
		}
		if (Objects.equals(info.getIsPromoter(), 1)) {
			throw new ResourceException("该用户已经是推广员");
		}

		LambdaUpdateWrapper<Promoter> uw = new LambdaUpdateWrapper<Promoter>()
				.eq(Promoter::getUserId, userId)
				.set(Promoter::getIsPromoter, 1)
				.set(Promoter::getDisabled, 0);
		applyRowDataToWrapper(uw, rowData);

		int rows = promoterMapper.update(null, uw);
		if (rows <= 0) {
			throw new ResourceException("更新失败");
		}
		promoterGradeUpgradeTriggerService.upgradeGrade(companyId, userId);

		info = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getUserId, userId)
				.last("LIMIT 1"));
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("pid", info != null ? info.getPid() : null);
		return Map.of("list", List.of(first));
	}

	private Promoter buildInsertRow(long companyId, long userId, Long inviterUserId) {
		Promoter insert = new Promoter();
		insert.setUserId(userId);
		insert.setCompanyId(companyId);
		insert.setIdentityId(0L);
		insert.setIsSubordinates(0);
		insert.setGradeLevel(1);
		insert.setIsPromoter(0);
		insert.setDisabled(0);
		insert.setShopStatus(0);
		insert.setIsBuy(0);
		insert.setPromoterName("");
		insert.setRegionsId(null);
		insert.setAddress("");
		int now = (int) (System.currentTimeMillis() / 1000L);
		insert.setCreated(now);
		insert.setUpdated(now);
		if (inviterUserId != null && inviterUserId > 0L) {
			Promoter inviterRow = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
					.eq(Promoter::getCompanyId, companyId)
					.eq(Promoter::getUserId, inviterUserId)
					.last("LIMIT 1"));
			if (inviterRow != null && Objects.equals(inviterRow.getIsPromoter(), 1)) {
				insert.setPid(inviterRow.getId());
				insert.setPmobile(memberAccountService.findMobileStored(companyId, inviterUserId));
				insert.setPname(inviterRow.getPromoterName());
			}
		}
		return insert;
	}

	private void mergePromoterIdentity(long companyId, Map<String, Object> params, Map<String, Object> rowData) {
		long identityId = parseLongStrictForIdentity(params.get("identity_id"));
		PromoterIdentity idRow = promoterIdentityMapper.selectOne(new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId)
				.eq(PromoterIdentity::getId, identityId)
				.last("LIMIT 1"));
		if (idRow == null) {
			throw new ResourceException("推广员身份错误");
		}
		if (Objects.equals(idRow.getIsSubordinates(), 1)) {
			rowData.clear();
			rowData.put("identity_id", idRow.getId());
			rowData.put("is_subordinates", idRow.getIsSubordinates());
			return;
		}
		rowData.put("identity_id", idRow.getId());
		rowData.put("is_subordinates", idRow.getIsSubordinates());
		Object rawName = params.get("promoter_name");
		if (StringUtils.hasText(String.valueOf(rawName != null ? rawName : "").trim())) {
			rowData.put("promoter_name", String.valueOf(params.get("promoter_name")).trim());
		} else {
			rowData.put("promoter_name", idRow.getName());
		}
		long pid = toLongPidParam(params.get("pid"));
		if (pid <= 0L) {
			throw new ResourceException("上级推广员信息错误");
		}
		Promoter parent = promoterMapper.selectOne(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getCompanyId, companyId)
				.eq(Promoter::getUserId, pid)
				.last("LIMIT 1"));
		if (parent == null || !Objects.equals(parent.getIsPromoter(), 1)) {
			throw new ResourceException("上级推广员信息错误");
		}
		rowData.put("pid", pid);
		rowData.put("pmobile", nullToEmpty(params.get("pmobile")));
		rowData.put("pname", nullToEmpty(params.get("pname")));
	}

	private static long parseLongStrictForIdentity(Object v) {
		if (v == null) {
			throw new ResourceException("推广员身份错误");
		}
		try {
			if (v instanceof Number n) {
				long id = n.longValue();
				if (id <= 0L) {
					throw new ResourceException("推广员身份错误");
				}
				return id;
			}
			long id = Long.parseLong(String.valueOf(v).trim());
			if (id <= 0L) {
				throw new ResourceException("推广员身份错误");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new ResourceException("推广员身份错误");
		}
	}

	private static long toLongPidParam(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("上级推广员信息错误");
		}
	}

	private static String nullToEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private static long toLongParam(Object v) {
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

	private static void applyRowDataToWrapper(LambdaUpdateWrapper<Promoter> uw, Map<String, Object> rowData) {
		if (rowData.containsKey("promoter_name")) {
			uw.set(Promoter::getPromoterName, stringifyCell(rowData.get("promoter_name")));
		}
		if (rowData.containsKey("regions_id")) {
			uw.set(Promoter::getRegionsId, stringifyCell(rowData.get("regions_id")));
		}
		if (rowData.containsKey("address")) {
			uw.set(Promoter::getAddress, stringifyCell(rowData.get("address")));
		}
		if (rowData.containsKey("identity_id")) {
			uw.set(Promoter::getIdentityId, toLongBoxed(rowData.get("identity_id")));
		}
		if (rowData.containsKey("is_subordinates")) {
			uw.set(Promoter::getIsSubordinates, toInteger(rowData.get("is_subordinates")));
		}
		if (rowData.containsKey("pid")) {
			uw.set(Promoter::getPid, toLongBoxed(rowData.get("pid")));
		}
		if (rowData.containsKey("pmobile")) {
			uw.set(Promoter::getPmobile, rowData.get("pmobile") == null ? null : String.valueOf(rowData.get("pmobile")));
		}
		if (rowData.containsKey("pname")) {
			uw.set(Promoter::getPname, rowData.get("pname") == null ? null : String.valueOf(rowData.get("pname")));
		}
	}

	private static String stringifyCell(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private static Long toLongBoxed(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static Integer toInteger(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
