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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.dto.OpenapiMemberV2ListQueryFilter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberListService {

	private static final String SQL_ERROR_MESSAGE = "系统异常";

	private final OpenapiMemberV2ListFilterBuilder filterBuilder;
	private final OpenapiMemberV2ListEnrichmentService enrichmentService;
	private final MembersMapper membersMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OpenapiThirdApiV2MemberListService(
			OpenapiMemberV2ListFilterBuilder filterBuilder,
			OpenapiMemberV2ListEnrichmentService enrichmentService,
			MembersMapper membersMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.filterBuilder = filterBuilder;
		this.enrichmentService = enrichmentService;
		this.membersMapper = membersMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId, int page, int pageSize, Map<String, Object> rawParams) {
		OpenapiMemberV2ListFilterBuilder.BuildResult built = filterBuilder.build(companyId, rawParams);
		OpenapiMemberV2ListQueryFilter filter = built.filter();
		MemberV2ListContext context = built.context();

		long totalCount;
		List<Map<String, Object>> rows;
		try {
			totalCount = membersMapper.countMemberV2ListForOpenapiDistinct(filter);
			rows = membersMapper.selectMemberV2ListForOpenapi(new Page<>(page, pageSize, false), filter);
		} catch (DataAccessException ex) {
			throw new ResourceException(SQL_ERROR_MESSAGE);
		}

		decryptSensitiveFields(rows);
		if (!rows.isEmpty()) {
			enrichmentService.enrichList(companyId, rows, context);
		}
		return OpenapiThirdApiV2MemberTagListService.formatListStruct(totalCount, rows, page, pageSize);
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
}
