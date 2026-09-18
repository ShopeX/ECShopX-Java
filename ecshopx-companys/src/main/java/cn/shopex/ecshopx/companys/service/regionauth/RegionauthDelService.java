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

package cn.shopex.ecshopx.companys.service.regionauth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class RegionauthDelService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthDelService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public void del(long companyId, String regionauthIdPath) {
		int now = (int) Instant.now().getEpochSecond();
		LambdaUpdateWrapper<Regionauth> u = new LambdaUpdateWrapper<>();
		u.apply("regionauth_id = {0}", regionauthIdPath)
				.eq(Regionauth::getCompanyId, companyId)
				.eq(Regionauth::getState, 1)
				.set(Regionauth::getState, -1)
				.set(Regionauth::getUpdated, now);
		int rows = regionauthMapper.update(null, u);
		if (rows <= 0) {
			throw new ResourceException("操作失败");
		}
	}
}
