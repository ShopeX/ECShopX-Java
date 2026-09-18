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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.service.oms.OmsSettingAdminService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoicePushOmsJobService {

	private static final Logger log = LoggerFactory.getLogger(InvoicePushOmsJobService.class);

	private final OmsSettingAdminService omsSettingAdminService;
	private final OrderInvoiceDetailService orderInvoiceDetailService;

	public InvoicePushOmsJobService(
			OmsSettingAdminService omsSettingAdminService, OrderInvoiceDetailService orderInvoiceDetailService) {
		this.omsSettingAdminService = omsSettingAdminService;
		this.orderInvoiceDetailService = orderInvoiceDetailService;
	}

	public void execute(long invoiceId, long companyId) {
		if (invoiceId <= 0 || companyId <= 0) {
			log.warn("Invoice push OMS skipped: invalid ids invoiceId={} companyId={}", invoiceId, companyId);
			return;
		}
		Object omsRaw;
		try {
			omsRaw = omsSettingAdminService.getOmsSetting(companyId);
		} catch (Exception e) {
			log.error("Invoice push OMS: cannot load OMS setting companyId={} invoiceId={}", companyId, invoiceId, e);
			return;
		}
		if (!omsConnectionParamsPresent(omsRaw)) {
			log.error(
					"Invoice push OMS skipped: OMS host/node/secret not configured companyId={} invoiceId={}",
					companyId,
					invoiceId);
			return;
		}

		Map<String, Object> detail;
		try {
			detail = orderInvoiceDetailService.getInvoiceDetail(companyId, String.valueOf(invoiceId));
		} catch (ResourceException e) {
			log.warn(
					"Invoice push OMS skipped: invoice detail unavailable companyId={} invoiceId={} reason={}",
					companyId,
					invoiceId,
					e.getMessage());
			return;
		}
		Object itemsObj = detail.get("invoice_items");
		if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
			log.warn("Invoice push OMS skipped: empty line items invoiceId={}", invoiceId);
			return;
		}

		log.info(
				"Invoice push OMS: prepared payload invoiceId={} companyId={} lineCount={} (HTTP submit not wired)",
				invoiceId,
				companyId,
				items.size());
	}

	private static boolean omsConnectionParamsPresent(Object raw) {
		if (!(raw instanceof Map<?, ?> map)) {
			return false;
		}
		String host = stringVal(map.get("host"));
		String node = firstNonBlank(stringVal(map.get("node")), stringVal(map.get("node_id")));
		String secret = stringVal(map.get("secret"));
		return StringUtils.hasText(host) && StringUtils.hasText(node) && StringUtils.hasText(secret);
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		return b == null ? "" : b;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
