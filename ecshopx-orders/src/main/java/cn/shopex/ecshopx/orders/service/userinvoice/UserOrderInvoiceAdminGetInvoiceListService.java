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

package cn.shopex.ecshopx.orders.service.userinvoice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.UserOrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.repository.UserOrderInvoiceRepository;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserOrderInvoiceAdminGetInvoiceListService {

	private static final Logger log = LoggerFactory.getLogger(UserOrderInvoiceAdminGetInvoiceListService.class);

	private static final String ORDER_NOT_FOUND_MARKER = "订单不存在";

	private final UserOrderInvoiceRepository userOrderInvoiceRepository;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final ObjectMapper objectMapper;

	public UserOrderInvoiceAdminGetInvoiceListService(
			UserOrderInvoiceRepository userOrderInvoiceRepository,
			OrderAssociationsMapper orderAssociationsMapper,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			ObjectMapper objectMapper) {
		this.userOrderInvoiceRepository = userOrderInvoiceRepository;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getInvoiceList(
			long companyId,
			Long filterUserId,
			Long filterId,
			String filterOrderId,
			int filterStatus,
			int page,
			int pageSize) {
		Integer statusForRepo = filterStatus != 0 ? Integer.valueOf(filterStatus) : null;
		Page<UserOrderInvoice> pg =
				userOrderInvoiceRepository.lists(
						companyId, filterUserId, filterId, filterOrderId, statusForRepo, page, pageSize);
		long totalCount = pg.getTotal();
		List<Map<String, Object>> outList = new ArrayList<>();

		for (UserOrderInvoice v : pg.getRecords()) {
			Map<String, Object> invoiceArr = parseInvoiceJsonForList(v.getInvoice());
			Map<String, Object> vInfo = invoiceArr;

			Object orderInfoForPayload = resolveOrderInfoForRow(companyId, v.getOrderId());

			invoiceArr.put("orderInfo", orderInfoForPayload);
			invoiceArr.put("id", v.getId());
			invoiceArr.put("order_id", v.getOrderId());
			invoiceArr.put("user_id", v.getUserId());
			invoiceArr.put("status", v.getStatus());
			invoiceArr.put("type_hz", "电子发票");
			invoiceArr.put("tax_rate", "0.13");
			invoiceArr.put("user_name", "李健");
			invoiceArr.put(
					"download_url",
					"https://erp-test-1300056727.cos.ap-chengdu.myqcloud.com/pc/JPG/15b9aecdf66b4f4fb40e36784f6eb3db-_MG_9228.JPG");

			long longCents = totalFeeCentsFromOrderInfoPayload(orderInfoForPayload);
			String amountStr =
					new BigDecimal(longCents)
							.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
							.toPlainString();
			invoiceArr.put("amount", amountStr);

			if (vInfo.containsKey("fapiaoinfo")) {
				Object fapiaoinfoRaw = vInfo.get("fapiaoinfo");
				if (fapiaoinfoRaw != null
						&& (fapiaoinfoRaw instanceof List<?> || fapiaoinfoRaw instanceof Map<?, ?>)) {
					BigDecimal sum = sumFapiaoLines(fapiaoinfoRaw);
					invoiceArr.put(
							"amount",
							sum.divide(BigDecimal.ONE, 2, RoundingMode.HALF_UP).toPlainString());
					log.debug("fapiaoinfo amount aggregate invoiceId={} amount={}", v.getId(), invoiceArr.get("amount"));
				}
			}

			outList.add(invoiceArr);
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("list", outList);
		payload.put("total_count", totalCount);
		return payload;
	}

	private Object resolveOrderInfoForRow(long companyId, String orderIdRaw) {
		if (orderIdRaw == null || orderIdRaw.isBlank()) {
			return Boolean.FALSE;
		}
		String trimmed = orderIdRaw.trim();
		long orderIdForDb;
		try {
			orderIdForDb = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return Boolean.FALSE;
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdForDb)
								.last("LIMIT 1"));
		if (assoc == null) {
			return Boolean.FALSE;
		}

		try {
			Map<String, Object> bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, trimmed, false);
			Object rawOi = bundle.get("orderInfo");
			if (rawOi instanceof Map<?, ?> m) {
				return deepCopyToMutableMap(m);
			}
			return new LinkedHashMap<String, Object>();
		} catch (ResourceException e) {
			String msg = e.getMessage();
			if (msg != null && msg.contains(ORDER_NOT_FOUND_MARKER)) {
				return new LinkedHashMap<String, Object>();
			}
			throw e;
		}
	}

	private Map<String, Object> parseInvoiceJsonForList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (JsonProcessingException e) {
			log.warn("invoice json parse failed: {}", e.getMessage());
			return new LinkedHashMap<>();
		}
	}

	private static LinkedHashMap<String, Object> deepCopyToMutableMap(Map<?, ?> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			out.put(String.valueOf(e.getKey()), deepCopyValue(e.getValue()));
		}
		return out;
	}

	private static Object deepCopyValue(Object v) {
		if (v instanceof Map<?, ?> m) {
			return deepCopyToMutableMap(m);
		}
		if (v instanceof Collection<?> c) {
			List<Object> nl = new ArrayList<>();
			for (Object x : c) {
				nl.add(deepCopyValue(x));
			}
			return nl;
		}
		return v;
	}

	private static long totalFeeCentsFromOrderInfoPayload(Object orderInfoForPayload) {
		if (orderInfoForPayload == Boolean.FALSE || !(orderInfoForPayload instanceof Map<?, ?>)) {
			return 0L;
		}
		Object tfRaw = ((Map<?, ?>) orderInfoForPayload).get("total_fee");
		return parseCentsLong(tfRaw);
	}

	private static long parseCentsLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static BigDecimal sumFapiaoLines(Object fapiaoinfoRaw) {
		BigDecimal sum = BigDecimal.ZERO;
		if (fapiaoinfoRaw instanceof List<?> list) {
			for (Object vf : list) {
				sum = sum.add(lineAmount(vf));
			}
		} else if (fapiaoinfoRaw instanceof Map<?, ?> map) {
			for (Object vf : map.values()) {
				sum = sum.add(lineAmount(vf));
			}
		}
		return sum;
	}

	private static BigDecimal lineAmount(Object vf) {
		if (!(vf instanceof Map<?, ?> row)) {
			return BigDecimal.ZERO;
		}
		BigDecimal price = toBigDecimal(row.get("price"));
		BigDecimal num = toBigDecimal(row.get("num"));
		return price.multiply(num);
	}

	private static BigDecimal toBigDecimal(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		if (o instanceof BigDecimal bd) {
			return bd;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			String s = String.valueOf(o).trim();
			if (s.isEmpty()) {
				return BigDecimal.ZERO;
			}
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}
}
