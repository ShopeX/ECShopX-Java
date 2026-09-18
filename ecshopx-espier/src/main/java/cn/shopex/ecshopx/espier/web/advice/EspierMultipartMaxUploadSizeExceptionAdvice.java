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

package cn.shopex.ecshopx.espier.web.advice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.espier.api.front.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EspierMultipartMaxUploadSizeExceptionAdvice {

	private static final String H5_WXAPP_ESPIER_UPLOAD_SUFFIX = "/wxapp/espier/upload";

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public void handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		throw multipartTooLargeMessage(request);
	}

	@ExceptionHandler(MultipartException.class)
	public void handleMultipartWrapped(MultipartException ex, HttpServletRequest request) throws MultipartException {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof MaxUploadSizeExceededException) {
				throw multipartTooLargeMessage(request);
			}
		}
		throw ex;
	}

	private static ResourceException multipartTooLargeMessage(HttpServletRequest request) {
		if (request != null && isH5WxappEspierUpload(request)) {
			return new ResourceException("图片上传最大为2M");
		}
		return new ResourceException("请上传正确格式或标准大小文件");
	}

	private static boolean isH5WxappEspierUpload(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.endsWith(H5_WXAPP_ESPIER_UPLOAD_SUFFIX);
	}
}
