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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.members.mapper.AdminMemberBatchOperatingMapper;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberBatchOperatingUserIdsPageService {

	private final AdminMemberBatchOperatingMapper adminMemberBatchOperatingMapper;

	public AdminMemberBatchOperatingUserIdsPageService(AdminMemberBatchOperatingMapper adminMemberBatchOperatingMapper) {
		this.adminMemberBatchOperatingMapper = adminMemberBatchOperatingMapper;
	}

	public List<Long> listUserIdsPage(long companyId, Map<String, Object> filter, int page, int pageSize) {
		AdminMemberBatchOperatingMemberQueryFilter f =
				AdminMemberBatchOperatingMemberQueryFilterSupport.unwrap(filter);
		if (f.getCompanyId() != companyId) {
			f.setCompanyId(companyId);
		}
		long offset = (long) (page - 1) * pageSize;
		return adminMemberBatchOperatingMapper.listUserIdsPage(f, offset, pageSize);
	}
}
