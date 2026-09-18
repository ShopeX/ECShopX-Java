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

package cn.shopex.ecshopx.workwechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.workwechat.service.WorkWechatAppNotifyService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatCustomerNotifyService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatReportNotifyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = false,
		notFound = false)
@RestController("workWechatAdminV1Callback")
@RequestMapping("/api/v1/workwechat")
public class WorkWechatCallbackController {

	private final WorkWechatAppNotifyService workWechatAppNotifyService;
	private final WorkWechatCustomerNotifyService workWechatCustomerNotifyService;
	private final WorkWechatReportNotifyService reportNotifyService;

	public WorkWechatCallbackController(
			WorkWechatAppNotifyService workWechatAppNotifyService,
			WorkWechatCustomerNotifyService workWechatCustomerNotifyService,
			WorkWechatReportNotifyService reportNotifyService) {
		this.workWechatAppNotifyService = workWechatAppNotifyService;
		this.workWechatCustomerNotifyService = workWechatCustomerNotifyService;
		this.reportNotifyService = reportNotifyService;
	}

	@RequestMapping(value = "/notify/{corpid}", name = "企业微信回调")
	public ResponseEntity<?> notify(
			@PathVariable("corpid") String corpid,
			HttpServletRequest request,
			@RequestParam(value = "msg_signature", required = false) String msgSignature,
			@RequestParam(value = "timestamp", required = false) String timestamp,
			@RequestParam(value = "nonce", required = false) String nonce,
			@RequestParam(value = "echostr", required = false) String echostr,
			@RequestBody(required = false) String rawBody) {
		boolean echostrParamPresent = request.getParameter("echostr") != null;
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("msg_signature", msgSignature);
		queryParams.put("timestamp", timestamp);
		queryParams.put("nonce", nonce);
		queryParams.put("echostr", echostr);
		return workWechatAppNotifyService.handle(corpid, echostrParamPresent, queryParams, rawBody);
	}

	@RequestMapping(value = "/customer/notify/{corpid}", name = "客户联系回调")
	public ResponseEntity<?> customerNotify(
			@PathVariable("corpid") String corpid,
			HttpServletRequest request,
			@RequestParam(value = "msg_signature", required = false) String msgSignature,
			@RequestParam(value = "timestamp", required = false) String timestamp,
			@RequestParam(value = "nonce", required = false) String nonce,
			@RequestParam(value = "echostr", required = false) String echostr,
			@RequestBody(required = false) String rawBody) {
		boolean echostrParamPresent = request.getParameter("echostr") != null;
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("msg_signature", msgSignature);
		queryParams.put("timestamp", timestamp);
		queryParams.put("nonce", nonce);
		queryParams.put("echostr", echostr);
		return workWechatCustomerNotifyService.handle(corpid, echostrParamPresent, queryParams, rawBody);
	}

	@RequestMapping(value = "/report/notify/{corpid}", name = "通讯录回调")
	public ResponseEntity<?> reportNotify(
			@PathVariable("corpid") String corpid,
			HttpServletRequest request,
			@RequestParam(value = "msg_signature", required = false) String msgSignature,
			@RequestParam(value = "timestamp", required = false) String timestamp,
			@RequestParam(value = "nonce", required = false) String nonce,
			@RequestParam(value = "echostr", required = false) String echostr,
			@RequestBody(required = false) String rawBody) {
		boolean echostrParamPresent = request.getParameter("echostr") != null;
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("msg_signature", msgSignature);
		queryParams.put("timestamp", timestamp);
		queryParams.put("nonce", nonce);
		queryParams.put("echostr", echostr);
		return reportNotifyService.handle(corpid, echostrParamPresent, queryParams, rawBody);
	}
}
