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
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AftersalesRefundConfirmService {

	private static final Logger log = LoggerFactory.getLogger(AftersalesRefundConfirmService.class);

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final AftersalesRefundAsyncPort aftersalesRefundAsyncPort;
	private final JushuitanSettingReadPort jushuitanSettingReadPort;
	private final AftersalesBrokeragePort aftersalesBrokeragePort;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	private final WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;
	private final ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
	private final ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

	public AftersalesRefundConfirmService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundService aftersalesRefundService,
			AftersalesRefundAsyncPort aftersalesRefundAsyncPort,
			JushuitanSettingReadPort jushuitanSettingReadPort,
			AftersalesBrokeragePort aftersalesBrokeragePort,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort,
			ApplicationEventPublisher applicationEventPublisher,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher,
			WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder,
			ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
			ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.aftersalesRefundAsyncPort = aftersalesRefundAsyncPort;
		this.jushuitanSettingReadPort = jushuitanSettingReadPort;
		this.aftersalesBrokeragePort = aftersalesBrokeragePort;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrderLeftAftersalesWritePort = normalOrderLeftAftersalesWritePort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.wdtErpTradeAfterSaleDispatchPublisher = wdtErpTradeAfterSaleDispatchPublisher;
		this.wdtErpTradeAfterSaleBusPayloadBuilder = wdtErpTradeAfterSaleBusPayloadBuilder;
		this.thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher = thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
		this.thirdPartyTradeAftersalesSaasErpDispatchPublisher = thirdPartyTradeAftersalesSaasErpDispatchPublisher;
	}

	/**
	 * Merged query and body parameters for refund checks. Reject decisions accept the refusal note as either
	 * {@code refunds_memo} or {@code refund_memo}; values are normalized before {@link #confirmRefund(Map)} runs.
	 */
	public Map<String, Object> refundCheck(LinkedHashMap<String, Object> merged, HttpServletRequest request) {
		mergeJwt(request, merged);
		validateRefundCheckParams(merged);
		long companyId = longVal(merged.get("company_id"));
		Object rawBn = merged.get("aftersales_bn");
		if (isBatchForm(rawBn)) {
			List<Long> bns = parseBnList(rawBn);
			List<Object> resultList = new ArrayList<>();
			for (long bn : bns) {
				LinkedHashMap<String, Object> per = new LinkedHashMap<>(merged);
				per.put("aftersales_bn", bn);
				fillRefundFeePointFromDbIfEmpty(companyId, bn, per);
				per.put("operator_type", "admin");
				per.put("operator_id", merged.get("operator_id"));
				resultList.add(confirmRefund(per));
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("status", true);
			out.put("result", resultList);
			return out;
		}
		long singleBn = longVal(rawBn);
		fillRefundFeePointFromDbIfEmpty(companyId, singleBn, merged);
		merged.put("operator_type", "admin");
		Map<String, Object> single = confirmRefund(merged);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", true);
		out.put("result", single);
		return out;
	}

	private void mergeJwt(HttpServletRequest request, LinkedHashMap<String, Object> merged) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> jwt)) {
			throw new UnauthorizedException("未登录");
		}
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("企业id必填");
		}
		merged.put("company_id", longVal(companyIdObj));
		merged.put("operator_type", str(jwt.get("operator_type")));
		merged.put("operator_id", longVal(jwt.get("operator_id")));
	}

	private void validateRefundCheckParams(LinkedHashMap<String, Object> merged) {
		List<String> errors = new ArrayList<>();
		if (!merged.containsKey("aftersales_bn") || merged.get("aftersales_bn") == null) {
			errors.add("售后单号不能为空");
		} else {
			Object raw = merged.get("aftersales_bn");
			if (isBatchForm(raw)) {
				if (parseBnList(raw).isEmpty()) {
					errors.add("售后单号不能为空");
				}
			} else if (isLooseEmpty(raw)) {
				errors.add("售后单号不能为空");
			}
		}
		if (!merged.containsKey("check_refund") || merged.get("check_refund") == null) {
			errors.add("审核退款不能为空");
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException(String.join("、", errors));
		}
		if (isRejectCheckRefund(merged.get("check_refund"))) {
			boolean hasRefundsMemo =
					merged.containsKey("refunds_memo") && !isLooseEmpty(merged.get("refunds_memo"));
			boolean hasRefundMemo =
					merged.containsKey("refund_memo") && !isLooseEmpty(merged.get("refund_memo"));
			if (!hasRefundsMemo && !hasRefundMemo) {
				throw new BadRequestException("拒绝退款原因不能为空");
			}
		}
	}

	private void fillRefundFeePointFromDbIfEmpty(long companyId, long aftersalesBn, LinkedHashMap<String, Object> merged) {
		boolean missingOrEmptyFee = !merged.containsKey("refund_fee") || isLooseEmpty(merged.get("refund_fee"));
		boolean missingOrEmptyPoint = !merged.containsKey("refund_point") || isLooseEmpty(merged.get("refund_point"));
		if (!missingOrEmptyFee && !missingOrEmptyPoint) {
			return;
		}
		Aftersales row =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (row == null) {
			throw new ResourceException("需要退款的售后单不存在");
		}
		if (missingOrEmptyFee) {
			merged.put("refund_fee", row.getRefundFee());
		}
		if (missingOrEmptyPoint) {
			merged.put("refund_point", row.getRefundPoint());
		}
	}

	public Map<String, Object> confirmRefund(Map<String, Object> param) {
		long companyId = longVal(param.get("company_id"));
		long aftersalesBn = longVal(param.get("aftersales_bn"));
		String bnLabel = String.valueOf(param.get("aftersales_bn"));
		Aftersales aftersales =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (aftersales == null) {
			throw new ResourceException("需要退款的售后单不存在");
		}
		Integer st = aftersales.getAftersalesStatus();
		if (st != null && (st == 2 || st == 3)) {
			throw new ResourceException("售后单已处理");
		}
		if (st == null || st != 1) {
			throw new ResourceException("售后" + bnLabel + "不是审核中，无需审核");
		}
		if (param.containsKey("refund_fee") && !isLooseEmpty(param.get("refund_fee"))) {
			int requested = intVal(param.get("refund_fee"));
			int cap = aftersales.getRefundFee() == null ? 0 : aftersales.getRefundFee();
			if (requested > cap) {
				throw new ResourceException("实退金额必须小于等于应退金额");
			}
		}
		if (param.containsKey("refund_point") && !isLooseEmpty(param.get("refund_point"))) {
			int requested = intVal(param.get("refund_point"));
			int cap = aftersales.getRefundPoint() == null ? 0 : aftersales.getRefundPoint();
			if (requested > cap) {
				throw new ResourceException("实退积分必须小于等于应退积分");
			}
		}
		AftersalesRefund refund = aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn);
		if (refund == null) {
			throw new ResourceException("售后单不存在退款单");
		}
		if ("SUCCESS".equals(refund.getRefundStatus())) {
			throw new ResourceException("退款单已退款成功，无需重复操作");
		}
		boolean reject = isRejectCheckRefund(param.get("check_refund"));
		if (!reject
				&& "REFUND_GOODS".equals(aftersales.getAftersalesType())) {
			Map<String, Object> jt = jushuitanSettingReadPort.readJushuitanSetting(companyId);
			if (Boolean.TRUE.equals(jt.get("is_open"))
					&& aftersales.getProgress() != null
					&& aftersales.getProgress() != 8) {
				throw new ResourceException("卖家暂未收货，请先到聚水潭进行处理");
			}
		}
		final long orderId = aftersales.getOrderId() == null ? 0L : aftersales.getOrderId();
		final boolean rejectFinal = reject;
		transactionTemplate.execute(
				status -> {
					runConfirmRefundTx(
							param,
							companyId,
							aftersalesBn,
							bnLabel,
							aftersales,
							refund,
							rejectFinal,
							orderId);
					return null;
				});
		return reloadAftersalesRowMap(companyId, aftersalesBn);
	}

	private void runConfirmRefundTx(
			Map<String, Object> param,
			long companyId,
			long aftersalesBn,
			String bnLabel,
			Aftersales aftersales,
			AftersalesRefund refund,
			boolean reject,
			long orderId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> refundFields = new LinkedHashMap<>();
		if (reject) {
			refundFields.put("refund_status", "REFUSE");
		} else {
			refundFields.put("refund_status", "AUDIT_SUCCESS");
			String channel = "offline_pay".equals(refund.getPayType()) ? "offline" : "original";
			refundFields.put("refund_channel", channel);
			if (param.containsKey("refund_fee")) {
				refundFields.put("refund_fee", intVal(param.get("refund_fee")));
			}
			if (param.containsKey("refund_point")) {
				refundFields.put("refund_point", intVal(param.get("refund_point")));
			}
			if (param.containsKey("freight")) {
				int freight = intVal(param.get("freight"));
				refundFields.put("freight", freight);
				refundFields.put("return_freight", freight != 0 ? 1 : 0);
			}
		}
		if (param.containsKey("refunds_memo") && !isLooseEmpty(param.get("refunds_memo"))) {
			refundFields.put("refunds_memo", str(param.get("refunds_memo")));
		} else if (reject
				&& param.containsKey("refund_memo")
				&& !isLooseEmpty(param.get("refund_memo"))) {
			refundFields.put("refunds_memo", str(param.get("refund_memo")));
		}
		String refundRowMemo = resolveRefundRowMemo(param);
		refundFields.put("update_time", now);
		int ru = aftersalesRefundService.updateRefundByAftersalesKeys(companyId, aftersalesBn, refundFields);
		if (ru == 0) {
			throw new ResourceException("需要退款的售后单不存在");
		}
		if (reject) {
			aftersales.setAftersalesStatus(3);
			aftersales.setProgress(3);
			aftersales.setRefuseReason(refundRowMemo);
		} else {
			aftersales.setAftersalesStatus(2);
			aftersales.setProgress(4);
		}
		aftersales.setUpdateTime(now);
		int mu = aftersalesMapper.updateById(aftersales);
		if (mu == 0) {
			throw new ResourceException("需要退款的售后单不存在");
		}
		LambdaUpdateWrapper<AftersalesDetail> duw =
				new LambdaUpdateWrapper<AftersalesDetail>()
						.eq(AftersalesDetail::getCompanyId, companyId)
						.eq(AftersalesDetail::getAftersalesBn, aftersalesBn);
		if (reject) {
			duw.set(AftersalesDetail::getAftersalesStatus, 3).set(AftersalesDetail::getProgress, 3);
		} else {
			duw.set(AftersalesDetail::getAftersalesStatus, 2).set(AftersalesDetail::getProgress, 4);
		}
		duw.set(AftersalesDetail::getUpdateTime, now);
		List<AftersalesDetail> detailRows =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
		if (!detailRows.isEmpty()) {
			int du = aftersalesDetailMapper.update(null, duw);
			if (du == 0) {
				throw new ResourceException("需要退款的售后单不存在");
			}
		}
		if (reject) {
			Map<String, Object> logMap = new LinkedHashMap<>();
			logMap.put("order_id", orderId);
			logMap.put("company_id", companyId);
			logMap.put("operator_type", "user");
			long opUser = param.containsKey("user_id") ? longVal(param.get("user_id")) : 0L;
			logMap.put("operator_id", opUser);
			logMap.put("remarks", "订单售后");
			logMap.put(
					"detail",
					refundRejectOrderProcessLogDetail(bnLabel, refundRowMemo));
			logMap.put("params", new LinkedHashMap<>(param));
			orderProcessLogPublishPort.publish(logMap);
			int sum = 0;
			for (AftersalesDetail d : detailRows) {
				sum += d.getNum() == null ? 0 : d.getNum();
			}
			if (sum > 0 && orderId > 0L) {
				normalOrderLeftAftersalesWritePort.addLeftAftersalesNum(companyId, orderId, sum);
			}
		} else {
			Map<String, Object> logMap = new LinkedHashMap<>();
			logMap.put("order_id", orderId);
			logMap.put("company_id", companyId);
			logMap.put("operator_type", "admin");
			long opId =
					aftersales.getUserId() != null && aftersales.getUserId() > 0L
							? aftersales.getUserId()
							: longVal(param.get("operator_id"));
			logMap.put("operator_id", opId);
			logMap.put("remarks", "订单售后");
			logMap.put("detail", refundAgreeOrderProcessLogDetail(bnLabel));
			logMap.put("params", new LinkedHashMap<>(param));
			orderProcessLogPublishPort.publish(logMap);
			for (AftersalesDetail d : detailRows) {
				Map<String, Object> bp = new LinkedHashMap<>();
				bp.put("company_id", companyId);
				bp.put("order_id", orderId);
				bp.put("item_id", d.getItemId() == null ? 0L : d.getItemId());
				bp.put("num", d.getNum() == null ? 0 : d.getNum());
				aftersalesBrokeragePort.brokerageByAftersales(bp);
			}
		}
		Map<String, Object> erpPayload = new LinkedHashMap<>(aftersalesToSnakeMap(aftersales));
		erpPayload.put("aftersales_action", reject ? "cancel" : "update");
		if (reject) {
			thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.publish(erpPayload);
		} else {
			thirdPartyTradeAftersalesSaasErpDispatchPublisher.publish(erpPayload);
		}
		sendWxaTemplateMsg(
				aftersales.getAftersalesStatus() == null ? 0 : aftersales.getAftersalesStatus(),
				erpPayload);
		Map<String, Object> postRow = new LinkedHashMap<>(aftersalesToSnakeMap(aftersales));
		registerPostCommitCallbacks(companyId, orderId, reject, postRow);
	}

	/**
	 * Registers {@link TransactionSynchronization#afterCommit} callbacks so outbound work (including
	 * {@link AftersalesRefundAsyncPort#scheduleOrderRefundComplete} and the invoice-red snapshot passed to
	 * {@link AftersalesRefundAsyncPort#scheduleInvoiceRed}) runs only after the refund decision transaction
	 * commits, matching the intended asynchronous job dispatch ordering.
	 */
	private void registerPostCommitCallbacks(
			long companyId, long orderId, boolean reject, Map<String, Object> resultRow) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			final boolean r = reject;
			final long cid = companyId;
			final long oid = orderId;
			final Map<String, Object> row = new LinkedHashMap<>(resultRow);
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							if (r) {
								long reloadCompanyId = longVal(row.get("company_id"));
								long reloadBn = longVal(row.get("aftersales_bn"));
								Aftersales reloaded =
										aftersalesMapper.selectOne(
												new LambdaQueryWrapper<Aftersales>()
														.eq(Aftersales::getCompanyId, reloadCompanyId)
														.eq(Aftersales::getAftersalesBn, reloadBn));
								if (reloaded != null) {
									jushuitanTradeAftersalesDispatchPublisher.publish(
											jushuitanTradeAftersalesBusPayloadBuilder.build(reloaded, new LinkedHashMap<>()));
									applicationEventPublisher.publishEvent(
											new JushuitanTradeAftersalesSyncSpringEvent(
													AftersalesRefundConfirmService.this, aftersalesToSnakeMap(reloaded)));
								}
							} else {
								aftersalesRefundAsyncPort.scheduleOrderRefundComplete(cid, oid);
								long reloadCompanyIdApprove = longVal(row.get("company_id"));
								long reloadBnApprove = longVal(row.get("aftersales_bn"));
								Aftersales reloadedApprove =
										aftersalesMapper.selectOne(
												new LambdaQueryWrapper<Aftersales>()
														.eq(Aftersales::getCompanyId, reloadCompanyIdApprove)
														.eq(Aftersales::getAftersalesBn, reloadBnApprove));
								if (reloadedApprove != null) {
									jushuitanTradeAftersalesDispatchPublisher.publish(
											jushuitanTradeAftersalesBusPayloadBuilder.build(
													reloadedApprove, new LinkedHashMap<>()));
									applicationEventPublisher.publishEvent(
											new JushuitanTradeAftersalesSyncSpringEvent(
													AftersalesRefundConfirmService.this,
													aftersalesToSnakeMap(reloadedApprove)));
									wdtErpTradeAfterSaleDispatchPublisher.publish(
											wdtErpTradeAfterSaleBusPayloadBuilder.build(reloadedApprove));
									aftersalesRefundAsyncPort.scheduleInvoiceRed(
											aftersalesToSnakeMap(reloadedApprove));
								}
							}
						}
					});
		}
	}

	private Map<String, Object> reloadAftersalesRowMap(long companyId, long aftersalesBn) {
		Aftersales row =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (row == null) {
			throw new ResourceException("需要退款的售后单不存在");
		}
		return aftersalesToSnakeMap(row);
	}

	private Map<String, Object> aftersalesToSnakeMap(Aftersales a) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", a.getAftersalesBn());
		m.put("order_id", a.getOrderId());
		m.put("company_id", a.getCompanyId());
		m.put("user_id", a.getUserId());
		m.put("salesman_id", a.getSalesmanId());
		m.put("item_bn", a.getItemBn());
		m.put("shop_id", a.getShopId());
		m.put("distributor_id", a.getDistributorId());
		m.put("supplier_id", a.getSupplierId());
		m.put("aftersales_type", a.getAftersalesType());
		m.put("aftersales_status", a.getAftersalesStatus());
		m.put("progress", a.getProgress());
		m.put("refund_fee", a.getRefundFee());
		m.put("refund_point", a.getRefundPoint());
		m.put("reason", a.getReason());
		m.put("description", a.getDescription());
		m.put("evidence_pic", a.getEvidencePic());
		m.put("refuse_reason", a.getRefuseReason());
		m.put("memo", a.getMemo());
		m.put("sendback_data", a.getSendbackData());
		m.put("sendconfirm_data", a.getSendconfirmData());
		m.put("third_data", a.getThirdData());
		m.put("aftersales_address", a.getAftersalesAddress());
		m.put("distributor_remark", a.getDistributorRemark());
		m.put("create_time", a.getCreateTime());
		m.put("update_time", a.getUpdateTime());
		m.put("contact", a.getContact());
		m.put("mobile", a.getMobile());
		m.put("merchant_id", a.getMerchantId());
		m.put("is_partial_cancel", a.getIsPartialCancel());
		m.put("return_type", a.getReturnType());
		m.put("return_distributor_id", a.getReturnDistributorId());
		m.put("self_delivery_operator_id", a.getSelfDeliveryOperatorId());
		m.put("freight", a.getFreight());
		m.put("freight_type", a.getFreightType());
		return m;
	}

	private void sendWxaTemplateMsg(int newAftersalesStatus, Map<String, Object> templateData) {
		log.info(
				"wxa template (placeholder) aftersales_bn={} order_id={} status={} refuse_reason={} refund_amount={}",
				templateData.get("aftersales_bn"),
				templateData.get("order_id"),
				newAftersalesStatus,
				templateData.get("refuse_reason"),
				templateData.get("refund_fee"));
	}

	/**
	 * Refund memo from request: prefers {@code refunds_memo}, then non-empty {@code refund_memo}
	 * (alternate key used by some admin clients).
	 */
	private static String resolveRefundRowMemo(Map<String, Object> param) {
		if (param.containsKey("refunds_memo") && !isLooseEmpty(param.get("refunds_memo"))) {
			return str(param.get("refunds_memo"));
		}
		if (param.containsKey("refund_memo") && !isLooseEmpty(param.get("refund_memo"))) {
			return str(param.get("refund_memo"));
		}
		return "";
	}

	private static String refundRejectOrderProcessLogDetail(String bnLabel, String refundsMemo) {
		return "售后单号：" + bnLabel + " 拒绝退款，拒绝退款原因：" + refundsMemo;
	}

	private static String refundAgreeOrderProcessLogDetail(String bnLabel) {
		return "售后单号：" + bnLabel + "，售后单同意退款";
	}

	private static boolean isRejectCheckRefund(Object checkRefundRaw) {
		if (checkRefundRaw == null) {
			return true;
		}
		if (checkRefundRaw instanceof Boolean b) {
			return !b;
		}
		if (checkRefundRaw instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (checkRefundRaw instanceof Character c) {
			return c == '0' || c == 0;
		}
		String s = String.valueOf(checkRefundRaw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return true;
		}
		return false;
	}

	private static boolean isBatchForm(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Collection<?> || raw instanceof Object[]) {
			return true;
		}
		if (raw instanceof long[] || raw instanceof int[] || raw instanceof Integer[]) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.startsWith("[");
		}
		return false;
	}

	private List<Long> parseBnList(Object raw) {
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> c) {
			for (Object o : c) {
				out.add(longVal(o));
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			for (Object o : arr) {
				out.add(longVal(o));
			}
			return out;
		}
		if (raw instanceof long[] arr) {
			for (long v : arr) {
				out.add(v);
			}
			return out;
		}
		if (raw instanceof int[] arr) {
			for (int v : arr) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof String s) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node != null && node.isArray()) {
					for (JsonNode el : node) {
						out.add(longVal(el.asText()));
					}
				}
			} catch (Exception ignored) {
				// ignore
			}
		}
		return out;
	}

	private static boolean isLooseEmpty(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			if (o instanceof Double d) {
				return d == 0.0;
			}
			if (o instanceof Float f) {
				return f == 0.0f;
			}
			return n.longValue() == 0L;
		}
		if (o instanceof CharSequence s) {
			String t = s.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (o instanceof Character c) {
			return c == '0' || c == 0;
		}
		if (o instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (o instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (o instanceof Object[] arr) {
			return arr.length == 0;
		}
		return false;
	}

	private static long longVal(Object o) {
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

	private static int intVal(Object o) {
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
