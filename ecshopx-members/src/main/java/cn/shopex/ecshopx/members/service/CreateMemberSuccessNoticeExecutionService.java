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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.port.members.MemberCreateSuccessWxaNoticePort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CreateMemberSuccessNoticeExecutionService {

	private static final Logger log = LoggerFactory.getLogger(CreateMemberSuccessNoticeExecutionService.class);

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter REGISTER_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final MemberAccountService memberAccountService;
	private final MemberCreateSuccessWxaNoticePort wxaNoticePort;

	public CreateMemberSuccessNoticeExecutionService(
			MemberAccountService memberAccountService, MemberCreateSuccessWxaNoticePort wxaNoticePort) {
		this.memberAccountService = memberAccountService;
		this.wxaNoticePort = wxaNoticePort;
	}

	public void sendCreateMemberSuccessNotice(Map<String, Object> payload) {
		try {
			Object rawCompany = payload.get("company_id");
			Object rawUser = payload.get("user_id");
			if (rawCompany == null || rawUser == null) {
				return;
			}
			long companyId = ((Number) rawCompany).longValue();
			long userId = ((Number) rawUser).longValue();

			String wxaAppid = stringVal(payload.get("wxa_appid"));
			String openid = stringVal(payload.get("openid"));
			if (!StringUtils.hasText(wxaAppid) || !StringUtils.hasText(openid)) {
				return;
			}

			Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
			String dateStr = resolveRegisterTimeDisplay(member);
			String notice = buildNoticeText(companyId, payload, member);

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("date", dateStr);
			data.put("notice", notice);

			Map<String, Object> send = new LinkedHashMap<>();
			send.put("company_id", companyId);
			send.put("scenes_name", "memberCreateSucc");
			send.put("appid", wxaAppid);
			send.put("openid", openid);
			send.put("data", data);

			wxaNoticePort.send(send);
		} catch (RuntimeException e) {
			log.debug("create member success wxa notice skipped: {}", e.toString());
		}
	}

	private String buildNoticeText(long companyId, Map<String, Object> payload, Map<String, Object> member) {
		long inviterId = 0L;
		Object rawInv = payload.get("inviter_id");
		if (rawInv instanceof Number n) {
			inviterId = n.longValue();
		}
		if (inviterId <= 0L) {
			return "欢迎加入会员";
		}
		Map<String, Object> inviter = memberAccountService.getMemberInfo(inviterId, companyId);
		String invMobile = stringVal(inviter.get("mobile"));
		if (!StringUtils.hasText(invMobile)) {
			return "欢迎加入会员";
		}
		return "邀请人 " + DataMasking.maskUname(invMobile);
	}

	private static String resolveRegisterTimeDisplay(Map<String, Object> member) {
		String createdDate = stringVal(member.get("created_date"));
		if (StringUtils.hasText(createdDate)) {
			return createdDate;
		}
		Object rawCreated = member.get("created");
		if (rawCreated instanceof Number n && n.longValue() > 0L) {
			return Instant.ofEpochSecond(n.longValue()).atZone(SHANGHAI).format(REGISTER_TIME_FMT);
		}
		return Instant.now().atZone(SHANGHAI).format(REGISTER_TIME_FMT);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
