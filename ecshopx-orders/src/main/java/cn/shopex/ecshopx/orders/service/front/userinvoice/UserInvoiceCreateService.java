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

package cn.shopex.ecshopx.orders.service.front.userinvoice;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserInvoiceCreateService {

	private static final Logger log = LoggerFactory.getLogger(UserInvoiceCreateService.class);

	private static final String INVOICE_APPLY_ERR_PREFIX = "开票申请有误：";

	private static final List<String> DUPLICATE_CHECK_STATUSES =
			List.of("success", "pending", "fail", "inProgress", "inProgress");

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final MembersMapper membersMapper;
	private final AftersalesMapper aftersalesMapper;
	private final OrderInvoiceMapper orderInvoiceMapper;
	private final UserInvoiceCreateTxService userInvoiceCreateTxService;
	private final ObjectMapper objectMapper;

	public UserInvoiceCreateService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			InvoiceSettingRedisService invoiceSettingRedisService,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			MembersMapper membersMapper,
			AftersalesMapper aftersalesMapper,
			OrderInvoiceMapper orderInvoiceMapper,
			UserInvoiceCreateTxService userInvoiceCreateTxService,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.membersMapper = membersMapper;
		this.aftersalesMapper = aftersalesMapper;
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.userInvoiceCreateTxService = userInvoiceCreateTxService;
		this.objectMapper = objectMapper;
	}

	public Object createInvoice(HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		Long companyId = parseLongOrNull(auth.get("company_id"));
		Long userId = parseLongOrNull(auth.get("user_id"));
		if (companyId == null || userId == null) {
			throw new ResourceException("参数错误");
		}
		boolean hasOrderId = hasOrderId(merged);
		if (hasOrderId) {
			String orderIdStr = normalizeStr(merged.get("order_id"));
			String lockKey = "create_invoice_order_times:" + orderIdStr;
			Long n = sharedStringRedisTemplate.opsForValue().increment(lockKey);
			log.info("create_invoice lock key={} count={}", lockKey, n);
			if (n != null && n > 1) {
				throw new ResourceException("您有发票申请正在处理，请稍后再试");
			}
			sharedStringRedisTemplate.expire(lockKey, Duration.ofSeconds(10));
		}
		validateRequest(merged, hasOrderId);
		merged.put("user_id", userId);
		merged.put("company_id", companyId);

		Object rawSetting = invoiceSettingRedisService.getInvoiceSetting(companyId);
		Map<String, Object> settingMap = asSettingMap(rawSetting);
		String invoiceLimitType = resolveInvoiceLimit(settingMap, rawSetting);

		Map<String, Object> orderInfo = null;
		if (hasOrderId) {
			String orderIdStr = normalizeStr(merged.get("order_id"));
			orderInfo = getOrderInfo(companyId, userId, orderIdStr);
			List<Map<String, Object>> invoiceItems = castItemMaps(orderInfo.get("items"));
			merged.put("invoice_item", invoiceItems);
		} else {
			Object rawItem = merged.get("invoice_item");
			if (rawItem instanceof List<?>) {
				List<Map<String, Object>> list = new ArrayList<>();
				for (Object o : (List<?>) rawItem) {
					if (o instanceof Map<?, ?> om) {
						@SuppressWarnings("unchecked")
						Map<String, Object> m = (Map<String, Object>) om;
						list.add(m);
					}
				}
				merged.put("invoice_item", list);
			} else if (rawItem instanceof String s && StringUtils.hasText(s)) {
				try {
					List<Map<String, Object>> parsed =
							objectMapper.readValue(s.trim(), new TypeReference<List<Map<String, Object>>>() {});
					if (parsed == null) {
						throw new ResourceException("订单商品信息不能为空");
					}
					merged.put("invoice_item", parsed);
				} catch (Exception e) {
					throw new ResourceException("订单商品信息不能为空");
				}
			} else {
				throw new ResourceException("订单商品信息不能为空");
			}
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invoiceItems = (List<Map<String, Object>>) merged.get("invoice_item");
		if (invoiceItems == null || invoiceItems.isEmpty()) {
			throw new ResourceException("订单商品信息不能为空");
		}

		if (hasOrderId) {
			String orderIdStr = normalizeStr(merged.get("order_id"));
			long dup =
					orderInvoiceMapper.selectCount(
							new LambdaQueryWrapper<OrderInvoice>()
									.eq(OrderInvoice::getCompanyId, companyId)
									.eq(OrderInvoice::getUserId, userId)
									.eq(OrderInvoice::getOrderId, orderIdStr)
									.in(OrderInvoice::getInvoiceStatus, DUPLICATE_CHECK_STATUSES));
			if (dup > 0) {
				throw new ResourceException("您有发票申请正在处理");
			}
		}

		merged.put("invoice_source", "user");
		merged.put("invoice_method", "online");

		if (hasOrderId) {
			checkCreateInvoice(merged, orderInfo, settingMap);
			return userInvoiceCreateTxService.createInvoiceOrder(merged, orderInfo);
		}
		return userInvoiceCreateTxService.createUserInvoice(merged, invoiceLimitType);
	}

	private static boolean hasOrderId(Map<String, Object> merged) {
		return merged != null
				&& merged.containsKey("order_id")
				&& merged.get("order_id") != null
				&& !normalizeStr(merged.get("order_id")).isBlank();
	}

	private void validateRequest(Map<String, Object> merged, boolean hasOrderId) {
		List<String> errs = new ArrayList<>();
		String invType = normalizeStr(merged.get("invoice_type"));
		if (!StringUtils.hasText(invType)) {
			errs.add("开票类型不能为空");
		} else if (!"enterprise".equals(invType) && !"individual".equals(invType)) {
			errs.add("开票类型无效");
		}
		if (!StringUtils.hasText(normalizeStr(merged.get("company_title")))) {
			errs.add("发票抬头不能为空");
		}
		if ("enterprise".equals(invType)) {
			if (!StringUtils.hasText(normalizeStr(merged.get("company_tax_number")))) {
				errs.add("企业税号不能为空");
			}
		}
		boolean emailOk = StringUtils.hasText(normalizeStr(merged.get("email")));
		boolean mobileOk = StringUtils.hasText(normalizeStr(merged.get("mobile")));
		if (!emailOk && !mobileOk) {
			errs.add("请填写电子邮箱或手机号");
		}
		if (!hasOrderId) {
			Object ii = merged.get("invoice_item");
			if (ii == null) {
				errs.add("请选择需要开票的商品");
			} else if (ii instanceof String s && !StringUtils.hasText(s)) {
				errs.add("请选择需要开票的商品");
			} else if (ii instanceof List<?> l && l.isEmpty()) {
				errs.add("请选择需要开票的商品");
			}
		}
		if (!errs.isEmpty()) {
			throw new ResourceException(INVOICE_APPLY_ERR_PREFIX + errs.get(0));
		}
	}

	private static String resolveInvoiceLimit(Map<String, Object> settingMap, Object rawSetting) {
		if (!(rawSetting instanceof Map<?, ?>)) {
			return "order";
		}
		Object il = settingMap.get("invoice_limit");
		if (il == null) {
			return "order";
		}
		String s = String.valueOf(il).trim();
		return s.isEmpty() ? "order" : s;
	}

	private Map<String, Object> getOrderInfo(long companyId, long userId, String orderIdStr) {
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdStr);
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在", 500);
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderIdNum)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在", 500);
		}
		if (order.getUserId() == null || !order.getUserId().equals(userId)) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在", 500);
		}
		Map<String, Object> orderInfo = new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(order));
		orderInfo.put("order_id", orderIdStr);
		orderInfo.put("company_id", companyId);
		applySyntheticNotpayCancel(orderInfo);
		Map<String, Object> bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdStr, false);
		Object itemsObj = null;
		Object bundleOrderInfo = bundle.get("orderInfo");
		if (bundleOrderInfo instanceof Map<?, ?> boi) {
			itemsObj = boi.get("items");
		}
		List<Map<String, Object>> itemsList;
		if (itemsObj instanceof List<?>) {
			itemsList = new ArrayList<>();
			for (Object o : (List<?>) itemsObj) {
				if (o instanceof Map<?, ?> om) {
					@SuppressWarnings("unchecked")
					Map<String, Object> im = (Map<String, Object>) om;
					itemsList.add(im);
				}
			}
		} else {
			itemsList = Collections.emptyList();
		}
		orderInfo.put("items", itemsList);
		return orderInfo;
	}

	private void checkCreateInvoice(
			Map<String, Object> merged, Map<String, Object> orderInfo, Map<String, Object> settingMap) {
		if (isInvoiceClosed(settingMap)) {
			throw new ResourceException("发票申请已关闭，无法开票");
		}
		if ("CANCEL".equals(str(orderInfo.get("order_status")))) {
			throw new ResourceException("订单已取消，无法开票");
		}
		if ("WAIT_PROCESS".equals(str(orderInfo.get("cancel_status")))) {
			throw new ResourceException("订单已取消，无法开票");
		}
		Object applyType = settingMap.get("apply_type");
		if ("NOTPAY".equals(str(orderInfo.get("order_status")))
				&& Integer.valueOf(2).equals(parseIntegerLoose(applyType))) {
			throw new ResourceException("订单未支付，无法开票");
		}
		List<Map<String, Object>> items = castItemMaps(orderInfo.get("items"));
		boolean anyPositive = false;
		for (Map<String, Object> it : items) {
			int tf = intVal(it.get("total_fee"));
			int rf = intVal(it.get("refunded_fee"));
			if (tf - rf > 0) {
				anyPositive = true;
				break;
			}
		}
		if (!anyPositive) {
			throw new ResourceException("商品已经全部售后，无法开票");
		}
		String orderIdStr = str(orderInfo.get("order_id"));
		long orderIdNum = Long.parseLong(orderIdStr.trim());
		long companyId = longFromAny(merged.get("company_id"));
		long userId = longFromAny(merged.get("user_id"));
		long pendingAs =
				aftersalesMapper.selectCount(
						new LambdaQueryWrapper<Aftersales>()
								.eq(Aftersales::getCompanyId, companyId)
								.eq(Aftersales::getOrderId, orderIdNum)
								.in(Aftersales::getAftersalesStatus, List.of(0, 1)));
		if (pendingAs > 0) {
			throw new ResourceException("您有售后申请未完成，请完成后申请开票。");
		}
		List<OrderInvoice> rows =
				orderInvoiceMapper.selectList(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getOrderId, orderIdStr)
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getUserId, userId));
		int maxTry = rows.stream().mapToInt(r -> r.getTryTimes() == null ? 0 : r.getTryTimes()).max().orElse(0);
		if (maxTry >= 5) {
			throw new ResourceException("发票重试次数超过限制，无法继续开票");
		}
	}

	private static boolean isInvoiceClosed(Map<String, Object> settingMap) {
		if (settingMap == null) {
			return false;
		}
		Object v = settingMap.get("invoice_status");
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = String.valueOf(v).trim().toLowerCase();
		return "0".equals(s) || "false".equals(s);
	}

	private static Map<String, Object> asSettingMap(Object rawSetting) {
		if (rawSetting instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		}
		return new LinkedHashMap<>();
	}

	private static List<Map<String, Object>> castItemMaps(Object itemsObj) {
		if (!(itemsObj instanceof List<?> list)) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> om) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) om;
				out.add(m);
			}
		}
		return out;
	}

	private static void applySyntheticNotpayCancel(Map<String, Object> orderInfo) {
		if (!"NOTPAY".equals(str(orderInfo.get("order_status")))) {
			return;
		}
		if ("drug".equals(str(orderInfo.get("order_class")))) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		int autoCancel = intVal(orderInfo.get("auto_cancel_time"));
		if (autoCancel <= 0 || autoCancel - now > 0) {
			return;
		}
		String payType = str(orderInfo.get("pay_type"));
		String offlineSt = str(orderInfo.get("offline_payment_status"));
		boolean branch1 = !"offline_pay".equals(payType) || "-1".equals(offlineSt);
		if (branch1) {
			orderInfo.put("order_status", "CANCEL");
			return;
		}
		if ("offline_pay".equals(payType) && !"0".equals(offlineSt)) {
			orderInfo.put("order_status", "CANCEL");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String normalizeStr(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
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

	private static long longFromAny(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static Long parseLongOrNull(Object v) {
		if (v == null) {
			return null;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			String s = String.valueOf(v).trim();
			if (s.isEmpty()) {
				return null;
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseIntegerLoose(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
