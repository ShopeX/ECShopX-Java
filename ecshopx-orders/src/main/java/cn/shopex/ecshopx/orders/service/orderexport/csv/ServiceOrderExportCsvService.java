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

package cn.shopex.ecshopx.orders.service.orderexport.csv;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportServiceOrderQuerySupport;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServiceOrderExportCsvService {

	private static final int PAGE_SIZE = 2000;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ServiceOrdersMapper serviceOrdersMapper;
	private final ExportCsvFileService exportCsvFileService;

	public ServiceOrderExportCsvService(
			ServiceOrdersMapper serviceOrdersMapper, ExportCsvFileService exportCsvFileService) {
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> export(long companyId, LinkedHashMap<String, Object> filter) {
		LinkedHashMap<String, String> title = buildTitle();
		List<Map<String, String>> rows = new ArrayList<>();
		int pageNum = 1;
		while (true) {
			Page<ServiceOrders> page = new Page<>(pageNum, PAGE_SIZE);
			IPage<ServiceOrders> result =
					serviceOrdersMapper.selectPage(
							page, OrderExportServiceOrderQuerySupport.toListWrapper(companyId, filter));
			List<ServiceOrders> list = result.getRecords();
			if (list == null || list.isEmpty()) {
				break;
			}
			for (ServiceOrders o : list) {
				rows.add(buildRow(o));
			}
			if (list.size() < PAGE_SIZE) {
				break;
			}
			pageNum++;
		}
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + companyId + "service_order";
		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private static LinkedHashMap<String, String> buildTitle() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("order_id", "订单号");
		t.put("title", "标题");
		t.put("order_status", "订单状态");
		t.put("order_class", "订单种类");
		t.put("total_fee", "订单金额");
		t.put("mobile", "手机号");
		t.put("user_id", "用户ID");
		t.put("shop_id", "店铺ID");
		t.put("create_time", "创建时间");
		return t;
	}

	private static Map<String, String> buildRow(ServiceOrders o) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("order_id", String.valueOf(o.getOrderId()));
		m.put("title", nz(o.getTitle()));
		m.put("order_status", nz(o.getOrderStatus()));
		m.put("order_class", nz(o.getOrderClass()));
		m.put("total_fee", nz(o.getTotalFee()));
		m.put("mobile", nz(o.getMobile()));
		m.put("user_id", o.getUserId() == null ? "" : String.valueOf(o.getUserId()));
		m.put("shop_id", o.getShopId() == null ? "" : String.valueOf(o.getShopId()));
		m.put("create_time", formatTime(o.getCreateTime()));
		return m;
	}

	private static String formatTime(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return CSV_TIME.format(Instant.ofEpochSecond(epoch.longValue()).atZone(SHANGHAI));
	}

	private static String nz(String s) {
		return StringUtils.hasText(s) ? s : "";
	}
}
