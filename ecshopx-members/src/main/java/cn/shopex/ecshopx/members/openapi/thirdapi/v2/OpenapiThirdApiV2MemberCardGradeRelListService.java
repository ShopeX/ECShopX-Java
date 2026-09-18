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
import cn.shopex.ecshopx.common.kaquan.port.OpenapiVipGradeRelUserOpenapiPagePort;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberListQueryFilter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCardGradeRelListService {

	private final MembersMapper membersMapper;
	private final OpenapiVipGradeRelUserOpenapiPagePort vipGradeRelUserOpenapiPagePort;
	private final OpenapiMemberCardGradeRelEnrichmentService enrichmentService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberCardGradeRelListService(
			MembersMapper membersMapper,
			OpenapiVipGradeRelUserOpenapiPagePort vipGradeRelUserOpenapiPagePort,
			OpenapiMemberCardGradeRelEnrichmentService enrichmentService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.membersMapper = membersMapper;
		this.vipGradeRelUserOpenapiPagePort = vipGradeRelUserOpenapiPagePort;
		this.enrichmentService = enrichmentService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			boolean gradeIdPresent,
			String gradeIdRaw,
			boolean vipGradeIdPresent,
			String vipGradeIdRaw,
			int page,
			int pageSize) {
		BranchSpec branch =
				validateAndResolveBranch(
						gradeIdPresent, gradeIdRaw, vipGradeIdPresent, vipGradeIdRaw);

		long totalCount;
		List<Map<String, Object>> rows;

		if (branch.byGrade()) {
			OpenapiMemberListQueryFilter filter = new OpenapiMemberListQueryFilter();
			filter.setCompanyId(companyId);
			filter.setGradeIdEq(branch.idValue());

			totalCount = membersMapper.countMemberListForOpenapi(filter);
			Page<Map<String, Object>> mpPage = new Page<>(page, pageSize, false);
			List<Map<String, Object>> rawRows = membersMapper.selectMemberListForOpenapi(mpPage, filter);
			rows = new ArrayList<>(rawRows.size());
			for (Map<String, Object> rawRow : rawRows) {
				LinkedHashMap<String, Object> item = new LinkedHashMap<>();
				item.put("user_id", longOrNull(rawRow.get("user_id")));
				item.put("mobile", decryptOrEmpty(rawRow.get("mobile")));
				rows.add(item);
			}
			if (!rows.isEmpty()) {
				enrichmentService.appendDetailToList(companyId, rows, false);
			}
		} else {
			OpenapiVipGradeRelUserOpenapiPagePort.PageResult relPage =
					vipGradeRelUserOpenapiPagePort.listUserIdsByVipGrade(
							companyId, branch.idValue(), page, pageSize);
			totalCount = relPage.totalCount();
			rows = new ArrayList<>();
			for (Long userId : relPage.userIds()) {
				rows.add(new LinkedHashMap<>(Map.of("user_id", userId)));
			}
			if (!rows.isEmpty()) {
				enrichmentService.appendInfoToList(companyId, rows, true);
				enrichmentService.appendDetailToList(companyId, rows, false);
			}
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStructPhpHandlerOrder(
				totalCount, reshapeToMobileUsernameOnly(rows), page, pageSize);
	}

	private record BranchSpec(boolean byGrade, long idValue) {}

	private static BranchSpec validateAndResolveBranch(
			boolean gradeIdPresent,
			String gradeIdRaw,
			boolean vipGradeIdPresent,
			String vipGradeIdRaw) {
		if (!gradeIdPresent && !vipGradeIdPresent) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "会员等级ID参数错误");
		}
		if (gradeIdPresent && vipGradeIdPresent) {
			throw new OpenapiMemberV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "会员等级ID与付费会员等级ID不能同时存在");
		}
		if (gradeIdPresent) {
			return new BranchSpec(true, parseStrictInteger(gradeIdRaw, "会员等级ID参数错误"));
		}
		return new BranchSpec(false, parseStrictInteger(vipGradeIdRaw, "付费会员等级ID参数错误"));
	}

	private static long parseStrictInteger(String raw, String message) {
		if (raw == null || raw.trim().isEmpty()) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
		String trimmed = raw.trim();
		if (trimmed.contains(".") || !trimmed.matches("-?\\d+")) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
	}

	private static List<Map<String, Object>> reshapeToMobileUsernameOnly(List<Map<String, Object>> rows) {
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (Map<String, Object> item : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("mobile", stringOrEmpty(item.get("mobile")));
			row.put("username", stringOrEmpty(item.get("username")));
			out.add(row);
		}
		return out;
	}

	private static String stringOrEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw);
	}

	private String decryptOrEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw);
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return sensitiveFieldEncryptor.decrypt(s);
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
