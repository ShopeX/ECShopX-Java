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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.EntryApply;
import cn.shopex.ecshopx.bspay.domain.UserCard;
import cn.shopex.ecshopx.bspay.domain.UserEnt;
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserEntGetService {

	private static final Logger log = LoggerFactory.getLogger(UserEntGetService.class);

	private final BsPayOperatorResolveService bsPayOperatorResolveService;
	private final EntryApplyMapper entryApplyMapper;
	private final UserEntMapper userEntMapper;
	private final UserIndvMapper userIndvMapper;
	private final UserCardMapper userCardMapper;

	public UserEntGetService(
			BsPayOperatorResolveService bsPayOperatorResolveService,
			EntryApplyMapper entryApplyMapper,
			UserEntMapper userEntMapper,
			UserIndvMapper userIndvMapper,
			UserCardMapper userCardMapper) {
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
		this.entryApplyMapper = entryApplyMapper;
		this.userEntMapper = userEntMapper;
		this.userIndvMapper = userIndvMapper;
		this.userCardMapper = userCardMapper;
	}

	public Map<String, Object> get(long companyId, Map<String, Object> jwtMap) {
		BsPayOperatorResolveService.OperatorContext opCtx = bsPayOperatorResolveService.resolve(jwtMap);
		int resolvedOperatorId = opCtx.operatorId();

		LambdaQueryWrapper<EntryApply> wrapper =
				new LambdaQueryWrapper<EntryApply>()
						.eq(EntryApply::getCompanyId, companyId)
						.eq(EntryApply::getOperatorId, resolvedOperatorId)
						.orderByDesc(EntryApply::getCreated)
						.last("LIMIT 1");

		EntryApply apply = entryApplyMapper.selectOne(wrapper);
		log.info(
				"user_ent get user info: companyId={}, resolvedOperatorId={}, entryApplyHit={}",
				companyId,
				resolvedOperatorId,
				apply != null);
		if (apply == null) {
			throw new ResourceException("用户信息不存在");
		}

		String rawUserId = apply.getUserId();
		if (rawUserId == null || rawUserId.trim().isEmpty()) {
			throw new ResourceException("获取用户进件信息失败");
		}
		long userPk;
		try {
			userPk = Long.parseLong(rawUserId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("获取用户进件信息失败");
		}

		String userType = apply.getUserType() == null ? "" : apply.getUserType().trim();
		LinkedHashMap<String, Object> merged;
		if ("ent".equals(userType)) {
			UserEnt ent = userEntMapper.selectById(userPk);
			if (ent == null) {
				throw new ResourceException("用户信息不存在");
			}
			merged = userEntToSnakeMap(ent);
		} else if ("indv".equals(userType)) {
			UserIndv indv = userIndvMapper.selectById(userPk);
			if (indv == null) {
				throw new ResourceException("用户信息不存在");
			}
			merged = userIndvToSnakeMap(indv);
		} else {
			throw new ResourceException("获取用户进件信息失败");
		}

		merged.put("user_type", apply.getUserType());
		merged.put(
				"approved_time",
				apply.getUpdated() == null ? null : String.valueOf(apply.getUpdated().intValue()));

		UserCard card =
				userCardMapper.selectOne(
						new LambdaQueryWrapper<UserCard>()
								.eq(UserCard::getUserId, userPk)
								.eq(UserCard::getCompanyId, companyId)
								.eq(UserCard::getUserType, userType)
								.last("LIMIT 1"));
		if (card != null) {
			Map<String, Object> cardMap = userCardToSnakeMap(card);
			cardMap.remove("id");
			for (Map.Entry<String, Object> entry : cardMap.entrySet()) {
				merged.put(entry.getKey(), entry.getValue());
			}
		}

		Object entTypeVal = merged.get("ent_type");
		merged.put("ent_type", entTypeVal == null ? "" : String.valueOf(entTypeVal).trim());

		merged.put("disabled_type", "");
		String auditForFlag =
				merged.get("audit_state") == null ? "" : merged.get("audit_state").toString().trim();
		if (Objects.equals("D", auditForFlag)) {
			merged.put("disabled_type", "user");
		} else if (Objects.equals("E", auditForFlag)) {
			merged.put("disabled_type", "all");
		}

		return merged;
	}

	private static LinkedHashMap<String, Object> userEntToSnakeMap(UserEnt e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("req_seq_id", nzStr(e.getReqSeqId()));
		m.put("sys_id", nzStr(e.getSysId()));
		m.put("huifu_id", nzStr(e.getHuifuId()));
		m.put("company_id", e.getCompanyId());
		m.put("is_update", e.getIsUpdate());
		m.put("reg_name", nzStr(e.getRegName()));
		m.put("license_code", nzStr(e.getLicenseCode()));
		m.put("license_validity_type", e.getLicenseValidityType());
		m.put("license_begin_date", nzStr(e.getLicenseBeginDate()));
		m.put("license_end_date", nzStr(e.getLicenseEndDate()));
		m.put("reg_prov_id", nzStr(e.getRegProvId()));
		m.put("reg_area_id", nzStr(e.getRegAreaId()));
		m.put("reg_district_id", nzStr(e.getRegDistrictId()));
		m.put("reg_detail", nzStr(e.getRegDetail()));
		m.put("legal_name", nzStr(e.getLegalName()));
		m.put("legal_cert_no", nzStr(e.getLegalCertNo()));
		m.put("legal_cert_validity_type", e.getLegalCertValidityType());
		m.put("legal_cert_begin_date", nzStr(e.getLegalCertBeginDate()));
		m.put("legal_cert_end_date", nzStr(e.getLegalCertEndDate()));
		m.put("contact_name", nzStr(e.getContactName()));
		m.put("contact_mobile", nzStr(e.getContactMobile()));
		m.put("ent_type", e.getEntType());
		m.put("audit_state", nzStr(e.getAuditState()));
		m.put("audit_desc", nzStr(e.getAuditDesc()));
		m.put("error_info", nzStr(e.getErrorInfo()));
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private static LinkedHashMap<String, Object> userIndvToSnakeMap(UserIndv u) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", u.getId());
		m.put("req_seq_id", nzStr(u.getReqSeqId()));
		m.put("sys_id", nzStr(u.getSysId()));
		m.put("huifu_id", nzStr(u.getHuifuId()));
		m.put("company_id", u.getCompanyId());
		m.put("is_update", u.getIsUpdate());
		m.put("name", nzStr(u.getName()));
		m.put("cert_no", nzStr(u.getCertNo()));
		m.put("cert_validity_type", u.getCertValidityType());
		m.put("cert_begin_date", nzStr(u.getCertBeginDate()));
		m.put("cert_end_date", nzStr(u.getCertEndDate()));
		m.put("mobile_no", nzStr(u.getMobileNo()));
		m.put("audit_state", nzStr(u.getAuditState()));
		m.put("audit_desc", nzStr(u.getAuditDesc()));
		m.put("error_info", nzStr(u.getErrorInfo()));
		m.put("created", u.getCreated());
		m.put("updated", u.getUpdated());
		return m;
	}

	private static LinkedHashMap<String, Object> userCardToSnakeMap(UserCard c) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", c.getId());
		m.put("sys_id", nzStr(c.getSysId()));
		m.put("huifu_id", nzStr(c.getHuifuId()));
		m.put("user_id", c.getUserId());
		m.put("req_seq_id", nzStr(c.getReqSeqId()));
		m.put("company_id", c.getCompanyId());
		m.put("user_type", nzStr(c.getUserType()));
		m.put("card_type", nzStr(c.getCardType()));
		m.put("card_name", nzStr(c.getCardName()));
		m.put("card_no", nzStr(c.getCardNo()));
		m.put("prov_id", nzStr(c.getProvId()));
		m.put("area_id", nzStr(c.getAreaId()));
		m.put("bank_code", nzStr(c.getBankCode()));
		m.put("branch_name", nzStr(c.getBranchName()));
		m.put("cert_no", nzStr(c.getCertNo()));
		m.put("cert_validity_type", c.getCertValidityType());
		m.put("cert_begin_date", nzStr(c.getCertBeginDate()));
		m.put("cert_end_date", nzStr(c.getCertEndDate()));
		m.put("mp", nzStr(c.getMp()));
		m.put("apply_no", nzStr(c.getApplyNo()));
		m.put("audit_state", nzStr(c.getAuditState()));
		m.put("audit_desc", nzStr(c.getAuditDesc()));
		m.put("error_info", nzStr(c.getErrorInfo()));
		m.put("created", c.getCreated());
		m.put("updated", c.getUpdated());
		return m;
	}

	private static String nzStr(String s) {
		return s == null ? "" : s;
	}
}
