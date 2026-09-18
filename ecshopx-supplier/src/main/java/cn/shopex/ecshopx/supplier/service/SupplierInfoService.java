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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SupplierInfoService {

	private static final ZoneId RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter RESPONSE_TIME_DATE_PREFIX =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SupplierMapper supplierMapper;

	public SupplierInfoService(SupplierMapper supplierMapper) {
		this.supplierMapper = supplierMapper;
	}

	private Supplier selectOneByCompanyIdAndOperatorId(long companyId, long operatorId) {
		LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<Supplier>()
				.eq(Supplier::getCompanyId, companyId)
				.eq(Supplier::getOperatorId, operatorId)
				.last("LIMIT 1");
		return supplierMapper.selectOne(w);
	}

	public LinkedHashMap<String, Object> getWxappSupplierPublicInfo(long companyId, long operatorId) {
		Supplier row = selectOneByCompanyIdAndOperatorId(companyId, operatorId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("wechat_qrcode", row == null || row.getWechatQrcode() == null ? "" : row.getWechatQrcode());
		out.put("service_tel", row == null || row.getServiceTel() == null ? "" : row.getServiceTel());
		out.put("supplier_name", row == null || row.getSupplierName() == null ? "" : row.getSupplierName());
		return out;
	}

	public Map<String, Object> getSupplierInfo(long companyId, long operatorId) {
		Supplier row = selectOneByCompanyIdAndOperatorId(companyId, operatorId);

		LinkedHashMap<String, Object> supplierInfoMap;
		if (row == null) {
			supplierInfoMap = new LinkedHashMap<>();
			supplierInfoMap.put("id", "");
			supplierInfoMap.put("supplier_name", "");
			supplierInfoMap.put("contact", "");
			supplierInfoMap.put("mobile", "");
			supplierInfoMap.put("business_license", "");
			supplierInfoMap.put("wechat_qrcode", "");
			supplierInfoMap.put("service_tel", "");
			supplierInfoMap.put("bank_name", "");
			supplierInfoMap.put("bank_account", "");
		} else {
			supplierInfoMap = toSnakeCaseResponseMap(row);
		}

		LinkedHashMap<String, Object> filterMap = new LinkedHashMap<>();
		filterMap.put("company_id", companyId);
		filterMap.put("operator_id", operatorId);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("supplier_info", supplierInfoMap);
		out.put("filter", filterMap);
		return out;
	}

	private static LinkedHashMap<String, Object> toSnakeCaseResponseMap(Supplier e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("id", e.getId());
		map.put("company_id", e.getCompanyId());
		map.put("supplier_name", e.getSupplierName());
		map.put("contact", e.getContact());
		map.put("mobile", e.getMobile());
		map.put("business_license", e.getBusinessLicense());
		map.put("wechat_qrcode", e.getWechatQrcode());
		map.put("service_tel", e.getServiceTel());
		map.put("bank_name", e.getBankName());
		map.put("bank_account", e.getBankAccount());
		map.put("is_check", e.getIsCheck());
		map.put("audit_remark", e.getAuditRemark());
		map.put("operator_id", e.getOperatorId());
		map.put("add_time", formatResponseTime(e.getAddTime()));
		map.put("modify_time", formatResponseTime(e.getModifyTime()));
		return map;
	}

	private static Object formatResponseTime(LocalDateTime t) {
		if (t == null) {
			return null;
		}
		ZonedDateTime zdt = t.atZone(RESPONSE_TIME_ZONE);
		int micros = zdt.getNano() / 1000;
		String dateStr = zdt.format(RESPONSE_TIME_DATE_PREFIX) + "." + String.format("%06d", micros);
		LinkedHashMap<String, Object> nested = new LinkedHashMap<>(3);
		nested.put("date", dateStr);
		nested.put("timezone_type", 3);
		nested.put("timezone", "PRC");
		return nested;
	}
}
