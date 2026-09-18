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

package cn.shopex.ecshopx.openapi.integration;

import cn.shopex.ecshopx.common.openapi.OpenapiDeveloperCredentials;
import cn.shopex.ecshopx.common.openapi.OpenapiDeveloperLookupPort;
import cn.shopex.ecshopx.openapi.domain.OpenapiDeveloper;
import cn.shopex.ecshopx.openapi.mapper.OpenapiDeveloperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiDeveloperLookupPortImpl implements OpenapiDeveloperLookupPort {

	private final OpenapiDeveloperMapper openapiDeveloperMapper;

	public OpenapiDeveloperLookupPortImpl(OpenapiDeveloperMapper openapiDeveloperMapper) {
		this.openapiDeveloperMapper = openapiDeveloperMapper;
	}

	@Override
	public Optional<OpenapiDeveloperCredentials> findByAppKey(String appKey) {
		if (!StringUtils.hasText(appKey)) {
			return Optional.empty();
		}
		OpenapiDeveloper developer =
				openapiDeveloperMapper.selectOne(
						new LambdaQueryWrapper<OpenapiDeveloper>()
								.eq(OpenapiDeveloper::getAppKey, appKey.trim())
								.last("LIMIT 1"));
		if (developer == null || developer.getCompanyId() == null || !StringUtils.hasText(developer.getAppSecret())) {
			return Optional.empty();
		}
		return Optional.of(
				new OpenapiDeveloperCredentials(developer.getCompanyId(), developer.getAppSecret()));
	}
}
