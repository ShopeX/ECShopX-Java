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

package cn.shopex.ecshopx.adapay.api.front.v1;

import cn.shopex.ecshopx.adapay.service.AdapayBankCodesQueryService;
import cn.shopex.ecshopx.adapay.web.AdapayPromoterFrontRequestMerge;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("adapayAccountFrontV1")
@RequestMapping("/api/v1/h5app/wxapp/adapay")
public class AccountController {

	private final AdapayBankCodesQueryService adapayBankCodesQueryService;

	public AccountController(AdapayBankCodesQueryService adapayBankCodesQueryService) {
		this.adapayBankCodesQueryService = adapayBankCodesQueryService;
	}

	@GetMapping(value = "/bank/list", name = "获取银行列表")
	@FrontNoAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> getBanksListsGet(
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "page_size", required = false) String pageSize,
			@RequestParam(value = "bank_name", required = false) String bankName) {
		ParsedBankListInputs in = parseQueryBankListInputs(page, pageSize, bankName);
		Map<String, Object> data =
				adapayBankCodesQueryService.getBanksListsPost(in.bankName(), in.page(), in.pageSize());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/bank/list", name = "获取银行列表")
	@FrontAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> getBanksListsPost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = AdapayPromoterFrontRequestMerge.mergeRequestAll(request, body);
		ParsedBankListInputs in = parseMergedBankListInputs(merged);
		Map<String, Object> data =
				adapayBankCodesQueryService.getBanksListsPost(in.bankName(), in.page(), in.pageSize());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private record ParsedBankListInputs(String bankName, Integer page, Integer pageSize) {}

	private static ParsedBankListInputs parseQueryBankListInputs(String page, String pageSize, String bankName) {
		int pageInt = parsePositiveIntLoose((Object) page, 1);
		int pageSizeInt = parsePositiveIntLoose((Object) pageSize, 20);
		String bankNameFilter = parseBankNameFilter(bankName);
		return new ParsedBankListInputs(bankNameFilter, Integer.valueOf(pageInt), Integer.valueOf(pageSizeInt));
	}

	private static ParsedBankListInputs parseMergedBankListInputs(Map<String, Object> merged) {
		int page = parsePositiveIntLoose(merged.get("page"), 1);
		int pageSize = parsePositiveIntLoose(merged.get("page_size"), 20);
		String bankName = parseBankNameFilter(merged.get("bank_name"));
		return new ParsedBankListInputs(bankName, page, pageSize);
	}

	private static String parseBankNameFilter(Object raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = String.valueOf(raw).trim();
		return StringUtils.hasText(trimmed) ? trimmed : null;
	}

	private static int parsePositiveIntLoose(Object raw, int fallback) {
		if (raw == null) {
			return fallback;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return fallback;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException ex) {
				return fallback;
			}
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}
}
