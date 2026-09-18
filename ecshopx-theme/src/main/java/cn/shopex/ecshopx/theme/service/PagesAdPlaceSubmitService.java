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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesAdPlace;
import cn.shopex.ecshopx.theme.mapper.PagesAdPlaceMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class PagesAdPlaceSubmitService {

	private final PagesAdPlaceMapper pagesAdPlaceMapper;

	public PagesAdPlaceSubmitService(PagesAdPlaceMapper pagesAdPlaceMapper) {
		this.pagesAdPlaceMapper = pagesAdPlaceMapper;
	}

	public void submit(long companyId, long adPlaceId, Long sourceIdFilter) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<PagesAdPlace> wrapper = new LambdaUpdateWrapper<>();
		wrapper.eq(PagesAdPlace::getCompanyId, companyId).eq(PagesAdPlace::getId, adPlaceId);
		if (sourceIdFilter == null) {
			wrapper.isNull(PagesAdPlace::getSourceId);
		} else {
			wrapper.eq(PagesAdPlace::getSourceId, sourceIdFilter);
		}
		wrapper.set(PagesAdPlace::getAuditStatus, "processing").set(PagesAdPlace::getUpdated, now);

		int rows = pagesAdPlaceMapper.update(null, wrapper);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
