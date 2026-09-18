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

package cn.shopex.ecshopx.espier.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiImageSavePort.OpenapiImageSavePayload;
import cn.shopex.ecshopx.espier.domain.UploadImages;
import cn.shopex.ecshopx.espier.mapper.UploadImagesMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1ImageSaveService {

	private final UploadImagesMapper uploadImagesMapper;

	public OpenapiThirdApiV1ImageSaveService(UploadImagesMapper uploadImagesMapper) {
		this.uploadImagesMapper = uploadImagesMapper;
	}

	public Map<String, Object> executeOpenapiImageSave(long companyId, OpenapiImageSavePayload payload) {
		UploadImages entity = new UploadImages();
		entity.setCompanyId(companyId);
		entity.setStorage(payload.storage());
		entity.setImageName(payload.imageName());
		entity.setImageUrl(payload.imageUrl());
		entity.setImageCatId(payload.imageCatId());

		if (payload.brief() != null) {
			entity.setBrief(payload.brief());
		}
		if (payload.imageType() != null) {
			entity.setImageType(payload.imageType());
		}
		if (payload.imageFullUrl() != null) {
			entity.setImageFullUrl(payload.imageFullUrl());
		}

		entity.setSupplierId(0L);
		entity.setMerchantId(0L);

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);

		uploadImagesMapper.insert(entity);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("image_id", entity.getImageId());
		return out;
	}
}
