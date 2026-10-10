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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskInviteRegisterCompletionService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateMemberSuccessMemberTaskInviteRegisterDispatchListener implements DispatchListener {

	private static final Logger log =
			LoggerFactory.getLogger(CreateMemberSuccessMemberTaskInviteRegisterDispatchListener.class);

	private final MemberTaskInviteRegisterCompletionService memberTaskInviteRegisterCompletionService;

	public CreateMemberSuccessMemberTaskInviteRegisterDispatchListener(
			MemberTaskInviteRegisterCompletionService memberTaskInviteRegisterCompletionService) {
		this.memberTaskInviteRegisterCompletionService = memberTaskInviteRegisterCompletionService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		log.debug(
				"member_task listener={} companyId={} userId={}",
				"CreateMemberSuccessMemberTaskInviteRegisterDispatchListener",
				payload == null ? null : payload.get("company_id"),
				payload == null ? null : payload.get("user_id"));
		memberTaskInviteRegisterCompletionService.apply(payload == null ? Map.of() : payload);
	}
}
