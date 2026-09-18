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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayDealerEnterapplySaveService {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayEnterapplyReadService readService;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfpayEnterapplyUpdateApplyService updateApplyService;
	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;

	public HfpayDealerEnterapplySaveService(
			HfpayEnterapplyReadService readService,
			HfPayPaymentSettingService paymentSettingService,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfpayEnterapplyUpdateApplyService updateApplyService,
			HfpayEnterapplyMapper hfpayEnterapplyMapper) {
		this.readService = readService;
		this.paymentSettingService = paymentSettingService;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
		this.acouJsonPostClient = acouJsonPostClient;
		this.updateApplyService = updateApplyService;
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
	}

	public Map<String, Object> saveDealerApply(long companyId, long userId, Map<String, Object> params) {
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("apply_type", "3");
		params.put("id_card_type", "10");

		Map<String, Object> existing = readService.getEnterapplyByCompanyAndUser(companyId, userId);
		if (existing != null) {
			String status = existing.get("status") == null ? "" : String.valueOf(existing.get("status")).trim();
			if ("2".equals(status) || "3".equals(status)) {
				throw new ResourceException("请勿重复申请");
			}
			long enterapplyId = parseExistingEnterapplyId(existing);
			params.put("hfpay_enterapply_id", enterapplyId);
		}

		boolean needUser01 = existing == null || isBlankUserCustId(existing.get("user_cust_id"));
		if (needUser01) {
			Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
			String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			payload.put("version", 10);
			payload.put("mer_cust_id", merCustId);
			payload.put("order_date", LocalDate.now(ZoneId.systemDefault()).format(DAY));
			payload.put("order_id", orderApplyIdGenerator.nextOrderId());
			payload.put("user_name", strTrim(params.get("user_name")));
			payload.put("id_card_type", 10);
			payload.put("id_card", strTrim(params.get("id_card")));
			payload.put("user_mobile", strTrim(params.get("user_mobile")));

			Map<String, Object> apiResult = acouJsonPostClient.user01(setting, payload);

			String respCode = apiResult.get("resp_code") == null ? "" : String.valueOf(apiResult.get("resp_code")).trim();
			if (!"C00000".equals(respCode) && !"C00001".equals(respCode) && !"C00002".equals(respCode)) {
				Object desc = apiResult.get("resp_desc");
				throw new ResourceException(desc == null ? "" : String.valueOf(desc));
			}

			boolean hasOrderId = apiResult.containsKey("order_id") && apiResult.get("order_id") != null;
			boolean hasOrderDate = apiResult.containsKey("order_date") && apiResult.get("order_date") != null;
			if (hasOrderId || hasOrderDate) {
				params.put("hf_order_id", apiResult.get("order_id"));
				params.put("hf_order_date", apiResult.get("order_date"));
			}
			if ("C00001".equals(respCode) || "C00002".equals(respCode)) {
				params.put("status", "2");
			}
			if ("C00000".equals(respCode)) {
				params.put("user_cust_id", apiResult.get("user_cust_id"));
				params.put("acct_id", apiResult.get("acct_id"));
				params.put("status", "3");
			}
		}

		checkUser(params);

		Object idRaw = params.get("hfpay_enterapply_id");
		if (idRaw != null && StringUtils.hasText(String.valueOf(idRaw).trim())) {
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("hfpay_enterapply_id", idRaw);
			filter.put("company_id", companyId);
			Map<String, Object> editData = new LinkedHashMap<>(params);
			return updateApplyService.updateApply(filter, editData);
		}

		HfpayEnterapply entity = new HfpayEnterapply();
		HfpayEnterapplyUpdateApplyService.applyColumnData(entity, params);
		hfpayEnterapplyMapper.insert(entity);
		Long id = entity.getHfpayEnterapplyId();
		HfpayEnterapply reloaded = id != null ? hfpayEnterapplyMapper.selectById(id) : null;
		if (reloaded == null) {
			reloaded = hfpayEnterapplyMapper.selectOne(new LambdaQueryWrapper<HfpayEnterapply>()
					.eq(HfpayEnterapply::getCompanyId, companyId)
					.eq(HfpayEnterapply::getUserId, userId)
					.orderByDesc(HfpayEnterapply::getHfpayEnterapplyId)
					.last("LIMIT 1"));
		}
		if (reloaded == null) {
			throw new ResourceException("保存数据失败");
		}
		return HfpayEnterapplyRowConverter.columnNamesData(reloaded);
	}

	private static boolean isBlankUserCustId(Object v) {
		if (v == null) {
			return true;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() || "0".equals(s);
	}

	private static String strTrim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
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
			throw new ResourceException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new ResourceException(message);
		}
	}

	private static void checkUser(Map<String, Object> params) {
		requireNonBlank(params, "user_name", "用户姓名必填");
		requireNonBlank(params, "id_card_type", "证件类型必填");
		requireNonBlank(params, "id_card", "身份证号必填");
		requireNonBlank(params, "user_mobile", "手机号必填");
		requireNonBlank(params, "bank_acct_num", "银行卡号必填");
	}
}
