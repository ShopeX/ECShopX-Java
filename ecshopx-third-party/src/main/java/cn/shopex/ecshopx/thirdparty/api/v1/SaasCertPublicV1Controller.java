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

package cn.shopex.ecshopx.thirdparty.api.v1;

import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertBindShopNodeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 矩阵 / 证书中心公开回调（对齐 PHP routes/thirdparty/saascert.php，无需 JWT）。
 */
@RestController("thirdPartySaasCertPublicV1")
@RequestMapping("/api/v1")
public class SaasCertPublicV1Controller {

	private final SaasCertBindShopNodeService saasCertBindShopNodeService;

	public SaasCertPublicV1Controller(SaasCertBindShopNodeService saasCertBindShopNodeService) {
		this.saasCertBindShopNodeService = saasCertBindShopNodeService;
	}

	@RequestMapping(
			value = "/third/saascert/cert/validate",
			method = {RequestMethod.GET, RequestMethod.POST},
			name = "Shopex证书反查")
	public ResponseEntity<Map<String, String>> certiValidate(HttpServletRequest request) {
		Map<String, String> postdata = flattenRequestParams(request);
		return ResponseEntity.ok(saasCertBindShopNodeService.certiValidate(postdata));
	}

	@RequestMapping(
			value = "/third/saascert/matrix/callback/{companyId}",
			method = {RequestMethod.GET, RequestMethod.POST},
			produces = MediaType.TEXT_PLAIN_VALUE,
			name = "矩阵绑定节点回打")
	public ResponseEntity<String> bindrelationCallback(
			@PathVariable("companyId") long companyId, HttpServletRequest request) {
		if (companyId <= 0L) {
			return ResponseEntity.ok("error");
		}
		Map<String, String> postdata = flattenRequestParams(request);
		String msg = saasCertBindShopNodeService.bindShopNode(companyId, postdata);
		return ResponseEntity.ok(msg == null ? "error" : msg);
	}

	private static Map<String, String> flattenRequestParams(HttpServletRequest request) {
		Map<String, String> out = new LinkedHashMap<>();
		request.getParameterMap().forEach((key, values) -> {
			if (values == null || values.length == 0) {
				out.put(key, "");
			} else {
				out.put(key, values[0]);
			}
		});
		return out;
	}
}
