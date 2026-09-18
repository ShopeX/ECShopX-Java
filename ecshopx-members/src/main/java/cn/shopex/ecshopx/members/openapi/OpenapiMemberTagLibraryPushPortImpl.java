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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagLibraryPushFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagLibraryPushPort;
import cn.shopex.ecshopx.members.service.MemberTagLibraryPushService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberTagLibraryPushPortImpl implements OpenapiMemberTagLibraryPushPort {

	private static final Logger log = LoggerFactory.getLogger(OpenapiMemberTagLibraryPushPortImpl.class);

	private final MemberTagLibraryPushService memberTagLibraryPushService;

	public OpenapiMemberTagLibraryPushPortImpl(MemberTagLibraryPushService memberTagLibraryPushService) {
		this.memberTagLibraryPushService = memberTagLibraryPushService;
	}

	@Override
	public Map<String, Object> pushTagLibrary(long companyId, List<Map<String, Object>> tagLibrary) {
		try {
			return memberTagLibraryPushService.pushTagLibrary(companyId, tagLibrary);
		} catch (Exception e) {
			log.error("标签库推送失败::{}::companyId={}", e.getMessage(), companyId, e);
			throw new OpenapiMemberTagLibraryPushFailException(
					"标签库推送失败：" + e.getMessage());
		}
	}
}
