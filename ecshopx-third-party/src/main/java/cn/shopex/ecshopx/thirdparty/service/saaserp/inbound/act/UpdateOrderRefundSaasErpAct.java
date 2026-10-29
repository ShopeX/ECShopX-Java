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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundOutboundBodyPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundStatePort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpTradeUpdateBodyPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeRefundAddPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeUpdatePort;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({
	SaasErpRefundStatePort.class,
	SaasErpRefundOutboundBodyPort.class,
	SaasErpTradeUpdateBodyPort.class,
	SaasErpStoreTradeRefundAddPort.class,
	SaasErpStoreTradeUpdatePort.class
})
public class UpdateOrderRefundSaasErpAct implements SaasErpActHandler {

	private static final Logger log = LoggerFactory.getLogger(UpdateOrderRefundSaasErpAct.class);

	private final SaasErpRefundStatePort statePort;
	private final SaasErpRefundOutboundBodyPort outboundBodyPort;
	private final SaasErpTradeUpdateBodyPort tradeUpdateBodyPort;
	private final SaasErpStoreTradeRefundAddPort refundAddPort;
	private final SaasErpStoreTradeUpdatePort tradeUpdatePort;

	public UpdateOrderRefundSaasErpAct(
			SaasErpRefundStatePort statePort,
			SaasErpRefundOutboundBodyPort outboundBodyPort,
			SaasErpTradeUpdateBodyPort tradeUpdateBodyPort,
			SaasErpStoreTradeRefundAddPort refundAddPort,
			SaasErpStoreTradeUpdatePort tradeUpdatePort) {
		this.statePort = statePort;
		this.outboundBodyPort = outboundBodyPort;
		this.tradeUpdateBodyPort = tradeUpdateBodyPort;
		this.refundAddPort = refundAddPort;
		this.tradeUpdatePort = tradeUpdatePort;
	}

	@Override
	public String act() {
		return "ome_create_reimburse";
	}

	@Override
	public SaasErpInboundResult handle(Map<String, String> params, long companyId) {
		String orderId = params.get("order_id");
		if (orderId == null || orderId.isBlank()) {
			return SaasErpInboundResult.fail("订单号缺少");
		}
		String status = params.get("status");
		if (status == null || status.isBlank()) {
			return SaasErpInboundResult.fail("退款状态缺少");
		}
		try {
			String aftersalesBn = resolveAftersalesBn(params);
			if (aftersalesBn.isEmpty()) {
				return handleWithoutAftersales(params, companyId, status);
			}
			return handleWithAftersales(params, companyId, status, aftersalesBn);
		} catch (RuntimeException e) {
			log.warn("saaserp ome_create_reimburse failed", e);
			return SaasErpInboundResult.fail(e.getMessage());
		}
	}

	private String resolveAftersalesBn(Map<String, String> params) {
		String direct = params.get("aftersales_bn");
		if (direct != null && !direct.isBlank()) {
			return direct;
		}
		String fromMemo = aftersalesBnFromMemo(params.get("memo"));
		if (fromMemo != null) {
			return fromMemo;
		}
		if (params.containsKey("refund_bn")) {
			String fromRefundBn = nonBlank(statePort.findAftersalesBnByRefundBn(params.get("refund_bn")));
			if (fromRefundBn != null) {
				return fromRefundBn;
			}
		}
		String refundId = params.get("refund_id");
		if (refundId == null) {
			refundId = "";
		}
		String fromRefundId = nonBlank(statePort.findAftersalesBnByRefundBn(refundId));
		return fromRefundId == null ? "" : fromRefundId;
	}

	private SaasErpInboundResult handleWithoutAftersales(
			Map<String, String> params, long companyId, String status) {
		String refundId = params.get("refund_id");
		if (refundId == null) {
			refundId = "";
		}
		Optional<Map<String, Object>> refundOpt =
				statePort.findRefund(companyId, params.get("order_id"), refundId);
		if (refundOpt == null || refundOpt.isEmpty()) {
			return SaasErpInboundResult.fail("退款单详情获取失败");
		}
		Map<String, Object> refund = refundOpt.get();
		Optional<Map<String, Object>> orderOpt =
				statePort.findOrder(requiredLong(refund, "company_id"), text(refund, "order_id"));
		if (orderOpt == null || orderOpt.isEmpty()) {
			return SaasErpInboundResult.fail("未找到订单");
		}
		Map<String, Object> order = orderOpt.get();
		if ("succ".equals(status)) {
			return SaasErpInboundResult.ok("操作成功", List.of());
		}
		if ("cancel".equals(status)) {
			Object result = statePort.refuseRefundWithoutAftersales(refund, params);
			return SaasErpInboundResult.ok("操作成功", result);
		}
		if ("refund".equals(status)) {
			Object result = statePort.agreeRefundWithoutAftersales(refund, params);
			if (isTruthy(result)) {
				sendRefundAddAndTradeUpdate(params, order, null);
			}
			return SaasErpInboundResult.ok("操作成功", result);
		}
		return SaasErpInboundResult.fail("无效的操作");
	}

