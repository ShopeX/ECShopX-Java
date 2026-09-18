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

import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.espier.domain.UploadImages;
import cn.shopex.ecshopx.espier.mapper.UploadImagesMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1ImageDeleteService {

	private final UploadImagesMapper uploadImagesMapper;

	public OpenapiThirdApiV1ImageDeleteService(UploadImagesMapper uploadImagesMapper) {
		this.uploadImagesMapper = uploadImagesMapper;
	}

	public void executeOpenapiImageDelete(long companyId, List<String> imageIds) {
		LambdaUpdateWrapper<UploadImages> uw = new LambdaUpdateWrapper<>();
		uw.eq(UploadImages::getCompanyId, companyId)
				.in(UploadImages::getImageId, imageIds)
				.set(UploadImages::getDisabled, Boolean.TRUE);

		int rows = uploadImagesMapper.update(null, uw);
		if (rows == 0) {
			throw new OpenapiLegacyZeroCodeFailException("未查询到更新数据");
		}
	}
}
