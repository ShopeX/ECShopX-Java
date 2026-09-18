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
import cn.shopex.ecshopx.members.service.CreateMemberSuccessNoticeExecutionService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreateMemberSuccessNoticeDispatchListener implements DispatchListener {

	private final CreateMemberSuccessNoticeExecutionService executionService;

	public CreateMemberSuccessNoticeDispatchListener(CreateMemberSuccessNoticeExecutionService executionService) {
		this.executionService = executionService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		String openid = stringVal(payload.get("openid"));
		String wxaAppid = stringVal(payload.get("wxa_appid"));
		if (!StringUtils.hasText(openid) || !StringUtils.hasText(wxaAppid)) {
			return;
		}
		executionService.sendCreateMemberSuccessNotice(payload);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
