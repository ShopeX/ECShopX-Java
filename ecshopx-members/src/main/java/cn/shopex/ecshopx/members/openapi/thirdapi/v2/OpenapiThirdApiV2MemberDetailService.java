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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.deposit.port.OpenapiMemberDetailDepositRechargeSumPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberDetailPointBalancePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDepositTotalPort;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberV2ListQueryFilter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberDetailService {

	private static final String SQL_ERROR_MESSAGE = "系统异常";
	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OpenapiMemberV2ListEnrichmentService enrichmentService;
	private final OpenapiMemberDetailPointBalancePort pointBalancePort;
	private final AdminMemberGetInfoDepositTotalPort depositBalancePort;
	private final OpenapiMemberDetailDepositRechargeSumPort rechargeSumPort;

	public OpenapiThirdApiV2MemberDetailService(
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OpenapiMemberV2ListEnrichmentService enrichmentService,
			OpenapiMemberDetailPointBalancePort pointBalancePort,
			AdminMemberGetInfoDepositTotalPort depositBalancePort,
			OpenapiMemberDetailDepositRechargeSumPort rechargeSumPort) {
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.enrichmentService = enrichmentService;
		this.pointBalancePort = pointBalancePort;
		this.depositBalancePort = depositBalancePort;
		this.rechargeSumPort = rechargeSumPort;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId, String mobileRaw) {
		String mobilePlain = validateMobile(mobileRaw);

		OpenapiMemberV2ListQueryFilter filter = new OpenapiMemberV2ListQueryFilter();
		filter.setCompanyId(companyId);
		filter.setMobileEq(sensitiveFieldEncryptor.encrypt(mobilePlain));

		List<Map<String, Object>> rows;
		try {
			rows = membersMapper.selectMemberV2ListForOpenapi(new Page<>(1, 1, false), filter);
		} catch (DataAccessException ex) {
			throw new ResourceException(SQL_ERROR_MESSAGE);
		}

		decryptSensitiveFields(rows);
		if (!rows.isEmpty()) {
			enrichmentService.enrichDetailRaw(companyId, rows);
			appendPointAndDeposit(companyId, rows);
		}

		if (rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private void appendPointAndDeposit(long companyId, List<Map<String, Object>> rows) {
		for (Map<String, Object> item : rows) {
			long userId = longValueOrZero(item.get("user_id"));
			long point = pointBalancePort.getPointBalance(companyId, userId);
			item.put("point", (int) point);

			long haveFen = depositBalancePort.readTotalFen(companyId, userId);
			Map<Long, Long> rechargeSums = rechargeSumPort.sumRechargeSuccessFenByUserIds(List.of(userId));
			long totalFen = rechargeSums.getOrDefault(userId, 0L);

			Map<String, Object> deposit = new LinkedHashMap<>();
			deposit.put("have", fenToYuanString(haveFen));
			deposit.put("total", fenToYuanString(totalFen));
			item.put("deposit", deposit);
		}
	}

	private String validateMobile(String mobileRaw) {
		if (!StringUtils.hasText(mobileRaw)) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}
		String mobilePlain = mobileRaw.trim();
		if (!MOBILE_PATTERN.matcher(mobilePlain).matches()) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.MEMBER_EXIST, "会员手机号已存在");
		}
		return mobilePlain;
	}

	private void decryptSensitiveFields(List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			Object mobileEnc = row.get("mobile");
			if (mobileEnc != null) {
				row.put("mobile", sensitiveFieldEncryptor.decrypt(String.valueOf(mobileEnc)));
			}
			Object usernameEnc = row.get("username");
			if (usernameEnc != null) {
				row.put("username", sensitiveFieldEncryptor.decrypt(String.valueOf(usernameEnc)));
			}
		}
	}

	private static String fenToYuanString(long fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static long longValueOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
