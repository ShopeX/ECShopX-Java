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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Advertisement;
import cn.shopex.ecshopx.distribution.mapper.AdvertisementMapper;
import cn.shopex.ecshopx.distribution.support.AdvertisementReleaseStatusSupport;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopScreenAdvertisementUpdateService {

	private final AdvertisementMapper advertisementMapper;

	private final MessageSource messageSource;

	public ShopScreenAdvertisementUpdateService(
			AdvertisementMapper advertisementMapper, MessageSource messageSource) {
		this.advertisementMapper = advertisementMapper;
		this.messageSource = messageSource;
	}

	public void updateAdvertisement(long companyId, List<Map<String, Object>> items) {
		Locale locale = LocaleContextHolder.getLocale();
		for (Map<String, Object> value : items) {
			if (value.get("id") == null) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}
			long id = parseItemId(value.get("id"), locale);

			Advertisement row = advertisementMapper.selectById(id);
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage("distribution.repositories.no_update_data_found", null, locale));
			}

			Long distributorIdRaw = row.getDistributorId();
			long distributorId = distributorIdRaw == null ? 0L : distributorIdRaw;

			boolean releaseStatusIsset = value.get("release_status") != null;
			boolean sortIsset = value.get("sort") != null;

			boolean updateReleaseColumns = false;
			boolean releaseTrue = false;
			Integer releaseTimeToSet = null;
			boolean updateSortColumn = false;
			Integer sortToSet = null;

			if (releaseStatusIsset) {
				boolean falsy = AdvertisementReleaseStatusSupport.releaseStatusInputFalsy(value.get("release_status"));
				releaseTrue = !falsy;
				updateReleaseColumns = true;
				if (releaseTrue) {
					long total =
							advertisementMapper.selectCount(
									Wrappers.<Advertisement>lambdaQuery()
											.eq(Advertisement::getCompanyId, companyId)
											.eq(Advertisement::getDistributorId, distributorId)
											.eq(Advertisement::getReleaseStatus, Boolean.TRUE));
					if (total >= 3L) {
						throw new ResourceException(
								messageSource.getMessage(
										"distribution.services.advertisement.published_ads_limit", null, locale));
					}
				}
				releaseTimeToSet = falsy ? Integer.valueOf(0) : Integer.valueOf((int) (System.currentTimeMillis() / 1000L));
			}

			if (sortIsset) {
				updateSortColumn = true;
				sortToSet = parseSort(value.get("sort"), locale);
			}

			if (!releaseStatusIsset && !sortIsset) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}

			LambdaUpdateWrapper<Advertisement> uw = Wrappers.lambdaUpdate();
			uw.eq(Advertisement::getId, id);
			if (updateReleaseColumns) {
				uw.set(Advertisement::getReleaseStatus, releaseTrue).set(Advertisement::getReleaseTime, releaseTimeToSet);
			}
			if (updateSortColumn) {
				uw.set(Advertisement::getSort, sortToSet);
			}
			int updated = advertisementMapper.update(null, uw);
			if (updated != 1) {
				throw new ResourceException(
						messageSource.getMessage("distribution.repositories.no_update_data_found", null, locale));
			}
		}
	}

	private long parseItemId(Object raw, Locale locale) {
		try {
			if (raw instanceof Number n) {
				long id = n.longValue();
				if (id <= 0L) {
					throw new ResourceException(
							messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
				}
				return id;
			}
			if (raw instanceof String s) {
				long id = Long.parseLong(s.trim());
				if (id <= 0L) {
					throw new ResourceException(
							messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
				}
				return id;
			}
			long id = Long.parseLong(String.valueOf(raw).trim());
			if (id <= 0L) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}
			return id;
		} catch (NumberFormatException ex) {
			throw new ResourceException(
					messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
		}
	}

	private Integer parseSort(Object raw, Locale locale) {
		if (raw == null) {
			throw new ResourceException(
					messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
		}
		if (raw instanceof Number n) {
			long lv = n.longValue();
			if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}
			return (int) lv;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException ex) {
				throw new ResourceException(
						messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
			}
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			throw new ResourceException(
					messageSource.getMessage("distribution.services.advertisement.params_error", null, locale));
		}
	}
}
