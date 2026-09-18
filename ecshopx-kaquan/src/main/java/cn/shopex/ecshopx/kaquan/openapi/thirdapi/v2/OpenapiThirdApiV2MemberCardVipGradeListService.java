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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeListService {

	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardVipGradeListService(
			VipGradeMapper vipGradeMapper, ObjectMapper objectMapper) {
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId, int page, int pageSize, String vipGradeIdRaw) {
		LambdaQueryWrapper<VipGrade> baseFilter = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId);
		applyVipGradeIdFilter(baseFilter, vipGradeIdRaw);

		long totalCount = vipGradeMapper.selectCount(baseFilter);

		LambdaQueryWrapper<VipGrade> listWrapper = new LambdaQueryWrapper<VipGrade>()
				.eq(VipGrade::getCompanyId, (int) companyId)
				.select(
						VipGrade::getVipGradeId,
						VipGrade::getLvType,
						VipGrade::getGradeName,
						VipGrade::getGuideTitle,
						VipGrade::getIsDefault,
						VipGrade::getIsDisabled,
						VipGrade::getBackgroundPicUrl,
						VipGrade::getPriceList,
						VipGrade::getPrivileges,
						VipGrade::getDescription,
						VipGrade::getExternalId,
						VipGrade::getCreated,
						VipGrade::getUpdated);
		applyVipGradeIdFilter(listWrapper, vipGradeIdRaw);
		listWrapper.orderByDesc(VipGrade::getVipGradeId);

		Page<VipGrade> pageRequest = new Page<>(page, pageSize, false);
		vipGradeMapper.selectPage(pageRequest, listWrapper);
		List<VipGrade> entities = pageRequest.getRecords();

		List<Map<String, Object>> list;
		if (entities.isEmpty()) {
			list = List.of();
		} else {
			list = entities.stream()
					.map(e -> OpenapiMemberCardVipGradeOpenApiFormatSupport
							.formatOpenApiVipGradeRow(e, objectMapper))
					.toList();
		}
		return formatListStruct(totalCount, list, page, pageSize);
	}

	private static void applyVipGradeIdFilter(LambdaQueryWrapper<VipGrade> wrapper, String vipGradeIdRaw) {
		if (phpIssetNonEmptyVipGradeId(vipGradeIdRaw)) {
			Long vipGradeId = tryParseLong(vipGradeIdRaw.trim());
			if (vipGradeId != null) {
				wrapper.eq(VipGrade::getVipGradeId, vipGradeId);
			} else {
				wrapper.eq(VipGrade::getVipGradeId, -1L);
			}
		}
	}

	private static boolean phpIssetNonEmptyVipGradeId(String raw) {
		return raw != null && !raw.isEmpty();
	}

	private static Long tryParseLong(String raw) {
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}
}
