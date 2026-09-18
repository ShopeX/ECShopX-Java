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

package cn.shopex.ecshopx.companys.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.companys.wxapp.H5WxappWorkWechatLoginPort;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("companysFrontV1Wxapp")
@RequestMapping("/api/v1/h5app")
public class WxappController {

	private static final String MSG_H5_COMPANY_UNAUTHORIZED = "无权访问该API,非法访问！";

	private final H5WxappWorkWechatLoginPort h5WxappWorkWechatLoginPort;

	public WxappController(H5WxappWorkWechatLoginPort h5WxappWorkWechatLoginPort) {
		this.h5WxappWorkWechatLoginPort = h5WxappWorkWechatLoginPort;
	}

	@PostMapping(value = "/wxapp/workwechatlogin", name = "企业微信小程序登录")
	public ResponseEntity<ApiResult<Map<String, Object>>> workwechatlogin(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body) {
		long companyId = requirePositiveH5CompanyId(request);
		Object appnameObj = body == null ? null : body.get("appname");
		String appname = appnameObj == null ? "" : String.valueOf(appnameObj).trim();
		if (!StringUtils.hasText(appname)) {
			throw new BadRequestException("缺少name参数，登录失败!");
		}
		Object codeObj = body == null ? null : body.get("code");
		String code = codeObj == null ? "" : String.valueOf(codeObj).trim();
		if (!StringUtils.hasText(code)) {
			throw new BadRequestException("导购登陆失败！");
		}
		Map<String, Object> data = h5WxappWorkWechatLoginPort.workwechatlogin(companyId, appname, code);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long requirePositiveH5CompanyId(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
			}
		} else {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException(MSG_H5_COMPANY_UNAUTHORIZED);
		}
		return companyId;
	}
}
