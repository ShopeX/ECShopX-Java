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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RegistrationRecordRowMapAssembler {

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public RegistrationRecordRowMapAssembler(SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> toApiRow(RegistrationRecord e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("record_id", e.getRecordId());
		m.put("activity_id", e.getActivityId());
		m.put("user_id", e.getUserId());
		m.put("mobile", Objects.toString(sensitiveFieldEncryptor.decrypt(e.getMobile() == null ? "" : e.getMobile()), ""));
		m.put("wxapp_appid", e.getWxappAppid());
		m.put("open_id", e.getOpenId());
		m.put("status", e.getStatus());
		m.put("content", e.getContent());
		m.put("reason", e.getReason());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("company_id", e.getCompanyId());
		m.put("verify_time", e.getVerifyTime());
		m.put("verify_operator", e.getVerifyOperator());
		m.put("true_name", e.getTrueName());
		m.put(
				"form_mobile",
				Objects.toString(sensitiveFieldEncryptor.decrypt(e.getFormMobile() == null ? "" : e.getFormMobile()), ""));
		m.put("group_no", e.getGroupNo());
		m.put("distributor_id", e.getDistributorId());
		m.put("record_no", e.getRecordNo());
		m.put("form_id", e.getFormId());
		m.put("get_points", e.getGetPoints());
		m.put("verify_code", e.getVerifyCode());
		m.put("is_white_list", e.getIsWhiteList());
		m.put("remark", e.getRemark());
		return m;
	}
}
