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
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.NormalOrderPartialCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AftersalesReviewService {

	private static final String ORDER_PROCESS_LOG_DETAIL_APPROVED_WAIT_RETURN_GOODS =
			"，售后单审核通过，等待商品回寄";

	private static final Logger log = LoggerFactory.getLogger(AftersalesReviewService.class);

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final AftersalesRefundAsyncPort aftersalesRefundAsyncPort;
	private final AftersalesBrokeragePort aftersalesBrokeragePort;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort;
	private final NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort;
	private final OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort;
	private final DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final LangueProperties langueProperties;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	private final WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;
	private final ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher
			thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
	private final ThirdPartyTradeAftersalesSaasErpDispatchPublisher
			thirdPartyTradeAftersalesSaasErpDispatchPublisher;
	private final TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher;

	public AftersalesReviewService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundService aftersalesRefundService,
			AftersalesRefundAsyncPort aftersalesRefundAsyncPort,
			AftersalesBrokeragePort aftersalesBrokeragePort,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort,
			NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort,
			OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort,
			DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort,
			ApplicationEventPublisher applicationEventPublisher,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager,
			LangueProperties langueProperties,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher,
			WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder,
			ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher
					thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
			ThirdPartyTradeAftersalesSaasErpDispatchPublisher
					thirdPartyTradeAftersalesSaasErpDispatchPublisher,
			TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.aftersalesRefundAsyncPort = aftersalesRefundAsyncPort;
		this.aftersalesBrokeragePort = aftersalesBrokeragePort;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrderLeftAftersalesWritePort = normalOrderLeftAftersalesWritePort;
		this.normalOrderPartialCancelRestorePort = normalOrderPartialCancelRestorePort;
		this.orderValidityPlatformSettingReadPort = orderValidityPlatformSettingReadPort;
		this.distributorAftersalesAddressDetailReadPort = distributorAftersalesAddressDetailReadPort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.langueProperties = langueProperties;
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.wdtErpTradeAfterSaleDispatchPublisher = wdtErpTradeAfterSaleDispatchPublisher;
		this.wdtErpTradeAfterSaleBusPayloadBuilder = wdtErpTradeAfterSaleBusPayloadBuilder;
		this.thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
		this.thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				thirdPartyTradeAftersalesSaasErpDispatchPublisher;
		this.tradeAftersalesDispatchPublisher = tradeAftersalesDispatchPublisher;
	}

	public Map<String, Object> aftersalesReview(LinkedHashMap<String, Object> merged, HttpServletRequest request) {
		mergeJwt(request, merged);
		normalizeActionDefaults(merged);
		merged.put("operator_type", "admin");
		validateReviewParams(merged);
		Object rawBn = merged.get("aftersales_bn");
		if (isBatchForm(rawBn)) {
			List<Long> bns = parseBnList(rawBn);
			List<Object> resultList = new ArrayList<>();
			for (long bn : bns) {
				LinkedHashMap<String, Object> per = new LinkedHashMap<>(merged);
				per.put("aftersales_bn", bn);
				resultList.add(review(per, request));
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("status", true);
			out.put("result", resultList);
			return out;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", true);
		out.put("result", review(merged, request));
		return out;
	}

	public Object review(Map<String, Object> param, HttpServletRequest request) {
		long companyId = longVal(param.get("company_id"));
		long aftersalesBn = longVal(param.get("aftersales_bn"));
		String bnLabel = String.valueOf(param.get("aftersales_bn"));
		Aftersales row;
		try {
			row =
					aftersalesMapper.selectOne(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.eq(Aftersales::getAftersalesBn, aftersalesBn));
		} catch (TooManyResultsException e) {
			throw new ResourceException("售后单数据异常");
		}
		if (row == null) {
			throw new ResourceException("售后单数据异常");
		}
		Integer st = row.getAftersalesStatus();
		if (st == null || st != 0) {
			throw new ResourceException("售后" + bnLabel + "已处理，无需审核");
		}
		Map<String, Object> returnSnapshot = aftersalesToSnakeMap(row);
		boolean approved = isApprovedApproved(param.get("is_approved"));
		final long orderId = row.getOrderId() == null ? 0L : row.getOrderId();
		final boolean approvedFinal = approved;
		transactionTemplate.execute(
				status -> {
					runReviewTx(param, companyId, aftersalesBn, bnLabel, row, approvedFinal, orderId, request);
					return null;
				});
		sendWxaTemplateMsg(
				row.getAftersalesStatus() == null ? 0 : row.getAftersalesStatus(),
				buildTemplateData(param, row));
		return returnSnapshot;
	}

	private void runReviewTx(
			Map<String, Object> param,
			long companyId,
			long aftersalesBn,
			String bnLabel,
			Aftersales a,
			boolean approved,
			long orderId,
			HttpServletRequest request) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (!approved) {
			Map<String, Object> rf = new LinkedHashMap<>();
			rf.put("refund_status", "REFUSE");
			rf.put("update_time", now);
			int ru = aftersalesRefundService.updateRefundByAftersalesKeys(companyId, aftersalesBn, rf);
			if (ru == 0) {
				throw new ResourceException("售后单数据异常");
			}
			if (Boolean.TRUE.equals(a.getIsPartialCancel())) {
				normalOrderPartialCancelRestorePort.partialCancelRestore(companyId, orderId, false);
			} else {
				List<AftersalesDetail> details =
						aftersalesDetailMapper.selectList(
								new LambdaQueryWrapper<AftersalesDetail>()
										.eq(AftersalesDetail::getCompanyId, companyId)
										.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
				int sum = 0;
				for (AftersalesDetail d : details) {
					sum += d.getNum() == null ? 0 : d.getNum();
				}
				if (sum > 0 && orderId > 0L) {
					normalOrderLeftAftersalesWritePort.addLeftAftersalesNum(companyId, orderId, sum);
				}
			}
			a.setProgress(3);
			a.setAftersalesStatus(3);
			a.setRefuseReason(str(param.get("refuse_reason")));
			a.setUpdateTime(now);
			Map<String, Object> logMap = new LinkedHashMap<>();
			logMap.put("order_id", orderId);
			logMap.put("company_id", companyId);
			logMap.put("operator_type", str(param.get("operator_type")));
			logMap.put("operator_id", longVal(param.get("operator_id")));
			logMap.put("remarks", "订单售后");
			logMap.put("detail", "售后单号：" + bnLabel + " 售后单驳回，驳回原因：" + str(param.get("refuse_reason")));
			logMap.put("params", new LinkedHashMap<>(param));
			orderProcessLogPublishPort.publish(logMap);
			wdtErpTradeAfterSaleDispatchPublisher.publish(wdtErpTradeAfterSaleBusPayloadBuilder.build(a));
			persistMainAndDetailsAndErp(
					companyId, aftersalesBn, a, approved, now, param, null);
			tradeAftersalesDispatchPublisher.publish(new LinkedHashMap<>(aftersalesToSnakeMap(a)));
			registerRejectJushuitanAfterCommit(companyId, aftersalesBn);
			return;
		}
		applyRefundFeePointFreightFromParam(param, a);
		String type = a.getAftersalesType();
		if ("ONLY_REFUND".equals(type)) {
			int refundFee = intVal(param.get("refund_fee"));
			int refundPoint = intVal(param.get("refund_point"));
			int freight = intVal(param.get("freight"));
			if (refundFee < 0 || freight < 0) {
				throw new ResourceException("请填写退款金额并且大于等于0！");
			}
			int capFee = a.getRefundFee() == null ? 0 : a.getRefundFee();
			int capPoint = a.getRefundPoint() == null ? 0 : a.getRefundPoint();
			int capFreight = a.getFreight() == null ? 0 : a.getFreight();
			if (refundFee > capFee) {
				throw new ResourceException("退款金额不能大于应退金额！");
			}
			if (refundPoint > capPoint) {
				throw new ResourceException("退款积分不能大于应退积分！");
			}
			if (freight > capFreight) {
				throw new ResourceException("退款运费不能大于应退运费！");
			}
			AftersalesRefund refundRow = aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn);
			if (refundRow == null) {
				throw new ResourceException("售后单数据异常");
			}
			String channel = "offline_pay".equals(refundRow.getPayType()) ? "offline" : "original";
			Map<String, Object> rf = new LinkedHashMap<>();
			rf.put("refund_status", "AUDIT_SUCCESS");
			rf.put("refund_channel", channel);
			rf.put("refund_fee", refundFee);
			rf.put("freight", freight);
			rf.put("return_freight", freight != 0 ? 1 : 0);
			rf.put("refund_point", refundPoint);
			rf.put("update_time", now);
			int ru = aftersalesRefundService.updateRefundByAftersalesKeys(companyId, aftersalesBn, rf);
			if (ru == 0) {
				throw new ResourceException("售后单数据异常");
			}
			a.setProgress(9);
			a.setAftersalesStatus(1);
			a.setUpdateTime(now);
			Map<String, Object> logMap = new LinkedHashMap<>();
			logMap.put("order_id", orderId);
			logMap.put("company_id", companyId);
			logMap.put("operator_type", str(param.get("operator_type")));
			logMap.put("operator_id", longVal(param.get("operator_id")));
			logMap.put("remarks", "订单售后");
			logMap.put("detail", "售后单号：" + bnLabel + "，同意退款");
			logMap.put("params", new LinkedHashMap<>(param));
			orderProcessLogPublishPort.publish(logMap);
			List<AftersalesDetail> detailRows =
					aftersalesDetailMapper.selectList(
							new LambdaQueryWrapper<AftersalesDetail>()
									.eq(AftersalesDetail::getCompanyId, companyId)
									.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
			for (AftersalesDetail d : detailRows) {
				Map<String, Object> bp = new LinkedHashMap<>();
				bp.put("company_id", companyId);
				bp.put("order_id", orderId);
				bp.put("item_id", d.getItemId() == null ? 0L : d.getItemId());
				bp.put("num", d.getNum() == null ? 0 : d.getNum());
				aftersalesBrokeragePort.brokerageByAftersales(bp);
			}
			if (Boolean.TRUE.equals(a.getIsPartialCancel())) {
				normalOrderPartialCancelRestorePort.partialCancelRestore(companyId, orderId, true);
			}
			persistMainAndDetailsAndErp(companyId, aftersalesBn, a, approved, now, param, null);
			registerOnlyRefundApprovePostCommit(companyId, orderId, aftersalesToSnakeMap(a));
			return;
		}
		a.setProgress(1);
		a.setAftersalesStatus(1);
		if ("offline".equalsIgnoreCase(str(a.getReturnType()))) {
			a.setProgress(2);
		}
		Map<String, Object> platform = orderValidityPlatformSettingReadPort.readPlatformSetting(companyId);
		int days = intValFlexible(platform.get("auto_refuse_time"), 0);
		long epochSeconds;
		if (days > 0) {
			epochSeconds = Instant.now().plus(days, ChronoUnit.DAYS).getEpochSecond();
		} else {
			epochSeconds = Instant.now().getEpochSecond();
		}
		int autoRefuseEpoch = (int) epochSeconds;
		a.setUpdateTime(now);
		Object addrIdRaw = param.get("aftersales_address_id");
		if (addrIdRaw != null && !isLooseEmpty(addrIdRaw)) {
			long addressId = longVal(addrIdRaw);
			String lang = RequestLangTag.current(langueProperties);
			Map<String, Object> addr =
					distributorAftersalesAddressDetailReadPort.getDetail(companyId, addressId, lang);
			Map<String, Object> addrPayload = new LinkedHashMap<>();
			addrPayload.put("aftersales_address_id", addressId);
			addrPayload.put("aftersales_contact", str(addr.get("contact")));
			addrPayload.put("aftersales_mobile", str(addr.get("mobile")));
			String full =
					str(addr.get("province"))
							+ str(addr.get("city"))
							+ str(addr.get("area"))
							+ str(addr.get("address"));
			addrPayload.put("aftersales_address", full);
			try {
				a.setAftersalesAddress(objectMapper.writeValueAsString(addrPayload));
			} catch (JsonProcessingException e) {
				throw new ResourceException("售后单数据异常");
			}
		}
		Map<String, Object> jobPayload = new LinkedHashMap<>();
		jobPayload.put("company_id", companyId);
		jobPayload.put("order_id", orderId);
		jobPayload.put("aftersales_bn", aftersalesBn);
		aftersalesRefundAsyncPort.scheduleAftersalesSuccessSendMsg(jobPayload);
		Map<String, Object> logMap = new LinkedHashMap<>();
		logMap.put("order_id", orderId);
		logMap.put("company_id", companyId);
		logMap.put("operator_type", str(param.get("operator_type")));
		logMap.put("operator_id", longVal(param.get("operator_id")));
		logMap.put("remarks", "订单售后");
		logMap.put("detail", "售后单号：" + bnLabel + ORDER_PROCESS_LOG_DETAIL_APPROVED_WAIT_RETURN_GOODS);
		logMap.put("params", new LinkedHashMap<>(param));
		orderProcessLogPublishPort.publish(logMap);
		persistMainAndDetailsAndErp(companyId, aftersalesBn, a, approved, now, param, autoRefuseEpoch);
	}

	private void persistMainAndDetailsAndErp(
			long companyId,
			long aftersalesBn,
			Aftersales a,
			boolean approved,
			int now,
			Map<String, Object> param,
			Integer detailAutoRefuseEpoch) {
		int mu = aftersalesMapper.updateById(a);
		if (mu == 0) {
			throw new ResourceException("售后单更新失败");
		}
		LambdaUpdateWrapper<AftersalesDetail> duw =
				new LambdaUpdateWrapper<AftersalesDetail>()
						.eq(AftersalesDetail::getCompanyId, companyId)
						.eq(AftersalesDetail::getAftersalesBn, aftersalesBn);
		duw.set(AftersalesDetail::getProgress, a.getProgress())
				.set(AftersalesDetail::getAftersalesStatus, a.getAftersalesStatus())
				.set(AftersalesDetail::getUpdateTime, now);
		if (detailAutoRefuseEpoch != null) {
			duw.set(AftersalesDetail::getAutoRefuseTime, String.valueOf(detailAutoRefuseEpoch));
		}
		long detailCount =
				aftersalesDetailMapper.selectCount(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
		if (detailCount > 0L) {
			int du = aftersalesDetailMapper.update(null, duw);
			if (du == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}
		Map<String, Object> erpPayload = new LinkedHashMap<>(aftersalesToSnakeMap(a));
		erpPayload.put("aftersales_action", approved ? "update" : "cancel");
		if (approved) {
			thirdPartyTradeAftersalesSaasErpDispatchPublisher.publish(erpPayload);
		} else {
			thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.publish(erpPayload);
		}
	}

	private void registerOnlyRefundApprovePostCommit(long companyId, long orderId, Map<String, Object> postRow) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		final long cid = companyId;
		final long oid = orderId;
		final Map<String, Object> row = new LinkedHashMap<>(postRow);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						aftersalesRefundAsyncPort.scheduleOrderRefundComplete(cid, oid);
						long bn = longVal(row.get("aftersales_bn"));
						Aftersales persisted =
								aftersalesMapper.selectOne(
										new LambdaQueryWrapper<Aftersales>()
												.eq(Aftersales::getCompanyId, cid)
												.eq(Aftersales::getAftersalesBn, bn));
						if (persisted != null) {
							jushuitanTradeAftersalesDispatchPublisher.publish(
									jushuitanTradeAftersalesBusPayloadBuilder.build(
											persisted, new LinkedHashMap<>()));
							wdtErpTradeAfterSaleDispatchPublisher.publish(
									wdtErpTradeAfterSaleBusPayloadBuilder.build(persisted));
						}
						aftersalesRefundAsyncPort.scheduleInvoiceRed(row);
					}
				});
	}

	private void registerRejectJushuitanAfterCommit(long companyId, long aftersalesBn) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		final long cid = companyId;
		final long bn = aftersalesBn;
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						Aftersales persisted =
								aftersalesMapper.selectOne(
										new LambdaQueryWrapper<Aftersales>()
												.eq(Aftersales::getCompanyId, cid)
												.eq(Aftersales::getAftersalesBn, bn));
						if (persisted != null) {
							jushuitanTradeAftersalesDispatchPublisher.publish(
									jushuitanTradeAftersalesBusPayloadBuilder.build(
											persisted, new LinkedHashMap<>()));
							applicationEventPublisher.publishEvent(
									new JushuitanTradeAftersalesSyncSpringEvent(
											AftersalesReviewService.this, aftersalesToSnakeMap(persisted)));
						}
					}
				});
	}

	private void applyRefundFeePointFreightFromParam(Map<String, Object> param, Aftersales a) {
		if (param.containsKey("refund_fee") && !isLooseEmpty(param.get("refund_fee"))) {
			int v = intVal(param.get("refund_fee"));
			if (v <= 0) {
				param.put("refund_fee", a.getRefundFee() == null ? 0 : a.getRefundFee());
			}
		}
		if (param.containsKey("refund_point") && !isLooseEmpty(param.get("refund_point"))) {
			int v = intValFlexible(param.get("refund_point"), 0);
			if (v <= 0) {
				param.put("refund_point", a.getRefundPoint() == null ? 0 : a.getRefundPoint());
			}
		}
		if (param.containsKey("freight") && !isLooseEmpty(param.get("freight"))) {
			int v = intValFlexible(param.get("freight"), 0);
			if (v <= 0) {
				param.put("freight", a.getFreight() == null ? 0 : a.getFreight());
			}
		}
	}

	private void normalizeActionDefaults(LinkedHashMap<String, Object> merged) {
		merged.put("refund_fee", refundFeeIntDefault(merged.get("refund_fee")));
		if (!merged.containsKey("refund_point") || isLooseEmpty(merged.get("refund_point"))) {
			merged.put("refund_point", 0);
		} else {
			merged.put("refund_point", intValFlexible(merged.get("refund_point"), 0));
		}
		if (!merged.containsKey("freight") || isLooseEmpty(merged.get("freight"))) {
			merged.put("freight", 0);
		} else {
			merged.put("freight", intValFlexible(merged.get("freight"), 0));
		}
	}

	private static int refundFeeIntDefault(Object raw) {
		if (raw == null) {
			return 0;
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void validateReviewParams(LinkedHashMap<String, Object> merged) {
		List<String> errors = new ArrayList<>();
		if (!merged.containsKey("aftersales_bn") || merged.get("aftersales_bn") == null) {
			errors.add("售后单号必填");
		} else {
			Object raw = merged.get("aftersales_bn");
			if (isBatchForm(raw)) {
				if (parseBnList(raw).isEmpty()) {
					errors.add("售后单号必填");
				}
			} else if (isLooseEmpty(raw)) {
				errors.add("售后单号必填");
			}
		}
		if (!merged.containsKey("is_approved") || isApprovedSelectionMissing(merged.get("is_approved"))) {
			errors.add("处理结果必选");
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException(String.join("、", errors));
		}
		if (refuseReasonRequired(merged.get("is_approved"))) {
			if (!merged.containsKey("refuse_reason") || isLooseEmpty(merged.get("refuse_reason"))) {
				throw new BadRequestException("拒绝原因必填");
			}
		}
	}

	private static boolean isApprovedSelectionMissing(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		return false;
	}

	private static boolean refuseReasonRequired(Object isApprovedRaw) {
		if (isApprovedRaw instanceof Boolean) {
			return false;
		}
		return !isApprovedApproved(isApprovedRaw);
	}

	private static boolean isApprovedApproved(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof Character c) {
			return c != '0' && c != 0;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		return true;
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

	private Map<String, Object> buildTemplateData(Map<String, Object> param, Aftersales a) {
		Map<String, Object> templateData = new LinkedHashMap<>();
		templateData.put("aftersales_type", a.getAftersalesType());
		templateData.put("aftersales_bn", a.getAftersalesBn());
		templateData.put("user_id", a.getUserId());
		templateData.put("company_id", a.getCompanyId());
		templateData.put("refuse_reason", str(param.get("refuse_reason")));
		templateData.put("order_id", a.getOrderId());
		templateData.put("refund_fee", a.getRefundFee());
		return templateData;
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

	private static int intValFlexible(Object raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v < 0 ? defaultValue : v;
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(t);
			return v < 0 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
