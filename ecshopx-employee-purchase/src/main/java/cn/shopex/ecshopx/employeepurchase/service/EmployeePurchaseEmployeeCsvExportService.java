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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.service.DistributorEmployeeListLookupService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeAdminListQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeAdminListRow;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseEmployeeCsvExportService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final int PAGE_SIZE = 500;

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("mobile", "手机号");
		TITLE_HEADERS.put("name", "姓名");
		TITLE_HEADERS.put("auth_type", "登录类型");
		TITLE_HEADERS.put("account", "账户");
		TITLE_HEADERS.put("email", "邮箱");
		TITLE_HEADERS.put("distributor_name", "来源店铺");
		TITLE_HEADERS.put("enterprise_id", "企业ID");
		TITLE_HEADERS.put("enterprise_name", "企业名称");
		TITLE_HEADERS.put("enterprise_sn", "企业编码");
		TITLE_HEADERS.put("member_mobile", "会员手机号");
	}

	private final EmployeeAdminListQueryMapper employeeAdminListQueryMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorEmployeeListLookupService distributorEmployeeListLookupService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public EmployeePurchaseEmployeeCsvExportService(EmployeeAdminListQueryMapper employeeAdminListQueryMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorEmployeeListLookupService distributorEmployeeListLookupService,
			DistributorSelfMetaService distributorSelfMetaService,
				ExportCsvFileService exportCsvFileService, ExportLogCreateService exportLogCreateService) {
		this.employeeAdminListQueryMapper = employeeAdminListQueryMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorEmployeeListLookupService = distributorEmployeeListLookupService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(EmployeeAdminExportQuery query, long operatorId, boolean datapassBlock) {
		long companyId = query.companyId();
		long count = employeeAdminListQueryMapper.countList(companyId, query.distributorId(), query.mobile(),
				query.account(), query.email(), query.memberMobile(), query.enterpriseId());
		if (count == 0) {
			return;
		}
		List<Map<String, String>> csvRows = new ArrayList<>();
		for (int page = 1;; page++) {
			long offset = (long) (page - 1) * PAGE_SIZE;
			List<EmployeeAdminListRow> rows = employeeAdminListQueryMapper.selectListPage(companyId,
					query.distributorId(), query.mobile(), query.account(), query.email(), query.memberMobile(),
					query.enterpriseId(), offset, PAGE_SIZE);
			if (rows.isEmpty()) {
				break;
			}
			List<Map<String, Object>> pageMaps = new ArrayList<>();
			for (EmployeeAdminListRow row : rows) {
				pageMaps.add(EmployeeListService.rowToSnakeMap(row));
			}
			for (Map<String, Object> rowMap : pageMaps) {
				Object n = rowMap.get("name");
				Object m = rowMap.get("mobile");
				if (n instanceof String ns) {
					rowMap.put("name", sensitiveFieldEncryptor.decrypt(ns));
				}
				if (m instanceof String ms) {
					rowMap.put("mobile", sensitiveFieldEncryptor.decrypt(ms));
				}
			}
			Set<Integer> distIds = new LinkedHashSet<>();
			for (Map<String, Object> rowMap : pageMaps) {
				Object d = rowMap.get("distributor_id");
				if (d instanceof Number num) {
					int id = num.intValue();
					if (id >= 0) {
						distIds.add(id);
					}
				}
			}
			Map<Integer, String> idToName =
					distributorEmployeeListLookupService.distributorIdToName(companyId, distIds, PAGE_SIZE);
			Map<String, Object> selfRow = distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId);
			Object selfNameObj = selfRow.get("name");
			String selfName = selfNameObj == null ? "" : selfNameObj.toString();
			for (Map<String, Object> rowMap : pageMaps) {
				Object d = rowMap.get("distributor_id");
				int id = d instanceof Number ? ((Number) d).intValue() : -1;
				String distributorName;
				if (id == 0) {
					distributorName = selfName;
				} else {
					distributorName = idToName.getOrDefault(id, "");
				}
				rowMap.put("distributor_name", distributorName);
			}
			if (datapassBlock) {
				for (Map<String, Object> rowMap : pageMaps) {
					Object mob = rowMap.get("mobile");
					if (mob != null) {
						rowMap.put("mobile", DataMasking.maskMobile(mob.toString()));
					}
					Object mm = rowMap.get("member_mobile");
					if (mm != null) {
						rowMap.put("member_mobile", DataMasking.maskMobile(mm.toString()));
					}
					Object nm = rowMap.get("name");
					if (nm != null) {
						rowMap.put("name", DataMasking.maskTruename(nm.toString()));
					}
				}
			}
			for (Map<String, Object> rowMap : pageMaps) {
				csvRows.add(toCsvRow(rowMap));
			}
		}
		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + "_企业员工列表";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(companyId, operatorId, "employee_purchase_employees",
				uploaded.get("filename"), uploaded.get("url"), finishSec);
	}

	private static Map<String, String> toCsvRow(Map<String, Object> rowMap) {
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		for (String key : TITLE_HEADERS.keySet()) {
			out.put(key, cellForKey(key, rowMap));
		}
		return out;
	}

	private static String cellForKey(String key, Map<String, Object> rowMap) {
		if ("auth_type".equals(key)) {
			Object at = rowMap.get("auth_type");
			return authTypeLabel(at == null ? null : at.toString());
		}
		Object v = rowMap.get(key);
		if (v == null) {
			return "";
		}
		return v.toString();
	}

	private static String authTypeLabel(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "--";
		}
		return switch (raw.trim().toLowerCase()) {
			case "mobile" -> "手机号";
			case "account" -> "账号";
			case "email" -> "邮箱";
			case "qr_code" -> "二维码";
			default -> "--";
		};
	}
}
