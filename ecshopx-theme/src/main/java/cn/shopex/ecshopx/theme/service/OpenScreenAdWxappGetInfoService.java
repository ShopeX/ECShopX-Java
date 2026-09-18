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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.theme.domain.OpenScreenAd;
import cn.shopex.ecshopx.theme.mapper.OpenScreenAdMapper;
import cn.shopex.ecshopx.theme.support.OpenScreenAdColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OpenScreenAdWxappGetInfoService {

	private static final Logger log = LoggerFactory.getLogger(OpenScreenAdWxappGetInfoService.class);

	private final OpenScreenAdMapper openScreenAdMapper;
	private final OpenScreenAdColumnNamesDataMapper openScreenAdColumnNamesDataMapper;
	private final ObjectMapper objectMapper;

	public Object getInfo(long companyId) {
		long nowSec = Instant.now().getEpochSecond();
		var wrapper = new LambdaQueryWrapper<OpenScreenAd>()
				.eq(OpenScreenAd::getCompanyId, companyId)
				.eq(OpenScreenAd::getIsEnable, 1)
				.le(OpenScreenAd::getStartTime, nowSec)
				.ge(OpenScreenAd::getEndTime, nowSec);

		long totalCount = openScreenAdMapper.selectCount(wrapper);
		if (totalCount == 0) {
			return Collections.emptyList();
		}

		wrapper.orderByDesc(OpenScreenAd::getId).last("LIMIT 1");
		OpenScreenAd row = openScreenAdMapper.selectOne(wrapper);
		if (row == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> data =
				new LinkedHashMap<>(openScreenAdColumnNamesDataMapper.toColumnNamesData(row));

		String raw = row.getAdUrl();
		if (raw == null || !StringUtils.hasText(raw)) {
			data.put("ad_url", null);
		} else {
			try {
				data.put("ad_url", objectMapper.readValue(raw, Object.class));
			} catch (JsonProcessingException e) {
				log.warn("Invalid ad_url JSON for companyId={}", companyId, e);
				data.put("ad_url", null);
			}
		}

		return data;
	}
}
