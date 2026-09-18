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

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PushMemberTagRelationJobDispatchPublisher {

	private static final DispatchOptions PUSH_MEMBER_TAG_RELATION_JOB_OPTIONS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					"marketing",
					null,
					RetryPolicy.platformDefault());

	private final DispatchFacade dispatchFacade;

	public PushMemberTagRelationJobDispatchPublisher(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	public void enqueuePushMemberTagRelation(
			long companyId, String action, List<Map<String, Object>> relations) {
		dispatchPushMemberTagRelationJob(companyId, action, relations);
	}

	private void dispatchPushMemberTagRelationJob(
			long companyId, String action, List<Map<String, Object>> relations) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("action", action == null ? "" : action);
		payload.put("relations", relations == null ? List.of() : relations);
		dispatchFacade.dispatchJob(
				MembersBundleDispatchJobNames.PUSH_MEMBER_TAG_RELATION_JOB,
				payload,
				PUSH_MEMBER_TAG_RELATION_JOB_OPTIONS);
	}
}
