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
import cn.shopex.ecshopx.members.service.WechatFansTagsSyncService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SyncWechatTagsDispatchListener implements DispatchListener {

	private final WechatFansTagsSyncService wechatFansTagsSyncService;

	public SyncWechatTagsDispatchListener(WechatFansTagsSyncService wechatFansTagsSyncService) {
		this.wechatFansTagsSyncService = wechatFansTagsSyncService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		Object companyObj = payload.get("company_id");
		long companyId =
				companyObj instanceof Number n
						? n.longValue()
						: Long.parseLong(String.valueOf(companyObj).trim());
		String authorizerAppid = String.valueOf(payload.get("authorizer_appid"));
		wechatFansTagsSyncService.syncWechatTags(authorizerAppid, companyId);
	}
}
