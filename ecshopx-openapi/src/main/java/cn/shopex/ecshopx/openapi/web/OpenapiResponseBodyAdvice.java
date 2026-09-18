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

package cn.shopex.ecshopx.openapi.web;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(basePackages = {
	"cn.shopex.ecshopx.openapi.thirdapi.v1",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.orders",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.member",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.items",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.distributor",
	"cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan"
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OpenapiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		if (returnType.getContainingClass().isAnnotationPresent(OpenapiResponse.class)) {
			return true;
		}
		return returnType.hasMethodAnnotation(OpenapiResponse.class);
	}

	@Override
	public Object beforeBodyWrite(
			Object body,
			MethodParameter returnType,
			MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof OpenapiEnvelope) {
			return body;
		}
		if (body instanceof byte[]) {
			return body;
		}
		if (selectedContentType != null && MediaType.IMAGE_PNG.includes(selectedContentType)) {
			return body;
		}
		return OpenapiEnvelope.success(body);
	}
}
