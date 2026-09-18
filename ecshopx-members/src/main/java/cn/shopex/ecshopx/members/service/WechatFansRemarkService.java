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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserRemarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WechatFansRemarkService {

	private final WechatFansMapper wechatFansMapper;
	private final OfficialAccountUserRemarkService officialAccountUserRemarkService;

	public WechatFansRemarkService(
			WechatFansMapper wechatFansMapper,
			OfficialAccountUserRemarkService officialAccountUserRemarkService) {
		this.wechatFansMapper = wechatFansMapper;
		this.officialAccountUserRemarkService = officialAccountUserRemarkService;
	}

	public Map<String, Object> wxremark(
			String authorizerAppid, Long companyId, String openId, String remark) {
		officialAccountUserRemarkService.updateRemark(authorizerAppid, openId, remark);

		WechatFans row = wechatFansMapper.selectOne(
				new LambdaQueryWrapper<WechatFans>()
						.eq(WechatFans::getOpenId, openId)
						.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
						.eq(WechatFans::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException("满足条件的用户不存在");
		}

		long nowSec = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<WechatFans> uw = new LambdaUpdateWrapper<WechatFans>()
				.eq(WechatFans::getOpenId, openId)
				.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
				.eq(WechatFans::getCompanyId, companyId)
				.set(WechatFans::getRemark, remark)
				.set(WechatFans::getUpdated, nowSec);
		int n = wechatFansMapper.update(null, uw);
		if (n <= 0) {
			throw new ResourceException("满足条件的用户不存在");
		}

		row.setRemark(remark);
		row.setUpdated(nowSec);
		return toFanDataMap(row);
	}

	private static Map<String, Object> toFanDataMap(WechatFans row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", row.getCompanyId());
		m.put("authorizer_appid", row.getAuthorizerAppid());
		m.put("subscribed", row.getSubscribed());
		m.put("open_id", row.getOpenId());
		m.put("nickname", row.getNickname());
		m.put("sex", row.getSex());
		m.put("city", row.getCity());
		m.put("country", row.getCountry());
		m.put("province", row.getProvince());
		m.put("language", row.getLanguage());
		m.put("headimgurl", row.getHeadimgurl());
		m.put("subscribe_time", row.getSubscribeTime());
		m.put("unionid", row.getUnionid());
		m.put("remark", row.getRemark());
		m.put("groupid", row.getGroupid());
		m.put("tagids", row.getTagids());
		m.put("tagpop", row.getTagpop() != null ? row.getTagpop() : Boolean.FALSE);
		m.put("remarkpop", row.getRemarkpop() != null ? row.getRemarkpop() : Boolean.FALSE);
		return m;
	}
}
