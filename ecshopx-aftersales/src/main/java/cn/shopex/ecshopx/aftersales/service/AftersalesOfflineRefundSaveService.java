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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesOfflineRefund;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesOfflineRefundMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.support.EmployeePurchasePrepaidRefundLinesResolver;
import cn.shopex.ecshopx.common.dispatch.TradeRefundStatisticsJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AftersalesOfflineRefundSaveService {

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesOfflineRefundMapper aftersalesOfflineRefundMapper;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher;
	private final ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
			employeePurchasePrepaidAftersalesRestorePort;
	private final EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver;
	private final AftersalesRefundOnlineRefundPort aftersalesRefundOnlineRefundPort;

	public AftersalesOfflineRefundSaveService(
			AftersalesRefundMapper aftersalesRefundMapper,
			AftersalesOfflineRefundMapper aftersalesOfflineRefundMapper,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			TradeRefundStatisticsJobDispatchPublisher tradeRefundStatisticsJobDispatchPublisher,
			ObjectProvider<EmployeePurchasePrepaidAftersalesRestorePort>
					employeePurchasePrepaidAftersalesRestorePort,
			EmployeePurchasePrepaidRefundLinesResolver employeePurchasePrepaidRefundLinesResolver,
			AftersalesRefundOnlineRefundPort aftersalesRefundOnlineRefundPort) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.aftersalesOfflineRefundMapper = aftersalesOfflineRefundMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.tradeRefundStatisticsJobDispatchPublisher = tradeRefundStatisticsJobDispatchPublisher;
		this.employeePurchasePrepaidAftersalesRestorePort = employeePurchasePrepaidAftersalesRestorePort;
		this.employeePurchasePrepaidRefundLinesResolver = employeePurchasePrepaidRefundLinesResolver;
		this.aftersalesRefundOnlineRefundPort = aftersalesRefundOnlineRefundPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean saveOfflineRefund(LinkedHashMap<String, Object> merged, long companyId) {
		if (isInvalidRequired(merged.get("refund_bn"))) {
			throw new ResourceException("售后单号必填");
		}
		if (isInvalidRequired(merged.get("bank_account_name"))) {
			throw new ResourceException("收款人户名必填");
		}
		if (isInvalidRequired(merged.get("bank_account_no"))) {
			throw new ResourceException("收款银行帐号必填");
		}
		if (isInvalidRequired(merged.get("bank_name"))) {
			throw new ResourceException("开户银行必填");
		}
		if (isInvalidRequired(merged.get("refund_account_name"))) {
			throw new ResourceException("退款人户名必填");
		}
		if (isInvalidRequired(merged.get("refund_account_bank"))) {
			throw new ResourceException("退款银行帐号必填");
		}
		if (isInvalidRequired(merged.get("refund_account_no"))) {
			throw new ResourceException("退款开户银行必填");
		}

		long refundBnLong;
		try {
			refundBnLong = Long.parseLong(String.valueOf(merged.get("refund_bn")).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("售后单号必填");
		}

		LambdaQueryWrapper<AftersalesRefund> q = new LambdaQueryWrapper<>();
		q.eq(AftersalesRefund::getRefundBn, refundBnLong);
		AftersalesRefund refundInfo = aftersalesRefundMapper.selectOne(q);
		if (refundInfo == null) {
			throw new ResourceException("未查询到退款单");
		}

		AftersalesOfflineRefund entity = new AftersalesOfflineRefund();
		entity.setRefundBn(refundBnLong);
		entity.setOrderId(refundInfo.getOrderId());
		entity.setCompanyId(companyId);
		entity.setRefundFee(refundInfo.getRefundFee());
		entity.setBankAccountName(String.valueOf(merged.get("bank_account_name")).trim());
		entity.setBankAccountNo(String.valueOf(merged.get("bank_account_no")).trim());
		entity.setBankName(String.valueOf(merged.get("bank_name")).trim());
		entity.setRefundAccountName(String.valueOf(merged.get("refund_account_name")).trim());
		entity.setRefundAccountBank(String.valueOf(merged.get("refund_account_bank")).trim());
		entity.setRefundAccountNo(String.valueOf(merged.get("refund_account_no")).trim());
		int now = (int) (System.currentTimeMillis() / 1000);
		entity.setCreateTime(now);
		entity.setUpdateTime(now);

		int inserted = aftersalesOfflineRefundMapper.insert(entity);
		if (inserted < 1) {
			throw new ResourceException("操作失败，请稍后重试");
		}

		String payType =
				refundInfo.getPayType() == null ? "" : refundInfo.getPayType().toLowerCase(Locale.ROOT);
		int refundPoint = refundInfo.getRefundPoint() == null ? 0 : refundInfo.getRefundPoint();
		// 组合支付：走 doRefund（offline 渠道成功 + 退积分 + 落 refunded_point）
		boolean mixedPointRefund = !"point".equals(payType) && refundPoint > 0;
		if (mixedPointRefund) {
			long refundCompanyId =
					refundInfo.getCompanyId() == null ? companyId : refundInfo.getCompanyId();
			aftersalesRefundOnlineRefundPort.executeRefund(refundCompanyId, refundBnLong);
		} else {
			LambdaUpdateWrapper<AftersalesRefund> uw = new LambdaUpdateWrapper<>();
			uw.eq(AftersalesRefund::getRefundBn, refundBnLong)
					.set(AftersalesRefund::getRefundStatus, "SUCCESS");
			int refundRows = aftersalesRefundMapper.update(null, uw);
			if (refundRows != 1) {
				throw new ResourceException("售后退款单不存在");
			}
			publishTradeRefundStatistics(companyId, refundInfo);
			restoreEmployeePurchasePrepaidIfNeeded(companyId, refundInfo);
		}

		if (refundInfo.getAftersalesBn() == null) {
			return true;
		}

		long aftersalesBn = refundInfo.getAftersalesBn();

		LambdaUpdateWrapper<Aftersales> aw = new LambdaUpdateWrapper<>();
		aw.eq(Aftersales::getAftersalesBn, aftersalesBn)
				.eq(Aftersales::getCompanyId, companyId)
				.set(Aftersales::getAftersalesStatus, 2)
				.set(Aftersales::getProgress, 4);
		int mainRows = aftersalesMapper.update(null, aw);
		if (mainRows < 1) {
			throw new ResourceException("售后主表不存在");
		}

		LambdaUpdateWrapper<AftersalesDetail> dw = new LambdaUpdateWrapper<>();
		dw.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
				.eq(AftersalesDetail::getCompanyId, companyId)
				.set(AftersalesDetail::getAftersalesStatus, 2)
				.set(AftersalesDetail::getProgress, 4);
		int detailRows = aftersalesDetailMapper.update(null, dw);
		if (detailRows < 1) {
			throw new ResourceException("未查询到更新数据");
		}

		return true;
	}

	private void restoreEmployeePurchasePrepaidIfNeeded(long companyId, AftersalesRefund refundInfo) {
		String payType =
				refundInfo.getPayType() == null ? "" : refundInfo.getPayType().toLowerCase(Locale.ROOT);
		if (!"prepaid_point".equals(payType)) {
			return;
		}
		EmployeePurchasePrepaidAftersalesRestorePort port =
				employeePurchasePrepaidAftersalesRestorePort.getIfAvailable();
		if (port == null) {
			return;
		}
		String freightType = refundInfo.getFreightType() == null ? "cash" : refundInfo.getFreightType();
		int baseRefundFee = refundInfo.getRefundFee() == null ? 0 : refundInfo.getRefundFee();
		int baseRefundPoint = refundInfo.getRefundPoint() == null ? 0 : refundInfo.getRefundPoint();
		int freight = refundInfo.getFreight() == null ? 0 : refundInfo.getFreight();
		int effectiveRefundFee = baseRefundFee;
		int effectiveRefundPoint = baseRefundPoint;
		if ("cash".equals(freightType)) {
			effectiveRefundFee += freight;
		} else if ("point".equals(freightType)) {
			effectiveRefundPoint += freight;
		}
		int requested = Math.max(0, effectiveRefundFee) + Math.max(0, effectiveRefundPoint);
		port.restoreOnRefundSuccess(
				companyId,
				refundInfo.getOrderId(),
				refundInfo.getRefundBn(),
				requested,
				employeePurchasePrepaidRefundLinesResolver.resolve(companyId, refundInfo));
	}

	private void publishTradeRefundStatistics(long companyId, AftersalesRefund refundInfo) {
		Map<String, Object> statsPayload = new LinkedHashMap<>();
		statsPayload.put("company_id", companyId);
		statsPayload.put("order_id", refundInfo.getOrderId());
		statsPayload.put("refund_bn", refundInfo.getRefundBn());
		statsPayload.put("trade_id", refundInfo.getTradeId());
		statsPayload.put("pay_type", refundInfo.getPayType());
		statsPayload.put("refund_fee", refundInfo.getRefundFee() == null ? 0 : refundInfo.getRefundFee());
		if (refundInfo.getDistributorId() != null && refundInfo.getDistributorId() > 0L) {
			statsPayload.put("distributor_id", refundInfo.getDistributorId());
		}
		if (refundInfo.getMerchantId() != null && refundInfo.getMerchantId() > 0L) {
			statsPayload.put("merchant_id", refundInfo.getMerchantId());
		}
		tradeRefundStatisticsJobDispatchPublisher.publish(statsPayload);
	}

	private static boolean isInvalidRequired(Object o) {
		return o == null || String.valueOf(o).trim().isEmpty();
	}
}
