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
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetSliderService {

	private final SliderMapper sliderMapper;

	private final ObjectMapper objectMapper;

	public DistributorH5GetSliderService(SliderMapper sliderMapper, ObjectMapper objectMapper) {
		this.sliderMapper = sliderMapper;
		this.objectMapper = objectMapper;
	}

	public Object getSlider(long companyId, long distributorId) {
		Slider first =
				sliderMapper.selectOne(
						new LambdaQueryWrapper<Slider>()
								.eq(Slider::getCompanyId, companyId)
								.eq(Slider::getDistributorId, distributorId));
		boolean missing = first == null;
		if (!missing) {
			return SliderColumnNamesDataMapper.toColumnNamesData(first, objectMapper);
		}
		Slider fallback =
				sliderMapper.selectOne(
						new LambdaQueryWrapper<Slider>()
								.eq(Slider::getCompanyId, companyId)
								.eq(Slider::getDistributorId, 0L));
		if (fallback != null) {
			return SliderColumnNamesDataMapper.toColumnNamesData(fallback, objectMapper);
		}
		return Collections.emptyList();
	}
}
