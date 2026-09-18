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

package cn.shopex.ecshopx.espier.openapi;

import cn.shopex.ecshopx.espier.domain.UploadImages;
import cn.shopex.ecshopx.espier.mapper.UploadImagesMapper;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.espier.storage.LocalStoragePublicUrl;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1ImageListService {

	private final UploadImagesMapper uploadImagesMapper;
	private final FileStorageService fileStorageService;
	private final StorageProperties storageProperties;

	public OpenapiThirdApiV1ImageListService(
			UploadImagesMapper uploadImagesMapper,
			FileStorageService fileStorageService,
			StorageProperties storageProperties) {
		this.uploadImagesMapper = uploadImagesMapper;
		this.fileStorageService = fileStorageService;
		this.storageProperties = storageProperties;
	}

	public Map<String, Object> executeOpenapiImageList(
			HttpServletRequest request,
			long companyId,
			Map<String, Object> queryMap,
			boolean disabledFilterActive,
			Object disabledFilterValue,
			boolean imageNameFilterActive,
			String imageNameFilterValue,
			boolean imageCatIdFilterActive,
			Object imageCatIdFilterValue) {
		String filterStorage = resolveStorageDefault(queryMap.get("storage"));
		long distributorId = parseDistributorIdOrZero(queryMap.get("distributor_id"));

		LambdaQueryWrapper<UploadImages> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UploadImages::getCompanyId, companyId);
		wrapper.eq(UploadImages::getStorage, filterStorage);
		wrapper.eq(UploadImages::getDistributorId, distributorId);
		wrapper.eq(UploadImages::getDisabled, disabledFilterActive ? Boolean.TRUE : Boolean.FALSE);

		if (imageNameFilterActive) {
			wrapper.like(UploadImages::getImageName, imageNameFilterValue);
		}

		if (imageCatIdFilterActive) {
			applyImageCatIdEq(wrapper, imageCatIdFilterValue);
		}

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
			list = mapOpenApiRows(request, diskFileType, rows);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCountInt);
		out.put("list", list);
		return out;
	}

	private List<Map<String, Object>> mapOpenApiRows(
			HttpServletRequest request, String diskFileType, List<UploadImages> rows) {
		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (UploadImages entity : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("image_id", entity.getImageId());
			row.put("image_name", entity.getImageName());
			row.put("image_type", entity.getImageType());
			row.put(
					"image_url",
					resolvePublicListItemUrl(request, diskFileType, entity.getImageUrl()));
			row.put("create_time", entity.getCreated());
			list.add(row);
		}
		return list;
	}

	private static String resolveStorageDefault(Object raw) {
		if (raw == null) {
			return "image";
		}
		if (raw instanceof Number n && n.longValue() == 0L) {
			return "image";
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return "image";
		}
		return s;
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

	private static void applyImageCatIdEq(LambdaQueryWrapper<UploadImages> wrapper, Object raw) {
		if (raw == null) {
			return;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
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

	private String resolvePublicListItemUrl(HttpServletRequest request, String diskFileType, String imageUrlKey) {
		if (request == null || !"local".equalsIgnoreCase(storageProperties.getDriver())) {
			return fileStorageService.url(diskFileType, imageUrlKey);
		}
		return LocalStoragePublicUrl.resolve(request, storageProperties, imageUrlKey);
	}
}
