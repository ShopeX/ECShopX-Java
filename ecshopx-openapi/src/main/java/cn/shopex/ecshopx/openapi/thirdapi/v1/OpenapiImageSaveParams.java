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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiImageSavePort.OpenapiImageSavePayload;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OpenapiImageSaveParams {

	private static final String MSG_IMAGE_NAME = "图片名称必填,不能超过50个字符";
	private static final String MSG_STORAGE = "存储类型必填";
	private static final String MSG_IMAGE_TYPE = "图片分类长度不能超过20个字符";
	private static final String MSG_IMAGE_URL = "图片链接必填";

	private OpenapiImageSaveParams() {}

	public record BuildResult(OpenapiImageSavePayload payload) {}

	public static BuildResult build(
			String imageNameParam,
			String storageParam,
			String imageUrlParam,
			String imageCatIdParam,
			String briefParam,
			String imageTypeParam,
			String imageFullUrlParam,
			Map<String, Object> body) {
		List<String> errors = new ArrayList<>();

		String imageNameRaw = OpenapiRequestParams.originalString(imageNameParam, body, "image_name");
		if (imageNameRaw == null || imageNameRaw.isBlank()) {
			errors.add(MSG_IMAGE_NAME);
		} else if (imageNameRaw.length() > 100) {
			errors.add(MSG_IMAGE_NAME);
		}

		String storage = OpenapiRequestParams.mergeString(storageParam, body, "storage");
		if (storage == null) {
			errors.add(MSG_STORAGE);
		}

		if (OpenapiMemberQueryParams.isParamPresent(imageTypeParam, body, "image_type")) {
			String imageTypeRaw = OpenapiRequestParams.mergeString(imageTypeParam, body, "image_type");
			if (imageTypeRaw != null && imageTypeRaw.length() > 20) {
				errors.add(MSG_IMAGE_TYPE);
			}
		}

		String imageUrl = OpenapiRequestParams.mergeString(imageUrlParam, body, "image_url");
		if (imageUrl == null) {
			errors.add(MSG_IMAGE_URL);
		}

		if (!errors.isEmpty()) {
			StringBuilder errmsg = new StringBuilder();
			for (String msg : errors) {
				errmsg.append(msg).append('，');
			}
			throw new ResourceException(errmsg.toString());
		}

		long imageCatId = resolveImageCatId(imageCatIdParam, body);
		String brief = OpenapiRequestParams.mergeString(briefParam, body, "brief");
		String imageType = OpenapiRequestParams.mergeString(imageTypeParam, body, "image_type");
		String imageFullUrl = OpenapiRequestParams.mergeString(imageFullUrlParam, body, "image_full_url");

		OpenapiImageSavePayload payload =
				new OpenapiImageSavePayload(imageNameRaw, storage, imageUrl, imageCatId, brief, imageType, imageFullUrl);
		return new BuildResult(payload);
	}

	private static long resolveImageCatId(String imageCatIdParam, Map<String, Object> body) {
		if (!OpenapiMemberQueryParams.isParamPresent(imageCatIdParam, body, "image_cat_id")) {
			return 0L;
		}
		String raw = OpenapiRequestParams.originalString(imageCatIdParam, body, "image_cat_id");
		if (raw == null || raw.isBlank() || "0".equals(raw.trim())) {
			return 0L;
		}
		try {
			return new BigDecimal(raw.trim()).longValueExact();
		} catch (NumberFormatException | ArithmeticException e) {
			return 0L;
		}
	}
}
