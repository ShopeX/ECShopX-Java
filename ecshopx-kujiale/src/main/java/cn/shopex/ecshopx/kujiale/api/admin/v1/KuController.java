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

package cn.shopex.ecshopx.kujiale.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.kujiale.api.admin.v1.request.KujialeDesignerWorksBindItemRequest;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksBindItemService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksItemsListService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksListService;
import cn.shopex.ecshopx.kujiale.service.KujialeDesignerWorksUnbindItemService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO_FIXED)
@RestController("kujialeKuAdminV1")
@RequestMapping("/api/v1")
public class KuController {

	private final KujialeDesignerWorksBindItemService kujialeDesignerWorksBindItemService;
	private final KujialeDesignerWorksUnbindItemService kujialeDesignerWorksUnbindItemService;
	private final KujialeDesignerWorksItemsListService kujialeDesignerWorksItemsListService;
	private final KujialeDesignerWorksListService kujialeDesignerWorksListService;

	public KuController(
			KujialeDesignerWorksBindItemService kujialeDesignerWorksBindItemService,
			KujialeDesignerWorksUnbindItemService kujialeDesignerWorksUnbindItemService,
			KujialeDesignerWorksItemsListService kujialeDesignerWorksItemsListService,
			KujialeDesignerWorksListService kujialeDesignerWorksListService) {
		this.kujialeDesignerWorksBindItemService = kujialeDesignerWorksBindItemService;
		this.kujialeDesignerWorksUnbindItemService = kujialeDesignerWorksUnbindItemService;
		this.kujialeDesignerWorksItemsListService = kujialeDesignerWorksItemsListService;
		this.kujialeDesignerWorksListService = kujialeDesignerWorksListService;
	}

	@PostMapping(value = "/kujiale/designer-works/bind-item", name = "绑定设计师作品与商品")
	public ResponseEntity<Map<String, Object>> bindDesignerWorksItem(
			@FlexibleBody KujialeDesignerWorksBindItemRequest body) {
		return ResponseEntity.ok(kujialeDesignerWorksBindItemService.bind(body));
	}

	@DeleteMapping(value = "/kujiale/designer-works/unbind-item", name = "解绑设计师作品与商品")
	public ResponseEntity<Map<String, Object>> unbindDesignerWorksItem(
			HttpServletRequest request, @FlexibleBody JsonNode body) {
		// multipart 表单字段与 JSON 的合并方式不同，此路由对齐为一个空行记录
		JsonNode effective = body;
		String ct = request.getContentType();
		if (ct != null && ct.toLowerCase().contains("multipart/form-data")) {
			effective = JsonNodeFactory.instance.objectNode();
		}
		Map<String, Object> inner = kujialeDesignerWorksUnbindItemService.unbind(effective);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@Activated(routeAlias = "kujiale.designer.works.items")
	@GetMapping(value = "/kujiale/designer-works/items", name = "查询关联了design的商品列表")
	public ResponseEntity<Map<String, Object>> getDesignerWorksItems(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "item_name", required = false) String itemName,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "goods_bn", required = false) String goodsBn,
			@RequestParam(value = "design_id", required = false) String designId,
			@RequestParam(value = "design_name", required = false) String designName,
			@RequestParam(value = "approve_status", required = false) String[] approveStatus,
			@RequestParam(value = "item_category", required = false) String[] itemCategory) {
		long companyId = readCompanyIdForDesignerWorks(request);
		Map<String, Object> inner = kujialeDesignerWorksItemsListService.list(
				companyId,
				page,
				pageSize,
				itemName,
				itemBn,
				goodsBn,
				designId,
				designName,
				approveStatus,
				itemCategory);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	private static long readCompanyIdForDesignerWorks(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new ResourceException("无法获取公司ID");
		}
		long cid;
		if (v instanceof Number n) {
			cid = n.longValue();
		} else {
			try {
				cid = Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("无法获取公司ID");
			}
		}
		if (cid <= 0) {
			throw new ResourceException("无法获取公司ID");
		}
		return cid;
	}

	@Activated(routeAlias = "kujiale.designer.works.list")
	@GetMapping(value = "/kujiale/designer-works/list", name = "获取设计师作品列表")
	public ResponseEntity<Map<String, Object>> getDesignerWorksList(
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "design_name", required = false) String designName,
			@RequestParam(value = "design_id", required = false) String designId,
			@RequestParam(value = "keyword", required = false) String keyword) {
		Map<String, Object> inner =
				kujialeDesignerWorksListService.list(page, pageSize, designName, designId, keyword);
		return ResponseEntity.ok(Map.of("data", inner));
	}
}
