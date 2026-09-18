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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.support.AftersalesRefundEntityTradeRefundPayloadMapper;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AftersalesShopPartialCancelCreateService {

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final AftersalesRefundAsyncPort aftersalesRefundAsyncPort;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	private final WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;
	private final TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	private final AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper;

	public AftersalesShopPartialCancelCreateService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			AftersalesRefundAsyncPort aftersalesRefundAsyncPort,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher,
			WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder,
			@Qualifier("tradeRefundAsyncFanOut") TradeRefundDispatchPublisher tradeRefundDispatchPublisher,
			AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.aftersalesRefundAsyncPort = aftersalesRefundAsyncPort;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.wdtErpTradeAfterSaleDispatchPublisher = wdtErpTradeAfterSaleDispatchPublisher;
		this.wdtErpTradeAfterSaleBusPayloadBuilder = wdtErpTradeAfterSaleBusPayloadBuilder;
		this.tradeRefundDispatchPublisher = tradeRefundDispatchPublisher;
		this.tradeRefundPayloadMapper = tradeRefundPayloadMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public Map<String, Object> createForShopPartialCancel(
			long companyId,
			long orderId,
			long userId,
			long supplierId,
			String operatorType,
			long operatorId,
			String reasonText,
			List<Map<String, Object>> detail) {
		if (detail == null || detail.isEmpty()) {
			throw new ResourceException("没有可退款商品");
		}
		Optional<Map<String, Object>> tradeOpt = orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isEmpty()) {
			throw new ResourceException("支付信息未找到！");
		}
		Map<String, Object> trade = tradeOpt.get();
		String tradeId = String.valueOf(trade.get("trade_id"));

		Optional<Map<String, Object>> headOpt = orderNormalOrderHeaderReadPort.getHeader(companyId, orderId);
		if (headOpt.isEmpty()) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		Map<String, Object> orderHead = headOpt.get();

		List<Map<String, Object>> orderLines = orderNormalOrderItemsReadPort.listItems(companyId, orderId);

		long aftersalesBn = genAftersalesBnLong();
		int now = (int) (System.currentTimeMillis() / 1000L);

		Map<String, NormalOrderLineView> lineById = new LinkedHashMap<>();
		for (Map<String, Object> ol : orderLines) {
			NormalOrderLineView v = NormalOrderLineView.from(ol);
			lineById.put(String.valueOf(v.id), v);
		}

		int totalRefundFee = 0;
		int totalRefundPoint = 0;
		long lastSupplierId = 0L;
		String lastItemBn = "";

		for (Map<String, Object> d : detail) {
			long subId = longVal(d.get("id"));
			int applyNum = intVal(d.get("num"));
			if (applyNum <= 0) {
				continue;
			}
			NormalOrderLineView sub = lineById.get(String.valueOf(subId));
			if (sub == null) {
				throw new ResourceException("查询订单明细错误");
			}
			int refundFee;
			int refundPoint;
			if (applyNum >= sub.num) {
				refundFee = sub.totalFee;
				refundPoint = sub.point;
			} else {
				refundFee = (int) Math.floor((double) sub.totalFee * applyNum / sub.num);
				refundPoint = (int) Math.floor((double) sub.point * applyNum / sub.num);
			}
			totalRefundFee += refundFee;
			totalRefundPoint += refundPoint;
			lastSupplierId = sub.supplierId;
			lastItemBn = sub.itemBn;

			AftersalesDetail ad = new AftersalesDetail();
			ad.setCompanyId(companyId);
			ad.setUserId(userId);
			ad.setDistributorId(sub.distributorId);
			ad.setAftersalesBn(aftersalesBn);
			ad.setOrderId(String.valueOf(orderId));
			ad.setSubOrderId(subId);
			ad.setGoodsId(sub.goodsId);
			ad.setItemId(sub.itemId);
			ad.setItemBn(sub.itemBn);
			ad.setItemPic(sub.pic);
			ad.setRefundFee(refundFee);
			ad.setRefundPoint(refundPoint);
			ad.setItemName(sub.itemName);
			ad.setOrderItemType(sub.orderItemType);
			ad.setNum(applyNum);
			ad.setAftersalesType("ONLY_REFUND");
			ad.setProgress(0);
			ad.setAftersalesStatus(0);
			ad.setCreateTime(now);
			ad.setUpdateTime(now);
			aftersalesDetailMapper.insert(ad);
		}

		if (totalRefundFee == 0 && totalRefundPoint == 0) {
			throw new ResourceException("没有可退款商品");
		}

		long shopId = longVal(orderHead.get("shop_id"));
		long distributorId = longVal(orderHead.get("distributor_id"));
		long merchantId = longVal(orderHead.get("merchant_id"));
		String payType = str(orderHead.get("pay_type"));
		String freightType = str(orderHead.get("freight_type"));
		if (!StringUtils.hasText(freightType)) {
			freightType = "cash";
		}

		Aftersales main = new Aftersales();
		main.setAftersalesBn(aftersalesBn);
		main.setShopId(shopId);
		main.setOrderId(orderId);
		main.setCompanyId(companyId);
		main.setSupplierId((int) Math.min(Integer.MAX_VALUE, Math.max(0, lastSupplierId)));
		main.setUserId(userId);
		main.setDistributorId(distributorId);
		main.setAftersalesType("ONLY_REFUND");
		main.setAftersalesStatus(0);
		main.setProgress(0);
		main.setReason(reasonText);
		main.setRefundFee(totalRefundFee);
		main.setRefundPoint(totalRefundPoint);
		main.setItemBn(lastItemBn);
		main.setIsPartialCancel(true);
		main.setFreight(0);
		main.setFreightType(freightType);
		main.setMerchantId(merchantId);
		main.setCreateTime(now);
		main.setUpdateTime(now);
		aftersalesMapper.insert(main);

		String tradePayType = str(trade.get("pay_type"));
		String refundChannel = "offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";
		int curPayFeeInt =
				"point".equalsIgnoreCase(tradePayType)
						? totalRefundPoint
						: (int) Math.round(totalRefundFee * doubleVal(trade.get("cur_fee_rate")));

		long refundBn = genRefundBnLong();
		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundBn(refundBn);
		refund.setAftersalesBn(aftersalesBn);
		refund.setOrderId(orderId);
		refund.setTradeId(tradeId);
		refund.setCompanyId(companyId);
		refund.setSupplierId(lastSupplierId);
		refund.setUserId(userId);
		refund.setShopId(shopId);
		refund.setDistributorId(distributorId);
		refund.setRefundType("0");
		refund.setRefundChannel(refundChannel);
		refund.setRefundStatus("READY");
		refund.setRefundFee(totalRefundFee);
		refund.setRefundPoint(totalRefundPoint);
		refund.setReturnFreight(0);
		refund.setFreight(0);
		refund.setFreightType(freightType);
		refund.setPayType(payType);
		refund.setCurrency("point".equalsIgnoreCase(tradePayType) ? "" : str(trade.get("fee_type")));
		refund.setCurFeeType("point".equalsIgnoreCase(tradePayType) ? "" : str(trade.get("cur_fee_type")));
		refund.setCurFeeRate(
				trade.get("cur_fee_rate") instanceof Number
						? ((Number) trade.get("cur_fee_rate")).doubleValue()
						: 1.0);
		refund.setCurFeeSymbol("point".equalsIgnoreCase(tradePayType) ? "" : str(trade.get("cur_fee_symbol")));
		refund.setCurPayFee(String.valueOf(curPayFeeInt));
		refund.setReturnPoint(0);
		refund.setMerchantId(longVal(trade.get("merchant_id")));
		refund.setCreateTime(now);
		refund.setUpdateTime(now);
		aftersalesRefundMapper.insert(refund);

		long resolvedOperatorId = "user".equalsIgnoreCase(operatorType) ? userId : operatorId;
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderId);
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("supplier_id", supplierId);
		params.put("operator_type", operatorType);
		params.put("operator_id", resolvedOperatorId);
		params.put("reason", reasonText);
		params.put("is_partial_cancel", true);
		params.put("aftersales_bn", aftersalesBn);
		params.put("detail", detail);

		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		log.put("supplier_id", supplierId);
		log.put("operator_type", operatorType);
		log.put("operator_id", resolvedOperatorId);
		log.put("remarks", "订单售后");
		log.put("detail", "售后单号：" + aftersalesBn + " 申请售后，申请原因：" + reasonText);
		log.put("params", params);
		log.put("is_show", false);
		orderProcessLogPublishPort.publish(log);

		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		String redisKey = "OrderPayStatistics:normal:" + companyId + ":" + day;
		sharedStringRedisTemplate.opsForHash().increment(redisKey, "orderAftersales", 1);
		if (distributorId > 0) {
			sharedStringRedisTemplate
					.opsForHash()
					.increment(redisKey, distributorId + "_orderAftersales", 1);
		}
		if (merchantId > 0) {
			sharedStringRedisTemplate
					.opsForHash()
					.increment(redisKey, merchantId + "_merchant_orderAftersales", 1);
		}

		registerTradeRefundDispatchPublishPostCommit(companyId, aftersalesBn);
		registerWdtErpTradeAfterSalePublishPostCommit(companyId, aftersalesBn);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("aftersales_bn", aftersalesBn);
		out.put("company_id", companyId);
		out.put("order_id", orderId);
		out.put("user_id", userId);
		out.put("refund_fee", totalRefundFee);
		out.put("refund_point", totalRefundPoint);
		out.put("supplier_id", (int) lastSupplierId);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public void autoApproveOnlyRefund(
			long companyId,
			long aftersalesBn,
			int refundFee,
			int refundPoint,
			int freight,
			String operatorType,
			long operatorId,
			long userId) {
		Aftersales a =
				aftersalesMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn)
								.last("LIMIT 1"));
		if (a == null) {
			throw new ResourceException("售后单数据异常");
		}
		if (a.getAftersalesStatus() == null || a.getAftersalesStatus() != 0) {
			throw new ResourceException("售后" + aftersalesBn + "已处理，无需审核");
		}
		if (!"ONLY_REFUND".equalsIgnoreCase(a.getAftersalesType())) {
			throw new ResourceException("售后类型不支持自动审核");
		}
		int useRefundFee = refundFee > 0 ? refundFee : (a.getRefundFee() == null ? 0 : a.getRefundFee());
		int useRefundPoint = refundPoint > 0 ? refundPoint : (a.getRefundPoint() == null ? 0 : a.getRefundPoint());
		int useFreight = freight > 0 ? freight : (a.getFreight() == null ? 0 : a.getFreight());
		if (useRefundFee < 0 || useFreight < 0) {
			throw new ResourceException("请填写退款金额并且大于等于0！");
		}
		if (useRefundFee > (a.getRefundFee() == null ? 0 : a.getRefundFee())) {
			throw new ResourceException("退款金额不能大于应退金额！");
		}
		if (useRefundPoint > (a.getRefundPoint() == null ? 0 : a.getRefundPoint())) {
			throw new ResourceException("退款积分不能大于应退积分！");
		}
		if (useFreight > (a.getFreight() == null ? 0 : a.getFreight())) {
			throw new ResourceException("退款运费不能大于应退运费！");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<AftersalesRefund> ru = new LambdaUpdateWrapper<>();
		ru.eq(AftersalesRefund::getCompanyId, companyId)
				.eq(AftersalesRefund::getAftersalesBn, aftersalesBn)
				.set(AftersalesRefund::getRefundStatus, "AUDIT_SUCCESS")
				.set(AftersalesRefund::getRefundFee, useRefundFee)
				.set(AftersalesRefund::getRefundPoint, useRefundPoint)
				.set(AftersalesRefund::getFreight, useFreight)
				.set(AftersalesRefund::getReturnFreight, useFreight > 0 ? 1 : 0)
				.set(AftersalesRefund::getUpdateTime, now);
		aftersalesRefundMapper.update(null, ru);

		LambdaUpdateWrapper<Aftersales> au = new LambdaUpdateWrapper<>();
		au.eq(Aftersales::getCompanyId, companyId)
				.eq(Aftersales::getAftersalesBn, aftersalesBn)
				.set(Aftersales::getProgress, 9)
				.set(Aftersales::getAftersalesStatus, 1)
				.set(Aftersales::getUpdateTime, now);
		aftersalesMapper.update(null, au);

		LambdaUpdateWrapper<AftersalesDetail> du = new LambdaUpdateWrapper<>();
		du.eq(AftersalesDetail::getCompanyId, companyId)
				.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
				.set(AftersalesDetail::getProgress, 9)
				.set(AftersalesDetail::getAftersalesStatus, 1)
				.set(AftersalesDetail::getUpdateTime, now);
		aftersalesDetailMapper.update(null, du);

		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", a.getOrderId());
		log.put("company_id", companyId);
		log.put("operator_type", operatorType);
		log.put("operator_id", "user".equalsIgnoreCase(operatorType) ? userId : operatorId);
		log.put("remarks", "订单售后");
		log.put("detail", "售后单号：" + aftersalesBn + "，同意退款");
		orderProcessLogPublishPort.publish(log);

		a.setProgress(9);
		a.setAftersalesStatus(1);
		a.setUpdateTime(now);
		a.setRefundFee(useRefundFee);
		a.setRefundPoint(useRefundPoint);
		a.setFreight(useFreight);

		long oid = a.getOrderId() == null ? 0L : a.getOrderId();
		Map<String, Object> invoiceRedSnapshot = aftersalesToSnakeMapForInvoiceRed(a);
		registerScheduleOrderRefundCompletePostCommit(companyId, oid, invoiceRedSnapshot);
		registerJushuitanTradeAftersalesPublishPostCommit(companyId, aftersalesBn);
	}

	private void registerScheduleOrderRefundCompletePostCommit(
			long companyId, long orderId, Map<String, Object> invoiceRedAftersalesSnapshot) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		final long cid = companyId;
		final long oid = orderId;
		final Map<String, Object> snapshot = new LinkedHashMap<>(invoiceRedAftersalesSnapshot);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						aftersalesRefundAsyncPort.scheduleOrderRefundComplete(cid, oid);
					}
				});
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						aftersalesRefundAsyncPort.scheduleInvoiceRed(snapshot);
					}
				});
	}

	private void registerTradeRefundDispatchPublishPostCommit(long companyId, long aftersalesBn) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		final long cid = companyId;
		final long bn = aftersalesBn;
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						AftersalesRefund persisted =
								aftersalesRefundMapper.selectOne(
										new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
														AftersalesRefund>()
												.eq(AftersalesRefund::getCompanyId, cid)
												.eq(AftersalesRefund::getAftersalesBn, bn)
												.last("LIMIT 1"));
						if (persisted != null) {
							tradeRefundDispatchPublisher.publish(
									tradeRefundPayloadMapper.toDispatchPayload(persisted));
						}
					}
				});
	}

	private void registerWdtErpTradeAfterSalePublishPostCommit(long companyId, long aftersalesBn) {
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
										new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
														Aftersales>()
												.eq(Aftersales::getCompanyId, cid)
												.eq(Aftersales::getAftersalesBn, bn)
												.last("LIMIT 1"));
						if (persisted != null) {
							wdtErpTradeAfterSaleDispatchPublisher.publish(
									wdtErpTradeAfterSaleBusPayloadBuilder.build(persisted));
						}
					}
				});
	}

	private void registerJushuitanTradeAftersalesPublishPostCommit(long companyId, long aftersalesBn) {
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
										new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
														Aftersales>()
												.eq(Aftersales::getCompanyId, cid)
												.eq(Aftersales::getAftersalesBn, bn)
												.last("LIMIT 1"));
						if (persisted != null) {
							jushuitanTradeAftersalesDispatchPublisher.publish(
									jushuitanTradeAftersalesBusPayloadBuilder.build(
											persisted, new LinkedHashMap<>()));
						}
					}
				});
	}

	private Map<String, Object> aftersalesToSnakeMapForInvoiceRed(Aftersales a) {
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
		String autoRefuse = "0";
		if (a.getCompanyId() != null && a.getAftersalesBn() != null) {
			AftersalesDetail d =
					aftersalesDetailMapper.selectOne(
							new LambdaQueryWrapper<AftersalesDetail>()
									.eq(AftersalesDetail::getCompanyId, a.getCompanyId())
									.eq(AftersalesDetail::getAftersalesBn, a.getAftersalesBn())
									.orderByAsc(AftersalesDetail::getDetailId)
									.last("LIMIT 1"));
			if (d != null && d.getAutoRefuseTime() != null && !d.getAutoRefuseTime().isBlank()) {
				autoRefuse = d.getAutoRefuseTime();
			}
		}
		m.put("auto_refuse_time", autoRefuse);
		return m;
	}

	private static long genAftersalesBnLong() {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		int rnd = ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
		return Long.parseLong(day + rnd);
	}

	private static long genRefundBnLong() {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		long rnd = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
		return Long.parseLong("2" + day + rnd);
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

	private static double doubleVal(Object o) {
		if (o == null) {
			return 1.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static final class NormalOrderLineView {
		private final long id;
		private final int num;
		private final int totalFee;
		private final int point;
		private final long supplierId;
		private final long distributorId;
		private final long goodsId;
		private final long itemId;
		private final String itemBn;
		private final String itemName;
		private final String orderItemType;
		private final String pic;

		private NormalOrderLineView(
				long id,
				int num,
				int totalFee,
				int point,
				long supplierId,
				long distributorId,
				long goodsId,
				long itemId,
				String itemBn,
				String itemName,
				String orderItemType,
				String pic) {
			this.id = id;
			this.num = num;
			this.totalFee = totalFee;
			this.point = point;
			this.supplierId = supplierId;
			this.distributorId = distributorId;
			this.goodsId = goodsId;
			this.itemId = itemId;
			this.itemBn = itemBn;
			this.itemName = itemName;
			this.orderItemType = orderItemType;
			this.pic = pic;
		}

		static NormalOrderLineView from(Map<String, Object> m) {
			return new NormalOrderLineView(
					longVal(m.get("id")),
					intVal(m.get("num")),
					intVal(m.get("total_fee")),
					intVal(m.get("point")),
					longVal(m.get("supplier_id")),
					longVal(m.get("distributor_id")),
					longVal(m.get("goods_id")),
					longVal(m.get("item_id")),
					str(m.get("item_bn")),
					str(m.get("item_name")),
					str(m.get("order_item_type")),
					str(m.get("pic")));
		}
	}
}
