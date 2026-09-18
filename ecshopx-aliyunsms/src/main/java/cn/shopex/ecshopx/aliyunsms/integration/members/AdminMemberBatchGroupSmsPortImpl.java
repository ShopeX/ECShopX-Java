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

package cn.shopex.ecshopx.aliyunsms.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchGroupSmsPort;
import cn.shopex.ecshopx.members.domain.MemberSmsLog;
import cn.shopex.ecshopx.members.mapper.MemberSmsLogMapper;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("adminMemberBatchGroupSmsPortImpl")
public class AdminMemberBatchGroupSmsPortImpl implements AdminMemberBatchGroupSmsPort {

	private final MemberSmsLogMapper memberSmsLogMapper;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final GroupSendSmsJobDispatchPublisher groupSendSmsJobDispatchPublisher;

	public AdminMemberBatchGroupSmsPortImpl(
			MemberSmsLogMapper memberSmsLogMapper,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			GroupSendSmsJobDispatchPublisher groupSendSmsJobDispatchPublisher) {
		this.memberSmsLogMapper = memberSmsLogMapper;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.groupSendSmsJobDispatchPublisher = groupSendSmsJobDispatchPublisher;
	}

	@Override
	public void enqueue(long companyId, String sender, long distributorId, List<Long> userIds, String smsContent) {
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		Map<Long, Map<String, String>> contacts =
				membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(userIds, userIds.size());
		List<String> phones = new ArrayList<>();
		for (Long uid : userIds) {
			Map<String, String> c = contacts.get(uid);
			String m = c == null ? "" : c.getOrDefault("mobile", "");
			if (StringUtils.hasText(m)) {
				phones.add(m);
			}
		}
		String joined = phones.stream().collect(Collectors.joining(","));
		long now = System.currentTimeMillis() / 1000L;
		MemberSmsLog row = new MemberSmsLog();
		row.setCompanyId(companyId);
		row.setSendToPhones(joined);
		row.setSmsContent(smsContent == null ? "" : smsContent);
		row.setOperator("管理员");
		row.setStatus(1);
		row.setCreated(now);
		row.setUpdated(now);
		memberSmsLogMapper.insert(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("send_to_phones", phones);
		payload.put("sms_content", smsContent == null ? "" : smsContent);
		payload.put("operator", "管理员");
		payload.put("sender", sender == null ? "" : sender);
		payload.put("distributor_id", distributorId);
		groupSendSmsJobDispatchPublisher.enqueueGroupSendSms(payload);
	}
}
