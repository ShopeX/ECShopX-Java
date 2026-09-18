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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import cn.shopex.ecshopx.members.service.WechatFansSyncPersistService;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserInfoBatchService;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagMemberService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class WechatSubscribeDispatchListener implements DispatchListener {

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final OfficialAccountUserInfoBatchService userInfoBatchService;
	private final WechatFansSyncPersistService wechatFansSyncPersistService;
	private final OfficialAccountUserTagMemberService tagMemberService;
	private final WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;
	private final WechatFansMapper wechatFansMapper;
	private final TransactionTemplate transactionTemplate;

	public WechatSubscribeDispatchListener(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			OfficialAccountUserInfoBatchService userInfoBatchService,
			WechatFansSyncPersistService wechatFansSyncPersistService,
			OfficialAccountUserTagMemberService tagMemberService,
			WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper,
			WechatFansMapper wechatFansMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.tokenService = tokenService;
		this.userInfoBatchService = userInfoBatchService;
		this.wechatFansSyncPersistService = wechatFansSyncPersistService;
		this.tagMemberService = tagMemberService;
		this.wechatFansBindWechatTagMapper = wechatFansBindWechatTagMapper;
		this.wechatFansMapper = wechatFansMapper;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		String event = stringVal(payload.get("event"));
		if ("subscribe".equalsIgnoreCase(event)) {
			handleSubscribe(payload);
		} else if ("unsubscribe".equalsIgnoreCase(event)) {
			handleUnsubscribe(payload);
		}
	}

	private void handleSubscribe(Map<String, Object> payload) {
		String openId = stringVal(payload.get("openId"));
		String authorizerAppId = stringVal(payload.get("authorizerAppId"));
		Long companyIdObj = longOrNull(payload.get("company_id"));
		if (openId.isEmpty() || authorizerAppId.isEmpty() || companyIdObj == null) {
			return;
		}
		long companyId = companyIdObj;

		String token = tokenService.getAuthorizerAccessToken(authorizerAppId);
		List<Map<String, Object>> users = userInfoBatchService.batchGetUserInfo(token, List.of(openId));
		if (users.isEmpty()) {
			return;
		}
		Map<String, Object> userRow = users.get(0);
		Object unionid = userRow.get("unionid");
		if (unionid == null || stringVal(unionid).isEmpty()) {
			return;
		}
		wechatFansSyncPersistService.saveUser(authorizerAppId, companyId, List.of(userRow));
	}

	private void handleUnsubscribe(Map<String, Object> payload) {
		String openId = stringVal(payload.get("openId"));
		String authorizerAppId = stringVal(payload.get("authorizerAppId"));
		Long companyIdObj = longOrNull(payload.get("company_id"));
		if (openId.isEmpty() || authorizerAppId.isEmpty() || companyIdObj == null) {
			return;
		}
		long companyId = companyIdObj;

		LambdaQueryWrapper<WechatFansBindWechatTag> bindQuery =
				new LambdaQueryWrapper<WechatFansBindWechatTag>()
						.eq(WechatFansBindWechatTag::getOpenId, openId)
						.eq(WechatFansBindWechatTag::getCompanyId, companyId)
						.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppId);
		List<WechatFansBindWechatTag> binds = wechatFansBindWechatTagMapper.selectList(bindQuery);
		Set<Long> tagIds =
				binds.stream()
						.map(WechatFansBindWechatTag::getTagId)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		for (Long tagId : tagIds) {
			if (tagId != null) {
				tagMemberService.untagUsers(authorizerAppId, List.of(openId), tagId);
			}
		}

		transactionTemplate.executeWithoutResult(
				status -> {
					if (!binds.isEmpty()) {
						wechatFansBindWechatTagMapper.delete(bindQuery);
					}
					WechatFans fan =
							wechatFansMapper.selectOne(
									new LambdaQueryWrapper<WechatFans>()
											.eq(WechatFans::getCompanyId, companyId)
											.eq(WechatFans::getOpenId, openId)
											.eq(WechatFans::getAuthorizerAppid, authorizerAppId)
											.last("LIMIT 1"));
					if (fan != null) {
						fan.setSubscribed(Boolean.FALSE);
						fan.setUpdated(Instant.now().getEpochSecond());
						wechatFansMapper.updateById(fan);
					}
				});
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static Long longOrNull(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s && !s.isBlank()) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
