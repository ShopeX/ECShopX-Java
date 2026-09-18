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

import cn.shopex.ecshopx.crossborder.domain.OriginCountry;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelPointAccessRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService.ItemDetailAttrData;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlatformItemsDetailCoreService {

	private final ItemsRepository itemsRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsRelPointAccessRepository itemsRelPointAccessRepository;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final ItemsDetailIntroArticleService itemsDetailIntroArticleService;
	private final ItemsDetailVideoPicService itemsDetailVideoPicService;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final OriginCountryMapper originCountryMapper;
	private final CrossBorderItemTaxDetailService crossBorderItemTaxDetailService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemDetailScalarHelper itemDetailScalarHelper;

	public PlatformItemsDetailCoreService(ItemsRepository itemsRepository, ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService, ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsRelPointAccessRepository itemsRelPointAccessRepository, PointMemberRuleReadService pointMemberRuleReadService,
			ItemsCategoryPathByItemService itemsCategoryPathByItemService, ItemsDetailIntroArticleService itemsDetailIntroArticleService,
			ItemsDetailVideoPicService itemsDetailVideoPicService,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			OriginCountryMapper originCountryMapper, CrossBorderItemTaxDetailService crossBorderItemTaxDetailService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository, ItemsMedicineService itemsMedicineService,
			ItemDetailScalarHelper itemDetailScalarHelper) {
		this.itemsRepository = itemsRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsRelPointAccessRepository = itemsRelPointAccessRepository;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.itemsDetailIntroArticleService = itemsDetailIntroArticleService;
		this.itemsDetailVideoPicService = itemsDetailVideoPicService;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.originCountryMapper = originCountryMapper;
		this.crossBorderItemTaxDetailService = crossBorderItemTaxDetailService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.itemsMedicineService = itemsMedicineService;
		this.itemDetailScalarHelper = itemDetailScalarHelper;
	}

	public Map<String, Object> build(long companyId, long itemId, String authorizerAppId) {
		return build(companyId, itemId, authorizerAppId, List.of());
	}

	public Map<String, Object> build(long companyId, long itemId, String authorizerAppId, List<Long> limitItemIds) {
		Items entity = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (entity == null) {
			return Map.of();
		}
		Optional<String> dataSourceOpt = itemsRepository.trySelectDataSource(itemId, companyId);
		Map<String, Object> detail = GoodsItemsListRowMapper.toRow(entity);
		detail.remove("distributor_name");
		itemDetailScalarHelper.normalizeDetailScalars(detail);
		detail.put("data_source", dataSourceOpt.orElse(""));

		detail.put("type_labels", List.of());
		String itemType = str(detail.get("item_type"));
		if ("services".equals(itemType)) {
			detail.put("spec_items", List.of());
			applyCategoryAndPaths(companyId, detail);
			finishCommonEnrichments(detail, entity, companyId, authorizerAppId);
			itemsMedicineService.applyMedicineDataToRows(companyId, List.of(detail));
			return detail;
		}

		boolean multiSpec = itemDetailScalarHelper.isMultiSpec(detail.get("nospec"));
		List<Items> skuRows = List.of();
		List<Long> skuIds = List.of(itemId);
		if (multiSpec) {
			long def = entity.getDefaultItemId() != null ? entity.getDefaultItemId() : itemId;
			skuRows = itemsRepository.listByDefaultItemIdAndCompany(def, companyId);
			skuRows = filterSkuRowsByLimitItemIds(skuRows, limitItemIds);
			skuIds = skuRows.stream().map(Items::getItemId).collect(Collectors.toList());
		}

		long defaultItemId = entity.getDefaultItemId() != null ? entity.getDefaultItemId() : itemId;
		List<ItemRelAttributes> relAll = new ArrayList<>();
		relAll.addAll(itemRelAttributesRepository.listByCompanyAndItemId(companyId, defaultItemId));
		if (multiSpec) {
			relAll.addAll(itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, skuIds, "item_spec"));
		}

		detail.put("spec_pics", "");
		for (ItemRelAttributes ra : relAll) {
			if ("item_spec".equals(ra.getAttributeType()) && ra.getItemId() != null && ra.getItemId().equals(itemId) && StringUtils.hasText(ra.getImageUrl())) {
				detail.put("spec_pics", ra.getImageUrl());
				break;
			}
		}

		ItemDetailAttrData ad = itemsRelAttrValuesQueryService.assemble(companyId, relAll);
		if (!ad.brand.isEmpty()) {
			detail.put("brand_id", ad.brand.get("brand_id"));
			detail.put("goods_brand", ad.brand.get("goods_brand"));
			detail.put("brand_logo", ad.brand.get("brand_logo"));
		}
		detail.put("attribute_ids", ad.attributeIds);
		detail.put("attr_values_custom", new LinkedHashMap<>(ad.attrValuesCustom));

		if (ad.itemParams.isEmpty()) {
			itemDetailScalarHelper.applyFallbackItemParams(detail);
		} else {
			detail.put("item_params", ad.itemParams);
		}

		detail.put("item_spec_desc", ad.itemSpecDesc);
		detail.put("spec_images", ad.specImages);
		boolean skipPointAccess = dataSourceOpt.filter("supplier_goods"::equals).isPresent();
		applyItemSpecBlock(detail, ad, skuRows, companyId, entity, multiSpec, skipPointAccess);

		applyCategoryAndPaths(companyId, detail);
		finishCommonEnrichments(detail, entity, companyId, authorizerAppId);
		itemsMedicineService.applyMedicineDataToRows(companyId, List.of(detail));
		return detail;
	}

	private void applyCategoryAndPaths(long companyId, Map<String, Object> detail) {
		long mainItemId = toLong(detail.get("item_id"));
		List<Long> catIds = itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, List.of(mainItemId)).stream()
				.map(c -> c.getCategoryId()).distinct().collect(Collectors.toList());
		detail.put("item_category", catIds);

		Object im = detail.get("item_main_cat_id");
		long mainCat = 0L;
		if (im instanceof Number n) {
			mainCat = n.longValue();
		} else if (im != null && StringUtils.hasText(im.toString())) {
			try {
				mainCat = Long.parseLong(im.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		if (mainCat > 0) {
			List<Map<String, Object>> mainPath = itemsCategoryPathByItemService.getCategoryPathById(companyId, mainCat, true);
			detail.put("item_category_main", mainPath);
			if (mainPath.isEmpty()) {
				detail.put("item_main_cat_id", "");
			}
		} else {
			detail.put("item_category_main", List.of());
		}

		@SuppressWarnings("unchecked")
		List<Long> ic = (List<Long>) detail.get("item_category");
		if (ic != null && !ic.isEmpty()) {
			detail.put("item_category_info", itemsCategoryPathByItemService.getCategoryPathById(companyId, ic.get(0), false));
		} else {
			detail.put("item_category_info", List.of());
		}
	}

	private void finishCommonEnrichments(Map<String, Object> detail, Items entity, long companyId, String authorizerAppId) {
		detail.put("intro", itemsDetailIntroArticleService.pro(detail.get("intro"), authorizerAppId, 0L, "", companyId));

		String vt = str(detail.get("video_type"));
		if (StringUtils.hasText(str(detail.get("videos"))) && StringUtils.hasText(authorizerAppId) && !"tencent".equals(vt)) {
			itemsDetailVideoPicService.applyVideoUrl(detail, authorizerAppId);
		} else {
			detail.put("videos_url", "");
		}
		if ("tencent".equals(vt)) {
			detail.put("tencent_vid", detail.get("videos"));
		}

		detail.put("distributor_sale_status", true);
		String ap = str(detail.get("approve_status"));
		if ("instock".equals(ap) || "only_show".equals(ap)) {
			detail.put("distributor_sale_status", false);
		}
		Integer store = entity.getStore();
		int sales = entity.getSales() != null ? entity.getSales() : 0;
		detail.put("item_total_store", detail.getOrDefault("item_total_store", store));
		detail.put("item_total_sales", detail.getOrDefault("item_total_sales", sales));

		long distId = toLong(detail.get("distributor_id"));
		detail.put("distributor_info", GoodsItemsDetailDistributorInfoLoader.load(adminOrderDetailDistributionSupportPort, companyId, distId));

		long ocId = toLong(detail.get("origincountry_id"));
		if (ocId <= 0) {
			detail.put("origincountry_name", "");
			detail.put("origincountry_img_url", "");
		} else {
			OriginCountry oc = originCountryMapper.selectById(ocId);
			if (oc != null) {
				detail.put("origincountry_name", oc.getOrigincountryName() != null ? oc.getOrigincountryName() : "");
				detail.put("origincountry_img_url", oc.getOrigincountryImgUrl() != null ? oc.getOrigincountryImgUrl() : "");
			} else {
				detail.put("origincountry_name", "");
				detail.put("origincountry_img_url", "");
			}
		}

		crossBorderItemTaxDetailService.applyCrossBorderTaxFields(detail, entity);

		detail.put("templates_name", "");
		Long tid = entity.getTemplatesId() != null ? entity.getTemplatesId().longValue() : 0L;
		if (tid > 0) {
			shippingTemplatesQueryRepository.findTemplateName(tid, companyId).ifPresent(name -> detail.put("templates_name", name));
		}
	}

	public void applyItemSpecBlockForSupplier(Map<String, Object> detail, ItemDetailAttrData ad, List<Items> skuRows, long companyId,
			Items mainEntity, boolean multiSpec) {
		if (ad.itemSpecNested.isEmpty()) {
			detail.put("spec_items", List.of());
			return;
		}
		applyItemSpecBlock(detail, ad, skuRows, companyId, mainEntity, multiSpec, true);
	}

	private void applyItemSpecBlock(Map<String, Object> detail, ItemDetailAttrData ad, List<Items> skuRows, long companyId, Items mainEntity,
			boolean multiSpec, boolean skipPointAccess) {
		Map<Long, Long> pointByItem = Map.of();
		if (!skipPointAccess) {
			Map<String, Object> pointRule = pointMemberRuleReadService.getPointRule(companyId);
			String access = str(pointRule.get("access"));
			boolean orderAccess = "order".equals(access);
			if (!orderAccess && multiSpec && !skuRows.isEmpty()) {
				List<Long> ids = skuRows.stream().map(Items::getItemId).collect(Collectors.toList());
				pointByItem = itemsRelPointAccessRepository.mapPointByItemId(companyId, ids);
			} else if (!orderAccess && !multiSpec) {
				var one = itemsRelPointAccessRepository.findByCompanyAndItemId(companyId, mainEntity.getItemId());
				if (one != null && one.getPoint() != null) {
					pointByItem = Map.of(mainEntity.getItemId(), one.getPoint());
				}
			}
		}

		List<Map<String, Object>> specItems = new ArrayList<>();
		if (multiSpec && !skuRows.isEmpty()) {
			List<String> approveRank = new ArrayList<>();
			int totalStore = 0;
			int totalSales = 0;
			for (Items sku : skuRows) {
				Map<Long, Map<String, Object>> bySpec = ad.itemSpecNested.getOrDefault(sku.getItemId(), Map.of());
				List<Map<String, Object>> temp = new ArrayList<>(bySpec.values());
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("item_id", sku.getItemId());
				row.put("price", sku.getPrice());
				row.put("store", sku.getStore());
				row.put("cost_price", sku.getCostPrice());
				row.put("item_bn", sku.getItemBn());
				row.put("barcode", sku.getBarcode());
				row.put("market_price", sku.getMarketPrice());
				row.put("item_unit", sku.getItemUnit());
				row.put("volume", sku.getVolume());
				row.put("approve_status", sku.getApproveStatus() != null ? sku.getApproveStatus() : "");
				row.put("is_default", sku.getIsDefault());
				row.put("weight", GoodsItemsListRowMapper.toRow(sku).get("weight"));
				row.put("item_spec", temp);
				row.put("point_num", pointByItem.getOrDefault(sku.getItemId(), 0L).intValue());
				row.put("start_num", sku.getStartNum());
				row.put("delivery_time", sku.getDeliveryTime() != null ? sku.getDeliveryTime() : 0);
				specItems.add(row);
				approveRank.add(str(row.get("approve_status")));
				totalStore += sku.getStore() != null ? sku.getStore() : 0;
				totalSales += sku.getSales() != null ? sku.getSales() : 0;
			}
			detail.put("approve_status", aggregateApproveStatus(approveRank));
			detail.put("item_total_store", totalStore);
			detail.put("item_total_sales", totalSales);
		} else {
			detail.put("spec_items", List.of());
			if (!skipPointAccess && mainEntity.getItemId() != null) {
				detail.put("point_num", pointByItem.getOrDefault(mainEntity.getItemId(), 0L).intValue());
			}
			return;
		}
		detail.put("spec_items", specItems);
	}

	private static String aggregateApproveStatus(List<String> statuses) {
		if (statuses.contains("onsale")) {
			return "onsale";
		}
		if (statuses.contains("only_show")) {
			return "only_show";
		}
		if (statuses.contains("offline_sale")) {
			return "offline_sale";
		}
		return "instock";
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

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static List<Items> filterSkuRowsByLimitItemIds(List<Items> skuRows, List<Long> limitItemIds) {
		if (limitItemIds == null || limitItemIds.isEmpty() || skuRows == null || skuRows.isEmpty()) {
			return skuRows == null ? List.of() : skuRows;
		}
		Set<Long> allowed = new LinkedHashSet<>(limitItemIds);
		Map<Long, Items> byId = skuRows.stream().filter(s -> s.getItemId() != null).collect(Collectors.toMap(Items::getItemId, s -> s, (a, b) -> a, LinkedHashMap::new));
		List<Items> ordered = new ArrayList<>();
		for (Long id : limitItemIds) {
			Items it = byId.get(id);
			if (it != null) {
				ordered.add(it);
			}
		}
		if (ordered.isEmpty()) {
			return skuRows.stream().filter(s -> s.getItemId() != null && allowed.contains(s.getItemId())).collect(Collectors.toList());
		}
		return ordered;
	}
}
