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
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.dispatch.ItemAddEventDispatchPublisher;
import cn.shopex.ecshopx.goods.dispatch.ItemCreateEventDispatchPublisher;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelPointAccessRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.espier.service.upload.IntroHtmlDataImageUploadService;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformItemsAddService {

	private final IntroHtmlDataImageUploadService introHtmlDataImageUploadService;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final MerchantQueryService merchantQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemProcessUpdateItemService itemProcessUpdateItemService;
	private final ItemSpecParamsResolver itemSpecParamsResolver;
	private final NormalItemTypeHandler normalItemTypeHandler;
	private final ServicesItemTypeHandler servicesItemTypeHandler;
	private final ItemsRepository itemsRepository;
	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final ItemStoreService itemStoreService;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelPointAccessRepository itemsRelPointAccessRepository;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;
	private final ItemsRelCatsWriteService itemsRelCatsWriteService;
	private final ItemAddEventDispatchPublisher itemAddEventDispatchPublisher;
	private final ItemCreateEventDispatchPublisher itemCreateEventDispatchPublisher;
	private final ItemsMultiLangWriteService itemsMultiLangWriteService;
	private final ObjectMapper objectMapper;

	public PlatformItemsAddService(
			IntroHtmlDataImageUploadService introHtmlDataImageUploadService,
			PointMemberRuleReadService pointMemberRuleReadService,
			MerchantQueryService merchantQueryService,
			DistributorListQueryService distributorListQueryService,
			ItemsMedicineService itemsMedicineService,
			ItemProcessUpdateItemService itemProcessUpdateItemService,
			ItemSpecParamsResolver itemSpecParamsResolver,
			NormalItemTypeHandler normalItemTypeHandler,
			ServicesItemTypeHandler servicesItemTypeHandler,
			ItemsRepository itemsRepository,
			ItemsBarcodeRepository itemsBarcodeRepository,
			ItemStoreService itemStoreService,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelPointAccessRepository itemsRelPointAccessRepository,
			ItemCreatePromotionGuardService itemCreatePromotionGuardService,
			ItemsRelCatsWriteService itemsRelCatsWriteService,
			ItemAddEventDispatchPublisher itemAddEventDispatchPublisher,
			ItemCreateEventDispatchPublisher itemCreateEventDispatchPublisher,
			ItemsMultiLangWriteService itemsMultiLangWriteService,
			ObjectMapper objectMapper) {
		this.introHtmlDataImageUploadService = introHtmlDataImageUploadService;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.merchantQueryService = merchantQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsMedicineService = itemsMedicineService;
		this.itemProcessUpdateItemService = itemProcessUpdateItemService;
		this.itemSpecParamsResolver = itemSpecParamsResolver;
		this.normalItemTypeHandler = normalItemTypeHandler;
		this.servicesItemTypeHandler = servicesItemTypeHandler;
		this.itemsRepository = itemsRepository;
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.itemStoreService = itemStoreService;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelPointAccessRepository = itemsRelPointAccessRepository;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
		this.itemsRelCatsWriteService = itemsRelCatsWriteService;
		this.itemAddEventDispatchPublisher = itemAddEventDispatchPublisher;
		this.itemCreateEventDispatchPublisher = itemCreateEventDispatchPublisher;
		this.itemsMultiLangWriteService = itemsMultiLangWriteService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public long addItemsTransactional(Map<String, Object> params) {
		String itemType = str(params.getOrDefault("item_type", "services"));
		params.putIfAbsent("item_type", itemType);
		ItemTypeHandler handler = "normal".equals(itemType) ? normalItemTypeHandler : servicesItemTypeHandler;
		boolean gift = truthyGift(params.get("is_gift"));
		long companyId = toLong(params.get("company_id"));
		ItemsCreateContext ctx = new ItemsCreateContext(companyId, str(params.get("operator_type")), itemType, gift, false);

		Map<String, Object> data = commonParams(params);
		Map<String, Object> medicineData = new LinkedHashMap<>();
		itemsMedicineService.assertMedicineSettingsAndEnrichParams(companyId, params, data);
		if (Integer.valueOf(1).equals(toInt(params.get("is_medicine")))) {
			medicineData.put("active", true);
		}

		long goodsId = params.get("goods_id") != null ? toLong(params.get("goods_id")) : 0L;
		if (goodsId == 0 && params.get("item_id") != null) {
			itemProcessUpdateItemService.run(params, itemType);
			Items cur = itemsRepository.getByItemIdAndCompany(toLong(params.get("item_id")), companyId);
			if (cur != null) {
				goodsId = cur.getGoodsId() != null ? cur.getGoodsId() : 0L;
			}
		}

		List<Long> itemIds = new ArrayList<>();
		Map<Long, Long> itemPrices = new LinkedHashMap<>();
		Long defaultItemId = params.get("default_item_id") != null ? toLong(params.get("default_item_id")) : null;
		Map<String, Object> lastResult = new LinkedHashMap<>();
		String requestLang = itemsMultiLangWriteService.resolveLang(params);

		if (isMultiSpec(params.get("nospec"))) {
			Map<Long, String> specImages = parseSpecImages(params.get("spec_images"));
			data.put("spec_images", specImages);
			List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));
			if (specItems.isEmpty()) {
				throw new ResourceException("请填写正确的规格数据");
			}
			boolean forceCreate = params.get("item_id") == null;
			long minPrice = 0;
			for (Map<String, Object> row : specItems) {
				Map<String, Object> sku = mergeSku(data, row);
				Map<String, Object> res = persistOneSku(sku, row, ctx, handler, forceCreate, specImages, requestLang);
				long iid = toLong(res.get("item_id"));
				itemIds.add(iid);
				long priceFen = toLong(res.get("price"));
				itemPrices.put(iid, priceFen);
				if (defaultItemId == null) {
					if (minPrice == 0) {
						minPrice = priceFen;
						defaultItemId = iid;
					} else if (priceFen < minPrice) {
						minPrice = priceFen;
						defaultItemId = iid;
					}
				}
				if (defaultItemId == null && onsaleLike(row.get("approve_status"))) {
					defaultItemId = iid;
				}
				afterSkuSideEffects(params, medicineData, sku, res, ctx, handler);
				lastResult = res;
			}
			if (defaultItemId == null && !itemIds.isEmpty()) {
				defaultItemId = itemIds.get(0);
			}
		} else {
			if (params.get("start_num") != null) {
				data.put("start_num", toInt(params.get("start_num")));
			}
			boolean forceCreate = params.get("item_id") == null;
			Map<String, Object> sku = mergeSku(data, params);
			Map<String, Object> res = persistOneSku(sku, params, ctx, handler, forceCreate, Map.of(), requestLang);
			long iid = toLong(res.get("item_id"));
			itemIds.add(iid);
			itemPrices.put(iid, toLong(res.get("price")));
			if (defaultItemId == null) {
				defaultItemId = iid;
			}
			afterSkuSideEffects(params, medicineData, sku, res, ctx, handler);
			lastResult = res;
		}

		if (goodsId == 0) {
			goodsId = defaultItemId != null ? defaultItemId : 0L;
		}
		boolean giftFlag = truthyGift(data.get("is_gift"));
		if (giftFlag) {
			itemCreatePromotionGuardService.checkNotFinishedActivityValid(companyId, true, itemIds, List.of(goodsId));
		}
		itemCreatePromotionGuardService.checkItemPrice(companyId, List.of(goodsId), itemPrices);

		itemsRepository.updateByItemIds(companyId, itemIds, defaultItemId != null ? defaultItemId : 0L, goodsId);
		if (defaultItemId != null) {
			itemsRepository.clearDefaultFlagExcept(companyId, defaultItemId, defaultItemId);
			itemsRepository.setDefaultItem(companyId, defaultItemId, true);
		}

		boolean isCreateRelData = !Boolean.FALSE.equals(params.get("isCreateRelData"));
		if (isCreateRelData) {
			applyRelData(params, defaultItemId != null ? defaultItemId : 0L);
		}

		Map<String, Object> entities = buildCreateEventEntities(lastResult, params);
		itemCreateEventDispatchPublisher.publish(entities, itemIds);

		return defaultItemId != null ? defaultItemId : 0L;
	}

	private void afterSkuSideEffects(
			Map<String, Object> params,
			Map<String, Object> medicineData,
			Map<String, Object> sku,
			Map<String, Object> res,
			ItemsCreateContext ctx,
			ItemTypeHandler handler) {
		if (!medicineData.isEmpty()) {
			itemsMedicineService.updateItemMedicineData(sku, medicineData, res);
		}
		itemAddEventDispatchPublisher.schedulePublishAfterCommit(toLong(res.get("item_id")), ctx.getCompanyId());
	}

	private Map<String, Object> persistOneSku(
			Map<String, Object> sku,
			Map<String, Object> itemSpecSourceParams,
			ItemsCreateContext ctx,
			ItemTypeHandler handler,
			boolean forceCreate,
			Map<Long, String> specImages,
			String requestLang) {
		Map<String, Object> work = new LinkedHashMap<>(sku);
		itemSpecParamsResolver.resolve(work, itemSpecSourceParams, ctx);
		handler.preRelItemParams(work, sku, ctx);
		Items entity = mapToItems(work, ctx);
		Long existingId = sku.get("item_id") != null ? toLong(sku.get("item_id")) : null;
		boolean isUpdate = existingId != null && existingId > 0 && !forceCreate;
		if (isUpdate) {
			entity.setItemId(existingId);
			itemsRepository.updateByItemId(existingId, ctx.getCompanyId(), entity);
		} else {
			itemsRepository.insert(entity);
		}
		long itemId = entity.getItemId();
		if (isUpdate) {
			itemsMultiLangWriteService.afterItemUpdate(ctx.getCompanyId(), itemId, work, requestLang);
		} else {
			itemsMultiLangWriteService.afterItemCreate(ctx.getCompanyId(), itemId, work, requestLang);
		}
		long defaultItemIdForBarcode = work.get("default_item_id") != null ? toLong(work.get("default_item_id")) : itemId;
		long distributorIdForBarcode =
				entity.getDistributorId() != null ? entity.getDistributorId().longValue() : 0L;
		itemsBarcodeRepository.saveBarcode(
				ctx.getCompanyId(), distributorIdForBarcode, itemId, defaultItemIdForBarcode, str(sku.get("barcode")));
		int store = work.get("store") != null ? toInt(work.get("store")) : 0;
		itemStoreService.saveItemStore(itemId, store, 0L);
		saveItemSpecRel(ctx.getCompanyId(), itemId, itemSpecSourceParams.get("item_spec"), specImages);
		Map<String, Object> itemsResult = new LinkedHashMap<>();
		itemsResult.put("item_id", itemId);
		itemsResult.put("company_id", ctx.getCompanyId());
		itemsResult.put("price", work.get("price"));
		itemsResult.put("item_category", entity.getItemCategory());
		itemsResult.put("brand_id", entity.getBrandId());
		itemsResult.put("goods_id", entity.getGoodsId());
		itemsResult.put("default_item_id", entity.getDefaultItemId());
		itemsResult = handler.createRelItem(itemsResult, sku, ctx);
		String pointAccess = str(work.get("point_access"));
		if ("items".equals(pointAccess)) {
			long pt = pointForRelTable(sku, work);
			itemsRelPointAccessRepository.upsertForItem(ctx.getCompanyId(), itemId, pt);
		}
		return itemsResult;
	}

	private void saveItemSpecRel(long companyId, long itemId, Object raw, Map<Long, String> specImages) {
		if (raw == null) {
			return;
		}
		List<Map<String, Object>> rows = asMapList(raw);
		int sort = 0;
		for (Map<String, Object> v : rows) {
			ItemRelAttributes row = new ItemRelAttributes();
			row.setCompanyId(companyId);
			row.setItemId(itemId);
			row.setAttributeId(toLong(v.get("spec_id")));
			row.setAttributeType("item_spec");
			row.setAttributeValueId(v.get("spec_value_id") != null ? toLong(v.get("spec_value_id")) : null);
			row.setAttributeSort(sort++);
			long specValueId = v.get("spec_value_id") != null ? toLong(v.get("spec_value_id")) : 0L;
			row.setImageUrl(itemSpecRelImageUrlOrNull(specImages != null ? specImages.get(specValueId) : null));
			row.setCustomAttributeValue(
					v.get("spec_custom_value_name") != null ? str(v.get("spec_custom_value_name")) : null);
			itemRelAttributesRepository.insert(row);
		}
	}

	private Map<String, Object> buildCreateEventEntities(Map<String, Object> lastSku, Map<String, Object> params) {
		Map<String, Object> m = new LinkedHashMap<>(lastSku);
		m.put("company_id", params.get("company_id"));
		Object cat = lastSku.get("item_category");
		if (cat != null) {
			m.put("item_main_cat_id", cat);
		}
		if (params.get("item_main_cat_id") != null) {
			m.put("item_main_cat_id", params.get("item_main_cat_id"));
		}
		return m;
	}

	private void applyRelData(Map<String, Object> params, long defaultItemId) {
		if (defaultItemId <= 0) {
			return;
		}
		long companyId = toLong(params.get("company_id"));
		Object ic = params.get("item_category");
		if (ValuePresence.hasEffectiveValue(ic)) {
			List<Long> cats = parseCategoryIdsForRel(ic);
			if (cats.isEmpty()) {
				throw new ResourceException("请选择销售分类");
			}
			itemsRelCatsWriteService.setItemsCategory(companyId, defaultItemId, cats);
		}
		Object brandId = params.get("brand_id");
		if (brandId != null && toLong(brandId) > 0) {
			ItemRelAttributes row = new ItemRelAttributes();
			row.setCompanyId(companyId);
			row.setItemId(defaultItemId);
			row.setAttributeId(toLong(brandId));
			row.setAttributeType("brand");
			itemRelAttributesRepository.insert(row);
		}
		Object ip = params.get("item_params");
		if (ip != null) {
			for (Map<String, Object> v : asMapList(ip)) {
				ItemRelAttributes row = new ItemRelAttributes();
				row.setCompanyId(companyId);
				row.setItemId(defaultItemId);
				row.setAttributeId(toLong(v.get("attribute_id")));
				row.setAttributeType("item_params");
				row.setAttributeValueId(v.get("attribute_value_id") != null ? toLong(v.get("attribute_value_id")) : null);
				row.setCustomAttributeValue(v.get("attribute_value_name") != null ? v.get("attribute_value_name").toString() : null);
				itemRelAttributesRepository.insert(row);
			}
		}
	}

	private Map<String, Object> commonParams(Map<String, Object> params) {
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

		String auditStatus = "approved";
		long distributorId = params.get("distributor_id") != null ? toLong(params.get("distributor_id")) : 0L;
		if (distributorId > 0) {
			String distributorAudit = resolveDistributorItemAuditStatus(companyId, distributorId);
			if (StringUtils.hasText(distributorAudit)) {
				auditStatus = distributorAudit;
			}
		}
		data.put("audit_status", auditStatus);

		data.put("company_id", companyId);
		data.put("item_type", str(params.getOrDefault("item_type", "services")));
		data.put("consume_type", str(params.getOrDefault("consume_type", "every")));
		data.put("item_name", str(params.get("item_name")));
		data.put("item_unit", str(params.getOrDefault("item_unit", "")));
		data.put("brief", str(params.get("brief")));
		data.put("sort", params.get("sort") != null ? toInt(params.get("sort")) : 1);
		data.put("templates_id", params.get("templates_id"));
		data.put("is_show_specimg", truthy(params.get("is_show_specimg")));
		data.put("pics", params.get("pics"));
		data.put("videos", params.get("videos"));
		data.put("intro", intro);
		data.put("special_type", str(params.getOrDefault("special_type", "normal")));
		data.put("purchase_agreement", str(params.get("purchase_agreement")));
		data.put("enable_agreement", truthy(params.get("enable_agreement")));
		data.put("item_category", params.get("item_main_cat_id") != null ? str(params.get("item_main_cat_id")) : str(params.get("item_category")));
		data.put("nospec", str(params.getOrDefault("nospec", "true")));
		data.put("brand_id", params.get("brand_id"));
		data.put("tax_rate", intOrDefault(params.get("tax_rate"), 13));
		data.put("distributor_id", (int) distributorId);
		data.put("is_gift", params.get("is_gift"));
		data.put("operator_type", str(params.get("operator_type")));
		data.put("origincountry_id", params.get("origincountry_id") != null ? toLong(params.get("origincountry_id")) : 0L);
		data.put("taxstrategy_id", params.get("taxstrategy_id") != null ? toLong(params.get("taxstrategy_id")) : 0L);
		data.put("taxation_num", params.get("taxation_num") != null ? toInt(params.get("taxation_num")) : 0);
		data.put("is_medicine", params.get("is_medicine"));
		// —— 以下与 GoodsBundle ItemsService::commonParams 对齐（mergeSku 后勿依赖 items.item_category 表示销售类目）——
		data.put("pics_create_qrcode", params.get("pics_create_qrcode") != null ? params.get("pics_create_qrcode") : List.of());
		data.put("video_type", str(params.getOrDefault("video_type", "local")));
		data.put("video_pic_url", str(params.get("video_pic_url")));
		Object tdk = params.get("tdk_content");
		if (tdk != null && StringUtils.hasText(str(tdk))) {
			data.put("tdk_content", str(tdk));
		} else {
			data.put("tdk_content", "{\"title\":\"\",\"mate_description\":\"\",\"mate_keywords\":\"\"}");
		}
		data.put("is_market", params.containsKey("is_market") ? toInt(params.get("is_market")) : 1);
		data.put("goods_bn", str(params.get("goods_bn")));
		data.put("item_bn", str(params.get("item_bn")));
		if (params.containsKey("is_default")) {
			data.put("is_default", params.get("is_default"));
		}
		if (params.get("default_item_id") != null) {
			data.put("default_item_id", params.get("default_item_id"));
		}
		if (params.get("goods_id") != null) {
			data.put("goods_id", params.get("goods_id"));
		}
		String itemSrc = str(params.get("item_source"));
		data.put("item_source", StringUtils.hasText(itemSrc) ? (!itemSrc.isEmpty() ? itemSrc : "mall") : "mall");
		data.put("type", params.get("type") != null ? toInt(params.get("type")) : 0);
		data.put("is_profit", truthy(params.get("is_profit")));
		data.put("profit_type", params.get("profit_type") != null ? toInt(params.get("profit_type")) : 0);
		data.put("crossborder_tax_rate", str(params.get("crossborder_tax_rate")));
		putRegionsFields(data, params);
		data.put("date_type", str(params.get("date_type")));
		if (params.get("begin_date") != null) {
			data.put("begin_date", toInt(params.get("begin_date")));
		}
		if (params.get("end_date") != null) {
			data.put("end_date", toInt(params.get("end_date")));
		}
		if (params.get("fixed_term") != null) {
			data.put("fixed_term", toInt(params.get("fixed_term")));
		}
		data.put("item_address_province", str(params.get("item_address_province")));
		data.put("item_address_city", str(params.get("item_address_city")));
		return data;
	}

	private String resolveDistributorItemAuditStatus(long companyId, long distributorId) {
		List<Distributor> rows = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
		if (rows == null || rows.isEmpty()) {
			return "";
		}
		Distributor distributorInfo = rows.get(0);
		String auditStatus = "";
		if (Boolean.TRUE.equals(distributorInfo.getIsAuditGoods())) {
			auditStatus = "processing";
		}
		Long merchantId = distributorInfo.getMerchantId();
		if (merchantId != null && merchantId > 0L) {
			Merchant merchantInfo = merchantQueryService.getInfo(companyId, merchantId, false);
			if (merchantInfo != null && merchantInfo.isAuditGoods()) {
				auditStatus = "processing";
			}
		}
		return auditStatus;
	}

	private Items mapToItems(Map<String, Object> work, ItemsCreateContext ctx) {
		Items it = new Items();
		it.setCompanyId(ctx.getCompanyId());
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
		it.setPicsCreateQrcode(encodeJsonColumn(work.get("pics_create_qrcode"), true));
		it.setVideoType(str(work.getOrDefault("video_type", "local")));
		it.setVideoPicUrl(str(work.get("video_pic_url")));
		it.setVideos(encodeJsonColumn(work.get("videos"), false));
		it.setIntro(str(work.get("intro")));
		it.setSpecialType(str(work.get("special_type")));
		it.setPurchaseAgreement(str(work.get("purchase_agreement")));
		it.setEnableAgreement(Boolean.TRUE.equals(work.get("enable_agreement")));
		it.setItemCategory(mainCategoryIdForItemsColumn(work));
		it.setNospec(str(work.get("nospec")));
		if (work.get("brand_id") != null) {
			it.setBrandId((int) toLong(work.get("brand_id")));
		}
		it.setTaxRate(intOrDefault(work.get("tax_rate"), 13));
		it.setDistributorId((int) toLong(work.get("distributor_id")));
		it.setItemSource(str(work.getOrDefault("item_source", "mall")));
		it.setTdkContent(str(work.get("tdk_content")));
		it.setIsMarket(work.get("is_market") != null ? toInt(work.get("is_market")) : 1);
		it.setType(work.get("type") != null ? toInt(work.get("type")) : 0);
		it.setIsProfit(work.get("is_profit") != null ? Boolean.TRUE.equals(work.get("is_profit")) : false);
		it.setProfitType(work.get("profit_type") != null ? toInt(work.get("profit_type")) : 0);
		it.setRegionsId(regionsToCsv(work.get("regions_id")));
		it.setRegions(regionsToCsv(work.get("regions")));
		it.setDateType(str(work.get("date_type")));
		if (work.get("begin_date") != null) {
			it.setBeginDate(toInt(work.get("begin_date")));
		}
		if (work.get("end_date") != null) {
			it.setEndDate(toInt(work.get("end_date")));
		}
		if (work.get("fixed_term") != null) {
			it.setFixedTerm(toInt(work.get("fixed_term")));
		}
		it.setItemAddressProvince(str(work.get("item_address_province")));
		it.setItemAddressCity(str(work.get("item_address_city")));
		it.setApproveStatus(str(work.get("approve_status")));
		it.setAuditStatus(str(work.get("audit_status")));
		it.setPrice(toInt(work.get("price")));
		it.setCostPrice(work.get("cost_price") != null ? toInt(work.get("cost_price")) : 0);
		it.setMarketPrice(work.get("market_price") != null ? toInt(work.get("market_price")) : 0);
		it.setProfitFee(work.get("profit_fee") != null ? toInt(work.get("profit_fee")) : 0);
		it.setWeight(doubleValueOrZero(work.get("weight")));
		it.setVolume(doubleValueOrZero(work.get("volume")));
		it.setBarcode(str(work.get("barcode")));
		it.setItemUnit(str(work.get("item_unit")));
		it.setStore(toInt(work.get("store")));
		it.setIsDefault(Boolean.TRUE.equals(work.get("is_default")));
		// 字段默认值是 0L，不显式置 null 时 MyBatis 更新会把已有 goods_id/default_item_id 冲成 0
		it.setDefaultItemId(null);
		it.setGoodsId(null);
		if (work.get("default_item_id") != null) {
			long def = toLong(work.get("default_item_id"));
			if (def > 0L) {
				it.setDefaultItemId(def);
			}
		}
		if (work.get("goods_id") != null) {
			long gid = toLong(work.get("goods_id"));
			if (gid > 0L) {
				it.setGoodsId(gid);
			}
		}
		it.setPoint(pointsForItemsColumn(work));
		if (work.get("start_num") != null) {
			it.setStartNum(toInt(work.get("start_num")));
		}
		if (work.get("delivery_time") != null) {
			it.setDeliveryTime(toInt(work.get("delivery_time")));
		}
		it.setRebate(work.get("rebate") != null ? toInt(work.get("rebate")) : 0);
		it.setRebateType(StringUtils.hasText(str(work.get("rebate_type"))) ? str(work.get("rebate_type")) : "default");
		it.setSupplierId(work.get("supplier_id") != null ? (int) toLong(work.get("supplier_id")) : 0);
		it.setIsGift(ctx.isGift());
		it.setOrigincountryId(toLong(work.get("origincountry_id")));
		it.setTaxstrategyId(toLong(work.get("taxstrategy_id")));
		it.setTaxationNum(toInt(work.get("taxation_num")));
		String crossborderRate = str(work.get("crossborder_tax_rate")).trim();
		it.setCrossborderTaxRate(crossborderRate.isEmpty() ? "" : crossborderRate);
		if (work.get("is_prescription") != null) {
			it.setIsPrescription(toInt(work.get("is_prescription")));
		}
		if (work.get("is_medicine") != null) {
			it.setIsMedicine(toInt(work.get("is_medicine")));
		} else {
			it.setIsMedicine(0);
		}
		return it;
	}

	/**
	 * items.item_category 存主类目（item_main_cat_id）；body 里的 item_category[] 仅用于销售类目关联，mergeSku 后会覆盖同名键，
	 * 故落库时必须以 item_main_cat_id 为准。
	 */
	private static String mainCategoryIdForItemsColumn(Map<String, Object> work) {
		Object main = work.get("item_main_cat_id");
		String ms = str(main).trim();
		if (StringUtils.hasText(ms)) {
			return ms;
		}
		Object ic = work.get("item_category");
		if (ic instanceof List<?> || ic != null && ic.getClass().isArray()) {
			return "";
		}
		return str(ic).trim();
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

	private static long pointForRelTable(Map<String, Object> sku, Map<String, Object> work) {
		if (sku.get("point_num") != null) {
			return toLong(sku.get("point_num"));
		}
		if (work.get("point_num") != null) {
			return toLong(work.get("point_num"));
		}
		if (sku.get("point") != null) {
			return toLong(sku.get("point"));
		}
		if (work.get("point") != null) {
			return toLong(work.get("point"));
		}
		return 0L;
	}

	private static int pointsForItemsColumn(Map<String, Object> work) {
		if (work.get("point") != null) {
			return (int) toLong(work.get("point"));
		}
		return 0;
	}

	private static final Set<String> MERGE_SKU_SKIP_KEYS =
			Set.of("audit_status", "audit_reason", "audit_date");

	private static Map<String, Object> mergeSku(Map<String, Object> data, Map<String, Object> row) {
		Map<String, Object> m = new LinkedHashMap<>(data);
		for (Map.Entry<String, Object> e : row.entrySet()) {
			if (MERGE_SKU_SKIP_KEYS.contains(e.getKey())) {
				continue;
			}
			m.put(e.getKey(), e.getValue());
		}
		normalizeRegionsInMap(m);
		return m;
	}

	private static void normalizeRegionsInMap(Map<String, Object> m) {
		if (m.containsKey("regions_id")) {
			m.put("regions_id", regionsToCsv(m.get("regions_id")));
		}
		if (m.containsKey("regions")) {
			m.put("regions", regionsToCsv(m.get("regions")));
		}
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		String s = nospec.toString();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private static boolean onsaleLike(Object approve) {
		if (approve == null) {
			return false;
		}
		String s = approve.toString();
		return "onsale".equals(s) || "only_show".equals(s) || "offline_sale".equals(s);
	}

	private Map<Long, String> parseSpecImages(Object raw) {
		if (raw == null) {
			return Map.of();
		}
		try {
			List<Map<String, Object>> arr =
					raw instanceof String s ? objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {}) : asMapList(raw);
			Map<Long, String> out = new LinkedHashMap<>();
			for (Map<String, Object> m : arr) {
				if (m.get("spec_value_id") == null || m.get("item_image_url") == null) {
					continue;
				}
				String encoded = encodeSpecRelImageUrl(m.get("item_image_url"));
				if (encoded != null) {
					out.put(toLong(m.get("spec_value_id")), encoded);
				}
			}
			return out;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private List<Map<String, Object>> parseSpecItems(Object raw) {
		if (raw == null) {
			return List.of();
		}
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

	/** 仅保留正数分类 id；解析失败时抛出业务校验异常。 */
	private static List<Long> parseCategoryIdsForRel(Object ic) {
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

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(v.toString()) || "1".equals(v.toString());
	}

	private static boolean truthyGift(Object v) {
		return truthy(v);
	}

	/**
	 * 将 JSON 列数据序列化为合法 JSON 字符串，避免 List#toString() 输出非标准 JSON。
	 *
	 * @param nullAsEmptyJsonArray {@code true} 时 null → {@code []}（如 {@code pics}）；{@code false} 时 null → {@code ""}（如 {@code videos}）
	 */
	private static String itemSpecRelImageUrlOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty() || "[]".equals(t)) {
			return null;
		}
		return t;
	}

	private String encodeSpecRelImageUrl(Object img) {
		if (img == null) {
			return null;
		}
		if (img instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		try {
			return objectMapper.writeValueAsString(img);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to encode spec image url", e);
		}
	}

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

	private static int intOrDefault(Object o, int defaultVal) {
		if (o == null) {
			return defaultVal;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return defaultVal;
		}
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
		return Long.parseLong(s);
	}
}
