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

package cn.shopex.ecshopx.orders.service.invoice.export;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailStatusAppApplier;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds invoice-related order-line CSV exports. Export list queries use a 2000-row page size.
 */
@Service
public class InvoiceExportCsvExportService {

	/** Invoice export list batch size (2000 rows per page; do not reduce to 500). */
	private static final int PAGE_SIZE = 2000;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final Pattern ORDER_ID_NUMERIC = Pattern.compile("^[0-9]+$");
	private static final List<String> INVOICE_STATUSES =
			List.of("pending", "success", "inProgress", "waste", "failed");

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderInvoiceMapper orderInvoiceMapper;
	private final TradeMapper tradeMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final InvoiceExportItemCategoryPathService invoiceExportItemCategoryPathService;

	public InvoiceExportCsvExportService(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderInvoiceMapper orderInvoiceMapper,
			TradeMapper tradeMapper,
			ExportCsvFileService exportCsvFileService,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			InvoiceExportItemCategoryPathService invoiceExportItemCategoryPathService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderInvoiceMapper = orderInvoiceMapper;
		this.tradeMapper = tradeMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.invoiceExportItemCategoryPathService = invoiceExportItemCategoryPathService;
	}

	public Optional<Map<String, String>> runExport(InvoiceExportJobContext ctx) {
		if ("supplier_order".equals(ctx.orderType())) {
			return runSupplierExport(ctx);
		}
		return runNormalExport(ctx);
	}

