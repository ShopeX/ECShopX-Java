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
public class WxaTryReleaseService {

	private final WxaOpenPlatformCodeApiClient codeApiClient;
	private final WeappProcessAuditForTryReleaseService weappProcessAuditForTryReleaseService;

	public WxaTryReleaseService(
			WxaOpenPlatformCodeApiClient codeApiClient,
			WeappProcessAuditForTryReleaseService weappProcessAuditForTryReleaseService) {
		this.codeApiClient = codeApiClient;
		this.weappProcessAuditForTryReleaseService = weappProcessAuditForTryReleaseService;
	}

	public String tryRelease(long companyId, String wxaAppId) {
		JsonNode status = codeApiClient.getLatestAuditstatus(wxaAppId);
		if (status.path("errcode").asInt(0) > 0) {
			throw new BadRequestException(status.path("errmsg").asText("微信接口错误"));
		}
		int st = status.path("status").asInt(-1);
		if (st == 1) {
			weappProcessAuditForTryReleaseService.applyAuditRejected(
					wxaAppId, status.path("reason").asText(""));
			return "小程序审核失败，请查看失败原因";
		}
		if (st == 2) {
			return "小程序正在审核";
		}
		codeApiClient.releaseMiniProgramIgnoring85052(wxaAppId);
		weappProcessAuditForTryReleaseService.applyReleased(wxaAppId);
		return "小程序发布成功";
	}
}
