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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPayloadMaps;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailStatusAppApplier;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListExecutorKind;
import cn.shopex.ecshopx.orders.service.admin.orderlist.AdminOrderListQueryExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappFrontNormalOrderListService {

	private static final Logger log = LoggerFactory.getLogger(WxappFrontNormalOrderListService.class);

	private final AdminOrderListQueryExecutor adminOrderListQueryExecutor;
	private final AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler;
	private final WxappOrderDetailShippingFeeLineSupport wxappOrderDetailShippingFeeLineSupport;

	public WxappFrontNormalOrderListService(
			AdminOrderListQueryExecutor adminOrderListQueryExecutor,
			AdminOrderDetailStatusAppApplier adminOrderDetailStatusAppApplier,
			InvoiceSettingRedisService invoiceSettingRedisService,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			WxappOrderInvoiceDetailAssembler wxappOrderInvoiceDetailAssembler,
			WxappOrderDetailShippingFeeLineSupport wxappOrderDetailShippingFeeLineSupport) {
		this.adminOrderListQueryExecutor = adminOrderListQueryExecutor;
		this.adminOrderDetailStatusAppApplier = adminOrderDetailStatusAppApplier;
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.wxappOrderInvoiceDetailAssembler = wxappOrderInvoiceDetailAssembler;
		this.wxappOrderDetailShippingFeeLineSupport = wxappOrderDetailShippingFeeLineSupport;
	}

	public Map<String, Object> getOrderList(
			AdminOrderListExecutorKind kind,
			Map<String, Object> filter,
			int page,
			int limit,
			String from,
			boolean needTotal) {
		Map<String, Object> f = new LinkedHashMap<>(filter);
		Object invoiceListRaw = f.remove("invoice_list");
		int invoiceListFlag = intVal(invoiceListRaw);
		boolean invoiceListActive = invoiceListFlag != 0;
		f.remove("is_distribution");
		f.putIfAbsent("supplier_id", 0);
		String listFrom = listContextFrom(from);
		f.put("wxapp_order_list_from", listFrom);

		if (invoiceListActive) {
			applyInvoiceListOrderFilters(f);
		}

		AdminOrderListQueryExecutor.PageResult pr =
				adminOrderListQueryExecutor.queryPage(kind, f, page, limit, false);

		if (invoiceListActive) {
			long companyId = longVal(f.get("company_id"));
			long supplierId = longVal(f.get("supplier_id"));
			for (Map<String, Object> row : pr.list()) {
				reloadItemsForInvoiceList(companyId, row, supplierId);
				maybeAppendFreightInvoiceLine(companyId, row);
			}
		}

		applyOrderListStatusForConsumerContext(pr.list(), listFrom);
		applyWxappAftersalesLogs(pr.list());

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", pr.list());
		LinkedHashMap<String, Object> pager = new LinkedHashMap<>();
		pager.put("count", pr.total());
		pager.put("page_no", page);
		pager.put("page_size", limit);
		out.put("pager", pager);
		return out;
	}

	private void applyInvoiceListOrderFilters(Map<String, Object> f) {
		long companyId = longVal(f.get("company_id"));
		f.put("invoice_status|neq", "DONE");
		f.put("total_fee|gt", 0);
		f.put("order_status", "DONE");
		f.put("cancel_status", "NO_APPLY_CANCEL");
		int invoiceOpenTerm = 6;
		Object settingRaw = invoiceSettingRedisService.getInvoiceSetting(companyId);
		if (settingRaw instanceof Map<?, ?> sm && sm.get("invoice_open_term") != null) {
			invoiceOpenTerm = intVal(sm.get("invoice_open_term"));
			if (invoiceOpenTerm <= 0) {
				invoiceOpenTerm = 6;
			}
		}
		int invoiceOpenSeconds = invoiceOpenTerm * 30 * 24 * 60 * 60;
		int boundary = (int) (System.currentTimeMillis() / 1000L) - invoiceOpenSeconds;
		f.put("create_time|gte", boundary);
	}

	private void reloadItemsForInvoiceList(long companyId, Map<String, Object> row, long supplierId) {
		long orderId = longVal(row.get("order_id"));
		if (orderId <= 0L) {
			return;
		}
		LambdaQueryWrapper<NormalOrdersItems> w =
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderId)
						.in(NormalOrdersItems::getIsInvoice, 0, 3)
						.eq(NormalOrdersItems::getRefundedFee, 0)
						.apply("CAST(total_fee AS SIGNED) > 0");
		if (supplierId > 0L) {
			w.eq(NormalOrdersItems::getSupplierId, (int) supplierId);
		}
		List<NormalOrdersItems> items = normalOrdersItemsMapper.selectList(w);
		row.put("items", buildItemMapsLikeAdminList(items));
	}

	private static List<Map<String, Object>> buildItemMapsLikeAdminList(List<NormalOrdersItems> rows) {
		List<Map<String, Object>> itemMaps = new ArrayList<>();
		if (rows == null) {
			return itemMaps;
		}
		for (NormalOrdersItems it : rows) {
			Map<String, Object> im = AdminOrderDetailPayloadMaps.itemToMap(it);
			im.put("item_holder", "self");
			im.put("supplier_name", "");
			im.put("sale_price", im.get("price"));
			int totalFee = intVal(im.get("total_fee"));
			int pointFee = intVal(im.get("point_fee"));
			int refunded = intVal(im.get("refunded_fee"));
			im.put("after_sales_fee", 0);
			im.put("remain_fee", totalFee);
			im.put("remain_point", pointFee);
			im.put("refundable_amount", totalFee - refunded);
			String ds = str(im.get("delivery_status"));
			int num = intVal(im.get("num"));
			Integer din = it.getDeliveryItemNum();
			if ("DONE".equals(ds) && (din == null || din == 0)) {
				im.put("delivery_item_num", num);
			}
			int effectiveDin = intVal(im.get("delivery_item_num"));
			im.put("delivery_item_num", Math.min(effectiveDin > 0 ? effectiveDin : 0, num));
			if (StringUtils.hasText(str(im.get("delivery_corp")))) {
				String corp = str(im.get("delivery_corp"));
				im.put("delivery_corp_name", corp);
			}
			itemMaps.add(im);
		}
		return itemMaps;
	}

	private void maybeAppendFreightInvoiceLine(long companyId, Map<String, Object> row) {
		int freight = intVal(row.get("freight_fee"));
		if (freight <= 0) {
			return;
		}
		long orderId = longVal(row.get("order_id"));
		if (wxappOrderInvoiceDetailAssembler.hasShippingFeeInvoice(companyId, orderId)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
		if (items == null) {
			items = new ArrayList<>();
			row.put("items", items);
		}
		items.add(wxappOrderDetailShippingFeeLineSupport.buildLine(row.get("freight_fee")));
	}

	private void applyOrderListStatusForConsumerContext(List<Map<String, Object>> rows, String listFrom) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : rows) {
			@SuppressWarnings("unchecked")
			Map<String, Object> dadaMap =
					row.get("dada") instanceof Map<?, ?> dm ? (Map<String, Object>) dm : new LinkedHashMap<>();
			@SuppressWarnings("unchecked")
			Map<String, Object> cancelData =
					row.get("cancelData") instanceof Map<?, ?> cm
							? (Map<String, Object>) cm
							: new LinkedHashMap<>();
			String cancelFrom = cancelData.isEmpty() ? "" : str(cancelData.get("cancel_from"));
			adminOrderDetailStatusAppApplier.apply(row, dadaMap, cancelFrom, listFrom);
		}
	}

	private static String listContextFrom(String from) {
		if (from == null || !StringUtils.hasText(from.trim())) {
			return "front_list";
		}
		return from.trim();
	}

	private void applyWxappAftersalesLogs(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> row : rows) {
			int leftNum = intVal(row.get("left_aftersales_num"));
			Object autoClose = row.get("order_auto_close_aftersales_time");
			int autoCloseInt = intVal(autoClose);
			boolean windowOk = autoCloseInt <= 0 || autoCloseInt > now;
			if (windowOk && leftNum > 0) {
				row.put("can_apply_aftersales", 1);
				log.debug("[getOrderList][can_apply_aftersales]:1 order_id={}", row.get("order_id"));
			}
			log.debug("[getOrderList][order_id]:{}", row.get("order_id"));
			log.debug("[getOrderList][order_status]:{}", row.get("order_status"));
			if ("CANCEL".equals(str(row.get("order_status")))) {
				row.put("can_apply_aftersales", 0);
				log.debug("[getOrderList][can_apply_aftersales]:0");
			}
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
}