	private Optional<Map<String, String>> runNormalExport(InvoiceExportJobContext ctx) {
		LinkedHashMap<String, Object> filter = ctx.filter();
		long companyId = ctx.companyId();
		LinkedHashMap<String, String> title = buildNormalTitleRow();
		List<Map<String, String>> rows = new ArrayList<>();

		int pageNum = 1;
		while (true) {
			Page<NormalOrders> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<NormalOrders> result =
					normalOrdersMapper.selectPage(
							page, InvoiceExportNormalOrderQuerySupport.toListWrapper(companyId, filter));
			List<NormalOrders> orders = result.getRecords();
			if (orders == null || orders.isEmpty()) {
				break;
			}
			List<Long> orderIds = orders.stream().map(NormalOrders::getOrderId).toList();
			Map<Long, List<NormalOrdersItems>> itemsByOrder = loadItems(companyId, orderIds, null);
			Map<Long, OrderInvoice> invByOrder = loadSingleInvoicePerOrder(companyId, orderIds);
			Map<Long, String> tradeNoByOrder = loadTradeNos(companyId, orderIds);
			Map<Long, String> memberNames = loadMemberDisplayNames(companyId, extractUserIds(orders, itemsByOrder));
			Map<Long, String> categoryPaths =
					invoiceExportItemCategoryPathService.loadCategoryPaths(
							companyId, extractItemIds(itemsByOrder));

			for (NormalOrders ord : orders) {
				long oid = ord.getOrderId();
				List<NormalOrdersItems> items = itemsByOrder.getOrDefault(oid, List.of());
				if (items.isEmpty()) {
					continue;
				}
				String tradeNo = tradeNoByOrder.getOrDefault(oid, "");
				OrderInvoice inv = invByOrder.get(oid);
				String memberName = memberNames.getOrDefault(ord.getUserId(), "");
				String orderStatusMsg = resolveOrderStatusMessage(ord);
				Map<String, String> orderInvoiceFlat = parseOrderInvoiceJson(ord.getInvoice());
				for (NormalOrdersItems it : items) {
					rows.add(
							buildNormalDataRow(
									ord,
									it,
									inv,
									tradeNo,
									memberName,
									orderStatusMsg,
									categoryPaths.getOrDefault(it.getItemId(), ""),
									orderInvoiceFlat));
				}
			}

			if (orders.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "invoice";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private Optional<Map<String, String>> runSupplierExport(InvoiceExportJobContext ctx) {
		LinkedHashMap<String, Object> filter = ctx.filter();
		long companyId = ctx.companyId();
		int supplierId = (int) ctx.operatorId();
		LinkedHashMap<String, String> title = buildSupplierTitleRow();
		List<Map<String, String>> rows = new ArrayList<>();

		int pageNum = 1;
		while (true) {
			Page<SupplierOrder> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<SupplierOrder> result =
					supplierOrderMapper.selectPage(
							page, InvoiceExportSupplierOrderQuerySupport.toListWrapper(companyId, filter));
			List<SupplierOrder> supplierOrders = result.getRecords();
			if (supplierOrders == null || supplierOrders.isEmpty()) {
				break;
			}
			List<Long> orderIds = supplierOrders.stream().map(SupplierOrder::getOrderId).distinct().toList();
			Map<Long, NormalOrders> normalByOrderId = loadNormalOrdersByIds(orderIds);
			Map<Long, List<NormalOrdersItems>> itemsByOrder = loadItems(companyId, orderIds, supplierId);
			Map<Long, String> tradeNoByOrder = loadTradeNos(companyId, orderIds);
			Map<Long, String> memberNames =
					loadMemberDisplayNames(
							companyId,
							supplierOrders.stream()
									.map(SupplierOrder::getUserId)
									.filter(uid -> uid != null && uid > 0L)
									.distinct()
									.toList());
			Map<Long, String> categoryPaths =
					invoiceExportItemCategoryPathService.loadCategoryPaths(
							companyId, extractItemIds(itemsByOrder));

			for (SupplierOrder so : supplierOrders) {
				long oid = so.getOrderId();
				NormalOrders ord = normalByOrderId.get(oid);
				if (ord == null) {
					continue;
				}
				List<NormalOrdersItems> items = itemsByOrder.getOrDefault(oid, List.of());
				if (items.isEmpty()) {
					continue;
				}
				String tradeNo = tradeNoByOrder.getOrDefault(oid, "");
				String memberName =
						memberNames.getOrDefault(so.getUserId() != null ? so.getUserId() : ord.getUserId(), "");
				String orderStatusMsg = resolveOrderStatusMessage(ord);
				// Supplier CSV invoice columns use the main order row's invoice JSON (order list snapshot), not supplier_order.invoice.
				Map<String, String> orderInvoiceFlat = parseOrderInvoiceJson(ord.getInvoice());
				for (NormalOrdersItems it : items) {
					rows.add(
							buildSupplierDataRow(
									ord,
									it,
									tradeNo,
									memberName,
									orderStatusMsg,
									categoryPaths.getOrDefault(it.getItemId(), ""),
									orderInvoiceFlat));
				}
			}

			if (supplierOrders.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}

		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "supplier_invoice";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private static LinkedHashMap<String, String> buildNormalTitleRow() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("trade_no", "订单序号");
		title.put("name", "用户名");
		title.put("order_status", "订单状态");
		title.put("end_time", "订单完成时间");
		title.put("item_name", "商品名称");
		title.put("item_spec_desc", "规格");
		title.put("category_tree", "管理分类");
		title.put("price", "商品销售单价");
		title.put("cost_price", "成本/计算单价");
		title.put("item_num", "购买数量");
		title.put("item_fee", "销售总金额（¥）");
		title.put("commission_fee", "商品佣金");
		title.put("cost_fee", "成本总金额（¥）");
		title.put("freight_fee", "运费（总）");
		title.put("total_fee_total", "实付金额（总）");
		title.put("point_fee", "积分抵扣（¥）");
		title.put("total_fee", "现金实付（¥）");
		title.put("discount_fee", "优惠总金额");
		title.put("discount_info", "优惠详情");
		title.put("content", "发票抬头");
		title.put("registration_number", "税号");
		title.put("company_address", "单位地址");
		title.put("company_phone", "电话号码");
		title.put("bankname", "开户银行");
		title.put("bankaccount", "银行账户");
		title.put("email", "收票邮箱");
		title.put("invoice_status", "发票状态");
		title.put("invoice_amount", "发票金额");
		title.put("invoice_type", "发票类型");
		title.put("invoice_type_code", "发票类型编码");
		title.put("invoice_method", "发票方式");
		title.put("invoice_source", "发票来源");
		title.put("remark", "备注");
		return title;
	}

	private static LinkedHashMap<String, String> buildSupplierTitleRow() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("trade_no", "订单序号");
		title.put("name", "用户名");
		title.put("order_status", "订单状态");
		title.put("end_time", "订单完成时间");
		title.put("item_name", "商品名称");
		title.put("item_spec_desc", "规格");
		title.put("category_tree", "管理分类");
		title.put("cost_price", "成本/计算单价");
		title.put("item_num", "购买数量");
		title.put("cost_fee", "成本总金额（¥）");
		title.put("freight_fee", "运费（总）");
		title.put("content", "发票抬头");
		title.put("registration_number", "税号");
		title.put("company_address", "单位地址");
		title.put("company_phone", "电话号码");
		title.put("bankname", "开户银行");
		title.put("bankaccount", "银行账户");
		title.put("email", "收票邮箱");
		return title;
	}

	private LinkedHashMap<String, String> buildNormalDataRow(
			NormalOrders ord,
			NormalOrdersItems it,
			OrderInvoice inv,
			String tradeNo,
			String memberName,
			String orderStatusMsg,
			String categoryPath,
			Map<String, String> orderInvoiceFlat) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderIdCell(String.valueOf(ord.getOrderId())));
		row.put("trade_no", nz(tradeNo));
		row.put("name", nz(memberName));
		row.put("order_status", nz(orderStatusMsg));
		row.put("end_time", formatEpoch(ord.getEndTime()));
		row.put("item_name", sanitizeItemName(it.getItemName()));
		row.put("item_spec_desc", sanitizeItemSpec(it.getItemSpecDesc()));
		row.put("category_tree", nz(categoryPath));
		row.put("price", centsToYuan(it.getPrice()));
		row.put("cost_price", centsToYuan(it.getCostPrice()));
		row.put("item_num", it.getNum() == null ? "" : String.valueOf(it.getNum()));
		row.put("item_fee", centsToYuan(it.getItemFee()));
		row.put("commission_fee", centsToYuan(it.getCommissionFee()));
		row.put("cost_fee", centsToYuan(it.getCostFee()));
		row.put("freight_fee", freightFeeRaw(ord.getFreightFee()));
		int itemTotal = it.getTotalFee() == null ? 0 : it.getTotalFee();
		int ordFreight = ord.getFreightFee() == null ? 0 : ord.getFreightFee();
		int itemPoint = it.getPointFee() == null ? 0 : it.getPointFee();
		row.put("total_fee_total", centsToYuan(itemTotal + ordFreight + itemPoint));
		row.put("point_fee", centsToYuan(it.getPointFee()));
		row.put("total_fee", centsToYuan(it.getTotalFee()));
		row.put("discount_fee", centsToYuan(resolveItemDiscountCents(it)));
		row.put("discount_info", nz(buildDiscountInfoDesc(it.getDiscountInfo())));

		applyOrderInvoiceSnapshot(row, orderInvoiceFlat);
		if (inv != null) {
			applyOrderInvoiceRecord(row, inv);
		} else {
			row.put("invoice_status", "");
			row.put("invoice_amount", "");
			row.put("invoice_type", "");
			row.put("invoice_type_code", "");
			row.put("invoice_method", "");
			row.put("invoice_source", "");
			row.put("remark", "");
		}
		normalizeBankAccountCell(row, inv == null);
		return row;
	}

	private LinkedHashMap<String, String> buildSupplierDataRow(
			NormalOrders ord,
			NormalOrdersItems it,
			String tradeNo,
			String memberName,
			String orderStatusMsg,
			String categoryPath,
			Map<String, String> orderInvoiceFlat) {
		LinkedHashMap<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderIdCell(String.valueOf(ord.getOrderId())));
		row.put("trade_no", nz(tradeNo));
		row.put("name", nz(memberName));
		row.put("order_status", nz(orderStatusMsg));
		row.put("end_time", formatEpoch(ord.getEndTime()));
		row.put("item_name", sanitizeItemName(it.getItemName()));
		row.put("item_spec_desc", sanitizeItemSpec(it.getItemSpecDesc()));
		row.put("category_tree", nz(categoryPath));
		row.put("cost_price", centsToYuan(it.getCostPrice()));
		row.put("item_num", it.getNum() == null ? "" : String.valueOf(it.getNum()));
		row.put("cost_fee", centsToYuan(it.getCostFee()));
		row.put("freight_fee", freightFeeRaw(ord.getFreightFee()));
		applyOrderInvoiceSnapshot(row, orderInvoiceFlat);
		normalizeBankAccountCell(row, true);
		return row;
	}

