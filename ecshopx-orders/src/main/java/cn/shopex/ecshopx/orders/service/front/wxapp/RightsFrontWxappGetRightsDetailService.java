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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.orders.service.reservation.RightsTimesCardRowFactory;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RightsFrontWxappGetRightsDetailService {

	private final RightsMapper rightsMapper;
	private final RightsTimesCardRowFactory rightsTimesCardRowFactory;

	public RightsFrontWxappGetRightsDetailService(
			RightsMapper rightsMapper, RightsTimesCardRowFactory rightsTimesCardRowFactory) {
		this.rightsMapper = rightsMapper;
		this.rightsTimesCardRowFactory = rightsTimesCardRowFactory;
	}

	public Map<String, Object> getRightsDetail(String rightsIdRaw, Map<String, Object> authClaims) {
		if (rightsIdRaw == null || rightsIdRaw.isBlank()) {
			throw new BadRequestException("获取权益详情出错", 422);
		}
		String trimmed = rightsIdRaw.trim();
		long rightsId;
		try {
			rightsId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取权益详情出错", 422);
		}
		if (rightsId < 1L) {
			throw new BadRequestException("获取权益详情出错", 422);
		}

		Rights r = rightsMapper.selectById(rightsId);
		if (r == null) {
			throw new ResourceException("rights_id=%d的权益不存在".formatted(rightsId));
		}

		int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> result = rightsTimesCardRowFactory.toWxappRightsDetailRow(r, nowEpochSec);
		result.put("server_time", (long) nowEpochSec);

		long authCompanyId = longVal(authClaims.get("company_id"));
		long rowCompanyId = r.getCompanyId() != null ? r.getCompanyId() : 0L;
		if (authCompanyId != rowCompanyId) {
			throw new ResourceException("获取权益信息有误，请确认权益 ID");
		}

		return result;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
