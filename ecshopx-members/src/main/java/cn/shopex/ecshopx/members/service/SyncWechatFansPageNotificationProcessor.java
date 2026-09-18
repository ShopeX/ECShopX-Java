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

import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserInfoBatchService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SyncWechatFansPageNotificationProcessor {

	private static final Logger log = LoggerFactory.getLogger(SyncWechatFansPageNotificationProcessor.class);

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final OfficialAccountUserInfoBatchService officialAccountUserInfoBatchService;
	private final WechatFansSyncPersistService wechatFansSyncPersistService;

	public SyncWechatFansPageNotificationProcessor(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			OfficialAccountUserInfoBatchService officialAccountUserInfoBatchService,
			WechatFansSyncPersistService wechatFansSyncPersistService) {
		this.tokenService = tokenService;
		this.officialAccountUserInfoBatchService = officialAccountUserInfoBatchService;
		this.wechatFansSyncPersistService = wechatFansSyncPersistService;
	}

	public void handle(Map<String, Object> payload) {
		try {
			Object companyObj = payload.get("company_id");
			if (companyObj == null) {
				return;
			}
			long companyId = companyObj instanceof Number n ? n.longValue() : Long.parseLong(companyObj.toString());

			Object appidObj = payload.get("authorizer_appid");
			String authorizerAppid = appidObj == null ? "" : appidObj.toString().trim();
			if (authorizerAppid.isEmpty()) {
				return;
			}

			Object rawOpenIds = payload.get("open_ids");
			if (!(rawOpenIds instanceof List<?> list) || list.isEmpty()) {
				return;
			}
			List<String> openIds = new ArrayList<>(list.size());
			for (Object o : list) {
				if (o != null) {
					openIds.add(o.toString());
				}
			}
			if (openIds.isEmpty()) {
				return;
			}

			String token = tokenService.getAuthorizerAccessToken(authorizerAppid);
			int pages = (int) Math.ceil(openIds.size() / 100.0);
			for (int p = 0; p < pages; p++) {
				int from = p * 100;
				int to = Math.min(from + 100, openIds.size());
				List<String> sub = new ArrayList<>(openIds.subList(from, to));
				List<Map<String, Object>> userInfoList =
						officialAccountUserInfoBatchService.batchGetUserInfo(token, sub);
				wechatFansSyncPersistService.saveUser(authorizerAppid, companyId, userInfoList);
			}
		} catch (Exception e) {
			Object appidForLog = payload.get("authorizer_appid");
			log.error("wechat fans sync async page failed: authorizerAppid={}", appidForLog, e);
		}
	}
}
