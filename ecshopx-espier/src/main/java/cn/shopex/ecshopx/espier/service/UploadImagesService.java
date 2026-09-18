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

package cn.shopex.ecshopx.espier.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.espier.domain.UploadImages;
import cn.shopex.ecshopx.espier.domain.UploadImagesCat;
import cn.shopex.ecshopx.espier.mapper.UploadImagesCatMapper;
import cn.shopex.ecshopx.espier.mapper.UploadImagesMapper;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.espier.storage.LocalStoragePublicUrl;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UploadImagesService {

	private final UploadImagesMapper uploadImagesMapper;
	private final FileStorageService fileStorageService;
	private final StorageProperties storageProperties;
	private final UploadImagesCatMapper uploadImagesCatMapper;
	private final CommonLangModReadService commonLangModReadService;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public UploadImagesService(
			UploadImagesMapper uploadImagesMapper,
			FileStorageService fileStorageService,
			StorageProperties storageProperties,
			UploadImagesCatMapper uploadImagesCatMapper,
			CommonLangModReadService commonLangModReadService,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.uploadImagesMapper = uploadImagesMapper;
		this.fileStorageService = fileStorageService;
		this.storageProperties = storageProperties;
		this.uploadImagesCatMapper = uploadImagesCatMapper;
		this.commonLangModReadService = commonLangModReadService;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> saveImage(
			HttpServletRequest request,
			long companyId,
			String operatorType,
			Long operatorId,
			long merchantIdFromJwt,
			Map<String, Object> mergedParams) {
		List<String> errors = new ArrayList<>();
		String name = stringFromParams(mergedParams, "image_name");
		if (name == null) {
			errors.add("图片名称不能为空");
		} else if (name.length() > 100) {
			errors.add("图片名称不能超过100个字符");
		}
		String storage = stringFromParams(mergedParams, "storage");
		if (storage == null) {
			errors.add("存储引擎不能为空");
		}
		String imageUrl = stringFromParams(mergedParams, "image_url");
		if (imageUrl == null) {
			errors.add("图片地址不能为空");
		}
		if (mergedParams.containsKey("image_type")) {
			Object rawType = mergedParams.get("image_type");
			if (rawType != null) {
				String t = String.valueOf(rawType).trim();
				if (!t.isEmpty() && t.length() > 20) {
					errors.add("图片类型不能超过20个字符");
				}
			}
		}
		if (!errors.isEmpty()) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(String.join("，", errors));
		}

		long imageCatId = 0L;
		Object rawCat = mergedParams.get("image_cat_id");
		if (rawCat != null) {
			String t = String.valueOf(rawCat).trim();
			if (!t.isEmpty()) {
				try {
					imageCatId = Long.parseLong(t);
				} catch (NumberFormatException e) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException("图片分类ID格式不正确");
				}
			}
		}

		long supplierId = 0L;
		long merchantId = 0L;
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("supplier".equals(ot)) {
			supplierId = operatorId == null ? 0L : operatorId;
		} else if ("merchant".equals(ot)) {
			merchantId = merchantIdFromJwt;
		}

		UploadImages entity = new UploadImages();
		entity.setCompanyId(companyId);
		entity.setStorage(storage);
		entity.setImageName(name);
		entity.setImageUrl(imageUrl);
		entity.setImageCatId(imageCatId);
		entity.setBrief("");
		String it = stringFromParams(mergedParams, "image_type");
		if (it != null) {
			entity.setImageType(it);
		}
		String ifu = stringFromParams(mergedParams, "image_full_url");
		if (ifu != null) {
			entity.setImageFullUrl(ifu);
		}
		entity.setSupplierId(supplierId);
		entity.setMerchantId(merchantId);
		entity.setDisabled(Boolean.FALSE);
		entity.setDistributorId(parseDistributorIdOrZero(mergedParams.get("distributor_id")));

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);

		uploadImagesMapper.insert(entity);

		String publicUrl = resolvePublicImageUrl(request, entity.getImageUrl());

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("image_id", entity.getImageId());
		out.put("company_id", entity.getCompanyId());
		out.put("storage", entity.getStorage());
		out.put("image_name", entity.getImageName());
		out.put("brief", entity.getBrief() == null ? "" : entity.getBrief());
		out.put("image_cat_id", entity.getImageCatId());
		String typeOut = entity.getImageType() != null ? entity.getImageType() : "item";
		out.put("image_type", typeOut);
		out.put("image_url", entity.getImageUrl());
		int disabledOut = entity.getDisabled() != null && entity.getDisabled() ? 1 : 0;
		out.put("disabled", disabledOut);
		out.put("distributor_id", entity.getDistributorId());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("supplier_id", entity.getSupplierId());
		out.put("merchant_id", entity.getMerchantId());
		out.put("image_full_url", publicUrl);
		out.put("url", publicUrl);
		return out;
	}

	public Map<String, Object> getImageList(
			HttpServletRequest request,
			long companyId,
			String operatorType,
			Long operatorId,
			long merchantIdFromJwt,
			Map<String, Object> queryMap) {
		String storageOpt = EspierAdminJwtControllerSupport.optionalTrimmedString(queryMap.get("storage"));
		String filterStorage =
				(storageOpt == null || storageOpt.isBlank()) ? "image" : storageOpt;

		long supplierId = 0L;
		long merchantId = 0L;
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("supplier".equals(ot)) {
			supplierId = operatorId == null ? 0L : operatorId;
		} else if ("merchant".equals(ot)) {
			merchantId = merchantIdFromJwt;
		}

		long distributorId = parseDistributorIdOrZero(queryMap.get("distributor_id"));

		boolean onlyDisabled = filterFieldLooksActive(queryMap.get("disabled"));

		LambdaQueryWrapper<UploadImages> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UploadImages::getCompanyId, companyId);
		wrapper.eq(UploadImages::getSupplierId, supplierId);
		wrapper.eq(UploadImages::getMerchantId, merchantId);
		wrapper.eq(UploadImages::getDistributorId, distributorId);
		wrapper.eq(UploadImages::getDisabled, onlyDisabled ? Boolean.TRUE : Boolean.FALSE);
		wrapper.eq(UploadImages::getStorage, filterStorage);

		if (filterFieldLooksActive(queryMap.get("image_name"))) {
			wrapper.eq(UploadImages::getImageName, String.valueOf(queryMap.get("image_name")));
		}

		applyImageCatIdEqIfApplicable(wrapper, queryMap);

		wrapper.orderByDesc(UploadImages::getCreated);

		long page = parsePageOrPageSizeWithDefault(queryMap, "page", 1L);
		long pageSize = parsePageOrPageSizeWithDefault(queryMap, "pageSize", 20L);

		long total = uploadImagesMapper.selectCount(wrapper);
		if (total < 0L || total > (long) Integer.MAX_VALUE) {
			throw new IllegalStateException("total count out of int range: " + total);
		}
		int totalCountInt = Math.toIntExact(total);

		List<Map<String, Object>> list;
		if (total == 0L) {
			list = Collections.emptyList();
		} else if (pageSize == 0L) {
			list = Collections.emptyList();
		} else {
			long offset = pageSize * (page - 1L);
			wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
			List<UploadImages> rows = uploadImagesMapper.selectList(wrapper);
			String diskFileType = "videos".equalsIgnoreCase(filterStorage) ? "videos" : "image";
			list = new ArrayList<>(rows.size());
			for (UploadImages entity : rows) {
				list.add(toImageListRowMap(request, diskFileType, entity));
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCountInt);
		out.put("list", list);
		return out;
	}

	public Map<String, Object> editImageCat(
			long companyId,
			String operatorType,
			Long operatorId,
			long merchantIdFromJwt,
			Map<String, Object> mergedParams) {
		Long requestParentIdOrNull = null;

		Object rawName = mergedParams.get("image_cat_name");
		String trimmedName = rawName == null ? "" : String.valueOf(rawName).trim();
		if (trimmedName.isEmpty()) {
			throw new BadRequestException("文件夹名称必填");
		}
		if (trimmedName.length() > 20) {
			throw new BadRequestException("文件夹名称不能超过20个字符");
		}

		if (mergedParams.containsKey("parent_id")) {
			Object raw = mergedParams.get("parent_id");
			if (raw == null) {
				requestParentIdOrNull = null;
			} else {
				String ts = String.valueOf(raw).trim();
				if (ts.isEmpty()) {
					throw new BadRequestException("父级分类格式不正确");
				}
				requestParentIdOrNull = parseFiniteLongNoFraction(ts, "父级分类格式不正确");
			}
		}

		Long validatedSortValue = null;
		if (mergedParams.containsKey("sort") && mergedParams.get("sort") != null) {
			validatedSortValue = parseStrictIntegerSort(mergedParams.get("sort"));
		}

		boolean updateMode = false;
		long imageCatIdForUpdate = 0L;
		Object rawId = mergedParams.get("image_cat_id");
		if (rawId != null) {
			String t = String.valueOf(rawId).trim();
			if (!t.isEmpty()) {
				try {
					long parsed = Long.parseLong(t);
					if (parsed != 0L) {
						updateMode = true;
						imageCatIdForUpdate = parsed;
					}
				} catch (NumberFormatException e) {
					throw new BadRequestException("分类ID格式不正确");
				}
			}
		}

		long supplierId = 0L;
		long merchantId = 0L;
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("supplier".equals(ot)) {
			supplierId = operatorId == null ? 0L : operatorId;
		} else if ("merchant".equals(ot)) {
			merchantId = merchantIdFromJwt;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (updateMode) {
			Long filterParentId;
			if (mergedParams.containsKey("parent_id")) {
				filterParentId = requestParentIdOrNull;
			} else {
				filterParentId = null;
			}
			LambdaQueryWrapper<UploadImagesCat> w = new LambdaQueryWrapper<>();
			w.eq(UploadImagesCat::getImageCatId, imageCatIdForUpdate)
					.eq(UploadImagesCat::getCompanyId, companyId);
			if (filterParentId == null) {
				w.isNull(UploadImagesCat::getParentId);
			} else {
				w.eq(UploadImagesCat::getParentId, filterParentId);
			}
			UploadImagesCat row = uploadImagesCatMapper.selectOne(w);
			if (row == null) {
				throw new ResourceException("分类Id为" + imageCatIdForUpdate + "不存在");
			}
			row.setImageCatName(trimmedName);
			if (mergedParams.containsKey("parent_id")) {
				row.setParentId(requestParentIdOrNull == null ? 0L : requestParentIdOrNull);
			}
			if (mergedParams.containsKey("sort")
					&& mergedParams.get("sort") != null
					&& isSortValueTruthy(mergedParams.get("sort"))) {
				row.setSort(validatedSortValue);
			}
			row.setUpdated(now);
			uploadImagesCatMapper.updateById(row);
			return toImagesCatDataMap(row);
		}

		boolean hasParent = requestParentIdOrNull != null && requestParentIdOrNull != 0L;
		String newPath;
		if (hasParent) {
			LambdaQueryWrapper<UploadImagesCat> pw = new LambdaQueryWrapper<>();
			pw.eq(UploadImagesCat::getImageCatId, requestParentIdOrNull)
					.eq(UploadImagesCat::getCompanyId, companyId);
			UploadImagesCat parentRow = uploadImagesCatMapper.selectOne(pw);
			if (parentRow == null) {
				throw new ResourceException("父分类id为" + requestParentIdOrNull + "不存在");
			}
			String basePath = parentRow.getPath() == null ? "," : parentRow.getPath();
			newPath = basePath + requestParentIdOrNull + ",";
		} else {
			newPath = ",";
		}

		UploadImagesCat entity = new UploadImagesCat();
		entity.setCompanyId(companyId);
		entity.setImageCatName(trimmedName);
		entity.setParentId(hasParent ? requestParentIdOrNull : 0L);
		entity.setPath(newPath);
		entity.setSupplierId(supplierId);
		entity.setMerchantId(merchantId);
		entity.setDistributorId(0L);
		if (mergedParams.containsKey("image_type")) {
			Object itRaw = mergedParams.get("image_type");
			if (itRaw != null) {
				String it = String.valueOf(itRaw).trim();
				if (!it.isEmpty()) {
					entity.setImageType(it);
				}
			}
		}
		if (mergedParams.containsKey("sort")
				&& mergedParams.get("sort") != null
				&& isSortValueTruthy(mergedParams.get("sort"))) {
			entity.setSort(validatedSortValue);
		} else {
			entity.setSort(0L);
		}
		entity.setCreated(now);
		entity.setUpdated(now);
		uploadImagesCatMapper.insert(entity);
		return toImagesCatDataMap(entity);
	}

	public Map<String, Object> getCatChildren(
			long companyId,
			String operatorType,
			Long operatorId,
			long merchantIdFromJwt,
			String imageCatIdRaw,
			String distributorIdRaw,
			String acceptLanguageHeader) {
		long supplierId = 0L;
		long merchantId = 0L;
		String ot = operatorType == null ? "" : operatorType.trim();
		if ("supplier".equals(ot)) {
			supplierId = operatorId == null ? 0L : operatorId;
		} else if ("merchant".equals(ot)) {
			merchantId = merchantIdFromJwt;
		}
		long distributorId = parseDistributorIdOrZero(distributorIdRaw);

		LambdaQueryWrapper<UploadImagesCat> base = new LambdaQueryWrapper<>();
		base.eq(UploadImagesCat::getCompanyId, companyId);
		applyParentIdFilter(base, imageCatIdRaw);
		base.eq(UploadImagesCat::getSupplierId, supplierId)
				.eq(UploadImagesCat::getMerchantId, merchantId)
				.eq(UploadImagesCat::getDistributorId, distributorId);

		long totalCount = uploadImagesCatMapper.selectCount(base);
		if (totalCount < 0L || totalCount > (long) Integer.MAX_VALUE) {
			throw new IllegalStateException("total count out of int range: " + totalCount);
		}
		int totalCountInt = Math.toIntExact(totalCount);

		List<Map<String, Object>> list = new ArrayList<>();
		if (totalCount > 0) {
			Page<UploadImagesCat> page = new Page<>(1, 100, false);
			base.orderByDesc(UploadImagesCat::getCreated);
			uploadImagesCatMapper.selectPage(page, base);
			for (UploadImagesCat e : page.getRecords()) {
				Map<String, Object> m = toImagesCatDataMap(e);
				m.put("parent_id", e.getParentId() == null ? 0L : e.getParentId());
				list.add(m);
			}
			commonLangModReadService.mergeEspierUploadImagesCatImageCatNameForList(
					companyId, list, acceptLanguageHeader);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCountInt);
		out.put("list", list);
		return out;
	}

	public Object getCatInfo(long companyId, String imageCatIdRaw, String acceptLanguageHeader) {
		String raw = imageCatIdRaw == null ? "" : imageCatIdRaw.trim();
		if (raw.isEmpty()) {
			return Collections.emptyList();
		}
		long imageCatId;
		try {
			BigDecimal bd = new BigDecimal(raw);
			if (bd.scale() > 0) {
				return Collections.emptyList();
			}
			imageCatId = bd.longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<UploadImagesCat> w = new LambdaQueryWrapper<>();
		w.eq(UploadImagesCat::getCompanyId, companyId).eq(UploadImagesCat::getImageCatId, imageCatId);
		UploadImagesCat entity = uploadImagesCatMapper.selectOne(w);
		if (entity == null) {
			return Collections.emptyList();
		}

		Map<String, Object> row = toImagesCatDataMap(entity);
		row.put("parent_id", entity.getParentId() == null ? 0L : entity.getParentId());

		List<Map<String, Object>> singleton = new ArrayList<>(1);
		singleton.add(row);
		commonLangModReadService.mergeEspierUploadImagesCatImageCatNameForList(
				companyId, singleton, acceptLanguageHeader);
		return row;
	}

	private static void applyParentIdFilter(LambdaQueryWrapper<UploadImagesCat> wrapper, String imageCatIdRaw) {
		if (imageCatIdRaw == null || imageCatIdRaw.isEmpty() || "0".equals(imageCatIdRaw)) {
			wrapper.eq(UploadImagesCat::getParentId, 0L);
			return;
		}
		String v = imageCatIdRaw;
		try {
			BigDecimal bd = new BigDecimal(v);
			wrapper.eq(UploadImagesCat::getParentId, bd.longValue());
		} catch (NumberFormatException e) {
			wrapper.apply("parent_id = {0}", v);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> delImgCat(long companyId, String imageCatIdRaw) {
		String raw = imageCatIdRaw == null ? "" : imageCatIdRaw;

		LambdaQueryWrapper<UploadImagesCat> childW = new LambdaQueryWrapper<>();
		childW.eq(UploadImagesCat::getCompanyId, companyId).apply("parent_id = {0}", raw);
		long childCount = uploadImagesCatMapper.selectCount(childW);
		if (childCount > 0L) {
			throw new ResourceException("请先删除子文件夹");
		}

		LambdaQueryWrapper<UploadImages> imgW = new LambdaQueryWrapper<>();
		imgW.eq(UploadImages::getCompanyId, companyId)
				.apply("image_cat_id = {0}", raw)
				.eq(UploadImages::getDisabled, Boolean.FALSE);
		long imagesCount = uploadImagesMapper.selectCount(imgW);
		if (imagesCount > 0L) {
			throw new ResourceException("该文件夹下存在图片，不能删除");
		}

		LambdaQueryWrapper<UploadImagesCat> selfW = new LambdaQueryWrapper<>();
		selfW.eq(UploadImagesCat::getCompanyId, companyId).apply("image_cat_id = {0}", raw);
		UploadImagesCat existing = uploadImagesCatMapper.selectOne(selfW);
		if (existing == null) {
			throw new ResourceException("删除的数据不存在");
		}

		String table = "espier_uploadimages_cat";
		String module = "espier_uploadimages_cat";
		List<String> langs = langueProperties.getList();
		long dataIdForLang = existing.getImageCatId() == null ? 0L : existing.getImageCatId();
		if (langs != null) {
			for (String lang : langs) {
				if (StringUtils.hasText(lang)) {
					commonLangModWriteService.deleteLang(
							(int) companyId, table, dataIdForLang, module, lang.trim());
				}
			}
		}

		int removed = uploadImagesCatMapper.delete(selfW);
		if (removed != 1) {
			throw new ResourceException("删除的数据不存在");
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteImage(long companyId, String imageIdRaw) {
		if (imageIdRaw == null) {
			throw new BadRequestException("请选择要删除的图片");
		}
		if (imageIdRaw.isEmpty()) {
			throw new BadRequestException("请选择要删除的图片");
		}
		if ("0".equals(imageIdRaw)) {
			throw new BadRequestException("请选择要删除的图片");
		}

		String[] parts = imageIdRaw.split(",", -1);
		if (parts.length > 100) {
			throw new BadRequestException("单次最多删除100个图片");
		}

		List<String> tokens = Arrays.asList(parts);

		LambdaUpdateWrapper<UploadImages> uw = new LambdaUpdateWrapper<>();
		uw.eq(UploadImages::getCompanyId, companyId)
				.in(UploadImages::getImageId, tokens)
				.set(UploadImages::getDisabled, Boolean.TRUE);

		int rows = uploadImagesMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public void moveImageCat(long companyId, Map<String, Object> mergedParams) {
		List<String> errors = new ArrayList<>();
		Object rawCat = mergedParams.get("image_cat_id");
		boolean catMissing =
				!mergedParams.containsKey("image_cat_id")
						|| rawCat == null
						|| String.valueOf(rawCat).trim().isEmpty();
		long targetImageCatId = 0L;
		if (catMissing) {
			errors.add("图片分类必填");
		} else {
			String catRaw = String.valueOf(rawCat).trim();
			try {
				BigDecimal bd = new BigDecimal(catRaw);
				if (bd.scale() > 0) {
					errors.add("图片分类ID格式不正确");
				} else {
					targetImageCatId = bd.longValueExact();
				}
			} catch (NumberFormatException | ArithmeticException e) {
				errors.add("图片分类ID格式不正确");
			}
		}

		Object rawImageId = mergedParams.get("image_id");
		boolean imageIdMissing =
				!mergedParams.containsKey("image_id")
						|| rawImageId == null
						|| String.valueOf(rawImageId).trim().isEmpty();
		if (imageIdMissing) {
			errors.add("图片id必填");
		}

		if (!errors.isEmpty()) {
			throw new BadRequestException(String.join("，", errors));
		}

		boolean requiresCategoryRow = targetImageCatId != 0L;
		if (requiresCategoryRow) {
			LambdaQueryWrapper<UploadImagesCat> cw = new LambdaQueryWrapper<>();
			cw.eq(UploadImagesCat::getCompanyId, companyId)
					.eq(UploadImagesCat::getImageCatId, targetImageCatId);
			UploadImagesCat cat = uploadImagesCatMapper.selectOne(cw);
			if (cat == null) {
				throw new ResourceException("被移动到的图片类型分类不存在");
			}
		}

		String rawIds = String.valueOf(mergedParams.get("image_id")).trim();
		String[] parts = rawIds.split(",");
		List<Long> imageIds = new ArrayList<>();
		for (String part : parts) {
			String seg = part.trim();
			if (seg.isEmpty()) {
				continue;
			}
			try {
				BigDecimal bd = new BigDecimal(seg);
				if (bd.scale() > 0) {
					throw new BadRequestException("图片ID格式不正确");
				}
				imageIds.add(bd.longValueExact());
			} catch (NumberFormatException | ArithmeticException e) {
				throw new BadRequestException("图片ID格式不正确");
			}
		}
		if (imageIds.isEmpty()) {
			throw new BadRequestException("图片id必填");
		}

		LambdaQueryWrapper<UploadImages> base = new LambdaQueryWrapper<>();
		base.eq(UploadImages::getCompanyId, companyId)
				.in(UploadImages::getImageId, imageIds)
				.eq(UploadImages::getDisabled, Boolean.FALSE);

		long totalCount = uploadImagesMapper.selectCount(base);
		if (totalCount <= 0) {
			throw new ResourceException("被移动图片不存在");
		}

		Page<UploadImages> page = new Page<>(1, 100, false);
		uploadImagesMapper.selectPage(page, base);
		List<UploadImages> list = page.getRecords();

		List<Long> updateImageIds = new ArrayList<>();
		for (UploadImages item : list) {
			long currentCat = item.getImageCatId() == null ? 0L : item.getImageCatId();
			if (currentCat != targetImageCatId) {
				updateImageIds.add(item.getImageId());
			}
		}
		if (updateImageIds.isEmpty()) {
			return;
		}

		LambdaQueryWrapper<UploadImages> uw = new LambdaQueryWrapper<>();
		uw.eq(UploadImages::getCompanyId, companyId)
				.in(UploadImages::getImageId, updateImageIds)
				.eq(UploadImages::getDisabled, Boolean.FALSE);
		List<UploadImages> rows = uploadImagesMapper.selectList(uw);
		if (rows.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		for (UploadImages row : rows) {
			row.setImageCatId(targetImageCatId);
			row.setUpdated(now);
			uploadImagesMapper.updateById(row);
		}
	}

	private static Map<String, Object> toImagesCatDataMap(UploadImagesCat e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("image_cat_id", e.getImageCatId());
		m.put("company_id", e.getCompanyId());
		m.put("image_cat_name", e.getImageCatName());
		m.put("parent_id", e.getParentId());
		m.put("image_type", e.getImageType());
		m.put("path", e.getPath());
		m.put("sort", e.getSort());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("supplier_id", e.getSupplierId());
		m.put("distributor_id", e.getDistributorId());
		m.put("merchant_id", e.getMerchantId());
		return m;
	}

	private static long parseFiniteLongNoFraction(String trimmed, String errorMsg) {
		try {
			BigDecimal bd = new BigDecimal(trimmed);
			if (bd.scale() > 0) {
				throw new BadRequestException(errorMsg);
			}
			return bd.longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException(errorMsg);
		}
	}

	private static Long parseStrictIntegerSort(Object raw) {
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			throw new BadRequestException("排序必须是整数");
		}
		try {
			BigDecimal bd = new BigDecimal(s);
			if (bd.scale() > 0) {
				throw new BadRequestException("排序必须是整数");
			}
			return bd.longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException("排序必须是整数");
		}
	}

	private static boolean isSortValueTruthy(Object v) {
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) != 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static long parseDistributorIdOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		String td = String.valueOf(raw).trim();
		if (td.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(td);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean shouldApplyImageCatIdFilter(boolean imageCatIdKeyPresent, Object raw) {
		if (!imageCatIdKeyPresent) {
			return false;
		}
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static boolean filterFieldLooksActive(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static long parsePageOrPageSizeWithDefault(Map<String, Object> queryMap, String key, long defaultVal) {
		if (!queryMap.containsKey(key)) {
			return defaultVal;
		}
		Object raw = queryMap.get(key);
		if (raw == null) {
			return defaultVal;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return defaultVal;
		}
		try {
			BigDecimal bd = new BigDecimal(s);
			if (bd.stripTrailingZeros().scale() > 0) {
				return defaultVal;
			}
			return bd.longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			return defaultVal;
		}
	}

	private static void applyImageCatIdEqIfApplicable(LambdaQueryWrapper<UploadImages> wrapper, Map<String, Object> queryMap) {
		boolean present = queryMap.containsKey("image_cat_id");
		Object raw = queryMap.get("image_cat_id");
		if (!shouldApplyImageCatIdFilter(present, raw)) {
			return;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if ("0".equals(t)) {
				wrapper.eq(UploadImages::getImageCatId, 0L);
				return;
			}
			try {
				BigDecimal bd = new BigDecimal(t);
				if (bd.stripTrailingZeros().scale() > 0) {
					return;
				}
				wrapper.eq(UploadImages::getImageCatId, bd.longValueExact());
			} catch (NumberFormatException | ArithmeticException e) {
				return;
			}
			return;
		}
		if (raw instanceof Number) {
			try {
				BigDecimal bd = new BigDecimal(String.valueOf(raw));
				if (bd.stripTrailingZeros().scale() > 0) {
					return;
				}
				wrapper.eq(UploadImages::getImageCatId, bd.longValueExact());
			} catch (NumberFormatException | ArithmeticException e) {
				// omit condition
			}
		}
	}

	private Map<String, Object> toImageListRowMap(HttpServletRequest request, String diskFileType, UploadImages entity) {
		String u = resolvePublicListItemUrl(request, diskFileType, entity.getImageUrl());
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("image_id", entity.getImageId());
		out.put("company_id", entity.getCompanyId());
		out.put("storage", entity.getStorage());
		out.put("image_name", entity.getImageName());
		out.put("brief", entity.getBrief());
		out.put("image_cat_id", entity.getImageCatId() == null ? 0L : entity.getImageCatId());
		String typeOut = entity.getImageType() != null ? entity.getImageType() : "item";
		out.put("image_type", typeOut);
		out.put("image_url", entity.getImageUrl());
		int disabledOut = entity.getDisabled() != null && entity.getDisabled() ? 1 : 0;
		out.put("disabled", disabledOut);
		out.put("distributor_id", entity.getDistributorId());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("supplier_id", entity.getSupplierId());
		out.put("merchant_id", entity.getMerchantId());
		out.put("image_full_url", u);
		out.put("url", u);
		return out;
	}

	private String resolvePublicListItemUrl(HttpServletRequest request, String diskFileType, String imageUrlKey) {
		if (request == null || !"local".equalsIgnoreCase(storageProperties.getDriver())) {
			return fileStorageService.url(diskFileType, imageUrlKey);
		}
		return LocalStoragePublicUrl.resolve(request, storageProperties, imageUrlKey);
	}

	private static String stringFromParams(Map<String, Object> m, String key) {
		Object o = m.get(key);
		if (o == null) {
			return null;
		}
		String s = String.valueOf(o).trim();
		return s.isEmpty() ? null : s;
	}

	/**
	 * Returns the publicly reachable URL for the stored image key.
	 * <p>When {@link StorageProperties#getDriver()} is {@code local}, builds an absolute URL via
	 * {@link LocalStoragePublicUrl} (request Host with explicit port, else configured local.url).
	 * Otherwise returns {@link FileStorageService#url} for the {@code image} disk.
	 */
	private String resolvePublicImageUrl(HttpServletRequest request, String imageUrl) {
		if (request == null || !"local".equalsIgnoreCase(storageProperties.getDriver())) {
			return fileStorageService.url("image", imageUrl);
		}
		return LocalStoragePublicUrl.resolve(request, storageProperties, imageUrl);
	}
}
