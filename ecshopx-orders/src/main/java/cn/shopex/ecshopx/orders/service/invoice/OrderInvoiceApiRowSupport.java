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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.util.StringUtils;

public final class OrderInvoiceApiRowSupport {

	public static final List<String> COLS_ORDER = List.of(
			"id",
			"invoice_apply_bn",
			"user_id",
			"company_id",
			"regionauth_id",
			"order_id",
			"invoice_type",
			"company_title",
			"company_tax_number",
			"company_address",
			"company_telephone",
			"bank_name",
			"bank_account",
			"email",
			"mobile",
			"invoice_status",
			"try_times",
			"invoice_amount",
			"invoice_file_url",
			"invoice_file_url_red",
			"invoice_method",
			"invoice_source",
			"remark",
			"is_oms",
			"create_time",
			"update_time",
			"invoice_type_code",
			"end_time",
			"close_aftersales_time",
			"query_content",
			"red_content",
			"serial_no",
			"red_serial_no",
			"red_apply_bn",
			"order_shop_id",
			"user_card_code");

	private OrderInvoiceApiRowSupport() {
	}

	public static LinkedHashMap<String, Object> toColumnNamesData(OrderInvoice e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (e == null) {
			for (String col : COLS_ORDER) {
				m.put(col, null);
			}
			return m;
		}
		for (String col : COLS_ORDER) {
			m.put(col, columnValue(e, col));
		}
		return m;
	}

	/** Unset or blank longtext JSON columns are omitted from JSON payload as null. */
	public static Object longtextJsonColumnForApi(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		return raw;
	}

	public static Object columnValue(OrderInvoice e, String col) {
		return switch (col) {
			case "id" -> e.getId();
			case "invoice_apply_bn" -> e.getInvoiceApplyBn();
			case "user_id" -> e.getUserId();
			case "company_id" -> e.getCompanyId();
			case "regionauth_id" -> e.getRegionauthId();
			case "order_id" -> e.getOrderId();
			case "invoice_type" -> e.getInvoiceType();
			case "company_title" -> e.getCompanyTitle();
			case "company_tax_number" -> e.getCompanyTaxNumber();
			case "company_address" -> e.getCompanyAddress();
			case "company_telephone" -> e.getCompanyTelephone();
			case "bank_name" -> e.getBankName();
			case "bank_account" -> e.getBankAccount();
			case "email" -> e.getEmail();
			case "mobile" -> e.getMobile();
			case "invoice_status" -> e.getInvoiceStatus();
			case "try_times" -> e.getTryTimes();
			case "invoice_amount" -> e.getInvoiceAmount();
			case "invoice_file_url" -> e.getInvoiceFileUrl();
			case "invoice_file_url_red" -> e.getInvoiceFileUrlRed();
			case "invoice_method" -> e.getInvoiceMethod();
			case "invoice_source" -> e.getInvoiceSource();
			case "remark" -> e.getRemark();
			case "is_oms" -> e.getIsOms();
			case "create_time" -> e.getCreateTime();
			case "update_time" -> e.getUpdateTime();
			case "invoice_type_code" -> e.getInvoiceTypeCode();
			case "end_time" -> e.getEndTime();
			case "close_aftersales_time" -> e.getCloseAftersalesTime();
			case "query_content" -> longtextJsonColumnForApi(e.getQueryContent());
			case "red_content" -> longtextJsonColumnForApi(e.getRedContent());
			case "serial_no" -> e.getSerialNo();
			case "red_serial_no" -> e.getRedSerialNo();
			case "red_apply_bn" -> e.getRedApplyBn();
			case "order_shop_id" -> e.getOrderShopId();
			case "user_card_code" -> e.getUserCardCode();
			default -> null;
		};
	}
}
