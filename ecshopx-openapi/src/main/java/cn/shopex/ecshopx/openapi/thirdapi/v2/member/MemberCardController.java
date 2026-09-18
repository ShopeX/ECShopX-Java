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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardV2UpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2MemberCard")
@RequestMapping("/api/openapi/internal/v2")
public class MemberCardController extends OpenapiBaseController {

	private final OpenapiMemberCardV2DetailPort memberCardV2DetailPort;
	private final OpenapiMemberCardV2UpdatePort memberCardV2UpdatePort;

	public MemberCardController(
			OpenapiMemberCardV2DetailPort memberCardV2DetailPort,
			OpenapiMemberCardV2UpdatePort memberCardV2UpdatePort) {
		this.memberCardV2DetailPort = memberCardV2DetailPort;
		this.memberCardV2UpdatePort = memberCardV2UpdatePort;
	}

	@GetMapping(value = "/ecx.member_card.detail", name = "开放接口查询会员卡基础设置")
	public Map<String, Object> detail(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		return memberCardV2DetailPort.getMemberCardDetail(companyId);
	}

	@PostMapping(value = "/ecx.member_card.update", name = "开放接口修改会员卡基础设置")
	public Void update(
			HttpServletRequest request,
			@RequestParam(name = "brand_name", required = false) String brandNameParam,
			@RequestParam(name = "logo_url", required = false) String logoUrlParam,
			@RequestParam(name = "title", required = false) String titleParam,
			@RequestParam(name = "color", required = false) String colorParam,
			@RequestParam(name = "background_pic_url", required = false) String backgroundPicUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> params =
				OpenapiMemberCardV2UpdateHandlerParams.collectPresentFields(
						body,
						brandNameParam,
						logoUrlParam,
						titleParam,
						colorParam,
						backgroundPicUrlParam);
		memberCardV2UpdatePort.updateMemberCard(companyId, params);
		return null;
	}
}
