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
import cn.shopex.ecshopx.bspay.domain.UserIndv;
import cn.shopex.ecshopx.bspay.mapper.EntryApplyMapper;
import cn.shopex.ecshopx.bspay.mapper.UserCardMapper;
import cn.shopex.ecshopx.bspay.mapper.UserIndvMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserIndvModifyService {

	private static final String AUDIT_CARD_FAIL = "D";
	private static final String AUDIT_SUCCESS = "E";

	private final UserIndvCreateService userIndvCreateService;
	private final UserIndvMapper userIndvMapper;
	private final UserCardMapper userCardMapper;
	private final EntryApplyMapper entryApplyMapper;
	private final BsPayOperatorResolveService bsPayOperatorResolveService;

	public UserIndvModifyService(
			UserIndvCreateService userIndvCreateService,
			UserIndvMapper userIndvMapper,
			UserCardMapper userCardMapper,
			EntryApplyMapper entryApplyMapper,
			BsPayOperatorResolveService bsPayOperatorResolveService) {
		this.userIndvCreateService = userIndvCreateService;
		this.userIndvMapper = userIndvMapper;
		this.userCardMapper = userCardMapper;
		this.entryApplyMapper = entryApplyMapper;
		this.bsPayOperatorResolveService = bsPayOperatorResolveService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void modify(long companyId, Map<String, Object> jwtMap, Map<String, Object> body) {
		Map<String, Object> params = new LinkedHashMap<>(body);
		params.put("company_id", companyId);
		String certNo = UserIndvCreateService.trimBspayParamString(params.get("cert_no"));
		String cardNo = UserIndvCreateService.trimBspayParamString(params.get("card_no"));
		params.put("cert_no", certNo);
		params.put("card_no", cardNo);

		userIndvCreateService.validateIndvModifyPrerequisites(params);
		userIndvCreateService.checkIndvCertAndRegions(params);

		long indvId = ((Number) params.get("id")).longValue();

		LambdaQueryWrapper<UserIndv> q = Wrappers.lambdaQuery();
		q.eq(UserIndv::getId, indvId).eq(UserIndv::getCompanyId, companyId);
		UserIndv existing = userIndvMapper.selectOne(q);
		if (existing == null) {
			throw new ResourceException("开户信息不存在");
		}

		boolean isSuccessUpdate =
				AUDIT_CARD_FAIL.equals(existing.getAuditState()) || AUDIT_SUCCESS.equals(existing.getAuditState());

		int now = (int) (System.currentTimeMillis() / 1000);
		log.info("modify data====>{}", params);

		LambdaUpdateWrapper<UserIndv> uw = Wrappers.lambdaUpdate();
		uw.eq(UserIndv::getId, indvId).eq(UserIndv::getCompanyId, companyId);
		uw.set(UserIndv::getName, UserIndvCreateService.trimBspayParamString(params.get("name")));
		if (!isSuccessUpdate) {
			uw.set(UserIndv::getCertNo, certNo);
		}
		uw.set(
				UserIndv::getCertValidityType,
				UserIndvCreateService.parseCertValidityTypeForPersist(params.get("cert_validity_type")));
		uw.set(UserIndv::getCertBeginDate, UserIndvCreateService.trimBspayParamString(params.get("cert_begin_date")));
		uw.set(UserIndv::getCertEndDate, UserIndvCreateService.trimBspayParamString(params.get("cert_end_date")));
		uw.set(UserIndv::getMobileNo, UserIndvCreateService.trimBspayParamString(params.get("mobile_no")));
		uw.set(UserIndv::getAuditState, "A");
		uw.set(UserIndv::getAuditDesc, "");
		uw.set(UserIndv::getUpdated, now);

		int rows = userIndvMapper.update(null, uw);
		if (rows != 1) {
			throw new ResourceException("个人用户信息更新失败");
		}

		LambdaQueryWrapper<UserCard> cardQw = Wrappers.lambdaQuery();
		cardQw.eq(UserCard::getUserId, indvId)
				.eq(UserCard::getCompanyId, companyId)
				.eq(UserCard::getUserType, "indv");
		UserCard card = userCardMapper.selectOne(cardQw);
		if (card == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LambdaUpdateWrapper<UserCard> cardUw = Wrappers.lambdaUpdate();
		if (!isSuccessUpdate) {
			cardUw.set(UserCard::getCardName, UserIndvCreateService.trimBspayParamString(params.get("name")));
		}
		cardUw.set(UserCard::getCardNo, cardNo);
		cardUw.set(UserCard::getProvId, UserIndvCreateService.trimBspayParamString(params.get("prov_id")));
		cardUw.set(UserCard::getAreaId, UserIndvCreateService.trimBspayParamString(params.get("area_id")));
		cardUw.set(UserCard::getBankCode, UserIndvCreateService.trimBspayParamString(params.get("bank_code")));
		cardUw.set(UserCard::getBranchName, UserIndvCreateService.trimBspayParamString(params.get("branch_name")));
		cardUw.set(UserCard::getCertNo, certNo);
		cardUw.set(
				UserCard::getCertValidityType,
				UserIndvCreateService.parseCertValidityTypeForPersist(params.get("cert_validity_type")));
		cardUw.set(UserCard::getCertBeginDate, UserIndvCreateService.trimBspayParamString(params.get("cert_begin_date")));
		cardUw.set(UserCard::getCertEndDate, UserIndvCreateService.trimBspayParamString(params.get("cert_end_date")));
		cardUw.set(UserCard::getMp, UserIndvCreateService.trimBspayParamString(params.get("mp")));
		cardUw.set(UserCard::getUpdated, now);

		log.info(
				"modify cardInfo====> card_no={} prov={} area={}",
				cardNo,
				UserIndvCreateService.trimBspayParamString(params.get("prov_id")),
				UserIndvCreateService.trimBspayParamString(params.get("area_id")));

		cardUw.eq(UserCard::getId, card.getId());
		int cRows = userCardMapper.update(null, cardUw);
		if (cRows != 1) {
			throw new ResourceException("结算卡更新失败");
		}

		BsPayOperatorResolveService.OperatorContext opCtx = bsPayOperatorResolveService.resolve(jwtMap);

		EntryApply entryApply = new EntryApply();
		entryApply.setUserType("indv");
		entryApply.setUserName(UserIndvCreateService.trimBspayParamString(params.get("name")));
		entryApply.setCompanyId(companyId);
		entryApply.setUserId(String.valueOf(indvId));
		entryApply.setOperatorId(opCtx.operatorId());
		entryApply.setOperatorType(opCtx.operatorType());
		entryApply.setAddress("");
		entryApply.setStatus("WAIT_APPROVE");
		entryApply.setCreated(now);
		entryApply.setUpdated(now);

		log.info(
				"modify apply====> user_type=indv user_name={} user_id={} operator_id={} operator_type={}",
				UserIndvCreateService.trimBspayParamString(params.get("name")),
				indvId,
				opCtx.operatorId(),
				opCtx.operatorType());

		int applyRows = entryApplyMapper.insert(entryApply);
		if (applyRows <= 0) {
			throw new ResourceException("开户申请创建失败");
		}
	}
}
