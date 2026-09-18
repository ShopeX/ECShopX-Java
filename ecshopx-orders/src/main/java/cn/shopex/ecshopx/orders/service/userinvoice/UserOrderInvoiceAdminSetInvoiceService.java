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

package cn.shopex.ecshopx.orders.service.userinvoice;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.UserOrderInvoice;
import cn.shopex.ecshopx.orders.repository.UserOrderInvoiceRepository;
import cn.shopex.ecshopx.orders.service.fapiao.hangxin.HangxinFapiaoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserOrderInvoiceAdminSetInvoiceService {

	private final UserOrderInvoiceRepository userOrderInvoiceRepository;
	private final HangxinFapiaoService hangxinFapiaoService;
	private final ObjectMapper objectMapper;

	public UserOrderInvoiceAdminSetInvoiceService(
			UserOrderInvoiceRepository userOrderInvoiceRepository,
			HangxinFapiaoService hangxinFapiaoService,
			ObjectMapper objectMapper) {
		this.userOrderInvoiceRepository = userOrderInvoiceRepository;
		this.hangxinFapiaoService = hangxinFapiaoService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> setInvoice(
			long companyId,
			int status,
			long requestInvoiceId,
			Long filterUserId,
			String filterOrderId,
			int page,
			int pageSize) {
		Long filterInvId = requestInvoiceId == 0L ? null : requestInvoiceId;
		Page<UserOrderInvoice> pg =
				userOrderInvoiceRepository.lists(
						companyId, filterUserId, filterInvId, filterOrderId, null, page, pageSize);
		long total = pg.getTotal();
		List<UserOrderInvoice> rows = pg.getRecords();

		List<Map<String, Object>> outList = new ArrayList<>();
		Map<String, Object> lastQueryRes = null;
		Map<String, Object> lastRedRes = null;

		for (UserOrderInvoice v : rows) {
			Map<String, Object> invoiceArr = parseInvoiceJson(v.getInvoice());
			invoiceArr.put("id", v.getId());
			invoiceArr.put("order_id", v.getOrderId());
			invoiceArr.put("user_id", v.getUserId());
			invoiceArr.put("status", v.getStatus());
			invoiceArr.put("type_hz", "");
			invoiceArr.put("amount", "0");
			outList.add(invoiceArr);

			String kptype = "1";

			if (status == 2
					&& requestInvoiceId == v.getId()
					&& missingFapiaoInfo(invoiceArr)
					&& Objects.equals(v.getStatus(), 1)) {
				Map<String, Object> params = new LinkedHashMap<>(invoiceArr);
				params.put("company_id", companyId);
				params.put("kptype", "1");
				Map<String, Object> openRes = hangxinFapiaoService.createFapiao(params);
				invoiceArr.put("fapiaoinfo", openRes);
				Integer stOrNull = statusIfHangxinOk(openRes) ? Integer.valueOf(status) : null;
				persistInvoiceRow(v.getId(), invoiceArr, stOrNull);
			}

			if (status == 5
					&& requestInvoiceId == v.getId()
					&& Objects.equals(v.getStatus(), 2)) {
				Map<String, Object> params = new LinkedHashMap<>(invoiceArr);
				params.put("company_id", companyId);
				params.put("kptype", "2");
				Map<String, Object> redRes = hangxinFapiaoService.createFapiao(params);
				lastRedRes = redRes;
				invoiceArr.put("fapiaoinfo_red", redRes);
				Integer stOrNull = statusIfHangxinOk(redRes) ? Integer.valueOf(status) : null;
				persistInvoiceRow(v.getId(), invoiceArr, stOrNull);
				kptype = "2";
			}

			if (status == 5 || status == 8 || (status == 2 && requestInvoiceId == v.getId())) {
				Map<String, Object> params = new LinkedHashMap<>(invoiceArr);
				params.put("company_id", companyId);
				params.put("kptype", kptype);
				Map<String, Object> queryRes = hangxinFapiaoService.getFapiao(params);
				lastQueryRes = queryRes;
				Integer stOrNull = statusIfHangxinOk(queryRes) ? Integer.valueOf(status) : null;
				Object resultObj = queryRes.get("result");
				if ("1".equals(kptype)) {
					invoiceArr.put("fapiaoinfo_query", resultObj);
				} else {
					invoiceArr.put("fapiaoinfo_query_red", resultObj);
				}
				persistInvoiceRow(v.getId(), invoiceArr, stOrNull);
			}
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("list", outList);
		payload.put("total_count", total);
		if (lastQueryRes != null) {
			payload.put("query_res", lastQueryRes);
		}
		if (lastRedRes != null) {
			payload.put("red_res", lastRedRes);
		}
		return payload;
	}

	private void persistInvoiceRow(long id, Map<String, Object> invoiceArr, Integer statusOrNull) {
		try {
			String json = objectMapper.writeValueAsString(invoiceArr);
			userOrderInvoiceRepository.updateInvoiceAndStatusById(id, json, statusOrNull);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("发票数据格式错误");
		}
	}

	private Map<String, Object> parseInvoiceJson(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new BadRequestException("发票数据格式错误");
		}
	}

	private static boolean missingFapiaoInfo(Map<String, Object> invoiceArr) {
		return !invoiceArr.containsKey("fapiaoinfo") || invoiceArr.get("fapiaoinfo") == null;
	}

	private static boolean statusIfHangxinOk(Map<String, Object> hangxinRes) {
		Object rc = hangxinRes.get("returnCode");
		return "0000".equals(rc == null ? null : String.valueOf(rc).trim());
	}
}
