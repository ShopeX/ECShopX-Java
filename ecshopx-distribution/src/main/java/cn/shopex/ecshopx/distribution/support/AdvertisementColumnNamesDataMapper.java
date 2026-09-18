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

package cn.shopex.ecshopx.distribution.support;

import cn.shopex.ecshopx.distribution.domain.Advertisement;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdvertisementColumnNamesDataMapper {

	private AdvertisementColumnNamesDataMapper() {}

	public static Map<String, Object> toColumnNamesData(Advertisement row) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", row.getId());
		map.put("title", row.getTitle());
		map.put("company_id", row.getCompanyId());
		map.put("thumb_img", row.getThumbImg());
		map.put("media_url", row.getMediaUrl());
		map.put("release_time", row.getReleaseTime());
		map.put("release_status", Boolean.TRUE.equals(row.getReleaseStatus()));
		map.put("created", row.getCreated());
		map.put("sort", row.getSort());
		map.put("distributor_id", row.getDistributorId());
		map.put("media_type", row.getMediaType());
		return map;
	}
}
