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

package cn.shopex.ecshopx.datacube.service.deliverystaff;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.repository.OperatorsDeliveryStaffDataSupportRepository;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeliveryStaffDataCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(DeliveryStaffDataCsvExportService.class);

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter FILE_TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("staff_no", "配送员编码");
		TITLE_HEADERS.put("username", "配送员姓名");
		TITLE_HEADERS.put("mobile", "手机号");
		TITLE_HEADERS.put("distributor_names", "所属店铺");
		TITLE_HEADERS.put("payment_method", "配送结算方式");
		TITLE_HEADERS.put("user_count", "配送客户数");
		TITLE_HEADERS.put("order_count", "配送订单量");
		TITLE_HEADERS.put("payment_fee", "配送单价");
		TITLE_HEADERS.put("total_fee_count", "订单金额");
		TITLE_HEADERS.put("self_delivery_fee_count", "配送费用");
		TITLE_HEADERS.put("staff_type", "配送员类型");
		TITLE_HEADERS.put("staff_attribute", "配送员属性");
	}

	private final OperatorsDeliveryStaffDataSupportRepository operatorsDeliveryStaffDataSupportRepository;

	private final AdminDeliveryStaffDataListService adminDeliveryStaffDataListService;

	private final DistributorMapper distributorMapper;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	private final ExportCsvFileService exportCsvFileService;

	private final ExportLogCreateService exportLogCreateService;

	public DeliveryStaffDataCsvExportService(
			OperatorsDeliveryStaffDataSupportRepository operatorsDeliveryStaffDataSupportRepository,
			AdminDeliveryStaffDataListService adminDeliveryStaffDataListService,
			DistributorMapper distributorMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.operatorsDeliveryStaffDataSupportRepository = operatorsDeliveryStaffDataSupportRepository;
		this.adminDeliveryStaffDataListService = adminDeliveryStaffDataListService;
		this.distributorMapper = distributorMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(AdminDeliveryStaffDataExportFilter filter) {
		long cnt = operatorsDeliveryStaffDataSupportRepository.count(filter);
		if (cnt == 0) {
			return;
		}
		List<Map<String, String>> csvRows = new ArrayList<>();
		for (int page = 1;; page++) {
			Map<String, Object> chunk = adminDeliveryStaffDataListService.getDeliveryStaffDataList(filter, page, 500);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) chunk.get("list");
			if (list == null || list.isEmpty()) {
				break;
			}
			Set<Long> distIds = new LinkedHashSet<>();
			for (Map<String, Object> r : list) {
				collectDistributorIds(r, distIds);
			}
			Map<Long, String> distNames = loadDistributorNames(filter.getCompanyId(), distIds);
			for (Map<String, Object> r : list) {
				csvRows.add(toCsvRow(r, distNames));
			}
		}
		if (csvRows.isEmpty()) {
			return;
		}
		String fileBase = FILE_TS.format(ZonedDateTime.now(SHANGHAI)) + filter.getCompanyId() + "配送员业绩";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				filter.getCompanyId(),
				filter.getOperatorId(),
				"delivery_staffdata",
				uploaded.get("filename"),
				uploaded.get("url"),
				finishSec);
	}

	private void collectDistributorIds(Map<String, Object> row, Set<Long> out) {
		Object raw = row.get("distributor_ids");
		if (!(raw instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				Object ido = m.get("distributor_id");
				Long id = toLongId(ido);
				if (id != null && id > 0) {
					out.add(id);
				}
			}
		}
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distIds) {
		Map<Long, String> idToName = new LinkedHashMap<>();
		if (distIds.isEmpty()) {
			return idToName;
		}
		List<Distributor> rows = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, distIds)
				.select(Distributor::getDistributorId, Distributor::getName));
		for (Distributor d : rows) {
			if (d.getDistributorId() != null) {
				idToName.put(d.getDistributorId(), d.getName() != null ? d.getName() : "");
			}
		}
		return idToName;
	}

	private Map<String, String> toCsvRow(Map<String, Object> val, Map<Long, String> distNames) {
		Map<String, String> tmp = new LinkedHashMap<>();
		tmp.put("staff_no", dashOrString(val.get("staff_no")));
		tmp.put("username", dashOrString(val.get("username")));
		tmp.put("mobile", formatMobile(val.get("mobile")));
		tmp.put("distributor_names", formatDistributorNames(val, distNames));
		tmp.put("payment_method", "-");
		tmp.put("user_count", String.valueOf(toLong(val.get("user_count"))));
		tmp.put("order_count", String.valueOf(toLong(val.get("order_count"))));
		tmp.put("payment_fee", "-");
		tmp.put("total_fee_count", fenToYuanString(toLong(val.get("total_fee_count"))));
		tmp.put("self_delivery_fee_count", fenToYuanString(toLong(val.get("self_delivery_fee_count"))));
		tmp.put("staff_type", "-");
		tmp.put("staff_attribute", "-");

		Object pm = val.get("payment_method");
		if ("order".equals(pm)) {
			tmp.put("payment_method", "按单笔订单");
			int fee = toInt(val.get("payment_fee"));
			tmp.put("payment_fee", String.valueOf(fee / 100.0));
		} else if ("amount".equals(pm)) {
			tmp.put("payment_method", "按订单金额比例");
			int fee = toInt(val.get("payment_fee"));
			tmp.put("payment_fee", "%" + (fee / 100.0));
		}

		Object st = val.get("staff_type");
		if ("platform".equals(st)) {
			tmp.put("staff_type", "平台配送员");
		} else if ("distributor".equals(st)) {
			tmp.put("staff_type", "店铺配送员");
		} else if ("shop".equals(st)) {
			tmp.put("staff_type", "商家配送员");
		}

		Object sa = val.get("staff_attribute");
		if ("full_time".equals(sa)) {
			tmp.put("staff_attribute", "全职");
		} else if ("part_time".equals(sa)) {
			tmp.put("staff_attribute", "兼职");
		}

		return tmp;
	}

	private String formatMobile(Object encrypted) {
		if (encrypted == null) {
			return "-";
		}
		String s = encrypted.toString();
		if (!StringUtils.hasText(s)) {
			return "-";
		}
		try {
			String plain = sensitiveFieldEncryptor.decrypt(s);
			return StringUtils.hasText(plain) ? plain : "-";
		} catch (Exception e) {
			return "-";
		}
	}

	private static String formatDistributorNames(Map<String, Object> val, Map<Long, String> distNames) {
		Object raw = val.get("distributor_ids");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return "-";
		}
		List<String> names = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				Long id = toLongId(m.get("distributor_id"));
				if (id != null && id > 0) {
					String n = distNames.get(id);
					if (StringUtils.hasText(n)) {
						names.add(n);
					}
				}
			}
		}
		if (names.isEmpty()) {
			return "-";
		}
		return String.join(" ", names);
	}

	private static String dashOrString(Object o) {
		if (o == null) {
			return "-";
		}
		String s = o.toString();
		return StringUtils.hasText(s) ? s : "-";
	}

	private static String fenToYuanString(long fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long toLongId(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