	private static void applyOrderInvoiceSnapshot(LinkedHashMap<String, String> row, Map<String, String> flat) {
		row.put("content", nz(flat.get("content")));
		row.put("registration_number", nz(flat.get("registration_number")));
		row.put("company_address", nz(flat.get("company_address")));
		row.put("company_phone", nz(flat.get("company_phone")));
		row.put("bankname", nz(flat.get("bankname")));
		row.put("bankaccount", nz(flat.get("bankaccount")));
		row.put("email", nz(flat.get("email")));
	}

	private static void applyOrderInvoiceRecord(LinkedHashMap<String, String> row, OrderInvoice inv) {
		row.put("content", nz(inv.getCompanyTitle()));
		row.put("registration_number", nz(inv.getCompanyTaxNumber()));
		row.put("company_address", nz(inv.getCompanyAddress()));
		row.put("company_phone", nz(inv.getCompanyTelephone()));
		row.put("bankname", nz(inv.getBankName()));
		row.put("bankaccount", nz(inv.getBankAccount()));
		row.put("email", nz(inv.getEmail()));
		row.put("invoice_status", nz(inv.getInvoiceStatus()));
		row.put("invoice_amount", centsToYuan(inv.getInvoiceAmount()));
		row.put("invoice_type", nz(inv.getInvoiceType()));
		row.put("invoice_type_code", nz(inv.getInvoiceTypeCode()));
		row.put("invoice_method", nz(inv.getInvoiceMethod()));
		row.put("invoice_source", nz(inv.getInvoiceSource()));
		row.put("remark", nz(inv.getRemark()));
	}

