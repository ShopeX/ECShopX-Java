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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Advertisement;
import cn.shopex.ecshopx.distribution.mapper.AdvertisementMapper;
import cn.shopex.ecshopx.distribution.support.AdvertisementColumnNamesDataMapper;
import cn.shopex.ecshopx.distribution.support.AdvertisementReleaseStatusSupport;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class ShopScreenAdvertisementCreateService {

	private final AdvertisementMapper advertisementMapper;

	private final MessageSource messageSource;

	public ShopScreenAdvertisementCreateService(
			AdvertisementMapper advertisementMapper, MessageSource messageSource) {
		this.advertisementMapper = advertisementMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> addAdvertisement(long companyId, Map<String, Object> merged) {
		Locale locale = LocaleContextHolder.getLocale();
		boolean anyTruthy = false;
		for (Object v : merged.values()) {
			if (valueLooksPresent(v)) {
				anyTruthy = true;
				break;
			}
		}
		if (!anyTruthy) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.ads_params_error", null, locale));
		}

		Object titleRaw = merged.get("title");
		if (titleRaw == null) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.ads_required", null, locale));
		}
		if (titleRaw instanceof Number n && n.longValue() == 0L) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.ads_required", null, locale));
		}
		String titleStr = String.valueOf(titleRaw).trim();
		if (!StringUtils.hasText(titleStr) || "0".equals(titleStr)) {
			throw new BadRequestException(
					messageSource.getMessage("distribution.shop_screen.ads_required", null, locale));
		}

		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		if (requiredStringInvalid(merged.get("title"))) {
			fieldErrors.put(
					"title",
					List.of(
							messageSource.getMessage(
									"distribution.shop_screen.validation.title_required", null, locale)));
		}
		if (requiredStringInvalid(merged.get("media_url"))) {
			fieldErrors.put(
					"media_url",
					List.of(
							messageSource.getMessage(
									"distribution.shop_screen.validation.media_url_required", null, locale)));
		}
		if (requiredStringInvalid(merged.get("thumb_img"))) {
			fieldErrors.put(
					"thumb_img",
					List.of(
							messageSource.getMessage(
									"distribution.shop_screen.validation.thumb_img_required", null, locale)));
		}
		Object mediaTypeRaw = merged.get("media_type");
		String mediaType =
				mediaTypeRaw == null ? "" : String.valueOf(mediaTypeRaw).trim();
		if (!"image".equals(mediaType) && !"video".equals(mediaType)) {
			fieldErrors.put(
					"media_type",
					List.of(
							messageSource.getMessage(
									"distribution.shop_screen.validation.media_type_in", null, locale)));
		}
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage("distribution.shop_screen.params_error", null, locale),
					fieldErrors);
		}

		long distributorId = parseLongDefaultAllowMissing(merged.get("distributor_id"), 0L);

		Advertisement entity = new Advertisement();
		entity.setCompanyId(companyId);
		entity.setTitle(titleStr);
		entity.setMediaUrl(String.valueOf(merged.get("media_url")).trim());
		entity.setMediaType(mediaType);
		entity.setThumbImg(String.valueOf(merged.get("thumb_img")).trim());
		entity.setDistributorId(distributorId);
		entity.setCreated((int) (System.currentTimeMillis() / 1000L));

		if (merged.containsKey("release_status")) {
			Object rs = merged.get("release_status");
			if (AdvertisementReleaseStatusSupport.releaseStatusInputFalsy(rs)) {
				entity.setReleaseStatus(false);
				entity.setReleaseTime(0);
			} else {
				entity.setReleaseStatus(true);
				entity.setReleaseTime((int) (System.currentTimeMillis() / 1000L));
			}
		}

		try {
			advertisementMapper.insert(entity);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("保存失败");
		}

		return AdvertisementColumnNamesDataMapper.toColumnNamesData(entity);
	}

	private static boolean valueLooksPresent(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			return StringUtils.hasText(t) && !"0".equals(t);
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		return true;
	}

	private static boolean requiredStringInvalid(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		String s = String.valueOf(raw).trim();
		return !StringUtils.hasText(s) || "0".equals(s);
	}

	private long parseLongDefaultAllowMissing(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("distributor_id 格式错误");
		}
	}
}