	private SaasErpInboundResult handleWithAftersales(
			Map<String, String> params, long companyId, String status, String aftersalesBn) {
		Optional<Map<String, Object>> afterOpt = statePort.findAftersales(companyId, aftersalesBn);
		if (afterOpt == null || afterOpt.isEmpty()) {
			return SaasErpInboundResult.fail("售后单不存在");
		}
		Map<String, Object> aftersales = afterOpt.get();
		Optional<Map<String, Object>> orderOpt =
				statePort.findOrder(requiredLong(aftersales, "company_id"), text(aftersales, "order_id"));
		if (orderOpt == null || orderOpt.isEmpty()) {
			return SaasErpInboundResult.fail("未找到订单");
		}
		Map<String, Object> order = orderOpt.get();
		if ("succ".equals(status)) {
			return SaasErpInboundResult.ok("操作成功", List.of());
		}
		Boolean agree = agreeFlag(status);
		if (agree == null) {
			return SaasErpInboundResult.fail("无效的操作");
		}
		String type = text(aftersales, "aftersales_type");
		if ("REFUND_GOODS".equals(type)) {
			Object result = statePort.confirmRefundGoods(aftersales, params, agree);
			if (agree && isTruthy(result)) {
				sendRefundGoods(params, aftersales, aftersalesBn);
			}
			return SaasErpInboundResult.ok("操作成功", result);
		}
		if ("ONLY_REFUND".equals(type)) {
			Object result = statePort.reviewOnlyRefund(aftersales, params, agree);
			if (agree && isTruthy(result)) {
				sendRefundAddAndTradeUpdate(params, order, aftersalesBn);
			}
			return SaasErpInboundResult.ok("操作成功", null);
		}
		return SaasErpInboundResult.ok("操作成功", null);
	}

	private void sendRefundAddAndTradeUpdate(
			Map<String, String> params, Map<String, Object> order, String aftersalesBn) {
		try {
			long companyId = requiredLong(order, "company_id");
			String orderId = text(order, "order_id");
			Map<String, Object> body =
					outboundBodyPort.refundAddBody(refundId(params), companyId, orderId, aftersalesBn);
			refundAddPort.callStoreTradeRefundAdd(body);
			sendTradeUpdate(companyId, orderId, text(order, "order_class"));
		} catch (RuntimeException e) {
			log.warn("saaserp ome_create_reimburse outbound failed", e);
		}
	}

	private void sendRefundGoods(
			Map<String, String> params, Map<String, Object> aftersales, String aftersalesBn) {
		try {
			Map<String, Object> body =
					outboundBodyPort.refundSendSaasErpBody(
							refundId(params),
							requiredLong(aftersales, "company_id"),
							text(aftersales, "order_id"),
							aftersalesBn);
			refundAddPort.callStoreTradeRefundAdd(body);
		} catch (RuntimeException e) {
			log.warn("saaserp ome_create_reimburse outbound failed", e);
		}
	}

	private void sendTradeUpdate(long companyId, String orderId, String orderClass) {
		Optional<Map<String, Object>> body = tradeUpdateBodyPort.tradeUpdateBody(companyId, orderId, orderClass);
		if (body != null && body.isPresent()) {
			tradeUpdatePort.callStoreTradeUpdate(body.get());
		}
	}

	private static Boolean agreeFlag(String status) {
		if ("cancel".equals(status)) {
			return Boolean.FALSE;
		}
		if ("refund".equals(status)) {
			return Boolean.TRUE;
		}
		return null;
	}

	private static String refundId(Map<String, String> params) {
		String refundId = params.get("refund_id");
		return refundId == null ? "" : refundId;
	}

	private static String aftersalesBnFromMemo(String memo) {
		if (memo == null) {
			return null;
		}
		int bar = memo.indexOf('|');
		if (bar < 0) {
			return null;
		}
		String head = memo.substring(0, bar);
		if (!head.matches("[0-9]+")) {
			return null;
		}
		return head;
	}

	private static String nonBlank(Optional<String> value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		String text = value.get();
		if (text == null || text.isBlank()) {
			return null;
		}
		return text;
	}

	private static long requiredLong(Map<String, Object> row, String key) {
		Object value = row.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			return Long.parseLong(text.trim());
		}
		throw new IllegalStateException(key);
	}

	private static String text(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : String.valueOf(value);
	}

	private static boolean isTruthy(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		if (value instanceof Number number) {
			double numeric = number.doubleValue();
			return numeric != 0.0d && !Double.isNaN(numeric);
		}
		if (value instanceof CharSequence text) {
			return text.length() > 0;
		}
		if (value instanceof Map<?, ?> map) {
			return !map.isEmpty();
		}
		if (value instanceof Collection<?> collection) {
			return !collection.isEmpty();
		}
		return true;
	}
}
