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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class AliyunsmsAddSmsSignImageSupport {

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Value("${aliyunsms.sign.default-image-path:}")
	private String defaultImagePath;

	public String resolveDefaultSignImagePath() {
		return defaultImagePath == null ? "" : defaultImagePath.trim();
	}

	/**
	 * Enriches {@code sign_file} / {@code delegate_file} path entries into structures suitable for the update-sign
	 * client ({@code fileContents} base64, {@code fileSuffix}). When both paths are absent, fills {@code sign_file}
	 * from {@link #resolveDefaultSignImagePath()}.
	 */
	public Map<String, Object> enrichModifyParams(@SuppressWarnings("unused") long companyId, Map<String, Object> params) {
		if (params == null) {
			throw new ResourceException("参数不能为空");
		}
		Map<String, Object> out = new LinkedHashMap<>(params);
		boolean hasSignPath = pathStringPresent(out.get("sign_file"));
		boolean hasDelegatePath = pathStringPresent(out.get("delegate_file"));
		if (!hasSignPath && !hasDelegatePath) {
			String def = resolveDefaultSignImagePath();
			if (def.isEmpty()) {
				throw new ResourceException("未配置默认签名图路径");
			}
			out.put("sign_file", filePathToContentsMap(def));
		} else {
			if (hasSignPath) {
				out.put("sign_file", filePathToContentsMap(String.valueOf(out.get("sign_file")).trim()));
			}
			if (hasDelegatePath) {
				out.put("delegate_file", filePathToContentsMap(String.valueOf(out.get("delegate_file")).trim()));
			}
		}
		return out;
	}

	private Map<String, String> filePathToContentsMap(String pathStr) {
		try {
			byte[] bytes = readAllBytes(pathStr);
			String b64 = Base64.getEncoder().encodeToString(bytes);
			String suffix = suffixFromPath(pathStr);
			Map<String, String> m = new LinkedHashMap<>();
			m.put("fileContents", b64);
			m.put("fileSuffix", suffix);
			return m;
		} catch (IOException e) {
			throw new ResourceException("读取签名图片失败: " + e.getMessage());
		}
	}

	private byte[] readAllBytes(String pathStr) throws IOException {
		if (pathStr.startsWith("classpath:")) {
			Resource r = resourceLoader.getResource(pathStr);
			if (!r.exists()) {
				throw new IOException("资源不存在: " + pathStr);
			}
			try (InputStream in = r.getInputStream()) {
				return in.readAllBytes();
			}
		}
		return Files.readAllBytes(Path.of(pathStr));
	}

	private static String suffixFromPath(String pathStr) {
		String name;
		if (pathStr.startsWith("classpath:")) {
			int slash = Math.max(pathStr.lastIndexOf('/'), pathStr.lastIndexOf('\\'));
			name = slash >= 0 ? pathStr.substring(slash + 1) : pathStr;
		} else {
			name = Paths.get(pathStr).getFileName().toString();
		}
		int dot = name.lastIndexOf('.');
		return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1) : "";
	}

	private static boolean pathStringPresent(Object raw) {
		if (!(raw instanceof String s)) {
			return false;
		}
		return !s.trim().isEmpty();
	}
}
