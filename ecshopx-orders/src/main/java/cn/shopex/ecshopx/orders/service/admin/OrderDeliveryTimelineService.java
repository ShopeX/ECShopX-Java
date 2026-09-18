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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.OrderProcessLog;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProcessLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderDeliveryTimelineService {

	private final OrderProcessLogMapper orderProcessLogMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final ObjectMapper objectMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OrderDeliveryTimelineService(
			OrderProcessLogMapper orderProcessLogMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			ObjectMapper objectMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.orderProcessLogMapper = orderProcessLogMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.objectMapper = objectMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public List<Map<String, Object>> buildDeliveryLog(
			Map<String, Object> orderInfo, Map<String, Object> tradeInfo, Map<String, Object> dadaMap) {
		List<Map<String, Object>> result = new ArrayList<>();
		long orderId = longVal(orderInfo.get("order_id"));
		long companyId = longVal(orderInfo.get("company_id"));
		String receiptType = str(orderInfo.get("receipt_type"));

		result.add(node(intVal(orderInfo.get("create_time")), "买家已下单", 0));

		if (tradeInfo != null && timeExpireHasEffectiveValue(tradeInfo.get("time_expire"))) {
			Object payTime = tradeInfo.get("time_start");
			if (payTime != null && StringUtils.hasText(String.valueOf(payTime))) {
				int pt = parsePayEpoch(payTime);
				if (pt > 0) {
					result.add(node(pt, "买家已付款", 0));
				}
			}
		}

		if ("dada".equals(receiptType)) {
			NormalOrdersRelDada dadaRow = loadDada(companyId, orderId);
			if (dadaRow != null && dadaRow.getAcceptTime() != null && dadaRow.getAcceptTime() > 0) {
				result.add(node(dadaRow.getAcceptTime(), "商家已接单", 0));
			}
			for (OrderProcessLog log : listLogs(companyId, orderId, "同城配送", null)) {
				String enc = log.getDetail() == null ? "" : log.getDetail();
				String msg = sensitiveFieldEncryptor.decrypt(enc);
				result.add(
						node(
								log.getCreateTime() != null ? log.getCreateTime() : 0,
								msg,
								1));
			}
		} else if ("merchant".equals(receiptType)) {
			for (OrderProcessLog log : listLogs(companyId, orderId, null, Boolean.TRUE)) {
				String remEnc = log.getRemarks() == null ? "" : log.getRemarks();
				String msg = sensitiveFieldEncryptor.decrypt(remEnc);
				Map<String, Object> n =
						node(
								log.getCreateTime() != null ? log.getCreateTime() : 0,
								msg,
								1);
				n.put("delivery_remark", log.getDeliveryRemark() != null ? log.getDeliveryRemark() : "");
				n.put("pics", decodePics(log.getPics()));
				result.add(n);
			}
		} else {
			for (OrderProcessLog log : listLogs(companyId, orderId, "订单发货", null)) {
				String detEnc = log.getDetail() == null ? "" : log.getDetail();
				String detail = sensitiveFieldEncryptor.decrypt(detEnc);
				int idx = detail.indexOf("信息修改");
				String msg = (idx > 0) ? "物流信息修改" : "商家已发货";
				result.add(node(log.getCreateTime() != null ? log.getCreateTime() : 0, msg, 0));
			}
		}

		int endTime = intVal(orderInfo.get("end_time"));
		if (endTime > 0) {
			boolean dadaUndelivered =
					"dada".equals(receiptType)
							&& dadaMap != null
							&& "10".equals(String.valueOf(dadaMap.get("dada_status")));
			if (dadaUndelivered) {
				result.add(node(endTime, "未妥投", 0));
			} else {
				result.add(node(endTime, "收货人确认签收", 0));
			}
		}

		result.sort(Comparator.comparingInt(m -> intVal(m.get("time"))));
		return result;
	}

	public List<Map<String, Object>> buildMerchantSelfDeliveryTrackerEntries(long companyId, long orderId) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (OrderProcessLog log : listLogs(companyId, orderId, null, Boolean.TRUE)) {
			String remEnc = log.getRemarks() == null ? "" : log.getRemarks();
			String msg = sensitiveFieldEncryptor.decrypt(remEnc);
			Map<String, Object> n =
					node(
							log.getCreateTime() != null ? log.getCreateTime() : 0,
							msg,
							1);
			n.put("delivery_remark", log.getDeliveryRemark() != null ? log.getDeliveryRemark() : "");
			n.put("pics", decodePics(log.getPics()));
			result.add(n);
		}
		return result;
	}

	private List<OrderProcessLog> listLogs(long companyId, long orderId, String remarks, Boolean isShow) {
		LambdaQueryWrapper<OrderProcessLog> w = new LambdaQueryWrapper<>();
		w.eq(OrderProcessLog::getCompanyId, companyId)
				.eq(OrderProcessLog::getOrderId, orderId);
		if (remarks != null) {
			w.eq(OrderProcessLog::getRemarks, remarks);
		}
		if (isShow != null) {
			w.eq(OrderProcessLog::getIsShow, isShow);
		}
		w.orderByAsc(OrderProcessLog::getCreateTime).orderByDesc(OrderProcessLog::getId);
		return orderProcessLogMapper.selectList(w);
	}

	private NormalOrdersRelDada loadDada(long companyId, long orderId) {
		LambdaQueryWrapper<NormalOrdersRelDada> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrdersRelDada::getCompanyId, companyId)
				.eq(NormalOrdersRelDada::getOrderId, orderId)
				.last("LIMIT 1");
		return normalOrdersRelDadaMapper.selectOne(w);
	}

	private List<Object> decodePics(String picsJson) {
		if (picsJson == null || !StringUtils.hasText(picsJson.trim())) {
			return new ArrayList<>();
		}
		try {
			List<Object> parsed = objectMapper.readValue(picsJson, new TypeReference<List<Object>>() {});
			return parsed != null ? parsed : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static boolean timeExpireHasEffectiveValue(Object timeExpire) {
		if (timeExpire == null) {
			return false;
		}
		String s = String.valueOf(timeExpire).trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s);
	}

	private static Map<String, Object> node(int time, String msg, int level) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("time", time);
		m.put("msg", msg);
		m.put("level", level);
		m.put("delivery_remark", "");
		m.put("pics", new ArrayList<>());
		return m;
	}

	private static int parsePayEpoch(Object timeExpire) {
		String s = String.valueOf(timeExpire).trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		if (s.length() >= 14 && s.chars().allMatch(Character::isDigit)) {
			try {
				int y = Integer.parseInt(s.substring(0, 4));
				int mo = Integer.parseInt(s.substring(4, 6));
				int d = Integer.parseInt(s.substring(6, 8));
				int h = Integer.parseInt(s.substring(8, 10));
				int mi = Integer.parseInt(s.substring(10, 12));
				int se = Integer.parseInt(s.substring(12, 14));
				java.time.LocalDateTime ldt = java.time.LocalDateTime.of(y, mo, d, h, mi, se);
				return (int) ldt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
			} catch (Exception ignored) {
				return 0;
			}
		}
		try {
			long n = Long.parseLong(s);
			if (n > 9_999_999_999L) {
				n = n / 1000;
			}
			return (int) n;
		} catch (NumberFormatException e) {
			return 0;
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
		return o == null ? "" : String.valueOf(o);
	}
}
