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

package cn.shopex.ecshopx.hfpay.service.enterapply;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayEnterapplySaveService {

	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;
	private final HfpayEnterapplyUpdateApplyService hfpayEnterapplyUpdateApplyService;
	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;
	private final ObjectMapper objectMapper;

	public HfpayEnterapplySaveService(
			HfpayEnterapplyReadService hfpayEnterapplyReadService,
			HfpayEnterapplyUpdateApplyService hfpayEnterapplyUpdateApplyService,
			HfpayEnterapplyMapper hfpayEnterapplyMapper,
			ObjectMapper objectMapper) {
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
		this.hfpayEnterapplyUpdateApplyService = hfpayEnterapplyUpdateApplyService;
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> saveEnterapply(long companyId, Map<String, Object> params) {
		params.put("company_id", companyId);

		long distributorId = parseRequiredDistributorId(params);

		Map<String, Object> existing = hfpayEnterapplyReadService.getEnterapply(companyId, distributorId);
		if (existing == null) {
			params.put("status", "1");
		} else {
			String status = existing.get("status") == null ? "" : String.valueOf(existing.get("status")).trim();
			if ("2".equals(status) || "3".equals(status)) {
				throw new ResourceException("该状态下不允许编辑");
			}
			long enterapplyId = parseExistingEnterapplyId(existing);
			params.put("hfpay_enterapply_id", enterapplyId);
		}

		String applyType = params.get("apply_type") == null ? "" : String.valueOf(params.get("apply_type")).trim();
		switch (applyType) {
			case "1" -> checkCorp(params);
			case "2" -> checkSolo(params);
			case "3" -> checkUser(params);
			default -> throw new BadRequestException("未知的入驻类型");
		}

		if ("1".equals(applyType)) {
			List<Map<String, String>> list = new ArrayList<>(1);
			Map<String, String> one = new LinkedHashMap<>();
			one.put("custName", String.valueOf(params.get("controlling_shareholder_cust_name")).trim());
			one.put("idCardType", String.valueOf(params.get("controlling_shareholder_id_card_type")).trim());
			one.put("idCard", String.valueOf(params.get("controlling_shareholder_id_card")).trim());
			list.add(one);
			try {
				params.put("controlling_shareholder", objectMapper.writeValueAsString(list));
			} catch (JsonProcessingException e) {
				throw new ResourceException("保存数据失败");
			}
		}

		Object idRaw = params.get("hfpay_enterapply_id");
		if (idRaw != null && StringUtils.hasText(String.valueOf(idRaw).trim())) {
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("hfpay_enterapply_id", idRaw);
			filter.put("company_id", companyId);
			Map<String, Object> editData = new LinkedHashMap<>(params);
			return hfpayEnterapplyUpdateApplyService.updateApply(filter, editData);
		}

		HfpayEnterapply entity = new HfpayEnterapply();
		HfpayEnterapplyUpdateApplyService.applyColumnData(entity, params);
		hfpayEnterapplyMapper.insert(entity);
		Long id = entity.getHfpayEnterapplyId();
		HfpayEnterapply reloaded = id != null ? hfpayEnterapplyMapper.selectById(id) : null;
		if (reloaded == null) {
			reloaded = hfpayEnterapplyMapper.selectOne(new LambdaQueryWrapper<HfpayEnterapply>()
					.eq(HfpayEnterapply::getCompanyId, companyId)
					.eq(HfpayEnterapply::getDistributorId, distributorId)
					.orderByDesc(HfpayEnterapply::getHfpayEnterapplyId)
					.last("LIMIT 1"));
		}
		if (reloaded == null) {
			throw new ResourceException("保存数据失败");
		}
		return HfpayEnterapplyRowConverter.columnNamesData(reloaded);
	}

	private static long parseRequiredDistributorId(Map<String, Object> params) {
		Object raw = params.get("distributor_id");
		if (raw == null) {
			throw new BadRequestException("缺少必填参数: distributor_id");
		}
		if (raw instanceof Number) {
			return ((Number) raw).longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("缺少必填参数: distributor_id");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("缺少必填参数: distributor_id");
		}
	}

	private static long parseExistingEnterapplyId(Map<String, Object> existing) {
		Object idRaw = existing.get("hfpay_enterapply_id");
		if (idRaw instanceof Number) {
			return ((Number) idRaw).longValue();
		}
		if (idRaw == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String s = String.valueOf(idRaw).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("未查询到更新数据");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static void requireNonBlank(Map<String, Object> params, String key, String message) {
		Object v = params.get(key);
		if (v == null) {
			throw new BadRequestException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(message);
		}
	}

	private void checkCorp(Map<String, Object> params) {
		requireNonBlank(params, "corp_license_type", "企业证照类型必填");
		String corpLicenseType = String.valueOf(params.get("corp_license_type")).trim();
		if (!"1".equals(corpLicenseType) && !"2".equals(corpLicenseType)) {
			throw new BadRequestException("企业证照类型不正确");
		}
		requireNonBlank(params, "corp_name", "企业名称必填");
		if ("1".equals(corpLicenseType)) {
			requireNonBlank(params, "business_code", "营业执照注册号必填");
			requireNonBlank(params, "institution_code", "组织机构代码必填");
			requireNonBlank(params, "tax_code", "税务登记证号必填");
		} else {
			requireNonBlank(params, "social_credit_code", "统一社会信用代码必填");
		}
		requireNonBlank(params, "license_start_date", "证照起始日期必填");
		requireNonBlank(params, "license_end_date", "证照结束日期必填");
		requireNonBlank(params, "controlling_shareholder_cust_name", "控股股东姓名必填");
		requireNonBlank(params, "controlling_shareholder_id_card_type", "控股股东证件类型必填");
		requireNonBlank(params, "controlling_shareholder_id_card", "控股股东证件号必填");
		requireNonBlank(params, "legal_name", "法人姓名必填");
		requireNonBlank(params, "legal_id_card_type", "法人证件类型必填");
		requireNonBlank(params, "legal_id_card", "法人证件号码必填");
		requireNonBlank(params, "legal_cert_start_date", "法人证件起始日期必填");
		requireNonBlank(params, "legal_cert_end_date", "法人证件起始日期必填");
		requireNonBlank(params, "legal_mobile", "法人手机号码必填");
		requireNonBlank(params, "contact_name", "企业联系人姓名必填");
		requireNonBlank(params, "contact_mobile", "联系人手机号必填");
		requireNonBlank(params, "contact_email", "联系人邮箱必填");
		requireNonBlank(params, "bank_acct_name", "开户银行账户名必填");
		requireNonBlank(params, "bank_id", "开户银行必填");
		requireNonBlank(params, "bank_acct_num", "开户银行账号必填");
		requireNonBlank(params, "bank_prov", "开户银行省份必填");
		requireNonBlank(params, "bank_area", "开户银行地区必填");
		requireNonBlank(params, "bank_branch", "企业开户银行的支行名称");
	}

	private void checkSolo(Map<String, Object> params) {
		requireNonBlank(params, "solo_name", "个体户名称必填");
		requireNonBlank(params, "business_code", "营业执照注册号必填");
		requireNonBlank(params, "license_start_date", "证照起始日期必填");
		requireNonBlank(params, "license_end_date", "证照结束日期必填");
		requireNonBlank(params, "solo_business_address", "个体户经营地址必填");
		requireNonBlank(params, "solo_reg_address", "个体户注册地址必填");
		requireNonBlank(params, "solo_fixed_telephone", "个体户固定电话必填");
		requireNonBlank(params, "business_scope", "经营范围必填");
		requireNonBlank(params, "legal_name", "法人姓名必填");
		requireNonBlank(params, "legal_id_card_type", "法人证件类型必填");
		requireNonBlank(params, "legal_id_card", "法人证件号码必填");
		requireNonBlank(params, "legal_cert_start_date", "法人证件起始日期必填");
		requireNonBlank(params, "legal_cert_end_date", "法人证件起始日期必填");
		requireNonBlank(params, "legal_mobile", "法人手机号码必填");
		requireNonBlank(params, "contact_name", "企业联系人姓名必填");
		requireNonBlank(params, "contact_mobile", "联系人手机号必填");
		requireNonBlank(params, "contact_email", "联系人邮箱必填");
		requireNonBlank(params, "occupation", "职业必填");
		requireNonBlank(params, "bank_acct_num", "开户银行账号必填");
	}

	private void checkUser(Map<String, Object> params) {
		requireNonBlank(params, "user_name", "用户姓名必填");
		requireNonBlank(params, "id_card_type", "证件类型必填");
		requireNonBlank(params, "id_card", "身份证号必填");
		requireNonBlank(params, "user_mobile", "手机号必填");
		requireNonBlank(params, "bank_acct_num", "银行卡号必填");
	}
}
