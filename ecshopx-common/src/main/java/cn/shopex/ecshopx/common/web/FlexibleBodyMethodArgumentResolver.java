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

package cn.shopex.ecshopx.common.web;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 统一解析 JSON / form-urlencoded / multipart 为 POJO 或 {@link Map}。
 */
public class FlexibleBodyMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ObjectMapper objectMapper;
	@Nullable
	private final Validator validator;

	public FlexibleBodyMethodArgumentResolver(ObjectMapper objectMapper, @Nullable Validator validator) {
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(FlexibleBody.class);
	}

	@Override
	public Object resolveArgument(
			MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new IllegalStateException("HttpServletRequest is required");
		}
		FlexibleBody ann = parameter.getParameterAnnotation(FlexibleBody.class);
		boolean required = ann == null || ann.required();

		Object body = readBody(request, parameter, required);
		if (body == null && required) {
			throw new BadRequestException("请求体不能为空");
		}
		if (body != null && parameter.hasParameterAnnotation(Valid.class) && validator != null) {
			validate(body, parameter);
		}
		return body;
	}

	@Nullable
	private Object readBody(HttpServletRequest request, MethodParameter parameter, boolean required) throws IOException {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");

		Class<?> rawType = parameter.getParameterType();
		if (JsonNode.class.isAssignableFrom(rawType)) {
			if (jsonLike) {
				byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
				if (bytes.length == 0) {
					return null;
				}
				try {
					return objectMapper.readTree(bytes);
				} catch (JsonProcessingException ex) {
					if (!required) {
						return null;
					}
					throw ex;
				}
			}
			Map<String, Object> fromForm = FlexibleHttpServletParameterMap.toObjectMap(request);
			if (fromForm.isEmpty()) {
				return null;
			}
			return objectMapper.valueToTree(fromForm);
		}
		if (Map.class.isAssignableFrom(rawType)) {
			if (jsonLike) {
				byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
				if (bytes.length == 0) {
					return null;
				}
				JavaType javaType = objectMapper.getTypeFactory().constructType(parameter.getGenericParameterType());
				try {
					return objectMapper.readValue(bytes, javaType);
				} catch (JsonProcessingException ex) {
					if (!required) {
						return null;
					}
					throw ex;
				}
			}
			Map<String, Object> fromForm = FlexibleHttpServletParameterMap.toObjectMap(request);
			if (required && fromForm.isEmpty()) {
				throw new BadRequestException("请求体不能为空");
			}
			return fromForm;
		}

		if (jsonLike) {
			byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
			if (bytes.length == 0) {
				return required ? null : BeanUtils.instantiateClass(rawType);
			}
			JavaType javaType = objectMapper.getTypeFactory().constructType(parameter.getGenericParameterType());
			return objectMapper.readValue(bytes, javaType);
		}

		Object target = BeanUtils.instantiateClass(rawType);
		Map<String, Object> fromForm = FlexibleHttpServletParameterMap.toObjectMap(request);
		if (fromForm.isEmpty()) {
			return required ? null : target;
		}
		JavaType javaType = objectMapper.getTypeFactory().constructType(parameter.getGenericParameterType());
		try {
			return objectMapper.convertValue(fromForm, javaType);
		} catch (IllegalArgumentException ex) {
			ServletRequestDataBinder binder = new ServletRequestDataBinder(target, parameter.getParameterName());
			binder.bind(request);
			return target;
		}
	}

	private void validate(Object body, MethodParameter parameter) throws MethodArgumentNotValidException {
		Set<ConstraintViolation<Object>> violations = validator.validate(body);
		if (violations.isEmpty()) {
			return;
		}
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(body, "body");
		for (ConstraintViolation<Object> cv : violations) {
			String field = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "";
			errors.addError(new FieldError("body", field, cv.getMessage()));
		}
		throw new MethodArgumentNotValidException(parameter, errors);
	}
}
