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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.repository.NormalOrdersItemsQueryRepository;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterQueryRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EpidemicRegisterCsvExportService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(CN);
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("order_id", "订单号");
		TITLE_HEADERS.put("name", "姓名");
		TITLE_HEADERS.put("mobile", "手机号");
		TITLE_HEADERS.put("cert_id", "身份证号");
		TITLE_HEADERS.put("temperature", "体温");
		TITLE_HEADERS.put("job", "职业");
		TITLE_HEADERS.put("symptom", "症状");
		TITLE_HEADERS.put("symptom_des", "症状描述");
		TITLE_HEADERS.put("distributor_id", "店铺ID");
		TITLE_HEADERS.put("distributor_name", "店铺名称");
		TITLE_HEADERS.put("item_name", "商品名称");
		TITLE_HEADERS.put("item_bn", "商品编码");
		TITLE_HEADERS.put("barcode", "商品条码");
		TITLE_HEADERS.put("num", "商品数量");
		TITLE_HEADERS.put("is_risk_area", "14天内是否去过中高风险地区");
		TITLE_HEADERS.put("created", "登记时间");
	}

	private static final Set<String> NO_PROCESS_COLS = Set.of("name", "mobile", "temperature", "job", "symptom",
			"symptom_des", "distributor_name", "distributor_id", "created", "item_name", "item_bn", "barcode", "num");

	private final OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository;
	private final NormalOrdersItemsQueryRepository normalOrdersItemsQueryRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final DistributorMapper distributorMapper;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public EpidemicRegisterCsvExportService(OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository,
			NormalOrdersItemsQueryRepository normalOrdersItemsQueryRepository, ItemsQueryRepository itemsQueryRepository,
			DistributorMapper distributorMapper,
			ExportCsvFileService exportCsvFileService, ExportLogCreateService exportLogCreateService) {
		this.orderEpidemicRegisterQueryRepository = orderEpidemicRegisterQueryRepository;
		this.normalOrdersItemsQueryRepository = normalOrdersItemsQueryRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.distributorMapper = distributorMapper;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(OrderEpidemicRegisterListFilter filter, long operatorId, boolean datapassBlock) {
		long companyId = filter.getCompanyId();
		List<Map<String, String>> csvRows = new ArrayList<>();
		boolean any = false;
		for (int page = 1;; page++) {
			List<OrderEpidemicRegister> chunk = orderEpidemicRegisterQueryRepository.pageByFilterForExport(filter, page,
					500);
			if (chunk.isEmpty()) {
				break;
			}
			any = true;
			Set<Long> distIds = new LinkedHashSet<>();
			for (OrderEpidemicRegister r : chunk) {
				if (r.getDistributorId() != null && r.getDistributorId() > 0L) {
					distIds.add(r.getDistributorId());
				}
			}
			Map<Long, String> distNames = loadDistributorNames(companyId, distIds);
			for (OrderEpidemicRegister reg : chunk) {
				Map<String, Object> src = buildSourceRow(reg, datapassBlock, companyId, distNames);
				csvRows.add(formatRow(src));
			}
		}
		if (!any) {
			return;
		}
		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + companyId + "疫情防控登记列表";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, csvRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(companyId, operatorId, "epidemic_register", uploaded.get("filename"),
				uploaded.get("url"), finishSec);
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distIds) {
		Map<Long, String> idToName = new LinkedHashMap<>();
		if (distIds.isEmpty()) {
			return idToName;
		}
		LambdaQueryWrapper<Distributor> dw = new LambdaQueryWrapper<>();
		dw.eq(Distributor::getCompanyId, companyId).in(Distributor::getDistributorId, distIds)
				.select(Distributor::getDistributorId, Distributor::getName);
		List<Distributor> dists = distributorMapper.selectList(dw);
		for (Distributor d : dists) {
			if (d.getDistributorId() != null && StringUtils.hasText(d.getName())) {
				idToName.put(d.getDistributorId(), d.getName());
			}
		}
		return idToName;
	}

	private Map<String, Object> buildSourceRow(OrderEpidemicRegister reg, boolean datapassBlock, long companyId,
			Map<Long, String> distNames) {
		Map<String, Object> src = new LinkedHashMap<>();
		if (reg.getOrderId() != null) {
			src.put("order_id", reg.getOrderId());
		}
		if (reg.getName() != null) {
			src.put("name", reg.getName());
		}
		if (reg.getMobile() != null) {
			src.put("mobile", reg.getMobile());
		}
		if (reg.getCertId() != null) {
			src.put("cert_id", reg.getCertId());
		}
		if (reg.getTemperature() != null) {
			src.put("temperature", reg.getTemperature());
		}
		if (reg.getJob() != null) {
			src.put("job", reg.getJob());
		}
		if (reg.getSymptom() != null) {
			src.put("symptom", reg.getSymptom());
		}
		if (reg.getSymptomDes() != null) {
			src.put("symptom_des", reg.getSymptomDes());
		}
		if (reg.getDistributorId() != null) {
			src.put("distributor_id", reg.getDistributorId());
		}
		if (reg.getIsRiskArea() != null) {
			src.put("is_risk_area", reg.getIsRiskArea());
		}
		Integer created = reg.getCreated();
		if (created != null) {
			src.put("created", CREATED_FMT.format(Instant.ofEpochSecond(created.intValue())));
		}

		if (datapassBlock) {
			Object n = src.get("name");
			if (n instanceof String ns) {
				src.put("name", DataMasking.maskTruename(ns));
			}
			Object m = src.get("mobile");
			if (m instanceof String ms) {
				src.put("mobile", DataMasking.maskMobile(ms));
			}
			Object c = src.get("cert_id");
			if (c instanceof String cs) {
				src.put("cert_id", DataMasking.maskIdcard(cs));
			}
		}

		Long did = reg.getDistributorId();
		if (did != null) {
			String dn = distNames.get(did);
			if (StringUtils.hasText(dn)) {
				src.put("distributor_name", dn);
			}
		}

		long cid = reg.getCompanyId() != null ? reg.getCompanyId().longValue() : companyId;
		long uid = reg.getUserId() != null ? reg.getUserId() : 0L;
		long oid = reg.getOrderId() != null ? reg.getOrderId() : 0L;
		List<NormalOrdersItems> lines = normalOrdersItemsQueryRepository.listByCompanyUserOrder(cid, uid, oid);
		int numSum = 0;
		for (NormalOrdersItems line : lines) {
			if (line.getNum() != null) {
				numSum += line.getNum();
			}
		}
		src.put("num", String.valueOf(numSum));

		List<Long> itemIds = lines.stream().map(NormalOrdersItems::getItemId).filter(Objects::nonNull).distinct()
				.toList();
		Map<Long, Items> itemById = new LinkedHashMap<>();
		if (!itemIds.isEmpty()) {
			for (Items it : itemsQueryRepository.listByCompanyIdAndItemIds(companyId, itemIds)) {
				if (it.getItemId() != null) {
					itemById.put(it.getItemId(), it);
				}
			}
		}
		StringBuilder itemNames = new StringBuilder();
		StringBuilder itemBns = new StringBuilder();
		StringBuilder barcodes = new StringBuilder();
		for (NormalOrdersItems line : lines) {
			Long iid = line.getItemId();
			Items it = iid == null ? null : itemById.get(iid);
			itemNames.append(it != null && it.getItemName() != null ? it.getItemName() : "").append('\n');
			itemBns.append(it != null && it.getItemBn() != null ? it.getItemBn() : "").append('\n');
			String bc = it != null ? it.getBarcode() : null;
			barcodes.append(StringUtils.hasText(bc) ? bc : "--").append('\n');
		}
		src.put("item_name", itemNames.toString());
		src.put("item_bn", itemBns.toString());
		src.put("barcode", barcodes.toString());

		return src;
	}

	private Map<String, String> formatRow(Map<String, Object> src) {
		Map<String, String> out = new LinkedHashMap<>();
		for (String key : TITLE_HEADERS.keySet()) {
			out.put(key, formatCell(key, src));
		}
		return out;
	}

	private static String formatCell(String key, Map<String, Object> src) {
		boolean isset = src.containsKey(key) && src.get(key) != null;
		if (NO_PROCESS_COLS.contains(key) && isset) {
			return stringify(src.get(key));
		}
		if (("order_id".equals(key) || "cert_id".equals(key)) && isset) {
			return "\t" + stringify(src.get(key)) + "\t";
		}
		if ("is_risk_area".equals(key) && isset) {
			Object v = src.get(key);
			if (v instanceof Integer i) {
				if (i == 0) {
					return "否";
				}
				if (i == 1) {
					return "是";
				}
			}
			return "--";
		}
		return "--";
	}

	private static String stringify(Object v) {
		return v == null ? "" : v.toString();
	}
}
