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

import cn.shopex.ecshopx.distribution.domain.Slider;
import cn.shopex.ecshopx.distribution.mapper.SliderMapper;
import cn.shopex.ecshopx.distribution.support.SliderColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShopScreenSliderGetService {

	private final SliderMapper sliderMapper;

	private final ObjectMapper objectMapper;

	public ShopScreenSliderGetService(SliderMapper sliderMapper, ObjectMapper objectMapper) {
		this.sliderMapper = sliderMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSlider(long companyId, long distributorId) {
		Slider entity =
				sliderMapper.selectOne(
						new LambdaQueryWrapper<Slider>()
								.eq(Slider::getCompanyId, companyId)
								.eq(Slider::getDistributorId, distributorId));
		if (entity == null) {
			return buildDefaultSliderPayload(companyId, distributorId);
		}
		return SliderColumnNamesDataMapper.toColumnNamesData(entity, objectMapper);
	}

	private static Map<String, Object> buildDefaultSliderPayload(long companyId, long distributorId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("company_id", companyId);
		map.put("distributor_id", distributorId);
		map.put("desc_status", "true");
		map.put("image_list", Collections.emptyList());
		Map<String, Object> styleParams = new LinkedHashMap<>();
		styleParams.put("content", "true");
		styleParams.put("current", "false");
		styleParams.put("dot", "true");
		styleParams.put("dotColor", "dark");
		styleParams.put("dotCover", "false");
		styleParams.put("dotLocation", "center");
		styleParams.put("interval", "false");
		styleParams.put("numNavShape", "false");
		styleParams.put("padded", "false");
		styleParams.put("rounded", "false");
		styleParams.put("shape", "circle");
		styleParams.put("spacing", "false");
		map.put("style_params", styleParams);
		map.put("sub_title", "");
		map.put("title", "");
		return map;
	}
}
