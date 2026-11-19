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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInvoiceCallbackPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import cn.shopex.ecshopx.orders.domain.OrderInvoiceLog;
import cn.shopex.ecshopx.orders.domain.OrderInvoices;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceItemMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceLogMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoicesMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceApiRowSupport;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceStatusChangeHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class OrdersSaasErpInvoiceCallbackAdapter implements SaasErpInvoiceCallbackPort {

	private static final Logger log = LoggerFactory.getLogger(OrdersSaasErpInvoiceCallbackAdapter.class);

	private static final Map<Integer, String> STATUS_BY_MEMO =
			Map.of(1, "inProgress", 2, "success", 3, "waste", 4, "failed");

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
	private final OrderInvoiceItemMapper orderInvoiceItemMapper;
	private final OrderInvoicesMapper orderInvoicesMapper;
	private final OrderInvoiceLogMapper orderInvoiceLogMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderInvoiceStatusChangeHandler orderInvoiceStatusChangeHandler;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OrdersSaasErpInvoiceCallbackAdapter(
			OrderInvoiceMapper orderInvoiceMapper,
			OrderInvoiceItemMapper orderInvoiceItemMapper,
			OrderInvoicesMapper orderInvoicesMapper,
			OrderInvoiceLogMapper orderInvoiceLogMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderInvoiceStatusChangeHandler orderInvoiceStatusChangeHandler,
			ObjectMapper objectMapper,
			TransactionTemplate transactionTemplate) {
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.orderInvoiceItemMapper = orderInvoiceItemMapper;
		this.orderInvoicesMapper = orderInvoicesMapper;
		this.orderInvoiceLogMapper = orderInvoiceLogMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderInvoiceStatusChangeHandler = orderInvoiceStatusChangeHandler;
		this.objectMapper = objectMapper;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public Map<String, Object> handle(Map<String, String> params) {
		Map<String, String> callbackData = params == null ? Map.of() : params;
		String invoiceNo = trimToNull(callbackData.get("invoice_no"));

		OrderInvoice invoiceInfo = null;
		if (invoiceNo != null) {
			invoiceInfo =
					orderInvoiceMapper.selectOne(
							new LambdaQueryWrapper<OrderInvoice>()
									.eq(OrderInvoice::getInvoiceApplyBn, invoiceNo)
									.last("LIMIT 1"));
		}

		if (invoiceInfo != null) {
			final OrderInvoice foundInvoice = invoiceInfo;
			try {
				Map<String, Object> updateResult =
						transactionTemplate.execute(
								status -> processFoundInvoice(foundInvoice, callbackData));
				Map<String, Object> ok = new LinkedHashMap<>();
				ok.put("success", true);
				ok.put("message", "开票状态更新成功");
				ok.put("data", objectMapper.writeValueAsString(updateResult));
				log.info("开票回调处理成功");
				return ok;
			} catch (Exception e) {
				log.error("开票回调处理异常", e);
				Map<String, Object> fail = new LinkedHashMap<>();
				fail.put("success", false);
				fail.put("message", "开票状态更新失败: " + e.getMessage());
				return fail;
			}
		}

		String orderBn = trimToNull(callbackData.get("order_bn"));
		if (orderBn != null) {
			Long orderId = parseLong(orderBn);
			if (orderId != null) {
				int nowSec = (int) (System.currentTimeMillis() / 1000L);
				normalOrdersMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.set(NormalOrders::getInvoiceStatus, "DONE")
								.set(NormalOrders::getUpdateTime, nowSec));
				normalOrdersItemsMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.set(NormalOrdersItems::getIsInvoice, 1)
								.set(NormalOrdersItems::getUpdateTime, nowSec));
			}
		}

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("success", true);
		ok.put("message", "开票状态更新成功");
		return ok;
	}

	private Map<String, Object> processFoundInvoice(
			OrderInvoice invoiceInfo, Map<String, String> callbackData) {
		int memo = parseMemo(callbackData.get("invoice_memo"));
		String mappedStatus = STATUS_BY_MEMO.getOrDefault(memo, "failed");

		Map<String, Object> updateData = new LinkedHashMap<>();
		updateData.put("invoice_status", mappedStatus);
		String pdfInfo = trimToNull(callbackData.get("pdf_info"));
		if (pdfInfo != null) {
			updateData.put("invoice_file_url", pdfInfo);
		}

		String oldStatus = invoiceInfo.getInvoiceStatus();
		int nowSec = (int) (System.currentTimeMillis() / 1000L);

		LambdaUpdateWrapper<OrderInvoice> invoiceUpdate =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getInvoiceApplyBn, invoiceInfo.getInvoiceApplyBn());
		invoiceUpdate.set(OrderInvoice::getInvoiceStatus, mappedStatus);
		if (pdfInfo != null) {
			invoiceUpdate.set(OrderInvoice::getInvoiceFileUrl, pdfInfo);
		}
		invoiceUpdate.set(OrderInvoice::getUpdateTime, nowSec);
		int rows = orderInvoiceMapper.update(null, invoiceUpdate);
		if (rows == 0) {
			throw new IllegalStateException("未查询到更新数据");
		}

		if (memo == 2 || memo == 3) {
			OrderInvoices issued = new OrderInvoices();
			issued.setInvoiceId(invoiceInfo.getId());
			issued.setCompanyId(invoiceInfo.getCompanyId());
			issued.setInvoiceType(memo == 2 ? "blue" : "red");
			issued.setInvoiceNo(callbackData.get("invoice_no"));
			String invoiceCode = trimToNull(callbackData.get("invoice_code"));
			if (invoiceCode == null) {
				invoiceCode = trimToNull(callbackData.get("blue_invoice_code"));
			}
			if (invoiceCode != null) {
				issued.setInvoiceCode(invoiceCode);
			}
			String invoiceTime = trimToNull(callbackData.get("invoice_time"));
			if (invoiceTime != null) {
				issued.setInvoiceTime(invoiceTime);
			}
			issued.setCreateTime(nowSec);
			issued.setUpdateTime(nowSec);
			orderInvoicesMapper.insert(issued);
		}

		writeSystemOperatorLog(invoiceInfo, updateData);
		orderInvoiceStatusChangeHandler.handle(invoiceInfo.getId(), oldStatus, mappedStatus);

		if (memo == 3 || memo == 4) {
			int isInvoice = memo == 3 ? 3 : 0;
			List<OrderInvoiceItem> allInvoiceItems =
					orderInvoiceItemMapper.selectList(
							new LambdaQueryWrapper<OrderInvoiceItem>()
									.eq(OrderInvoiceItem::getInvoiceId, invoiceInfo.getId())
									.eq(OrderInvoiceItem::getCompanyId, invoiceInfo.getCompanyId()));

			List<Long> oids = new ArrayList<>();
			for (OrderInvoiceItem item : allInvoiceItems) {
				Long oid = parseLong(item.getOid());
				if (oid != null) {
					oids.add(oid);
				}
			}
			if (!oids.isEmpty()) {
				normalOrdersItemsMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrdersItems>()
								.in(NormalOrdersItems::getId, oids)
								.set(NormalOrdersItems::getIsInvoice, isInvoice)
								.set(NormalOrdersItems::getUpdateTime, nowSec));
			}

			List<Long> orderIds = new ArrayList<>();
			for (OrderInvoiceItem item : allInvoiceItems) {
				Long parsed = parseLong(item.getOrderId());
				if (parsed != null && !orderIds.contains(parsed)) {
					orderIds.add(parsed);
				}
			}
			for (Long orderId : orderIds) {
				List<NormalOrdersItems> stillInvoiced =
						normalOrdersItemsMapper.selectList(
								new LambdaQueryWrapper<NormalOrdersItems>()
										.eq(NormalOrdersItems::getOrderId, orderId)
										.eq(NormalOrdersItems::getIsInvoice, 1));
				String orderInvoiceStatus = stillInvoiced.isEmpty() ? "PENDING" : "PARTAIL";
				normalOrdersMapper.update(
						null,
						new LambdaUpdateWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.set(NormalOrders::getInvoiceStatus, orderInvoiceStatus)
								.set(NormalOrders::getUpdateTime, nowSec));
			}
		}

		OrderInvoice fresh = orderInvoiceMapper.selectById(invoiceInfo.getId());
		if (fresh == null) {
			throw new IllegalStateException("未查询到更新数据");
		}
		return OrderInvoiceApiRowSupport.toColumnNamesData(fresh);
	}

	private void writeSystemOperatorLog(OrderInvoice invoiceInfo, Map<String, Object> updateData) {
		List<Map<String, Object>> paramsList = new ArrayList<>();
		for (Map.Entry<String, String> en : ALLOW_FIELD_LABELS.entrySet()) {
			String key = en.getKey();
			if (!updateData.containsKey(key)) {
				continue;
			}
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("field", key);
			p.put("name", en.getValue());
			p.put("oldValue", Objects.toString(getInvoiceField(invoiceInfo, key), ""));
			p.put("newValue", Objects.toString(updateData.get(key), ""));
			paramsList.add(p);
		}

		String operatorContent;
		try {
			operatorContent = objectMapper.writeValueAsString(paramsList);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("记录发票日志失败", e);
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		OrderInvoiceLog logRow = new OrderInvoiceLog();
		logRow.setInvoiceId(invoiceInfo.getId());
		logRow.setOperatorType("system");
		logRow.setOperatorId(1L);
		logRow.setOperatorContent(operatorContent);
		logRow.setCreateTime(nowSec);
		logRow.setUpdateTime(nowSec);
		orderInvoiceLogMapper.insert(logRow);
	}

	private static Object getInvoiceField(OrderInvoice invoice, String key) {
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

	private static int parseMemo(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long parseLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Long.valueOf(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
