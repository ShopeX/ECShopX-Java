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

package cn.shopex.ecshopx.ali.port;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import cn.shopex.ecshopx.ali.mapper.AliMiniAppSettingMapper;
import cn.shopex.ecshopx.common.port.ali.AliMiniAppCompanyIdLookupPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AliMiniAppCompanyIdLookupPortImpl implements AliMiniAppCompanyIdLookupPort {

	private final AliMiniAppSettingMapper aliMiniAppSettingMapper;

	public AliMiniAppCompanyIdLookupPortImpl(AliMiniAppSettingMapper aliMiniAppSettingMapper) {
		this.aliMiniAppSettingMapper = aliMiniAppSettingMapper;
	}

	@Override
	public Long findCompanyIdByAuthorizerAppid(String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			return null;
		}
		AliMiniAppSetting row =
				aliMiniAppSettingMapper.selectOne(
						new LambdaQueryWrapper<AliMiniAppSetting>()
								.eq(AliMiniAppSetting::getAuthorizerAppid, authorizerAppid.trim())
								.last("LIMIT 1"));
		if (row == null || row.getCompanyId() == null) {
			return null;
		}
		long cid = row.getCompanyId();
		return cid > 0L ? cid : null;
	}
}
