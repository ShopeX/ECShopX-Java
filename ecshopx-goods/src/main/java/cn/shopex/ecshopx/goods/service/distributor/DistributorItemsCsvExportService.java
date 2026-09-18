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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.goods.service.distributor.dto.DistributorItemsExportContext;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorItemsCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(DistributorItemsCsvExportService.class);
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);

	private final DistributorItemsRelListCoreService relListCore;
	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final DistributorListQueryService distributorListQueryService;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public DistributorItemsCsvExportService(
			DistributorItemsRelListCoreService relListCore,
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			DistributorListQueryService distributorListQueryService,
			MemberCardGradeMapper memberCardGradeMapper,
			VipGradeMapper vipGradeMapper,
			ObjectMapper objectMapper) {
		this.relListCore = relListCore;
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.distributorListQueryService = distributorListQueryService;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public void runExport(DistributorItemsExportContext ctx) {
		long companyId = ctx.getCompanyId();
		long distributorId = ctx.getDistributorId();
		String distributorName = resolveDistributorName(companyId, distributorId);
		List<MemberCardGrade> memberGrades = loadMemberCardGrades(companyId);
		List<VipGrade> vipGrades = loadVipGrades(companyId);
		LinkedHashMap<String, String> titles = buildTitles(memberGrades, vipGrades);

		List<Map<String, String>> csvRows = new ArrayList<>();
		boolean any = false;
		int pageSize = ctx.getPageSize();
		try {
			for (int page = 1; ; page++) {
				Map<String, Object> filterScratch = new LinkedHashMap<>();
				Map<String, Object> coreOut = relListCore.query(companyId, distributorId, ctx.getFilterBase(), pageSize,
						page, filterScratch, ctx.getAcceptLanguage());
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> list = (List<Map<String, Object>>) coreOut.get("list");
				if (list == null || list.isEmpty()) {
					break;
				}
				any = true;
				List<Long> pageItemIds = list.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).distinct()
						.toList();
				Map<Long, String> mpriceJsonByItem = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId,
						pageItemIds);
				for (Map<String, Object> row : list) {
					csvRows.add(buildCsvRow(row, titles, distributorName, mpriceJsonByItem, memberGrades, vipGrades));
				}
			}
		} catch (Exception e) {
			log.error("distributor items export failed during paging or row build", e);
			return;
		}

		if (!any) {
			return;
		}

		String fileBase = FILE_TS.format(ZonedDateTime.now(CN)) + companyId + "店铺关联商品";
		Map<String, String> uploaded;
		try {
			uploaded = exportCsvFileService.exportCsv(fileBase, titles, csvRows);
		} catch (Exception e) {
			log.error("distributor items export failed during csv upload", e);
			return;
		}
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.error("distributor items export failed: missing upload result for companyId={}", companyId,
					new IllegalStateException("empty upload"));
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		try {
			exportLogCreateService.createFinishLog(companyId, ctx.getOperatorId(), "distributor_items",
					uploaded.get("filename"), uploaded.get("url"), finishSec);
		} catch (Exception e) {
			log.error("distributor items export: export log insert failed", e);
		}
	}

	private String resolveDistributorName(long companyId, long distributorId) {
		List<Distributor> rows = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
		if (rows == null || rows.isEmpty()) {
			return "";
		}
		String n = rows.get(0).getName();
		return n != null ? n : "";
	}

	private List<MemberCardGrade> loadMemberCardGrades(long companyId) {
		LambdaQueryWrapper<MemberCardGrade> w = new LambdaQueryWrapper<>();
		w.eq(MemberCardGrade::getCompanyId, String.valueOf(companyId)).orderByAsc(MemberCardGrade::getGradeId);
		return memberCardGradeMapper.selectList(w);
	}

	private List<VipGrade> loadVipGrades(long companyId) {
		LambdaQueryWrapper<VipGrade> w = new LambdaQueryWrapper<>();
		w.apply("company_id = {0}", companyId);
		w.and(x -> x.eq(VipGrade::getIsDisabled, false).or().isNull(VipGrade::getIsDisabled));
		w.orderByAsc(VipGrade::getVipGradeId);
		return vipGradeMapper.selectList(w);
	}

	private static LinkedHashMap<String, String> buildTitles(List<MemberCardGrade> memberGrades, List<VipGrade> vipGrades) {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("item_name", "名称");
		titles.put("item_bn", "货号");
		titles.put("store", "库存");
		titles.put("is_can_sale", "上下架状态");
		titles.put("is_total_store", "店铺库存");
		titles.put("barcode", "条码");
		titles.put("distributor_name", "店铺名称");
		titles.put("price", "商品价格");
		titles.put("market_price", "市场价");
		titles.put("cost_price", "成本价");
		for (MemberCardGrade g : memberGrades) {
			if (g.getGradeId() != null) {
				String nm = g.getGradeName() != null ? g.getGradeName() : "";
				titles.put("grade_price" + g.getGradeId(), nm);
			}
		}
		for (VipGrade v : vipGrades) {
			if (v.getVipGradeId() != null) {
				String nm = v.getGradeName() != null ? v.getGradeName() : "";
				titles.put("vip_grade_price" + v.getVipGradeId(), nm);
			}
		}
		return titles;
	}

	private Map<String, String> buildCsvRow(Map<String, Object> r, LinkedHashMap<String, String> titles,
			String distributorName, Map<Long, String> mpriceJsonByItem, List<MemberCardGrade> memberGrades,
			List<VipGrade> vipGrades) {
		Map<String, String> row = new LinkedHashMap<>();
		for (String k : titles.keySet()) {
			row.put(k, "");
		}

		row.put("item_name", str(r.get("item_name")));
		row.put("item_bn", excelTextCell(str(r.get("item_bn"))));
		row.put("store", formatStore(r.get("store")));
		row.put("is_can_sale", Boolean.TRUE.equals(r.get("is_can_sale")) ? "是" : "否");
		boolean totalStore = Boolean.TRUE.equals(r.get("is_total_store"));
		row.put("is_total_store", totalStore ? "否" : "是");
		row.put("barcode", excelTextCell(str(r.get("barcode"))));
		row.put("distributor_name", distributorName);
		row.put("price", centsToYuanStr(r.get("price")));
		row.put("market_price", centsToYuanStr(r.get("market_price")));
		row.put("cost_price", centsToYuanStr(r.get("cost_price")));

		JsonNode mpriceRoot = parseMpriceRoot(mpriceJsonByItem.get(longVal(r.get("item_id"))));
		for (MemberCardGrade g : memberGrades) {
			if (g.getGradeId() == null) {
				continue;
			}
			String k = "grade_price" + g.getGradeId();
			row.put(k, tierPriceYuanFromJson(mpriceRoot, "grade", g.getGradeId()));
		}
		for (VipGrade v : vipGrades) {
			if (v.getVipGradeId() == null) {
				continue;
			}
			String k = "vip_grade_price" + v.getVipGradeId();
			row.put(k, tierPriceYuanFromJson(mpriceRoot, "vipGrade", v.getVipGradeId()));
		}
		return row;
	}

	private JsonNode parseMpriceRoot(String raw) {
		return MemberPriceColumnCodec.parseRoot(objectMapper, raw);
	}

	private String tierPriceYuanFromJson(JsonNode root, String bagName, long tierId) {
		if (root == null || !root.isObject()) {
			return "";
		}
		JsonNode bag = root.get(bagName);
		if (bag == null || !bag.isObject()) {
			return "";
		}
		JsonNode val = bag.get(Long.toString(tierId));
		if (val == null) {
			val = bag.get(String.valueOf(tierId));
		}
		if (val == null || val.isNull()) {
			return "";
		}
		long cents;
		if (val.isNumber()) {
			cents = val.longValue();
		} else {
			String t = val.asText();
			if (!StringUtils.hasText(t)) {
				return "";
			}
			try {
				cents = Long.parseLong(t.trim());
			} catch (NumberFormatException e) {
				return "";
			}
		}
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String excelTextCell(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return "\t" + s;
	}

	private static String formatStore(Object v) {
		if (v == null) {
			return "0";
		}
		if (v instanceof Number n) {
			return Long.toString(n.longValue());
		}
		return str(v);
	}

	private static String centsToYuanStr(Object v) {
		if (v == null) {
			return "";
		}
		long cents = longVal(v);
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
