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
public class WxaRevertCodeReleaseService {

	private final WxaOpenPlatformCodeApiClient codeApiClient;

	public WxaRevertCodeReleaseService(WxaOpenPlatformCodeApiClient codeApiClient) {
		this.codeApiClient = codeApiClient;
	}

	public JsonNode revertcoderelease(long companyId, String wxaAppId) {
		JsonNode root = codeApiClient.getRevertCodeRelease(wxaAppId);
		if (root == null) {
			return null;
		}
		int ec = root.path("errcode").asInt(0);
		if (ec == 0) {
			return root;
		}
		if (ec == 87011) {
			throw new BadRequestException("现网已经在灰度发布，不能进行版本回退");
		}
		if (ec == 87012) {
			throw new BadRequestException("该版本不能回退");
		}
		if (ec == -1) {
			throw new BadRequestException("微信系统错误");
		}
		return null;
	}
}
