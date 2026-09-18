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

package cn.shopex.ecshopx.wechat.repository;

import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WeappAuthorizerAppidRepository {

	private final WeappMapper weappMapper;

	public WeappAuthorizerAppidRepository(WeappMapper weappMapper) {
		this.weappMapper = weappMapper;
	}

	public Optional<String> findAuthorizerAppid(long companyId, String templateName) {
		Weapp row = weappMapper.selectOne(
				new LambdaQueryWrapper<Weapp>()
						.eq(Weapp::getCompanyId, companyId)
						.eq(Weapp::getTemplateName, templateName)
						.isNull(Weapp::getDeletedAt)
						.last("LIMIT 1"));
		if (row == null || row.getAuthorizerAppid() == null || row.getAuthorizerAppid().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(row.getAuthorizerAppid());
	}
}
