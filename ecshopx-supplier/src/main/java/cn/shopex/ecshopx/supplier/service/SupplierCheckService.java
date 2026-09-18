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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierCheckService {

	private static final ZoneId RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter RESPONSE_TIME_DATE_PREFIX =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SupplierMapper supplierMapper;

	public SupplierCheckService(SupplierMapper supplierMapper) {
		this.supplierMapper = supplierMapper;
	}

	public Object checkSupplier(long companyId, long supplierId, long isCheck, String auditRemark) {
		if (isCheck == 0L) {
			throw new BadRequestException("请选择审核结果");
		}
		if (isCheck == 2L && !StringUtils.hasText(auditRemark)) {
			throw new BadRequestException("请输入审核备注");
		}

		LocalDateTime modifyTimeAtWrite = LocalDateTime.now();
		LambdaUpdateWrapper<Supplier> wrapper = new LambdaUpdateWrapper<Supplier>()
				.eq(Supplier::getCompanyId, companyId)
				.eq(Supplier::getId, supplierId)
				.set(Supplier::getIsCheck, isCheck)
				.set(Supplier::getAuditRemark, auditRemark)
				.set(Supplier::getModifyTime, modifyTimeAtWrite);

		int rows = supplierMapper.update(null, wrapper);
		if (rows == 0) {
			return Boolean.TRUE;
		}
		Supplier fresh = supplierMapper.selectById(supplierId);
		if (fresh == null) {
			return Boolean.TRUE;
		}
		return toSnakeCaseResponseMap(fresh, modifyTimeAtWrite);
	}

	private static Map<String, Object> toSnakeCaseResponseMap(Supplier e, LocalDateTime modifyTimeForResponse) {
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
		map.put("modify_time", formatResponseTime(modifyTimeForResponse));
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
