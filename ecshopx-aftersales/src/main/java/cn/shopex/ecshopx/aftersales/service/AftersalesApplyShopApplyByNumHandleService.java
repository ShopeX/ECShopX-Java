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
import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyPostCommitCommand;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.distribution.OfflineAftersalesDistributorHeadReadPort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.order.OrderItemsProfitWritePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Shop quantity-based aftersales apply: transactional persistence then ordered post-commit work.
 * When synchronization is active, post-commit runs after {@code TransactionTemplate} commit via
 * {@code registerSynchronization#afterCommit}; otherwise it runs immediately after the template returns.
 *
 * <p>For {@code REFUND_GOODS} and {@code EXCHANGING_GOODS}, post-commit publishes
 * {@link TradeAftersalesDispatchPublisher} and {@link ThirdPartyTradeAftersalesSaasErpDispatchPublisher}
 * with the persisted aftersales entity map, then shared wait-deal and integration publishers.
 *
 * <p>For other types, that hook publishes the trade refund fan-out on {@link TradeRefundDispatchPublisher}
 * before Jushuitan, WDT, and other integration publishers (see private post-commit ordering method in this
 * class). The published value is the refund row map produced from persistence, merged with aftersales
 * header entries where the row does not already define a key.
 */
@Service
public class AftersalesApplyShopApplyByNumHandleService {

	private static final String ORDER_PROCESS_LOG_REMARKS_AFTER_SALES = "订单售后";

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesRefundService aftersalesRefundService;
	private final AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService;
	private final OrderItemsProfitWritePort orderItemsProfitWritePort;
	private final AftersalesBrokeragePort aftersalesBrokeragePort;
	private final JdbcTemplate jdbcTemplate;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher;
	private final AftersalesApplyAsyncPort aftersalesApplyAsyncPort;
	private final SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher;
	private final OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort;
	private final OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort;
	private final OfflineAftersalesDistributorHeadReadPort offlineAftersalesDistributorHeadReadPort;
	private final WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	private final WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;
	private final TradeRefundDispatchPublisher tradeRefundDispatchPublisher;
	private final TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher;
	private final ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher;

	private static final String SQL_DISTRIBUTOR_VALID =
			"(is_valid = 1 OR LOWER(TRIM(CAST(is_valid AS CHAR))) IN ('true','1'))";

	public AftersalesApplyShopApplyByNumHandleService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesRefundService aftersalesRefundService,
			AftersalesApplyDetailQueryService aftersalesApplyDetailQueryService,
			OrderItemsProfitWritePort orderItemsProfitWritePort,
			AftersalesBrokeragePort aftersalesBrokeragePort,
			JdbcTemplate jdbcTemplate,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			ApplicationEventPublisher applicationEventPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher,
			AftersalesApplyAsyncPort aftersalesApplyAsyncPort,
			SendAfterSaleWaitDealNoticeJobDispatchPublisher sendAfterSaleWaitDealNoticeJobDispatchPublisher,
			OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort,
			OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager,
			DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort,
			OfflineAftersalesDistributorHeadReadPort offlineAftersalesDistributorHeadReadPort,
			WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher,
			WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder,
			@Qualifier("tradeRefundAsyncFanOut") TradeRefundDispatchPublisher tradeRefundDispatchPublisher,
			@Qualifier("tradeAftersalesAsyncFanOut") TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher,
			ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesRefundService = aftersalesRefundService;
		this.aftersalesApplyDetailQueryService = aftersalesApplyDetailQueryService;
		this.orderItemsProfitWritePort = orderItemsProfitWritePort;
		this.aftersalesBrokeragePort = aftersalesBrokeragePort;
		this.jdbcTemplate = jdbcTemplate;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.invoiceRedJobDispatchPublisher = invoiceRedJobDispatchPublisher;
		this.aftersalesApplyAsyncPort = aftersalesApplyAsyncPort;
		this.sendAfterSaleWaitDealNoticeJobDispatchPublisher = sendAfterSaleWaitDealNoticeJobDispatchPublisher;
		this.orderValidityPlatformSettingReadPort = orderValidityPlatformSettingReadPort;
		this.orderNormalOrderItemsReadPort = orderNormalOrderItemsReadPort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.distributorAftersalesAddressDetailReadPort = distributorAftersalesAddressDetailReadPort;
		this.offlineAftersalesDistributorHeadReadPort = offlineAftersalesDistributorHeadReadPort;
		this.wdtErpTradeAfterSaleDispatchPublisher = wdtErpTradeAfterSaleDispatchPublisher;
		this.wdtErpTradeAfterSaleBusPayloadBuilder = wdtErpTradeAfterSaleBusPayloadBuilder;
		this.tradeRefundDispatchPublisher = tradeRefundDispatchPublisher;
		this.tradeAftersalesDispatchPublisher = tradeAftersalesDispatchPublisher;
		this.thirdPartyTradeAftersalesSaasErpDispatchPublisher = thirdPartyTradeAftersalesSaasErpDispatchPublisher;
	}

	/**
	 * Runs the quantity-based apply handle: transactional inserts and updates execute inside a
	 * {@link TransactionTemplate}, with optional {@link TransactionSynchronizationManager} after-commit
	 * registration when the surrounding context has an active transaction.
	 * <p>After commit, the non goods-return / non exchange branch invokes
	 * {@link TradeRefundDispatchPublisher#publish} once with a map that combines the persisted refund
	 * row (fees, points, identifiers, {@code refund_channel}, {@code refund_status}, and related fields)
	 * with aftersales header values merged in place where not already set, so asynchronous trade-refund
	 * listeners receive the same shape as the in-request refund entity.
	 */
	public void shopApplyByNumHandle(
			Map<String, Object> orderInfo, Map<String, Object> trade, Map<String, Object> data) {
		long companyId = longVal(data.get("company_id"));
		String rt0 = strOrDefault(data.get("return_type"), "logistics");
		if ("offline".equalsIgnoreCase(rt0) && longVal(data.get("aftersales_address_id")) > 0L) {
			resolveOfflineAftersalesReturn(companyId, orderInfo, data);
		}
		Map<String, Object> setting = orderValidityPlatformSettingReadPort.readPlatformSetting(companyId);
		boolean isRefundFreight = truthySetting(setting.get("is_refund_freight"));

		long aftersalesBn = genAftersalesBnLong();
		Map<String, Object> aftersalesData = buildAftersalesDataShell(orderInfo, data, aftersalesBn);

		AftersalesApplyPostCommitCommand[] asyncCmdHolder = new AftersalesApplyPostCommitCommand[1];
		boolean[] asyncRegistered = {false};

		TxOutcome outcome =
				transactionTemplate.execute(
						status -> {
							try {
								return runTransactionalCore(
										orderInfo,
										trade,
										data,
										aftersalesBn,
										aftersalesData,
										isRefundFreight,
										asyncCmdHolder,
										asyncRegistered);
							} catch (RuntimeException e) {
								status.setRollbackOnly();
								throw e;
							}
						});

		if (!asyncRegistered[0] && asyncCmdHolder[0] != null) {
			performPostCommitOrdered(companyId, orderInfo, data, outcome, asyncCmdHolder[0]);
		}
	}

	/**
	 * Runs ordered post-commit side effects after a successful apply transaction: Redis counters,
	 * integration-oriented Spring events, wait-deal notification dispatch, then routed async work.
	 *
	 * <p>The wait-deal notification publish performed here is shared by internal quantity-based shop
	 * apply and by related front aftersales creation flows that complete through this same
	 * post-commit hook; all converge on the same publisher call once persistence has succeeded.
	 *
	 * <p>{@code TradeRefundDispatchPublisher#publish} fans out on the Dispatch Bus (including
	 * {@code listener:thirdparty.listeners.TradeRefundSendSaasErp}); register that listener only once
	 * in {@code SystemLinkTradeRefundDispatchListenerRegistrationConfig}.
	 */
	private void performPostCommitOrdered(
			long companyId,
			Map<String, Object> orderInfo,
			Map<String, Object> data,
			TxOutcome outcome,
			AftersalesApplyPostCommitCommand cmd) {
		String ymd = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE);
		String redisKey = "OrderPayStatistics:normal:" + companyId + ":" + ymd;
		companysRedisTemplate.opsForHash().increment(redisKey, "orderAftersales", 1L);
		if (orderInfo.containsKey("distributor_id") && orderInfo.get("distributor_id") != null) {
			companysRedisTemplate
					.opsForHash()
					.increment(redisKey, orderInfo.get("distributor_id") + "_orderAftersales", 1L);
		}
		Object merchantRaw = orderInfo.get("merchant_id");
		if (merchantRaw != null && !str(merchantRaw).isEmpty() && longVal(merchantRaw) != 0L) {
			companysRedisTemplate
					.opsForHash()
					.increment(redisKey, merchantRaw + "_merchant_orderAftersales", 1L);
		}

		String ast = str(data.get("aftersales_type"));
		if ("REFUND_GOODS".equals(ast) || "EXCHANGING_GOODS".equals(ast)) {
			tradeAftersalesDispatchPublisher.publish(outcome.aftersalesPayload());
			thirdPartyTradeAftersalesSaasErpDispatchPublisher.publish(outcome.aftersalesPayload());
		} else {
			Map<String, Object> refundRow = outcome.refundPayload();
			mergeAftersalesHeadIntoRefundMapInPlace(refundRow, outcome.aftersalesPayload());
			tradeRefundDispatchPublisher.publish(refundRow);
			applicationEventPublisher.publishEvent(new SaasErpRefundSpringEvent(this, refundRow));
			Map<String, Object> invoiceSnapshot = outcome.aftersalesPayload();
			if (invoiceSnapshot != null) {
				invoiceRedJobDispatchPublisher.publish(new LinkedHashMap<>(invoiceSnapshot));
			}
		}

		sendAfterSaleWaitDealNoticeJobDispatchPublisher.publish(companyId, cmd.aftersalesBn());
		Map<String, Object> jstPayload = outcome.jushuitanBusPayload();
		if (jstPayload != null && !jstPayload.isEmpty()) {
			jushuitanTradeAftersalesDispatchPublisher.publish(jstPayload);
		}

		Aftersales persisted =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, cmd.companyId())
								.eq(Aftersales::getAftersalesBn, cmd.aftersalesBn())
								.last("LIMIT 1"));
		if (persisted != null) {
			wdtErpTradeAfterSaleDispatchPublisher.publish(
					wdtErpTradeAfterSaleBusPayloadBuilder.build(persisted));
		}

		aftersalesApplyAsyncPort.dispatchPostCommitSideEffects(cmd);
	}

	private TxOutcome runTransactionalCore(
			Map<String, Object> orderInfo,
			Map<String, Object> trade,
			Map<String, Object> data,
			long aftersalesBn,
			Map<String, Object> aftersalesData,
			boolean isRefundFreight,
			AftersalesApplyPostCommitCommand[] asyncCmdHolder,
			boolean[] asyncRegistered) {
		long companyId = longVal(data.get("company_id"));
		long orderId = longVal(data.get("order_id"));
		long userId = longVal(data.get("user_id"));
		String ast = str(data.get("aftersales_type"));
		boolean goodsReturned = Boolean.TRUE.equals(data.get("goods_returned"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> detailList = (List<Map<String, Object>>) data.get("detail");
		if (detailList == null || detailList.isEmpty()) {
			throw new ResourceException("请提交审核售后的商品");
		}

		List<Map<String, Object>> orderLines = orderNormalOrderItemsReadPort.listItems(companyId, orderId);

		BigDecimal totalRefundFeeBd = BigDecimal.ZERO;
		BigDecimal totalRefundPointBd = BigDecimal.ZERO;
		BigDecimal totalReturnPointBd = BigDecimal.ZERO;
		int applyNum = 0;
		BigDecimal subItemFeeBd = BigDecimal.ZERO;
		BigDecimal subItemPointBd = BigDecimal.ZERO;
		boolean isRefundFreightFlag = false;

		for (Map<String, Object> v : detailList) {
			long subId = longVal(v.get("id"));
			Map<String, Object> subOrderInfo = findLine(orderLines, subId);
			if (subOrderInfo == null) {
				throw new ResourceException("申请售后商品的订单不存在");
			}
			int lineNum = intVal(subOrderInfo.get("num"));
			BigDecimal lineTotalFeeBd = bdInt(subOrderInfo.get("total_fee"));
			BigDecimal linePointBd = bdInt(subOrderInfo.get("point"));

			if (v.get("total_fee") != null && intVal(v.get("total_fee")) > 0) {
				subItemFeeBd = bdInt(v.get("total_fee"));
			} else {
				subItemFeeBd = floorDivMulBd(lineTotalFeeBd, lineNum, intVal(v.get("num")));
			}
			if (v.get("total_point") != null && intVal(v.get("total_point")) > 0) {
				subItemPointBd = bdInt(v.get("total_point"));
			} else {
				subItemPointBd = floorDivMulBd(linePointBd, lineNum, intVal(v.get("num")));
			}

			int appliedNum = aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, subId);
			int appliedRefundFee =
					aftersalesApplyDetailQueryService.sumAppliedRefundFee(companyId, orderId, subId);
			int appliedRefundPoint =
					aftersalesApplyDetailQueryService.sumAppliedRefundPoint(companyId, orderId, subId);

			BigDecimal refundFeeBd;
			BigDecimal refundPointBd;
			int num = intVal(v.get("num"));
			if (num == lineNum) {
				refundFeeBd = lineTotalFeeBd;
				refundPointBd = linePointBd;
			} else {
				int leftNum = lineNum - appliedNum - num;
				if (leftNum == 0) {
					refundFeeBd = lineTotalFeeBd.subtract(bdInt(appliedRefundFee));
					subItemFeeBd = refundFeeBd;
					refundPointBd = linePointBd.subtract(bdInt(appliedRefundPoint));
					subItemPointBd = refundPointBd;
				} else if (leftNum > 0) {
					refundFeeBd = floorDivMulBd(lineTotalFeeBd, lineNum, num);
					refundPointBd = floorDivMulBd(linePointBd, lineNum, num);
				} else {
					throw new ResourceException("申请售后单数据异常");
				}
			}

			totalRefundFeeBd = totalRefundFeeBd.add(refundFeeBd);
			totalRefundPointBd = totalRefundPointBd.add(refundPointBd);

			String itemName = str(subOrderInfo.get("item_name"));
			if (StringUtils.hasText(str(subOrderInfo.get("item_spec_desc")))) {
				itemName = itemName + "(" + str(subOrderInfo.get("item_spec_desc")) + ")";
			}

			totalReturnPointBd =
					totalReturnPointBd.add(
							computeReturnPointBd(subOrderInfo, num, appliedNum, companyId, orderId));

			int ts = (int) (System.currentTimeMillis() / 1000L);
			AftersalesDetail ad = new AftersalesDetail();
			ad.setCompanyId(companyId);
			ad.setUserId(userId);
			ad.setDistributorId(longVal(orderInfo.get("distributor_id")));
			ad.setAftersalesBn(aftersalesBn);
			ad.setOrderId(String.valueOf(orderId));
			ad.setSubOrderId(subId);
			ad.setGoodsId(longVal(subOrderInfo.get("goods_id")));
			ad.setItemId(longVal(subOrderInfo.get("item_id")));
			ad.setItemBn(str(subOrderInfo.get("item_bn")));
			ad.setItemPic(str(subOrderInfo.get("pic")));
			ad.setRefundFee(bdToNonNegInt(subItemFeeBd));
			ad.setRefundPoint(bdToNonNegInt(subItemPointBd));
			ad.setItemName(itemName);
			ad.setOrderItemType(str(subOrderInfo.get("order_item_type")));
			ad.setNum(num);
			ad.setAftersalesType(ast);
			ad.setProgress(intVal(aftersalesData.get("progress")));
			ad.setAftersalesStatus(intVal(aftersalesData.get("aftersales_status")));
			ad.setCreateTime(ts);
			ad.setUpdateTime(ts);
			aftersalesDetailMapper.insert(ad);
			applyNum += num;

			orderItemsProfitWritePort.resetOrderProfitStatusByItem(
					companyId, orderId, longVal(subOrderInfo.get("item_id")));

			if ("ONLY_REFUND".equals(ast) || goodsReturned) {
				Map<String, Object> bp = new LinkedHashMap<>();
				bp.put("company_id", companyId);
				bp.put("order_id", orderId);
				bp.put("item_id", longVal(subOrderInfo.get("item_id")));
				bp.put("num", num);
				aftersalesBrokeragePort.brokerageByAftersales(bp);
			}

			aftersalesData.put("supplier_id", Math.min(Integer.MAX_VALUE, intVal(subOrderInfo.get("supplier_id"))));
			aftersalesData.put("item_bn", str(subOrderInfo.get("item_bn")));

			isRefundFreightFlag = isRefundFinishByNum(companyId, orderId, subId, num, orderLines);
		}

		if (subItemFeeBd.compareTo(totalRefundFeeBd) > 0) {
			throw new ResourceException(
					"售后申请金额不能超过剩余金额! "
							+ bdToNonNegInt(subItemFeeBd)
							+ " > "
							+ bdToNonNegInt(totalRefundFeeBd));
		}
		if (subItemPointBd.compareTo(totalRefundPointBd) > 0) {
			throw new ResourceException(
					"售后申请积分不能超过剩余积分! "
							+ bdToNonNegInt(subItemPointBd)
							+ " > "
							+ bdToNonNegInt(totalRefundPointBd));
		}
		aftersalesData.put("refund_fee", bdToNonNegInt(subItemFeeBd));
		aftersalesData.put("refund_point", bdToNonNegInt(subItemPointBd));

		BigDecimal freightBd = BigDecimal.ZERO;
		BigDecimal freightPointBd = BigDecimal.ZERO;
		String orderFreightType = str(orderInfo.get("freight_type"));
		if (isRefundFreight && isRefundFreightFlag) {
			if ("cash".equals(orderFreightType)) {
				BigDecimal maxFreightBd = bdInt(orderInfo.get("freight_fee"));
				int refundedFreight =
						aftersalesApplyDetailQueryService.sumAppliedFreightCash(companyId, orderId);
				BigDecimal remainFreightBd = maxFreightBd.subtract(bdInt(refundedFreight));
				BigDecimal capBd = maxFreightBd.min(remainFreightBd);
				BigDecimal reqBd = bdInt(data.get("freight"));
				freightBd =
						reqBd.compareTo(BigDecimal.ZERO) > 0 && reqBd.compareTo(capBd) <= 0 ? reqBd : capBd;
				aftersalesData.put("freight", bdToNonNegInt(freightBd));
			} else if ("point".equals(orderFreightType)) {
				BigDecimal maxFpBd = bdInt(orderInfo.get("freight_point"));
				int refundedFp =
						aftersalesApplyDetailQueryService.sumAppliedFreightPoint(companyId, orderId);
				BigDecimal remainFpBd = maxFpBd.subtract(bdInt(refundedFp));
				BigDecimal capFpBd = maxFpBd.min(remainFpBd);
				BigDecimal reqFpBd = bdInt(data.get("freight"));
				freightPointBd =
						reqFpBd.compareTo(BigDecimal.ZERO) > 0 && reqFpBd.compareTo(capFpBd) <= 0
								? reqFpBd
								: capFpBd;
				aftersalesData.put("freight", 0);
			}
			aftersalesData.put("freight_type", orderFreightType);
		} else {
			aftersalesData.put(
					"freight_type", str(orderInfo.get("freight_type")).isEmpty() ? "cash" : orderFreightType);
			aftersalesData.put("freight", 0);
		}

		int tsMain = (int) (System.currentTimeMillis() / 1000L);
		Aftersales main = new Aftersales();
		main.setAftersalesBn(aftersalesBn);
		main.setShopId(longVal(orderInfo.get("shop_id")));
		main.setOrderId(orderId);
		main.setCompanyId(companyId);
		main.setSupplierId(intVal(aftersalesData.get("supplier_id")));
		main.setUserId(userId);
		main.setDistributorId(longVal(orderInfo.get("distributor_id")));
		main.setAftersalesType(ast);
		main.setAftersalesStatus(intVal(aftersalesData.get("aftersales_status")));
		main.setProgress(intVal(aftersalesData.get("progress")));
		main.setReason(str(data.get("reason")));
		main.setDescription(str(data.get("description")));
		main.setEvidencePic(serializeEvidence(data.get("evidence_pic")));
		main.setRefundFee(bdToNonNegInt(subItemFeeBd));
		main.setRefundPoint(bdToNonNegInt(subItemPointBd));
		main.setItemBn(str(aftersalesData.get("item_bn")));
		main.setSalesmanId(longVal(data.get("salesman_id")));
		main.setMerchantId(longVal(orderInfo.get("merchant_id")));
		main.setReturnType(strOrDefault(data.get("return_type"), "logistics"));
		Object retDist = data.get("return_distributor_id");
		main.setReturnDistributorId(
				retDist != null ? longVal(retDist) : longVal(data.get("distributor_id")));
		main.setContact(str(data.get("contact")));
		String mobMain = str(data.get("mobile"));
		if (mobMain.isEmpty()) {
			mobMain = str(orderInfo.get("mobile"));
		}
		main.setMobile(mobMain);
		main.setSelfDeliveryOperatorId(longVal(data.get("self_delivery_operator_id")));
		if (data.get("offline_aftersales_address_json") != null) {
			main.setAftersalesAddress(str(data.get("offline_aftersales_address_json")));
		}
		main.setFreight(bdToNonNegInt(bdInt(aftersalesData.get("freight"))));
		main.setFreightType(str(aftersalesData.get("freight_type")));
		main.setCreateTime(tsMain);
		main.setUpdateTime(tsMain);
		aftersalesMapper.insert(main);

		int leftAftersalesNum = intVal(orderInfo.get("left_aftersales_num"));
		int nextLeftAftersalesNum = Math.max(0, leftAftersalesNum - applyNum);
		jdbcTemplate.update(
				"UPDATE orders_normal_orders SET left_aftersales_num = ? WHERE company_id = ? AND order_id = ?",
				nextLeftAftersalesNum,
				companyId,
				orderId);

		String tradePayType = str(trade.get("pay_type"));
		String refundChannel = "offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";
		BigDecimal curRate = toBd(trade.get("cur_fee_rate"));
		BigDecimal curPayFeeBd = subItemFeeBd.multiply(curRate).setScale(0, RoundingMode.HALF_UP);

		Map<String, Object> refundParam = new LinkedHashMap<>();
		refundParam.put("company_id", companyId);
		refundParam.put("user_id", userId);
		refundParam.put("aftersales_bn", aftersalesBn);
		refundParam.put("order_id", orderId);
		refundParam.put("trade_id", str(trade.get("trade_id")));
		refundParam.put("shop_id", longVal(aftersalesData.get("shop_id")));
		refundParam.put("distributor_id", longVal(orderInfo.get("distributor_id")));
		refundParam.put("refund_type", 0);
		refundParam.put("refund_channel", refundChannel);
		refundParam.put("refund_fee", bdToNonNegInt(subItemFeeBd));
		refundParam.put("refund_point", bdToNonNegInt(subItemPointBd));
		refundParam.put("return_freight", 0);
		refundParam.put("pay_type", str(orderInfo.get("pay_type")));
		refundParam.put("currency", str(trade.get("fee_type")));
		refundParam.put("cur_fee_type", str(trade.get("cur_fee_type")));
		refundParam.put("cur_fee_rate", trade.get("cur_fee_rate"));
		refundParam.put("cur_fee_symbol", str(trade.get("cur_fee_symbol")));
		refundParam.put("cur_pay_fee", curPayFeeBd.toPlainString());
		refundParam.put("return_point", bdToNonNegInt(totalReturnPointBd));
		refundParam.put("merchant_id", longVal(orderInfo.get("merchant_id")));
		refundParam.put("refund_status", "READY");
		refundParam.put("supplier_id", longVal(aftersalesData.get("supplier_id")));
		if (isRefundFreight && isRefundFreightFlag) {
			refundParam.put("return_freight", 1);
			refundParam.put("freight_type", orderFreightType);
			if ("cash".equals(orderFreightType)) {
				refundParam.put("freight", bdToNonNegInt(freightBd));
			} else {
				refundParam.put("freight", bdToNonNegInt(freightPointBd));
			}
		} else {
			refundParam.put(
					"freight_type", str(orderInfo.get("freight_type")).isEmpty() ? "cash" : orderFreightType);
			refundParam.put("freight", 0);
		}

		aftersalesRefundService.createRefund(refundParam);
		long refundBn = longVal(refundParam.get("refund_bn"));

		Map<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderId);
		log.put("company_id", companyId);
		log.put("supplier_id", longVal(aftersalesData.get("supplier_id")));
		log.put("operator_type", str(data.get("operator_type")));
		log.put("operator_id", longVal(data.get("operator_id")));
		log.put("remarks", ORDER_PROCESS_LOG_REMARKS_AFTER_SALES);
		log.put("detail", "售后单号：" + aftersalesBn + " 后台申请售后，申请原因：" + str(data.get("reason")));
		log.put("params", data);
		orderProcessLogPublishPort.publish(log);

		asyncCmdHolder[0] =
				new AftersalesApplyPostCommitCommand(
						companyId, orderId, ast, goodsReturned, aftersalesBn, refundBn);

		TxOutcome outcome =
				new TxOutcome(
						buildAftersalesPayload(main, aftersalesBn),
						buildRefundPayload(refundParam, refundBn),
						jushuitanTradeAftersalesBusPayloadBuilder.build(main, data));
		AftersalesApplyPostCommitCommand cmd = asyncCmdHolder[0];

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			final long cid = companyId;
			final Map<String, Object> oi = orderInfo;
			final Map<String, Object> d = data;
			final TxOutcome oc = outcome;
			final AftersalesApplyPostCommitCommand c = cmd;
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							performPostCommitOrdered(cid, oi, d, oc, c);
						}
					});
			asyncRegistered[0] = true;
		}

		return outcome;
	}

	private record TxOutcome(
			Map<String, Object> aftersalesPayload,
			Map<String, Object> refundPayload,
			Map<String, Object> jushuitanBusPayload) {}

	/**
	 * Merges header-level aftersales attributes from {@code aftersalesHead} into {@code refundRow} using
	 * {@link Map#putIfAbsent}, so the refund-side map also carries main-row fields (for example
	 * {@code aftersales_type}) for post-commit handling while leaving any keys already set on the refund
	 * row unchanged.
	 */
	private static void mergeAftersalesHeadIntoRefundMapInPlace(
			Map<String, Object> refundRow, Map<String, Object> aftersalesHead) {
		if (refundRow == null || aftersalesHead == null || aftersalesHead.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> e : aftersalesHead.entrySet()) {
			String k = e.getKey();
			if (k == null || e.getValue() == null) {
				continue;
			}
			refundRow.putIfAbsent(k, e.getValue());
		}
	}

	private Map<String, Object> buildAftersalesPayload(Aftersales main, long aftersalesBn) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", aftersalesBn);
		m.put("order_id", main.getOrderId());
		m.put("company_id", main.getCompanyId());
		m.put("aftersales_type", main.getAftersalesType());
		return m;
	}

	private Map<String, Object> buildRefundPayload(Map<String, Object> refundParam, long refundBn) {
		Map<String, Object> m = new LinkedHashMap<>(refundParam);
		m.put("refund_bn", refundBn);
		return m;
	}

	private Map<String, Object> buildAftersalesDataShell(
			Map<String, Object> orderInfo, Map<String, Object> data, long aftersalesBn) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_bn", aftersalesBn);
		m.put("shop_id", longVal(orderInfo.get("shop_id")));
		m.put("order_id", longVal(data.get("order_id")));
		m.put("company_id", longVal(data.get("company_id")));
		m.put("user_id", longVal(data.get("user_id")));
		m.put("distributor_id", longVal(orderInfo.get("distributor_id")));
		m.put("aftersales_type", str(data.get("aftersales_type")));
		m.put("aftersales_status", 0);
		m.put("progress", 0);
		m.put("reason", str(data.get("reason")));
		m.put("description", str(data.get("description")));
		m.put("evidence_pic", data.get("evidence_pic"));
		m.put("salesman_id", longVal(data.get("salesman_id")));
		m.put("contact", str(data.get("contact")));
		String mob = str(data.get("mobile"));
		if (mob.isEmpty()) {
			mob = str(orderInfo.get("mobile"));
		}
		m.put("mobile", mob);
		m.put("merchant_id", longVal(orderInfo.get("merchant_id")));
		m.put("return_type", strOrDefault(data.get("return_type"), "logistics"));
		Object retDist = data.get("return_distributor_id");
		m.put(
				"return_distributor_id",
				retDist != null ? longVal(retDist) : longVal(data.get("distributor_id")));
		m.put("freight", 0);
		return m;
	}

	private boolean isRefundFinishByNum(
			long companyId, long orderId, long nowId, int nowNum, List<Map<String, Object>> allItems) {
		if (allItems == null || allItems.isEmpty()) {
			return false;
		}
		boolean flag = false;
		for (Map<String, Object> v : allItems) {
			if ("gift".equals(str(v.get("order_item_type")))) {
				continue;
			}
			long id = longVal(v.get("id"));
			int lineNum = intVal(v.get("num"));
			int applied = aftersalesApplyDetailQueryService.sumAppliedNum(companyId, orderId, id);
			if (id == nowId) {
				if (nowNum + applied < lineNum) {
					return false;
				}
			} else if (applied < lineNum) {
				return false;
			}
			flag = true;
		}
		return flag;
	}

	private BigDecimal computeReturnPointBd(
			Map<String, Object> subOrderInfo,
			int num,
			int appliedNum,
			long companyId,
			long orderId) {
		int orderNum = intVal(subOrderInfo.get("num"));
		if (orderNum <= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal getPointsBd = bdInt(subOrderInfo.get("get_points"));
		long subId = longVal(subOrderInfo.get("id"));
		if (orderNum - appliedNum - num == 0) {
			List<Map<String, Object>> rows =
					aftersalesApplyDetailQueryService.listReturnPointRows(companyId, orderId, subId);
			BigDecimal allocatedBd = BigDecimal.ZERO;
			for (Map<String, Object> row : rows) {
				int rowNum = intVal(row.get("num"));
				BigDecimal proportion =
						BigDecimal.valueOf(rowNum)
								.divide(BigDecimal.valueOf(orderNum), 5, RoundingMode.HALF_UP);
				allocatedBd =
						allocatedBd.add(
								proportion
										.multiply(getPointsBd)
										.setScale(0, RoundingMode.HALF_UP));
			}
			return getPointsBd.subtract(allocatedBd);
		}
		BigDecimal proportion =
				BigDecimal.valueOf(num).divide(BigDecimal.valueOf(orderNum), 5, RoundingMode.HALF_UP);
		return proportion.multiply(getPointsBd).setScale(0, RoundingMode.HALF_UP);
	}

	private static BigDecimal floorDivMulBd(BigDecimal total, int divisor, int mult) {
		if (divisor <= 0 || mult <= 0) {
			return BigDecimal.ZERO;
		}
		return total
				.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(mult))
				.setScale(0, RoundingMode.FLOOR);
	}

	private static BigDecimal bdInt(Object o) {
		return BigDecimal.valueOf(intVal(o));
	}

	private static int bdToNonNegInt(BigDecimal b) {
		if (b == null) {
			return 0;
		}
		return b.setScale(0, RoundingMode.FLOOR).max(BigDecimal.ZERO).intValue();
	}

	private static Map<String, Object> findLine(List<Map<String, Object>> lines, long subId) {
		if (lines == null) {
			return null;
		}
		for (Map<String, Object> l : lines) {
			if (longVal(l.get("id")) == subId) {
				return l;
			}
		}
		return null;
	}

	private String serializeEvidence(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			return "";
		}
	}

	private void resolveOfflineAftersalesReturn(
			long companyId, Map<String, Object> orderInfo, Map<String, Object> data) {
		long orderDistId = longVal(orderInfo.get("distributor_id"));
		long selfDistributorId = 0L;
		Long headFilter = orderDistId > 0L ? orderDistId : null;
		Optional<Map<String, Object>> headOpt =
				offlineAftersalesDistributorHeadReadPort.findH5OfflineAftersalesHeadRow(companyId, headFilter);
		Map<String, Object> primaryRow = headOpt.orElse(null);
		if (primaryRow == null || primaryRow.isEmpty()) {
			throw new ResourceException("该订单不支持到店退货");
		}
		long resolvedPrimaryDistId;
		if (orderDistId == 0L) {
			resolvedPrimaryDistId = longVal(primaryRow.get("distributor_id"));
			selfDistributorId = resolvedPrimaryDistId;
		} else {
			resolvedPrimaryDistId = orderDistId;
		}

		long addressId = longVal(data.get("aftersales_address_id"));
		Map<String, Object> addr;
		try {
			addr = distributorAftersalesAddressDetailReadPort.getDetail(companyId, addressId, "");
		} catch (ResourceException ex) {
			if ("地址不存在".equals(ex.getMessage())) {
				throw new ResourceException("请选择正确的退货门店");
			}
			throw ex;
		}
		if (addr == null || addr.isEmpty()) {
			throw new ResourceException("请选择正确的退货门店");
		}

		long addrDistId = longVal(addr.get("distributor_id"));
		boolean selfCase = addrDistId == resolvedPrimaryDistId;
		if (!selfCase) {
			List<Long> linked = parseOfflineLinkedDistributorIds(primaryRow.get("offline_aftersales_distributor_id"));
			if (!linked.contains(addrDistId)) {
				throw new ResourceException("请选择正确的退货门店");
			}
		}

		Long returnDid =
				jdbcQueryReturnDistributorId(companyId, addrDistId, selfCase);
		if (returnDid == null) {
			throw new ResourceException("请选择正确的退货门店");
		}
		long returnForMain = (returnDid == selfDistributorId) ? 0L : returnDid;
		data.put("return_distributor_id", returnForMain);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("aftersales_address_id", addressId);
		payload.put("aftersales_contact", str(addr.get("contact")));
		payload.put("aftersales_mobile", str(addr.get("mobile")));
		payload.put(
				"aftersales_address",
				str(addr.get("province"))
						+ str(addr.get("city"))
						+ str(addr.get("area"))
						+ str(addr.get("address")));
		payload.put("aftersales_name", str(addr.get("name")));
		payload.put("aftersales_hours", str(addr.get("hours")));
		try {
			data.put("offline_aftersales_address_json", objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new ResourceException("售后单数据异常");
		}
	}

	private Long jdbcQueryReturnDistributorId(long companyId, long addrDistId, boolean selfCase) {
		String flagCol = selfCase ? "offline_aftersales_self" : "offline_aftersales_other";
		String sql =
				"SELECT distributor_id FROM distribution_distributor WHERE company_id = ? AND distributor_id = ? AND offline_aftersales = 1 AND "
						+ SQL_DISTRIBUTOR_VALID
						+ " AND "
						+ flagCol
						+ " = 1 LIMIT 1";
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, companyId, addrDistId);
		if (rows.isEmpty()) {
			return null;
		}
		return longVal(rows.get(0).get("distributor_id"));
	}

	private List<Long> parseOfflineLinkedDistributorIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				long v = longVal(o);
				if (v > 0L) {
					out.add(v);
				}
			}
			return out;
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty() || "0".equals(t) || "[]".equals(t)) {
			return List.of();
		}
		try {
			JsonNode root = objectMapper.readTree(t);
			if (root != null && root.isArray()) {
				List<Long> out = new ArrayList<>();
				for (JsonNode n : root) {
					if (n == null || n.isNull()) {
						continue;
					}
					if (n.isNumber()) {
						long v = n.longValue();
						if (v > 0L) {
							out.add(v);
						}
					} else if (n.isTextual()) {
						try {
							long v = Long.parseLong(n.asText().trim());
							if (v > 0L) {
								out.add(v);
							}
						} catch (NumberFormatException ignored) {
							// skip
						}
					}
				}
				return out;
			}
		} catch (Exception ignored) {
			// fall through
		}
		String[] parts = t.split(",");
		List<Long> out = new ArrayList<>();
		for (String p : parts) {
			String s = p == null ? "" : p.trim();
			if (s.isEmpty()) {
				continue;
			}
			try {
				long v = Long.parseLong(s);
				if (v > 0L) {
					out.add(v);
				}
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private static boolean truthySetting(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
			return true;
		}
		try {
			return Integer.parseInt(s) != 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long genAftersalesBnLong() {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		int rnd = ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
		return Long.parseLong(day + rnd);
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

	private static String strOrDefault(Object o, String d) {
		String s = str(o);
		return s.isEmpty() ? d : s;
	}

	private static BigDecimal toBd(Object o) {
		if (o == null) {
			return BigDecimal.ONE;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof BigInteger bi) {
			return new BigDecimal(bi);
		}
		if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long) {
			return BigDecimal.valueOf(((Number) o).longValue());
		}
		if (o instanceof Number n) {
			String s = n.toString();
			if (s == null || s.isBlank()) {
				return BigDecimal.ONE;
			}
			try {
				return new BigDecimal(s.trim());
			} catch (NumberFormatException e) {
				return BigDecimal.ONE;
			}
		}
		try {
			return new BigDecimal(String.valueOf(o).trim());
		} catch (Exception e) {
			return BigDecimal.ONE;
		}
	}
}
