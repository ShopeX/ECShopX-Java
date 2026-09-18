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

package cn.shopex.ecshopx.openapi.thirdapi.gateway;

import cn.shopex.ecshopx.common.openapi.OpenapiNotImplementedException;
import cn.shopex.ecshopx.openapi.web.OpenapiPathPatterns;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("openapiGateway")
public class OpenapiGatewayController {

	private final OpenapiGatewayDispatchService dispatchService;

	public OpenapiGatewayController(OpenapiGatewayDispatchService dispatchService) {
		this.dispatchService = dispatchService;
	}

	@RequestMapping(
			value = {OpenapiPathPatterns.PUBLIC_PREFIX, OpenapiPathPatterns.PUBLIC_PREFIX + "/{method}"},
			method = {
				RequestMethod.GET,
				RequestMethod.POST,
				RequestMethod.PUT,
				RequestMethod.PATCH,
				RequestMethod.DELETE,
				RequestMethod.HEAD,
				RequestMethod.OPTIONS
			})
	public void gateway(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			dispatchService.dispatch(request, response, null);
		} catch (OpenapiGatewayDispatchService.OpenapiGatewayException ex) {
			dispatchService.writeFail(response, ex.getCode(), ex.getMessage(), ex.getData());
		} catch (OpenapiNotImplementedException ex) {
			dispatchService.writeFail(
					response,
					cn.shopex.ecshopx.common.openapi.OpenapiErrorCode.NOT_IMPLEMENTED,
					ex.getMessage(),
					Map.of("method", ex.getMethod(), "version", ex.getVersion()));
		}
	}
}
