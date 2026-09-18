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

package cn.shopex.ecshopx.members.api.front.v1;

import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Apple form_post 回调（无 FrontNoAuth；对齐 PHP routes/frontapi/member.php L124–126）。 */
@RestController("membersFrontV1TrustLoginAppleCallback")
@RequestMapping("/api/v1/h5app/wxapp/trustlogin/apple")
public class TrustLoginAppleCallbackController {

	private final SocialTrustLoginService socialTrustLoginService;

	public TrustLoginAppleCallbackController(SocialTrustLoginService socialTrustLoginService) {
		this.socialTrustLoginService = socialTrustLoginService;
	}

	@RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
	public ResponseEntity<String> appleOAuthCallback(
			HttpServletRequest request,
			@RequestParam(name = "h5_host", required = false) String queryH5Host,
			@RequestParam(name = "state", required = false) String state,
			@RequestParam(name = "code", required = false) String code,
			@RequestParam(name = "error", required = false) String error,
			@RequestParam(name = "error_description", required = false) String errorDescription) {
		String bodyState = request.getParameter("state");
		String bodyCode = request.getParameter("code");
		String effectiveState = StringUtils.hasText(state) ? state : bodyState;
		String effectiveCode = StringUtils.hasText(code) ? code : bodyCode;
		String bodyError = request.getParameter("error");
		String effectiveError = StringUtils.hasText(error) ? error : bodyError;
		String bodyErrorDesc = request.getParameter("error_description");
		String effectiveErrorDesc =
				StringUtils.hasText(errorDescription) ? errorDescription : bodyErrorDesc;

		String h5Host = socialTrustLoginService.resolveAppleCallbackH5Host(queryH5Host, effectiveState);
		if (!StringUtils.hasText(h5Host)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Missing H5_BASE_URL configuration");
		}
		if (StringUtils.hasText(effectiveError)) {
			Map<String, String> extra = new LinkedHashMap<>();
			extra.put("error", effectiveError);
			if (StringUtils.hasText(effectiveErrorDesc)) {
				extra.put("error_description", effectiveErrorDesc);
			}
			String landing = socialTrustLoginService.buildAppleH5LandingUrl(h5Host, "", extra);
			return redirect(landing);
		}
		String normalized = SocialTrustLoginService.normalizeOAuthCode(effectiveCode);
		if (!StringUtils.hasText(normalized)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing authorization code");
		}
		String landing = socialTrustLoginService.buildAppleH5LandingUrl(h5Host, normalized, Map.of());
		return redirect(landing);
	}

	private static ResponseEntity<String> redirect(String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.LOCATION, url);
		return new ResponseEntity<>("", headers, HttpStatus.FOUND);
	}
}
