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

package cn.shopex.ecshopx.espier.service.upload;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IntroHtmlDataImageUploadService {

	private static final Pattern DATA_IMAGE = Pattern.compile("data:image/([^;]+);base64,([^\"'\\s>]+)", Pattern.CASE_INSENSITIVE);

	private final EspierImageUploadClient espierImageUploadClient;

	public IntroHtmlDataImageUploadService(EspierImageUploadClient espierImageUploadClient) {
		this.espierImageUploadClient = espierImageUploadClient;
	}

	public String replaceDataImageUrlsInIntro(String introHtml, long companyId) {
		if (!StringUtils.hasText(introHtml)) {
			return introHtml;
		}
		Matcher m = DATA_IMAGE.matcher(introHtml);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String mimePart = m.group(1);
			String b64 = m.group(2);
			String mime = "image/" + mimePart;
			byte[] bytes;
			try {
				bytes = Base64.getDecoder().decode(b64.getBytes(StandardCharsets.US_ASCII));
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("详情中的内嵌图片 Base64 格式无效");
			}
			String url;
			try {
				url = espierImageUploadClient.uploadDecodedImage(companyId, bytes, mime);
			} catch (ResourceException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new ResourceException("图片上传失败：" + e.getMessage());
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(url));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
