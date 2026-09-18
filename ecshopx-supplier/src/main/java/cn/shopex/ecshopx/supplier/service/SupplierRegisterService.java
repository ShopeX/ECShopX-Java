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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.dto.admin.v1.SupplierRegisterRequest;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.service.multilang.SupplierOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SupplierRegisterService {

	private static final List<String> REGISTER_VALIDATE_PROPERTY_ORDER = List.of(
			"supplierName",
			"contact",
			"mobile",
			"businessLicense",
			"wechatQrcode",
			"serviceTel",
			"bankName",
			"bankAccount");

	private static final ZoneId RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	private static final DateTimeFormatter RESPONSE_TIME_DATE_PREFIX =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final SupplierMapper supplierMapper;
	private final SupplierOutsideMultiLangWriteService supplierOutsideMultiLangWriteService;
	private final Validator validator;

	public SupplierRegisterService(
			SupplierMapper supplierMapper,
			SupplierOutsideMultiLangWriteService supplierOutsideMultiLangWriteService,
			Validator validator) {
		this.supplierMapper = supplierMapper;
		this.supplierOutsideMultiLangWriteService = supplierOutsideMultiLangWriteService;
		this.validator = validator;
	}

	public Map<String, Object> register(
			SupplierRegisterRequest body, long companyId, long operatorId, String requestLangTag) {
		Set<ConstraintViolation<SupplierRegisterRequest>> viol = validator.validate(body);
		if (!viol.isEmpty()) {
			throw new BadRequestException(firstRegisterViolationMessage(viol));
		}

		LambdaQueryWrapper<Supplier> wName = new LambdaQueryWrapper<Supplier>()
				.eq(Supplier::getCompanyId, companyId)
				.eq(Supplier::getSupplierName, body.getSupplierName());
		Supplier rowName = supplierMapper.selectOne(wName);

		if (rowName != null) {
			if (rowName.getOperatorId() != null && !Objects.equals(rowName.getOperatorId(), operatorId)) {
				throw new ResourceException("供应商名称不能重复");
			}
			applyBodyToSupplier(rowName, body, companyId, operatorId);
			int rows = supplierMapper.updateById(rowName);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Supplier fresh = supplierMapper.selectById(rowName.getId());
			return toSnakeCaseResponseMap(fresh);
		}

		LambdaQueryWrapper<Supplier> wOp = new LambdaQueryWrapper<Supplier>()
				.eq(Supplier::getCompanyId, companyId)
				.eq(Supplier::getOperatorId, operatorId);
		Supplier rowOp = supplierMapper.selectOne(wOp);
		if (rowOp != null) {
			applyBodyToSupplier(rowOp, body, companyId, operatorId);
			int rows = supplierMapper.updateById(rowOp);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Supplier fresh = supplierMapper.selectById(rowOp.getId());
			return toSnakeCaseResponseMap(fresh);
		}

		Supplier ins = new Supplier();
		ins.setSupplierName(body.getSupplierName());
		ins.setContact(body.getContact());
		ins.setMobile(body.getMobile());
		ins.setBusinessLicense(body.getBusinessLicense());
		ins.setWechatQrcode(body.getWechatQrcode());
		ins.setServiceTel(body.getServiceTel());
		ins.setBankName(body.getBankName());
		ins.setBankAccount(body.getBankAccount());
		ins.setCompanyId(companyId);
		ins.setOperatorId(operatorId);
		ins.setIsCheck(0L);
		ins.setAuditRemark(null);
		ins.setAdapayMchId(null);
		ins.setWxOpenid(null);
		LocalDateTime now = LocalDateTime.now();
		ins.setAddTime(now);
		ins.setModifyTime(now);
		supplierMapper.insert(ins);

		Map<String, Object> langBag = new LinkedHashMap<>();
		langBag.put("supplier_name", body.getSupplierName());
		langBag.put("contact", body.getContact());
		langBag.put("business_license", body.getBusinessLicense());
		langBag.put("bank_name", body.getBankName());
		supplierOutsideMultiLangWriteService.addForNewSupplier(ins.getId(), companyId, langBag, requestLangTag);

		return toSnakeCaseResponseMap(supplierMapper.selectById(ins.getId()));
	}

	private static String firstRegisterViolationMessage(Set<ConstraintViolation<SupplierRegisterRequest>> viol) {
		LinkedHashMap<String, String> byProperty = new LinkedHashMap<>();
		for (ConstraintViolation<SupplierRegisterRequest> v : viol) {
			String prop = v.getPropertyPath().toString();
			byProperty.putIfAbsent(prop, v.getMessage());
		}
		for (String name : REGISTER_VALIDATE_PROPERTY_ORDER) {
			String msg = byProperty.get(name);
			if (msg != null) {
				return msg;
			}
		}
		return viol.iterator().next().getMessage();
	}

	private static void applyBodyToSupplier(Supplier target, SupplierRegisterRequest body, long companyId, long operatorId) {
		target.setSupplierName(body.getSupplierName());
		target.setContact(body.getContact());
		target.setMobile(body.getMobile());
		target.setBusinessLicense(body.getBusinessLicense());
		target.setWechatQrcode(body.getWechatQrcode());
		target.setServiceTel(body.getServiceTel());
		target.setBankName(body.getBankName());
		target.setBankAccount(body.getBankAccount());
		target.setCompanyId(companyId);
		target.setOperatorId(operatorId);
		target.setIsCheck(0L);
		target.setModifyTime(LocalDateTime.now());
	}

	private static Map<String, Object> toSnakeCaseResponseMap(Supplier e) {
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
