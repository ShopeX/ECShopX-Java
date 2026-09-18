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

package cn.shopex.ecshopx.companys.service.wxexternalconfig;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxExternalConfig;
import cn.shopex.ecshopx.companys.domain.WxExternalRoutes;
import cn.shopex.ecshopx.companys.mapper.WxExternalConfigMapper;
import cn.shopex.ecshopx.companys.mapper.WxExternalRoutesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxExternalConfigDeleteService {

	private final WxExternalConfigMapper wxExternalConfigMapper;
	private final WxExternalRoutesMapper wxExternalRoutesMapper;

	public WxExternalConfigDeleteService(
			WxExternalConfigMapper wxExternalConfigMapper,
			WxExternalRoutesMapper wxExternalRoutesMapper) {
		this.wxExternalConfigMapper = wxExternalConfigMapper;
		this.wxExternalRoutesMapper = wxExternalRoutesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteWxExternalConfig(long companyId, String wxExternalConfigIdRaw) {
		String p = wxExternalConfigIdRaw == null ? "" : wxExternalConfigIdRaw.trim();
		if (p.isEmpty()) {
			throw new ResourceException("配置不存在");
		}
		long configId;
		try {
			configId = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new ResourceException("配置不存在");
		}
		if (configId <= 0L) {
			throw new ResourceException("配置不存在");
		}

		wxExternalConfigMapper.delete(
				new LambdaQueryWrapper<WxExternalConfig>()
						.eq(WxExternalConfig::getCompanyId, companyId)
						.eq(WxExternalConfig::getWxExternalConfigId, configId));
		wxExternalRoutesMapper.delete(
				new LambdaQueryWrapper<WxExternalRoutes>()
						.eq(WxExternalRoutes::getCompanyId, companyId)
						.eq(WxExternalRoutes::getWxExternalConfigId, configId));
	}
}
