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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberMassSmsFanOutDispatchPort;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.domain.MemberSmsLog;
import cn.shopex.ecshopx.members.mapper.MemberSmsLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberSmsSendService {

	private final MemberSmsLogMapper memberSmsLogMapper;
	private final AdminMemberMassSmsFanOutDispatchPort adminMemberMassSmsFanOutDispatchPort;

	public AdminMemberSmsSendService(
			MemberSmsLogMapper memberSmsLogMapper,
			@Qualifier("adminMemberMassSmsFanOutDispatchPortImpl")
					AdminMemberMassSmsFanOutDispatchPort adminMemberMassSmsFanOutDispatchPort) {
		this.memberSmsLogMapper = memberSmsLogMapper;
		this.adminMemberMassSmsFanOutDispatchPort = adminMemberMassSmsFanOutDispatchPort;
	}

	public Map<String, Object> smsSends(long companyId, HttpServletRequest request, Map<String, Object> bodyOrNull) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (bodyOrNull != null) {
			merged.putAll(bodyOrNull);
		}

		if (!merged.containsKey("mobile") || merged.get("mobile") == null) {
			throw new BadRequestException("mobile 必须为非空数组");
		}
		Object mobileRaw = merged.get("mobile");
		if (mobileRaw instanceof String || mobileRaw instanceof String[]) {
			throw new BadRequestException("mobile 必须为非空数组");
		}
		if (!(mobileRaw instanceof List<?>)) {
			throw new BadRequestException("mobile 必须为非空数组");
		}
		List<?> mobileList = (List<?>) mobileRaw;
		List<String> mobiles = new ArrayList<>();
		for (Object o : mobileList) {
			String s = String.valueOf(o).trim();
			if (!s.isEmpty()) {
				mobiles.add(s);
			}
		}
		if (mobiles.isEmpty()) {
			throw new BadRequestException("mobile 必须为非空数组");
		}

		Object sc = merged.get("sms_content");
		String smsContent =
				(sc == null || (sc instanceof String t && !StringUtils.hasText(t)))
						? null
						: String.valueOf(sc).trim();
		if (smsContent != null && smsContent.isEmpty()) {
			smsContent = null;
		}

		long now = System.currentTimeMillis() / 1000L;
		MemberSmsLog row = new MemberSmsLog();
		row.setCompanyId(companyId);
		row.setSendToPhones(String.join(",", mobiles));
		if (smsContent != null) {
			row.setSmsContent(smsContent);
		}
		row.setOperator("管理员");
		row.setStatus(1);
		row.setCreated(now);
		row.setUpdated(now);
		memberSmsLogMapper.insert(row);
		if (row.getLogId() == null || row.getLogId() <= 0) {
			throw new ResourceException("写入短信日志失败");
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("log_id", row.getLogId());
		data.put("company_id", companyId);
		data.put("send_to_phones", new ArrayList<>(mobiles));
		data.put("sms_content", row.getSmsContent() == null ? "" : row.getSmsContent());
		data.put("operator", "管理员");
		data.put("status", row.getStatus() == null ? 1 : row.getStatus());
		data.put("created", row.getCreated());
		data.put("updated", row.getUpdated());

		adminMemberMassSmsFanOutDispatchPort.dispatchFanOutAfterPersist(companyId, new ArrayList<>(mobiles), smsContent);
		return data;
	}
}
