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

package cn.shopex.ecshopx.kaquan.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCreateRequestMergeService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeUpdateService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardSetService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("kaquanMemberCardAdminV1")
@RequestMapping("/api/v1")
public class MemberCardController {

	private final DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService;
	private final MemberCardSetService memberCardSetService;
	private final MemberCardGradeUpdateService memberCardGradeUpdateService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final ObjectMapper objectMapper;

	public MemberCardController(DiscountCardCreateRequestMergeService discountCardCreateRequestMergeService,
			MemberCardSetService memberCardSetService,
			MemberCardGradeUpdateService memberCardGradeUpdateService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			ObjectMapper objectMapper) {
		this.discountCardCreateRequestMergeService = discountCardCreateRequestMergeService;
		this.memberCardSetService = memberCardSetService;
		this.memberCardGradeUpdateService = memberCardGradeUpdateService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "membercard.setting")
	@PutMapping(value = "/membercard", name = "更新会员卡设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setMemberCard(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		Map<String, Object> result = memberCardSetService.setMemberCard(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "membercard.info")
	@GetMapping(value = "/membercard", name = "获取会员卡信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMemberCard(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> result = memberCardSetService.getMemberCard(companyId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "membercard.grade.add")
	@PutMapping(value = "/membercard/grade", name = "更新会员卡等级")
	@SuppressWarnings("unused")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMembercardGrade(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestParam(name = "card_id", required = false) String ignoredCardId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> merged = discountCardCreateRequestMergeService.merge(request, body);
		if (!merged.containsKey("grade_info") || merged.get("grade_info") == null) {
			throw new BadRequestException("grade_info 无效");
		}
		Object gradeInfoRaw = merged.get("grade_info");
		List<Map<String, Object>> gradeInfoList = parseGradeInfoList(gradeInfoRaw);
		memberCardGradeUpdateService.updateGrades(companyId, gradeInfoList);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private List<Map<String, Object>> parseGradeInfoList(Object gradeInfoRaw) {
		if (gradeInfoRaw instanceof String s) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node == null || !node.isArray()) {
					throw new BadRequestException("grade_info 格式无效");
				}
				List<Map<String, Object>> list = new ArrayList<>();
				for (JsonNode el : node) {
					list.add(objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {}));
				}
				return list;
			} catch (BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("grade_info 格式无效");
			}
		}
		if (gradeInfoRaw instanceof List<?> rawList) {
			List<Map<String, Object>> list = new ArrayList<>();
			for (Object el : rawList) {
				if (el instanceof Map<?, ?> mm) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : mm.entrySet()) {
						if (e.getKey() != null) {
							row.put(String.valueOf(e.getKey()), e.getValue());
						}
					}
					list.add(row);
				} else {
					try {
						list.add(objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {}));
					} catch (IllegalArgumentException e) {
						throw new BadRequestException("grade_info 格式无效");
					}
				}
			}
			return list;
		}
		throw new BadRequestException("grade_info 格式无效");
	}

	@Activated(routeAlias = "membercard.default.grade")
	@GetMapping(value = "/membercard/defaultGrade", name = "获取会员卡默认等级")
	public ResponseEntity<ApiResult<Object>> getDefaultGrade(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Object data = memberCardGradeQueryService.getDefaultGradeData(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "membercard.grade.list")
	@GetMapping(value = "/membercard/grades", name = "获取会员等级列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getGradeList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		List<Map<String, Object>> list = memberCardGradeQueryService.getGradeListByCompanyId(companyId, true);
		return ResponseEntity.ok(ApiResult.ok(list));
	}
}
