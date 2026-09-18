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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.systemlink.WdtErpTradeCancelOrderAddAssemblePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WdtErpTradeCancelOrderAddAssemblePortImpl implements WdtErpTradeCancelOrderAddAssemblePort {

	private static final int ITEMS_LIMIT = 200;
	private static final int GROUP_MEMBER_LIMIT = 50;
	private static final DateTimeFormatter WDT_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;

	public WdtErpTradeCancelOrderAddAssemblePortImpl(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
	}

	@Override
	public List<List<Object>> assembleOrderAddBodies(
			long companyId, Map<String, Object> erpCancelPayloadSnakeCase, String wdtShopNo) {
		Map<String, Object> ctx = enrichWithPrimarySuccessTrade(companyId, erpCancelPayloadSnakeCase);
		String sourceType = str(ctx.get("trade_source_type"));
		if ("normal_groups".equals(sourceType) || "groups".equals(sourceType)) {
			return assembleGroupBodies(companyId, ctx, wdtShopNo);
		}
		if ("normal".equals(sourceType)
				|| "normal_shopguide".equals(sourceType)
				|| (StringUtils.hasText(sourceType) && sourceType.startsWith("normal_"))) {
			long orderId = longOrZero(ctx.get("order_id"));
			if (orderId <= 0) {
				return List.of();
			}
			List<Object> one = buildOrderAddTriple(companyId, orderId, ctx, wdtShopNo);
			return one == null ? List.of() : List.of(one);
		}
		return List.of();
	}

	private Map<String, Object> enrichWithPrimarySuccessTrade(
			long companyId, Map<String, Object> erpCancelPayloadSnakeCase) {
		Map<String, Object> ctx = new LinkedHashMap<>(erpCancelPayloadSnakeCase);
		long orderId = longOrZero(ctx.get("order_id"));
		if (orderId <= 0) {
			return ctx;
		}
		String cid = String.valueOf(companyId);
		String oid = String.valueOf(orderId);
		LambdaQueryWrapper<Trade> base = new LambdaQueryWrapper<>();
		base.eq(Trade::getCompanyId, cid).eq(Trade::getOrderId, oid).eq(Trade::getTradeState, "SUCCESS");
		long cnt = tradeMapper.selectCount(base);
		if (cnt <= 0) {
			return ctx;
		}
		LambdaQueryWrapper<Trade> qw = new LambdaQueryWrapper<>();
		qw.eq(Trade::getCompanyId, cid).eq(Trade::getOrderId, oid).eq(Trade::getTradeState, "SUCCESS");
		if (cnt > 1) {
			qw.ne(Trade::getPayType, "point");
		}
		qw.last("LIMIT 1");
		Trade t = tradeMapper.selectOne(qw);
		if (t == null) {
			return ctx;
		}
		ctx.put("trade_source_type", t.getTradeSourceType());
		ctx.put("transaction_id", t.getTransactionId());
		ctx.put("pay_type", t.getPayType());
		return ctx;
	}

	private List<List<Object>> assembleGroupBodies(
			long companyId, Map<String, Object> tradeRowSnakeCase, String wdtShopNo) {
		long orderId = longOrZero(tradeRowSnakeCase.get("order_id"));
		if (orderId <= 0) {
			return List.of();
		}
		NormalOrders self =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (self == null) {
			return List.of();
		}
		Long actId = self.getActId();
		if (actId == null || actId <= 0) {
			return List.of();
		}
		List<NormalOrders> members =
				normalOrdersMapper.selectList(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getActId, actId)
								.eq(NormalOrders::getOrderStatus, "PAYED")
								.last("LIMIT " + GROUP_MEMBER_LIMIT));
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		List<List<Object>> out = new ArrayList<>();
		for (NormalOrders m : members) {
			if (m.getOrderId() == null) {
				continue;
			}
			List<Object> triple = buildOrderAddTriple(companyId, m.getOrderId(), tradeRowSnakeCase, wdtShopNo);
			if (triple != null) {
				out.add(triple);
			}
		}
		return out;
	}

	private List<Object> buildOrderAddTriple(
			long companyId, long orderId, Map<String, Object> tradeRowSnakeCase, String wdtShopNo) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return null;
		}
		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.last("LIMIT " + ITEMS_LIMIT));
		if (itemRows == null || itemRows.isEmpty()) {
			return null;
		}
		long triggerOrderId = longOrZero(tradeRowSnakeCase.get("order_id"));
		String tid = String.valueOf(orderId);
		List<Map<String, Object>> orderLines = new ArrayList<>();
		double goodsCount = 0d;
		double receivableYuan = 0d;
		int i = 0;
		for (NormalOrdersItems it : itemRows) {
			int num = it.getNum() == null ? 0 : it.getNum();
			double priceYuan = fenToYuan(it.getPrice());
			double discountYuan = fenToYuan(it.getDiscountFee());
			double lineTotalYuan = priceYuan * num - discountYuan;
			if (lineTotalYuan < 0) {
				lineTotalYuan = 0;
			}
			goodsCount += num;
			receivableYuan += lineTotalYuan;
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("tid", tid);
			line.put("oid", tid + "-" + i);
			line.put("order_type", 0);
			line.put("status", 30);
			line.put("refund_status", 2);
			line.put("goods_id", it.getItemBn() != null ? it.getItemBn() : "");
			line.put("spec_id", it.getItemBn() != null ? it.getItemBn() : "");
			line.put("goods_no", it.getItemBn() != null ? it.getItemBn() : "");
			line.put("spec_no", it.getItemBn() != null ? it.getItemBn() : "");
			line.put("goods_name", it.getItemName() != null ? it.getItemName() : "");
			line.put("spec_name", "");
			line.put("num", (double) num);
			line.put("price", priceYuan);
			line.put("adjust_amount", 0d);
			line.put("refund_amount", "");
			line.put("discount", discountYuan);
			line.put("share_discount", 0d);
			line.put("total_amount", lineTotalYuan);
			line.put("cid", "");
			line.put("remark", "");
			line.put("json", "");
			orderLines.add(line);
			i++;
		}
		String payTime = formatTime(order.getCreateTime());
		String outerPayId = "";
		int payMethod = 1;
		if (orderId == triggerOrderId) {
			outerPayId = str(tradeRowSnakeCase.get("transaction_id"));
			payMethod = resolvePayMethod(str(tradeRowSnakeCase.get("pay_type")));
		}
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.put("is_auto_wms", false);
		raw.put("post_amount", 0d);
		raw.put("other_amount", 0d);
		raw.put("discount", 0d);
		raw.put("platform_cost", 0d);
		raw.put("cod_amount", "");
		raw.put("received", "");
		raw.put("tid", tid);
		raw.put("process_status", 70);
		raw.put("trade_status", 40);
		raw.put("refund_status", 2);
		raw.put("pay_status", 2);
		raw.put("order_count", orderLines.size());
		raw.put("pay_method", payMethod);
		raw.put("trade_time", payTime);
		raw.put("pay_time", payTime);
		raw.put("end_time", payTime);
		raw.put("buyer_nick", "u" + (order.getUserId() == null ? "0" : order.getUserId()));
		raw.put("buyer_message", "");
		raw.put("buyer_email", "");
		raw.put("buyer_area", "");
		raw.put("receiver_name", order.getReceiverName() != null ? order.getReceiverName() : "");
		raw.put(
				"receiver_area",
				joinArea(order.getReceiverState(), order.getReceiverCity(), order.getReceiverDistrict()));
		raw.put("receiver_address", order.getReceiverAddress() != null ? order.getReceiverAddress() : "");
		raw.put("receiver_zip", order.getReceiverZip() != null ? order.getReceiverZip() : "");
		raw.put("receiver_mobile", order.getReceiverMobile() != null ? order.getReceiverMobile() : "");
		raw.put("receiver_telno", "");
		raw.put("invoice_type", 0);
		raw.put("invoice_title", "");
		raw.put("invoice_content", "");
		raw.put("logistics_type", -1);
		raw.put("consign_interval", 0);
		raw.put("delivery_term", 1);
		raw.put("to_deliver_time", "");
		raw.put("pay_id", outerPayId);
		raw.put("pay_account", "");
		raw.put("remark", order.getRemark() != null ? order.getRemark() : "");
		raw.put("remark_flag", 0);
		raw.put("goods_count", goodsCount);
		raw.put("receivable", roundMoney(receivableYuan));
		List<Map<String, Object>> rawList = List.of(raw);
		List<Object> triple = new ArrayList<>(3);
		triple.add(wdtShopNo);
		triple.add(rawList);
		triple.add(orderLines);
		return triple;
	}

	private static int resolvePayMethod(String payType) {
		if (payType == null) {
			return 1;
		}
		String p = payType.toLowerCase();
		if (p.contains("wx")) {
			return 2;
		}
		return 1;
	}

	private static String joinArea(String state, String city, String district) {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.hasText(state)) {
			sb.append(state.trim());
		}
		if (StringUtils.hasText(city)) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(city.trim());
		}
		if (StringUtils.hasText(district)) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(district.trim());
		}
		return sb.toString();
	}

	private static String formatTime(Integer epochSec) {
		if (epochSec == null || epochSec <= 0) {
			return WDT_TIME.format(Instant.now());
		}
		return WDT_TIME.format(Instant.ofEpochSecond(epochSec.longValue()));
	}

	private static double fenToYuan(Integer fen) {
		if (fen == null) {
			return 0d;
		}
		return roundMoney(fen / 100.0d);
	}

	private static double roundMoney(double v) {
		return Math.round(v * 100.0d) / 100.0d;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
