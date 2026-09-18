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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionCreateService {

	private final MessageSource messageSource;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageMainItemPromotionsMapper packageMainItemPromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;
	private final ObjectMapper objectMapper;

	public PackagePromotionCreateService(
			MessageSource messageSource,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			PackagePromotionsMapper packagePromotionsMapper,
			PackageMainItemPromotionsMapper packageMainItemPromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher,
			ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageMainItemPromotionsMapper = packageMainItemPromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.salespersonItemsShelvesJobDispatchPublisher = salespersonItemsShelvesJobDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		validatePackageName(params, locale);
		formatPackageParams(params, locale);
		checkPackageRequest(params, locale, false);
		marketingActivityCrossPromotionGuardService.checkActivityValidByPackage(params, 0L);

		long companyId = readLong(params.get("company_id"));
		List<Long> mainItemIds = readOrderedDistinctItemIds(params.get("main_items"), "item_id");
		Map<String, Object> mainSkuPack = marketingActivityCatalogAccess.loadSkuItemsList(companyId, mainItemIds);
		int total = ((Number) mainSkuPack.getOrDefault("total_count", 0)).intValue();
		if (total <= 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainList = (List<Map<String, Object>>) mainSkuPack.get("list");
		if (mainList == null || mainList.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		Map<String, Object> firstMainRow = mainList.get(0);
		long goodsId = readLong(firstMainRow.get("goods_id"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainItemsTyped = (List<Map<String, Object>>) params.get("main_items");
		Map<String, Object> firstMainReq = mainItemsTyped.get(0);
		long mainItemId = readLong(firstMainReq.get("item_id"));
		String mainItemPriceStr = Objects.toString(firstMainReq.get("item_price"), "").trim();
		BigDecimal mainItemPriceYuan = new BigDecimal(mainItemPriceStr);
		long mainItemPriceStored =
				mainItemPriceYuan.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).longValue();

		String packageName = Objects.toString(params.get("_validated_package_name"), "").trim();
		List<Object> validGradeElements = readValidGradeElements(params.get("valid_grade"), locale);
		String validGradeJoined =
				validGradeElements.stream().map(Objects::toString).collect(Collectors.joining(","));
		int usedPlatform = readUsedPlatformInt(params, locale);
		int startTime = toInt(params.get("start_time"));
		int endTime = toInt(params.get("end_time"));
		int ts = (int) (System.currentTimeMillis() / 1000L);

		PackagePromotions main = new PackagePromotions();
		main.setCompanyId(companyId);
		main.setPackageName(packageName);
		main.setGoodsId(goodsId);
		main.setMainItemId(mainItemId);
		main.setMainItemPrice(mainItemPriceStored);
		main.setValidGrade(validGradeJoined);
		main.setUsedPlatform(usedPlatform);
		main.setFreePostage(true);
		main.setPackageTotalPrice(0);
		main.setStartTime(startTime);
		main.setEndTime(endTime);
		main.setPackageStatus("AGREE");
		main.setReason("");
		main.setSourceId(readLong(params.get("source_id")));
		main.setSourceType(Objects.toString(params.get("source_type"), "admin"));
		main.setCreated(ts);
		main.setUpdated(ts);
		packagePromotionsMapper.insert(main);
		Long packageId = main.getPackageId();
		if (packageId == null || packageId <= 0L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}

		rebuildPackageMainChildRelationsAndTotalPrice(
				packageId, companyId, params, mainList, startTime, endTime, ts, locale);

		final long shelvesCompanyId = companyId;
		final long shelvesPackageId = packageId;
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							salespersonItemsShelvesJobDispatchPublisher.publish(
									shelvesCompanyId, shelvesPackageId, "package");
						}
					});
		} else {
			salespersonItemsShelvesJobDispatchPublisher.publish(companyId, packageId, "package");
		}

		PackagePromotions persisted = packagePromotionsMapper.selectById(packageId);
		if (persisted == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		return toSnakeCaseRow(persisted);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(String packageIdRaw, Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		validatePackageName(params, locale);
		validateUpdateActionRules(params, locale);
		formatPackageParams(params, locale);
		checkPackageRequest(params, locale, true);
		long packageId = parsePathPackageIdOrThrow(packageIdRaw, locale);
		marketingActivityCrossPromotionGuardService.checkActivityValidByPackage(params, packageId);

		long companyId = readLong(params.get("company_id"));
		List<Long> mainItemIds = readOrderedDistinctItemIds(params.get("main_items"), "item_id");
		Map<String, Object> mainSkuPack = marketingActivityCatalogAccess.loadSkuItemsList(companyId, mainItemIds);
		int total = ((Number) mainSkuPack.getOrDefault("total_count", 0)).intValue();
		if (total <= 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainList = (List<Map<String, Object>>) mainSkuPack.get("list");
		if (mainList == null || mainList.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		Map<String, Object> firstMainRow = mainList.get(0);
		long goodsId = readLong(firstMainRow.get("goods_id"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainItemsTyped = (List<Map<String, Object>>) params.get("main_items");
		Map<String, Object> firstMainReq = mainItemsTyped.get(0);
		long mainItemId = readLong(firstMainReq.get("item_id"));
		String mainItemPriceStr = Objects.toString(firstMainReq.get("item_price"), "").trim();
		BigDecimal mainItemPriceYuan = new BigDecimal(mainItemPriceStr);
		long mainItemPriceStored =
				mainItemPriceYuan.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).longValue();

		String packageName = Objects.toString(params.get("_validated_package_name"), "").trim();
		List<Object> validGradeElements = readValidGradeElements(params.get("valid_grade"), locale);
		String validGradeJoined =
				validGradeElements.stream().map(Objects::toString).collect(Collectors.joining(","));
		int usedPlatform = readUsedPlatformInt(params, locale);
		int startTime = toInt(params.get("start_time"));
		int endTime = toInt(params.get("end_time"));
		int ts = (int) (System.currentTimeMillis() / 1000L);

		LambdaUpdateWrapper<PackagePromotions> firstMainUpdate =
				new LambdaUpdateWrapper<PackagePromotions>()
						.eq(PackagePromotions::getPackageId, packageId)
						.set(PackagePromotions::getCompanyId, companyId)
						.set(PackagePromotions::getPackageName, packageName)
						.set(PackagePromotions::getGoodsId, goodsId)
						.set(PackagePromotions::getMainItemId, mainItemId)
						.set(PackagePromotions::getMainItemPrice, mainItemPriceStored)
						.set(PackagePromotions::getValidGrade, validGradeJoined)
						.set(PackagePromotions::getUsedPlatform, usedPlatform)
						.set(PackagePromotions::getFreePostage, true)
						.set(PackagePromotions::getPackageTotalPrice, 0)
						.set(PackagePromotions::getStartTime, startTime)
						.set(PackagePromotions::getEndTime, endTime)
						.set(PackagePromotions::getPackageStatus, "AGREE")
						.set(PackagePromotions::getReason, "")
						.set(PackagePromotions::getUpdated, ts);
		int n = packagePromotionsMapper.update(null, firstMainUpdate);
		if (n == 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.no_update_data_found", null, locale));
		}

		LambdaQueryWrapper<PackageMainItemPromotions> delMainWrap = new LambdaQueryWrapper<>();
		delMainWrap
				.eq(PackageMainItemPromotions::getPackageId, packageId)
				.eq(PackageMainItemPromotions::getCompanyId, companyId);
		packageMainItemPromotionsMapper.delete(delMainWrap);

		LambdaQueryWrapper<PackageItemPromotions> delItemWrap = new LambdaQueryWrapper<>();
		delItemWrap.eq(PackageItemPromotions::getPackageId, packageId).eq(PackageItemPromotions::getCompanyId, companyId);
		packageItemPromotionsMapper.delete(delItemWrap);

		rebuildPackageMainChildRelationsAndTotalPrice(
				packageId, companyId, params, mainList, startTime, endTime, ts, locale);

		final long shelvesCompanyId = companyId;
		final long shelvesPackageId = packageId;
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							salespersonItemsShelvesJobDispatchPublisher.publish(
									shelvesCompanyId, shelvesPackageId, "package");
						}
					});
		} else {
			salespersonItemsShelvesJobDispatchPublisher.publish(companyId, packageId, "package");
		}

		PackagePromotions persisted = packagePromotionsMapper.selectById(packageId);
		if (persisted == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.main_sku_not_found", null, locale));
		}
		return toSnakeCaseRow(persisted);
	}

	private long parsePathPackageIdOrThrow(String packageIdRaw, Locale locale) {
		try {
			if (!StringUtils.hasText(packageIdRaw == null ? "" : packageIdRaw.trim())) {
				throw new NumberFormatException();
			}
			long pid = Long.parseLong(packageIdRaw.trim());
			if (pid <= 0L) {
				throw new NumberFormatException();
			}
			return pid;
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.invalid_package_id", null, locale));
		}
	}

	private void validateUpdateActionRules(Map<String, Object> params, Locale locale) {
		Object mainItemObj = params.get("main_item");
		if (!(mainItemObj instanceof Map<?, ?>)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> mainItem = (Map<String, Object>) mainItemObj;
		Object idRaw = mainItem.get("item_id");
		try {
			if (idRaw == null) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
			}
			long id = idRaw instanceof Number num ? num.longValue() : Long.parseLong(idRaw.toString().trim());
			if (id <= 0L) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
		}
		Object priceRaw = mainItem.get("item_price");
		String priceStr = priceRaw == null ? "" : Objects.toString(priceRaw, "").trim();
		if (!StringUtils.hasText(priceStr)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.main_item_price_required", null, locale));
		}
		// Update path validates main_item only; main_items is enforced later in checkPackageRequest
		// so composite bodies hit used_platform / times before main_items (see admin-v1-update-test scenario 4).
	}

	private void rebuildPackageMainChildRelationsAndTotalPrice(
			long packageId,
			long companyId,
			Map<String, Object> params,
			List<Map<String, Object>> mainList,
			int startTime,
			int endTime,
			int updatedEpochSeconds,
			Locale locale) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainItemsTyped = (List<Map<String, Object>>) params.get("main_items");
		Map<Long, String> mainItemPriceById = new LinkedHashMap<>();
		for (Map<String, Object> row : mainItemsTyped) {
			long iid = readLong(row.get("item_id"));
			String pstr = Objects.toString(row.get("item_price"), "").trim();
			mainItemPriceById.put(iid, pstr);
		}
		for (Map<String, Object> data : mainList) {
			long rowItemId = readLong(data.get("item_id"));
			String priceStr = mainItemPriceById.get(rowItemId);
			if (!StringUtils.hasText(priceStr)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_price_required", null, locale));
			}
			long relPrice =
					new BigDecimal(priceStr)
							.multiply(new BigDecimal("100"))
							.setScale(0, RoundingMode.HALF_UP)
							.longValue();
			PackageMainItemPromotions rel = new PackageMainItemPromotions();
			rel.setPackageId(packageId);
			rel.setCompanyId(companyId);
			rel.setGoodsId(readLong(data.get("goods_id")));
			rel.setMainItemId(rowItemId);
			rel.setMainItemPrice(relPrice);
			packageMainItemPromotionsMapper.insert(rel);
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> itemsReq = (List<Map<String, Object>>) params.get("items");
		List<Long> childIdsOrdered = new ArrayList<>();
		for (Map<String, Object> it : itemsReq) {
			childIdsOrdered.add(readLong(it.get("item_id")));
		}
		Map<String, Object> childSkuPack = marketingActivityCatalogAccess.loadSkuItemsList(companyId, childIdsOrdered);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> childSkuList = (List<Map<String, Object>>) childSkuPack.get("list");
		if (childSkuList == null) {
			childSkuList = List.of();
		}
		Map<Long, Map<String, Object>> childSkuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> row : childSkuList) {
			long iid = readLong(row.get("item_id"));
			childSkuByItemId.put(iid, row);
		}
		Set<Long> distinctGoods = new LinkedHashSet<>();
		for (Long cid : childIdsOrdered) {
			Map<String, Object> r = childSkuByItemId.get(cid);
			if (r == null) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.items_not_found", null, locale));
			}
			distinctGoods.add(readLong(r.get("goods_id")));
		}
		if (distinctGoods.size() > 10) {
			throw new ResourceException(messageSource.getMessage("promotions.package.max_10_products", null, locale));
		}

		BigDecimal packageTotalYuan = BigDecimal.ZERO;
		LinkedHashMap<Long, Boolean> seenDefaultItem = new LinkedHashMap<>();
		for (Map<String, Object> reqItem : itemsReq) {
			long itemId = readLong(reqItem.get("item_id"));
			Map<String, Object> row = childSkuByItemId.get(itemId);
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.items_not_found", null, locale));
			}
			if ("drug".equalsIgnoreCase(Objects.toString(row.get("special_type"), ""))) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.prescription_drug_not_allowed", null, locale));
			}
			String newPriceStr = Objects.toString(reqItem.get("new_price"), "").trim();
			long defaultItemId = readOptionalLong(reqItem.get("default_item_id"));
			long defId = defaultItemId > 0L ? defaultItemId : itemId;
			boolean isShow = !seenDefaultItem.containsKey(defId);
			if (isShow) {
				seenDefaultItem.put(defId, true);
				packageTotalYuan = packageTotalYuan.add(new BigDecimal(newPriceStr));
			}
			int packagePrice =
					new BigDecimal(newPriceStr)
							.multiply(new BigDecimal("100"))
							.setScale(2, RoundingMode.HALF_UP)
							.intValue();
			String title = Objects.toString(row.get("item_name"), "");
			String imageDefaultId = firstPicString(row.get("pics"));
			Integer price = readIntegerPrice(row.get("price"));

			PackageItemPromotions pit = new PackageItemPromotions();
			pit.setPackageId(packageId);
			pit.setItemId(itemId);
			pit.setDefaultItemId(defId);
			pit.setCompanyId(companyId);
			pit.setTitle(title);
			pit.setImageDefaultId(imageDefaultId);
			pit.setPackagePrice(packagePrice);
			pit.setPrice(price);
			pit.setStatus(true);
			pit.setStartTime(startTime);
			pit.setEndTime(endTime);
			pit.setIsShow(isShow);
			pit.setCreated(updatedEpochSeconds);
			pit.setUpdated(updatedEpochSeconds);
			packageItemPromotionsMapper.insert(pit);
		}

		int packageTotalPriceInt =
				packageTotalYuan.setScale(0, RoundingMode.DOWN).intValueExact();
		LambdaUpdateWrapper<PackagePromotions> totalPriceUpdate =
				new LambdaUpdateWrapper<PackagePromotions>()
						.eq(PackagePromotions::getPackageId, packageId)
						.set(PackagePromotions::getPackageTotalPrice, packageTotalPriceInt)
						.set(PackagePromotions::getUpdated, updatedEpochSeconds);
		packagePromotionsMapper.update(null, totalPriceUpdate);
	}

	private void validatePackageName(Map<String, Object> params, Locale locale) {
		if (!params.containsKey("package_name")) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.package_name_required", null, locale));
		}
		Object raw = params.get("package_name");
		if (raw == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.package_name_required", null, locale));
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.package_name_required", null, locale));
		}
		String name;
		if (raw instanceof String s) {
			name = s.trim();
		} else if (raw instanceof Number n) {
			name = n.toString().trim();
		} else {
			name = Objects.toString(raw, "").trim();
			if (!StringUtils.hasText(name)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.package_name_required", null, locale));
			}
		}
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.package_name_required", null, locale));
		}
		if (name.length() > 50) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.package_name_too_long", null, locale));
		}
		params.put("_validated_package_name", name);
	}

	private void formatPackageParams(Map<String, Object> params, Locale locale) {
		Object mainItemsObj = params.get("main_items");
		if (mainItemsObj instanceof List<?> mainList) {
			for (Object row : mainList) {
				if (row instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mm = (Map<String, Object>) m;
					Object v = mm.get("item_price");
					String itemPriceTrimmed = Objects.toString(v, "").trim();
					if (StringUtils.hasText(itemPriceTrimmed)) {
						mm.put("item_price", formatMoneyPlain(v, locale, "promotions.package.main_item_price_required"));
					}
				}
			}
		}
		Object itemsObj = params.get("items");
		if (itemsObj instanceof List<?> itemList) {
			for (Object row : itemList) {
				if (row instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mm = (Map<String, Object>) m;
					Object v = mm.get("new_price");
					String newPriceTrimmed = Objects.toString(v, "").trim();
					if (StringUtils.hasText(newPriceTrimmed)) {
						mm.put("new_price", formatMoneyPlain(v, locale, "promotions.package.new_price_required"));
					}
				}
			}
		}
	}

	private String formatMoneyPlain(Object value, Locale locale, String badRequestMessageKey) {
		String s = Objects.toString(value, "").trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(messageSource.getMessage(badRequestMessageKey, null, locale));
		}
		try {
			return new BigDecimal(s).setScale(2, RoundingMode.HALF_UP).toPlainString();
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(badRequestMessageKey, null, locale));
		}
	}

	private void checkPackageRequest(Map<String, Object> params, Locale locale, boolean prioritizeTimeWhenItemsEmpty) {
		// Create: main_items → … → used_platform / times (legacy create validation order).
		// Update: mirror admin update validator order after main_item — items.*, valid_grade (array),
		// used_platform, start/end — all before main_items (update payload has no main_items key).
		boolean isUpdate = prioritizeTimeWhenItemsEmpty;
		boolean updateActionMirrorDone = false;
		boolean itemsRowsValidatedInUpdateMirror = false;

		boolean activityTimesAlreadyNormalized = false;
		if (prioritizeTimeWhenItemsEmpty) {
			Object itemsPeek = params.get("items");
			if (itemsPeek instanceof List<?> emptyCheck && emptyCheck.isEmpty()) {
				validateAndNormalizeActivityTimes(params, locale);
				activityTimesAlreadyNormalized = true;
			}
		}

		if (isUpdate) {
			Object itemsForMirror = params.get("items");
			if (itemsForMirror instanceof List<?> mirrorItemList && !mirrorItemList.isEmpty()) {
				validatePackageItemRequestRows(locale, mirrorItemList);
				itemsRowsValidatedInUpdateMirror = true;
			}
			validateUpdateValidGradeArrayRule(params, locale);
			if (!params.containsKey("used_platform")) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.used_platform_required", null, locale));
			}
			readUsedPlatformInt(params, locale);
			if (!activityTimesAlreadyNormalized) {
				validateAndNormalizeActivityTimes(params, locale);
				activityTimesAlreadyNormalized = true;
			}
			updateActionMirrorDone = true;
		}

		// 1. main_items (whole block)
		Object mainItemsObj = params.get("main_items");
		if (!(mainItemsObj instanceof List<?>) || ((List<?>) mainItemsObj).isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.main_items_required", null, locale));
		}
		List<?> mainList = (List<?>) mainItemsObj;

		// 2. main_items.* (ascending index; per row: item_id then item_price)
		for (int i = 0; i < mainList.size(); i++) {
			Object rowObj = mainList.get(i);
			if (!(rowObj instanceof Map<?, ?>)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> row = (Map<String, Object>) rowObj;
			long mainLineItemId = readRequiredPositiveSkuId(row.get("item_id"), locale, true);
			if (mainLineItemId <= 0L) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_id_required", null, locale));
			}
			if (!row.containsKey("item_price") || row.get("item_price") == null) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_price_required", null, locale));
			}
			String itemPriceStr = Objects.toString(row.get("item_price"), "").trim();
			if (!StringUtils.hasText(itemPriceStr)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.main_item_price_required", null, locale));
			}
		}

		// 3. items (whole block)
		Object itemsObj = params.get("items");
		if (!(itemsObj instanceof List<?>) || ((List<?>) itemsObj).isEmpty()) {
			throw new BadRequestException(messageSource.getMessage("promotions.package.items_required", null, locale));
		}
		List<?> itemList = (List<?>) itemsObj;

		// 4. items.* (ascending; per row: item_id then new_price)
		if (!(isUpdate && itemsRowsValidatedInUpdateMirror)) {
			validatePackageItemRequestRows(locale, itemList);
		}

		// 5. child line must not repeat the first main SKU (first main row validated in step 2)
		@SuppressWarnings("unchecked")
		Map<String, Object> firstMainRow = (Map<String, Object>) mainList.get(0);
		long firstMainId = readLong(firstMainRow.get("item_id"));
		for (int i = 0; i < itemList.size(); i++) {
			@SuppressWarnings("unchecked")
			Map<String, Object> it = (Map<String, Object>) itemList.get(i);
			long cid = readLong(it.get("item_id"));
			if (cid == firstMainId) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.cannot_contain_main_product", null, locale));
			}
		}

		// 6. valid_grade
		readValidGradeElements(params.get("valid_grade"), locale);

		// 7. used_platform
		if (!updateActionMirrorDone) {
			if (!params.containsKey("used_platform")) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.used_platform_required", null, locale));
			}
			readUsedPlatformInt(params, locale);
		}

		// 8–9. start_time / end_time (required, > 0) then start > end
		if (!activityTimesAlreadyNormalized) {
			validateAndNormalizeActivityTimes(params, locale);
		}

		// 10. loadItemIdAndTypeRows: missing SKU and regular/cross-border mix
		long companyId = readLong(params.get("company_id"));
		List<Long> mainIds = readOrderedDistinctItemIds(params.get("main_items"), "item_id");
		List<Long> childIds = readOrderedDistinctItemIds(params.get("items"), "item_id");
		List<Long> allSkuIds = mergeDistinctPreserveOrder(mainIds, childIds);
		List<Map<String, Object>> typeRows =
				marketingActivityCatalogAccess.loadItemIdAndTypeRows(companyId, allSkuIds);
		if (typeRows.size() < allSkuIds.size()) {
			throw new ResourceException(messageSource.getMessage("promotions.package.items_not_found", null, locale));
		}
		Object baseType = typeRows.get(0).get("type");
		for (Map<String, Object> row : typeRows) {
			if (!Objects.equals(row.get("type"), baseType)) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.regular_crossborder_cannot_package", null, locale));
			}
		}
	}

	/** Update: valid_grade must be an array when present; reject non-array shapes before used_platform. */
	private void validateUpdateValidGradeArrayRule(Map<String, Object> params, Locale locale) {
		if (!params.containsKey("valid_grade")) {
			return;
		}
		Object raw = params.get("valid_grade");
		if (raw == null) {
			return;
		}
		if (raw instanceof List<?>) {
			return;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				List<Object> parsed = objectMapper.readValue(s.trim(), new TypeReference<List<Object>>() {});
				if (parsed != null) {
					return;
				}
			} catch (Exception e) {
				// fall through
			}
			throw new ResourceException(
					messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
		}
		throw new ResourceException(
				messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
	}

	private void validatePackageItemRequestRows(Locale locale, List<?> itemList) {
		for (int i = 0; i < itemList.size(); i++) {
			Object rowObj = itemList.get(i);
			if (!(rowObj instanceof Map<?, ?>)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.item_id_required", null, locale));
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> it = (Map<String, Object>) rowObj;
			if (it.get("item_id") == null) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.item_id_required", null, locale));
			}
			long cid = readRequiredPositiveSkuId(it.get("item_id"), locale, false);
			if (cid <= 0L) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.item_id_required", null, locale));
			}
			Object np = it.get("new_price");
			String nps = np == null ? "" : Objects.toString(np, "").trim();
			if (!StringUtils.hasText(nps)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.package.new_price_required", null, locale));
			}
		}
	}

	private void validateAndNormalizeActivityTimes(Map<String, Object> params, Locale locale) {
		int st = parsePositiveEpochSeconds(params.get("start_time"));
		int et = parsePositiveEpochSeconds(params.get("end_time"));
		if (st <= 0 || et <= 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.package.activity_time_required", null, locale));
		}
		if (st > et) {
			throw new ResourceException(messageSource.getMessage("promotions.package.start_after_end", null, locale));
		}
		params.put("start_time", st);
		params.put("end_time", et);
	}

	private long readRequiredPositiveSkuId(Object raw, Locale locale, boolean mainLine) {
		if (raw == null) {
			return 0L;
		}
		String key = mainLine ? "promotions.package.main_item_id_required" : "promotions.package.item_id_required";
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(Objects.toString(raw, "").trim());
			if (v <= 0L) {
				return 0L;
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(key, null, locale));
		}
	}

	private List<Object> readValidGradeElements(Object raw, Locale locale) {
		if (raw instanceof List<?> l) {
			if (l.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
			}
			List<Object> out = new ArrayList<>();
			for (Object o : l) {
				out.add(o);
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				List<Object> parsed = objectMapper.readValue(s.trim(), new TypeReference<List<Object>>() {});
				if (parsed == null || parsed.isEmpty()) {
					throw new ResourceException(
							messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
				}
				return parsed;
			} catch (Exception e) {
				throw new ResourceException(
						messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
			}
		}
		throw new ResourceException(messageSource.getMessage("promotions.package.valid_grade_required", null, locale));
	}

	private int readUsedPlatformInt(Map<String, Object> params, Locale locale) {
		Object up = params.get("used_platform");
		int uv;
		try {
			uv = up instanceof Number n ? n.intValue() : Integer.parseInt(up.toString().trim());
		} catch (Exception e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.used_platform_invalid", null, locale));
		}
		if (!Set.of(0, 1, 2, 3).contains(uv)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.package.used_platform_invalid", null, locale));
		}
		return uv;
	}

	private static List<Long> readOrderedDistinctItemIds(Object rowsObj, String key) {
		if (!(rowsObj instanceof List<?> l)) {
			return List.of();
		}
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Object row : l) {
			if (!(row instanceof Map<?, ?> m)) {
				continue;
			}
			Object id = m.get(key);
			if (id == null) {
				continue;
			}
			long lid = id instanceof Number n ? n.longValue() : Long.parseLong(id.toString().trim());
			if (lid <= 0L) {
				continue;
			}
			if (seen.add(lid)) {
				out.add(lid);
			}
		}
		return out;
	}

	private static List<Long> mergeDistinctPreserveOrder(List<Long> a, List<Long> b) {
		LinkedHashSet<Long> s = new LinkedHashSet<>();
		s.addAll(a);
		s.addAll(b);
		return new ArrayList<>(s);
	}

	private static int parsePositiveEpochSeconds(Object v) {
		if (v == null) {
			return 0;
		}
		long sec;
		if (v instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		if (sec <= 0L || sec > Integer.MAX_VALUE) {
			return 0;
		}
		return (int) sec;
	}

	private String firstPicString(Object pics) {
		if (pics == null) {
			return "";
		}
		if (pics instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			return first != null ? first.toString() : "";
		}
		if (pics instanceof String s) {
			String t = s.trim();
			if (t.startsWith("[")) {
				try {
					JsonNode arr = objectMapper.readTree(t);
					if (arr.isArray() && arr.size() > 0) {
						JsonNode n0 = arr.get(0);
						return n0 != null && !n0.isNull() ? n0.asText("") : "";
					}
				} catch (Exception ignored) {
				}
			}
			return t;
		}
		return pics.toString();
	}

	private static Integer readIntegerPrice(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long readOptionalLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static Map<String, Object> toSnakeCaseRow(PackagePromotions e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("package_id", e.getPackageId());
		m.put("company_id", e.getCompanyId());
		m.put("goods_id", e.getGoodsId());
		m.put("main_item_id", e.getMainItemId());
		m.put("main_item_price", e.getMainItemPrice());
		m.put("package_name", e.getPackageName());
		m.put("valid_grade", e.getValidGrade());
		m.put("used_platform", e.getUsedPlatform());
		m.put("free_postage", e.getFreePostage());
		m.put("package_total_price", e.getPackageTotalPrice());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("package_status", e.getPackageStatus());
		m.put("reason", e.getReason());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("source_type", e.getSourceType());
		m.put("source_id", e.getSourceId());
		return m;
	}
}
