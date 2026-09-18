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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Monitors;
import cn.shopex.ecshopx.datacube.mapper.MonitorsMapper;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MonitorsAddService {

	private final MonitorsMapper monitorsMapper;
	private final WechatAuthMapper wechatAuthMapper;

	public MonitorsAddService(MonitorsMapper monitorsMapper, WechatAuthMapper wechatAuthMapper) {
		this.monitorsMapper = monitorsMapper;
		this.wechatAuthMapper = wechatAuthMapper;
	}

	public Map<String, Object> addMonitor(
			long companyId,
			String wxappid,
			String monitorPath,
			String monitorPathParamsEncoded,
			String pageName,
			String regionauthId) {
		String params = monitorPathParamsEncoded != null ? monitorPathParamsEncoded : "";
		String region = regionauthId != null ? regionauthId : "";

		Monitors existing =
				monitorsMapper.selectOne(
						new LambdaQueryWrapper<Monitors>()
								.eq(Monitors::getCompanyId, companyId)
								.eq(Monitors::getWxappid, wxappid)
								.eq(Monitors::getMonitorPath, monitorPath)
								.eq(Monitors::getMonitorPathParams, params)
								.eq(Monitors::getRegionauthId, region));
		if (existing != null) {
			throw new ResourceException("此链接已经添加过，不能重复添加.");
		}

		WechatAuth auth = wechatAuthMapper.selectById(wxappid);
		String nickName = (auth != null && auth.getNickName() != null) ? auth.getNickName() : "";

		int now = (int) (System.currentTimeMillis() / 1000);
		Monitors entity = new Monitors();
		entity.setCompanyId(companyId);
		entity.setWxappid(wxappid);
		entity.setNickName(nickName);
		entity.setMonitorPath(monitorPath);
		entity.setMonitorPathParams(params);
		entity.setPageName(pageName != null ? pageName : "");
		entity.setRegionauthId(region);
		entity.setCreated(now);
		entity.setUpdated(now);

		monitorsMapper.insert(entity);

		Map<String, Object> row = new LinkedHashMap<>();
		Long mid = entity.getMonitorId();
		row.put("monitor_id", mid != null && mid <= Integer.MAX_VALUE ? mid.intValue() : mid);
		Long cid = entity.getCompanyId();
		row.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		row.put("wxappid", entity.getWxappid());
		row.put("nick_name", entity.getNickName() != null ? entity.getNickName() : "");
		row.put("monitor_path", entity.getMonitorPath());
		row.put("monitor_path_params", entity.getMonitorPathParams() != null ? entity.getMonitorPathParams() : "");
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("page_name", entity.getPageName() != null ? entity.getPageName() : "");
		row.put("regionauth_id", entity.getRegionauthId() != null ? entity.getRegionauthId() : "");
		return row;
	}
}
