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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 热词图片格式与大小校验（ECX-11390 F-002/AC-005）。
 *
 * <p>PRD 要求：gif、png、jpg，文件小于 5 MB，不限尺寸。扩展名与文件头魔数必须同时匹配，
 * 避免把任意文件改名后当图片上传。
 */
public final class SearchHotwordImageRules {

	/** 单张图片大小上限：严格小于 5 MB。 */
	public static final long MAX_BYTES = 5L * 1024L * 1024L;

	public static final Set<String> ALLOWED_EXTENSIONS = Set.of("gif", "png", "jpg", "jpeg");

	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

	private static final byte[] GIF_MAGIC = {'G', 'I', 'F', '8'};

	private SearchHotwordImageRules() {}

	/** 归一化扩展名为 gif / png / jpg；不支持时抛 422。 */
	public static String requireAllowedExtension(String originalFilename) {
		String ext = extensionOf(originalFilename);
		if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new BadRequestException(
					"仅支持 gif、png、jpg 格式的图片",
					Map.of("file", List.of("validation.image_extension")),
					422);
		}
		return "jpeg".equals(ext) ? "jpg" : ext;
	}

	/** 校验大小：必须大于 0 且严格小于 5 MB。 */
	public static void assertSize(long size) {
		if (size <= 0L) {
			throw new BadRequestException(
					"请上传图片文件", Map.of("file", List.of("validation.required")), 422);
		}
		if (size >= MAX_BYTES) {
			throw new BadRequestException(
					"图片大小必须小于 5MB",
					Map.of("file", List.of("validation.image_size")),
					422);
		}
	}

	/** 校验文件头与扩展名一致。 */
	public static void assertContentMatches(byte[] bytes, String ext) {
		String detected = detectType(bytes);
		boolean matches = switch (ext) {
			case "gif" -> "gif".equals(detected);
			case "png" -> "png".equals(detected);
			default -> "jpg".equals(detected);
		};
		if (!matches) {
			throw new BadRequestException(
					"仅支持 gif、png、jpg 格式的图片",
					Map.of("file", List.of("validation.image_extension")),
					422);
		}
	}

	/** 依据文件头识别图片类型，返回 {@code gif}/{@code png}/{@code jpg} 或 {@code null}。 */
	public static String detectType(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		if (startsWith(bytes, GIF_MAGIC)) {
			return "gif";
		}
		if (startsWith(bytes, PNG_MAGIC)) {
			return "png";
		}
		if (startsWith(bytes, JPEG_MAGIC)) {
			return "jpg";
		}
		return null;
	}

	private static boolean startsWith(byte[] bytes, byte[] magic) {
		if (bytes.length < magic.length) {
			return false;
		}
		for (int i = 0; i < magic.length; i++) {
			if (bytes[i] != magic[i]) {
				return false;
			}
		}
		return true;
	}

	private static String extensionOf(String originalFilename) {
		if (originalFilename == null) {
			return null;
		}
		int dot = originalFilename.lastIndexOf('.');
		if (dot < 0 || dot == originalFilename.length() - 1) {
			return null;
		}
		return originalFilename.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
	}
}
