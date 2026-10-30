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

package cn.shopex.ecshopx.espier.support;

import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Upload size / format user-facing messages resolved via {@code espier-messages*} bundles.
 */
public final class EspierUploadMessages {

	public static final String IMAGE_MAX_SIZE = "espier.upload.image_max_size";
	public static final String FILE_SIZE_EXCEEDED = "espier.upload.file_size_exceeded";
	public static final String INVALID_FORMAT_OR_SIZE = "espier.upload.invalid_format_or_size";

	/** Single image / video size cap in mebibytes (aligned with Spring multipart 50MB). */
	public static final int MAX_UPLOAD_MB = 50;
	public static final long MAX_UPLOAD_BYTES = MAX_UPLOAD_MB * 1024L * 1024L;

	private EspierUploadMessages() {}

	public static Locale currentLocale() {
		return Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
	}

	public static String imageMaxSize(MessageSource messageSource) {
		return messageSource.getMessage(
				IMAGE_MAX_SIZE,
				new Object[] {MAX_UPLOAD_MB},
				"图片上传最大为" + MAX_UPLOAD_MB + "M",
				currentLocale());
	}

	public static String fileSizeExceeded(MessageSource messageSource) {
		return messageSource.getMessage(
				FILE_SIZE_EXCEEDED, null, "上传文件大小超出限制", currentLocale());
	}

	public static String invalidFormatOrSize(MessageSource messageSource) {
		return messageSource.getMessage(
				INVALID_FORMAT_OR_SIZE, null, "请上传正确格式或标准大小文件", currentLocale());
	}
}
