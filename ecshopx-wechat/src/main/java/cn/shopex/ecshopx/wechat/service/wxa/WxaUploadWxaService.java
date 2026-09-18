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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.config.WechatWxaPublishProperties;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.wxa.model.MergedUploadWxaInput;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaUploadWxaService {

	private final WechatWxaPublishProperties props;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaWeappPublishOrchestrationService publishOrchestration;

	public WxaUploadWxaService(
			WechatWxaPublishProperties props,
			WechatAuthQueryService wechatAuthQueryService,
			WxaWeappPublishOrchestrationService publishOrchestration) {
		this.props = props;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.publishOrchestration = publishOrchestration;
	}

	public void uploadWxa(long companyId, String authorizerAppid, long operatorId, MergedUploadWxaInput in) {
		String wxa = in.wxaAppId() == null ? "" : in.wxaAppId().trim();
		if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, wxa)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		boolean automatic = Boolean.TRUE.equals(props.getIsAutomaticSubmitReview());
		if (automatic) {
			publishOrchestration.submitAudit(
					companyId,
					authorizerAppid,
					operatorId,
					in.templateName(),
					in.wxaName(),
					in.templateOptions(),
					wxa,
					false);
		} else {
			publishOrchestration.onlyCommitTempCode(
					companyId,
					authorizerAppid,
					operatorId,
					in.templateName(),
					in.wxaName(),
					in.templateOptions(),
					wxa);
		}
	}

	public static boolean wxaAppIdHasText(String trimmed) {
		return trimmed != null && StringUtils.hasText(trimmed) && !"0".equals(trimmed.trim());
	}

	/**
	 * 是否与「请求里带了 wxaAppId 且需先做绑定校验」一致：仅对原始参数判断（不 trim）。
	 * 视为未带有效参数、不触发绑定校验：{@code null}、空串、单字符串 {@code "0"}。
	 */
	public static boolean wxaAppIdRawParamRequiresBindingCheck(String wxaAppIdFromRequest) {
		if (wxaAppIdFromRequest == null || wxaAppIdFromRequest.isEmpty()) {
			return false;
		}
		return !"0".equals(wxaAppIdFromRequest);
	}

	public void submitReview(long companyId, String authorizerAppid, long operatorId, MergedUploadWxaInput in) {
		String rawWxa = in.wxaAppId();
		String wxaNorm = rawWxa == null ? "" : rawWxa.trim();
		if (wxaAppIdHasText(wxaNorm)
				&& !wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, wxaNorm)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		publishOrchestration.submitReviewWithoutCodeUpload(
				companyId,
				authorizerAppid,
				operatorId,
				in.templateName(),
				in.wxaName(),
				in.templateOptions(),
				wxaNorm);
	}

	public void commitTempCode(long companyId, String authorizerAppid, long operatorId, MergedUploadWxaInput in) {
		String rawWxa = in.wxaAppId();
		String wxaNormalized = rawWxa == null ? "" : rawWxa.trim();
		boolean wxaAppIdHasText = StringUtils.hasText(wxaNormalized);
		if (wxaAppIdHasText) {
			if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, wxaNormalized)) {
				throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
			}
		} else if (!Boolean.TRUE.equals(props.getOpenThird())) {
			throw new BadRequestException(
					"未配置小程序 AppId 且开放平台已关闭，无法完成域名修改与代码提交", 400);
		}
		publishOrchestration.onlyCommitTempCode(
				companyId,
				authorizerAppid,
				operatorId,
				in.templateName(),
				in.wxaName(),
				in.templateOptions(),
				wxaNormalized);
	}
}
