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

import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.common.dispatch.InvoicePushOmsJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceApiRowSupport;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserInvoiceCreateTxService {

	private static final String FREIGHT_ITEM_BN = "shippingFeeLine";

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final OrderInvoiceItemMapper orderInvoiceItemMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final MembersMapper membersMapper;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final ObjectMapper objectMapper;
	private final InvoicePushOmsJobDispatchPublisher invoicePushOmsJobDispatchPublisher;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public UserInvoiceCreateTxService(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceItemMapper orderInvoiceItemMapper,
			OrderInvoiceLogMapper orderInvoiceLogMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			MembersMapper membersMapper,
			InvoiceSettingRedisService invoiceSettingRedisService,
			ObjectMapper objectMapper,
			InvoicePushOmsJobDispatchPublisher invoicePushOmsJobDispatchPublisher,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceItemMapper = orderInvoiceItemMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.membersMapper = membersMapper;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.objectMapper = objectMapper;
		this.invoicePushOmsJobDispatchPublisher = invoicePushOmsJobDispatchPublisher;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createInvoiceOrder(Map<String, Object> data, Map<String, Object> orderInfo) {
		try {
			long companyId = longFromAny(data.get("company_id"));
			Map<String, Object> settingMap = asSettingMap(invoiceSettingRedisService.getInvoiceSetting(companyId));
			mergeInvoiceMethodFromSetting(data, settingMap);
			String orderIdStr = str(orderInfo.get("order_id"));
			List<Map<String, Object>> lines = pickInvoiceLines(castItemMaps(orderInfo.get("items")), null);
			if (lines.isEmpty()) {
				throw new ResourceException("商品已经全部售后，无法开票");
			}
			OrderInvoice inv =
					insertInvoiceWithLines(data, orderInfo, settingMap, orderIdStr, lines, sumFreight(orderInfo, settingMap), false);
			return OrderInvoiceApiRowSupport.toColumnNamesData(orderInvoiceMapper.selectById(inv.getId()));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("生成发票失败:" + e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> createUserInvoice(Map<String, Object> data, String invoiceLimitType) {
		try {
			long companyId = longFromAny(data.get("company_id"));
			long userId = longFromAny(data.get("user_id"));
			Map<String, Object> settingMap = asSettingMap(invoiceSettingRedisService.getInvoiceSetting(companyId));
			mergeInvoiceMethodFromSetting(data, settingMap);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> invoiceItems = (List<Map<String, Object>>) data.get("invoice_item");
			if (invoiceItems == null) {
				invoiceItems = Collections.emptyList();
			}
			String limit = invoiceLimitType == null ? "order" : invoiceLimitType.trim().toLowerCase();
			if ("order".equals(limit)) {
				List<Map<String, Object>> results = new ArrayList<>();
				Set<String> orderKeys = new LinkedHashSet<>();
				for (Map<String, Object> m : invoiceItems) {
					String oid = normalizeStr(m.get("order_id"));
					if (StringUtils.hasText(oid)) {
						orderKeys.add(oid);
					}
				}
				for (String orderIdStr : orderKeys) {
					Map<String, Object> orderInfo = loadOrderInfoForTx(companyId, userId, orderIdStr);
					Set<String> wantedIds = wantedLineIdsForOrder(invoiceItems, orderIdStr);
					List<Map<String, Object>> lines =
							pickInvoiceLines(castItemMaps(orderInfo.get("items")), wantedIds.isEmpty() ? null : wantedIds);
					if (lines.isEmpty()) {
						continue;
					}
					OrderInvoice inv =
							insertInvoiceWithLines(data, orderInfo, settingMap, orderIdStr, lines, sumFreight(orderInfo, settingMap), true);
					results.add(OrderInvoiceApiRowSupport.toColumnNamesData(orderInvoiceMapper.selectById(inv.getId())));
				}
				if (results.isEmpty()) {
					throw new ResourceException("订单商品信息不能为空");
				}
				return results;
			}
			if ("item".equals(limit)) {
				List<Map<String, Object>> allLines = new ArrayList<>();
				Set<String> orderKeys = new LinkedHashSet<>();
				for (Map<String, Object> m : invoiceItems) {
					String oid = normalizeStr(m.get("order_id"));
					if (StringUtils.hasText(oid)) {
						orderKeys.add(oid);
					}
				}
				int totalFreight = 0;
				for (String orderIdStr : orderKeys) {
					Map<String, Object> orderInfo = loadOrderInfoForTx(companyId, userId, orderIdStr);
					Set<String> wantedIds = wantedLineIdsForOrder(invoiceItems, orderIdStr);
					List<Map<String, Object>> lines =
							pickInvoiceLines(castItemMaps(orderInfo.get("items")), wantedIds.isEmpty() ? null : wantedIds);
					for (Map<String, Object> ln : lines) {
						LinkedHashMap<String, Object> copy = new LinkedHashMap<>(ln);
						copy.put("order_id", orderIdStr);
						allLines.add(copy);
					}
					totalFreight += intVal(orderInfo.get("freight_fee"));
				}
				if (allLines.isEmpty()) {
					throw new ResourceException("订单商品信息不能为空");
				}
				String compoundOrderIds = orderKeys.stream().sorted().collect(Collectors.joining(","));
				Map<String, Object> syntheticOrderInfo = new LinkedHashMap<>();
				syntheticOrderInfo.put("order_id", compoundOrderIds);
				syntheticOrderInfo.put("freight_fee", totalFreight);
				syntheticOrderInfo.put("company_id", companyId);
				OrderInvoice inv =
						insertInvoiceWithLinesForMulti(
								data, syntheticOrderInfo, settingMap, compoundOrderIds, allLines, totalFreight, true);
				return List.of(OrderInvoiceApiRowSupport.toColumnNamesData(orderInvoiceMapper.selectById(inv.getId())));
			}
			throw new ResourceException("参数错误");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("开票申请失败：" + e.getMessage());
		}
	}

	private OrderInvoice insertInvoiceWithLines(
			Map<String, Object> data,
			Map<String, Object> orderInfo,
			Map<String, Object> settingMap,
			String orderIdStr,
			List<Map<String, Object>> lines,
			int freightFeeForLine,
			boolean fireOmsEvent)
			throws JsonProcessingException {
		long userId = longFromAny(data.get("user_id"));
		long companyId = longFromAny(data.get("company_id"));
		long orderIdNum = Long.parseLong(orderIdStr.trim());
		Members member = membersMapper.selectById(userId);
		String bn = nextUserInvoiceApplyBn(companyId);
		OrderInvoice inv = buildInvoiceHeader(data, orderInfo, orderIdStr, member, bn);
		orderInvoiceMapper.insert(inv);
		Long invoiceId = inv.getId();
		if (invoiceId == null) {
			throw new ResourceException("生成发票失败:未获得发票主键");
		}
		int nowSec = (int) (System.currentTimeMillis() / 1000);
		int totalAmount = 0;
		for (Map<String, Object> line : lines) {
			OrderInvoiceItem row = buildGoodsLine(invoiceId, bn, companyId, userId, orderIdStr, line, nowSec);
			orderInvoiceItemMapper.insert(row);
			totalAmount += row.getAmount() == null ? 0 : row.getAmount();
		}
		if (freightFeeForLine > 0 && isFreightInvoiceSeparate(settingMap)) {
			OrderInvoiceItem freight =
					buildFreightLine(invoiceId, bn, companyId, userId, orderIdStr, freightFeeForLine, settingMap, nowSec);
			orderInvoiceItemMapper.insert(freight);
			totalAmount += freight.getAmount() == null ? 0 : freight.getAmount();
		}
		LambdaUpdateWrapper<OrderInvoice> iu =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.set(OrderInvoice::getInvoiceAmount, totalAmount)
						.set(OrderInvoice::getUpdateTime, nowSec);
		orderInvoiceMapper.update(null, iu);
		for (Map<String, Object> line : lines) {
			Long rowId = longOrNull(line.get("id"));
			if (rowId != null) {
				normalOrdersItemsMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getId, rowId)
								.eq(NormalOrdersItems::getOrderId, orderIdNum)
								.set(NormalOrdersItems::getIsInvoice, 1)
								.set(NormalOrdersItems::getUpdateTime, nowSec));
			}
		}
		normalOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getOrderId, orderIdNum)
						.eq(NormalOrders::getCompanyId, companyId)
						.set(NormalOrders::getInvoiceStatus, "pending")
						.set(NormalOrders::getUpdateTime, nowSec));
		insertUserLog(invoiceId, userId, data, orderInfo, nowSec);
		if (fireOmsEvent) {
			invoicePushOmsJobDispatchPublisher.publish(invoiceId, companyId);
		}
		return inv;
	}

	private OrderInvoice insertInvoiceWithLinesForMulti(
			Map<String, Object> data,
			Map<String, Object> syntheticOrderInfo,
			Map<String, Object> settingMap,
			String compoundOrderIds,
			List<Map<String, Object>> lines,
			int totalFreight,
			boolean fireOmsEvent)
			throws JsonProcessingException {
		long userId = longFromAny(data.get("user_id"));
		long companyId = longFromAny(data.get("company_id"));
		Members member = membersMapper.selectById(userId);
		String bn = nextUserInvoiceApplyBn(companyId);
		OrderInvoice inv = buildInvoiceHeader(data, syntheticOrderInfo, compoundOrderIds, member, bn);
		orderInvoiceMapper.insert(inv);
		Long invoiceId = inv.getId();
		if (invoiceId == null) {
			throw new ResourceException("生成发票失败:未获得发票主键");
		}
		int nowSec = (int) (System.currentTimeMillis() / 1000);
		int totalAmount = 0;
		Set<Long> touchedOrders = new LinkedHashSet<>();
		for (Map<String, Object> line : lines) {
			String lineOrderId = normalizeStr(line.get("order_id"));
			if (!StringUtils.hasText(lineOrderId)) {
				lineOrderId = compoundOrderIds.split(",", 2)[0];
			}
			long onum = Long.parseLong(lineOrderId.trim());
			touchedOrders.add(onum);
			OrderInvoiceItem row = buildGoodsLine(invoiceId, bn, companyId, userId, lineOrderId, line, nowSec);
			orderInvoiceItemMapper.insert(row);
			totalAmount += row.getAmount() == null ? 0 : row.getAmount();
			Long rowId = longOrNull(line.get("id"));
			if (rowId != null) {
				normalOrdersItemsMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getId, rowId)
								.eq(NormalOrdersItems::getOrderId, onum)
								.set(NormalOrdersItems::getIsInvoice, 1)
								.set(NormalOrdersItems::getUpdateTime, nowSec));
			}
		}
		if (totalFreight > 0 && isFreightInvoiceSeparate(settingMap)) {
			OrderInvoiceItem freight =
					buildFreightLine(
							invoiceId, bn, companyId, userId, compoundOrderIds, totalFreight, settingMap, nowSec);
			orderInvoiceItemMapper.insert(freight);
			totalAmount += freight.getAmount() == null ? 0 : freight.getAmount();
		}
		LambdaUpdateWrapper<OrderInvoice> iu =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.set(OrderInvoice::getInvoiceAmount, totalAmount)
						.set(OrderInvoice::getUpdateTime, nowSec);
		orderInvoiceMapper.update(null, iu);
		for (Long onum : touchedOrders) {
			normalOrdersMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrders>()
							.eq(NormalOrders::getOrderId, onum)
							.eq(NormalOrders::getCompanyId, companyId)
							.set(NormalOrders::getInvoiceStatus, "pending")
							.set(NormalOrders::getUpdateTime, nowSec));
		}
		insertUserLog(invoiceId, userId, data, syntheticOrderInfo, nowSec);
		if (fireOmsEvent) {
			invoicePushOmsJobDispatchPublisher.publish(invoiceId, companyId);
		}
		return inv;
	}

	private Map<String, Object> loadOrderInfoForTx(long companyId, long userId, String orderIdStr) {
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdStr.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在");
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderIdNum)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在");
		}
		if (order.getUserId() == null || !order.getUserId().equals(userId)) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在");
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

	private void mergeInvoiceMethodFromSetting(Map<String, Object> data, Map<String, Object> settingMap) {
		if (settingMap == null) {
			return;
		}
		Object im = settingMap.get("invoice_method");
		if (im != null) {
			String s = String.valueOf(im).trim();
			if (StringUtils.hasText(s)) {
				data.put("invoice_method", s);
			}
		}
	}

	private OrderInvoice buildInvoiceHeader(
			Map<String, Object> data,
			Map<String, Object> orderInfo,
			String orderIdStr,
			Members member,
			String bn) {
		long userId = longFromAny(data.get("user_id"));
		long companyId = longFromAny(data.get("company_id"));
		int nowSec = (int) (System.currentTimeMillis() / 1000);
		OrderInvoice inv = new OrderInvoice();
		inv.setInvoiceApplyBn(bn);
		inv.setUserId(userId);
		inv.setCompanyId(companyId);
		inv.setOrderId(orderIdStr);
		inv.setInvoiceType(str(data.get("invoice_type")));
		inv.setInvoiceTypeCode(str(data.get("invoice_type_code")));
		inv.setCompanyTitle(str(data.get("company_title")));
		inv.setCompanyTaxNumber(emptyToNull(str(data.get("company_tax_number"))));
		inv.setCompanyAddress(emptyToNull(str(data.get("company_address"))));
		inv.setCompanyTelephone(emptyToNull(str(data.get("company_telephone"))));
		inv.setBankName(emptyToNull(str(data.get("bank_name"))));
		inv.setBankAccount(emptyToNull(str(data.get("bank_account"))));
		inv.setEmail(emptyToNull(str(data.get("email"))));
		inv.setMobile(emptyToNull(str(data.get("mobile"))));
		inv.setInvoiceSource(str(data.get("invoice_source")));
		inv.setInvoiceMethod(str(data.get("invoice_method")));
		inv.setRemark(emptyToNull(str(data.get("remark"))));
		inv.setInvoiceStatus("pending");
		inv.setTryTimes(0);
		inv.setInvoiceAmount(0);
		inv.setIsOms(0);
		inv.setCreateTime(nowSec);
		inv.setUpdateTime(nowSec);
		if (orderInfo != null) {
			int endTime = intVal(orderInfo.get("end_time"));
			if (endTime > 0) {
				inv.setEndTime(endTime);
				inv.setCloseAftersalesTime(intVal(orderInfo.get("order_auto_close_aftersales_time")));
			}
			Object distributorId = orderInfo.get("distributor_id");
			inv.setOrderShopId(distributorId == null ? "" : String.valueOf(distributorId));
		}
		if (member != null) {
			inv.setUserCardCode(member.getUserCardCode());
		}
		return inv;
	}

	private OrderInvoiceItem buildGoodsLine(
			long invoiceId,
			String bn,
			long companyId,
			long userId,
			String orderIdStr,
			Map<String, Object> line,
			int nowSec) {
		int total = intVal(line.get("total_fee"));
		int refunded = intVal(line.get("refunded_fee"));
		int amount = Math.max(0, total - refunded);
		int num = intVal(line.get("num"));
		OrderInvoiceItem row = new OrderInvoiceItem();
		row.setInvoiceId(invoiceId);
		row.setInvoiceApplyBn(bn);
		row.setUserId(userId);
		row.setCompanyId(companyId);
		row.setOrderId(orderIdStr);
		row.setOid(line.get("id") == null ? null : String.valueOf(line.get("id")));
		row.setItemName(str(line.get("item_name")));
		row.setItemBn(str(line.get("item_bn")));
		row.setMainImg(emptyToNull(str(line.get("pic"))));
		row.setSpecInfo(emptyToNull(str(line.get("spec_info"))));
		row.setItemSpecDesc(emptyToNull(str(line.get("item_spec_desc"))));
		row.setNum(num);
		row.setAmount(amount);
		row.setInvoiceTaxRate(formatTaxRate(line.get("tax_rate")));
		row.setOriginalNum(num);
		row.setOriginalAmount(total);
		row.setCreateTime(nowSec);
		row.setUpdateTime(nowSec);
		return row;
	}

	private OrderInvoiceItem buildFreightLine(
			long invoiceId,
			String bn,
			long companyId,
			long userId,
			String orderIdStr,
			int freightFee,
			Map<String, Object> settingMap,
			int nowSec) {
		String freightName = str(settingMap.get("freight_name"));
		if (!StringUtils.hasText(freightName)) {
			freightName = "运费";
		}
		OrderInvoiceItem row = new OrderInvoiceItem();
		row.setInvoiceId(invoiceId);
		row.setInvoiceApplyBn(bn);
		row.setUserId(userId);
		row.setCompanyId(companyId);
		row.setOrderId(orderIdStr);
		row.setOid(null);
		row.setItemName(freightName);
		row.setItemBn(FREIGHT_ITEM_BN);
		row.setMainImg(null);
		row.setSpecInfo(null);
		row.setItemSpecDesc(null);
		row.setNum(1);
		row.setAmount(Math.max(0, freightFee));
		row.setInvoiceTaxRate(formatTaxRate(settingMap.get("freight_tax_rate")));
		row.setOriginalNum(1);
		row.setOriginalAmount(Math.max(0, freightFee));
		row.setCreateTime(nowSec);
		row.setUpdateTime(nowSec);
		return row;
	}

	private void insertUserLog(long invoiceId, long userId, Map<String, Object> data, Map<String, Object> orderInfo, int nowSec)
			throws JsonProcessingException {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("title", "用户申请发票");
		payload.put("remark", "用户申请发票");
		payload.put("params", Collections.emptyList());
		payload.put("data", data);
		payload.put("order_id", orderInfo.get("order_id"));
		String content = objectMapper.writeValueAsString(payload);
		OrderInvoiceLog logRow = new OrderInvoiceLog();
		logRow.setInvoiceId(invoiceId);
		logRow.setOperatorType("user");
		logRow.setUserId(userId);
		logRow.setOperatorContent(content);
		logRow.setCreateTime(nowSec);
		logRow.setUpdateTime(nowSec);
		orderInvoiceLogMapper.insert(logRow);
	}

	private static int sumFreight(Map<String, Object> orderInfo, Map<String, Object> settingMap) {
		if (!isFreightInvoiceSeparate(settingMap)) {
			return 0;
		}
		return Math.max(0, intVal(orderInfo.get("freight_fee")));
	}

	private static boolean isFreightInvoiceSeparate(Map<String, Object> settingMap) {
		return settingMap != null && "2".equals(str(settingMap.get("freight_invoice")).trim());
	}

	private static String formatTaxRate(Object raw) {
		if (raw == null) {
			return "0%";
		}
		String s = String.valueOf(raw).trim();
		if (s.endsWith("%")) {
			return s;
		}
		int p = intVal(raw);
		if (p <= 0) {
			return "0%";
		}
		if (p <= 100) {
			return p + "%";
		}
		return (p / 100) + "." + String.format(Locale.US, "%02d", p % 100) + "%";
	}

	private String nextUserInvoiceApplyBn(long companyId) {
		return "I"
				+ companyId
				+ String.format(Locale.US, "%013d", System.currentTimeMillis() % 10_000_000_000_000L)
				+ String.format(Locale.US, "%04d", ThreadLocalRandom.current().nextInt(10_000));
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

	private static List<Map<String, Object>> pickInvoiceLines(List<Map<String, Object>> all, Set<String> allowedLineIds) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> line : all) {
			if (allowedLineIds != null && !allowedLineIds.contains(String.valueOf(line.get("id")))) {
				continue;
			}
			int net = intVal(line.get("total_fee")) - intVal(line.get("refunded_fee"));
			if (net > 0) {
				out.add(line);
			}
		}
		return out;
	}

	private static Set<String> wantedLineIdsForOrder(List<Map<String, Object>> invoiceItems, String orderIdStr) {
		Set<String> ids = new LinkedHashSet<>();
		for (Map<String, Object> m : invoiceItems) {
			if (!orderIdStr.equals(normalizeStr(m.get("order_id")))) {
				continue;
			}
			Object id = m.get("id");
			if (id != null) {
				ids.add(String.valueOf(id));
			}
		}
		return ids;
	}

	private static String normalizeStr(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String emptyToNull(String s) {
		return StringUtils.hasText(s) ? s : null;
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

	private static Long longOrNull(Object v) {
		if (v == null) {
			return null;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
