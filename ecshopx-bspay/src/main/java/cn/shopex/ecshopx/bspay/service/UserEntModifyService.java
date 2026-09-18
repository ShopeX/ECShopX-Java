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
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserEntMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserEntModifyService {

	private static final String AUDIT_CARD_FAIL = "D";
	private static final String AUDIT_SUCCESS = "E";

	private static final List<String> USER_ENT_UPDATE_WHITELIST = List.of(
			"req_seq_id",
			"sys_id",
			"huifu_id",
			"company_id",
			"is_update",
			"reg_name",
			"license_code",
			"license_validity_type",
			"license_begin_date",
			"license_end_date",
			"reg_prov_id",
			"reg_area_id",
			"reg_district_id",
			"reg_detail",
			"legal_name",
			"legal_cert_no",
			"legal_cert_validity_type",
			"legal_cert_begin_date",
			"legal_cert_end_date",
			"contact_name",
			"contact_mobile",
			"ent_type",
			"audit_state",
			"audit_desc",
			"error_info",
			"created");

	private final UserEntCreateService userEntCreateService;
	private final UserEntMapper userEntMapper;
	private final UserCardMapper userCardMapper;
	private final EntryApplyMapper entryApplyMapper;
	private final BsPayOperatorResolveService bsPayOperatorResolveService;

	public UserEntModifyService(
			UserEntCreateService userEntCreateService,
			UserEntMapper userEntMapper,
			UserCardMapper userCardMapper,
			EntryApplyMapper entryApplyMapper,
			BsPayOperatorResolveService bsPayOperatorResolveService) {
		this.userEntCreateService = userEntCreateService;
		this.userEntMapper = userEntMapper;
		this.userCardMapper = userCardMapper;
		this.entryApplyMapper = entryApplyMapper;
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void modify(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		String legalCertNo = str(params.get("legal_cert_no"));
		String cardNo = str(params.get("card_no"));
		params.put("legal_cert_no", legalCertNo);
		params.put("card_no", cardNo);

		userEntCreateService.validateEntModifyBody(params);
		userEntCreateService.normalizeEntBodyParams(params);

		long entId = ((Number) params.get("id")).longValue();

		LambdaQueryWrapper<UserEnt> entQw = Wrappers.lambdaQuery();
		entQw.eq(UserEnt::getId, entId).eq(UserEnt::getCompanyId, companyId);
		UserEnt existing = userEntMapper.selectOne(entQw);
		if (existing == null) {
			throw new ResourceException("开户信息不存在");
		}

		boolean isSuccessUpdate =
				AUDIT_CARD_FAIL.equals(existing.getAuditState()) || AUDIT_SUCCESS.equals(existing.getAuditState());

		int now = (int) (System.currentTimeMillis() / 1000);

		log.info("modify data====>{}", params);

		LambdaUpdateWrapper<UserEnt> entUw = Wrappers.lambdaUpdate();
		entUw.eq(UserEnt::getId, entId).eq(UserEnt::getCompanyId, companyId);
		for (String col : USER_ENT_UPDATE_WHITELIST) {
			if (!params.containsKey(col)) {
				continue;
			}
			if (isSuccessUpdate && "legal_cert_no".equals(col)) {
				continue;
			}
			applyUserEntSet(entUw, col, params.get(col));
		}
		entUw.set(UserEnt::getUpdated, now);

		int rows = userEntMapper.update(null, entUw);
		if (rows != 1) {
			throw new ResourceException("企业用户信息更新失败");
		}

		int cardTypeInt = userEntCreateService.persistCardType(params.get("card_type"));
		LambdaUpdateWrapper<UserCard> cardUw = Wrappers.lambdaUpdate();
		cardUw.set(UserCard::getCardType, String.valueOf(cardTypeInt));
		cardUw.set(UserCard::getCardNo, str(params.get("card_no")));
		cardUw.set(UserCard::getProvId, str(params.get("prov_id")));
		cardUw.set(UserCard::getAreaId, str(params.get("area_id")));
		cardUw.set(UserCard::getBankCode, str(params.get("bank_code")));
		cardUw.set(UserCard::getBranchName, str(params.get("branch_name")));
		cardUw.set(UserCard::getMp, str(params.get("mp")));
		cardUw.set(UserCard::getCertNo, str(params.get("legal_cert_no")));
		cardUw.set(
				UserCard::getCertValidityType,
				userEntCreateService.persistLegalCertValidityType(params.get("legal_cert_validity_type")));
		cardUw.set(UserCard::getCertBeginDate, str(params.get("legal_cert_begin_date")));
		cardUw.set(UserCard::getCertEndDate, str(params.get("legal_cert_end_date")));
		if (!isSuccessUpdate) {
			cardUw.set(UserCard::getCardName, str(params.get("card_name")));
		}
		cardUw.set(UserCard::getUpdated, now);

		log.info(
				"modify cardInfo====> type={} card_no={} prov={} area={}",
				cardTypeInt,
				str(params.get("card_no")),
				str(params.get("prov_id")),
				str(params.get("area_id")));

		LambdaQueryWrapper<UserCard> cardQw = Wrappers.lambdaQuery();
		cardQw.eq(UserCard::getUserId, entId)
				.eq(UserCard::getCompanyId, companyId)
				.eq(UserCard::getUserType, "ent");
		UserCard card = userCardMapper.selectOne(cardQw);
		if (card == null) {
			throw new ResourceException("未查询到更新数据");
		}

		cardUw.eq(UserCard::getId, card.getId());
		int cRows = userCardMapper.update(null, cardUw);
		if (cRows != 1) {
			throw new ResourceException("结算卡更新失败");
		}

		BsPayOperatorResolveService.OperatorContext opCtx = bsPayOperatorResolveService.resolve(jwtMap);

		EntryApply entryApply = new EntryApply();
		entryApply.setUserType("ent");
		entryApply.setUserName(str(params.get("reg_name")));
		entryApply.setCompanyId(companyId);
		entryApply.setUserId(String.valueOf(entId));
		entryApply.setOperatorId(opCtx.operatorId());
		entryApply.setOperatorType(opCtx.operatorType());
		entryApply.setAddress("");
		entryApply.setStatus("WAIT_APPROVE");
		entryApply.setCreated(now);
		entryApply.setUpdated(now);

		log.info(
				"modify apply====> user_type=ent user_name={} user_id={} operator_id={} operator_type={}",
				str(params.get("reg_name")),
				entId,
				opCtx.operatorId(),
				opCtx.operatorType());

		int applyRows = entryApplyMapper.insert(entryApply);
		if (applyRows <= 0) {
			throw new ResourceException("开户申请创建失败");
		}
	}

	private void applyUserEntSet(LambdaUpdateWrapper<UserEnt> uw, String col, Object raw) {
		switch (col) {
			case "license_validity_type" -> uw.set(
					UserEnt::getLicenseValidityType, userEntCreateService.persistLicenseValidityType(raw));
			case "legal_cert_validity_type" -> uw.set(
					UserEnt::getLegalCertValidityType, userEntCreateService.persistLegalCertValidityType(raw));
			case "ent_type" -> uw.set(UserEnt::getEntType, userEntCreateService.persistEntType(raw));
			case "company_id" -> uw.set(UserEnt::getCompanyId, toLong(raw));
			case "is_update" -> uw.set(UserEnt::getIsUpdate, toInteger(raw));
			case "created" -> uw.set(UserEnt::getCreated, toInteger(raw));
			case "req_seq_id" -> uw.set(UserEnt::getReqSeqId, str(raw));
			case "sys_id" -> uw.set(UserEnt::getSysId, str(raw));
			case "huifu_id" -> uw.set(UserEnt::getHuifuId, str(raw));
			case "reg_name" -> uw.set(UserEnt::getRegName, str(raw));
			case "license_code" -> uw.set(UserEnt::getLicenseCode, str(raw));
			case "license_begin_date" -> uw.set(UserEnt::getLicenseBeginDate, str(raw));
			case "license_end_date" -> uw.set(UserEnt::getLicenseEndDate, str(raw));
			case "reg_prov_id" -> uw.set(UserEnt::getRegProvId, str(raw));
			case "reg_area_id" -> uw.set(UserEnt::getRegAreaId, str(raw));
			case "reg_district_id" -> uw.set(UserEnt::getRegDistrictId, str(raw));
			case "reg_detail" -> uw.set(UserEnt::getRegDetail, str(raw));
			case "legal_name" -> uw.set(UserEnt::getLegalName, str(raw));
			case "legal_cert_no" -> uw.set(UserEnt::getLegalCertNo, str(raw));
			case "legal_cert_begin_date" -> uw.set(UserEnt::getLegalCertBeginDate, str(raw));
			case "legal_cert_end_date" -> uw.set(UserEnt::getLegalCertEndDate, str(raw));
			case "contact_name" -> uw.set(UserEnt::getContactName, str(raw));
			case "contact_mobile" -> uw.set(UserEnt::getContactMobile, str(raw));
			case "audit_state" -> uw.set(UserEnt::getAuditState, str(raw));
			case "audit_desc" -> uw.set(UserEnt::getAuditDesc, str(raw));
			case "error_info" -> uw.set(UserEnt::getErrorInfo, str(raw));
			default -> {
				// whitelist 已穷尽
			}
		}
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(str(raw));
	}

	private static int toInteger(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(str(raw));
	}

	private static String str(Object o) {
		return o == null ? "" : Objects.toString(o, "").trim();
	}
}
