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

import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCardGradeListService {

	private final MemberCardGradeMapper memberCardGradeMapper;
	private final MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardGradeListService(
			MemberCardGradeMapper memberCardGradeMapper,
			MemberCardGradeMultiLangReadService memberCardGradeMultiLangReadService,
			ObjectMapper objectMapper) {
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.memberCardGradeMultiLangReadService = memberCardGradeMultiLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId, int page, int pageSize, String gradeIdRaw, String langRaw) {
		String companyIdStr = String.valueOf(companyId);

		LambdaQueryWrapper<MemberCardGrade> baseFilter = new LambdaQueryWrapper<MemberCardGrade>()
				.eq(MemberCardGrade::getCompanyId, companyIdStr);
		applyGradeIdFilter(baseFilter, gradeIdRaw);

		long totalCount = memberCardGradeMapper.selectCount(baseFilter);

		LambdaQueryWrapper<MemberCardGrade> listWrapper = new LambdaQueryWrapper<MemberCardGrade>()
				.eq(MemberCardGrade::getCompanyId, companyIdStr)
				.select(
						MemberCardGrade::getGradeId,
						MemberCardGrade::getGradeName,
						MemberCardGrade::getDefaultGrade,
						MemberCardGrade::getBackgroundPicUrl,
						MemberCardGrade::getGradeBackground,
						MemberCardGrade::getDescription,
						MemberCardGrade::getPrivileges,
						MemberCardGrade::getPromotionCondition,
						MemberCardGrade::getExternalId,
						MemberCardGrade::getCreated,
						MemberCardGrade::getUpdated);
		applyGradeIdFilter(listWrapper, gradeIdRaw);

		Page<MemberCardGrade> pageRequest = new Page<>(page, pageSize, false);
		memberCardGradeMapper.selectPage(pageRequest, listWrapper);
		List<MemberCardGrade> entities = pageRequest.getRecords();

		List<Map<String, Object>> list;
		if (entities.isEmpty()) {
			list = List.of();
		} else {
			List<Map<String, Object>> overlayRows = entities.stream()
					.map(OpenapiMemberCardGradeOpenApiFormatSupport::toOverlayRow)
					.toList();
			String lang = StringUtils.hasText(langRaw) ? langRaw.trim() : "zh-CN";
			memberCardGradeMultiLangReadService.applyOverlays(companyId, overlayRows, lang);
			list = overlayRows.stream()
					.map(row -> OpenapiMemberCardGradeOpenApiFormatSupport.formatOpenApiGradeRow(row, objectMapper))
					.toList();
		}
		return formatListStruct(totalCount, list, page, pageSize);
	}

	private static void applyGradeIdFilter(LambdaQueryWrapper<MemberCardGrade> wrapper, String gradeIdRaw) {
		if (phpIssetNonEmptyGradeId(gradeIdRaw)) {
			Long gradeId = tryParseLong(gradeIdRaw.trim());
			if (gradeId != null) {
				wrapper.eq(MemberCardGrade::getGradeId, gradeId);
			} else {
				wrapper.eq(MemberCardGrade::getGradeId, -1L);
			}
		}
	}

	private static boolean phpIssetNonEmptyGradeId(String raw) {
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
