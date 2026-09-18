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
import cn.shopex.ecshopx.shuyun.service.openplatform.OfflineBenefitCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("shuyunOfflineBenefitCallbackV1")
@RequestMapping("/api/v1")
public class ShuyunOfflineBenefitCallbackController {

	private final InboundSignedCallbackPreparer preparer;
	private final OfflineBenefitCallbackService offlineBenefitCallbackService;

	public ShuyunOfflineBenefitCallbackController(
			InboundSignedCallbackPreparer preparer,
			OfflineBenefitCallbackService offlineBenefitCallbackService) {
		this.preparer = preparer;
		this.offlineBenefitCallbackService = offlineBenefitCallbackService;
	}

	@PostMapping(
			value = {
				"/shuyun/open-platform/callback/offline-benefit/create",
				"/third/shuyun/open-platform/callback/offline-benefit/create"
			},
			name = "数云线下权益创建回调",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> create(HttpServletRequest request) throws IOException {
		InboundPrepareResult prepared = prepare(request);
		if (prepared instanceof InboundPrepareResult.Err err) {
			return err.response();
		}
		PreparedInboundCallback ctx = ((InboundPrepareResult.Ok) prepared).prepared();
		try {
			String benefitId = offlineBenefitCallbackService.create(ctx.companyId(), ctx.body());
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("benefitId", benefitId);
			return ResponseEntity.ok(mapOf(10000, "", data));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(422).body(mapOf(422, e.getMessage(), new LinkedHashMap<>()));
		}
	}

	@PostMapping(
			value = {
				"/shuyun/open-platform/callback/offline-benefit/single-send",
				"/third/shuyun/open-platform/callback/offline-benefit/single-send"
			},
			name = "数云线下权益单笔发放回调",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> singleSend(HttpServletRequest request) throws IOException {
		InboundPrepareResult prepared = prepare(request);
		if (prepared instanceof InboundPrepareResult.Err err) {
			return err.response();
		}
		PreparedInboundCallback ctx = ((InboundPrepareResult.Ok) prepared).prepared();
		try {
			Map<String, String> data = offlineBenefitCallbackService.singleSend(ctx.companyId(), ctx.body());
			String benefitCode = data.getOrDefault("benefitCode", "");
			String msg = data.getOrDefault("message", "");
			int code = 10000;
			if (!StringUtils.hasText(benefitCode)) {
				code = msg.contains("异步发放处理中") ? 10001 : 50001;
			}
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("batchId", data.get("batchId"));
			payload.put("benefitCode", benefitCode);
			return ResponseEntity.ok(mapOf(code, msg, payload));
		} catch (IllegalArgumentException e) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("batchId", "");
			data.put("benefitCode", "");
			return ResponseEntity.status(422).body(mapOf(422, e.getMessage(), data));
		}
	}

	@PostMapping(
			value = {
				"/shuyun/open-platform/callback/offline-benefit/batch-send",
				"/third/shuyun/open-platform/callback/offline-benefit/batch-send"
			},
			name = "数云线下权益批量发放回调",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> batchSend(HttpServletRequest request) throws IOException {
		InboundPrepareResult prepared = prepare(request);
		if (prepared instanceof InboundPrepareResult.Err err) {
			return err.response();
		}
		PreparedInboundCallback ctx = ((InboundPrepareResult.Ok) prepared).prepared();
		try {
			Map<String, String> data = offlineBenefitCallbackService.batchSend(ctx.companyId(), ctx.body());
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("batchId", data.get("batchId"));
			return ResponseEntity.ok(mapOf(10000, data.getOrDefault("message", ""), payload));
		} catch (IllegalArgumentException e) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("batchId", "");
			return ResponseEntity.status(422).body(mapOf(422, e.getMessage(), data));
		}
	}

	private InboundPrepareResult prepare(HttpServletRequest request) throws IOException {
		String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		return preparer.prepare(request, raw, InboundSignedPrepareMode.OFFLINE_BENEFIT);
	}

	private static Map<String, Object> mapOf(int code, String message, Map<String, Object> data) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("message", message == null ? "" : message);
		body.put("data", data);
		return body;
	}
}
