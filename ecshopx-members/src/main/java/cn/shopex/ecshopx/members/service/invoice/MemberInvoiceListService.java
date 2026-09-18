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

package cn.shopex.ecshopx.members.service.invoice;

import cn.shopex.ecshopx.members.domain.MembersInvoices;
import cn.shopex.ecshopx.members.mapper.MembersInvoicesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberInvoiceListService {

	private final MembersInvoicesMapper membersInvoicesMapper;

	public MemberInvoiceListService(MembersInvoicesMapper membersInvoicesMapper) {
		this.membersInvoicesMapper = membersInvoicesMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getInvoiceList(long companyId, long userId, int page, int pageSize) {
		LambdaQueryWrapper<MembersInvoices> base = buildFilterWrapper(companyId, userId);
		long total = membersInvoicesMapper.selectCount(base);

		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0L) {
			LambdaQueryWrapper<MembersInvoices> listWrapper = buildFilterWrapper(companyId, userId);
			listWrapper
					.orderByDesc(MembersInvoices::getIsDef)
					.orderByDesc(MembersInvoices::getCreated);
			if (pageSize > 0) {
				long offset = (long) pageSize * (long) (page - 1);
				listWrapper.last("LIMIT " + offset + "," + pageSize);
			}
			List<MembersInvoices> rows = membersInvoicesMapper.selectList(listWrapper);
			for (MembersInvoices entity : rows) {
				list.add(MemberInvoiceResponseMaps.toRow(entity));
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private static LambdaQueryWrapper<MembersInvoices> buildFilterWrapper(long companyId, long userId) {
		LambdaQueryWrapper<MembersInvoices> w = new LambdaQueryWrapper<>();
		w.eq(MembersInvoices::getCompanyId, companyId).eq(MembersInvoices::getUserId, userId);
		return w;
	}
}
