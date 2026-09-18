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

package cn.shopex.ecshopx.shuyun.api.callback.v1;

import cn.shopex.ecshopx.shuyun.auth.InboundPrepareResult;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.auth.InboundSignedPrepareMode;
import cn.shopex.ecshopx.shuyun.auth.PreparedInboundCallback;
import cn.shopex.ecshopx.shuyun.service.openplatform.LoyaltyGradeCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("shuyunOpenPlatformLoyaltyGradeCallbackV1")
@RequestMapping("/api/v1")
public class ShuyunOpenPlatformLoyaltyGradeCallbackController {

	private final InboundSignedCallbackPreparer preparer;
	private final LoyaltyGradeCallbackService loyaltyGradeCallbackService;

	public ShuyunOpenPlatformLoyaltyGradeCallbackController(
			InboundSignedCallbackPreparer preparer, LoyaltyGradeCallbackService loyaltyGradeCallbackService) {
		this.preparer = preparer;
		this.loyaltyGradeCallbackService = loyaltyGradeCallbackService;
	}

	@PostMapping(
			value = {
				"/shuyun/open-platform/callback/loyalty-grade",
				"/third/shuyun/open-platform/callback/loyalty-grade"
			},
			name = "数云会员等级变更回调",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> callback(HttpServletRequest request) throws IOException {
		String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		InboundPrepareResult prepared =
				preparer.prepare(request, raw, InboundSignedPrepareMode.LOYALTY_MEMBER_GRADE_CHANGE);
		if (prepared instanceof InboundPrepareResult.Err err) {
			return err.response();
		}
		PreparedInboundCallback ctx = ((InboundPrepareResult.Ok) prepared).prepared();
		try {
			loyaltyGradeCallbackService.applyGradeChange(ctx.companyId(), ctx.body());
		} catch (IllegalArgumentException e) {
			return fail("42201", truncate(e.getMessage()));
		} catch (Exception e) {
			return fail("42203", truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
		}
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("msg", "");
		ok.put("code", "10000");
		ok.put("success", "true");
		return ResponseEntity.ok(ok);
	}

	private static ResponseEntity<Map<String, Object>> fail(String code, String msg) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("msg", msg == null ? "" : msg);
		body.put("code", code);
		body.put("success", "false");
		return ResponseEntity.ok(body);
	}

	private static String truncate(String msg) {
		if (msg == null) {
			return "";
		}
		return msg.length() > 200 ? msg.substring(0, 200) : msg;
	}
}
