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

package cn.shopex.ecshopx.goods.service.recommend;

import cn.shopex.ecshopx.goods.domain.recommend.GoodsRecommendDisplaySetting;
import cn.shopex.ecshopx.goods.mapper.recommend.GoodsRecommendDisplaySettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsRecommendDisplaySettingService {

	private final GoodsRecommendDisplaySettingMapper mapper;

	private final MessageSource messageSource;

	public GoodsRecommendDisplaySettingService(
			GoodsRecommendDisplaySettingMapper mapper, MessageSource messageSource) {
		this.mapper = mapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getDisplaySetting(long companyId) {
		GoodsRecommendDisplaySetting row = findByCompanyId(companyId);
		if (row == null) {
			return GoodsRecommendDisplayDefaults.defaultResponseMap(companyId);
		}
		return GoodsRecommendDisplayDefaults.toResponseMap(row, true);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveDisplaySetting(long companyId, Map<String, Object> input) {
		GoodsRecommendDisplaySettingValidator.validate(input, messageSource);
		int now = (int) Instant.now().getEpochSecond();
		GoodsRecommendDisplaySetting row = findByCompanyId(companyId);
		if (row == null) {
			row = GoodsRecommendDisplayDefaults.newEntityDefaults(companyId, now);
		}
		GoodsRecommendDisplaySettingValidator.applyInput(row, input);
		row.setUpdated(now);
		if (row.getId() == null) {
			row.setCreated(now);
			mapper.insert(row);
		} else {
			mapper.updateById(row);
		}
		return GoodsRecommendDisplayDefaults.toResponseMap(row, true);
	}

	private GoodsRecommendDisplaySetting findByCompanyId(long companyId) {
		return mapper.selectOne(
				new LambdaQueryWrapper<GoodsRecommendDisplaySetting>()
						.eq(GoodsRecommendDisplaySetting::getCompanyId, companyId)
						.last("LIMIT 1"));
	}
}
