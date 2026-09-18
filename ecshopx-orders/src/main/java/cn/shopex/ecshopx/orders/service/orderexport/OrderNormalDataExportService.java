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

package cn.shopex.ecshopx.orders.service.orderexport;

import cn.shopex.ecshopx.espier.service.ExportXlsxFileService;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportNormalOrderQuerySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderNormalDataExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private static final Map<String, String> ORDER_STATUS_LABEL = Map.of(
			"DONE", "订单完成",
			"NOTPAY", "未支付",
			"PAYED", "已支付",
			"CANCEL", "已取消",
			"WAIT_BUYER_CONFIRM", "待用户收货");

	private static final Map<String, String> DELIVERY_STATUS_LABEL = Map.of(
			"DONE", "已发货",
			"PENDING", "待发货",
			"PARTAIL", "部分发货");

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final ExportXlsxFileService exportXlsxFileService;

	public OrderNormalDataExportService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			ExportXlsxFileService exportXlsxFileService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.exportXlsxFileService = exportXlsxFileService;
	}

	public Map<String, String> exportOrderNormalData(long companyId, HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = buildFilter(companyId, request);
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId;

		LinkedHashMap<String, Object> countFilter = copyLinkedHashMap(filter);
		prepareMobileForRepositoryCount(countFilter);
		long count = normalOrdersMapper.selectCount(OrderExportNormalOrderQuerySupport.toCountWrapper(companyId, countFilter));
		if (count == 0L) {
			return Map.of("filename", "", "url", "");
		}

		LinkedHashMap<String, Object> listFilter = copyLinkedHashMap(filter);
		applyCheckMobile(listFilter, companyId);
		prepareMobileForRepositoryList(listFilter);

		int page = parseExportPage(request.getParameter("exportStart"));
		Integer limitOrNull = parseExportLimitOrNull(request.getParameter("exportLimit"));
		long offset = computeOffset(page, limitOrNull);

		LambdaQueryWrapper<NormalOrders> w = OrderExportNormalOrderQuerySupport.toListWrapper(companyId, listFilter);
		if (limitOrNull != null && limitOrNull > 0) {
			w.last("LIMIT " + offset + ", " + limitOrNull);
		}
		List<NormalOrders> orders = normalOrdersMapper.selectList(w);

		LinkedHashMap<String, String> headers = headerTitles();
		XSSFWorkbook wb = new XSSFWorkbook();
		try {
			Sheet sheet = wb.createSheet("Sheet1");
			Row head = sheet.createRow(0);
			int hc = 0;
			for (String title : headers.values()) {
				head.createCell(hc++).setCellValue(title);
			}
			int rowIdx = 1;
			for (NormalOrders order : orders) {
				List<NormalOrdersItems> items =
						normalOrdersItemsMapper.selectList(
								new LambdaQueryWrapper<NormalOrdersItems>()
										.eq(NormalOrdersItems::getCompanyId, companyId)
										.eq(NormalOrdersItems::getOrderId, order.getOrderId()));
				for (NormalOrdersItems item : items) {
					Row row = sheet.createRow(rowIdx++);
					writeDataRow(row, order, item);
				}
			}
			Map<String, String> uploaded = exportXlsxFileService.exportXlsx(fileBaseName, wb);
			if (uploaded == null
					|| uploaded.isEmpty()
					|| !StringUtils.hasText(uploaded.get("url"))) {
				return Map.of("filename", "", "url", "");
			}
			return uploaded;
		} catch (Exception e) {
			try {
				wb.close();
			} catch (Exception ignored) {
			}
			return Map.of("filename", "", "url", "");
		}
	}

	private void writeDataRow(Row row, NormalOrders order, NormalOrdersItems item) {
		int c = 0;
		row.createCell(c++).setCellValue("'" + order.getOrderId() + "'");
		row.createCell(c++).setCellValue(nz(order.getTitle()));
		row.createCell(c++).setCellValue(formatCreateDate(order.getCreateTime()));
		row.createCell(c++).setCellValue(displayMobile(order.getMobile()));
		row.createCell(c++).setCellValue(formatOrderTotalFeeYuan(order.getTotalFee()));
		row.createCell(c++).setCellValue(mapOrderStatus(order.getOrderStatus()));
		row.createCell(c++).setCellValue(mapDeliveryStatus(order.getDeliveryStatus()));
		row.createCell(c++).setCellValue(nz(item.getItemName()));
		row.createCell(c++).setCellValue(itemFeeYuan(item.getTotalFee()));
		row.createCell(c++).setCellValue(item.getNum() == null ? "" : String.valueOf(item.getNum()));
	}

	private static LinkedHashMap<String, String> headerTitles() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("order_id", "订单号");
		m.put("title", "订单标题");
		m.put("create_date", "下单时间");
		m.put("mobile", "手机号");
		m.put("total_fee", "订单价格");
		m.put("order_status", "订单状态");
		m.put("delivery_status", "发货状态");
		m.put("item_name", "商品名称");
		m.put("item_total_fee", "商品金额");
		m.put("item_total_num", "商品数量");
		return m;
	}

	private static LinkedHashMap<String, Object> buildFilter(long companyId, HttpServletRequest request) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		String mobileRaw = request.getParameter("mobile");
		if (mobileRaw != null && !mobileRaw.isBlank()) {
			String m = mobileRaw.trim();
			if (m.length() == 11) {
				filter.put("mobile", m);
			} else {
				filter.put("trade_id", m);
			}
		}
		filter.put("order_type", "normal");

		String shopId = request.getParameter("shop_id");
		if (shopId != null && !shopId.isBlank()) {
			filter.put("shop_id", shopId.trim());
		}

		String timeStartBegin = request.getParameter("time_start_begin");
		if (timeStartBegin != null && !timeStartBegin.isBlank()) {
			filter.put("create_time|gte", request.getParameter("time_start_begin"));
			filter.put("create_time|lte", request.getParameter("time_start_end"));
		}

		String orderId = request.getParameter("order_id");
		if (orderId != null && !orderId.isBlank()) {
			filter.put("order_id", orderId.trim());
		}

		String status = request.getParameter("status");
		if (status != null && !status.isBlank()) {
			// Query param is status; filter key is order_status for the entity column.
			filter.put("order_status", status.trim());
		}

		return filter;
	}

	private static LinkedHashMap<String, Object> copyLinkedHashMap(LinkedHashMap<String, Object> source) {
		return new LinkedHashMap<>(source);
	}

	private void prepareMobileForRepositoryCount(LinkedHashMap<String, Object> countFilter) {
		if (countFilter.containsKey("mobile")) {
			String raw = String.valueOf(countFilter.get("mobile")).trim();
			if (StringUtils.hasText(raw)) {
				countFilter.put("mobile", LegacyFixedMobileEncrypt.fixedEncryptMobile(raw));
			}
		}
	}

	private void applyCheckMobile(LinkedHashMap<String, Object> listFilter, long companyId) {
		if (!listFilter.containsKey("mobile")) {
			return;
		}
		String plain = String.valueOf(listFilter.get("mobile")).trim();
		if (!StringUtils.hasText(plain)) {
			return;
		}
		Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, plain);
		if (uid != null) {
			listFilter.remove("mobile");
			listFilter.put("user_id", uid);
		}
	}

	private void prepareMobileForRepositoryList(LinkedHashMap<String, Object> listFilter) {
		if (listFilter.containsKey("mobile")) {
			String raw = String.valueOf(listFilter.get("mobile")).trim();
			if (StringUtils.hasText(raw)) {
				listFilter.put("mobile", LegacyFixedMobileEncrypt.fixedEncryptMobile(raw));
			}
		}
	}

	private static int parseExportPage(String exportStart) {
		if (exportStart == null || exportStart.isBlank()) {
			return 1;
		}
		try {
			int p = Integer.parseInt(exportStart.trim());
			return p < 1 ? 1 : p;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static Integer parseExportLimitOrNull(String exportLimit) {
		if (exportLimit == null || exportLimit.isBlank()) {
			return null;
		}
		try {
			int l = Integer.parseInt(exportLimit.trim());
			return l <= 0 ? null : l;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long computeOffset(int page, Integer limitOrNull) {
		if (limitOrNull == null || limitOrNull <= 0) {
			return 0L;
		}
		long off = (long) (page - 1) * limitOrNull;
		return off < 0L ? 0L : off;
	}

	private static String formatCreateDate(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return DISPLAY_TIME.format(Instant.ofEpochSecond(epoch.longValue()));
	}

	private static String formatOrderTotalFeeYuan(String totalFeeCents) {
		if (!StringUtils.hasText(totalFeeCents)) {
			return "0.00";
		}
		try {
			return new BigDecimal(totalFeeCents.trim())
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					.toPlainString();
		} catch (NumberFormatException e) {
			return "0.00";
		}
	}

	private static String itemFeeYuan(Integer cents) {
		if (cents == null) {
			return "0.00";
		}
		return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String displayMobile(String stored) {
		if (stored == null) {
			return "";
		}
		try {
			String d = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(stored);
			return d == null ? "" : d;
		} catch (Exception e) {
			return stored;
		}
	}

	private static String mapOrderStatus(String code) {
		if (!StringUtils.hasText(code)) {
			return "未知状态";
		}
		return ORDER_STATUS_LABEL.getOrDefault(code.trim(), "未知状态");
	}

	private static String mapDeliveryStatus(String code) {
		if (!StringUtils.hasText(code)) {
			return "未知状态";
		}
		return DELIVERY_STATUS_LABEL.getOrDefault(code.trim(), "未知状态");
	}

	private static String nz(String s) {
		return StringUtils.hasText(s) ? s : "";
	}
}
