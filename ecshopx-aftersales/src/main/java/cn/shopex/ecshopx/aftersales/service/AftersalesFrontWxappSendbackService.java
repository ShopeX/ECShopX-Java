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
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesLogiDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Handles member-facing wxapp aftersales sendback: validates auth and payload, updates persisted
 * return-shipment state in a transaction, then runs {@linkplain #publishMemberSideEffects post-commit
 * side effects}. The first post-commit outbound step on the happy path is a call to
 * {@link TradeAftersalesLogiDispatchPublisher#publish} so trade-aftersales logistics listeners observe
 * the refreshed aggregate map after the transaction commits.
 */
@Service
public class AftersalesFrontWxappSendbackService {

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

	public AftersalesFrontWxappSendbackService(
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
	 * Member-facing sendback persists updates inside a transaction, then schedules the wait-confirm notice after commit
	 * through {@link AftersalesRefundAsyncPort#scheduleSendAfterSaleWaitConfirmNotice(long, long)} on the same
	 * Bus-backed port implementation merchant admin sendback uses, so both surfaces converge on one asynchronous job
	 * dispatch path post-commit.
	 */
	public Object sendback(HttpServletRequest request, LinkedHashMap<String, Object> merged) {
		Map<String, Object> auth = readAuth(request);
		merged.put(
				"user_id",
				merged.get("user_id") != null ? merged.get("user_id") : auth.get("user_id"));

		Object adRaw = merged.get("aftersales_data");
		List<?> batchList = null;
		if (merged.containsKey("aftersales_data") && adRaw != null) {
			batchList = coerceToList(adRaw);
		}

		if (batchList != null) {
			merged.remove("aftersales_data");
			if (batchList.isEmpty()) {
				return Collections.emptyList();
			}
			validateBatchSharedParams(merged);
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object vf : batchList) {
				if (!isAftersalesBnScalar(vf)) {
					throw new BadRequestException("售后单号必填");
				}
				long bn = parseLongStrict(vf, "售后单号必填");
				merged.put("aftersales_bn", bn);
				result.add(executeOneSendback(merged, auth));
			}
			return result;
		}

		validateSingleBranchParams(merged);
		merged.put("user_id", parseLongStrict(auth.get("user_id"), "用户ID必填"));
		merged.put("company_id", parseLongStrict(auth.get("company_id"), "企业ID必填"));
		return executeOneSendback(merged, auth);
	}

	private Map<String, Object> readAuth(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static List<?> coerceToList(Object raw) {
		if (raw instanceof List<?> l) {
			return l;
		}
		if (raw instanceof Object[] arr) {
			return Arrays.asList(arr);
		}
		return null;
	}

	private static boolean isAftersalesBnScalar(Object vf) {
		if (vf == null) {
			return false;
		}
		return vf instanceof String || vf instanceof Number || vf instanceof CharSequence;
	}

	private void validateSingleBranchParams(LinkedHashMap<String, Object> merged) {
		List<String> msgs = new ArrayList<>();
		if (!merged.containsKey("aftersales_bn") || isLooseEmpty(merged.get("aftersales_bn"))) {
			msgs.add("售后单号必填");
		}
		if (!merged.containsKey("company_id") || merged.get("company_id") == null || isLooseEmpty(merged.get("company_id"))) {
			msgs.add("企业ID必填");
		}
		if (!merged.containsKey("user_id") || merged.get("user_id") == null || isLooseEmpty(merged.get("user_id"))) {
			msgs.add("用户ID必填");
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
		throwIfBadRequestMsgs(msgs);
	}

	private void validateBatchSharedParams(LinkedHashMap<String, Object> merged) {
		List<String> msgs = new ArrayList<>();
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
		throwIfBadRequestMsgs(msgs);
	}

	private static void throwIfBadRequestMsgs(List<String> msgs) {
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

	private Map<String, Object> executeOneSendback(LinkedHashMap<String, Object> merged, Map<String, Object> auth) {
		Objects.requireNonNull(auth, "auth");
		long companyId = longVal(merged.get("company_id"));
		long aftersalesBn = longVal(merged.get("aftersales_bn"));
		long effectiveUserId = longVal(merged.get("user_id"));

		Map<String, Object> first =
				aftersalesAdminDetailService.loadFullAftersalesForMember(companyId, aftersalesBn, effectiveUserId);

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
		final long uid = effectiveUserId;
		AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>();

		transactionTemplate.execute(
				status -> {
					LambdaUpdateWrapper<Aftersales> uw = new LambdaUpdateWrapper<>();
					uw.eq(Aftersales::getCompanyId, companyId)
							.eq(Aftersales::getAftersalesBn, aftersalesBn)
							.eq(Aftersales::getUserId, uid)
							.set(Aftersales::getProgress, 2)
							.set(Aftersales::getSendbackData, jsonString)
							.set(Aftersales::getUpdateTime, now);
					int n = aftersalesMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("没有售后信息");
					}

					LambdaUpdateWrapper<AftersalesDetail> duw = new LambdaUpdateWrapper<>();
					duw.eq(AftersalesDetail::getCompanyId, companyId)
							.eq(AftersalesDetail::getAftersalesBn, aftersalesBn)
							.eq(AftersalesDetail::getUserId, uid)
							.set(AftersalesDetail::getProgress, 2)
							.set(AftersalesDetail::getUpdateTime, now);
					int du = aftersalesDetailMapper.update(null, duw);
					if (du == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					Map<String, Object> logMap = new LinkedHashMap<>();
					logMap.put("order_id", orderIdForLog);
					logMap.put("company_id", companyId);
					logMap.put("operator_type", "user");
					logMap.put("operator_id", uid);
					logMap.put("remarks", "订单售后");
					logMap.put("detail", wxappSendbackOrderProcessLogDetail(String.valueOf(aftersalesBn)));
					logMap.put("params", new LinkedHashMap<>(merged));
					orderProcessLogPublishPort.publish(logMap);

					TransactionSynchronizationManager.registerSynchronization(
							new TransactionSynchronization() {
								@Override
								public void afterCommit() {
									Map<String, Object> r =
											aftersalesAdminDetailService.loadFullAftersalesForMember(cid, bn, uid);
									resultRef.set(r);
									publishMemberSideEffects(cid, bn, r);
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
	 * Post-commit member-side effects: publishes the reloaded aggregate to trade-aftersales logistics
	 * consumers first, then schedules the wait-confirm notice, optional third-party ERP publishers, and
	 * the Jushuitan sync notification.
	 *
	 * <p>The opening call is {@link TradeAftersalesLogiDispatchPublisher#publish(java.util.Map)} with a
	 * defensive copy of {@code result}. That port is the sole supported entry for broadcasting the same
	 * aggregate to bus-registered logistics listeners (including the SaaS ERP arm) using asynchronous
	 * Redis dispatch and listener-level queue metadata. Do not substitute {@code ApplicationEventPublisher}
	 * plus {@code SaasErpAftersalesSpringEvent} for that workflow; those types serve other integrations
	 * and would not honor the dispatch registry contract exercised by {@code TradeAftersalesLogiDispatchPublisher}.
	 *
	 * @param companyId tenant identifier passed to downstream ports
	 * @param aftersalesBn aftersales document number
	 * @param result aggregate view reloaded after commit; copied before {@code publish} and before other effects
	 */
	private void publishMemberSideEffects(long companyId, long aftersalesBn, Map<String, Object> result) {
		tradeAftersalesLogiDispatchPublisher.publish(new LinkedHashMap<>(result));
		aftersalesRefundAsyncPort.scheduleSendAfterSaleWaitConfirmNotice(companyId, aftersalesBn);
		long memberUserId = longVal(result.get("user_id"));
		LambdaQueryWrapper<Aftersales> persistedQ = new LambdaQueryWrapper<>();
		persistedQ.eq(Aftersales::getCompanyId, companyId)
				.eq(Aftersales::getAftersalesBn, aftersalesBn)
				.eq(Aftersales::getUserId, memberUserId);
		Aftersales persisted = aftersalesMapper.selectOne(persistedQ);
		if (persisted != null) {
			jushuitanTradeAftersalesDispatchPublisher.publish(
					jushuitanTradeAftersalesBusPayloadBuilder.build(persisted, new LinkedHashMap<>()));
		}
		applicationEventPublisher.publishEvent(
				new JushuitanTradeAftersalesSyncSpringEvent(this, new LinkedHashMap<>(result)));
		if (persisted != null) {
			wdtErpTradeAfterSaleDispatchPublisher.publish(
					wdtErpTradeAfterSaleBusPayloadBuilder.build(persisted));
		}
	}

	private static String wxappSendbackOrderProcessLogDetail(String aftersalesBn) {
		return "售后单号：" + aftersalesBn + "，售后单寄回商品";
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

	private static long parseLongStrict(Object o, String absentMessage) {
		if (o == null) {
			throw new BadRequestException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(absentMessage);
		}
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
}
