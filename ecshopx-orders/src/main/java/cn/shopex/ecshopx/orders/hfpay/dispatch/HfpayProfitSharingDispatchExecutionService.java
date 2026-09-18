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

package cn.shopex.ecshopx.orders.hfpay.dispatch;

import cn.shopex.ecshopx.hfpay.service.profit.HfpayProfitSplitConfirmService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HfpayProfitSharingDispatchExecutionService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final OrderProfitSharingMapper orderProfitSharingMapper;
	private final OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final HfpayProfitSplitConfirmService hfpayProfitSplitConfirmService;
	private final ObjectMapper objectMapper;

	public void executeFromDispatchPayload(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		Object rawEntities = payload.get("entities");
		if (!(rawEntities instanceof Map<?, ?> rawMap)) {
			return;
		}
		long orderId = extractLong(rawMap.get("order_id"));
		List<Long> ids = extractSharingIdList(rawMap.get("order_profit_sharing_id"));
		executeProfitSharing(orderId, ids);
	}

	public void executeProfitSharing(long orderId, List<Long> orderProfitSharingIds) {
		if (orderProfitSharingIds == null || orderProfitSharingIds.isEmpty()) {
			return;
		}
		log.trace("hfpay profit sharing dispatch orderId={} ids={}", orderId, orderProfitSharingIds);
		for (Long val : orderProfitSharingIds) {
			if (val == null || val <= 0L) {
				continue;
			}
			OrderProfitSharing data = orderProfitSharingMapper.selectById(val);
			if (data == null) {
				continue;
			}
			if (data.getStatus() != null && data.getStatus() == 1) {
				continue;
			}
			long companyId = data.getCompanyId() != null ? data.getCompanyId() : 0L;
			LambdaQueryWrapper<OrderProfitSharingDetails> detW = new LambdaQueryWrapper<>();
			detW.eq(OrderProfitSharingDetails::getSharingId, val);
			List<OrderProfitSharingDetails> shareDetails = orderProfitSharingDetailsMapper.selectList(detW);
			if (shareDetails == null) {
				shareDetails = List.of();
			}

			NormalOrders order = normalOrdersMapper.selectById(data.getOrderId());
			if (order == null) {
				continue;
			}

			LambdaQueryWrapper<Trade> tradeW = new LambdaQueryWrapper<>();
			tradeW.eq(Trade::getCompanyId, String.valueOf(companyId))
					.eq(Trade::getOrderId, String.valueOf(data.getOrderId()))
					.eq(Trade::getTradeState, "SUCCESS")
					.last("LIMIT 1");
			Trade trade = tradeMapper.selectOne(tradeW);
			if (trade == null) {
				log.debug("hfpay profit sharing skip: no success trade, sharingId={}", val);
				patchSharingFailed(val, "NO_TRADE", "无成功交易单");
				continue;
			}

			List<Map<String, Object>> divDetails = new ArrayList<>();
			for (OrderProfitSharingDetails v : shareDetails) {
				int tf = v.getTotalFee() != null ? v.getTotalFee() : 0;
				if (tf < 1) {
					continue;
				}
				LinkedHashMap<String, Object> line = new LinkedHashMap<>();
				line.put("divCustId", v.getChannelId() != null ? v.getChannelId() : "");
				line.put("divAcctId", v.getChannelAcctId() != null ? v.getChannelAcctId() : "");
				BigDecimal yuan =
						BigDecimal.valueOf(tf).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
				line.put("divAmt", yuan.toPlainString());
				divDetails.add(line);
			}
			if (divDetails.isEmpty()) {
				patchSharingFailed(val, "NO_DIV", "无有效分账明细");
				continue;
			}

			String orgOrderId = trade.getTradeId() != null ? trade.getTradeId() : "";
			String orgOrderDate = ymdFromOrderCreateTime(order.getCreateTime());
			int totalFeeFen = data.getTotalFee() != null ? data.getTotalFee() : 0;
			BigDecimal transYuan =
					BigDecimal.valueOf(totalFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			String divJson;
			try {
				divJson = objectMapper.writeValueAsString(divDetails);
			} catch (JsonProcessingException e) {
				patchSharingFailed(val, "JSON", e.getMessage());
				continue;
			}

			Map<String, Object> reslut;
			try {
				reslut = hfpayProfitSplitConfirmService.pay006(
						companyId,
						orgOrderId,
						orgOrderDate,
						"27",
						transYuan.toPlainString(),
						divJson);
			} catch (Exception e) {
				log.warn("hfpay pay006 call failed, sharingId={}", val, e);
				patchSharingFailed(val, "HTTP", e.getMessage() != null ? e.getMessage() : "error");
				continue;
			}

			log.debug("汇付天下延迟分账确认接口，接口返回信息：{}", reslut);
			String code = hfpayProfitSplitConfirmService.extractRespCode(reslut);
			int st = "C00000".equals(code) ? 1 : 2;
			Object oid = reslut != null ? reslut.get("order_id") : null;
			Object odt = reslut != null ? reslut.get("order_date") : null;
			Object rd = reslut != null ? reslut.get("resp_desc") : null;
			LambdaUpdateWrapper<OrderProfitSharing> u = new LambdaUpdateWrapper<>();
			u.eq(OrderProfitSharing::getOrderProfitSharingId, val);
			u.set(OrderProfitSharing::getStatus, st);
			u.set(OrderProfitSharing::getHfOrderId, oid != null ? String.valueOf(oid) : "");
			u.set(OrderProfitSharing::getHfOrderDate, odt != null ? String.valueOf(odt) : "");
			u.set(OrderProfitSharing::getRespCode, code);
			u.set(OrderProfitSharing::getRespDesc, rd != null ? String.valueOf(rd) : "");
			orderProfitSharingMapper.update(null, u);
		}
	}

	private static long extractLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static List<Long> extractSharingIdList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		return List.of();
	}

	private void patchSharingFailed(long sharingId, String code, String desc) {
		LambdaUpdateWrapper<OrderProfitSharing> u = new LambdaUpdateWrapper<>();
		u.eq(OrderProfitSharing::getOrderProfitSharingId, sharingId);
		u.set(OrderProfitSharing::getStatus, 2);
		u.set(OrderProfitSharing::getRespCode, code);
		u.set(OrderProfitSharing::getRespDesc, desc != null ? desc : "");
		orderProfitSharingMapper.update(null, u);
	}

	private static String ymdFromOrderCreateTime(Integer createTimeSec) {
		if (createTimeSec == null || createTimeSec <= 0) {
			return YMD.format(Instant.now().atZone(CN).toLocalDate());
		}
		return YMD.format(Instant.ofEpochSecond(createTimeSec.longValue()).atZone(CN).toLocalDate());
	}
}
