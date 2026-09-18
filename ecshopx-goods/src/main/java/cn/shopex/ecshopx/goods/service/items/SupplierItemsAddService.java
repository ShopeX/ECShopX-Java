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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.SupplierLinkedItemsSyncPatch;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import cn.shopex.ecshopx.espier.service.upload.IntroHtmlDataImageUploadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemsAddService {

	private final IntroHtmlDataImageUploadService introHtmlDataImageUploadService;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final ItemSpecParamsResolver itemSpecParamsResolver;
	private final NormalItemTypeHandler normalItemTypeHandler;
	private final ServicesItemTypeHandler servicesItemTypeHandler;
	private final SupplierItemsRepository supplierItemsRepository;
	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final SupplierItemStoreService supplierItemStoreService;
	private final SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;
	private final ObjectMapper objectMapper;

	public SupplierItemsAddService(
			IntroHtmlDataImageUploadService introHtmlDataImageUploadService,
			PointMemberRuleReadService pointMemberRuleReadService,
			ItemSpecParamsResolver itemSpecParamsResolver,
			NormalItemTypeHandler normalItemTypeHandler,
			ServicesItemTypeHandler servicesItemTypeHandler,
			SupplierItemsRepository supplierItemsRepository,
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			SupplierItemStoreService supplierItemStoreService,
			SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService,
			ItemCreatePromotionGuardService itemCreatePromotionGuardService,
			ObjectMapper objectMapper) {
		this.introHtmlDataImageUploadService = introHtmlDataImageUploadService;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.itemSpecParamsResolver = itemSpecParamsResolver;
		this.normalItemTypeHandler = normalItemTypeHandler;
		this.servicesItemTypeHandler = servicesItemTypeHandler;
		this.supplierItemsRepository = supplierItemsRepository;
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.supplierItemStoreService = supplierItemStoreService;
		this.supplierItemsAttrPersistenceService = supplierItemsAttrPersistenceService;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void addItemsTransactional(Map<String, Object> params) {
		String itemType = str(params.getOrDefault("item_type", "services"));
		params.putIfAbsent("item_type", itemType);
		ItemTypeHandler handler = "normal".equals(itemType) ? normalItemTypeHandler : servicesItemTypeHandler;
		boolean gift = truthy(params.get("is_gift"));
		long companyId = toLong(params.get("company_id"));
		ItemsCreateContext ctx = new ItemsCreateContext(companyId, str(params.get("operator_type")), itemType, gift, true);

		Map<String, Object> data = supplierCommonParams(params);
		long goodsId = params.get("goods_id") != null ? toLong(params.get("goods_id")) : 0L;
		if (goodsId == 0 && params.get("item_id") != null) {
			processSupplierUpdate(toLong(params.get("item_id")), companyId, params);
			SupplierItems cur = supplierItemsRepository.getByItemIdAndCompany(toLong(params.get("item_id")), companyId);
			if (cur != null) {
				goodsId = cur.getGoodsId() != null ? cur.getGoodsId() : 0L;
			}
		}

		List<Long> itemIds = new ArrayList<>();
		Map<Long, Long> supplierPriceFen = new LinkedHashMap<>();
		Long defaultItemId = params.get("default_item_id") != null ? toLong(params.get("default_item_id")) : null;

		Map<Long, Object> specImages = parseSpecImages(params.get("spec_images"));

		if (isMultiSpec(params.get("nospec"))) {
			List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));
			if (specItems.isEmpty()) {
				throw new ResourceException("请填写正确的规格数据");
			}
			boolean forceCreate = params.get("item_id") == null;
			long minPrice = 0;
			for (Map<String, Object> row : specItems) {
				if ("supplier".equals(ctx.getOperatorType())) {
					row.put("approve_status", truthy(params.get("is_market")) ? "onsale" : "instock");
				}
				Map<String, Object> sku = merge(data, row);
				long sid = persistSupplierSku(sku, params, ctx, handler, forceCreate, specImages);
				itemIds.add(sid);
				long pf = moneyFen(row.get("price"));
				supplierPriceFen.put(sid, pf);
				if (defaultItemId == null) {
					if (minPrice == 0) {
						minPrice = pf;
						defaultItemId = sid;
					} else if (pf < minPrice) {
						minPrice = pf;
						defaultItemId = sid;
					}
				}
				syncPlatformIfNeeded(sid, sku, ctx);
			}
			if (defaultItemId == null && !itemIds.isEmpty()) {
				defaultItemId = itemIds.get(0);
			}
		} else {
			if ("supplier".equals(ctx.getOperatorType())) {
				params.put("approve_status", truthy(params.get("is_market")) ? "onsale" : "instock");
			}
			Map<String, Object> sku = merge(data, params);
			boolean forceCreate = params.get("item_id") == null;
			long sid = persistSupplierSku(sku, params, ctx, handler, forceCreate, specImages);
			itemIds.add(sid);
			supplierPriceFen.put(sid, moneyFen(params.get("price")));
			if (defaultItemId == null) {
				defaultItemId = sid;
			}
			syncPlatformIfNeeded(sid, sku, ctx);
		}

		if (goodsId == 0) {
			goodsId = defaultItemId != null ? defaultItemId : 0L;
		}

		List<Items> platformRows = itemsRepository.listBySupplierItemIds(itemIds);
		if (!platformRows.isEmpty()) {
			List<Long> platformItemIds = new ArrayList<>();
			List<Long> goodsIds = new ArrayList<>();
			Map<Long, Long> platformPrice = new LinkedHashMap<>();
			for (Items it : platformRows) {
				platformItemIds.add(it.getItemId());
				goodsIds.add(it.getGoodsId() != null ? it.getGoodsId() : 0L);
				int sid = it.getSupplierItemId() != null ? it.getSupplierItemId() : 0;
				if (sid > 0 && supplierPriceFen.containsKey((long) sid)) {
					platformPrice.put(it.getItemId(), supplierPriceFen.get((long) sid));
				}
			}
			List<Long> distinctGoodsIds = goodsIds.stream()
					.filter(g -> g != null && g > 0)
					.distinct()
					.toList();
			if (truthy(data.get("is_gift"))) {
				itemCreatePromotionGuardService.checkNotFinishedActivityValid(companyId, true, platformItemIds, distinctGoodsIds);
			}
			if (!platformPrice.isEmpty() && !distinctGoodsIds.isEmpty()) {
				itemCreatePromotionGuardService.checkItemPrice(companyId, distinctGoodsIds, platformPrice);
			}
		}

		supplierItemsRepository.updateByItemIds(itemIds, defaultItemId != null ? defaultItemId : 0L, goodsId);
		if (defaultItemId != null) {
			for (Long id : itemIds) {
				supplierItemsRepository.updateIsDefaultByItemId(companyId, id, id.equals(defaultItemId));
			}
		}

		boolean isCreateRelData = !Boolean.FALSE.equals(params.get("isCreateRelData"));
		if (isCreateRelData && defaultItemId != null) {
			applySupplierRel(params, defaultItemId);
		}
	}

	private void processSupplierUpdate(long itemId, long companyId, Map<String, Object> params) {
		SupplierItems info = supplierItemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (info == null) {
			throw new ResourceException("更新的商品无效");
		}
		long supplierId = toLong(params.get("supplier_id"));
		if (supplierId > 0 && info.getSupplierId() != null && info.getSupplierId().intValue() != supplierId) {
			throw new ResourceException("无权更新该供应商商品");
		}
		if (!isMultiSpec(params.get("nospec"))) {
			return;
		}
		List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));
		if (specItems.isEmpty()) {
			return;
		}
		List<Long> newIds = new ArrayList<>();
		for (Map<String, Object> r : specItems) {
			if (r.get("item_id") != null) {
				newIds.add(toLong(r.get("item_id")));
			}
		}
		long def = info.getDefaultItemId() != null ? info.getDefaultItemId() : itemId;
		List<SupplierItems> existing = supplierItemsRepository.listByDefaultItemIdAndCompany(def, companyId);
		List<Long> deleteIds = new ArrayList<>();
		for (SupplierItems row : existing) {
			if (!newIds.contains(row.getItemId())) {
				deleteIds.add(row.getItemId());
			}
		}
		if (!deleteIds.isEmpty()) {
			supplierItemsRepository.deleteByItemIdsAndCompany(companyId, deleteIds);
		}
	}

	private void applySupplierRel(Map<String, Object> params, long defaultItemId) {
		long companyId = toLong(params.get("company_id"));
		Object ic = params.get("item_category");
		if (ValuePresence.hasEffectiveValue(ic)) {
			List<Long> cats = parseCategoryIdsForSupplierAttr(ic);
			if (cats.isEmpty()) {
				throw new ResourceException("请选择销售分类");
			}
			supplierItemsAttrPersistenceService.saveCategoryAttr(companyId, defaultItemId, cats);
		}
		Object brandId = params.get("brand_id");
		if (brandId != null && toLong(brandId) > 0) {
			supplierItemsAttrPersistenceService.saveBrandAttr(companyId, defaultItemId, toLong(brandId));
		}
		Object ip = params.get("item_params");
		if (ip != null) {
			List<Map<String, Object>> paramRows = asMapList(ip);
			if (!paramRows.isEmpty()) {
				supplierItemsAttrPersistenceService.saveItemParamsAttr(companyId, defaultItemId, paramRows);
			}
		}
	}

	private long persistSupplierSku(
			Map<String, Object> sku,
			Map<String, Object> requestParams,
			ItemsCreateContext ctx,
			ItemTypeHandler handler,
			boolean forceCreate,
			Map<Long, Object> specImages) {
		Map<String, Object> work = new LinkedHashMap<>(sku);
		itemSpecParamsResolver.resolve(work, sku, ctx);
		handler.preRelItemParams(work, sku, ctx);
		work.put("item_category", mainCategoryColumnValue(requestParams));
		work.put("nospec", nospecStorageValue(requestParams.get("nospec")));
		SupplierItems ent = mapToSupplier(work, ctx);
		Long existing = sku.get("item_id") != null ? toLong(sku.get("item_id")) : null;
		long itemId;
		if (existing != null && existing > 0 && !forceCreate) {
			supplierItemsRepository.updateOneByItemId(existing, ctx.getCompanyId(), ent);
			itemId = existing;
		} else {
			supplierItemsRepository.insert(ent);
			itemId = ent.getItemId();
		}
		List<Map<String, Object>> specRows = asMapList(sku.get("item_spec"));
		if (!specRows.isEmpty()) {
			supplierItemsAttrPersistenceService.saveItemSpecAttr(ctx.getCompanyId(), itemId, specRows, specImages);
		}
		supplierItemStoreService.saveSupplierItemStore(itemId, toInt(work.get("store")));
		return itemId;
	}

	private void syncPlatformIfNeeded(long supplierItemId, Map<String, Object> sku, ItemsCreateContext ctx) {
		long companyId = ctx.getCompanyId();
		List<Items> linked = itemsRepository.listByCompanyIdAndSupplierItemIds(companyId, List.of(supplierItemId));
		if (linked.isEmpty()) {
			return;
		}
		List<Long> itemIds = linked.stream().map(Items::getItemId).filter(id -> id != null && id > 0).toList();
		if (itemIds.isEmpty()) {
			return;
		}
		SupplierLinkedItemsSyncPatch patch = buildLinkedItemsSyncPatch(sku);
		itemsRepository.updateLinkedItemsOnSupplierEdit(companyId, itemIds, patch);
		int store = patch.store();
		for (Items row : linked) {
			Long itemId = row.getItemId();
			if (itemId == null) {
				continue;
			}
			itemStoreService.saveItemStore(itemId, store, 0L);
		}
	}

	private SupplierLinkedItemsSyncPatch buildLinkedItemsSyncPatch(Map<String, Object> sku) {
		SupplierLinkedItemsSyncPatch.Builder b = SupplierLinkedItemsSyncPatch.builder()
				.store(toInt(sku.get("store")))
				.pics(encodeJsonColumn(sku.get("pics"), true))
				.approveStatus(str(sku.get("approve_status")))
				.itemName(str(sku.get("item_name")));
		if (sku.get("cost_price") != null && StringUtils.hasText(str(sku.get("cost_price")))) {
			int costFen = (int) moneyFen(sku.get("cost_price"));
			if (costFen >= 0) {
				b.costPrice(costFen);
			}
		}
		if (sku.get("start_num") != null && StringUtils.hasText(str(sku.get("start_num")))) {
			int startNum = parseNonNegativeInt(sku.get("start_num"), "起订量格式错误");
			b.startNum(startNum);
		}
		String auditStatus = str(sku.get("audit_status"));
		if (isSupplierEditAuditResetStatus(auditStatus)) {
			b.auditStatus(auditStatus).auditReason("").auditDate(null);
		}
		return b.build();
	}

	private static boolean isSupplierEditAuditResetStatus(String auditStatus) {
		return "submiting".equals(auditStatus) || "submitting".equals(auditStatus) || "processing".equals(auditStatus);
	}

	private SupplierItems mapToSupplier(Map<String, Object> work, ItemsCreateContext ctx) {
		SupplierItems it = new SupplierItems();
		it.setCompanyId(ctx.getCompanyId());
		it.setSupplierId((int) toLong(work.get("supplier_id")));
		it.setItemType(str(work.get("item_type")));
		it.setConsumeType(str(work.get("consume_type")));
		it.setItemName(str(work.get("item_name")));
		it.setItemBn(str(work.get("item_bn")));
		it.setGoodsBn(str(work.get("goods_bn")));
		it.setBrief(str(work.get("brief")));
		it.setSort(toInt(work.get("sort")));
		if (work.get("templates_id") != null) {
			it.setTemplatesId((int) toLong(work.get("templates_id")));
		}
		it.setIsShowSpecimg(Boolean.TRUE.equals(work.get("is_show_specimg")));
		it.setPics(encodeJsonColumn(work.get("pics"), true));
		it.setVideos(encodeJsonColumn(work.get("videos"), false));
		it.setIntro(str(work.get("intro")));
		it.setSpecialType(str(work.get("special_type")));
		it.setPurchaseAgreement(str(work.get("purchase_agreement")));
		it.setEnableAgreement(Boolean.TRUE.equals(work.get("enable_agreement")));
		it.setItemCategory(str(work.get("item_category")));
		it.setNospec(nospecStorageValue(work.get("nospec")));
		if (work.get("brand_id") != null) {
			it.setBrandId((int) toLong(work.get("brand_id")));
		}
		it.setTaxRate(ValuePresence.hasEffectiveValue(work.get("tax_rate")) ? toInt(work.get("tax_rate")) : 0);
		it.setDistributorId((int) toLong(work.get("distributor_id")));
		it.setApproveStatus(str(work.get("approve_status")));
		it.setAuditStatus(str(work.get("audit_status")));
		it.setPrice(toInt(work.get("price")));
		it.setCostPrice(work.get("cost_price") != null ? toInt(work.get("cost_price")) : 0);
		it.setMarketPrice(ValuePresence.hasEffectiveValue(work.get("market_price")) ? toInt(work.get("market_price")) : 0);
		it.setWeight(doubleValueOrZero(work.get("weight")));
		it.setBarcode(str(work.get("barcode")));
		it.setItemUnit(str(work.get("item_unit")));
		it.setStore(toInt(work.get("store")));
		it.setStartNum(work.get("start_num") != null ? toInt(work.get("start_num")) : 0);
		it.setIsDefault(Boolean.TRUE.equals(work.get("is_default")));
		it.setPoint(toInt(work.get("point")));
		it.setIsGift(ctx.isGift());
		it.setIsMarket(truthy(work.get("is_market")) ? 1 : 0);
		String crossborderRate = str(work.get("crossborder_tax_rate")).trim();
		it.setCrossborderTaxRate(crossborderRate.isEmpty() ? "" : crossborderRate);
		it.setRegionsId(regionsToCsv(work.get("regions_id")));
		it.setRegions(regionsToCsv(work.get("regions")));
		return it;
	}

	private Map<String, Object> supplierCommonParams(Map<String, Object> params) {
		Map<String, Object> data = new LinkedHashMap<>();
		long companyId = toLong(params.get("company_id"));
		String intro = str(params.get("intro"));
		if (StringUtils.hasText(intro)) {
			intro = introHtmlDataImageUploadService.replaceDataImageUrlsInIntro(intro, companyId);
			params.put("intro", intro);
		}
		Map<String, Object> pointRule = pointMemberRuleReadService.getPointRule(companyId);
		Object access = pointRule.get("access");
		data.put("point_access", access != null ? access.toString() : "order");
		data.put("company_id", companyId);
		data.put("supplier_id", params.get("supplier_id"));
		data.put("item_type", str(params.getOrDefault("item_type", "services")));
		data.put("consume_type", str(params.getOrDefault("consume_type", "every")));
		data.put("item_name", str(params.get("item_name")));
		data.put("brief", str(params.get("brief")));
		data.put("sort", params.get("sort") != null ? toInt(params.get("sort")) : 1);
		data.put("templates_id", params.get("templates_id"));
		data.put("is_show_specimg", truthy(params.get("is_show_specimg")));
		data.put("pics", params.get("pics"));
		data.put("videos", params.get("videos"));
		data.put("intro", intro);
		data.put("special_type", str(params.getOrDefault("special_type", "normal")));
		data.put("item_category", mainCategoryColumnValue(params));
		data.put("nospec", nospecStorageValue(params.get("nospec")));
		data.put("brand_id", params.get("brand_id"));
		data.put("distributor_id", params.get("distributor_id") != null ? toInt(params.get("distributor_id")) : 0);
		data.put("audit_status", str(params.get("audit_status")));
		data.put("approve_status", str(params.get("approve_status")));
		data.put("operator_type", str(params.get("operator_type")));
		data.put("is_market", params.get("is_market"));
		data.put("is_gift", params.get("is_gift"));
		data.put("goods_bn", str(params.get("goods_bn")));
		putRegionsFields(data, params);
		return data;
	}

	private static void putRegionsFields(Map<String, Object> data, Map<String, Object> params) {
		Object rid = params.get("regions_id");
		if (rid != null) {
			data.put("regions_id", regionsToCsv(rid));
		}
		Object reg = params.get("regions");
		if (reg != null) {
			data.put("regions", regionsToCsv(reg));
		}
	}

	private static String regionsToCsv(Object raw) {
		if (raw instanceof List<?> list) {
			StringBuilder sb = new StringBuilder();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(o.toString().trim());
			}
			return sb.toString();
		}
		return str(raw);
	}

	private static long moneyFen(Object price) {
		if (price == null) {
			return 0L;
		}
		String s = price.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return new java.math.BigDecimal(s).multiply(java.math.BigDecimal.valueOf(100)).longValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("价格格式错误：" + s);
		}
	}

	private static int parseNonNegativeInt(Object o, String errorMessage) {
		long v = toLong(o);
		if (v < 0 || v > Integer.MAX_VALUE) {
			throw new ResourceException(errorMessage);
		}
		return (int) v;
	}

	private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
		Map<String, Object> m = new LinkedHashMap<>(a);
		for (Map.Entry<String, Object> e : b.entrySet()) {
			m.put(e.getKey(), e.getValue());
		}
		return m;
	}

	private Map<Long, Object> parseSpecImages(Object raw) {
		if (raw == null) {
			return Map.of();
		}
		try {
			List<Map<String, Object>> rows;
			if (raw instanceof String s) {
				rows = objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
			} else {
				rows = asMapList(raw);
			}
			Map<Long, Object> out = new LinkedHashMap<>();
			for (Map<String, Object> row : rows) {
				long specValueId = toLong(row.get("spec_value_id"));
				out.put(specValueId, row.get("item_image_url"));
			}
			return out;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private List<Map<String, Object>> parseSpecItems(Object raw) {
		try {
			if (raw instanceof String s) {
				return objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
			}
			return asMapList(raw);
		} catch (Exception e) {
			return List.of();
		}
	}

	private static List<Map<String, Object>> asMapList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(row);
			}
		}
		return out;
	}

	private static List<Long> parseCategoryIdsForSupplierAttr(Object ic) {
		if (ic instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				if (o instanceof String s && !StringUtils.hasText(s.trim())) {
					continue;
				}
				try {
					long id = toLong(o);
					if (id > 0L) {
						out.add(id);
					}
				} catch (NumberFormatException e) {
					throw new ResourceException("请选择销售分类");
				}
			}
			return out;
		}
		try {
			long id = toLong(ic);
			return id > 0L ? List.of(id) : List.of();
		} catch (NumberFormatException e) {
			throw new ResourceException("请选择销售分类");
		}
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec instanceof Boolean b) {
			return !b;
		}
		if (nospec == null) {
			return false;
		}
		String s = nospec.toString();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private static boolean truthy(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return v != null && ("true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString()));
	}

	/** 主类目 ID 写入 {@code supplier_items.item_category}，销售分类走属性表。 */
	private static String mainCategoryColumnValue(Map<String, Object> params) {
		Object mainCat = params.get("item_main_cat_id");
		if (mainCat != null && StringUtils.hasText(str(mainCat).trim())) {
			return str(mainCat).trim();
		}
		return "";
	}

	private static String nospecStorageValue(Object nospec) {
		return truthy(nospec) ? "true" : "false";
	}

	/**
	 * 将 JSON 列数据序列化为合法 JSON 字符串，避免 List#toString() 输出非标准 JSON。
	 *
	 * @param nullAsEmptyJsonArray {@code true} 时 null → {@code []}（如 {@code pics}）；{@code false} 时 null → {@code ""}（如 {@code videos}）
	 */
	private String encodeJsonColumn(Object v, boolean nullAsEmptyJsonArray) {
		if (v == null) {
			return nullAsEmptyJsonArray ? "[]" : "";
		}
		if (v instanceof String s) {
			return s.trim();
		}
		try {
			return objectMapper.writeValueAsString(v);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to encode JSON column value", e);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static int toInt(Object o) {
		return (int) toLong(o);
	}

	private static double doubleValueOrZero(Object o) {
		if (o == null) {
			return 0.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0.0;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("数值格式错误：" + s);
		}
	}
}
