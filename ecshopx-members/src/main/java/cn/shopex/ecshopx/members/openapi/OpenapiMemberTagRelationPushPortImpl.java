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

package cn.shopex.ecshopx.members.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagRelationPushFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagRelationPushPort;
import cn.shopex.ecshopx.members.service.MemberTagRelationInboundPushService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberTagRelationPushPortImpl implements OpenapiMemberTagRelationPushPort {

	private static final Logger log = LoggerFactory.getLogger(OpenapiMemberTagRelationPushPortImpl.class);

	private final MemberTagRelationInboundPushService memberTagRelationInboundPushService;

	public OpenapiMemberTagRelationPushPortImpl(
			MemberTagRelationInboundPushService memberTagRelationInboundPushService) {
		this.memberTagRelationInboundPushService = memberTagRelationInboundPushService;
	}

	@Override
	public Map<String, Object> pushTagRelations(
			long companyId,
			String action,
			String userId,
			String mobile,
			List<Map<String, Object>> relations) {
		try {
			return memberTagRelationInboundPushService.pushTagRelations(
					companyId, action, userId, mobile, relations);
		} catch (OpenapiMemberTagRelationPushFailException e) {
			throw e;
		} catch (Exception e) {
			log.error("标签关系推送失败::{}::companyId={}", e.getMessage(), companyId, e);
			throw new OpenapiMemberTagRelationPushFailException(
					"E9999", "标签关系推送失败：" + e.getMessage());
		}
	}
}
