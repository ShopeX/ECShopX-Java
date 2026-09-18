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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.dispatch.MerchantAuditFailNoticeDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.MerchantAuditSuccessNoticeDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.port.MerchantMainOperatorProvisioner;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantSettlementApplyAuditService {

	public static final String AUDIT_ONGOING = "1";
	public static final String AUDIT_SUCC = "2";
	public static final String AUDIT_FAIL = "3";

	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantMapper merchantMapper;
	private final MerchantOutsideLangWriteService merchantOutsideLangWriteService;
	private final MerchantMainOperatorProvisioner merchantMainOperatorProvisioner;
	private final MerchantAuditSuccessNoticeDispatchPublisher merchantAuditSuccessNoticePublisher;
	private final MerchantAuditFailNoticeDispatchPublisher merchantAuditFailNoticePublisher;

	public MerchantSettlementApplyAuditService(
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantMapper merchantMapper,
			MerchantOutsideLangWriteService merchantOutsideLangWriteService,
			MerchantMainOperatorProvisioner merchantMainOperatorProvisioner,
			MerchantAuditSuccessNoticeDispatchPublisher merchantAuditSuccessNoticePublisher,
			MerchantAuditFailNoticeDispatchPublisher merchantAuditFailNoticePublisher) {
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantMapper = merchantMapper;
		this.merchantOutsideLangWriteService = merchantOutsideLangWriteService;
		this.merchantMainOperatorProvisioner = merchantMainOperatorProvisioner;
		this.merchantAuditSuccessNoticePublisher = merchantAuditSuccessNoticePublisher;
		this.merchantAuditFailNoticePublisher = merchantAuditFailNoticePublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void settlementApplyAudit(Map<String, Object> params, String acceptLanguageHeader) {
		try {
			long jwtCompanyId = toLong(params.get("company_id"));
			long applyId = toLong(params.get("id"));

			MerchantSettlementApply info = merchantSettlementApplyMapper.selectById(applyId);
			if (info == null) {
				throw new ResourceException("入驻申请查询失败");
			}
			if (!AUDIT_ONGOING.equals(info.getAuditStatus())) {
				throw new ResourceException("入驻申请当前无需审核");
			}

			String auditStatus = String.valueOf(params.get("audit_status"));
			if (AUDIT_SUCC.equals(auditStatus)) {
				auditSucc(info, acceptLanguageHeader);
			} else if (AUDIT_FAIL.equals(auditStatus)) {
				auditFail(info);
			} else {
				throw new ResourceException("入驻申请审核失败,请核实后再试");
			}

			boolean auditGoods = booleanFromParam(params.get("audit_goods"));
			int now = (int) (System.currentTimeMillis() / 1000L);

			LambdaUpdateWrapper<MerchantSettlementApply> uw = new LambdaUpdateWrapper<>();
			uw.eq(MerchantSettlementApply::getCompanyId, jwtCompanyId)
					.eq(MerchantSettlementApply::getId, applyId)
					.set(MerchantSettlementApply::getAuditStatus, auditStatus)
					.set(MerchantSettlementApply::isAuditGoods, auditGoods)
					.set(MerchantSettlementApply::getUpdated, now);

			if (params.containsKey("audit_memo")) {
				Object am = params.get("audit_memo");
				if (am == null) {
					uw.set(MerchantSettlementApply::getAuditMemo, null);
				} else if (am instanceof String s) {
					uw.set(MerchantSettlementApply::getAuditMemo, s);
				} else {
					uw.set(MerchantSettlementApply::getAuditMemo, String.valueOf(am));
				}
			}

			int rows = merchantSettlementApplyMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg != null && !msg.isEmpty() ? msg : "操作失败");
		}
	}

	private void auditSucc(MerchantSettlementApply info, String acceptLanguageHeader) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Merchant merchant = new Merchant();
		merchant.setCompanyId(info.getCompanyId());
		merchant.setSettlementApplyId(info.getId());
		merchant.setMerchantName(str(info.getMerchantName()));
		merchant.setMerchantTypeId(info.getMerchantTypeId());
		merchant.setSettledType(str(info.getSettledType()));
		merchant.setSocialCreditCodeId(str(info.getSocialCreditCodeId()));
		merchant.setProvince(str(info.getProvince()));
		merchant.setCity(str(info.getCity()));
		merchant.setArea(str(info.getArea()));
		merchant.setRegionsId(str(info.getRegionsId()));
		merchant.setAddress(str(info.getAddress()));
		merchant.setLegalName(str(info.getLegalName()));
		merchant.setLegalCertId(info.getLegalCertId());
		merchant.setLegalMobile(str(info.getLegalMobile()));
		merchant.setEmail("");
		merchant.setBankAcctType(str(info.getBankAcctType()));
		merchant.setCardIdMask(str(info.getCardIdMask()));
		merchant.setBankName(str(info.getBankName()));
		merchant.setBankMobile(str(info.getBankMobile()));
		merchant.setLicenseUrl(str(info.getLicenseUrl()));
		merchant.setLegalCertidFrontUrl(str(info.getLegalCertidFrontUrl()));
		merchant.setLegalCertIdBackUrl(str(info.getLegalCertIdBackUrl()));
		merchant.setBankCardFrontUrl(str(info.getBankCardFrontUrl()));
		merchant.setContractUrl("");
		merchant.setSettledSuccSendsms("1");
		merchant.setAuditGoods(info.isAuditGoods());
		merchant.setSource(str(info.getSource()));
		merchant.setDisabled(false);
		merchant.setCreated(now);
		merchant.setUpdated(now);

		merchantMapper.insert(merchant);
		Long merchantId = merchant.getId();
		if (merchantId == null) {
			throw new ResourceException("操作失败");
		}

		Map<String, Object> langSourceMap = new LinkedHashMap<>();
		langSourceMap.put("merchant_name", str(info.getMerchantName()));
		langSourceMap.put("province", str(info.getProvince()));
		langSourceMap.put("city", str(info.getCity()));
		langSourceMap.put("area", str(info.getArea()));
		langSourceMap.put("address", str(info.getAddress()));
		langSourceMap.put("legal_name", str(info.getLegalName()));

		merchantOutsideLangWriteService.applyAfterInsert(
				merchantId, info.getCompanyId(), langSourceMap, acceptLanguageHeader);

		String mobilePlain = info.getMobile();
		merchantMainOperatorProvisioner.createMainMerchantOperatorWithoutPassword(
				info.getCompanyId(), mobilePlain, mobilePlain, merchantId);

		merchantAuditSuccessNoticePublisher.publish(info.getCompanyId(), mobilePlain);
	}

	private void auditFail(MerchantSettlementApply info) {
		merchantAuditFailNoticePublisher.publish(info.getCompanyId(), str(info.getMobile()));
	}

	private static String str(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	private static boolean booleanFromParam(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}
}
