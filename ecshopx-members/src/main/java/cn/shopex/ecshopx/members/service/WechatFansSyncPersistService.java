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
import cn.shopex.ecshopx.common.util.ElementJoiner;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WechatFansSyncPersistService {

	private static final Logger log = LoggerFactory.getLogger(WechatFansSyncPersistService.class);

	private final WechatFansMapper wechatFansMapper;
	private final WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;
	private final TransactionTemplate transactionTemplate;

	public WechatFansSyncPersistService(
			WechatFansMapper wechatFansMapper,
			WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.wechatFansMapper = wechatFansMapper;
		this.wechatFansBindWechatTagMapper = wechatFansBindWechatTagMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void saveUser(String authorizerAppid, long companyId, List<Map<String, Object>> userList) {
		if (userList == null) {
			return;
		}
		for (Map<String, Object> wx : userList) {
			Object subObj = wx.get("subscribe");
			int subVal = subObj instanceof Number n ? n.intValue() : -1;
			if (subVal != 1) {
				continue;
			}
			try {
				transactionTemplate.executeWithoutResult(status -> addUserCore(authorizerAppid, companyId, wx));
			} catch (Exception e) {
				log.debug("wechat fans sync addUser skip: {}", e.toString());
			}
		}
	}

	private void addUserCore(String authorizerAppid, long companyId, Map<String, Object> wx) {
		if (companyId <= 0) {
			throw new ResourceException("公司信息不能为空");
		}

		Object openIdObj = wx.get("openid");
		String openId = openIdObj == null ? "" : String.valueOf(openIdObj).trim();
		if (openId.isEmpty()) {
			throw new ResourceException("用户标识无效");
		}

		String u = wx.get("unionid") == null ? "" : String.valueOf(wx.get("unionid")).trim();
		u = u.isEmpty() ? "" : u;

		WechatFans existing =
				wechatFansMapper.selectOne(
						new LambdaQueryWrapper<WechatFans>()
								.eq(WechatFans::getCompanyId, companyId)
								.eq(WechatFans::getOpenId, openId)
								.eq(WechatFans::getAuthorizerAppid, authorizerAppid)
								.eq(WechatFans::getUnionid, u));

		if (existing == null && u.isEmpty()) {
			throw new ResourceException("unionid不能为空");
		}

		long nowSec = Instant.now().getEpochSecond();

		WechatFans entity = existing == null ? new WechatFans() : existing;
		entity.setCompanyId(companyId);
		entity.setAuthorizerAppid(authorizerAppid);
		entity.setOpenId(openId);
		entity.setSubscribed(Boolean.TRUE);
		entity.setNickname(nullToEmptyStr(wx.get("nickname")));
		entity.setSex(numberToInt(wx.get("sex"), 0));
		entity.setCity(nullToEmptyStr(wx.get("city")));
		entity.setCountry(nullToEmptyStr(wx.get("country")));
		entity.setProvince(nullToEmptyStr(wx.get("province")));
		entity.setLanguage(nullToEmptyStr(wx.get("language")));
		entity.setHeadimgurl(nullToEmptyStr(wx.get("headimgurl")));
		entity.setSubscribeTime(numberToInt(wx.get("subscribe_time"), 0));
		entity.setRemark(nullToEmptyStr(wx.get("remark")));
		entity.setUnionid(u.isEmpty() ? "" : u);
		entity.setTagids(ElementJoiner.joinComma(wx.get("tagid_list")));

		if (existing == null) {
			if (entity.getCreated() == null) {
				entity.setCreated(nowSec);
			}
			if (entity.getUpdated() == null) {
				entity.setUpdated(nowSec);
			}
			wechatFansMapper.insert(entity);
		} else {
			entity.setUpdated(nowSec);
			wechatFansMapper.updateById(entity);
		}

		String tagids = entity.getTagids();
		Object rawTags = wx.get("tagid_list");
		if (ValuePresence.hasEffectiveValue(tagids) && rawTags instanceof List<?> tagList) {
			wechatFansBindWechatTagMapper.delete(
					new LambdaQueryWrapper<WechatFansBindWechatTag>()
							.eq(WechatFansBindWechatTag::getOpenId, openId)
							.eq(WechatFansBindWechatTag::getCompanyId, companyId)
							.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppid));
			for (Object tagEl : tagList) {
				if (tagEl == null) {
					continue;
				}
				long tagIdLong =
						tagEl instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(tagEl).trim());
				WechatFansBindWechatTag row = new WechatFansBindWechatTag();
				row.setTagId(tagIdLong);
				row.setOpenId(openId);
				row.setCompanyId(companyId);
				row.setAuthorizerAppid(authorizerAppid);
				wechatFansBindWechatTagMapper.insert(row);
			}
		}
	}

	private static String nullToEmptyStr(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static int numberToInt(Object v, int defaultVal) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return defaultVal;
	}
}
