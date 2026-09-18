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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class WxaUndocodeauditService {

	private final WeappRowQueryService weappRowQueryService;
	private final WxaOpenPlatformCodeApiClient codeApiClient;
	private final WeappProcessAuditForTryReleaseService weappProcessAuditForTryReleaseService;

	public WxaUndocodeauditService(
			WeappRowQueryService weappRowQueryService,
			WxaOpenPlatformCodeApiClient codeApiClient,
			WeappProcessAuditForTryReleaseService weappProcessAuditForTryReleaseService) {
		this.weappRowQueryService = weappRowQueryService;
		this.codeApiClient = codeApiClient;
		this.weappProcessAuditForTryReleaseService = weappProcessAuditForTryReleaseService;
	}

	public void undocodeaudit(long companyId, String wxaAppId) {
		String app = wxaAppId == null ? "" : wxaAppId.trim();
		if (weappRowQueryService.getWeappInfo(companyId, app).isEmpty()) {
			throw new BadRequestException("没有需要撤回的版本");
		}
		JsonNode root = codeApiClient.withdrawAudit(app);
		int ec = root.path("errcode").asInt(0);
		if (ec == 0) {
			weappProcessAuditForTryReleaseService.applyUndocodeauditSuccess(app);
			return;
		}
		if (ec == 87013) {
			throw new BadRequestException("撤回次数达到上限（每天一次，每个月10次）");
		}
		if (ec == -1) {
			throw new BadRequestException("微信系统错误");
		}
		String msg = root.path("errmsg").asText("微信接口错误");
		if (msg == null || msg.isBlank()) {
			msg = "微信接口错误";
		}
		throw new BadRequestException(msg);
	}
}
