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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsProfit;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.mapper.ItemsProfitMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsAttrListRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoodsItemsCsvExportService {

	private static final Logger log = LoggerFactory.getLogger(GoodsItemsCsvExportService.class);
	private static final int ITEMS_DATA_EXPORT_PAGE_SIZE = 500;
	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final SupplierItemsListQueryRepository supplierItemsListQueryRepository;
	private final SupplierItemsAttrListRepository supplierItemsAttrListRepository;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;
	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsProfitMapper itemsProfitMapper;
	private final SupplierMapper supplierMapper;

	public GoodsItemsCsvExportService(ItemsListQueryRepository itemsListQueryRepository,
			SupplierItemsListQueryRepository supplierItemsListQueryRepository,
			SupplierItemsAttrListRepository supplierItemsAttrListRepository,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService,
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			MemberCardGradeMapper memberCardGradeMapper,
			VipGradeMapper vipGradeMapper,
			ObjectMapper objectMapper,
			ItemsCategoryPathByItemService itemsCategoryPathByItemService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsProfitMapper itemsProfitMapper,
			SupplierMapper supplierMapper) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.supplierItemsListQueryRepository = supplierItemsListQueryRepository;
		this.supplierItemsAttrListRepository = supplierItemsAttrListRepository;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsProfitMapper = itemsProfitMapper;
		this.supplierMapper = supplierMapper;
	}

	private record ItemsDataExportPreparedWork(LinkedHashMap<String, Object> work, boolean isGetSkuList) {
	}

	public void runExport(ItemsDataExportContext ctx) {
		ItemsDataExportPreparedWork prepared = prepareWorkMap(ctx);
		List<Map<String, String>> csvRows = new ArrayList<>();
		boolean any;
		// Platform item_source=supplier still lives in items (supplier_id>0); only supplier login reads supplier_items.
		boolean supplierOperatorCatalog = isSupplierOperator(ctx);
		try {
			if (supplierOperatorCatalog) {
				any = pagedSupplierGoodsExport(ctx, prepared.work(), csvRows);
			} else {
				any = pagedPlatformItemsExport(ctx, prepared.work(), prepared.isGetSkuList(), csvRows);
			}
		} catch (Exception e) {
			log.error("items data export failed during paging or row build", e);
			return;
		}
		if (!any) {
			return;
		}
		LinkedHashMap<String, String> titles;
		List<MemberCardGrade> memberGrades;
		List<VipGrade> vipGrades;
		if (supplierOperatorCatalog) {
			titles = buildTitleMapForSupplier();
			memberGrades = List.of();
			vipGrades = List.of();
		} else {
			memberGrades = loadMemberCardGrades(ctx.companyId());
			vipGrades = loadVipGrades(ctx.companyId());
			titles = buildTitleMapForPlatform(ctx.itemSource(), memberGrades, vipGrades);
		}
		for (Map<String, String> row : csvRows) {
			for (String k : titles.keySet()) {
				row.putIfAbsent(k, "");
			}
		}
		String fileBase = buildFileBaseName(Instant.now(), ctx.exportType());
		Map<String, String> uploaded;
		try {
			uploaded = exportCsvFileService.exportCsv(fileBase, titles, csvRows);
		} catch (Exception e) {
			log.error("items data export failed during csv upload", e);
			return;
		}
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.error("items data export: missing upload result for companyId={}", ctx.companyId(),
					new IllegalStateException("empty upload"));
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		long merchantIdForLog = ctx.merchantId() != null ? ctx.merchantId() : 0L;
		long supplierIdForLog = supplierOperatorCatalog ? ctx.operatorId() : 0L;
		try {
			exportLogCreateService.createFinishLog(ctx.companyId(), ctx.operatorId(), merchantIdForLog, supplierIdForLog,
					ctx.exportType(), uploaded.get("filename"), uploaded.get("url"), finishSec);
		} catch (Exception e) {
			log.error("items data export: export log insert failed", e);
		}
	}

	private static boolean isSupplierOperator(ItemsDataExportContext ctx) {
		return "supplier".equalsIgnoreCase(ctx.operatorType() != null ? ctx.operatorType() : "");
	}

	private ItemsDataExportPreparedWork prepareWorkMap(ItemsDataExportContext ctx) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(ctx.filterParams());
		work.put(ItemsListQueryRepository.KEY_COMPANY_ID, ctx.companyId());
		// items / supplier_items.supplier_id 存的是 operator_id，不是 supplier 表主键
		if ("supplier".equalsIgnoreCase(ctx.operatorType() != null ? ctx.operatorType() : "")) {
			work.put(ItemsListQueryRepository.KEY_SUPPLIER_ID_EQ, (int) ctx.operatorId());
		}
		work.remove("merchant_id");
		work.remove("operator_type");
		work.remove("item_source");
		work.remove("isGetSkuList");
		work.remove("is_default_true");
		work.remove("is_default");
		applyExportItemIdToDefaultItemIdNarrowing(work);
		return new ItemsDataExportPreparedWork(work, true);
	}

	/**
	 * When explicit item ids are present, keep only company_id + those ids (as default_item_id expansion via
	 * {@link ItemsListQueryRepository#KEY_ITEM_ID_OR_DEFAULT_IDS}).
	 */
	private static void applyExportItemIdToDefaultItemIdNarrowing(LinkedHashMap<String, Object> work) {
		Object idOrDef = work.get(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS);
		Object itemId = work.get("item_id");
		Object exportIds = idOrDef != null ? idOrDef : itemId;
		if (!exportItemIdsPresent(exportIds)) {
			return;
		}
		Object companyObj = work.get(ItemsListQueryRepository.KEY_COMPANY_ID);
		work.clear();
		if (companyObj != null) {
			work.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyObj);
		}
		work.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, exportIds);
	}

	private static boolean exportItemIdsPresent(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return pollingTruthyItemId(v);
	}

	private boolean pagedPlatformItemsExport(ItemsDataExportContext ctx, LinkedHashMap<String, Object> work,
			boolean ignoredIsGetSkuList, List<Map<String, String>> outRows) {
		long companyId = ctx.companyId();
		List<MemberCardGrade> memberGrades = loadMemberCardGrades(companyId);
		List<VipGrade> vipGrades = loadVipGrades(companyId);
		LinkedHashMap<String, String> titles = buildTitleMapForPlatform(ctx.itemSource(), memberGrades, vipGrades);
		long total = itemsListQueryRepository.countByParams(work);
		if (total <= 0) {
			return false;
		}
		boolean any = false;
		for (int offset = 0; offset < total; offset += ITEMS_DATA_EXPORT_PAGE_SIZE) {
			List<Items> page =
					itemsListQueryRepository.selectPageByParamsForSku(work, offset, ITEMS_DATA_EXPORT_PAGE_SIZE);
			if (page.isEmpty()) {
				break;
			}
			any = true;
			ExportBatchEnrichment plat = buildPlatformBatchEnrichment(companyId, page);
			List<Long> pageItemIds = page.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
			Map<Long, String> mpriceJson = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, pageItemIds);
			Map<Integer, String> supplierNames = loadSupplierNamesByOperatorId(companyId, page);
			for (Items it : page) {
				outRows.add(mapPlatformItemRow(it, titles, mpriceJson, memberGrades, vipGrades, plat, supplierNames, ctx));
			}
		}
		return any;
	}

	private boolean pagedSupplierGoodsExport(ItemsDataExportContext ctx, LinkedHashMap<String, Object> work,
			List<Map<String, String>> outRows) {
		work.remove("distributor_id");
		long companyId = ctx.companyId();
		LinkedHashMap<String, String> titles = buildTitleMapForSupplier();
		long total = supplierItemsListQueryRepository.countByParams(work);
		if (total <= 0) {
			return false;
		}
		boolean any = false;
		for (int offset = 0; offset < total; offset += ITEMS_DATA_EXPORT_PAGE_SIZE) {
			List<SupplierItems> page = supplierItemsListQueryRepository.selectPageByParams(work, offset,
					ITEMS_DATA_EXPORT_PAGE_SIZE);
			if (page.isEmpty()) {
				break;
			}
			any = true;
			SupplierBatchEnrichment enr = buildSupplierBatchEnrichment(companyId, page);
			for (SupplierItems it : page) {
				outRows.add(mapSupplierItemRow(it, titles, enr, ctx));
			}
		}
		return any;
	}

	private Map<String, String> mapPlatformItemRow(Items it, LinkedHashMap<String, String> titleKeysToHeader,
			Map<Long, String> mpriceJsonByItemId, List<MemberCardGrade> memberGrades, List<VipGrade> vipGrades,
			ExportBatchEnrichment plat, Map<Integer, String> supplierNamesByOperatorId, ItemsDataExportContext ctx) {
		Map<String, String> row = new LinkedHashMap<>();
		for (String k : titleKeysToHeader.keySet()) {
			row.put(k, "");
		}
		long companyId = ctx.companyId();
		long defaultItemId = it.getDefaultItemId() != null ? it.getDefaultItemId() : 0L;
		long itemId = it.getItemId() != null ? it.getItemId() : 0L;

		row.put("item_main_category", plat.mainCategoryPaths().getOrDefault(defaultItemId, ""));
		row.put("item_name", nz(it.getItemName()));
		row.put("goods_bn", excelTextCell(nz(it.getGoodsBn())));
		row.put("item_bn", excelTextCell(nz(it.getItemBn())));
		row.put("brief", nz(it.getBrief()));
		row.put("price", centsToYuanStr(it.getPrice()));
		row.put("market_price", centsToYuanStr(it.getMarketPrice()));
		row.put("cost_price", centsToYuanStr(it.getCostPrice()));
		row.put("start_num", it.getStartNum() != null ? it.getStartNum().toString() : "");
		JsonNode mpriceRoot = parseMpriceRoot(mpriceJsonByItemId.get(itemId));
		for (MemberCardGrade g : memberGrades) {
			if (g.getGradeId() == null) {
				continue;
			}
			row.put("grade_price" + g.getGradeId(), tierPriceYuanFromJson(mpriceRoot, "grade", g.getGradeId()));
		}
		for (VipGrade v : vipGrades) {
			if (v.getVipGradeId() == null) {
				continue;
			}
			row.put("vip_grade_price" + v.getVipGradeId(),
					tierPriceYuanFromJson(mpriceRoot, "vipGrade", v.getVipGradeId()));
		}
		row.put("store", it.getStore() != null ? it.getStore().toString() : "0");
		row.put("pics", formatPicsCell(it.getPics()));
		row.put("intro", formatIntroForCsv(it.getIntro()));
		row.put("spec_pics", plat.specPicsUrlByItemId().getOrDefault(itemId, ""));
		row.put("videos", "");
		row.put("goods_brand", plat.goodsBrandByDefaultItemId().getOrDefault(defaultItemId, ""));
		row.put("templates_id", plat.templateNames().getOrDefault(keyTemplates(it.getTemplatesId(), companyId), ""));
		row.put("item_category", plat.saleCategoryLabels().getOrDefault(defaultItemId, ""));
		row.put("weight", weightStr(it.getWeight()));
		row.put("barcode", excelTextCell(nz(it.getBarcode())));
		row.put("item_unit", nz(it.getItemUnit()));
		row.put("attribute_name", plat.specLabelByItemId().getOrDefault(itemId, ""));
		row.put("item_params", plat.itemParamsByDefaultItemId().getOrDefault(defaultItemId, ""));
		row.put("delivery_time",
				it.getDeliveryTime() != null && it.getDeliveryTime() > 0 ? it.getDeliveryTime().toString() : "");
		fillProfitCells(row, it, plat.profitByItemId().get(itemId));
		row.put("approve_status", approveStatusCn(it.getApproveStatus()));

		if ("supplier".equalsIgnoreCase(ctx.itemSource()) && it.getSupplierId() != null && it.getSupplierId() > 0) {
			row.put("supplier_name", supplierNamesByOperatorId.getOrDefault(it.getSupplierId(), ""));
		}

		return row;
	}

	private Map<String, String> mapSupplierItemRow(SupplierItems it, LinkedHashMap<String, String> titleKeysToHeader,
			SupplierBatchEnrichment enr, ItemsDataExportContext ctx) {
		Map<String, String> row = new LinkedHashMap<>();
		for (String k : titleKeysToHeader.keySet()) {
			row.put(k, "");
		}
		long companyId = ctx.companyId();
		long defaultItemId = it.getDefaultItemId() != null ? it.getDefaultItemId() : 0L;
		long itemId = it.getItemId() != null ? it.getItemId() : 0L;

		row.put("item_main_category", enr.mainCategoryPaths().getOrDefault(defaultItemId, ""));
		row.put("item_name", nz(it.getItemName()));
		row.put("goods_bn", excelTextCell(nz(it.getGoodsBn())));
		row.put("item_bn", excelTextCell(nz(it.getItemBn())));
		row.put("brief", nz(it.getBrief()));
		row.put("price", centsToYuanStr(it.getPrice()));
		row.put("market_price", centsToYuanStr(it.getMarketPrice()));
		row.put("cost_price", centsToYuanStr(it.getCostPrice()));
		row.put("start_num", it.getStartNum() != null ? it.getStartNum().toString() : "");
		row.put("store", it.getStore() != null ? it.getStore().toString() : "0");
		row.put("pics", formatPicsCell(it.getPics()));
		row.put("videos", "");
		row.put("goods_brand", enr.goodsBrandByDefaultItemId().getOrDefault(defaultItemId, ""));
		row.put("templates_id", enr.templateNames().getOrDefault(keyTemplates(it.getTemplatesId(), companyId), ""));
		row.put("item_category", enr.saleCategoryLabels().getOrDefault(defaultItemId, ""));
		row.put("weight", weightStr(it.getWeight()));
		row.put("barcode", excelTextCell(nz(it.getBarcode())));
		row.put("item_unit", nz(it.getItemUnit()));
		row.put("attribute_name", enr.specLabelByItemId().getOrDefault(itemId, ""));
		row.put("item_params", enr.itemParamsByDefaultItemId().getOrDefault(defaultItemId, ""));
		row.put("is_market", isMarketCn(it.getIsMarket()));
		return row;
	}

	private record ExportBatchEnrichment(
			Map<Long, String> mainCategoryPaths,
			Map<Long, String> saleCategoryLabels,
			Map<String, String> templateNames,
			Map<Long, String> itemParamsByDefaultItemId,
			Map<Long, String> goodsBrandByDefaultItemId,
			Map<Long, String> specLabelByItemId,
			Map<Long, String> specPicsUrlByItemId,
			Map<Long, ItemsProfit> profitByItemId) {
	}

	private record SupplierBatchEnrichment(
			Map<Long, String> mainCategoryPaths,
			Map<Long, String> saleCategoryLabels,
			Map<String, String> templateNames,
			Map<Long, String> itemParamsByDefaultItemId,
			Map<Long, String> goodsBrandByDefaultItemId,
			Map<Long, String> specLabelByItemId) {
	}

	private ExportBatchEnrichment buildPlatformBatchEnrichment(long companyId, List<Items> page) {
		List<Long> defaultIds = page.stream().map(Items::getDefaultItemId).filter(Objects::nonNull).distinct().toList();
		List<Long> itemIds = page.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
		Map<Long, String> mainCat = mainCategoryPathsForDefaults(companyId, page);
		Map<Long, String> saleCat = saleCategoryLabels(companyId, defaultIds);
		Map<String, String> tplNames = resolveTemplateNames(companyId, page.stream().map(Items::getTemplatesId).distinct().toList());
		AttrBundle ab = resolveAttrBundle(companyId, defaultIds, itemIds);
		Map<Long, ItemsProfit> profits = loadProfits(companyId, itemIds);
		return new ExportBatchEnrichment(mainCat, saleCat, tplNames, ab.itemParamsByDefaultItemId(), ab.goodsBrandByDefaultItemId(),
				ab.specLabelByItemId(), ab.specPicsByItemId(), profits);
	}

	private SupplierBatchEnrichment buildSupplierBatchEnrichment(long companyId, List<SupplierItems> page) {
		List<Long> defaultIds = page.stream().map(SupplierItems::getDefaultItemId).filter(Objects::nonNull).distinct().toList();
		List<Long> itemIds = page.stream().map(SupplierItems::getItemId).filter(Objects::nonNull).distinct().toList();
		Map<Long, String> mainCat = mainCategoryPathsForSupplierDefaults(companyId, page);
		Map<Long, String> saleCat = supplierSaleCategoryLabels(companyId, defaultIds);
		Map<String, String> tplNames =
				resolveTemplateNames(companyId, page.stream().map(SupplierItems::getTemplatesId).distinct().toList());
		AttrBundle ab = resolveSupplierAttrBundle(companyId, defaultIds, itemIds);
		return new SupplierBatchEnrichment(mainCat, saleCat, tplNames, ab.itemParamsByDefaultItemId(), ab.goodsBrandByDefaultItemId(),
				ab.specLabelByItemId());
	}

	/** 与 PHP 一致：销售分类来自 supplier_items_attr.category（挂在 default_item_id）。 */
	private Map<Long, String> supplierSaleCategoryLabels(long companyId, List<Long> defaultItemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (defaultItemIds.isEmpty()) {
			return out;
		}
		Map<Long, List<Long>> catsByItem = new LinkedHashMap<>();
		for (SupplierItemsAttr a : supplierItemsAttrListRepository.listCategoryAttrsByCompanyAndItemIds(companyId, defaultItemIds)) {
			if (a.getItemId() == null) {
				continue;
			}
			List<Long> ids = GoodsItemsListRowMapper.parseSupplierCategoryIds(a.getAttrData());
			if (!ids.isEmpty()) {
				catsByItem.put(a.getItemId(), ids);
			}
		}
		for (Long defId : defaultItemIds) {
			out.put(defId, formatSaleCategoryPaths(companyId, catsByItem.getOrDefault(defId, List.of())));
		}
		return out;
	}

	/** 品牌/参数/规格来自 supplier_items_attr，展开后复用平台属性名解析。 */
	private AttrBundle resolveSupplierAttrBundle(long companyId, List<Long> defaultItemIds, List<Long> itemIds) {
		List<ItemRelAttributes> relParams = expandSupplierAttrs(companyId,
				supplierItemsAttrListRepository.listByCompanyItemIdsAndAttributeType(companyId, defaultItemIds, "item_params"));
		List<ItemRelAttributes> relBrand = expandSupplierAttrs(companyId,
				supplierItemsAttrListRepository.listByCompanyItemIdsAndAttributeType(companyId, defaultItemIds, "brand"));
		List<ItemRelAttributes> relSpec = expandSupplierAttrs(companyId,
				supplierItemsAttrListRepository.listByCompanyItemIdsAndAttributeType(companyId, itemIds, "item_spec"));
		return buildAttrBundleFromRels(companyId, defaultItemIds, itemIds, relParams, relBrand, relSpec);
	}

	@SuppressWarnings("unchecked")
	private List<ItemRelAttributes> expandSupplierAttrs(long companyId, List<SupplierItemsAttr> rows) {
		List<ItemRelAttributes> out = new ArrayList<>();
		if (rows == null || rows.isEmpty()) {
			return out;
		}
		for (SupplierItemsAttr row : rows) {
			if (!StringUtils.hasText(row.getAttrData()) || row.getItemId() == null) {
				continue;
			}
			try {
				Map<String, Object> root = objectMapper.readValue(row.getAttrData(), new TypeReference<Map<String, Object>>() {});
				String at = row.getAttributeType();
				Object inner = root.get(at);
				Long rowAttributeId = row.getAttributeId();
				if ("item_params".equals(at) && inner instanceof Map<?, ?> pm && pm.get("params") instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof Map<?, ?> m) {
							out.add(syntheticSupplierRel(companyId, row.getItemId(), "item_params", (Map<String, Object>) m,
									rowAttributeId));
						}
					}
					continue;
				}
				if (inner instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof Map<?, ?> m) {
							out.add(syntheticSupplierRel(companyId, row.getItemId(), at, (Map<String, Object>) m, rowAttributeId));
						}
					}
				} else if (inner instanceof Map<?, ?> m) {
					out.add(syntheticSupplierRel(companyId, row.getItemId(), at, (Map<String, Object>) m, rowAttributeId));
				} else if ("brand".equals(at) && inner != null) {
					ItemRelAttributes rel = new ItemRelAttributes();
					rel.setCompanyId(companyId);
					rel.setItemId(row.getItemId());
					rel.setAttributeType("brand");
					rel.setAttributeId(inner instanceof Number n ? n.longValue() : Long.parseLong(inner.toString().trim()));
					rel.setAttributeValueId(0L);
					out.add(rel);
				}
			} catch (Exception ignored) {
				// skip malformed attr_data
			}
		}
		return out;
	}

	private ItemRelAttributes syntheticSupplierRel(long companyId, long itemId, String attributeType, Map<String, Object> m,
			Long rowAttributeId) {
		ItemRelAttributes rel = new ItemRelAttributes();
		rel.setCompanyId(companyId);
		rel.setItemId(itemId);
		rel.setAttributeType(attributeType);
		long attributeId = longVal(m.get("attribute_id"), 0L);
		if (attributeId <= 0L && "item_spec".equals(attributeType)) {
			attributeId = longVal(m.get("spec_id"), 0L);
		}
		if (attributeId <= 0L && rowAttributeId != null && rowAttributeId > 0L) {
			attributeId = rowAttributeId;
		}
		rel.setAttributeId(attributeId);
		long attributeValueId = longVal(m.get("attribute_value_id"), 0L);
		if (attributeValueId <= 0L && "item_spec".equals(attributeType)) {
			attributeValueId = longVal(m.get("spec_value_id"), 0L);
		}
		rel.setAttributeValueId(attributeValueId);
		Object c = m.get("custom_attribute_value");
		rel.setCustomAttributeValue(c != null ? c.toString() : null);
		Object img = m.get("image_url");
		rel.setImageUrl(img != null ? img.toString() : null);
		Object sort = m.get("attribute_sort");
		rel.setAttributeSort(sort instanceof Number n ? n.intValue() : 0);
		return rel;
	}

	private static long longVal(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return def;
		}
	}

	private Map<Long, String> mainCategoryPathsForDefaults(long companyId, List<Items> page) {
		Map<Long, String> out = new LinkedHashMap<>();
		for (Items it : page) {
			Long defId = it.getDefaultItemId();
			if (defId == null) {
				continue;
			}
			long mainCatId = parseMainCatIdFromItemCategory(it.getItemCategory());
			out.putIfAbsent(defId, pathFirstChainLabel(companyId, mainCatId, true));
		}
		return out;
	}

	private Map<Long, String> mainCategoryPathsForSupplierDefaults(long companyId, List<SupplierItems> page) {
		Map<Long, String> out = new LinkedHashMap<>();
		for (SupplierItems it : page) {
			Long defId = it.getDefaultItemId();
			if (defId == null) {
				continue;
			}
			long mainCatId = parseMainCatIdFromItemCategory(it.getItemCategory());
			out.putIfAbsent(defId, pathFirstChainLabel(companyId, mainCatId, true));
		}
		return out;
	}

	private static long parseMainCatIdFromItemCategory(String itemCategory) {
		if (!StringUtils.hasText(itemCategory)) {
			return 0L;
		}
		String t = itemCategory.trim();
		if (t.contains(",")) {
			t = t.substring(t.lastIndexOf(',') + 1).trim();
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Map<Long, String> saleCategoryLabels(long companyId, List<Long> defaultItemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (defaultItemIds.isEmpty()) {
			return out;
		}
		List<ItemsRelCats> rels = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, defaultItemIds);
		Map<Long, List<Long>> catsByItem = new LinkedHashMap<>();
		for (ItemsRelCats r : rels) {
			if (r.getItemId() == null || r.getCategoryId() == null) {
				continue;
			}
			catsByItem.computeIfAbsent(r.getItemId(), k -> new ArrayList<>()).add(r.getCategoryId());
		}
		for (Long defId : defaultItemIds) {
			List<Long> cids = catsByItem.getOrDefault(defId, List.of());
			out.put(defId, formatSaleCategoryPaths(companyId, cids));
		}
		return out;
	}

	private String formatSaleCategoryPaths(long companyId, List<Long> categoryIds) {
		if (categoryIds == null || categoryIds.isEmpty()) {
			return "";
		}
		List<String> parts = new ArrayList<>();
		for (Long cid : categoryIds) {
			if (cid != null && cid > 0) {
				String p = pathFirstChainLabel(companyId, cid, false);
				if (StringUtils.hasText(p)) {
					parts.add(p);
				}
			}
		}
		return String.join("|", parts);
	}

	private String pathFirstChainLabel(long companyId, long categoryId, boolean mainOnly) {
		if (categoryId <= 0) {
			return "";
		}
		List<Map<String, Object>> tree = itemsCategoryPathByItemService.getCategoryPathById(companyId, categoryId, mainOnly);
		return firstChainCategoryNames(tree);
	}

	@SuppressWarnings("unchecked")
	private static String firstChainCategoryNames(List<Map<String, Object>> roots) {
		if (roots == null || roots.isEmpty()) {
			return "";
		}
		List<String> names = new ArrayList<>();
		Map<String, Object> node = roots.get(0);
		while (node != null) {
			Object cn = node.get("category_name");
			if (cn != null) {
				names.add(cn.toString());
			}
			Object ch = node.get("children");
			if (ch instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map) {
				node = (Map<String, Object>) l.get(0);
			} else {
				break;
			}
		}
		return String.join("->", names);
	}

	private Map<String, String> resolveTemplateNames(long companyId, List<?> templateIds) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Object o : templateIds) {
			long tid = o instanceof Number n ? n.longValue() : 0L;
			String key = keyTemplates((int) tid, companyId);
			if (out.containsKey(key)) {
				continue;
			}
			if (tid <= 0) {
				out.put(key, "");
				continue;
			}
			out.put(key, shippingTemplatesQueryRepository.findTemplateName(tid, companyId).orElse(""));
		}
		return out;
	}

	private static String keyTemplates(Integer templatesId, long companyId) {
		int tid = templatesId != null ? templatesId : 0;
		return companyId + ":" + tid;
	}

	private record AttrBundle(
			Map<Long, String> itemParamsByDefaultItemId,
			Map<Long, String> goodsBrandByDefaultItemId,
			Map<Long, String> specLabelByItemId,
			Map<Long, String> specPicsByItemId) {
	}

	private AttrBundle resolveAttrBundle(long companyId, List<Long> defaultItemIds, List<Long> itemIds) {
		if (defaultItemIds.isEmpty() && itemIds.isEmpty()) {
			return new AttrBundle(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
		}
		List<ItemRelAttributes> relParams =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, defaultItemIds, "item_params");
		List<ItemRelAttributes> relBrand =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, defaultItemIds, "brand");
		List<ItemRelAttributes> relSpec =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, itemIds, "item_spec");
		return buildAttrBundleFromRels(companyId, defaultItemIds, itemIds, relParams, relBrand, relSpec);
	}

	private AttrBundle buildAttrBundleFromRels(long companyId, List<Long> defaultItemIds, List<Long> itemIds,
			List<ItemRelAttributes> relParams, List<ItemRelAttributes> relBrand, List<ItemRelAttributes> relSpec) {
		Map<Long, String> paramsByDef = new LinkedHashMap<>();
		Map<Long, String> brandByDef = new LinkedHashMap<>();
		Map<Long, String> specByItem = new LinkedHashMap<>();
		Map<Long, String> specPics = new LinkedHashMap<>();
		List<ItemRelAttributes> all = new ArrayList<>();
		all.addAll(relParams);
		all.addAll(relBrand);
		all.addAll(relSpec);
		if (all.isEmpty()) {
			for (Long defId : defaultItemIds) {
				paramsByDef.put(defId, "");
				brandByDef.putIfAbsent(defId, "");
			}
			for (Long iid : itemIds) {
				specByItem.put(iid, "");
				specPics.put(iid, "");
			}
			return new AttrBundle(paramsByDef, brandByDef, specByItem, specPics);
		}
		List<Long> attrIds =
				all.stream().map(ItemRelAttributes::getAttributeId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
		List<Long> valIds = all.stream().map(ItemRelAttributes::getAttributeValueId).filter(Objects::nonNull).distinct()
				.collect(Collectors.toList());
		Map<Long, ItemsAttributes> attrById =
				itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, attrIds).stream()
						.collect(Collectors.toMap(ItemsAttributes::getAttributeId, a -> a, (a, b) -> a));
		Map<Long, ItemsAttributeValues> valById = itemsAttributeValuesRepository
				.listByCompanyAndAttributeValueIdsIn(companyId, valIds).stream()
				.collect(Collectors.toMap(ItemsAttributeValues::getAttributeValueId, v -> v, (a, b) -> a));

		Map<Long, Map<Long, ParamCell>> paramCells = new LinkedHashMap<>();
		for (ItemRelAttributes r : relParams) {
			if (r.getItemId() == null) {
				continue;
			}
			ItemsAttributes def = attrById.get(r.getAttributeId());
			String an = def != null && def.getAttributeName() != null ? def.getAttributeName() : "";
			String av = "";
			if (StringUtils.hasText(r.getCustomAttributeValue())) {
				av = r.getCustomAttributeValue();
			} else if (r.getAttributeValueId() != null) {
				ItemsAttributeValues vv = valById.get(r.getAttributeValueId());
				av = vv != null && vv.getAttributeValue() != null ? vv.getAttributeValue() : "";
			}
			paramCells.computeIfAbsent(r.getItemId(), k -> new LinkedHashMap<>())
					.put(r.getAttributeId(), new ParamCell(r.getAttributeSort(), an + ":" + av));
		}
		for (Long defId : defaultItemIds) {
			Map<Long, ParamCell> cells = paramCells.get(defId);
			if (cells == null || cells.isEmpty()) {
				paramsByDef.put(defId, "");
			} else {
				String joined = cells.values().stream().sorted(Comparator.comparingInt(c -> c.sort)).map(c -> c.text)
						.collect(Collectors.joining("|"));
				paramsByDef.put(defId, joined);
			}
		}

		for (ItemRelAttributes r : relBrand) {
			if (r.getItemId() == null) {
				continue;
			}
			ItemsAttributes def = attrById.get(r.getAttributeId());
			String name = def != null && def.getAttributeName() != null ? def.getAttributeName() : "";
			brandByDef.putIfAbsent(r.getItemId(), name);
		}
		for (Long defId : defaultItemIds) {
			brandByDef.putIfAbsent(defId, "");
		}

		Map<Long, List<SpecCell>> specCells = new LinkedHashMap<>();
		for (ItemRelAttributes r : relSpec) {
			if (r.getItemId() == null) {
				continue;
			}
			ItemsAttributes def = attrById.get(r.getAttributeId());
			String sn = def != null && def.getAttributeName() != null ? def.getAttributeName() : "规格";
			String sv = "";
			if (StringUtils.hasText(r.getCustomAttributeValue())) {
				sv = r.getCustomAttributeValue();
			} else if (r.getAttributeValueId() != null) {
				ItemsAttributeValues vv = valById.get(r.getAttributeValueId());
				sv = vv != null && vv.getAttributeValue() != null ? vv.getAttributeValue() : "";
			}
			specCells.computeIfAbsent(r.getItemId(), k -> new ArrayList<>())
					.add(new SpecCell(r.getAttributeSort(), sn + ":" + sv, r.getImageUrl()));
		}
		for (Long iid : itemIds) {
			List<SpecCell> cells = specCells.get(iid);
			if (cells == null || cells.isEmpty()) {
				specByItem.put(iid, "");
				specPics.put(iid, "");
			} else {
				cells.sort(Comparator.comparingInt(c -> c.sort));
				specByItem.put(iid, cells.stream().map(c -> c.text).collect(Collectors.joining("|")));
				String pic = cells.stream().map(c -> c.imageUrl).filter(StringUtils::hasText).findFirst().orElse("");
				specPics.put(iid, pic);
			}
		}
		return new AttrBundle(paramsByDef, brandByDef, specByItem, specPics);
	}

	private record ParamCell(int sort, String text) {
	}

	private record SpecCell(int sort, String text, String imageUrl) {
	}

	private Map<Long, ItemsProfit> loadProfits(long companyId, List<Long> itemIds) {
		Map<Long, ItemsProfit> out = new LinkedHashMap<>();
		if (itemIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ItemsProfit> w = new LambdaQueryWrapper<>();
		w.eq(ItemsProfit::getCompanyId, companyId).in(ItemsProfit::getItemId, itemIds);
		for (ItemsProfit p : itemsProfitMapper.selectList(w)) {
			if (p.getItemId() != null) {
				out.put(p.getItemId(), p);
			}
		}
		return out;
	}

	private void fillProfitCells(Map<String, String> row, Items it, ItemsProfit profit) {
		boolean isProfit = Boolean.TRUE.equals(it.getIsProfit());
		row.put("is_profit", isProfit ? "1" : "0");
		row.put("profit_type", "");
		row.put("profit", "");
		row.put("popularize_profit", "");
		if (profit == null) {
			return;
		}
		row.put("profit_type", nz(profit.getProfitType()));
		if (!StringUtils.hasText(profit.getProfitConf())) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(profit.getProfitConf());
			String pt = profit.getProfitType() != null ? profit.getProfitType().trim() : "";
			if ("1".equals(pt)) {
				row.put("profit", textNode(root, "profit"));
				row.put("popularize_profit", textNode(root, "popularize_profit"));
			} else if ("2".equals(pt)) {
				row.put("profit", centsStringFromJsonNode(root.get("profit")));
				row.put("popularize_profit", centsStringFromJsonNode(root.get("popularize_profit")));
			}
		} catch (Exception ignored) {
			// keep empty profit cells
		}
	}

	private static String textNode(JsonNode root, String field) {
		if (root == null || !root.has(field) || root.get(field).isNull()) {
			return "";
		}
		return root.get(field).asText("");
	}

	private String centsStringFromJsonNode(JsonNode n) {
		if (n == null || n.isNull()) {
			return "";
		}
		long cents = n.isNumber() ? n.longValue() : parseLongSafe(n.asText());
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private Map<Integer, String> loadSupplierNamesByOperatorId(long companyId, List<Items> page) {
		List<Long> opIds = page.stream().map(Items::getSupplierId).filter(sid -> sid != null && sid > 0).map(Integer::longValue)
				.distinct().toList();
		Map<Integer, String> out = new LinkedHashMap<>();
		if (opIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<>();
		w.eq(Supplier::getCompanyId, companyId).in(Supplier::getOperatorId, opIds);
		for (Supplier s : supplierMapper.selectList(w)) {
			if (s.getOperatorId() != null) {
				out.put(s.getOperatorId().intValue(), s.getSupplierName() != null ? s.getSupplierName() : "");
			}
		}
		return out;
	}

	private LinkedHashMap<String, String> buildTitleMapForPlatform(String itemSource, List<MemberCardGrade> memberGrades,
			List<VipGrade> vipGrades) {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("item_main_category", "管理分类");
		titles.put("item_name", "商品名称");
		titles.put("goods_bn", "SPU编码");
		titles.put("item_bn", "SKU编码");
		if (StringUtils.hasText(itemSource) && "supplier".equalsIgnoreCase(itemSource.trim())) {
			titles.put("supplier_name", "供应商名称");
		}
		titles.put("brief", "简介");
		titles.put("price", "销售价");
		titles.put("market_price", "市场价");
		titles.put("cost_price", "成本价");
		titles.put("start_num", "起订量");
		for (MemberCardGrade g : memberGrades) {
			if (g.getGradeId() != null) {
				titles.put("grade_price" + g.getGradeId(), g.getGradeName() != null ? g.getGradeName() : "");
			}
		}
		for (VipGrade v : vipGrades) {
			if (v.getVipGradeId() != null) {
				titles.put("vip_grade_price" + v.getVipGradeId(), v.getGradeName() != null ? v.getGradeName() : "");
			}
		}
		titles.put("store", "库存");
		titles.put("pics", "图片");
		titles.put("intro", "详情图");
		titles.put("spec_pics", "规格图");
		titles.put("videos", "视频");
		titles.put("goods_brand", "品牌");
		titles.put("templates_id", "运费模板");
		titles.put("item_category", "分类");
		titles.put("weight", "重量");
		titles.put("barcode", "条形码");
		titles.put("item_unit", "单位");
		titles.put("attribute_name", "规格值");
		titles.put("item_params", "参数值");
		titles.put("delivery_time", "发货时间");
		titles.put("is_profit", "是否支持分润");
		titles.put("profit_type", "分润类型");
		titles.put("profit", "拉新分润");
		titles.put("popularize_profit", "推广分润");
		titles.put("approve_status", "商品状态");
		return titles;
	}

	private LinkedHashMap<String, String> buildTitleMapForSupplier() {
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("item_main_category", "管理分类");
		titles.put("item_name", "商品名称");
		titles.put("goods_bn", "SPU编码");
		titles.put("item_bn", "SKU编码");
		titles.put("brief", "简介");
		titles.put("price", "销售价");
		titles.put("market_price", "市场价");
		titles.put("cost_price", "成本价");
		titles.put("start_num", "起订量");
		titles.put("store", "库存");
		titles.put("pics", "图片");
		titles.put("videos", "视频");
		titles.put("goods_brand", "品牌");
		titles.put("templates_id", "运费模板");
		titles.put("item_category", "销售分类");
		titles.put("weight", "重量");
		titles.put("barcode", "条形码");
		titles.put("item_unit", "单位");
		titles.put("attribute_name", "规格值");
		titles.put("item_params", "参数值");
		titles.put("is_market", "供应状态");
		return titles;
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

	private String buildFileBaseName(Instant instant, String exportType) {
		String ts = FILE_TS.format(instant.atZone(CN));
		if ("supplier_goods".equals(exportType)) {
			return ts + "_supplier_items";
		}
		return ts + "items";
	}

	private static String nz(String s) {
		return s != null ? s : "";
	}

	private static String centsToYuanStr(Integer cents) {
		if (cents == null) {
			return "";
		}
		return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String weightStr(Double w) {
		if (w == null) {
			return "";
		}
		return BigDecimal.valueOf(w).stripTrailingZeros().toPlainString();
	}

	private static String approveStatusCn(String code) {
		if (!StringUtils.hasText(code)) {
			return "";
		}
		return switch (code.trim()) {
			case "onsale" -> "前台可销售";
			case "offline_sale" -> "前端不展示";
			case "instock" -> "不可销售";
			case "only_show" -> "前台仅展示";
			default -> code;
		};
	}

	private static String isMarketCn(Integer v) {
		if (v == null) {
			return "";
		}
		return v == 1 ? "可售" : "不可售";
	}

	private static String excelTextCell(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return "\t" + s;
	}

	private String formatPicsCell(String picsJson) {
		List<String> urls = parsePicsList(picsJson);
		if (urls.isEmpty()) {
			return "";
		}
		// 多图逗号分隔；勿预加引号，由 ExportCsvFileService 统一做 CSV 转义
		return String.join(",", urls);
	}

	private List<String> parsePicsList(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return List.of();
		}
		try {
			var n = objectMapper.readTree(picsJson);
			if (n.isArray()) {
				List<String> o = new ArrayList<>();
				for (var el : n) {
					if (el.isTextual()) {
						o.add(el.asText());
					}
				}
				return o;
			}
		} catch (Exception ignored) {
			// fall through
		}
		return List.of();
	}

	private String formatIntroForCsv(String intro) {
		if (!StringUtils.hasText(intro)) {
			return "";
		}
		List<String> urls = extractIntroImageUrls(intro);
		if (!urls.isEmpty()) {
			return String.join(";", urls);
		}
		String s = intro.replace("\r\n", " ").replace("\n", " ").replace("\t", " ").replace("\"", "\"\"");
		return "<!--HTML_CONTENT_START-->" + s + "<!--HTML_CONTENT_END-->";
	}

	private List<String> extractIntroImageUrls(String intro) {
		try {
			JsonNode root = objectMapper.readTree(intro.trim());
			if (!root.isArray()) {
				return List.of();
			}
			List<String> imageUrls = new ArrayList<>();
			boolean jsonFormat = false;
			for (JsonNode item : root) {
				if (!item.isObject()) {
					continue;
				}
				if (item.has("name") && "slider".equals(item.get("name").asText()) && item.has("data") && item.get("data").isArray()) {
					jsonFormat = true;
					for (JsonNode d : item.get("data")) {
						if (d.isObject() && d.has("imgUrl")) {
							imageUrls.add(d.get("imgUrl").asText());
						}
					}
				} else if (item.has("name") && "imgHotzone".equals(item.get("name").asText()) && item.has("config")) {
					JsonNode cfg = item.get("config");
					if (cfg.isObject() && cfg.has("imgUrl")) {
						jsonFormat = true;
						imageUrls.add(cfg.get("imgUrl").asText());
					}
				} else if (item.has("config") && item.get("config").isObject() && item.get("config").has("imgUrl")) {
					jsonFormat = true;
					imageUrls.add(item.get("config").get("imgUrl").asText());
				} else if (item.has("data") && item.get("data").isArray()) {
					for (JsonNode d : item.get("data")) {
						if (d.isObject() && d.has("imgUrl")) {
							jsonFormat = true;
							imageUrls.add(d.get("imgUrl").asText());
						}
					}
				}
			}
			return jsonFormat ? imageUrls : List.of();
		} catch (Exception e) {
			return List.of();
		}
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

	private static long parseLongSafe(String s) {
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	// --- 管理端轮询导出（GET exportCsvData）---

	public String exportItemsPollingFileName() {
		String ymd = LocalDate.now(CN).format(DateTimeFormatter.ofPattern("_yyyyMMdd_"));
		return "items" + ymd + ThreadLocalRandom.current().nextInt(1000, 10000);
	}

	public long requireCompanyIdForPolling(Map<String, Object> filter) {
		Object c = filter.get(ItemsListQueryRepository.KEY_COMPANY_ID);
		if (c == null) {
			throw new BadRequestException("无效导出参数");
		}
		if (c instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("无效导出参数");
			}
			return v;
		}
		String s = c.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("无效导出参数");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0) {
				throw new BadRequestException("无效导出参数");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("无效导出参数");
		}
	}

	public List<String> exportItemsPollingTitleValues(long companyId) {
		List<MemberCardGrade> memberGrades = loadMemberCardGrades(companyId);
		List<VipGrade> vipGrades = loadVipGrades(companyId);
		LinkedHashMap<String, String> titles = buildTitleMapForPlatform("", memberGrades, vipGrades);
		return new ArrayList<>(titles.values());
	}

	public int exportItemsPollingSkuCount(Map<String, Object> filterSource) {
		requireCompanyIdForPolling(filterSource);
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filterSource);
		work.remove("isGetSkuList");
		applyPollingCountItemIdNarrowing(work);
		work.remove("operator_type");
		work.remove("item_source");
		long c = itemsListQueryRepository.countByParams(work);
		if (c > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) c;
	}

	private static void applyPollingCountItemIdNarrowing(LinkedHashMap<String, Object> work) {
		if (!work.containsKey("item_id")) {
			return;
		}
		Object companyObj = work.get(ItemsListQueryRepository.KEY_COMPANY_ID);
		Object itemIdObj = work.get("item_id");
		work.clear();
		if (companyObj != null) {
			work.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyObj);
		}
		work.put("item_id", itemIdObj);
		if (pollingTruthyItemId(itemIdObj)) {
			work.put("default_item_id", itemIdObj);
			work.remove("item_id");
		}
	}

	public List<LinkedHashMap<String, String>> exportItemsPollingListRows(Map<String, Object> filterSource, int page,
			int ignoredPageSize) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(filterSource);
		long companyId = requireCompanyIdForPolling(work);
		if (pollingTruthyItemId(work.get("item_id"))) {
			work.put("default_item_id", work.get("item_id"));
			work.remove("item_id");
		}
		work.remove("is_default");
		work.remove("operator_type");
		work.remove("item_source");
		int p = page < 1 ? 1 : page;
		int offset = (p - 1) * 200;
		List<Items> pageItems = itemsListQueryRepository.selectPageByParamsForSku(work, offset, 200);
		if (pageItems.isEmpty()) {
			return List.of();
		}
		List<MemberCardGrade> memberGrades = loadMemberCardGrades(companyId);
		List<VipGrade> vipGrades = loadVipGrades(companyId);
		LinkedHashMap<String, String> titles = buildTitleMapForPlatform("", memberGrades, vipGrades);
		long operatorId = pollingExtractOperatorId(work);
		ItemsDataExportContext ctx =
				new ItemsDataExportContext(companyId, operatorId, "", null, "items", "", new LinkedHashMap<>());
		List<Long> pageItemIds = pageItems.stream().map(Items::getItemId).filter(Objects::nonNull).distinct().toList();
		Map<Long, String> mpriceJson = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, pageItemIds);
		Map<Integer, String> supplierNames = loadSupplierNamesByOperatorId(companyId, pageItems);
		ExportBatchEnrichment plat = buildPlatformBatchEnrichment(companyId, pageItems);
		List<LinkedHashMap<String, String>> out = new ArrayList<>();
		for (Items it : pageItems) {
			Map<String, String> row = mapPlatformItemRow(it, titles, mpriceJson, memberGrades, vipGrades, plat, supplierNames, ctx);
			LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
			for (String k : titles.keySet()) {
				ordered.put(k, row.getOrDefault(k, ""));
			}
			ordered.put("intro", "");
			ordered.put("spec_pics", "");
			out.add(ordered);
		}
		return out;
	}

	private static long pollingExtractOperatorId(Map<String, Object> work) {
		Object o = work.get("operator_id");
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

	private static boolean pollingTruthyItemId(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s);
	}
}
