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

package cn.shopex.ecshopx.kujiale.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.H5KujialeLikeAuthAttributes;
import cn.shopex.ecshopx.kujiale.api.front.v1.request.KujialeDesignLikeRequest;
import cn.shopex.ecshopx.kujiale.api.front.v1.request.KujialeDesginWorkDetailRequest;
import cn.shopex.ecshopx.kujiale.api.front.v1.request.KujialeDesginWorkListRequest;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerTagsH5ListService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksDetailService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksH5ListService;
import cn.shopex.ecshopx.kujiale.integration.KujialeH5CitiesClient;
import cn.shopex.ecshopx.kujiale.service.KujialeH5ProductListByPicIdService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksLikeSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("kujialeKuFrontV1")
@RequestMapping("/api/v1/h5app")
public class KuController {

	private final KujialeDesignerWorksDetailService kujialeDesignerWorksDetailService;
	private final KujialeDesignerWorksH5ListService kujialeDesignerWorksH5ListService;
	private final KujialeDesignerWorksLikeSaveService kujialeDesignerWorksLikeSaveService;
	private final KujialeDesignerTagsH5ListService kujialeDesignerTagsH5ListService;
	private final KujialeH5CitiesClient kujialeH5CitiesClient;
	private final KujialeH5ProductListByPicIdService kujialeH5ProductListByPicIdService;

	public KuController(
			KujialeDesignerWorksDetailService kujialeDesignerWorksDetailService,
			KujialeDesignerWorksH5ListService kujialeDesignerWorksH5ListService,
			KujialeDesignerWorksLikeSaveService kujialeDesignerWorksLikeSaveService,
			KujialeDesignerTagsH5ListService kujialeDesignerTagsH5ListService,
			KujialeH5CitiesClient kujialeH5CitiesClient,
			KujialeH5ProductListByPicIdService kujialeH5ProductListByPicIdService) {
		this.kujialeDesignerWorksDetailService = kujialeDesignerWorksDetailService;
		this.kujialeDesignerWorksH5ListService = kujialeDesignerWorksH5ListService;
		this.kujialeDesignerWorksLikeSaveService = kujialeDesignerWorksLikeSaveService;
		this.kujialeDesignerTagsH5ListService = kujialeDesignerTagsH5ListService;
		this.kujialeH5CitiesClient = kujialeH5CitiesClient;
		this.kujialeH5ProductListByPicIdService = kujialeH5ProductListByPicIdService;
	}

	@FrontNoAuth
	@PostMapping(value = "/wxapp/kujiale/desginList", name = "设计师方案列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> desginWorkList(
			@FlexibleBody KujialeDesginWorkListRequest body) {
		Map<String, Object> data =
				kujialeDesignerWorksH5ListService.getDesignerWorksList(
						body != null ? body : new KujialeDesginWorkListRequest());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/kujiale/desginTagsList", name = "方案标签列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getDesginTagsList() {
		List<Map<String, Object>> list = kujialeDesignerTagsH5ListService.getTagsListTree();
		return ResponseEntity.ok(ApiResult.ok(list));
	}

	@FrontNoAuth
	@PostMapping(value = "/wxapp/kujiale/desginDetail", name = "设计师方案详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> desginWorkDetail(
			@FlexibleBody KujialeDesginWorkDetailRequest body) {
		if (body == null
				|| !StringUtils.hasText(body.getDesignId())
				|| !StringUtils.hasText(body.getPlanId())) {
			throw new ResourceException("参数错误");
		}
		return ResponseEntity.ok(
				ApiResult.ok(kujialeDesignerWorksDetailService.getDetail(body.getDesignId(), body.getPlanId())));
	}

	@FrontNoAuth
	@PostMapping(value = "/wxapp/kujiale/viewcount", name = "设计师方案浏览量")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDesignViewCount(
			@FlexibleBody KujialeDesginWorkDetailRequest body) {
		if (body == null
				|| !StringUtils.hasText(body.getDesignId())
				|| !StringUtils.hasText(body.getPlanId())) {
			throw new ResourceException("参数错误");
		}
		Map<String, Object> row =
				kujialeDesignerWorksDetailService.updateViewCountReturningRow(
						body.getDesignId().trim(), body.getPlanId().trim());
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/kujiale/getProductList", name = "获取渲染图商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getProductListByPicId(
			HttpServletRequest request,
			@RequestParam(value = "pic_id", required = false) String picId,
			@RequestParam(value = "company_id", required = false) Long companyId) {
		Map<String, Object> data =
				kujialeH5ProductListByPicIdService.build(picId, companyId, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/kujiale/getDesignerDetail", name = "获取渲染图详情")
	public ResponseEntity<ApiResult<Object>> getDesignerPicById(
			@RequestParam(value = "pic_id", required = false) String picId) {
		Object data = kujialeDesignerWorksDetailService.getDesignerPicByPicId(picId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/kujiale/getLocationList", name = "获取城市列表")
	public ResponseEntity<ApiResult<Object>> getLocationList() {
		return ResponseEntity.ok(ApiResult.ok(kujialeH5CitiesClient.fetchCities()));
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/kujiale/like", name = "设计师方案点赞")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateDesignLikeCount(
			HttpServletRequest request,
			@FlexibleBody(required = false) KujialeDesignLikeRequest body) {
		@SuppressWarnings("unchecked")
		Map<String, Object> claims =
				(Map<String, Object>) request.getAttribute(H5KujialeLikeAuthAttributes.JWT_CLAIMS);
		if (claims == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int userId = parseUserIdOrUnauthorized(claims.get("user_id"));
		if (body == null
				|| !StringUtils.hasText(body.getDesignId())
				|| !StringUtils.hasText(body.getPlanId())
				|| !StringUtils.hasText(body.getIsLike())) {
			throw new ResourceException("参数错误");
		}
		String type = "true".equals(body.getIsLike()) ? "like" : "unlike";
		kujialeDesignerWorksLikeSaveService.saveLike(
				body.getDesignId().trim(), body.getPlanId().trim(), userId, type);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static int parseUserIdOrUnauthorized(Object raw) {
		if (raw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int id;
		if (raw instanceof Number n) {
			id = n.intValue();
		} else {
			try {
				id = Integer.parseInt(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		}
		if (id <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return id;
	}
}
