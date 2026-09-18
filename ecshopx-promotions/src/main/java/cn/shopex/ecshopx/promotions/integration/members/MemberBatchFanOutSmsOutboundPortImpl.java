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

package cn.shopex.ecshopx.promotions.integration.members;

import cn.shopex.ecshopx.common.members.admin.MemberBatchFanOutSmsOutboundPort;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import org.springframework.stereotype.Service;

@Service("memberBatchFanOutSmsOutboundPortImpl")
public class MemberBatchFanOutSmsOutboundPortImpl implements MemberBatchFanOutSmsOutboundPort {

	private final SmsSendTestService smsSendTestService;

	public MemberBatchFanOutSmsOutboundPortImpl(SmsSendTestService smsSendTestService) {
		this.smsSendTestService = smsSendTestService;
	}

	@Override
	public void sendOne(long companyId, String mobilePlain, String smsContentPlain) {
		smsSendTestService.sendMarketingFanOutPlainBodyOrLog(companyId, mobilePlain, smsContentPlain);
	}
}
