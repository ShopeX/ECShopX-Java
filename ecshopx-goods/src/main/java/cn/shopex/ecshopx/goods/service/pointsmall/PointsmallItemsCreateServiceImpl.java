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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsCreateService;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsRelCatsWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsCreateServiceImpl implements PointsmallItemsCreateService {

	private static final Set<String> ONSALE_LIKE = Set.of("onsale", "only_show", "offline_sale");

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final PointsmallItemSpecParamsResolver pointsmallItemSpecParamsResolver;
	private final PointsmallItemStoreRedisService pointsmallItemStoreRedisService;
	private final PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService;
	private final PointsmallItemsMultiLangWriteService pointsmallItemsMultiLangWriteService;
	private final ItemsMedicineService itemsMedicineService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public PointsmallItemsCreateServiceImpl(
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			PointsmallItemSpecParamsResolver pointsmallItemSpecParamsResolver,
			PointsmallItemStoreRedisService pointsmallItemStoreRedisService,
			PointsmallItemsRelCatsWriteService pointsmallItemsRelCatsWriteService,
			PointsmallItemsMultiLangWriteService pointsmallItemsMultiLangWriteService,
			ItemsMedicineService itemsMedicineService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.pointsmallItemSpecParamsResolver = pointsmallItemSpecParamsResolver;
		this.pointsmallItemStoreRedisService = pointsmallItemStoreRedisService;
		this.pointsmallItemsRelCatsWriteService = pointsmallItemsRelCatsWriteService;
		this.pointsmallItemsMultiLangWriteService = pointsmallItemsMultiLangWriteService;
		this.itemsMedicineService = itemsMedicineService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addItems(Map<String, Object> params) {
		try {
			addItemsCore(params);
		} catch (BadRequestException e) {
			throw e;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	private void addItemsCore(Map<String, Object> params) {
		long companyId = toLong(params.get("company_id"));
		String itemType = str(params.get("item_type"));
		if (!StringUtils.hasText(itemType)) {
			itemType = "normal";
			params.put("item_type", itemType);
		}

		Long goodsIdForBatch = parseTruthyGoodsId(params.get("goods_id"));

		Map<String, Object> data = commonParams(params);
		Map<String, Object> medicineData = new LinkedHashMap<>();
		itemsMedicineService.assertMedicineSettingsAndEnrichParams(companyId, params, data);
		if (Integer.valueOf(1).equals(toIntBoxed(params.get("is_medicine")))) {
			medicineData.put("active", true);
		}

		if (goodsIdForBatch == null && hasTruthyPathItemId(params)) {
			long pathItemId = toLong(params.get("item_id"));
			PointsmallItems existingAfterProcess = processUpdateItemEquivalent(pathItemId, companyId, params);
			Long gid = existingAfterProcess.getGoodsId();
			if (gid != null && gid > 0) {
				goodsIdForBatch = gid;
			}
		}

		List<Long> itemIds = new ArrayList<>();
		Long defaultItemId = null;

		boolean forceCreate = !hasTruthyPathItemId(params);

		if (isMultiSpec(params.get("nospec"))) {
			params.put("nospec", "false");
			data.put("nospec", "false");
			Map<Long, String> specImages = parseSpecImages(params);
			data.put("spec_images", specImages);

			List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));
			long minPriceFen = -1L;
			for (Map<String, Object> row : specItems) {
				Map<String, Object> res = createOneSku(data, row, companyId, itemType, params, medicineData, forceCreate);
				long iid = toLong(res.get("item_id"));
				itemIds.add(iid);
				long rowFen = moneyToFen(row.get("price"));
				if (minPriceFen < 0) {
					minPriceFen = rowFen;
				} else if (rowFen < minPriceFen) {
					minPriceFen = rowFen;
					defaultItemId = iid;
				}
				if (defaultItemId == null && onsaleLike(row.get("approve_status"))) {
					defaultItemId = iid;
				}
			}
			if (defaultItemId == null && !itemIds.isEmpty()) {
				defaultItemId = itemIds.get(0);
			}
		} else {
			params.put("nospec", "true");
			data.put("nospec", "true");
			Map<String, Object> res = createOneSku(data, params, companyId, itemType, params, medicineData, false);
			defaultItemId = toLong(res.get("item_id"));
			itemIds.add(defaultItemId);
		}

		if (goodsIdForBatch == null) {
			goodsIdForBatch = defaultItemId;
		}

		if (defaultItemId == null || itemIds.isEmpty()) {
			throw new ResourceException("创建商品失败");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<PointsmallItems> uAll = new LambdaUpdateWrapper<>();
		uAll.in(PointsmallItems::getItemId, itemIds)
				.set(PointsmallItems::getDefaultItemId, defaultItemId)
				.set(PointsmallItems::getGoodsId, goodsIdForBatch)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, uAll);

		LambdaUpdateWrapper<PointsmallItems> uClear = new LambdaUpdateWrapper<>();
		uClear.eq(PointsmallItems::getDefaultItemId, defaultItemId)
				.ne(PointsmallItems::getItemId, defaultItemId)
				.set(PointsmallItems::getIsDefault, false)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, uClear);

		LambdaUpdateWrapper<PointsmallItems> uDef = new LambdaUpdateWrapper<>();
		uDef.eq(PointsmallItems::getItemId, defaultItemId)
				.set(PointsmallItems::getIsDefault, true)
				.set(PointsmallItems::getUpdated, now);
		pointsmallItemsMapper.update(null, uDef);

		boolean isCreateRelData = !Boolean.FALSE.equals(params.get("isCreateRelData"));
		if (isCreateRelData) {
			List<Long> catIds = parseCategoryIds(params.get("item_category"));
			pointsmallItemsRelCatsWriteService.setItemsCategory(companyId, defaultItemId, catIds);
			itemsRelBrand(params, defaultItemId);
			itemsRelParams(params, defaultItemId);
		}
	}

	private static Long parseTruthyGoodsId(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0) {
				throw new BadRequestException("商品组ID格式错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品组ID格式错误");
		}
	}

	private Map<String, Object> commonParams(Map<String, Object> params) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("company_id", params.get("company_id"));
		d.put("item_type", str(params.get("item_type"), "normal"));
		d.put("consume_type", str(params.get("consume_type"), "every"));
		d.put("item_name", params.get("item_name"));
		d.put("item_unit", str(params.get("item_unit"), ""));
		d.put("brief", str(params.get("brief"), ""));
		d.put("sort", params.get("sort") != null ? toInt(params.get("sort")) : 1);
		d.put("templates_id", params.get("templates_id"));
		d.put("is_show_specimg", truthy(params.get("is_show_specimg")));
		d.put("pics", normalizePicsForJsonColumn(params.get("pics")));
		d.put("video_type", str(params.get("video_type"), "local"));
		if ("tencent".equals(d.get("video_type"))) {
			d.put("videos", str(params.get("tencent_vid"), ""));
		} else {
			d.put("videos", str(params.get("videos"), ""));
		}
		d.put("intro", str(params.get("intro"), ""));
		d.put("special_type", str(params.get("special_type"), "normal"));
		d.put("purchase_agreement", str(params.get("purchase_agreement"), ""));
		d.put("enable_agreement", truthy(params.get("enable_agreement")));
		d.put("item_category", params.get("item_main_cat_id") != null ? str(params.get("item_main_cat_id"), "") : "");
		d.put("nospec", str(params.get("nospec"), "true"));
		d.put("item_address_city", str(params.get("item_address_city"), ""));
		d.put("item_address_province", str(params.get("item_address_province"), ""));
		d.put("date_type", str(params.get("date_type"), ""));
		d.put("begin_date", params.get("begin_date") != null ? str(params.get("begin_date"), "") : "");
		d.put("end_date", params.get("end_date") != null ? str(params.get("end_date"), "") : "");
		d.put("fixed_term", params.get("fixed_term") != null ? str(params.get("fixed_term"), "") : "");
		d.put("brand_id", params.get("brand_id") != null ? toInt(params.get("brand_id")) : 0);
		d.put("tax_rate", params.get("tax_rate") != null ? toInt(params.get("tax_rate")) : 13);
		d.put("crossborder_tax_rate", str(params.get("crossborder_tax_rate"), ""));
		d.put("origincountry_id", params.get("origincountry_id") != null ? toLong(params.get("origincountry_id")) : 0L);
		d.put("type", params.get("type") != null ? toInt(params.get("type")) : 0);
		d.put("is_medicine", params.get("is_medicine") != null ? toInt(params.get("is_medicine")) : 0);
		d.put("audit_status", "approved");
		if (params.get("regions_id") != null) {
			d.put("regions_id", regionsJoin(params.get("regions_id")));
		} else {
			d.put("regions_id", "");
		}
		return d;
	}

	private String normalizePicsForJsonColumn(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Collection<?> c && !(raw instanceof Map<?, ?>)) {
			try {
				return objectMapper.writeValueAsString(c);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("图片数据格式错误");
			}
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		char first = s.charAt(0);
		if (first == '[' || first == '{') {
			try {
				JsonNode node = objectMapper.readTree(s);
				return objectMapper.writeValueAsString(node);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("图片数据格式错误");
			}
		}
		try {
			return objectMapper.writeValueAsString(List.of(s));
		} catch (JsonProcessingException e) {
			throw new ResourceException("图片数据序列化失败");
		}
	}

	private PointsmallItems processUpdateItemEquivalent(long pathItemId, long companyId, Map<String, Object> params) {
		LambdaQueryWrapper<PointsmallItems> q0 = new LambdaQueryWrapper<>();
		q0.eq(PointsmallItems::getItemId, pathItemId).eq(PointsmallItems::getCompanyId, companyId);
		PointsmallItems existing = pointsmallItemsMapper.selectOne(q0);
		if (existing == null) {
			throw new ResourceException("更新的商品无效");
		}

		List<Long> targetItemIdsForAttrDelete;

		if (isMultiSpec(existing.getNospec())) {
			long defaultItemIdDb = existing.getDefaultItemId() != null && existing.getDefaultItemId() > 0
					? existing.getDefaultItemId()
					: pathItemId;

			LambdaQueryWrapper<PointsmallItems> qs = new LambdaQueryWrapper<>();
			qs.eq(PointsmallItems::getCompanyId, companyId).eq(PointsmallItems::getDefaultItemId, defaultItemIdDb);
			List<PointsmallItems> siblings = pointsmallItemsMapper.selectList(qs);

			List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));

			List<Long> newItemIds = new ArrayList<>();
			for (Map<String, Object> row : specItems) {
				long rid = toLong(row.get("item_id"));
				if (rid > 0) {
					newItemIds.add(rid);
				}
			}

			List<Long> deleteIds = new ArrayList<>();
			for (PointsmallItems sib : siblings) {
				long sid = sib.getItemId();
				if (!newItemIds.contains(sid)) {
					deleteIds.add(sid);
				}
			}

			for (Long delId : deleteIds) {
				pointsmallItemStoreRedisService.delete(delId);
			}
			if (!deleteIds.isEmpty()) {
				LambdaQueryWrapper<PointsmallItems> qd = new LambdaQueryWrapper<>();
				qd.eq(PointsmallItems::getCompanyId, companyId).in(PointsmallItems::getItemId, deleteIds);
				pointsmallItemsMapper.delete(qd);
			}

			pointsmallItemsRelCatsWriteService.deleteRelCatsByItemId(companyId, pathItemId);
			targetItemIdsForAttrDelete = newItemIds;
		} else {
			targetItemIdsForAttrDelete = List.of(pathItemId);
		}

		if (!targetItemIdsForAttrDelete.isEmpty()) {
			LambdaQueryWrapper<PointsmallItemRelAttributes> qa = new LambdaQueryWrapper<>();
			qa.eq(PointsmallItemRelAttributes::getCompanyId, companyId)
					.in(PointsmallItemRelAttributes::getItemId, targetItemIdsForAttrDelete);
			pointsmallItemRelAttributesMapper.delete(qa);
		}

		return existing;
	}

	private static boolean hasTruthyPathItemId(Map<String, Object> params) {
		return toLong(params.get("item_id")) > 0;
	}

	private Map<String, Object> createOneSku(
			Map<String, Object> dataTemplate,
			Map<String, Object> skuParams,
			long companyId,
			String itemType,
			Map<String, Object> topParams,
			Map<String, Object> medicineData,
			boolean forceCreate) {
		Map<String, Object> skuData = new LinkedHashMap<>(dataTemplate);
		pointsmallItemSpecParamsResolver.resolve(skuData, skuParams, companyId, itemType, forceCreate);

		long rowItemId = toLong(skuParams.get("item_id"));
		if (!forceCreate && rowItemId > 0) {
			LambdaQueryWrapper<PointsmallItems> q = new LambdaQueryWrapper<>();
			q.eq(PointsmallItems::getItemId, rowItemId).eq(PointsmallItems::getCompanyId, companyId);
			PointsmallItems owned = pointsmallItemsMapper.selectOne(q);
			if (owned == null) {
				throw new ResourceException("更新的商品无效");
			}
			PointsmallItems entity = buildItemEntity(skuData, topParams);
			entity.setItemId(rowItemId);
			entity.setCreated(owned.getCreated());
			int now = (int) (System.currentTimeMillis() / 1000L);
			entity.setUpdated(now);
			int u = pointsmallItemsMapper.updateById(entity);
			if (u <= 0) {
				throw new ResourceException("更新的商品无效");
			}

			Map<String, Object> res = new LinkedHashMap<>();
			res.put("item_id", rowItemId);
			res.put("company_id", companyId);
			res.put("approve_status", entity.getApproveStatus());
			res.put("price", entity.getPrice());

			int store = entity.getStore() != null ? entity.getStore() : 0;
			if (store > 0) {
				pointsmallItemStoreRedisService.save(rowItemId, store);
			}

			writeItemSpecs(companyId, rowItemId, skuData, skuParams);

			String lang = resolveRequestLang(topParams, skuParams);
			pointsmallItemsMultiLangWriteService.afterItemUpdate(rowItemId, companyId, skuParams, lang);

			if (medicineData != null && !medicineData.isEmpty()) {
				Map<String, Object> skuForMed = new LinkedHashMap<>(skuParams);
				if (skuParams != topParams) {
					skuForMed.put("medicine_spec", str(skuParams.get("spec_name")));
					skuForMed.put("max_num", skuParams.get("max_num") != null ? skuParams.get("max_num") : 0);
				}
				itemsMedicineService.updateItemMedicineData(skuForMed, medicineData, res);
			}
			return res;
		}

		PointsmallItems entity = buildItemEntity(skuData, topParams);
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);
		pointsmallItemsMapper.insert(entity);
		Long itemId = entity.getItemId();
		if (itemId == null || itemId <= 0) {
			throw new ResourceException("创建商品失败");
		}

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("item_id", itemId);
		res.put("company_id", companyId);
		res.put("approve_status", entity.getApproveStatus());
		res.put("price", entity.getPrice());

		int store = entity.getStore() != null ? entity.getStore() : 0;
		if (store > 0) {
			pointsmallItemStoreRedisService.save(itemId, store);
		}

		writeItemSpecs(companyId, itemId, skuData, skuParams);

		String lang = resolveRequestLang(topParams, skuParams);
		pointsmallItemsMultiLangWriteService.afterItemCreate(itemId, companyId, skuParams, lang);

		if (medicineData != null && !medicineData.isEmpty()) {
			Map<String, Object> skuForMed = new LinkedHashMap<>(skuParams);
			if (skuParams != topParams) {
				skuForMed.put("medicine_spec", str(skuParams.get("spec_name")));
				skuForMed.put("max_num", skuParams.get("max_num") != null ? skuParams.get("max_num") : 0);
			}
			itemsMedicineService.updateItemMedicineData(skuForMed, medicineData, res);
		}
		return res;
	}

	private static String pointsItemSpecRelImageUrlOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty() || "[]".equals(t)) {
			return null;
		}
		return t;
	}

	private void writeItemSpecs(long companyId, long itemId, Map<String, Object> skuData, Map<String, Object> skuParams) {
		List<Map<String, Object>> specRows = extractItemSpecRows(skuParams.get("item_spec"));
		if (specRows.isEmpty()) {
			return;
		}
		Map<Long, String> specImages = specImageMap(skuData.get("spec_images"));
		int sort = 0;
		for (Map<String, Object> row : specRows) {
			long specId = toLong(row.get("spec_id"));
			long specValueId = toLong(row.get("spec_value_id"));
			int tempSort = row.get("attribute_sort") != null ? (int) toLong(row.get("attribute_sort")) : 0;
			int attributeSort = tempSort + sort;
			sort++;
			String itemImageUrl = pointsItemSpecRelImageUrlOrNull(specImages.get(specValueId));

			LambdaQueryWrapper<PointsmallItemRelAttributes> q = new LambdaQueryWrapper<>();
			q.eq(PointsmallItemRelAttributes::getCompanyId, companyId)
					.eq(PointsmallItemRelAttributes::getItemId, itemId)
					.eq(PointsmallItemRelAttributes::getAttributeId, specId)
					.eq(PointsmallItemRelAttributes::getAttributeType, "item_spec");
			PointsmallItemRelAttributes existing = pointsmallItemRelAttributesMapper.selectOne(q);

			if (existing != null) {
				existing.setAttributeSort(attributeSort);
				existing.setImageUrl(itemImageUrl);
				existing.setAttributeValueId(specValueId);
				existing.setCustomAttributeValue(row.get("spec_custom_value_name") != null ? str(row.get("spec_custom_value_name")) : null);
				int u = pointsmallItemRelAttributesMapper.updateById(existing);
				if (u <= 0) {
					throw new ResourceException("未查询到更新数据");
				}
			} else {
				PointsmallItemRelAttributes rel = new PointsmallItemRelAttributes();
				rel.setCompanyId(companyId);
				rel.setItemId(itemId);
				rel.setAttributeId(specId);
				rel.setAttributeSort(attributeSort);
				rel.setAttributeType("item_spec");
				rel.setImageUrl(itemImageUrl);
				rel.setAttributeValueId(specValueId);
				rel.setCustomAttributeValue(row.get("spec_custom_value_name") != null ? str(row.get("spec_custom_value_name")) : null);
				pointsmallItemRelAttributesMapper.insert(rel);
			}
		}
	}

	private List<Map<String, Object>> extractItemSpecRows(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				Object parsed = objectMapper.readValue(s, Object.class);
				return toMapList(parsed);
			} catch (JsonProcessingException e) {
				throw new ResourceException("您选中的规格不存在");
			}
		}
		if (raw instanceof List<?> list) {
			return toMapList(list);
		}
		return List.of();
	}

	private static List<Map<String, Object>> toMapList(Object parsed) {
		if (!(parsed instanceof List<?> list)) {
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

	private static List<Map<String, Object>> toMapList(List<?> list) {
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

	@SuppressWarnings("unchecked")
	private static Map<Long, String> specImageMap(Object raw) {
		if (!(raw instanceof Map<?, ?> m)) {
			return Map.of();
		}
		Map<Long, String> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(toLong(e.getKey()), e.getValue() != null ? e.getValue().toString() : "");
		}
		return out;
	}

	private Map<Long, String> parseSpecImages(Map<String, Object> params) {
		if (!params.containsKey("spec_images")) {
			return new LinkedHashMap<>();
		}
		Object raw = params.get("spec_images");
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return new LinkedHashMap<>();
			}
			try {
				List<Map<String, Object>> rows = objectMapper.readValue(s, new TypeReference<>() {
				});
				return specImageRowsToMap(rows);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("规格图片数据格式错误");
			}
		}
		if (raw instanceof List<?> list) {
			return specImageRowsToMap(toMapList(list));
		}
		throw new BadRequestException("规格图片数据格式错误");
	}

	private static Map<Long, String> specImageRowsToMap(List<Map<String, Object>> rows) {
		Map<Long, String> map = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long specValueId = toLong(row.get("spec_value_id"));
			String url = row.get("item_image_url") != null ? str(row.get("item_image_url")) : "";
			map.put(specValueId, url);
		}
		return map;
	}

	private List<Map<String, Object>> parseSpecItems(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请填写规格商品数据");
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("请填写规格商品数据");
			}
			try {
				Object parsed = objectMapper.readValue(s, Object.class);
				return assertSpecItemArray(parsed);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("规格商品数据格式错误");
			}
		}
		if (raw instanceof List<?>) {
			return assertSpecItemArray(raw);
		}
		throw new BadRequestException("规格商品数据格式错误");
	}

	private static List<Map<String, Object>> assertSpecItemArray(Object parsed) {
		if (!(parsed instanceof List<?> list)) {
			throw new BadRequestException("规格商品数据格式错误");
		}
		if (list.isEmpty()) {
			throw new BadRequestException("请填写规格商品数据");
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				throw new BadRequestException("规格商品数据格式错误");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
		}
		return out;
	}

	private void itemsRelBrand(Map<String, Object> params, long defaultItemId) {
		Object brandRaw = params.get("brand_id");
		if (brandRaw == null || !StringUtils.hasText(brandRaw.toString().trim())) {
			return;
		}
		long companyId = toLong(params.get("company_id"));
		long attributeId = Long.parseLong(brandRaw.toString().trim());
		PointsmallItemRelAttributes row = new PointsmallItemRelAttributes();
		row.setCompanyId(companyId);
		row.setItemId(defaultItemId);
		row.setAttributeId(attributeId);
		row.setAttributeType("brand");
		row.setAttributeSort(0);
		pointsmallItemRelAttributesMapper.insert(row);
	}

	private void itemsRelParams(Map<String, Object> params, long defaultItemId) {
		List<Map<String, Object>> rows = parseItemParamRows(params.get("item_params"));
		if (rows.isEmpty()) {
			return;
		}
		long companyId = toLong(params.get("company_id"));
		for (Map<String, Object> r : rows) {
			PointsmallItemRelAttributes row = new PointsmallItemRelAttributes();
			row.setCompanyId(companyId);
			row.setItemId(defaultItemId);
			row.setAttributeId(toLong(r.get("attribute_id")));
			row.setAttributeType("item_params");
			if (r.get("attribute_value_id") != null) {
				row.setAttributeValueId(toLong(r.get("attribute_value_id")));
			}
			if (r.get("attribute_value_name") != null) {
				row.setCustomAttributeValue(str(r.get("attribute_value_name")));
			}
			row.setAttributeSort(0);
			pointsmallItemRelAttributesMapper.insert(row);
		}
	}

	private List<Map<String, Object>> parseItemParamRows(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				Object parsed = objectMapper.readValue(s, Object.class);
				return toMapList(parsed);
			} catch (JsonProcessingException e) {
				throw new ResourceException("您选中的参数不存在");
			}
		}
		if (raw instanceof List<?> list) {
			return toMapList(list);
		}
		return List.of();
	}

	private List<Long> parseCategoryIds(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				collectCategoryIdsFromObject(o, out);
			}
			return out.stream().filter(id -> id > 0).collect(Collectors.toList());
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return List.of();
			}
			if (t.startsWith("[")) {
				throw new ResourceException("选中的分类不存在 或 错误");
			}
			long x = parseCategoryPlainLong(t);
			return x > 0 ? List.of(x) : List.of();
		}
		String asStr = str(v);
		if (!StringUtils.hasText(asStr)) {
			return List.of();
		}
		long x = parseCategoryPlainLong(asStr);
		return x > 0 ? List.of(x) : List.of();
	}

	private void collectCategoryIdsFromObject(Object o, List<Long> out) {
		if (o == null) {
			return;
		}
		if (o instanceof List<?> nested) {
			for (Object x : nested) {
				collectCategoryIdsFromObject(x, out);
			}
			return;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return;
			}
			if (t.startsWith("[")) {
				throw new ResourceException("选中的分类不存在 或 错误");
			}
			out.add(parseCategoryPlainLong(t));
			return;
		}
		if (o instanceof Number n) {
			out.add(n.longValue());
			return;
		}
		out.add(parseCategoryPlainLong(o.toString().trim()));
	}

	private long parseCategoryPlainLong(String t) {
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("选中的分类不存在 或 错误");
		}
	}

	private PointsmallItems buildItemEntity(Map<String, Object> d, Map<String, Object> topParams) {
		PointsmallItems it = new PointsmallItems();
		it.setCompanyId(toLong(d.get("company_id")));
		it.setItemType(str(d.get("item_type"), "normal"));
		it.setConsumeType(str(d.get("consume_type"), "every"));
		it.setItemName(str(d.get("item_name")));
		it.setItemUnit(str(d.get("item_unit"), "个"));
		it.setBrief(str(d.get("brief"), ""));
		it.setSort(d.get("sort") instanceof Number n ? n.intValue() : toInt(d.get("sort")));
		it.setTemplatesId(d.get("templates_id") != null ? (int) toLong(d.get("templates_id")) : null);
		it.setIsShowSpecimg(d.get("is_show_specimg") instanceof Boolean b ? b : truthy(d.get("is_show_specimg")));
		it.setPics(str(d.get("pics"), ""));
		it.setVideoType(str(d.get("video_type"), "local"));
		it.setVideos(str(d.get("videos"), ""));
		it.setIntro(str(d.get("intro"), ""));
		it.setSpecialType(str(d.get("special_type"), "normal"));
		it.setPurchaseAgreement(str(d.get("purchase_agreement"), ""));
		it.setEnableAgreement(d.get("enable_agreement") instanceof Boolean b ? b : truthy(d.get("enable_agreement")));
		it.setItemCategory(str(d.get("item_category"), ""));
		it.setNospec(str(d.get("nospec"), "true"));
		it.setItemAddressCity(str(d.get("item_address_city"), ""));
		it.setItemAddressProvince(str(d.get("item_address_province"), ""));
		it.setDateType(emptyToNull(str(d.get("date_type"), "")));
		it.setBeginDate(parseOptionalEpochInt(d.get("begin_date")));
		it.setEndDate(parseOptionalEpochInt(d.get("end_date")));
		it.setFixedTerm(parseOptionalEpochInt(d.get("fixed_term")));
		it.setBrandId(d.get("brand_id") != null ? (int) toLong(d.get("brand_id")) : 0);
		it.setTaxRate(d.get("tax_rate") != null ? (int) toLong(d.get("tax_rate")) : 13);
		it.setCrossborderTaxRate(str(d.get("crossborder_tax_rate"), ""));
		it.setOrigincountryId(d.get("origincountry_id") != null ? toLong(d.get("origincountry_id")) : 0L);
		it.setType(d.get("type") != null ? toInt(d.get("type")) : 0);
		it.setIsMedicine(topParams.get("is_medicine") != null ? toInt(topParams.get("is_medicine")) : 0);
		it.setIsPrescription(d.get("is_prescription") != null ? toInt(d.get("is_prescription")) : 0);
		it.setAuditStatus(str(d.get("audit_status"), "approved"));
		it.setRegionsId(str(d.get("regions_id"), ""));

		it.setItemBn(str(d.get("item_bn"), ""));
		it.setWeight(d.get("weight") != null ? toDouble(d.get("weight")) : 0.0);
		if (d.get("volume") != null && StringUtils.hasText(str(d.get("volume")))) {
			it.setVolume(toDouble(d.get("volume")));
		}
		it.setBarcode(str(d.get("barcode"), ""));
		it.setPrice(d.get("price") instanceof Number n ? n.intValue() : toInt(d.get("price")));
		it.setCostPrice(d.get("cost_price") instanceof Number n ? n.intValue() : 0);
		it.setMarketPrice(d.get("market_price") instanceof Number n ? n.intValue() : 0);
		it.setPoint(d.get("point") instanceof Number n ? n.intValue() : 0);
		it.setPayClass(str(d.get("pay_class"), "point"));
		it.setStore(d.get("store") instanceof Number n ? n.intValue() : toInt(d.get("store")));
		it.setApproveStatus(str(d.get("approve_status"), "onsale"));
		it.setIsDefault(d.get("is_default") instanceof Boolean b ? b : Boolean.TRUE.equals(d.get("is_default")) || "true".equalsIgnoreCase(str(d.get("is_default"))));
		it.setGoodsId(0L);
		it.setDefaultItemId(0L);
		it.setSales(0);
		return it;
	}

	private static String emptyToNull(String s) {
		return StringUtils.hasText(s) ? s : null;
	}

	private static Integer parseOptionalEpochInt(Object v) {
		if (v == null) {
			return null;
		}
		String s = str(v);
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return (int) Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Prefer top-level body {@code country_code}, then sku row, then Accept-Language / default. */
	private String resolveRequestLang(Map<String, Object> topParams, Map<String, Object> skuParams) {
		return RequestCountryCode.resolve(langueProperties, null, topParams, skuParams);
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		String s = nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private static boolean onsaleLike(Object approve) {
		if (approve == null) {
			return false;
		}
		return ONSALE_LIKE.contains(approve.toString().trim());
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static String regionsJoin(Object v) {
		if (v instanceof List<?> list) {
			return list.stream().map(Object::toString).collect(Collectors.joining(","));
		}
		return str(v);
	}

	private static int moneyToFen(Object v) {
		if (v == null) {
			return 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		return new BigDecimal(s).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static long toLong(Object o) {
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
		return Long.parseLong(s);
	}

	private static int toInt(Object o) {
		return (int) toLong(o);
	}

	private static Integer toIntBoxed(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static double toDouble(Object o) {
		if (o == null) {
			return 0.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		return Double.parseDouble(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String str(Object o, String def) {
		String s = str(o);
		return StringUtils.hasText(s) ? s : def;
	}
}
