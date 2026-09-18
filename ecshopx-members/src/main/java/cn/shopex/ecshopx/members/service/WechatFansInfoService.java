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

import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WechatFansInfoService {

	private final WechatFansMapper wechatFansMapper;

	public WechatFansInfoService(WechatFansMapper wechatFansMapper) {
		this.wechatFansMapper = wechatFansMapper;
	}

	public Object getWxFansInfo(String openId) {
		LambdaQueryWrapper<WechatFans> wrapper = new LambdaQueryWrapper<>();
		if (openId == null) {
			wrapper.isNull(WechatFans::getOpenId);
		} else {
			wrapper.eq(WechatFans::getOpenId, openId);
		}
		List<WechatFans> rows = wechatFansMapper.selectList(wrapper.last("LIMIT 1"));
		WechatFans row = rows.isEmpty() ? null : rows.get(0);
		if (row == null) {
			return Collections.emptyList();
		}
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
