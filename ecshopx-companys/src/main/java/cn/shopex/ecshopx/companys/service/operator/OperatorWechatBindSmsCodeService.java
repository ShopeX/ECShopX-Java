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

package cn.shopex.ecshopx.companys.service.operator;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OperatorWechatBindSmsCodeService {

	private static final Pattern CN_MOBILE_WECHAT_BIND = Pattern.compile("^1\\d{10}$");

	private final OperatorsQueryService operatorsQueryService;
	private final DistributorWechatRelQueryService distributorWechatRelQueryService;
	private final OperatorSmsSendOrchestrator operatorSmsSendOrchestrator;

	public OperatorWechatBindSmsCodeService(
			OperatorsQueryService operatorsQueryService,
			DistributorWechatRelQueryService distributorWechatRelQueryService,
			OperatorSmsSendOrchestrator operatorSmsSendOrchestrator) {
		this.operatorsQueryService = operatorsQueryService;
		this.distributorWechatRelQueryService = distributorWechatRelQueryService;
		this.operatorSmsSendOrchestrator = operatorSmsSendOrchestrator;
	}

	public void sendWechatBindSmsCode(Map<String, Object> input) {
		String mobile = stringVal(input.get("mobile"));
		if (mobile == null || !CN_MOBILE_WECHAT_BIND.matcher(mobile).matches()) {
			throw new ResourceException("手机号码错误");
		}

		String type = stringVal(input.get("type"));
		if (type == null || type.isBlank()) {
			type = "login";
		} else {
			type = type.trim();
		}

		Map<String, Object> operatorsInfo = operatorsQueryService.getOperatorByMobile(mobile, "distributor");
		if (operatorsInfo == null || operatorsInfo.isEmpty()) {
			throw new ResourceException("该手机号尚未关联云店账号");
		}

		long companyId = readRequiredPositiveLong(operatorsInfo.get("company_id"));
		long operatorId = readRequiredPositiveLong(operatorsInfo.get("operator_id"));

		if (distributorWechatRelQueryService.existsByCompanyIdAndOperatorId(companyId, operatorId)) {
			throw new ResourceException("该手机号已在店务端绑定");
		}

		if (!"login".equals(type)) {
			throw new ResourceException("错误的短信验证类型");
		}

		operatorSmsSendOrchestrator.sendVerifyCode(companyId, mobile, type);
	}

	private static String stringVal(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static long readRequiredPositiveLong(Object raw) {
		if (raw == null) {
			throw new ResourceException("该手机号尚未关联云店账号");
		}
		long v;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("该手机号尚未关联云店账号");
			}
		}
		if (v <= 0L) {
			throw new ResourceException("该手机号尚未关联云店账号");
		}
		return v;
	}
}
