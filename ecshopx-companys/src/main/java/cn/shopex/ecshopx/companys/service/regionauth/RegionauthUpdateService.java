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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class RegionauthUpdateService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthUpdateService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public void update(long companyId, String regionauthIdPath, @Nullable Object regionauthNameRaw) {
		String name = stringValue(regionauthNameRaw).trim();
		if (name.isEmpty()) {
			throw new BadRequestException("地区名称不能为空");
		}
		if (name.length() > 50) {
			throw new BadRequestException("地区名称长度不能超过50");
		}

		int now = (int) Instant.now().getEpochSecond();
		LambdaUpdateWrapper<Regionauth> u = new LambdaUpdateWrapper<>();
		u.apply("regionauth_id = {0}", regionauthIdPath)
				.eq(Regionauth::getCompanyId, companyId)
				.eq(Regionauth::getState, 1)
				.set(Regionauth::getRegionauthName, name)
				.set(Regionauth::getUpdated, now);
		int rows = regionauthMapper.update(null, u);
		if (rows <= 0) {
			throw new ResourceException("操作失败");
		}
	}

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		return String.valueOf(o);
	}
}
