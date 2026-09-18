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
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.port.order.BspayOrderKey;
import cn.shopex.ecshopx.common.port.order.NormalOrderAutoCloseAftersalesCronPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.payment.AdapayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.common.port.payment.BspayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.TradeAftersalesCancelSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class AftersalesService {

	private static final int AUTO_REFUSE_PAGE_SIZE = 20;
	private static final int AUTO_CLOSE_ORDER_ITEM_PAGE_SIZE = 20;
	private static final int SCHEDULE_DONE_PAGE_SIZE = 100;
	private static final int SCHEDULE_DONE_CLOSE_DAYS = 3;
	private static final String AUTO_REFUSE_REASON = "未收到商品自动驳回";

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final AftersalesAdminDetailService aftersalesAdminDetailService;
	private final AftersalesRefundService aftersalesRefundService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort;
	private final AftersalesAutoRefuseWxaTemplatePort aftersalesAutoRefuseWxaTemplatePort;
	private final NormalOrderAutoCloseAftersalesCronPort normalOrderAutoCloseAftersalesCronPort;
	private final AdapayScheduleAutoPaymentConfirmationPort adapayScheduleAutoPaymentConfirmationPort;
	private final BspayScheduleAutoPaymentConfirmationPort bspayScheduleAutoPaymentConfirmationPort;
	private final TransactionOperations transactionOperations;
	private final CompanysMapper companysMapper;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final AftersalesCancelNoticeJobPort aftersalesCancelNoticeJobPort;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final TradeAftersalesCancelDispatchPublisher tradeAftersalesCancelDispatchPublisher;
	private final ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher
			thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;

	public AftersalesService(
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			AftersalesAdminDetailService aftersalesAdminDetailService,
			AftersalesRefundService aftersalesRefundService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort,
			AftersalesAutoRefuseWxaTemplatePort aftersalesAutoRefuseWxaTemplatePort,
			NormalOrderAutoCloseAftersalesCronPort normalOrderAutoCloseAftersalesCronPort,
			AdapayScheduleAutoPaymentConfirmationPort adapayScheduleAutoPaymentConfirmationPort,
			BspayScheduleAutoPaymentConfirmationPort bspayScheduleAutoPaymentConfirmationPort,
			TransactionOperations transactionOperations,
			CompanysMapper companysMapper,
			ApplicationEventPublisher applicationEventPublisher,
			AftersalesCancelNoticeJobPort aftersalesCancelNoticeJobPort,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			@Qualifier("tradeAftersalesCancelAsyncFanOut")
					TradeAftersalesCancelDispatchPublisher tradeAftersalesCancelDispatchPublisher,
			ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher
					thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher) {
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.aftersalesAdminDetailService = aftersalesAdminDetailService;
		this.aftersalesRefundService = aftersalesRefundService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.normalOrderLeftAftersalesWritePort = normalOrderLeftAftersalesWritePort;
		this.aftersalesAutoRefuseWxaTemplatePort = aftersalesAutoRefuseWxaTemplatePort;
		this.normalOrderAutoCloseAftersalesCronPort = normalOrderAutoCloseAftersalesCronPort;
		this.adapayScheduleAutoPaymentConfirmationPort = adapayScheduleAutoPaymentConfirmationPort;
		this.bspayScheduleAutoPaymentConfirmationPort = bspayScheduleAutoPaymentConfirmationPort;
		this.transactionOperations = transactionOperations;
		this.companysMapper = companysMapper;
		this.applicationEventPublisher = applicationEventPublisher;
		this.aftersalesCancelNoticeJobPort = aftersalesCancelNoticeJobPort;
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.tradeAftersalesCancelDispatchPublisher = tradeAftersalesCancelDispatchPublisher;
		this.thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
	}

	/**
	 * 定时：自动关闭子单可关售后，并按支付通道触发延迟分账支付确认；返回本次至少成功关单并落库的子单行数（统计维度与 handler 约定一致）。
	 */
	public int scheduleAutoCloseOrderItemAftersales() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		long totalCount = normalOrderAutoCloseAftersalesCronPort.countPendingItems(now);
		if (log.isInfoEnabled()) {
			log.info("scheduleAutoCloseOrderItemAftersales totalCount:{}", totalCount);
		}
		if (totalCount == 0L) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / (double) AUTO_CLOSE_ORDER_ITEM_PAGE_SIZE);
		int processed = 0;
		LinkedHashMap<Long, BspayOrderKey> bspayOrderIds = new LinkedHashMap<>();
		for (int page = 1; page <= totalPage; page++) {
			var rows =
					normalOrderAutoCloseAftersalesCronPort.listPendingItems(
							now, page, AUTO_CLOSE_ORDER_ITEM_PAGE_SIZE);
			for (var val : rows) {
				int u = normalOrderAutoCloseAftersalesCronPort.updateItemAftersalesClosed(val.id());
				if (u > 0) {
					processed += u;
				}
				String payType = val.payType() == null ? "" : val.payType();
				if ("adapay".equals(payType)) {
					adapayScheduleAutoPaymentConfirmationPort.scheduleAutoPaymentConfirmation(
							val.companyId(), val.orderId());
				} else if ("bspay".equals(payType)) {
					bspayOrderIds.put(val.orderId(), new BspayOrderKey(val.companyId(), val.orderId()));
				}
			}
			if (log.isInfoEnabled()) {
				log.info("scheduleAutoCloseOrderItemAftersales bspayOrderIds: {}", bspayOrderIds);
			}
			if (!bspayOrderIds.isEmpty()) {
				for (BspayOrderKey k : bspayOrderIds.values()) {
					bspayScheduleAutoPaymentConfirmationPort.scheduleAutoPaymentConfirmation(
							k.companyId(), k.orderId());
				}
			}
		}
		return processed;
	}

	/**
	 * 定时：驳回满固定天数后尝试关单；与现网一致时主路径在状态校验处失败，成功落库条数为 0。
	 */
	public int scheduleAutoDoneAftersales() {
		List<Companys> companies =
				companysMapper.selectList(new QueryWrapper<Companys>().select("company_id"));
		if (companies == null || companies.isEmpty()) {
			return 0;
		}
		int threshold =
				(int) (System.currentTimeMillis() / 1000L) - SCHEDULE_DONE_CLOSE_DAYS * 86400;
		int processed = 0;
		for (Companys c : companies) {
			Long cid = c.getCompanyId();
			if (cid == null || cid == 0L) {
				continue;
			}
			LambdaQueryWrapper<Aftersales> filter =
					new LambdaQueryWrapper<Aftersales>()
							.eq(Aftersales::getCompanyId, cid)
							.eq(Aftersales::getAftersalesStatus, 3)
							.le(Aftersales::getUpdateTime, threshold);
			long count = aftersalesMapper.selectCount(filter);
			if (count == 0L) {
				continue;
			}
			int totalPage = (int) Math.ceil(count / (double) SCHEDULE_DONE_PAGE_SIZE);
			LambdaQueryWrapper<Aftersales> pageQuery =
					new LambdaQueryWrapper<Aftersales>()
							.eq(Aftersales::getCompanyId, cid)
							.eq(Aftersales::getAftersalesStatus, 3)
							.le(Aftersales::getUpdateTime, threshold)
							.orderByAsc(Aftersales::getCreateTime);
			for (int i = 1; i <= totalPage; i++) {
				Page<Aftersales> page = new Page<>(1, SCHEDULE_DONE_PAGE_SIZE, false);
				IPage<Aftersales> result = aftersalesMapper.selectPage(page, pageQuery);
				for (Aftersales row : result.getRecords()) {
					long bn = row.getAftersalesBn() == null ? 0L : row.getAftersalesBn();
					try {
						closeAftersalesOneForSchedule(cid, bn);
						processed++;
					} catch (Exception e) {
						log.warn(
								"scheduleAutoDoneAftersales skip companyId={} aftersalesBn={} errClass={} message={}",
								cid,
								bn,
								e.getClass().getName(),
								e.getMessage());
					}
				}
			}
		}
		return processed;
	}

	/**
	 * 消费者/小程序关单：按会员维度加载主单并执行关单事务（与定时关单共用事务体）。
	 */
	public Map<String, Object> closeAftersalesForConsumer(
			long companyId,
			long aftersalesBn,
			long userIdForFilter,
			String operatorType,
			long operatorId,
			LinkedHashMap<String, Object> logParams,
			String processLogDetail,
			Object eventSource) {
		LambdaQueryWrapper<Aftersales> w =
				new LambdaQueryWrapper<Aftersales>()
						.eq(Aftersales::getCompanyId, companyId)
						.eq(Aftersales::getAftersalesBn, aftersalesBn);
		if (userIdForFilter != 0L) {
			w.eq(Aftersales::getUserId, userIdForFilter);
		}
		Aftersales main = aftersalesMapper.selectOne(w);
		if (main == null) {
			throw new ResourceException("没有售后信息");
		}
		assertCloseableConsumerAftersales(main);
		return executeCloseAftersalesTransaction(
				main,
				companyId,
				aftersalesBn,
				operatorType,
				operatorId,
				logParams,
				processLogDetail,
				eventSource);
	}

	/**
	 * 定时关单路径：不限制 user_id；成功返回表示事务已提交。
	 */
	public Map<String, Object> closeAftersalesForSchedule(
			long companyId,
			long aftersalesBn,
			Object eventSource) {
		LambdaQueryWrapper<Aftersales> w =
				new LambdaQueryWrapper<Aftersales>()
						.eq(Aftersales::getCompanyId, companyId)
						.eq(Aftersales::getAftersalesBn, aftersalesBn);
		Aftersales main = aftersalesMapper.selectOne(w);
		if (main == null) {
			throw new ResourceException("没有售后信息");
		}
		assertCloseableConsumerAftersales(main);
		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("aftersales_bn", aftersalesBn);
		return executeCloseAftersalesTransaction(
				main,
				companyId,
				aftersalesBn,
				"system",
				0L,
				logParams,
				"售后单号：" + aftersalesBn + "，到期自动关闭售后",
				eventSource);
	}

	private void closeAftersalesOneForSchedule(long companyId, long aftersalesBn) {
		closeAftersalesForSchedule(companyId, aftersalesBn, this);
	}

	private static void assertCloseableConsumerAftersales(Aftersales main) {
		Integer st = main.getAftersalesStatus();
		if (Objects.equals(st, 4)) {
			throw new ResourceException("售后已撤销， 不需要重复操作！");
		}
		if (Objects.equals(st, 3)) {
			throw new ResourceException("售后已驳回， 不需要撤销！");
		}
		if (st != null && (st == 5 || st == 1 || st == 2)) {
			throw new ResourceException("售后单已被受理,不能撤销,请联系商家处理！");
		}
	}

	private Map<String, Object> executeCloseAftersalesTransaction(
			Aftersales mainRow,
			long companyId,
			long aftersalesBn,
			String operatorType,
			long operatorId,
			LinkedHashMap<String, Object> logParams,
			String processLogDetail,
			Object eventSource) {
		final long cid = companyId;
		final long bn = aftersalesBn;
		return transactionOperations.execute(
				status -> {
					AftersalesRefund refund = aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn);
					if (refund == null) {
						throw new ResourceException("售后退款单不存在");
					}

					int nowSec = (int) (System.currentTimeMillis() / 1000L);

					UpdateWrapper<Aftersales> uw = new UpdateWrapper<Aftersales>()
							.eq("company_id", companyId)
							.eq("aftersales_bn", aftersalesBn)
							.set("progress", 7)
							.set("aftersales_status", 4)
							.set("update_time", nowSec);
					int n = aftersalesMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					UpdateWrapper<AftersalesDetail> duw = new UpdateWrapper<AftersalesDetail>()
							.eq("company_id", companyId)
							.eq("aftersales_bn", aftersalesBn)
							.set("progress", 7)
							.set("aftersales_status", 4)
							.set("update_time", nowSec);
					int du = aftersalesDetailMapper.update(null, duw);
					if (du == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					Map<String, Object> rf = new LinkedHashMap<>();
					rf.put("refund_status", "CANCEL");
					rf.put("update_time", nowSec);
					int ru = aftersalesRefundService.updateRefundByAftersalesKeys(companyId, aftersalesBn, rf);
					if (ru == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					List<AftersalesDetail> details =
							aftersalesDetailMapper.selectList(
									new LambdaQueryWrapper<AftersalesDetail>()
											.eq(AftersalesDetail::getCompanyId, companyId)
											.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
					int sum = 0;
					for (AftersalesDetail d : details) {
						sum += (d.getNum() == null ? 0 : d.getNum());
					}
					if (sum > 0 && mainRow.getOrderId() != null && mainRow.getOrderId() > 0) {
						normalOrderLeftAftersalesWritePort.addLeftAftersalesNum(companyId, mainRow.getOrderId(), sum);
					}

					Map<String, Object> logMap = new LinkedHashMap<>();
					logMap.put("order_id", mainRow.getOrderId());
					logMap.put("company_id", companyId);
					logMap.put("operator_type", operatorType);
					logMap.put("operator_id", operatorId);
					logMap.put("remarks", "订单售后");
					logMap.put("detail", processLogDetail);
					logMap.put("params", new LinkedHashMap<>(logParams));
					orderProcessLogPublishPort.publish(logMap);

					Aftersales updated =
							aftersalesMapper.selectOne(
									new LambdaQueryWrapper<Aftersales>()
											.eq(Aftersales::getCompanyId, companyId)
											.eq(Aftersales::getAftersalesBn, aftersalesBn));
					if (updated == null) {
						throw new ResourceException("没有售后信息");
					}
					final LinkedHashMap<String, Object> mainTableResultMap = mainTableToSnakeMap(updated);

					TransactionSynchronizationManager.registerSynchronization(
							new TransactionSynchronization() {
								@Override
								public void afterCommit() {
									Map<String, Object> payload = new LinkedHashMap<>(mainTableResultMap);
									applicationEventPublisher.publishEvent(
											new TradeAftersalesCancelSpringEvent(eventSource, new LinkedHashMap<>(payload)));
									tradeAftersalesCancelDispatchPublisher.publish(new LinkedHashMap<>(payload));
									thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.publish(
											new LinkedHashMap<>(payload));
									if (updated != null) {
										jushuitanTradeAftersalesDispatchPublisher.publish(
												jushuitanTradeAftersalesBusPayloadBuilder.build(
														updated, new LinkedHashMap<>()));
									}
									applicationEventPublisher.publishEvent(
											new JushuitanTradeAftersalesSyncSpringEvent(
													eventSource, new LinkedHashMap<>(payload)));
									aftersalesCancelNoticeJobPort.scheduleSendAftersaleCancelNotice(cid, bn);
								}
							});

					return mainTableResultMap;
				});
	}

	private static LinkedHashMap<String, Object> mainTableToSnakeMap(Aftersales a) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
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

	/**
	 * 定时：自动驳回超时未回寄的退货售后明细；单条独立事务，失败仅跳过该条。
	 */
	public boolean scheduleAutoRefuse() {
		int time = (int) (System.currentTimeMillis() / 1000L) + 60;
		long count = aftersalesDetailMapper.selectCount(detailAutoRefuseFilter(time));
		if (count == 0) {
			return true;
		}
		int totalPage = (int) Math.ceil(count / (double) AUTO_REFUSE_PAGE_SIZE);
		for (int page = 1; page <= totalPage; page++) {
			Page<AftersalesDetail> p = new Page<>(page, AUTO_REFUSE_PAGE_SIZE, false);
			IPage<AftersalesDetail> result =
					aftersalesDetailMapper.selectPage(p, detailAutoRefuseFilter(time));
			for (AftersalesDetail detail : result.getRecords()) {
				processOneAutoRefuseDetail(detail);
			}
		}
		return true;
	}

	private void processOneAutoRefuseDetail(AftersalesDetail detail) {
		long companyId = detail.getCompanyId() == null ? 0L : detail.getCompanyId();
		long aftersalesBn = detail.getAftersalesBn() == null ? 0L : detail.getAftersalesBn();
		Aftersales main;
		try {
			main =
					aftersalesMapper.selectOne(
							new LambdaQueryWrapper<Aftersales>()
									.eq(Aftersales::getCompanyId, companyId)
									.eq(Aftersales::getAftersalesBn, aftersalesBn));
		} catch (TooManyResultsException e) {
			return;
		}
		if (main == null) {
			return;
		}
		Map<String, Object> templateData = buildAutoRefuseTemplateData(main, detail);
		int now = (int) (System.currentTimeMillis() / 1000L);
		try {
			transactionOperations.executeWithoutResult(
					status -> {
						UpdateWrapper<Aftersales> mainUp = new UpdateWrapper<Aftersales>()
								.eq("company_id", companyId)
								.eq("aftersales_bn", aftersalesBn)
								.set("progress", 3)
								.set("aftersales_status", 3)
								.set("refuse_reason", AUTO_REFUSE_REASON)
								.set("update_time", now);
						int m = aftersalesMapper.update(null, mainUp);
						if (m == 0) {
							throw new IllegalStateException("aftersales not updated");
						}
						Map<String, Object> rf = new LinkedHashMap<>();
						rf.put("refund_status", "REFUSE");
						rf.put("update_time", now);
						int ru = aftersalesRefundService.updateRefundByAftersalesKeys(companyId, aftersalesBn, rf);
						if (ru == 0) {
							throw new IllegalStateException("aftersales_refund not updated");
						}
						UpdateWrapper<AftersalesDetail> du = new UpdateWrapper<AftersalesDetail>()
								.eq("company_id", companyId)
								.eq("aftersales_bn", aftersalesBn)
								.eq("detail_id", detail.getDetailId())
								.set("progress", 3)
								.set("aftersales_status", 3)
								.set("update_time", now);
						int d = aftersalesDetailMapper.update(null, du);
						if (d == 0) {
							throw new IllegalStateException("aftersales_detail not updated");
						}
						long orderId = main.getOrderId() == null ? 0L : main.getOrderId();
						int addNum = detail.getNum() == null ? 0 : detail.getNum();
						if (addNum > 0 && orderId > 0L) {
							normalOrderLeftAftersalesWritePort.addLeftAftersalesNum(companyId, orderId, addNum);
						}
						aftersalesAutoRefuseWxaTemplatePort.sendSellerRefuseBuyer(templateData);
						Map<String, Object> logMap = new LinkedHashMap<>();
						logMap.put("order_id", orderId);
						logMap.put("company_id", companyId);
						logMap.put("operator_type", "system");
						logMap.put("remarks", "订单售后");
						logMap.put(
								"detail",
								"售后单号："
										+ aftersalesBn
										+ " 自动驳回，驳回原因："
										+ AUTO_REFUSE_REASON);
						orderProcessLogPublishPort.publish(logMap);
					});
		} catch (Exception ignored) {
			// 单条失败不影响同页其他条
		}
	}

	/**
	 * 在持久化 UPDATE 之前，基于主表与明细的当前内存行组装自动驳回订阅消息模板数据。
	 */
	public Map<String, Object> buildAutoRefuseTemplateData(Aftersales main, AftersalesDetail detail) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("aftersales_type", main.getAftersalesType());
		m.put("aftersales_bn", main.getAftersalesBn());
		m.put("user_id", main.getUserId());
		m.put("item_name", detail.getItemName() == null ? "" : detail.getItemName());
		m.put("company_id", main.getCompanyId());
		m.put("refuse_reason", main.getRefuseReason() == null ? "" : main.getRefuseReason());
		m.put("order_id", main.getOrderId());
		m.put("refund_amount", main.getRefundFee());
		return m;
	}

	private static LambdaQueryWrapper<AftersalesDetail> detailAutoRefuseFilter(int time) {
		return new LambdaQueryWrapper<AftersalesDetail>()
				.eq(AftersalesDetail::getProgress, 1)
				.eq(AftersalesDetail::getAftersalesType, "REFUND_GOODS")
				.apply("CAST(auto_refuse_time AS UNSIGNED) > 0")
				.apply("CAST(auto_refuse_time AS UNSIGNED) < {0}", time)
				.orderByAsc(AftersalesDetail::getDetailId);
	}

	public List<Map<String, Object>> updateRemark(
			LinkedHashMap<String, Object> merged,
			HttpServletRequest request) {
		mergeJwt(request, merged);

		if (!merged.containsKey("aftersales_bn") || isLooseEmpty(merged.get("aftersales_bn"))) {
			throw new BadRequestException("售后单号必填");
		}

		long aftersalesBn = longVal(merged.get("aftersales_bn"));

		String remarkForCheck;
		if (!merged.containsKey("remark")) {
			remarkForCheck = "";
		} else {
			remarkForCheck = merged.get("remark") == null ? "" : String.valueOf(merged.get("remark"));
		}
		if (utf16CodePointCount(remarkForCheck) > 150) {
			throw new ResourceException("字数请不要超过150个！");
		}

		long companyId = longVal(merged.get("company_id"));

		List<Aftersales> rows =
				aftersalesMapper.selectList(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (rows.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}

		boolean remarkKeyPresent = merged.containsKey("remark");
		boolean writeRemark = remarkKeyPresent && merged.get("remark") != null;

		List<Map<String, Object>> result = new ArrayList<>();
		for (Aftersales row : rows) {
			if (writeRemark) {
				row.setDistributorRemark(String.valueOf(merged.get("remark")));
				int now = (int) (System.currentTimeMillis() / 1000L);
				row.setUpdateTime(now);
				aftersalesMapper.updateById(row);
				Aftersales fresh = aftersalesMapper.selectById(aftersalesBn);
				if (fresh == null) {
					throw new ResourceException("未查询到更新数据");
				}
				result.add(aftersalesAdminDetailService.mapAftersalesMainRow(fresh));
			} else {
				result.add(aftersalesAdminDetailService.mapAftersalesMainRow(row));
			}
		}
		return result;
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

	private static int utf16CodePointCount(String s) {
		return s.codePointCount(0, s.length());
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
		if (o instanceof java.util.Collection<?> c) {
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