	/**
	 * Mirrors legacy CSV behavior: tab-wrap bank account only when the value comes from the order snapshot,
	 * not when overridden by an {@link OrderInvoice} row.
	 */
	private static void normalizeBankAccountCell(LinkedHashMap<String, String> row, boolean invoiceRecordMissing) {
		String v = row.get("bankaccount");
		if (!StringUtils.hasText(v)) {
			row.put("bankaccount", "");
			return;
		}
		if (invoiceRecordMissing) {
			row.put("bankaccount", "\t" + v + "\t");
		}
	}

	private Map<String, String> parseOrderInvoiceJson(String raw) {
		Map<String, String> out = new HashMap<>();
		if (!StringUtils.hasText(raw)) {
			return out;
		}
		try {
			JsonNode n = objectMapper.readTree(raw.trim());
			if (!n.isObject()) {
				return out;
			}
			putJsonString(out, n, "content");
			putJsonString(out, n, "company_title", "content");
			putJsonString(out, n, "registration_number");
			putJsonString(out, n, "company_tax_number", "registration_number");
			putJsonString(out, n, "company_address");
			putJsonString(out, n, "company_phone");
			putJsonString(out, n, "company_telephone", "company_phone");
			putJsonString(out, n, "bankname");
			putJsonString(out, n, "bank_name", "bankname");
			putJsonString(out, n, "bankaccount");
			putJsonString(out, n, "bank_account", "bankaccount");
			putJsonString(out, n, "email");
		} catch (Exception ignored) {
		}
		return out;
	}

	private static void putJsonString(Map<String, String> out, JsonNode n, String key) {
		putJsonString(out, n, key, key);
	}

	private static void putJsonString(Map<String, String> out, JsonNode n, String jsonKey, String outKey) {
		if (!n.has(jsonKey) || n.get(jsonKey).isNull()) {
			return;
		}
		String v = n.get(jsonKey).asText("");
		if (StringUtils.hasText(v) && !StringUtils.hasText(out.get(outKey))) {
			out.put(outKey, v);
		}
	}

