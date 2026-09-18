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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsCreateOrchestrator;
import cn.shopex.ecshopx.goods.service.items.ItemsUpdateOrchestrator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalGoodsImportRowOrchestrator {

	private static final Map<String, String> APPROVE_STATUS_LABEL =
			Map.of(
					"前台可销售",
					"onsale",
					"前端不展示",
					"offline_sale",
					"不可销售",
					"instock",
					"前台仅展示",
					"only_show");

	private static final ConcurrentHashMap<Long, UploadSession> SESSION = new ConcurrentHashMap<>();

	private final ItemsRepository itemsRepository;
	private final ItemsCreateOrchestrator itemsCreateOrchestrator;
	private final ItemsUpdateOrchestrator itemsUpdateOrchestrator;
	private final SupplierGoodsImportRowOrchestrator supplierGoodsImportRowOrchestrator;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;

	public NormalGoodsImportRowOrchestrator(
			ItemsRepository itemsRepository,
			ItemsCreateOrchestrator itemsCreateOrchestrator,
			ItemsUpdateOrchestrator itemsUpdateOrchestrator,
			SupplierGoodsImportRowOrchestrator supplierGoodsImportRowOrchestrator,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemRelAttributesRepository itemRelAttributesRepository) {
		this.itemsRepository = itemsRepository;
		this.itemsCreateOrchestrator = itemsCreateOrchestrator;
		this.itemsUpdateOrchestrator = itemsUpdateOrchestrator;
		this.supplierGoodsImportRowOrchestrator = supplierGoodsImportRowOrchestrator;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
	}

	public void acceptRow(long companyId, long distributorId, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		long did = distributorId > 0L ? distributorId : parseLong(r.get("distributor_id"));
		String itemBn = r.get("item_bn");

		if (StringUtils.hasText(itemBn)) {
			Items old = itemsRepository.findByItemBnAndCompany(itemBn.trim(), companyId);
			if (old != null) {
				long oldDid = old.getDistributorId() != null ? old.getDistributorId().longValue() : 0L;
				if (did != oldDid) {
					throw new BadRequestException("商品编码已存在其他店铺中，不能更新");
				}
				Map<String, Object> params = buildUpdateParams(companyId, did, r, old, row);
				itemsUpdateOrchestrator.updateItems(params, old.getItemId(), false);
				rememberSpuAnchorAfterWrite(companyId, r, row, itemBn);
				return;
			}
		}

		validateCreateRequired(r);
		Map<String, Object> params = buildCreateParams(companyId, did, r, row);
		itemsCreateOrchestrator.createItems(params);
		rememberSpuAnchorAfterWrite(companyId, r, row, itemBn);
	}

	private Map<String, Object> buildCreateParams(
			long companyId, long distributorId, Map<String, String> r, Map<String, Object> rawRow) {
		ItemsCategory mainCategory =
				supplierGoodsImportRowOrchestrator.resolveMainCategoryLeaf(companyId, r.get("item_main_category"));
		long templateId =
				supplierGoodsImportRowOrchestrator.resolveTemplateId(
						companyId, r.get("templates_id"), 0L, distributorId);
		List<Long> saleCatIds =
				supplierGoodsImportRowOrchestrator.resolveSaleCategories(
						companyId, distributorId, r.get("item_category"), false);

		boolean nospec = !StringUtils.hasText(r.get("item_spec"));
		boolean isCreateRelData = true;
		Long defaultItemId = null;
		Long goodsId = null;
		String goodsBn = r.get("goods_bn");

		SpuAnchor sessionAnchor = resolveSessionAnchor(rawRow, goodsBn);
		if (!nospec && sessionAnchor != null && sessionAnchor.defaultItemId != null && sessionAnchor.defaultItemId > 0L) {
			isCreateRelData = false;
			defaultItemId = sessionAnchor.defaultItemId;
			goodsId = sessionAnchor.goodsId != null && sessionAnchor.goodsId > 0L
					? sessionAnchor.goodsId
					: sessionAnchor.defaultItemId;
		}

		SpuAnchor dbAnchor = resolveDbSpuAnchor(companyId, goodsBn);
		if (!nospec && dbAnchor != null && dbAnchor.defaultItemId != null && dbAnchor.defaultItemId > 0L) {
			defaultItemId = dbAnchor.defaultItemId;
			goodsId = dbAnchor.goodsId != null && dbAnchor.goodsId > 0L ? dbAnchor.goodsId : dbAnchor.defaultItemId;
			isCreateRelData = false;
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("supplier_id", 0L);
		merged.put("operator_type", "");
		merged.put("item_type", "normal");
		merged.put("distributor_id", distributorId);
		merged.put("item_main_cat_id", mainCategory.getCategoryId());
		merged.put("item_name", r.get("item_name"));
		merged.put("item_bn", r.get("item_bn"));
		merged.put("goods_bn", goodsBn);
		merged.put("brief", r.get("brief"));
		merged.put("price", parseYuan(r.get("price")));
		merged.put("cost_price", parseYuan(r.get("cost_price")));
		merged.put("market_price", parseYuan(r.get("market_price")));
		merged.put("store", parseStore(r.get("store")));
		merged.put("pics", splitCommaList(r.get("pics")));
		merged.put("intro", buildIntroHtml(r.get("intro")));
		merged.put("videos", r.get("videos"));
		merged.put("item_category", saleCatIds);
		if (templateId > Integer.MAX_VALUE) {
			throw new BadRequestException("运费模板ID超出范围");
		}
		merged.put("templates_id", (int) templateId);
		merged.put("weight", parseWeight(r.get("weight")));
		merged.put("barcode", r.get("barcode"));
		merged.put("item_unit", r.get("item_unit"));
		merged.put("nospec", nospec ? "true" : "false");
		merged.put("is_default", isCreateRelData);
		merged.put("isCreateRelData", isCreateRelData);
		merged.put("approve_status", resolveApproveStatus(r.get("approve_status")));
		merged.put("start_num", parseStartNum(r.get("start_num")));
		merged.put("delivery_time", parseDeliveryTime(r.get("delivery_time")));
		merged.put("is_profit", resolveIsProfit(r.get("is_profit")));
		merged.put("profit_type", 0);
		merged.put("sort", 1);
		if (defaultItemId != null) {
			merged.put("default_item_id", defaultItemId);
		}
		if (goodsId != null && goodsId > 0L) {
			merged.put("goods_id", goodsId);
		}
		String brand = r.get("goods_brand");
		if (StringUtils.hasText(brand)) {
			int bid = itemsAttributesRepository
					.findFirstBrandByCompanyAndName(companyId, brand)
					.map(a -> a.getAttributeId() != null ? a.getAttributeId().intValue() : 0)
					.orElse(0);
			if (bid <= 0) {
				throw new BadRequestException(brand + " 品牌名称不存在");
			}
			merged.put("brand_id", bid);
		}

		if (!nospec) {
			r.put("default_item_id", defaultItemId != null ? String.valueOf(defaultItemId) : "");
			List<Map<String, Object>> itemSpec =
					supplierGoodsImportRowOrchestrator.getItemSpec(companyId, r, mainCategory, false);
			assertNoDuplicatePlatformSpec(companyId, defaultItemId, r.get("item_bn"), itemSpec);
			Map<String, Object> specItem = new LinkedHashMap<>();
			specItem.put("item_bn", r.get("item_bn"));
			specItem.put("weight", parseWeight(r.get("weight")));
			specItem.put("barcode", r.get("barcode"));
			specItem.put("price", parseYuan(r.get("price")));
			specItem.put("cost_price", parseYuan(r.get("cost_price")));
			specItem.put("market_price", parseYuan(r.get("market_price")));
			specItem.put("item_unit", r.get("item_unit"));
			specItem.put("store", parseStore(r.get("store")));
			specItem.put("is_default", isCreateRelData);
			if (defaultItemId != null) {
				specItem.put("default_item_id", defaultItemId);
			}
			specItem.put("item_spec", itemSpec);
			specItem.put("approve_status", merged.get("approve_status"));
			merged.put("spec_items", List.of(specItem));
			List<Map<String, Object>> specImages =
					supplierGoodsImportRowOrchestrator.getItemSpecImages(companyId, r, mainCategory);
			if (!specImages.isEmpty()) {
				merged.put("spec_images", specImages);
			}
		}
		return merged;
	}

	private Map<String, Object> buildUpdateParams(
			long companyId, long distributorId, Map<String, String> r, Items old, Map<String, Object> rawRow) {
		String goodsBn = StringUtils.hasText(r.get("goods_bn")) ? r.get("goods_bn").trim() : nz(old.getGoodsBn(), "").trim();
		long defId = old.getDefaultItemId() != null && old.getDefaultItemId() > 0L
				? old.getDefaultItemId()
				: old.getItemId();
		Long goodsId = old.getGoodsId() != null && old.getGoodsId() > 0L ? old.getGoodsId() : defId;

		// 同 SPU 编码归并到锚点，避免已拆成多商品的脏数据按自身 ID 继续更新
		SpuAnchor sessionAnchor = resolveSessionAnchor(rawRow, goodsBn);
		if (sessionAnchor != null && sessionAnchor.defaultItemId != null && sessionAnchor.defaultItemId > 0L) {
			defId = sessionAnchor.defaultItemId;
			goodsId = sessionAnchor.goodsId != null && sessionAnchor.goodsId > 0L
					? sessionAnchor.goodsId
					: sessionAnchor.defaultItemId;
		}
		SpuAnchor dbAnchor = resolveDbSpuAnchor(companyId, goodsBn);
		if (dbAnchor != null && dbAnchor.defaultItemId != null && dbAnchor.defaultItemId > 0L) {
			defId = dbAnchor.defaultItemId;
			goodsId = dbAnchor.goodsId != null && dbAnchor.goodsId > 0L ? dbAnchor.goodsId : dbAnchor.defaultItemId;
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("supplier_id", 0L);
		merged.put("operator_type", "");
		merged.put("item_type", "normal");
		merged.put("item_id", old.getItemId());
		merged.put("goods_id", goodsId);
		merged.put("default_item_id", defId);
		merged.put("is_default", Objects.equals(old.getItemId(), defId));
		merged.put("distributor_id", distributorId);
		merged.put("item_bn", nz(old.getItemBn(), r.get("item_bn")));
		merged.put("goods_bn", goodsBn);
		merged.put("item_name", StringUtils.hasText(r.get("item_name")) ? r.get("item_name") : nz(old.getItemName(), ""));
		merged.put("brief", StringUtils.hasText(r.get("brief")) ? r.get("brief") : nz(old.getBrief(), ""));
		merged.put("nospec", nz(old.getNospec(), "true"));
		merged.put("isCreateRelData", false);
		merged.put("price", fenToYuan(old.getPrice()));
		merged.put("cost_price", fenToYuan(old.getCostPrice()));
		merged.put("market_price", fenToYuan(old.getMarketPrice()));
		merged.put("store", old.getStore() != null ? old.getStore() : 0);
		merged.put("approve_status", nz(old.getApproveStatus(), "onsale"));

		ItemsCategory mainCategory = null;
		if (StringUtils.hasText(r.get("item_main_category"))) {
			mainCategory = supplierGoodsImportRowOrchestrator.resolveMainCategoryLeaf(companyId, r.get("item_main_category"));
			merged.put("item_main_cat_id", mainCategory.getCategoryId());
		}
		if (StringUtils.hasText(r.get("item_category"))) {
			merged.put(
					"item_category",
					supplierGoodsImportRowOrchestrator.resolveSaleCategories(
							companyId, distributorId, r.get("item_category"), false));
		}
		if (StringUtils.hasText(r.get("templates_id"))) {
			long tid =
					supplierGoodsImportRowOrchestrator.resolveTemplateId(
							companyId, r.get("templates_id"), 0L, distributorId);
			if (tid > Integer.MAX_VALUE) {
				throw new BadRequestException("运费模板ID超出范围");
			}
			merged.put("templates_id", (int) tid);
		}
		if (StringUtils.hasText(r.get("price"))) {
			merged.put("price", parseYuan(r.get("price")));
		}
		if (StringUtils.hasText(r.get("cost_price"))) {
			merged.put("cost_price", parseYuan(r.get("cost_price")));
		}
		if (StringUtils.hasText(r.get("market_price"))) {
			merged.put("market_price", parseYuan(r.get("market_price")));
		}
		if (StringUtils.hasText(r.get("store"))) {
			merged.put("store", parseStore(r.get("store")));
		}
		if (StringUtils.hasText(r.get("pics"))) {
			merged.put("pics", splitCommaList(r.get("pics")));
		}
		if (StringUtils.hasText(r.get("intro"))) {
			merged.put("intro", buildIntroHtml(r.get("intro")));
		}
		if (StringUtils.hasText(r.get("videos"))) {
			merged.put("videos", r.get("videos"));
		}
		if (StringUtils.hasText(r.get("weight"))) {
			merged.put("weight", parseWeight(r.get("weight")));
		}
		if (StringUtils.hasText(r.get("barcode"))) {
			merged.put("barcode", r.get("barcode"));
		}
		if (StringUtils.hasText(r.get("item_unit"))) {
			merged.put("item_unit", r.get("item_unit"));
		}
		if (StringUtils.hasText(r.get("start_num"))) {
			merged.put("start_num", parseStartNum(r.get("start_num")));
		}
		if (StringUtils.hasText(r.get("delivery_time"))) {
			merged.put("delivery_time", parseDeliveryTime(r.get("delivery_time")));
		}
		if (StringUtils.hasText(r.get("approve_status"))) {
			merged.put("approve_status", resolveApproveStatusStrict(r.get("approve_status")));
		}
		if (StringUtils.hasText(r.get("is_profit"))) {
			merged.put("is_profit", resolveIsProfit(r.get("is_profit")));
		}
		if (StringUtils.hasText(r.get("goods_brand"))) {
			int bid = itemsAttributesRepository
					.findFirstBrandByCompanyAndName(companyId, r.get("goods_brand"))
					.map(a -> a.getAttributeId() != null ? a.getAttributeId().intValue() : 0)
					.orElse(0);
			if (bid <= 0) {
				throw new BadRequestException(r.get("goods_brand") + " 品牌名称不存在");
			}
			merged.put("brand_id", bid);
		}

		if (StringUtils.hasText(r.get("item_spec")) && mainCategory != null) {
			r.put("default_item_id", String.valueOf(defId));
			List<Map<String, Object>> itemSpec =
					supplierGoodsImportRowOrchestrator.getItemSpec(companyId, r, mainCategory, false);
			assertNoDuplicatePlatformSpec(companyId, defId, r.get("item_bn"), itemSpec);
			merged.put("nospec", "false");
			List<Items> siblings = StringUtils.hasText(goodsBn)
					? itemsRepository.listByGoodsBnAndCompany(goodsBn, companyId)
					: itemsRepository.listByDefaultItemIdAndCompany(defId, companyId);
			if (siblings.isEmpty()) {
				siblings = List.of(old);
			} else if (siblings.stream().noneMatch(s -> Objects.equals(s.getItemId(), old.getItemId()))) {
				List<Items> withOld = new ArrayList<>(siblings);
				withOld.add(old);
				siblings = withOld;
			}
			List<Map<String, Object>> specItems = new ArrayList<>();
			for (Items sib : siblings) {
				Map<String, Object> si = new LinkedHashMap<>();
				si.put("item_id", sib.getItemId());
				si.put("item_bn", nz(sib.getItemBn(), ""));
				si.put("weight", sib.getWeight() != null ? sib.getWeight() : 0.0);
				si.put("barcode", nz(sib.getBarcode(), ""));
				si.put("price", fenToYuan(sib.getPrice()));
				si.put("cost_price", fenToYuan(sib.getCostPrice()));
				si.put("market_price", fenToYuan(sib.getMarketPrice()));
				si.put("item_unit", nz(sib.getItemUnit(), ""));
				si.put("store", sib.getStore() != null ? sib.getStore() : 0);
				si.put("is_default", Objects.equals(sib.getItemId(), defId));
				si.put("default_item_id", defId);
				si.put("approve_status", nz(sib.getApproveStatus(), "onsale"));
				if (Objects.equals(sib.getItemId(), old.getItemId())) {
					if (StringUtils.hasText(r.get("price"))) {
						si.put("price", parseYuan(r.get("price")));
					}
					if (StringUtils.hasText(r.get("cost_price"))) {
						si.put("cost_price", parseYuan(r.get("cost_price")));
					}
					if (StringUtils.hasText(r.get("market_price"))) {
						si.put("market_price", parseYuan(r.get("market_price")));
					}
					if (StringUtils.hasText(r.get("store"))) {
						si.put("store", parseStore(r.get("store")));
					}
					if (StringUtils.hasText(r.get("weight"))) {
						si.put("weight", parseWeight(r.get("weight")));
					}
					if (StringUtils.hasText(r.get("barcode"))) {
						si.put("barcode", r.get("barcode"));
					}
					if (StringUtils.hasText(r.get("item_unit"))) {
						si.put("item_unit", r.get("item_unit"));
					}
					if (StringUtils.hasText(r.get("approve_status"))) {
						si.put("approve_status", resolveApproveStatusStrict(r.get("approve_status")));
					}
					si.put("item_spec", itemSpec);
				}
				specItems.add(si);
			}
			merged.put("spec_items", specItems);
			List<Map<String, Object>> specImages =
					supplierGoodsImportRowOrchestrator.getItemSpecImages(companyId, r, mainCategory);
			if (!specImages.isEmpty()) {
				merged.put("spec_images", specImages);
			}
		}
		return merged;
	}

	private void assertNoDuplicatePlatformSpec(
			long companyId, Long defaultItemId, String itemBn, List<Map<String, Object>> specInfo) {
		if (defaultItemId == null || defaultItemId <= 0L || specInfo == null || specInfo.isEmpty()) {
			return;
		}
		List<Items> siblings = itemsRepository.listByDefaultItemIdAndCompany(defaultItemId, companyId);
		if (siblings.isEmpty()) {
			return;
		}
		List<Long> valueIds = specInfo.stream()
				.map(s -> toLongObj(s.get("spec_value_id")))
				.filter(Objects::nonNull)
				.toList();
		if (valueIds.isEmpty()) {
			return;
		}
		String bn = itemBn != null ? itemBn.trim() : "";
		List<Long> siblingIds = siblings.stream()
				.filter(s -> s.getItemId() != null && !bn.equals(nz(s.getItemBn(), "").trim()))
				.map(Items::getItemId)
				.toList();
		if (siblingIds.isEmpty()) {
			return;
		}
		List<ItemRelAttributes> attrs =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, siblingIds, "item_spec");
		Map<Long, List<Long>> byItem = new LinkedHashMap<>();
		for (ItemRelAttributes a : attrs) {
			if (a.getItemId() == null || a.getAttributeValueId() == null) {
				continue;
			}
			byItem.computeIfAbsent(a.getItemId(), k -> new ArrayList<>()).add(a.getAttributeValueId());
		}
		for (Map.Entry<Long, List<Long>> e : byItem.entrySet()) {
			List<Long> existing = e.getValue();
			if (existing.size() != valueIds.size()) {
				continue;
			}
			if (existing.containsAll(valueIds)) {
				throw new BadRequestException("相同规格值的商品已存在");
			}
		}
	}

	private void rememberSpuAnchorAfterWrite(
			long companyId, Map<String, String> r, Map<String, Object> rawRow, String itemBn) {
		boolean nospec = !StringUtils.hasText(r.get("item_spec"));
		String goodsBn = StringUtils.hasText(r.get("goods_bn")) ? r.get("goods_bn").trim() : "";
		if (nospec || !StringUtils.hasText(goodsBn) || !StringUtils.hasText(itemBn)) {
			return;
		}
		UploadSession session = sessionOf(rawRow);
		if (session == null) {
			return;
		}
		Items created = itemsRepository.findByItemBnAndCompany(itemBn.trim(), companyId);
		if (created == null || created.getItemId() == null) {
			return;
		}
		long def = created.getDefaultItemId() != null && created.getDefaultItemId() > 0L
				? created.getDefaultItemId()
				: created.getItemId();
		long gid = created.getGoodsId() != null && created.getGoodsId() > 0L ? created.getGoodsId() : def;
		session.spuByGoodsBn.put(goodsBn, new SpuAnchor(def, gid));
	}

	private SpuAnchor resolveSessionAnchor(Map<String, Object> rawRow, String goodsBn) {
		if (!StringUtils.hasText(goodsBn)) {
			return null;
		}
		UploadSession session = sessionOf(rawRow);
		if (session == null) {
			return null;
		}
		return session.spuByGoodsBn.get(goodsBn.trim());
	}

	private SpuAnchor resolveDbSpuAnchor(long companyId, String goodsBn) {
		if (!StringUtils.hasText(goodsBn)) {
			return null;
		}
		Items anchor = itemsRepository.findSpuAnchorByGoodsBnAndCompany(goodsBn.trim(), companyId);
		if (anchor == null || anchor.getItemId() == null) {
			return null;
		}
		long def = anchor.getDefaultItemId() != null && anchor.getDefaultItemId() > 0L
				? anchor.getDefaultItemId()
				: anchor.getItemId();
		long gid = anchor.getGoodsId() != null && anchor.getGoodsId() > 0L ? anchor.getGoodsId() : def;
		return new SpuAnchor(def, gid);
	}

	private static UploadSession sessionOf(Map<String, Object> row) {
		Object v = row.get("__upload_file_id__");
		if (v == null) {
			return null;
		}
		try {
			long id = Long.parseLong(String.valueOf(v).trim());
			if (id <= 0L) {
				return null;
			}
			return SESSION.computeIfAbsent(id, k -> new UploadSession());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void validateCreateRequired(Map<String, String> r) {
		List<String> errs = new ArrayList<>();
		if (!StringUtils.hasText(r.get("item_name"))) {
			errs.add("请填写商品名称");
		}
		if (!StringUtils.hasText(r.get("price"))) {
			errs.add("请填写价格");
		}
		if (!StringUtils.hasText(r.get("templates_id"))) {
			errs.add("请填写运费模板");
		}
		if (!StringUtils.hasText(r.get("item_main_category"))) {
			errs.add("请上传管理分类");
		}
		if (!StringUtils.hasText(r.get("store"))) {
			errs.add("请填写库存");
		}
		if (!StringUtils.hasText(r.get("item_category"))) {
			errs.add("请上传销售分类");
		}
		if (!errs.isEmpty()) {
			throw new BadRequestException(String.join(", ", errs));
		}
		int st = parseStore(r.get("store"));
		if (st < 0 || st > 999999999) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
	}

	private static String resolveApproveStatus(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "onsale";
		}
		String mapped = APPROVE_STATUS_LABEL.get(raw.trim());
		return mapped != null ? mapped : "onsale";
	}

	private static String resolveApproveStatusStrict(String raw) {
		String mapped = APPROVE_STATUS_LABEL.get(raw.trim());
		if (mapped == null) {
			throw new BadRequestException("商品状态错误");
		}
		return mapped;
	}

	private static boolean resolveIsProfit(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		if ("1".equals(t) || "是".equals(t) || "true".equalsIgnoreCase(t)) {
			return true;
		}
		if ("0".equals(t) || "否".equals(t) || "false".equalsIgnoreCase(t)) {
			return false;
		}
		throw new BadRequestException("是否支持分润参数错误");
	}

	private static int parseDeliveryTime(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			int n = new BigDecimal(raw.trim()).intValue();
			if (n < 0) {
				throw new BadRequestException("发货时间格式错误");
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException("发货时间格式错误");
		}
	}

	private static int parseStartNum(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			return Math.max(new BigDecimal(raw.trim()).intValue(), 0);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseStore(String raw) {
		return (int) parseLong(raw);
	}

	private static double parseWeight(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0.0;
		}
		try {
			return new BigDecimal(raw.trim()).doubleValue();
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static long parseLong(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return new BigDecimal(s.trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long toLongObj(Object o) {
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

	private static BigDecimal parseYuan(Object yuan) {
		if (yuan == null) {
			return BigDecimal.ZERO;
		}
		String s = yuan.toString().trim();
		if (s.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(s);
	}

	private static BigDecimal fenToYuan(Integer fen) {
		if (fen == null) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(fen.longValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	/**
	 * 拆分图片列。PHP 导出会把多图写成 {@code "url1,url2"}（内容自带引号），
	 * 直接 {@code split(",")} 会得到 {@code ["\"url1", "url2\""]} 脏数据。
	 */
	private static List<String> splitCommaList(String pics) {
		if (!StringUtils.hasText(pics)) {
			return List.of();
		}
		String s = stripOuterQuotes(pics.trim());
		return Arrays.stream(s.split(",")).map(String::trim).map(NormalGoodsImportRowOrchestrator::stripOuterQuotes)
				.filter(StringUtils::hasText).toList();
	}

	private static String stripOuterQuotes(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		String t = s.trim();
		while (!t.isEmpty() && (t.charAt(0) == '\\' || t.charAt(0) == '"')) {
			t = t.substring(1).trim();
		}
		while (!t.isEmpty()) {
			char c = t.charAt(t.length() - 1);
			if (c != '\\' && c != '"') {
				break;
			}
			t = t.substring(0, t.length() - 1).trim();
		}
		return t;
	}

	private static String buildIntroHtml(String introComma) {
		if (!StringUtils.hasText(introComma)) {
			return "";
		}
		StringBuilder intro = new StringBuilder();
		for (String u : introComma.split(",")) {
			String x = u.trim();
			if (!StringUtils.hasText(x)) {
				continue;
			}
			intro.append("<img src=\"").append(x).append("\" style=\"display: block;\">");
		}
		return intro.toString();
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}

	private static String nz(String s, String d) {
		return s != null ? s : d;
	}

	private static final class UploadSession {
		final ConcurrentHashMap<String, SpuAnchor> spuByGoodsBn = new ConcurrentHashMap<>();
	}

	private static final class SpuAnchor {
		final Long defaultItemId;
		final Long goodsId;

		SpuAnchor(Long defaultItemId, Long goodsId) {
			this.defaultItemId = defaultItemId;
			this.goodsId = goodsId;
		}
	}
}
