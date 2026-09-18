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
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeAfterSaleSyncSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AftersalesSendbackService {

	private static final String ORDER_AFTERSALES_SENDBACK_PROCESS_LOG_REMARKS = "订单售后";

	private final AftersalesAdminDetailService aftersalesAdminDetailService;
	private final AftersalesMapper aftersalesMapper;
	private final AftersalesDetailMapper aftersalesDetailMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final AftersalesRefundAsyncPort aftersalesRefundAsyncPort;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;
	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder;
	private final WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher;
	private final WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder;
	private final TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher;

	public AftersalesSendbackService(
			AftersalesAdminDetailService aftersalesAdminDetailService,
			AftersalesMapper aftersalesMapper,
			AftersalesDetailMapper aftersalesDetailMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			ApplicationEventPublisher applicationEventPublisher,
			AftersalesRefundAsyncPort aftersalesRefundAsyncPort,
			ObjectMapper objectMapper,
			PlatformTransactionManager platformTransactionManager,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder,
			WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher,
			WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder,
			TradeAftersalesLogiDispatchPublisher tradeAftersalesLogiDispatchPublisher) {
		this.aftersalesAdminDetailService = aftersalesAdminDetailService;
		this.aftersalesMapper = aftersalesMapper;
		this.aftersalesDetailMapper = aftersalesDetailMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.aftersalesRefundAsyncPort = aftersalesRefundAsyncPort;
		this.objectMapper = objectMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.jushuitanTradeAftersalesDispatchPublisher = jushuitanTradeAftersalesDispatchPublisher;
		this.jushuitanTradeAftersalesBusPayloadBuilder = jushuitanTradeAftersalesBusPayloadBuilder;
		this.wdtErpTradeAfterSaleDispatchPublisher = wdtErpTradeAfterSaleDispatchPublisher;
		this.wdtErpTradeAfterSaleBusPayloadBuilder = wdtErpTradeAfterSaleBusPayloadBuilder;
		this.tradeAftersalesLogiDispatchPublisher = tradeAftersalesLogiDispatchPublisher;
	}

	/**
	 * Admin Open API handler for recording return logistics (invoked only from
	 * {@link cn.shopex.ecshopx.aftersales.api.admin.v1.AftersalesController#sendback}).
	 * Runs persistence inside a transaction and registers {@code afterCommit} work that reloads full aftersales
	 * detail for side effects. The first post-commit dispatch-bus step is always
	 * {@link TradeAftersalesLogiDispatchPublisher#publish(java.util.Map)} with that reloaded map; this path does not publish
	 * {@link cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent}.
	 */
	public Map<String, Object> sendback(LinkedHashMap<String, Object> merged, HttpServletRequest request) {
		mergeJwt(request, merged);
		validateSendbackParams(merged);

		long companyId = longVal(merged.get("company_id"));
		long aftersalesBn = longVal(merged.get("aftersales_bn"));

		Map<String, Object> first = aftersalesAdminDetailService.loadFullAftersalesForAdmin(companyId, aftersalesBn);

		Object typeRaw = first.get("aftersales_type");
		if ("ONLY_REFUND".equals(typeRaw == null ? null : String.valueOf(typeRaw))) {
			throw new ResourceException("不需要回寄货品");
		}
		Object progRaw = first.get("progress");
		int p;
		if (progRaw == null) {
			p = Integer.MIN_VALUE;
		} else if (progRaw instanceof Number n) {
			p = n.intValue();
		} else {
			try {
				p = Integer.parseInt(String.valueOf(progRaw).trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("售后单数据异常");
			}
		}
		if (p != 1) {
			throw new ResourceException("您已提交回寄信息，请勿重复提交");
		}

		LinkedHashMap<String, Object> sendBackData = new LinkedHashMap<>();
		sendBackData.put("corp_code", String.valueOf(merged.get("corp_code")).trim());
		sendBackData.put("logi_no", String.valueOf(merged.get("logi_no")).trim());
		sendBackData.put(
				"receiver_address",
				merged.containsKey("receiver_address") ? String.valueOf(merged.get("receiver_address")) : "");
		sendBackData.put(
				"receiver_mobile",
				merged.containsKey("receiver_mobile") ? String.valueOf(merged.get("receiver_mobile")) : "");

		final String jsonString;
		try {
			jsonString = objectMapper.writeValueAsString(sendBackData);
		} catch (JsonProcessingException e) {
			throw new ResourceException("售后单数据异常");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		final long orderIdForLog = longVal(first.get("order_id"));

		final long cid = companyId;
		final long bn = aftersalesBn;
		AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>();

		// Persistence and process-log work run inside this transaction; dispatch-bus side effects are registered below
		// and run only after commit so reloads see persisted progress and sendback fields.
		transactionTemplate.execute(
				status -> {
					LambdaUpdateWrapper<Aftersales> uw = new LambdaUpdateWrapper<>();
					uw.eq(Aftersales::getCompanyId, companyId)
							.eq(Aftersales::getAftersalesBn, aftersalesBn)
							.set(Aftersales::getProgress, 2)
							.set(Aftersales::getSendbackData, jsonString)
							.set(Aftersales::getUpdateTime, now);
					int n = aftersalesMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("售后单号为" + aftersalesBn + "的售后单不存在");
					}

					long detailCount =
							aftersalesDetailMapper.selectCount(
									new LambdaQueryWrapper<AftersalesDetail>()
											.eq(AftersalesDetail::getCompanyId, companyId)
											.eq(AftersalesDetail::getAftersalesBn, aftersalesBn));
					if (detailCount == 0L) {
						throw new ResourceException("未查询到更新数据");
					}

					LambdaUpdateWrapper<AftersalesDetail> duw = new LambdaUpdateWrapper<>();
					duw.eq(AftersalesDetail::getCompanyId, companyId)
							.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
							.set(AftersalesDetail::getProgress, 2)
							.set(AftersalesDetail::getUpdateTime, now);
					int du = aftersalesDetailMapper.update(null, duw);
					if (du == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					Map<String, Object> logMap = new LinkedHashMap<>();
					logMap.put("order_id", orderIdForLog);
					logMap.put("company_id", companyId);
					logMap.put("operator_type", str(merged.get("operator_type")));
					logMap.put("operator_id", longVal(merged.get("operator_id")));
					logMap.put("remarks", ORDER_AFTERSALES_SENDBACK_PROCESS_LOG_REMARKS);
					logMap.put("detail", "售后单号：" + aftersalesBn + "，售后单寄回商品");
					logMap.put("params", new LinkedHashMap<>(merged));
					orderProcessLogPublishPort.publish(logMap);

					TransactionSynchronizationManager.registerSynchronization(
							new TransactionSynchronization() {
								@Override
								public void afterCommit() {
									Map<String, Object> r =
											aftersalesAdminDetailService.loadFullAftersalesForAdmin(cid, bn);
									resultRef.set(r);
									publishSideEffects(cid, bn, r);
								}
							});
					return null;
				});

		Map<String, Object> result = resultRef.get();
		if (result == null) {
			throw new ResourceException("售后单数据异常");
		}
		return result;
	}

	/**
	 * Invoked after the enclosing transaction commits. Publishes trade-aftersales logistics on the unified dispatch bus,
	 * reloads the persisted aftersales row and publishes the Jushuitan trade-aftersales dispatch-bus payload, followed
	 * by Jushuitan and WDT Spring synchronization events, the WDT ERP dispatch-bus payload when a persisted row
	 * exists, then schedules the wait-confirm notice job.
	 */
	private void publishSideEffects(long companyId, long aftersalesBn, Map<String, Object> result) {
		tradeAftersalesLogiDispatchPublisher.publish(new LinkedHashMap<>(result));
		Aftersales persisted =
				aftersalesMapper.selectOne(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getAftersalesBn, aftersalesBn));
		if (persisted != null) {
			jushuitanTradeAftersalesDispatchPublisher.publish(
					jushuitanTradeAftersalesBusPayloadBuilder.build(persisted, new LinkedHashMap<>()));
		}
		applicationEventPublisher.publishEvent(
				new JushuitanTradeAftersalesSyncSpringEvent(this, new LinkedHashMap<>(result)));
		applicationEventPublisher.publishEvent(
				new WdtErpTradeAfterSaleSyncSpringEvent(this, new LinkedHashMap<>(result)));
		if (persisted != null) {
			wdtErpTradeAfterSaleDispatchPublisher.publish(wdtErpTradeAfterSaleBusPayloadBuilder.build(persisted));
		}
		aftersalesRefundAsyncPort.scheduleSendAfterSaleWaitConfirmNotice(companyId, aftersalesBn);
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

	private void validateSendbackParams(LinkedHashMap<String, Object> merged) {
		List<String> msgs = new ArrayList<>();
		if (!merged.containsKey("aftersales_bn") || isLooseEmpty(merged.get("aftersales_bn"))) {
			msgs.add("售后单号必填");
		}
		if (!merged.containsKey("company_id") || merged.get("company_id") == null || isLooseEmpty(merged.get("company_id"))) {
			msgs.add("企业ID必填");
		}
		if (!merged.containsKey("corp_code") || isLooseEmpty(merged.get("corp_code"))) {
			msgs.add("物流公司不能为空");
		}
		if (!merged.containsKey("logi_no") || isLooseEmpty(merged.get("logi_no"))) {
			msgs.add("物流单号不能为空,运单号不能小于6,运单号不能大于20");
		} else {
			String t = String.valueOf(merged.get("logi_no")).trim();
			if (t.length() < 6 || t.length() > 30) {
				msgs.add("物流单号不能为空,运单号不能小于6,运单号不能大于20");
			}
		}
		if (!msgs.isEmpty()) {
			String joined = String.join("，", msgs);
			while (joined.startsWith("，")) {
				joined = joined.substring(1);
			}
			while (joined.endsWith("，")) {
				joined = joined.substring(0, joined.length() - 1);
			}
			throw new BadRequestException(joined);
		}
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