	private String resolveOrderStatusMessage(NormalOrders ord) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_status", ord.getOrderStatus());
		m.put("receipt_type", ord.getReceiptType());
		m.put("cancel_status", ord.getCancelStatus());
		m.put("ziti_status", ord.getZitiStatus());
		m.put("delivery_status", ord.getDeliveryStatus());
		m.put("prescription_status", ord.getPrescriptionStatus());
		m.put("diagnosis_data", null);
		adminOrderDetailStatusAppApplier.apply(m, null, "0");
		Object msg = m.get("order_status_msg");
		return msg == null ? "" : String.valueOf(msg);
	}

	private Map<Long, NormalOrders> loadNormalOrdersByIds(List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<NormalOrders> list = normalOrdersMapper.selectBatchIds(orderIds);
		Map<Long, NormalOrders> out = new HashMap<>();
		for (NormalOrders o : list) {
			out.put(o.getOrderId(), o);
		}
		return out;
	}

	private Map<Long, String> loadMemberDisplayNames(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		List<Long> distinct = userIds.stream().filter(id -> id != null && id > 0L).distinct().toList();
		if (distinct.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<MembersInfo> w = new LambdaQueryWrapper<>();
		w.eq(MembersInfo::getCompanyId, companyId);
		w.in(MembersInfo::getUserId, distinct);
		List<MembersInfo> infos = membersInfoMapper.selectList(w);
		Map<Long, String> out = new HashMap<>();
		for (MembersInfo mi : infos) {
			String raw = firstNonBlank(mi.getName(), mi.getUsername());
			out.put(mi.getUserId(), decryptMaybe(raw));
		}
		return out;
	}

	private String decryptMaybe(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			return sensitiveFieldEncryptor.decrypt(raw);
		} catch (Exception e) {
			return raw;
		}
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}

	private static List<Long> extractUserIds(
			List<NormalOrders> orders, Map<Long, List<NormalOrdersItems>> itemsByOrder) {
		List<Long> ids = new ArrayList<>();
		for (NormalOrders o : orders) {
			if (o.getUserId() != null && o.getUserId() > 0L) {
				ids.add(o.getUserId());
			}
		}
		for (List<NormalOrdersItems> list : itemsByOrder.values()) {
			for (NormalOrdersItems it : list) {
				if (it.getUserId() != null && it.getUserId() > 0L) {
					ids.add(it.getUserId());
				}
			}
		}
		return ids.stream().distinct().toList();
	}

	private static List<Long> extractItemIds(Map<Long, List<NormalOrdersItems>> itemsByOrder) {
		List<Long> ids = new ArrayList<>();
		for (List<NormalOrdersItems> list : itemsByOrder.values()) {
			for (NormalOrdersItems it : list) {
				if (it.getItemId() != null && it.getItemId() > 0L) {
					ids.add(it.getItemId());
				}
			}
		}
		return ids.stream().distinct().toList();
	}

	private Map<Long, List<NormalOrdersItems>> loadItems(
			long companyId, List<Long> orderIds, Integer supplierIdOrNull) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<NormalOrdersItems> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrdersItems::getCompanyId, companyId);
		w.in(NormalOrdersItems::getOrderId, orderIds);
		if (supplierIdOrNull != null) {
			w.eq(NormalOrdersItems::getSupplierId, supplierIdOrNull);
		}
		List<NormalOrdersItems> list = normalOrdersItemsMapper.selectList(w);
		Map<Long, List<NormalOrdersItems>> out = new HashMap<>();
		for (NormalOrdersItems it : list) {
			out.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
		}
		return out;
	}

	private Map<Long, OrderInvoice> loadSingleInvoicePerOrder(long companyId, List<Long> orderIds) {
		Map<Long, OrderInvoice> out = new HashMap<>();
		if (orderIds.isEmpty()) {
			return out;
		}
		final int chunk = 40;
		for (int i = 0; i < orderIds.size(); i += chunk) {
			List<Long> sub = orderIds.subList(i, Math.min(i + chunk, orderIds.size()));
			LambdaQueryWrapper<OrderInvoice> w = new LambdaQueryWrapper<>();
			w.eq(OrderInvoice::getCompanyId, companyId);
			w.in(OrderInvoice::getInvoiceStatus, INVOICE_STATUSES);
			w.orderByAsc(OrderInvoice::getId);
			w.and(q -> {
				boolean first = true;
				for (Long oid : sub) {
					if (first) {
						q.apply(
								"FIND_IN_SET(CAST({0} AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, order_id COLLATE utf8mb4_unicode_ci) > 0",
								String.valueOf(oid));
						first = false;
					} else {
						q.or(sq -> sq.apply(
								"FIND_IN_SET(CAST({0} AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci, order_id COLLATE utf8mb4_unicode_ci) > 0",
								String.valueOf(oid)));
					}
				}
			});
			List<OrderInvoice> list = orderInvoiceMapper.selectList(w);
			for (OrderInvoice inv : list) {
				for (Long oid : sub) {
					if (invoiceCoversOrder(inv, oid)) {
						out.put(oid, inv);
					}
				}
			}
		}
		return out;
	}

	private static boolean invoiceCoversOrder(OrderInvoice inv, long orderId) {
		if (inv.getOrderId() == null || inv.getOrderId().isBlank()) {
			return false;
		}
		for (String p : inv.getOrderId().split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				if (Long.parseLong(t) == orderId) {
					return true;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}

	private Map<Long, String> loadTradeNos(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<String> idStrs = orderIds.stream().map(String::valueOf).toList();
		LambdaQueryWrapper<Trade> w = new LambdaQueryWrapper<>();
		w.eq(Trade::getCompanyId, String.valueOf(companyId));
		w.in(Trade::getOrderId, idStrs);
		w.eq(Trade::getTradeState, "SUCCESS");
		List<Trade> trades = tradeMapper.selectList(w);
		Map<Long, String> m = new HashMap<>();
		for (Trade t : trades) {
			try {
				long oid = Long.parseLong(String.valueOf(t.getOrderId()).trim());
				if (m.containsKey(oid)) {
					continue;
				}
				m.put(oid, t.getTradeNo() != null ? t.getTradeNo() : "");
			} catch (NumberFormatException ignored) {
			}
		}
		return m;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String sanitizeItemName(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("#", "");
	}

	private static String sanitizeItemSpec(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return s.replace(',', '，');
	}

	private static String formatOrderIdCell(String orderIdStr) {
		if (ORDER_ID_NUMERIC.matcher(orderIdStr).matches()) {
			return "\t" + orderIdStr;
		}
		return orderIdStr;
	}

	private static String formatEpoch(Long epoch) {
		if (epoch == null || epoch == 0L) {
			return "";
		}
		if (epoch > 1_000_000_000_000L) {
			epoch = epoch / 1000L;
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), SHANGHAI).format(CSV_TIME);
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "";
		}
		if (cents == 0) {
			return "0";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static int resolveItemDiscountCents(NormalOrdersItems it) {
		int md = it.getMemberDiscount() == null ? 0 : it.getMemberDiscount();
		int cd = it.getCouponDiscount() == null ? 0 : it.getCouponDiscount();
		if (md > 0 && cd > 0) {
			return md + cd;
		}
		return it.getDiscountFee() == null ? 0 : it.getDiscountFee();
	}

	private String buildDiscountInfoDesc(String discountInfoJson) {
		if (!StringUtils.hasText(discountInfoJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(discountInfoJson.trim());
			if (!root.isArray()) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (JsonNode el : root) {
				if (!el.isObject()) {
					continue;
				}
				String part = discountDescForOne(el);
				if (StringUtils.hasText(part)) {
					sb.append(part);
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String discountDescForOne(JsonNode value) {
		JsonNode typeNode = value.get("type");
		if (typeNode == null || typeNode.isNull()) {
			return "";
		}
		String type = typeNode.asText("");
		JsonNode feeNode = value.get("discount_fee");
		int feeCents = feeNode == null || !feeNode.isNumber() ? 0 : feeNode.asInt();
		if (feeCents <= 0) {
			return "";
		}
		String feeYuan =
				BigDecimal.valueOf(feeCents)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString()
						+ "元";
		return switch (type) {
			case "full_discount" -> "满折：" + feeYuan + "; ";
			case "full_minus" -> "满减：" + feeYuan + "; ";
			case "coupon_discount" -> "折扣优惠券：" + feeYuan + "; ";
			case "cash_discount" -> "代金优惠券：" + feeYuan + "; ";
			case "limited_time_sale" -> "限时特惠：" + feeYuan + "; ";
			case "seckill" -> "秒杀：" + feeYuan + "; ";
			case "groups" -> "拼团：" + feeYuan + "; ";
			case "member_price" -> "会员价：" + feeYuan + "; ";
			case "member_tag_targeted_promotion" -> "定向促销：" + feeYuan + "; ";
			default -> "";
		};
	}

	private static String freightFeeRaw(Integer freightCents) {
		if (freightCents == null) {
			return "0";
		}
		return String.valueOf(freightCents);
	}
}
