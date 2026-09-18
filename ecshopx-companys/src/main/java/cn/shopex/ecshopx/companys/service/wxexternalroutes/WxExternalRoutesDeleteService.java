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

package cn.shopex.ecshopx.companys.service.wxexternalroutes;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.WxExternalRoutes;
import cn.shopex.ecshopx.companys.mapper.WxExternalRoutesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class WxExternalRoutesDeleteService {

	private final WxExternalRoutesMapper wxExternalRoutesMapper;

	public WxExternalRoutesDeleteService(WxExternalRoutesMapper wxExternalRoutesMapper) {
		this.wxExternalRoutesMapper = wxExternalRoutesMapper;
	}

	public void deleteWxExternalRoutes(long companyId, String wxExternalRoutesIdRaw) {
		String p = wxExternalRoutesIdRaw == null ? "" : wxExternalRoutesIdRaw.trim();
		if (p.isEmpty()) {
			throw new ResourceException("路径不存在");
		}
		long routesId;
		try {
			routesId = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new ResourceException("路径不存在");
		}
		if (routesId <= 0L) {
			throw new ResourceException("路径不存在");
		}

		wxExternalRoutesMapper.delete(
				new LambdaQueryWrapper<WxExternalRoutes>()
						.eq(WxExternalRoutes::getCompanyId, companyId)
						.eq(WxExternalRoutes::getWxExternalRoutesId, routesId));
	}
}
