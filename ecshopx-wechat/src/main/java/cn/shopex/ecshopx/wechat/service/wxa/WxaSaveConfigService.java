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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class WxaSaveConfigService {

	private final WechatAuthMapper wechatAuthMapper;

	public WxaSaveConfigService(WechatAuthMapper wechatAuthMapper) {
		this.wechatAuthMapper = wechatAuthMapper;
	}

	public void saveConfig(String wxaAppId, int autoPublish, String authorizerAppsecretTrimmed) {
		String pk = wxaAppId == null ? "" : wxaAppId.trim();
		WechatAuth row = wechatAuthMapper.selectById(pk);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		LambdaUpdateWrapper<WechatAuth> uw = new LambdaUpdateWrapper<>();
		uw.eq(WechatAuth::getAuthorizerAppid, pk)
				.set(WechatAuth::getAutoPublish, autoPublish)
				.set(WechatAuth::getAuthorizerAppsecret, authorizerAppsecretTrimmed)
				.set(WechatAuth::getUpdatedAt, LocalDateTime.now());
		wechatAuthMapper.update(null, uw);
	}
}
