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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.dto.RelItemTagNameRow;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalItemsTagCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(NormalItemsTagCsvExportService.class);

	private static final int PAGE_SIZE = 500;

	private static final String EXPORT_TYPE = "normal_items_tag";

	private static final LinkedHashMap<String, String> TITLES = new LinkedHashMap<>();

	static {
		TITLES.put("item_name", "商品名称");
		TITLES.put("item_bn", "商品货号");
		TITLES.put("tag_name", "标签名称");
	}

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public NormalItemsTagCsvExportService(ItemsListQueryRepository itemsListQueryRepository,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(NormalItemsTagExportContext ctx) {
		long companyId = ctx.companyId();
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>(ctx.filterParams());
		filter.remove("merchant_id");
		if (filter.containsKey("operator_type")) {
			filter.remove("operator_type");
		}

		@SuppressWarnings("unchecked")
		List<Long> idOrDef = (List<Long>) filter.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		if (idOrDef != null) {
			if (idOrDef.isEmpty()) {
				return;
			}
			LinkedHashMap<String, Object> narrowed = new LinkedHashMap<>();
			narrowed.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
			narrowed.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, new ArrayList<>(idOrDef));
			filter = narrowed;
		}

		filter.remove("is_default");
		filter.remove("is_default_true");

		long total = itemsListQueryRepository.countByParams(filter);
		if (total <= 0) {
			return;
		}

		List<Map<String, String>> allRows = new ArrayList<>();
		for (int offset = 0; offset < total; offset += PAGE_SIZE) {
			List<Items> page = itemsListQueryRepository.selectPageByParamsForSku(filter, offset, PAGE_SIZE);
			if (page.isEmpty()) {
				break;
			}
			List<Long> defaultIds = page.stream().map(Items::getDefaultItemId).filter(Objects::nonNull).filter(id -> id > 0)
					.distinct().collect(Collectors.toList());
			Map<Long, List<String>> itemIdToTagNames = mapDefaultItemIdToTagNames(companyId, defaultIds);
			for (Items it : page) {
				Long defId = it.getDefaultItemId();
				List<String> tagNames = defId != null && defId > 0 ? itemIdToTagNames.getOrDefault(defId, List.of()) : List.of();
				LinkedHashMap<String, String> row = new LinkedHashMap<>();
				row.put("item_name", excelTextCell(nz(it.getItemName())));
				row.put("item_bn", excelTextCell(nz(it.getItemBn())));
				row.put("tag_name", String.join(",", tagNames));
				allRows.add(row);
			}
		}

		String fileBase = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "normal_items_tag";
		Map<String, String> uploaded;
		try {
			uploaded = exportCsvFileService.exportCsv(fileBase, TITLES, allRows);
		} catch (Exception e) {
			log.error("normal items tag export: csv failed companyId={}", companyId, e);
			return;
		}
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.error("normal items tag export: missing upload companyId={}", companyId);
			return;
		}
		long epochSeconds = Instant.now().getEpochSecond();
		long merchantForLog = ctx.merchantId() != null ? ctx.merchantId() : 0L;
		long supplierIdForLog =
				"supplier".equalsIgnoreCase(ctx.operatorType() != null ? ctx.operatorType() : "") ? ctx.operatorId() : 0L;
		exportLogCreateService.createFinishLog(ctx.companyId(), ctx.operatorId(), merchantForLog, supplierIdForLog,
				EXPORT_TYPE, uploaded.get("filename"), uploaded.get("url"), epochSeconds);
	}

	private Map<Long, List<String>> mapDefaultItemIdToTagNames(long companyId, List<Long> defaultIds) {
		List<RelItemTagNameRow> rows = itemsRelTagsRepository.listRelItemIdTagNameRows(companyId, defaultIds);
		Map<Long, LinkedHashSet<String>> acc = new LinkedHashMap<>();
		for (RelItemTagNameRow row : rows) {
			Long relId = row.getRelItemId();
			if (relId == null || relId <= 0) {
				continue;
			}
			String name = row.getTagName() != null ? row.getTagName() : "";
			acc.computeIfAbsent(relId, k -> new LinkedHashSet<>()).add(name);
		}
		Map<Long, List<String>> out = new LinkedHashMap<>();
		for (Map.Entry<Long, LinkedHashSet<String>> e : acc.entrySet()) {
			out.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		return out;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String excelTextCell(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return "\t" + s;
	}
}
