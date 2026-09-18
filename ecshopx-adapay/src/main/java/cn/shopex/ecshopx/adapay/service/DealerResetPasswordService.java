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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DealerResetPasswordService {

	private static final String PASSWORD_ALPHABET =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#";

	private static final int DEFAULT_PASSWORD_LENGTH = 12;

	private final OperatorsCommandService operatorsCommandService;
	private final CompanySceneSmsSendPort companySceneSmsSendPort;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;

	public DealerResetPasswordService(
			OperatorsCommandService operatorsCommandService,
			CompanySceneSmsSendPort companySceneSmsSendPort,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort) {
		this.operatorsCommandService = operatorsCommandService;
		this.companySceneSmsSendPort = companySceneSmsSendPort;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
	}

	public void resetPassword(long companyId, long jwtOperatorId, String operatorIdPath) {
		if (operatorIdPath == null || operatorIdPath.trim().isEmpty()) {
			throw new ResourceException("请选择要修改的账号");
		}
		long targetOperatorId;
		try {
			targetOperatorId = Long.parseLong(operatorIdPath.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("operator_id 格式不正确");
		}

		String plainPassword = generatePlainPassword(DEFAULT_PASSWORD_LENGTH);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("password", plainPassword);
		Map<String, Object> rs = operatorsCommandService.updateOperator(targetOperatorId, companyId, payload);

		String username = rs.get("username") == null ? "" : String.valueOf(rs.get("username"));
		String mobile = rs.get("mobile") == null ? "" : String.valueOf(rs.get("mobile")).trim();
		if (mobile.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}

		try {
			companySceneSmsSendPort.sendDealerAccountResetPwd(companyId, mobile, username, plainPassword);
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			String msg = e.getMessage();
			throw new ResourceException(msg != null ? msg : "短信发送失败");
		}

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("name", username);

		long relDealerId;
		if (rs.containsKey("is_dealer_main") && isDealerMainFalsy(rs.get("is_dealer_main"))) {
			relDealerId = parseLongSafe(rs.get("dealer_parent_id"), targetOperatorId);
		} else {
			relDealerId = targetOperatorId;
		}
		long relMerchantId = jwtOperatorId;

		adapayOperationLogRecordPort.logRecord(logParams, relMerchantId, "dealer/reset", "merchant", jwtOperatorId);
		adapayOperationLogRecordPort.logRecord(logParams, relDealerId, "dealer/reset", "dealer", jwtOperatorId);
	}

	private static String generatePlainPassword(int length) {
		SecureRandom rnd = new SecureRandom();
		StringBuilder sb = new StringBuilder(length);
		int bound = PASSWORD_ALPHABET.length();
		for (int i = 0; i < length; i++) {
			sb.append(PASSWORD_ALPHABET.charAt(rnd.nextInt(bound)));
		}
		return sb.toString();
	}

	private static boolean isDealerMainFalsy(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = String.valueOf(raw).trim();
		if ("0".equals(s)) {
			return true;
		}
		if ("1".equals(s)) {
			return false;
		}
		return false;
	}

	private static long parseLongSafe(Object value, long fallback) {
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
