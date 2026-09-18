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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappAllListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorWxappAllListRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorH5GetAllDistributorListService {

	private final DistributorWxappAllListRepository distributorWxappAllListRepository;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public DistributorH5GetAllDistributorListService(
			DistributorWxappAllListRepository distributorWxappAllListRepository,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.distributorWxappAllListRepository = distributorWxappAllListRepository;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getAllDistributorList(
			long companyId,
			String lngRaw,
			String latRaw,
			String nameRaw,
			String provinceRaw,
			String cityRaw,
			int page,
			int pageSize,
			String requestLangTag) {
		int p = Math.max(1, page);
		int ps = pageSize <= 0 ? 10 : pageSize;

		DistributorWxappAllListFilter f = new DistributorWxappAllListFilter();
		f.setCompanyId(companyId);
		f.setNameLikeEscaped(escapedLikeOrNull(nameRaw));
		f.setProvinceLikeEscaped(escapedLikeOrNull(provinceRaw));
		f.setCityLikeEscaped(escapedLikeOrNull(cityRaw));

		boolean geoEnabled = false;
		Double userLng = null;
		Double userLat = null;
		if (lngRaw != null && latRaw != null) {
			String tl = lngRaw.trim();
			String ta = latRaw.trim();
			if (StringUtils.hasText(tl)
					&& StringUtils.hasText(ta)
					&& !"0".equals(tl)
					&& !"0".equals(ta)) {
				try {
					userLng = Double.parseDouble(tl);
					userLat = Double.parseDouble(ta);
					geoEnabled = true;
				} catch (NumberFormatException ignored) {
					geoEnabled = false;
				}
			}
		}
		f.setGeoEnabled(geoEnabled);
		f.setUserLng(userLng);
		f.setUserLat(userLat);
		f.setOffset((long) (p - 1) * ps);
		f.setLimit(ps);

		long total = distributorWxappAllListRepository.countByFilter(f);
		List<Distributor> entities = distributorWxappAllListRepository.selectPageByFilter(f);

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Distributor d : entities) {
			if (d.getMobile() != null) {
				d.setMobile(sensitiveFieldEncryptor.decrypt(d.getMobile()));
			}
			if (d.getContact() != null) {
				d.setContact(sensitiveFieldEncryptor.decrypt(d.getContact()));
			}
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf().intValue();
			long rowDistributorId = d.getDistributorId() == null ? 0L : d.getDistributorId();
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, rowDistributorId, distributorSelf);
			Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			if (geoEnabled && d.getDistance() != null) {
				BigDecimal km = d.getDistance();
				row.put("distance", km.stripTrailingZeros().toPlainString());
				if (km.compareTo(BigDecimal.ONE) < 0) {
					row.put(
							"distance_show",
							km.multiply(BigDecimal.valueOf(1000))
									.setScale(0, RoundingMode.HALF_UP)
									.toPlainString());
					row.put("distance_unit", "m");
				} else {
					row.put(
							"distance_show",
							km.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
					row.put("distance_unit", "km");
				}
			}
			listMaps.add(row);
		}

		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, listMaps);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", (int) Math.min(total, Integer.MAX_VALUE));
		body.put("list", listMaps);
		return body;
	}

	private static String escapedLikeOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		return DistributorListQueryService.escapeSqlLike(t);
	}
}
