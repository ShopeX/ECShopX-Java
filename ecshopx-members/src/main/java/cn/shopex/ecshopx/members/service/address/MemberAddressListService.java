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

package cn.shopex.ecshopx.members.service.address;

import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberAddressListService {

	private final MembersAddressMapper membersAddressMapper;

	private final MemberAddressLatLngNonNumericEnqueueService latLngNonNumericEnqueueService;

	public MemberAddressListService(
			MembersAddressMapper membersAddressMapper,
			MemberAddressLatLngNonNumericEnqueueService latLngNonNumericEnqueueService) {
		this.membersAddressMapper = membersAddressMapper;
		this.latLngNonNumericEnqueueService = latLngNonNumericEnqueueService;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getAddressList(
			long companyId,
			Long filterUserId,
			Long addressIdOrNull,
			String cityContainsOrNull,
			int page,
			int pageSize) {
		LambdaQueryWrapper<MembersAddress> countW =
				buildBaseWrapper(companyId, filterUserId, addressIdOrNull, cityContainsOrNull);
		long total = membersAddressMapper.selectCount(countW);

		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0L) {
			long offset = (long) pageSize * (long) (page - 1);
			LambdaQueryWrapper<MembersAddress> listW =
					buildBaseWrapper(companyId, filterUserId, addressIdOrNull, cityContainsOrNull);
			listW.last("LIMIT " + offset + "," + pageSize);
			List<MembersAddress> rows = membersAddressMapper.selectList(listW);
			for (MembersAddress entity : rows) {
				Map<String, Object> row = MemberAddressResponseMaps.toRow(entity);
				long uid = entity.getUserId() != null ? entity.getUserId() : 0L;
				latLngNonNumericEnqueueService.enqueueIfNeeded(companyId, uid, row);
				list.add(row);
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private static LambdaQueryWrapper<MembersAddress> buildBaseWrapper(
			long companyId,
			Long filterUserId,
			Long addressIdOrNull,
			String cityContainsOrNull) {
		LambdaQueryWrapper<MembersAddress> w = new LambdaQueryWrapper<>();
		w.eq(MembersAddress::getCompanyId, companyId);
		if (filterUserId == null) {
			w.isNull(MembersAddress::getUserId);
		} else {
			w.eq(MembersAddress::getUserId, filterUserId);
		}
		if (addressIdOrNull != null) {
			w.eq(MembersAddress::getAddressId, addressIdOrNull);
		}
		if (StringUtils.hasText(cityContainsOrNull)) {
			w.like(MembersAddress::getCity, cityContainsOrNull);
		}
		w.orderByDesc(MembersAddress::getIsDef).orderByDesc(MembersAddress::getUpdated);
		return w;
	}
}
