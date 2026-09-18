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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderInvoiceUpdateService {

	private static final ObjectMapper SNAKE =
			new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	private static final LinkedHashMap<String, String> ALLOW_FIELD_LABELS = new LinkedHashMap<>();

	static {
		ALLOW_FIELD_LABELS.put("invoice_status", "开票状态");
		ALLOW_FIELD_LABELS.put("invoice_file_url", "发票文件");
		ALLOW_FIELD_LABELS.put("invoice_method", "开票类型");
		ALLOW_FIELD_LABELS.put("company_title", "公司抬头");
		ALLOW_FIELD_LABELS.put("company_tax_number", "公司税号");
		ALLOW_FIELD_LABELS.put("company_address", "公司地址");
		ALLOW_FIELD_LABELS.put("company_telephone", "公司电话");
		ALLOW_FIELD_LABELS.put("bank_name", "开户银行");
		ALLOW_FIELD_LABELS.put("bank_account", "开户账号");
		ALLOW_FIELD_LABELS.put("email", "电子邮箱");
		ALLOW_FIELD_LABELS.put("mobile", "手机号码");
		ALLOW_FIELD_LABELS.put("remark", "备注");
	}

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	private final ObjectMapper objectMapper;

	public OrderInvoiceUpdateService(
			OrderInvoiceMapper orderInvoiceMapper,
			NormalOrdersMapper normalOrdersMapper,
			OrderInvoiceLogMapper orderInvoiceLogMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler,
			ObjectMapper objectMapper) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
		this.objectMapper = objectMapper;
	}

	public Object updateInvoiceForFrontMember(long userId, long companyId, Map<String, Object> merged) {
		Map<String, Object> data = merged == null ? new LinkedHashMap<>() : new LinkedHashMap<>(merged);

		boolean cancelBranch =
				data.containsKey("invoice_id")
						&& data.get("invoice_id") != null
						&& data.containsKey("invoice_status")
						&& data.get("invoice_status") != null
						&& "cancel".equals(String.valueOf(data.get("invoice_status")).trim());

		if (cancelBranch) {
			data.put("invoice_status", "cancel");
			long invoiceId = parseInvoiceIdFromMerged(data.get("invoice_id"));
			OrderInvoice row = requireInvoiceForMember(invoiceId, companyId, userId);
			String status = row.getInvoiceStatus();
			if ("cancel".equals(status)) {
				throw new ResourceException("发票已取消");
			}
			if (!("pending".equals(status) || "fail".equals(status))) {
				throw new ResourceException("发票当前状态不能取消");
			}
			return applyInvoiceUpdateAfterOrderChecks(companyId, invoiceId, row, data);
		}

		validateFrontNormalUpdate(data);
		data.put("user_id", userId);
		long invoiceId = parseInvoiceIdFromMerged(data.get("invoice_id"));
		OrderInvoice row = requireInvoiceForMember(invoiceId, companyId, userId);
		return applyInvoiceUpdateAfterOrderChecks(companyId, invoiceId, row, data);
	}

	public Object updateInvoice(long companyId, long operatorId, String invoiceIdRaw, Map<String, Object> merged) {
		String trimmedId = invoiceIdRaw == null ? "" : invoiceIdRaw.trim();
		if (!StringUtils.hasText(trimmedId)) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		final long invoiceId;
		try {
			invoiceId = Long.parseLong(trimmedId);
		} catch (NumberFormatException e) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		OrderInvoice actionRow =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.eq(OrderInvoice::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (actionRow == null) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		OrderInvoice invoice = orderInvoiceMapper.selectById(invoiceId);
		if (invoice == null) {
			return Collections.emptyList();
		}

		return applyInvoiceUpdateAfterOrderChecks(companyId, invoiceId, invoice, merged);
	}

	private OrderInvoice requireInvoiceForMember(long invoiceId, long companyId, long userId) {
		OrderInvoice row =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getUserId, userId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		return row;
	}

	private static long parseInvoiceIdFromMerged(Object raw) {
		if (raw == null) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		String trimmed = String.valueOf(raw).trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
	}

	private static void validateFrontNormalUpdate(Map<String, Object> merged) {
		List<String> parts = new ArrayList<>();
		if (!merged.containsKey("invoice_id")
				|| merged.get("invoice_id") == null
				|| !StringUtils.hasText(String.valueOf(merged.get("invoice_id")).trim())) {
			parts.add("发票ID必填");
		}
		if (!merged.containsKey("invoice_type")
				|| merged.get("invoice_type") == null
				|| !StringUtils.hasText(String.valueOf(merged.get("invoice_type")).trim())) {
			parts.add("发票类型必填");
		} else {
			String t = String.valueOf(merged.get("invoice_type")).trim();
			if (!"enterprise".equals(t) && !"individual".equals(t)) {
				parts.add("发票类型不正确");
			}
		}
		if (!merged.containsKey("company_title")
				|| merged.get("company_title") == null
				|| !StringUtils.hasText(String.valueOf(merged.get("company_title")).trim())) {
			parts.add("抬头必填");
		}
		if (!parts.isEmpty()) {
			throw new ResourceException("发票信息更新失败：" + String.join("；", parts));
		}
	}

	private Object applyInvoiceUpdateAfterOrderChecks(
			long companyId, long invoiceId, OrderInvoice invoice, Map<String, Object> merged) {
		String orderIdStr = OrderInvoicePrimaryOrderId.primarySegment(invoice.getOrderId());
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdStr);
		} catch (NumberFormatException e) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在");
		}

		NormalOrders orderRow =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderIdNum)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (orderRow == null) {
			throw new ResourceException("订单号为" + orderIdStr + "的订单不存在");
		}

		Map<String, Object> orderInfo = new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(orderRow));
		orderInfo.put("order_id", orderIdStr);
		orderInfo.put("company_id", companyId);
		applySyntheticNotpayCancel(orderInfo);
		if ("CANCEL".equals(str(orderInfo.get("order_status")))) {
			throw new ResourceException("订单已取消，无法修改发票信息");
		}

		if (merged == null || merged.isEmpty()) {
			OrderInvoice fresh = orderInvoiceMapper.selectById(invoiceId);
			return OrderInvoiceApiRowSupport.toColumnNamesData(fresh);
		}

		StringBuilder diffMsg = new StringBuilder();
		List<Map<String, Object>> paramsList = new ArrayList<>();
		for (Map.Entry<String, String> en : ALLOW_FIELD_LABELS.entrySet()) {
			String key = en.getKey();
			if (!isIssetForUpdate(merged, key)) {
				continue;
			}
			String oldCmp = normalizeForCompare(getInvoiceFieldForCompare(invoice, key));
			String newCmp = normalizeForCompare(merged.get(key));
			if (!Objects.equals(oldCmp, newCmp)) {
				diffMsg.append(en.getValue()).append(":").append(oldCmp).append("=>").append(newCmp).append(";");
			}
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("field", key);
			p.put("name", en.getValue());
			p.put("oldValue", oldCmp);
			p.put("newValue", newCmp);
			paramsList.add(p);
		}

		LambdaUpdateWrapper<OrderInvoice> uw = new LambdaUpdateWrapper<OrderInvoice>().eq(OrderInvoice::getId, invoiceId);
		applyWhitelistSets(uw, merged);
		int nowSec = (int) (System.currentTimeMillis() / 1000);
		uw.set(OrderInvoice::getUpdateTime, nowSec);
		int rows = orderInvoiceMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		String effectiveInvoiceStatus =
				isIssetForUpdate(merged, "invoice_status")
						? String.valueOf(merged.get("invoice_status"))
						: invoice.getInvoiceStatus();

		LambdaUpdateWrapper<NormalOrders> nu =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getOrderId, orderIdNum)
						.eq(NormalOrders::getCompanyId, companyId)
						.set(NormalOrders::getInvoiceStatus, effectiveInvoiceStatus)
						.set(NormalOrders::getUpdateTime, nowSec);
		int orderUpd = normalOrdersMapper.update(null, nu);
		if (orderUpd <= 0) {
			throw new ResourceException("订单不存在");
		}

		Map<String, Object> logPayload = new LinkedHashMap<>();
		logPayload.put("title", "更新发票");
		logPayload.put("remark", "更新发票:" + diffMsg);
		logPayload.put("params", paramsList);
		logPayload.put("result", Collections.emptyList());
		String operatorContent;
		try {
			operatorContent = objectMapper.writeValueAsString(logPayload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("记录发票日志失败");
		}

		OrderInvoiceLog logRow = new OrderInvoiceLog();
		logRow.setInvoiceId(invoiceId);
		logRow.setOperatorType("user");
		logRow.setUserId(invoice.getUserId() != null ? invoice.getUserId() : 1L);
		logRow.setOperatorContent(operatorContent);
		logRow.setCreateTime(nowSec);
		logRow.setUpdateTime(nowSec);
		orderInvoiceLogMapper.insert(logRow);

		OrderInvoice fresh = orderInvoiceMapper.selectById(invoiceId);
		return OrderInvoiceApiRowSupport.toColumnNamesData(fresh);
	}

	public Object updateInvoiceRemark(long companyId, long operatorId, String invoiceIdRaw, String remark) {
		String trimmedId = invoiceIdRaw == null ? "" : invoiceIdRaw.trim();
		if (!StringUtils.hasText(trimmedId)) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}
		final long invoiceId;
		try {
			invoiceId = Long.parseLong(trimmedId);
		} catch (NumberFormatException e) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		OrderInvoice actionRow =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.eq(OrderInvoice::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (actionRow == null) {
			throw new ResourceException("OrdersBundle/Order.invoice_not_found");
		}

		OrderInvoice row =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getId, invoiceId)
								.eq(OrderInvoice::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}
		String oldRemark = row.getRemark();

		int nowSec = (int) (System.currentTimeMillis() / 1000);
		String remarkVal = remark == null ? "" : remark;
		LambdaUpdateWrapper<OrderInvoice> uw =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, invoiceId)
						.eq(OrderInvoice::getCompanyId, companyId)
						.set(OrderInvoice::getRemark, remarkVal)
						.set(OrderInvoice::getUpdateTime, nowSec);
		int rows = orderInvoiceMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		if (rows > 0) {
			Map<String, Object> logPayload = new LinkedHashMap<>();
			logPayload.put("title", "更新发票备注");
			logPayload.put("remark", "更新发票备注");
			logPayload.put("params", Collections.emptyList());
			Map<String, Object> resultOne = new LinkedHashMap<>();
			resultOne.put("field", "remark");
			resultOne.put("name", "备注");
			resultOne.put("oldValue", oldRemark == null ? "" : oldRemark);
			resultOne.put("newValue", remarkVal);
			logPayload.put("result", resultOne);
			String operatorContent;
			try {
				operatorContent = objectMapper.writeValueAsString(logPayload);
			} catch (JsonProcessingException e) {
				throw new ResourceException("记录发票日志失败");
			}

			OrderInvoiceLog logRow = new OrderInvoiceLog();
			logRow.setInvoiceId(invoiceId);
			logRow.setOperatorType("system");
			logRow.setOperatorId(operatorId);
			logRow.setUserId(row.getUserId() != null ? row.getUserId() : 1L);
			logRow.setOperatorContent(operatorContent);
			logRow.setCreateTime(nowSec);
			logRow.setUpdateTime(nowSec);
			orderInvoiceLogMapper.insert(logRow);
		}

		OrderInvoice fresh = orderInvoiceMapper.selectById(invoiceId);
		return OrderInvoiceApiRowSupport.toColumnNamesData(fresh);
	}

	private void applyWhitelistSets(LambdaUpdateWrapper<OrderInvoice> uw, Map<String, Object> merged) {
		for (String col : OrderInvoiceApiRowSupport.COLS_ORDER) {
			if ("id".equals(col) || "update_time".equals(col) || !isIssetForUpdate(merged, col)) {
				continue;
			}
			Object raw = merged.get(col);
			switch (col) {
				case "invoice_apply_bn" -> uw.set(OrderInvoice::getInvoiceApplyBn, toStr(raw));
				case "user_id" -> uw.set(OrderInvoice::getUserId, toLong(raw));
				case "company_id" -> uw.set(OrderInvoice::getCompanyId, toLong(raw));
				case "regionauth_id" -> uw.set(OrderInvoice::getRegionauthId, toLong(raw));
				case "order_id" -> uw.set(OrderInvoice::getOrderId, toStr(raw));
				case "invoice_type" -> uw.set(OrderInvoice::getInvoiceType, toStr(raw));
				case "company_title" -> uw.set(OrderInvoice::getCompanyTitle, toStr(raw));
				case "company_tax_number" -> uw.set(OrderInvoice::getCompanyTaxNumber, toStr(raw));
				case "company_address" -> uw.set(OrderInvoice::getCompanyAddress, toStr(raw));
				case "company_telephone" -> uw.set(OrderInvoice::getCompanyTelephone, toStr(raw));
				case "bank_name" -> uw.set(OrderInvoice::getBankName, toStr(raw));
				case "bank_account" -> uw.set(OrderInvoice::getBankAccount, toStr(raw));
				case "email" -> uw.set(OrderInvoice::getEmail, toStr(raw));
				case "mobile" -> uw.set(OrderInvoice::getMobile, toStr(raw));
				case "invoice_status" -> uw.set(OrderInvoice::getInvoiceStatus, toStr(raw));
				case "try_times" -> uw.set(OrderInvoice::getTryTimes, toInteger(raw));
				case "invoice_amount" -> uw.set(OrderInvoice::getInvoiceAmount, toInteger(raw));
				case "invoice_file_url" -> uw.set(OrderInvoice::getInvoiceFileUrl, toStr(raw));
				case "invoice_file_url_red" -> uw.set(OrderInvoice::getInvoiceFileUrlRed, toStr(raw));
				case "invoice_method" -> uw.set(OrderInvoice::getInvoiceMethod, toStr(raw));
				case "invoice_source" -> uw.set(OrderInvoice::getInvoiceSource, toStr(raw));
				case "remark" -> uw.set(OrderInvoice::getRemark, toStr(raw));
				case "is_oms" -> uw.set(OrderInvoice::getIsOms, toInteger(raw));
				case "create_time" -> uw.set(OrderInvoice::getCreateTime, toInteger(raw));
				case "invoice_type_code" -> uw.set(OrderInvoice::getInvoiceTypeCode, toStr(raw));
				case "end_time" -> uw.set(OrderInvoice::getEndTime, toInteger(raw));
				case "close_aftersales_time" -> uw.set(OrderInvoice::getCloseAftersalesTime, toInteger(raw));
				case "query_content" -> uw.set(OrderInvoice::getQueryContent, toStr(raw));
				case "red_content" -> uw.set(OrderInvoice::getRedContent, toStr(raw));
				case "serial_no" -> uw.set(OrderInvoice::getSerialNo, toStr(raw));
				case "red_serial_no" -> uw.set(OrderInvoice::getRedSerialNo, toStr(raw));
				case "red_apply_bn" -> uw.set(OrderInvoice::getRedApplyBn, toStr(raw));
				case "order_shop_id" -> uw.set(OrderInvoice::getOrderShopId, toStr(raw));
				case "user_card_code" -> uw.set(OrderInvoice::getUserCardCode, toStr(raw));
				default -> {
					// COLS_ORDER is fixed; exhaustive switch for known columns
				}
			}
		}
	}

	private static String toStr(Object raw) {
		return raw == null ? null : String.valueOf(raw);
	}

	private static Long toLong(Object raw) {
		try {
			if (raw instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("参数类型错误");
		}
	}

	private static Integer toInteger(Object raw) {
		try {
			if (raw instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("参数类型错误");
		}
	}

	private static Object getInvoiceFieldForCompare(OrderInvoice invoice, String key) {
		return switch (key) {
			case "invoice_status" -> invoice.getInvoiceStatus();
			case "invoice_file_url" -> invoice.getInvoiceFileUrl();
			case "invoice_method" -> invoice.getInvoiceMethod();
			case "company_title" -> invoice.getCompanyTitle();
			case "company_tax_number" -> invoice.getCompanyTaxNumber();
			case "company_address" -> invoice.getCompanyAddress();
			case "company_telephone" -> invoice.getCompanyTelephone();
			case "bank_name" -> invoice.getBankName();
			case "bank_account" -> invoice.getBankAccount();
			case "email" -> invoice.getEmail();
			case "mobile" -> invoice.getMobile();
			case "remark" -> invoice.getRemark();
			default -> null;
		};
	}

	private static boolean isIssetForUpdate(Map<String, Object> merged, String key) {
		return merged != null && merged.containsKey(key) && merged.get(key) != null;
	}

	private static String normalizeForCompare(Object v) {
		return v == null ? "" : String.valueOf(v);
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
}
