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

package cn.shopex.ecshopx.orders.service.invoiceseller;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.orders.domain.InvoiceSeller;
import cn.shopex.ecshopx.orders.repository.InvoiceSellerRepository;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceSellerAdminUpdateService {

	private static final List<String> WHITELIST = List.of(
			"company_id",
			"seller_name",
			"payee",
			"reviewer",
			"seller_company_name",
			"seller_tax_no",
			"seller_bank_name",
			"seller_bank_account",
			"seller_phone",
			"seller_address",
			"created_at");

	private static final ZoneId CREATED_AT_ZONE = ZoneId.systemDefault();

	private final InvoiceSellerRepository invoiceSellerRepository;

	public InvoiceSellerAdminUpdateService(InvoiceSellerRepository invoiceSellerRepository) {
		this.invoiceSellerRepository = invoiceSellerRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateSeller(String idRaw, Map<String, Object> mergedIn) {
		Map<String, Object> merged = mergedIn == null ? new LinkedHashMap<>() : new LinkedHashMap<>(mergedIn);

		String trimmed = idRaw == null ? "" : idRaw.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
		if (id <= 0) {
			throw new ResourceException("未查询到更新数据");
		}

		InvoiceSeller entity = invoiceSellerRepository
				.findById(id)
				.orElseThrow(() -> new ResourceException("未查询到更新数据"));

		boolean anyWhitelistKeyPresent = false;
		boolean anyDirty = false;
		for (String col : WHITELIST) {
			if (!merged.containsKey(col)) {
				continue;
			}
			anyWhitelistKeyPresent = true;
			Object target = targetValueForColumn(col, merged.get(col));
			if (!Objects.equals(target, currentValueForColumn(col, entity))) {
				anyDirty = true;
				break;
			}
		}

		if (!anyWhitelistKeyPresent || !anyDirty) {
			return invoiceSellerRepository.toRowMap(entity);
		}

		for (String col : WHITELIST) {
			if (!merged.containsKey(col)) {
				continue;
			}
			Object target = targetValueForColumn(col, merged.get(col));
			if (Objects.equals(target, currentValueForColumn(col, entity))) {
				continue;
			}
			applyColumn(entity, col, merged.get(col));
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setUpdatedAt(now);

		return invoiceSellerRepository.updateEntityAndReturnRow(entity);
	}

	private static Object currentValueForColumn(String col, InvoiceSeller entity) {
		return switch (col) {
			case "company_id" -> entity.getCompanyId();
			case "seller_name" -> entity.getSellerName();
			case "payee" -> entity.getPayee();
			case "reviewer" -> entity.getReviewer();
			case "seller_company_name" -> entity.getSellerCompanyName();
			case "seller_tax_no" -> entity.getSellerTaxNo();
			case "seller_bank_name" -> entity.getSellerBankName();
			case "seller_bank_account" -> entity.getSellerBankAccount();
			case "seller_phone" -> entity.getSellerPhone();
			case "seller_address" -> entity.getSellerAddress();
			case "created_at" -> entity.getCreatedAt();
			default -> null;
		};
	}

	/** 与写库时 setter 语义一致，供脏比较使用。 */
	private static Object targetValueForColumn(String col, Object raw) {
		return switch (col) {
			case "company_id" -> parseCompanyIdLoose(raw);
			case "created_at" -> parseCreatedAtLoose(raw);
			case "seller_name",
					"payee",
					"reviewer",
					"seller_company_name",
					"seller_tax_no",
					"seller_bank_name",
					"seller_bank_account",
					"seller_phone",
					"seller_address" -> raw == null ? null : String.valueOf(raw);
			default -> null;
		};
	}

	private static void applyColumn(InvoiceSeller entity, String col, Object raw) {
		switch (col) {
			case "company_id" -> entity.setCompanyId(parseCompanyIdLoose(raw));
			case "created_at" -> entity.setCreatedAt(parseCreatedAtLoose(raw));
			case "seller_name" -> entity.setSellerName(raw == null ? null : String.valueOf(raw));
			case "payee" -> entity.setPayee(raw == null ? null : String.valueOf(raw));
			case "reviewer" -> entity.setReviewer(raw == null ? null : String.valueOf(raw));
			case "seller_company_name" ->
				entity.setSellerCompanyName(raw == null ? null : String.valueOf(raw));
			case "seller_tax_no" -> entity.setSellerTaxNo(raw == null ? null : String.valueOf(raw));
			case "seller_bank_name" -> entity.setSellerBankName(raw == null ? null : String.valueOf(raw));
			case "seller_bank_account" ->
				entity.setSellerBankAccount(raw == null ? null : String.valueOf(raw));
			case "seller_phone" -> entity.setSellerPhone(raw == null ? null : String.valueOf(raw));
			case "seller_address" -> entity.setSellerAddress(raw == null ? null : String.valueOf(raw));
			default -> {
				// 白名单外已在循环中排除
			}
		}
	}

	private static Long parseCompanyIdLoose(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		return Long.parseLong(s);
	}

	private static Integer parseCreatedAtLoose(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return Math.toIntExact(n.longValue());
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Math.toIntExact(Long.parseLong(s));
		} catch (NumberFormatException ignored) {
			Long epoch = DateExpressionParser.parseToEpochSecond(s, CREATED_AT_ZONE);
			if (epoch == null) {
				throw new IllegalStateException("无法解析创建时间");
			}
			return Math.toIntExact(epoch);
		}
	}
}
