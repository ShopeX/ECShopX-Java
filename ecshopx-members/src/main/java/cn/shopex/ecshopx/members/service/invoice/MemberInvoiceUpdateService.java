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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersInvoices;
import cn.shopex.ecshopx.members.mapper.MembersInvoicesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MemberInvoiceUpdateService {

	private final MembersInvoicesMapper membersInvoicesMapper;
	private final TransactionTemplate transactionTemplate;

	public MemberInvoiceUpdateService(
			MembersInvoicesMapper membersInvoicesMapper, TransactionTemplate transactionTemplate) {
		this.membersInvoicesMapper = membersInvoicesMapper;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> updateInvoice(
			long companyId,
			long userId,
			long invoicesId,
			Map<String, Object> body,
			Map<String, Object> patchBase) {
		return transactionTemplate.execute(
				status -> {
					boolean shouldClearOtherDefaults = isDefIsset(body) && looseEqualsOne(body.get("is_def"));
					if (shouldClearOtherDefaults) {
						MembersInvoices patchZero = new MembersInvoices();
						patchZero.setIsDef(false);
						membersInvoicesMapper.update(
								patchZero,
								new LambdaQueryWrapper<MembersInvoices>()
										.eq(MembersInvoices::getCompanyId, companyId)
										.eq(MembersInvoices::getUserId, userId));
					}

					MembersInvoices existing =
							membersInvoicesMapper.selectOne(
									new LambdaQueryWrapper<MembersInvoices>()
											.eq(MembersInvoices::getInvoicesId, invoicesId)
											.eq(MembersInvoices::getUserId, userId)
											.eq(MembersInvoices::getCompanyId, companyId)
											.last("LIMIT 1"));
					if (existing == null) {
						throw new ResourceException("未查询到更新数据");
					}

					Map<String, Object> patch = new LinkedHashMap<>(patchBase);
					if (isDefIsset(body)) {
						patch.put(
								"is_def",
								normalizeNonNullIsDefLikeCreate(body.get("is_def")) == 1);
					}

					long now = Instant.now().getEpochSecond();
					LambdaUpdateWrapper<MembersInvoices> uw =
							new LambdaUpdateWrapper<MembersInvoices>()
									.eq(MembersInvoices::getInvoicesId, invoicesId)
									.eq(MembersInvoices::getUserId, userId)
									.eq(MembersInvoices::getCompanyId, companyId)
									.set(MembersInvoices::getUpdated, now);

					if (patch.containsKey("invoices_type")) {
						uw.set(MembersInvoices::getInvoicesType, (String) patch.get("invoices_type"));
					}
					if (patch.containsKey("name")) {
						uw.set(MembersInvoices::getName, (String) patch.get("name"));
					}
					if (patch.containsKey("telephone")) {
						uw.set(MembersInvoices::getTelephone, (String) patch.get("telephone"));
					}
					if (patch.containsKey("tax_number")) {
						uw.set(MembersInvoices::getTaxNumber, (String) patch.get("tax_number"));
					}
					if (patch.containsKey("business_address")) {
						uw.set(MembersInvoices::getBusinessAddress, (String) patch.get("business_address"));
					}
					if (patch.containsKey("bank")) {
						uw.set(MembersInvoices::getBank, (String) patch.get("bank"));
					}
					if (patch.containsKey("bank_account")) {
						uw.set(MembersInvoices::getBankAccount, (String) patch.get("bank_account"));
					}
					if (patch.containsKey("is_def")) {
						uw.set(MembersInvoices::getIsDef, (Boolean) patch.get("is_def"));
					}

					int n = membersInvoicesMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					MembersInvoices refreshed = membersInvoicesMapper.selectById(invoicesId);
					if (refreshed == null) {
						throw new ResourceException("未查询到更新数据");
					}
					return MemberInvoiceResponseMaps.toRow(refreshed);
				});
	}

	private static boolean isDefIsset(Map<String, Object> body) {
		return body.containsKey("is_def") && body.get("is_def") != null;
	}

	private static boolean looseEqualsOne(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() == 1.0d;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if ("1".equals(t)) {
				return true;
			}
			try {
				return Double.parseDouble(t) == 1.0d;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static int normalizeNonNullIsDefLikeCreate(Object raw) {
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (d == 0.0d) {
				return 0;
			}
			if (d == 1.0d) {
				return 1;
			}
			throw new ResourceException("默认地址开启错误");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if ("0".equals(t)) {
				return 0;
			}
			if ("1".equals(t)) {
				return 1;
			}
			throw new ResourceException("默认地址开启错误");
		}
		throw new ResourceException("默认地址开启错误");
	}
}
