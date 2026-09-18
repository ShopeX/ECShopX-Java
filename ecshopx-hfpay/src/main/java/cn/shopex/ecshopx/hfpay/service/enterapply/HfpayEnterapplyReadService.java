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

package cn.shopex.ecshopx.hfpay.service.enterapply;

import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.dto.HfpayEnterapplyApplyListFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayEnterapplyReadService {

	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;
	private final ObjectMapper objectMapper;
	private final String importImagePublicBase;

	public HfpayEnterapplyReadService(
			HfpayEnterapplyMapper hfpayEnterapplyMapper,
			ObjectMapper objectMapper,
			@Value("${ecshopx.filesystem.import-image-public-base:}") String importImagePublicBase) {
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
		this.objectMapper = objectMapper;
		this.importImagePublicBase = importImagePublicBase;
	}

	public Map<String, Object> getEnterapply(long companyId, long distributorId) {
		HfpayEnterapply row = hfpayEnterapplyMapper.selectOne(new LambdaQueryWrapper<HfpayEnterapply>()
				.eq(HfpayEnterapply::getCompanyId, companyId)
				.eq(HfpayEnterapply::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		return HfpayEnterapplyRowConverter.enterapplyDetail(row, importImagePublicBase, objectMapper);
	}

	public Map<String, Object> getEnterapplyByCompanyAndUser(long companyId, long userId) {
		HfpayEnterapply row = hfpayEnterapplyMapper.selectOne(new LambdaQueryWrapper<HfpayEnterapply>()
				.eq(HfpayEnterapply::getCompanyId, companyId)
				.eq(HfpayEnterapply::getUserId, userId)
				.orderByDesc(HfpayEnterapply::getHfpayEnterapplyId)
				.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		return HfpayEnterapplyRowConverter.enterapplyDetail(row, importImagePublicBase, objectMapper);
	}

	public Map<String, Object> getEnterapplyForApply(long companyId, Long distributorId, String applyType) {
		LambdaQueryWrapper<HfpayEnterapply> w = new LambdaQueryWrapper<HfpayEnterapply>()
				.eq(HfpayEnterapply::getCompanyId, companyId);
		if (distributorId == null) {
			w.isNull(HfpayEnterapply::getDistributorId);
		} else {
			w.eq(HfpayEnterapply::getDistributorId, distributorId);
		}
		w.eq(HfpayEnterapply::getApplyType, applyType).last("LIMIT 1");
		HfpayEnterapply row = hfpayEnterapplyMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		return HfpayEnterapplyRowConverter.enterapplyDetail(row, importImagePublicBase, objectMapper);
	}

	public Map<String, Object> getApplyList(
			long companyId,
			int page,
			int pageSize,
			String name,
			String province,
			String city,
			String area,
			Long distributorId) {
		HfpayEnterapplyApplyListFilter filter = new HfpayEnterapplyApplyListFilter();
		filter.setCompanyId(companyId);
		filter.setApplyTypes(List.of("1", "2", "3"));
		if (StringUtils.hasText(name)) {
			filter.setNameContains(name.trim());
		}
		if (StringUtils.hasText(province)) {
			filter.setProvince(province.trim());
		}
		if (StringUtils.hasText(city)) {
			filter.setCity(city.trim());
		}
		if (StringUtils.hasText(area)) {
			filter.setArea(area.trim());
		}
		filter.setDistributorId(distributorId);
		if (pageSize > 0) {
			filter.setLimit(pageSize);
			filter.setOffset((long) (Math.max(page, 1) - 1) * pageSize);
		} else {
			filter.setLimit(null);
			filter.setOffset(null);
		}

		long totalRaw = hfpayEnterapplyMapper.countEnterapplyApplyJoinList(filter);
		int totalCount = (int) totalRaw;
		List<Map<String, Object>> list;
		if (totalRaw == 0) {
			list = Collections.emptyList();
		} else {
			list = hfpayEnterapplyMapper.selectEnterapplyApplyJoinList(filter);
			for (int i = 0; i < list.size(); i++) {
				Map<String, Object> row = list.get(i);
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>(row);
				// Map rows omit keys for NULL SQL columns unless callSettersOnNulls; normalize so `name` always appears in JSON.
				copy.put("distributor_id", row.get("distributor_id"));
				copy.put("name", row.get("name"));
				copy.put("status_msg", statusToMessage(copy.get("status")));
				list.set(i, copy);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", list);
		return result;
	}

	private static String statusToMessage(Object statusRaw) {
		if (statusRaw == null) {
			return "";
		}
		String s = String.valueOf(statusRaw).trim();
		return switch (s) {
			case "1" -> "未提交开户信息";
			case "2" -> "审核中";
			case "3" -> "审核成功";
			case "4" -> "审核失败";
			default -> "";
		};
	}
}
