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
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MemberInvoiceCreateService {

	private final MembersInvoicesMapper membersInvoicesMapper;
	private final TransactionTemplate transactionTemplate;

	public MemberInvoiceCreateService(
			MembersInvoicesMapper membersInvoicesMapper, TransactionTemplate transactionTemplate) {
		this.membersInvoicesMapper = membersInvoicesMapper;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> createInvoice(
			long companyId,
			long userId,
			String invoicesType,
			String name,
			String telephoneOrNull,
			String taxNumberOrNull,
			String businessAddressOrNull,
			String bankOrNull,
			String bankAccountOrNull,
			int isDefNormalized01) {
		long cnt =
				membersInvoicesMapper.selectCount(
						new LambdaQueryWrapper<MembersInvoices>()
								.eq(MembersInvoices::getCompanyId, companyId)
								.eq(MembersInvoices::getUserId, userId));

		final long cntFinal = cnt;
		return transactionTemplate.execute(
				status -> {
					int effectiveIsDef = isDefNormalized01;
					if (cntFinal == 0L) {
						effectiveIsDef = 1;
					}
					if (cntFinal > 0L && effectiveIsDef == 1) {
						MembersInvoices patch = new MembersInvoices();
						patch.setIsDef(false);
						membersInvoicesMapper.update(
								patch,
								new LambdaQueryWrapper<MembersInvoices>()
										.eq(MembersInvoices::getCompanyId, companyId)
										.eq(MembersInvoices::getUserId, userId));
					}
					MembersInvoices row = new MembersInvoices();
					row.setCompanyId(companyId);
					row.setUserId(userId);
					row.setInvoicesType(invoicesType);
					row.setName(name);
					row.setTelephone(telephoneOrNull);
					row.setTaxNumber(taxNumberOrNull);
					row.setBusinessAddress(businessAddressOrNull);
					row.setBank(bankOrNull);
					row.setBankAccount(bankAccountOrNull);
					row.setIsDef(effectiveIsDef == 1);
					long now = Instant.now().getEpochSecond();
					row.setCreated(now);
					row.setUpdated(now);
					membersInvoicesMapper.insert(row);
					return MemberInvoiceResponseMaps.toRow(row);
				});
	}
}
