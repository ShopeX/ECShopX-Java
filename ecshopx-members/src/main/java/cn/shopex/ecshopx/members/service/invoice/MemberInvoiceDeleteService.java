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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberInvoiceDeleteService {

	private final MembersInvoicesMapper membersInvoicesMapper;

	public MemberInvoiceDeleteService(MembersInvoicesMapper membersInvoicesMapper) {
		this.membersInvoicesMapper = membersInvoicesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteInvoice(long companyId, long userId, long invoicesId) {
		membersInvoicesMapper.delete(
				new LambdaQueryWrapper<MembersInvoices>()
						.eq(MembersInvoices::getInvoicesId, invoicesId)
						.eq(MembersInvoices::getCompanyId, companyId)
						.eq(MembersInvoices::getUserId, userId));
	}
}
