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

package cn.shopex.ecshopx.goods.service.pointsmall.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import cn.shopex.ecshopx.goods.service.pointsmall.export.dto.PointsmallItemsExportContext;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemsRelCats;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsRelCatsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(PointsmallItemsCsvExportService.class);
	private static final int PAGE_SIZE = 500;
	private static final String EXPORT_TYPE = "pointsmallitems";
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final ObjectMapper objectMapper;

	public PointsmallItemsCsvExportService(PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ItemsCategoryPathByItemService itemsCategoryPathByItemService,
			ObjectMapper objectMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.pointsmallItemsRelCatsMapper = pointsmallItemsRelCatsMapper;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.objectMapper = objectMapper;
	}

	public void runExport(PointsmallItemsExportContext ctx) {
		LinkedHashMap<String, Object> narrowed = narrowFilter(ctx.filterParams());
		LambdaQueryWrapper<PointsmallItems> wc = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(wc, narrowed);
		long total = pointsmallItemsMapper.selectCount(wc);
		if (total <= 0) {
			return;
		}
		LinkedHashMap<String, String> titles = buildTitles();
		List<Map<String, String>> csvRows = new ArrayList<>();
		long pages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
		long companyId = ctx.companyId();
		for (int p = 1; p <= pages; p++) {
			LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
			PointsmallItemsExportQuerySupport.applyParams(w, narrowed);
			w.orderByDesc(PointsmallItems::getItemId);
			w.last("LIMIT " + PAGE_SIZE + " OFFSET " + (PAGE_SIZE * (p - 1)));
			List<PointsmallItems> pageItems = pointsmallItemsMapper.selectList(w);
			if (pageItems.isEmpty()) {
				continue;
			}
			Map<Long, SkuExportExtras> extras = new HashMap<>();
			for (PointsmallItems it : pageItems) {
				extras.put(it.getItemId(), new SkuExportExtras());
			}
			replaceSkuSpec(companyId, pageItems, extras);
			enrichSkuRows(companyId, pageItems, extras);
			for (PointsmallItems it : pageItems) {
				csvRows.add(toCsvCells(it, extras.get(it.getItemId()), titles.keySet()));
			}
		}
		for (Map<String, String> row : csvRows) {
			for (String k : titles.keySet()) {
				row.putIfAbsent(k, "");
			}
		}
		String fileBase = FILE_TS.format(Instant.now()) + "items";
		Map<String, String> uploaded;
		try {
			uploaded = exportCsvFileService.exportCsv(fileBase, titles, csvRows);
		} catch (Exception e) {
			log.error("pointsmall items export csv upload failed", e);
			return;
		}
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.error("pointsmall items export: empty upload result companyId={}", ctx.companyId());
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		try {
			exportLogCreateService.createFinishLog(ctx.companyId(), ctx.operatorId(), EXPORT_TYPE, uploaded.get("filename"),
					uploaded.get("url"), finishSec);
		} catch (Exception e) {
			log.error("pointsmall items export log failed", e);
		}
	}

	private LinkedHashMap<String, Object> narrowFilter(LinkedHashMap<String, Object> raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(raw);
		out.remove("isGetSkuList");
		if (out.containsKey("item_id")) {
			Object itemIds = out.get("item_id");
			out.clear();
			out.put("company_id", raw.get("company_id"));
			out.put("item_id", itemIds);
		}
		Object idList = out.get("item_id");
		if (idList != null) {
			out.put("default_item_id", idList);
			out.remove("item_id");
		}
		out.remove("is_default");
		return out;
	}

	private static final class SkuExportExtras {
		String itemSpecDesc = "";
		String itemParams = "";
		String goodsBrand = "";
		String templateName = "";
		String mainCategoryPath = "";
		String displayCategoryPaths = "";
	}

	private void replaceSkuSpec(long companyId, List<PointsmallItems> list, Map<Long, SkuExportExtras> extras) {
		if (list.isEmpty()) {
			return;
		}
		List<Long> itemIds = list.stream().map(PointsmallItems::getItemId).filter(Objects::nonNull).distinct().toList();
		List<Long> defaultIds = list.stream().map(PointsmallItems::getDefaultItemId).filter(id -> id != null && id > 0)
				.distinct().toList();
		Map<Long, Integer> sumByDefault = sumStoreByDefaultIds(companyId, defaultIds);
		LambdaQueryWrapper<PointsmallItemRelAttributes> aw = new LambdaQueryWrapper<>();
		aw.eq(PointsmallItemRelAttributes::getCompanyId, companyId).in(PointsmallItemRelAttributes::getItemId, itemIds)
				.eq(PointsmallItemRelAttributes::getAttributeType, "item_spec").orderByAsc(PointsmallItemRelAttributes::getAttributeSort);
		List<PointsmallItemRelAttributes> specRels = pointsmallItemRelAttributesMapper.selectList(aw);
		Map<Long, String> specDescByItemId = buildGoodsItemSpecDesc(companyId, specRels);
		for (PointsmallItems it : list) {
			long def = it.getDefaultItemId() != null && it.getDefaultItemId() > 0 ? it.getDefaultItemId() : it.getItemId();
			it.setStore(sumByDefault.getOrDefault(def, it.getStore() != null ? it.getStore() : 0));
			SkuExportExtras ex = extras.get(it.getItemId());
			if (ex != null) {
				ex.itemSpecDesc = specDescByItemId.getOrDefault(it.getItemId(), "");
			}
		}
	}

	private Map<Long, Integer> sumStoreByDefaultIds(long companyId, List<Long> defaultIds) {
		if (defaultIds.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> rows = pointsmallItemsMapper.sumStoreByDefaultItemIds(companyId, defaultIds);
		Map<Long, Integer> m = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Object did = row.get("did");
			Object total = row.get("total");
			if (did == null || total == null) {
				continue;
			}
			long key = ((Number) did).longValue();
			int val = ((Number) total).intValue();
			m.put(key, val);
		}
		return m;
	}

	private Map<Long, String> buildGoodsItemSpecDesc(long companyId, List<PointsmallItemRelAttributes> rels) {
		if (rels.isEmpty()) {
			return Map.of();
		}
		List<Long> attrIds = rels.stream().map(PointsmallItemRelAttributes::getAttributeId).filter(Objects::nonNull).distinct()
				.toList();
		List<Long> valIds = rels.stream().map(PointsmallItemRelAttributes::getAttributeValueId).filter(id -> id != null && id > 0)
				.distinct().toList();
		Map<Long, ItemsAttributes> attrById = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, attrIds).stream()
				.collect(Collectors.toMap(ItemsAttributes::getAttributeId, a -> a, (a, b) -> a));
		Map<Long, ItemsAttributeValues> valById = itemsAttributeValuesRepository
				.listByCompanyAndAttributeValueIdsIn(companyId, valIds).stream()
				.collect(Collectors.toMap(ItemsAttributeValues::getAttributeValueId, v -> v, (a, b) -> a));
		Map<Long, List<String>> parts = new HashMap<>();
		for (PointsmallItemRelAttributes r : rels) {
			ItemsAttributes ia = attrById.get(r.getAttributeId());
			String specName = ia != null && ia.getAttributeName() != null ? ia.getAttributeName() : "-";
			String valName = "";
			if (r.getAttributeValueId() != null && r.getAttributeValueId() > 0) {
				ItemsAttributeValues iv = valById.get(r.getAttributeValueId());
				if (StringUtils.hasText(r.getCustomAttributeValue())) {
					valName = r.getCustomAttributeValue();
				} else if (iv != null && iv.getAttributeValue() != null) {
					valName = iv.getAttributeValue();
				}
			} else if (StringUtils.hasText(r.getCustomAttributeValue())) {
				valName = r.getCustomAttributeValue();
			}
			parts.computeIfAbsent(r.getItemId(), k -> new ArrayList<>()).add(specName + ":" + valName);
		}
		Map<Long, String> out = new HashMap<>();
		parts.forEach((id, ls) -> out.put(id, String.join(",", ls)));
		return out;
	}

	private void enrichSkuRows(long companyId, List<PointsmallItems> list, Map<Long, SkuExportExtras> extras) {
		if (list.isEmpty()) {
			return;
		}
		List<Long> itemIds = list.stream().map(PointsmallItems::getItemId).filter(Objects::nonNull).distinct().toList();
		LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemRelAttributes::getCompanyId, companyId).in(PointsmallItemRelAttributes::getItemId, itemIds)
				.in(PointsmallItemRelAttributes::getAttributeType, List.of("item_params", "brand"));
		List<PointsmallItemRelAttributes> rels = pointsmallItemRelAttributesMapper.selectList(w);
		PointsmallParamResolution pr = resolvePointsmallParams(companyId, rels);
		Map<Long, List<Long>> catsByItem = loadDisplayCategoryIds(companyId, itemIds);
		for (PointsmallItems it : list) {
			long iid = it.getItemId();
			long did = it.getDefaultItemId() != null && it.getDefaultItemId() > 0 ? it.getDefaultItemId() : iid;
			SkuExportExtras ex = extras.get(iid);
			if (ex == null) {
				continue;
			}
			ex.itemParams = formatItemParams(pr.itemParams, iid, did);
			String brand = pr.brandByItemId.get(iid);
			if (brand == null) {
				brand = pr.brandByItemId.get(did);
			}
			ex.goodsBrand = brand != null ? brand : "";
			int tid = it.getTemplatesId() != null ? it.getTemplatesId() : 0;
			ex.templateName = shippingTemplatesQueryRepository.findTemplateName(tid, companyId).orElse("");
			ex.mainCategoryPath = mainCategoryPath(companyId, parseLongOrZero(it.getItemCategory()));
			List<Long> cids = catsByItem.get(iid);
			if (cids == null) {
				cids = catsByItem.get(did);
			}
			ex.displayCategoryPaths = displayCategoryPaths(companyId, cids);
		}
	}

	private String formatItemParams(Map<Long, Map<Long, ParamCell>> byItem, long itemId, long defaultItemId) {
		Map<Long, ParamCell> m = byItem.get(itemId);
		if (m == null) {
			m = byItem.get(defaultItemId);
		}
		if (m == null || m.isEmpty()) {
			return "";
		}
		List<String> bits = new ArrayList<>();
		for (ParamCell c : m.values()) {
			bits.add(c.name + ":" + c.valueName);
		}
		return String.join("|", bits);
	}

	private record ParamCell(String name, String valueName) {
	}

	private static class PointsmallParamResolution {
		final Map<Long, String> brandByItemId = new HashMap<>();
		final Map<Long, Map<Long, ParamCell>> itemParams = new HashMap<>();
	}

	private PointsmallParamResolution resolvePointsmallParams(long companyId, List<PointsmallItemRelAttributes> data) {
		PointsmallParamResolution res = new PointsmallParamResolution();
		if (data.isEmpty()) {
			return res;
		}
		List<Long> attrIds = data.stream().map(PointsmallItemRelAttributes::getAttributeId).filter(Objects::nonNull).distinct()
				.toList();
		List<Long> valIds = data.stream().map(PointsmallItemRelAttributes::getAttributeValueId).filter(id -> id != null && id > 0)
				.distinct().toList();
		Map<Long, ItemsAttributes> attrById = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, attrIds).stream()
				.collect(Collectors.toMap(ItemsAttributes::getAttributeId, a -> a, (a, b) -> a));
		Map<Long, ItemsAttributeValues> valById = itemsAttributeValuesRepository.listByCompanyAndAttributeValueIdsIn(companyId, valIds)
				.stream().collect(Collectors.toMap(ItemsAttributeValues::getAttributeValueId, v -> v, (a, b) -> a));
		Map<Long, String> customByAttrId = new HashMap<>();
		for (PointsmallItemRelAttributes row : data) {
			if ("item_params".equals(row.getAttributeType()) && StringUtils.hasText(row.getCustomAttributeValue())) {
				customByAttrId.put(row.getAttributeId(), row.getCustomAttributeValue());
			}
		}
		for (PointsmallItemRelAttributes row : data) {
			if ("brand".equals(row.getAttributeType())) {
				ItemsAttributes ia = attrById.get(row.getAttributeId());
				if (ia != null) {
					res.brandByItemId.put(row.getItemId(), ia.getAttributeName());
				}
			} else if ("item_params".equals(row.getAttributeType())) {
				ItemsAttributes ia = attrById.get(row.getAttributeId());
				if (ia == null) {
					continue;
				}
				boolean hasVal = row.getAttributeValueId() != null && row.getAttributeValueId() > 0;
				String custom = customByAttrId.get(row.getAttributeId());
				if (!hasVal && !StringUtils.hasText(custom)) {
					continue;
				}
				String valName = "";
				if (StringUtils.hasText(custom)) {
					valName = custom;
				} else {
					ItemsAttributeValues iv = valById.get(row.getAttributeValueId());
					if (iv != null && iv.getAttributeValue() != null) {
						valName = iv.getAttributeValue();
					}
				}
				res.itemParams.computeIfAbsent(row.getItemId(), k -> new HashMap<>()).put(row.getAttributeId(),
						new ParamCell(ia.getAttributeName(), valName));
			}
		}
		return res;
	}

	private Map<Long, List<Long>> loadDisplayCategoryIds(long companyId, List<Long> itemIds) {
		Map<Long, List<Long>> map = new HashMap<>();
		if (itemIds.isEmpty()) {
			return map;
		}
		LambdaQueryWrapper<PointsmallItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemsRelCats::getCompanyId, companyId).in(PointsmallItemsRelCats::getItemId, itemIds);
		for (PointsmallItemsRelCats r : pointsmallItemsRelCatsMapper.selectList(w)) {
			map.computeIfAbsent(r.getItemId(), k -> new ArrayList<>()).add(r.getCategoryId());
		}
		return map;
	}

	private String mainCategoryPath(long companyId, long mainCatId) {
		if (mainCatId <= 0) {
			return "";
		}
		List<Map<String, Object>> tree = itemsCategoryPathByItemService.getCategoryPathById(companyId, mainCatId, true);
		return formatFirstBranchPath(tree);
	}

	private String displayCategoryPaths(long companyId, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return "";
		}
		List<String> paths = new ArrayList<>();
		for (Long cid : categoryIds) {
			if (cid == null || cid <= 0) {
				continue;
			}
			List<Map<String, Object>> tree = itemsCategoryPathByItemService.getCategoryPathById(companyId, cid, false);
			paths.add(formatFirstBranchPath(tree));
		}
		return String.join("|", paths);
	}

	@SuppressWarnings("unchecked")
	private String formatFirstBranchPath(List<Map<String, Object>> treeRoot) {
		if (treeRoot == null || treeRoot.isEmpty()) {
			return "";
		}
		List<String> names = new ArrayList<>();
		Map<String, Object> node = treeRoot.get(0);
		while (node != null) {
			Object nm = node.get("category_name");
			if (nm != null) {
				names.add(nm.toString());
			}
			Object ch = node.get("children");
			if (ch instanceof List<?> list && !list.isEmpty()) {
				node = (Map<String, Object>) list.get(0);
			} else {
				break;
			}
		}
		return String.join("->", names);
	}

	private Map<String, String> toCsvCells(PointsmallItems it, SkuExportExtras ex, java.util.Set<String> keys) {
		Map<String, String> row = new LinkedHashMap<>();
		String yuanPrice = centsToYuan(it.getPrice());
		String yuanMarket = centsToYuan(it.getMarketPrice());
		String yuanCost = centsToYuan(it.getCostPrice());
		String pics = formatPics(it.getPics());
		SkuExportExtras x = ex != null ? ex : new SkuExportExtras();
		for (String k : keys) {
			switch (k) {
				case "item_main_category" -> row.put(k, nullToEmpty(x.mainCategoryPath));
				case "item_name" -> row.put(k, nullToEmpty(it.getItemName()));
				case "item_bn" -> row.put(k, nullToEmpty(it.getItemBn()));
				case "brief" -> row.put(k, nullToEmpty(it.getBrief()));
				case "price" -> row.put(k, yuanPrice);
				case "market_price" -> row.put(k, yuanMarket);
				case "cost_price" -> row.put(k, yuanCost);
				case "point" -> row.put(k, it.getPoint() != null ? it.getPoint().toString() : "");
				case "store" -> row.put(k, it.getStore() != null ? it.getStore().toString() : "");
				case "pics" -> row.put(k, pics);
				case "videos" -> row.put(k, "");
				case "goods_brand" -> row.put(k, nullToEmpty(x.goodsBrand));
				case "templates_id" -> row.put(k, nullToEmpty(x.templateName));
				case "item_category" -> row.put(k, nullToEmpty(x.displayCategoryPaths));
				case "weight" -> row.put(k, it.getWeight() != null ? stripTrailingZeros(it.getWeight()) : "");
				case "barcode" -> row.put(k, nullToEmpty(it.getBarcode()));
				case "item_unit" -> row.put(k, nullToEmpty(it.getItemUnit()));
				case "attribute_name" -> row.put(k, nullToEmpty(x.itemSpecDesc));
				case "item_params" -> row.put(k, nullToEmpty(x.itemParams));
				default -> row.put(k, "");
			}
		}
		return row;
	}

	private static String nullToEmpty(String s) {
		return s != null ? s : "";
	}

	private static long parseLongOrZero(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim().split(",")[0]);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String centsToYuan(Integer cents) {
		if (cents == null) {
			return "";
		}
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String stripTrailingZeros(Double d) {
		if (d == null) {
			return "";
		}
		return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
	}

	private String formatPics(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		try {
			JsonNode n = objectMapper.readTree(picsJson);
			if (n.isArray()) {
				List<String> urls = new ArrayList<>();
				for (JsonNode x : n) {
					if (x.isTextual()) {
						urls.add(x.asText());
					} else if (x.has("url")) {
						urls.add(x.get("url").asText());
					}
				}
				return String.join(",", urls);
			}
		} catch (Exception ignored) {
		}
		return picsJson;
	}

	private static LinkedHashMap<String, String> buildTitles() {
		LinkedHashMap<String, String> t = new LinkedHashMap<>();
		t.put("item_main_category", "管理分类");
		t.put("item_name", "商品名称");
		t.put("item_bn", "商品编码");
		t.put("brief", "简介");
		t.put("price", "商品价格");
		t.put("market_price", "市场价");
		t.put("cost_price", "成本价");
		t.put("point", "积分价格");
		t.put("store", "库存");
		t.put("pics", "图片");
		t.put("videos", "视频");
		t.put("goods_brand", "品牌");
		t.put("templates_id", "运费模板");
		t.put("item_category", "分类");
		t.put("weight", "重量");
		t.put("barcode", "条形码");
		t.put("item_unit", "单位");
		t.put("attribute_name", "规格值");
		t.put("item_params", "参数值");
		return t;
	}
}
