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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.CompanysPharmaIndustrySettingReadService;
import cn.shopex.ecshopx.members.domain.MedicationPersonnel;
import cn.shopex.ecshopx.members.mapper.MedicationPersonnelMapper;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrdersDiagnosis;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDiagnosisMapper;
import cn.shopex.ecshopx.thirdparty.service.kuaizhen580.Kuaizhen580InitPreDemandHttpClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PrescriptionCreateDiagnosisService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final MedicationPersonnelMapper medicationPersonnelMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService;
	private final Kuaizhen580InitPreDemandHttpClient kuaizhen580InitPreDemandHttpClient;
	private final OrdersDiagnosisMapper ordersDiagnosisMapper;
	private final ObjectMapper objectMapper;

	public PrescriptionCreateDiagnosisService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			MedicationPersonnelMapper medicationPersonnelMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			CompanysPharmaIndustrySettingReadService companysPharmaIndustrySettingReadService,
			Kuaizhen580InitPreDemandHttpClient kuaizhen580InitPreDemandHttpClient,
			OrdersDiagnosisMapper ordersDiagnosisMapper,
			ObjectMapper objectMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.medicationPersonnelMapper = medicationPersonnelMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.companysPharmaIndustrySettingReadService = companysPharmaIndustrySettingReadService;
		this.kuaizhen580InitPreDemandHttpClient = kuaizhen580InitPreDemandHttpClient;
		this.ordersDiagnosisMapper = ordersDiagnosisMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createDiagnosis(
			HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		requirePresent(merged.get("order_id"), "缺少订单信息");
		long orderIdLong = parseLongOrBadRequest(merged.get("order_id"), "缺少订单信息");

		requirePresent(merged.get("company_id"), "缺少参数");
		long companyId = parseLongOrBadRequest(merged.get("company_id"), "缺少参数");

		requirePresent(merged.get("user_id"), "缺少用户信息");
		long userIdFromAuth = parseLongOrBadRequest(merged.get("user_id"), "缺少用户信息");

		requirePresent(merged.get("medication_personnel_id"), "请选择用药人");
		long personnelId = parseLongOrBadRequest(merged.get("medication_personnel_id"), "请选择用药人");

		int isPregnantWoman = parseBinary01(merged.get("is_pregnant_woman"), "请选择用药人是否孕妇");
		int isLactation = parseBinary01(merged.get("is_lactation"), "请选择用药人是否哺乳期");

		if (!merged.containsKey("before_ai_result_symptom")
				|| merged.get("before_ai_result_symptom") == null) {
			throw new BadRequestException("请选择症状");
		}

		if (!merged.containsKey("before_ai_result_used_medicine")
				|| merged.get("before_ai_result_used_medicine") == null) {
			throw new BadRequestException("请选择是否使用过此类药物");
		}
		if (!merged.containsKey("before_ai_result_body_abnormal")
				|| merged.get("before_ai_result_body_abnormal") == null) {
			throw new BadRequestException("请选择肝肾功能是否有异常");
		}

		Object rRaw = merged.get("prescription_order_random");
		String r = rRaw instanceof String s ? s : (rRaw == null ? null : String.valueOf(rRaw));
		boolean randomBranch = StringUtils.hasText(r);

		LambdaQueryWrapper<NormalOrders> orderW = new LambdaQueryWrapper<>();
		orderW.eq(NormalOrders::getOrderId, orderIdLong).eq(NormalOrders::getCompanyId, companyId);
		if (!randomBranch) {
			orderW.eq(NormalOrders::getUserId, userIdFromAuth);
		}
		NormalOrders order = normalOrdersMapper.selectOne(orderW);
		if (order == null) {
			throw new ResourceException("订单不存在");
		}

		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderIdLong)
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getUserId, order.getUserId()));

		List<Map<String, Object>> medicacines = new ArrayList<>();
		for (NormalOrdersItems item : items) {
			Integer ip = item.getIsPrescription();
			if (ip == null || ip == 0) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("medicineId", item.getItemId());
			m.put("number", item.getNum() != null ? item.getNum() : 0);
			medicacines.add(m);
		}

		MedicationPersonnel personnel =
				medicationPersonnelMapper.selectOne(
						new LambdaQueryWrapper<MedicationPersonnel>()
								.eq(MedicationPersonnel::getId, personnelId)
								.eq(MedicationPersonnel::getCompanyId, companyId)
								.eq(MedicationPersonnel::getUserId, userIdFromAuth));
		if (personnel == null) {
			throw new ResourceException("用药人不存在");
		}

		long distId = order.getDistributorId() == null ? 0L : order.getDistributorId();
		DistributionDistributorPeek peek;
		if (distId == 0) {
			LambdaQueryWrapper<DistributionDistributorPeek> distW = new LambdaQueryWrapper<>();
			distW.eq(DistributionDistributorPeek::getCompanyId, companyId)
					.eq(DistributionDistributorPeek::getDistributorSelf, 1)
					.last("LIMIT 1");
			peek = distributionDistributorPeekMapper.selectOne(distW);
		} else {
			LambdaQueryWrapper<DistributionDistributorPeek> distW = new LambdaQueryWrapper<>();
			distW.eq(DistributionDistributorPeek::getDistributorId, distId)
					.eq(DistributionDistributorPeek::getCompanyId, companyId)
					.last("LIMIT 1");
			peek = distributionDistributorPeekMapper.selectOne(distW);
		}

		Long peekStore = (peek != null ? peek.getKuaizhenStoreId() : null);
		long kuaizhenStoreId;
		if (peekStore != null && peekStore != 0L) {
			kuaizhenStoreId = peekStore;
		} else {
			Map<String, Object> medicineSetting =
					companysPharmaIndustrySettingReadService.getMedicineSetting(companyId);
			if (medicineSetting == null || medicineSetting.isEmpty()) {
				throw new ResourceException("缺少配置");
			}
			Object kzRaw = medicineSetting.get("kuaizhen580_config");
			if (!(kzRaw instanceof Map<?, ?> kzMap)) {
				throw new ResourceException("缺少配置");
			}
			Object storeRaw = kzMap.get("kuaizhen_store_id");
			long sid = parseConfigStoreId(storeRaw);
			if (sid == 0) {
				throw new ResourceException("缺少配置");
			}
			kuaizhenStoreId = sid;
		}

		Object symRaw = merged.get("before_ai_result_symptom");
		if (!(symRaw instanceof List<?>)) {
			throw new BadRequestException("请选择症状");
		}
		@SuppressWarnings("unchecked")
		List<?> symList = (List<?>) symRaw;
		Set<String> symptomSet = new LinkedHashSet<>();
		for (Object item : symList) {
			if (!(item instanceof Map<?, ?> symptomItem)) {
				throw new BadRequestException("请选择症状");
			}
			Object valObj = symptomItem.get("value");
			if (!(valObj instanceof List<?> values)) {
				throw new BadRequestException("请选择症状");
			}
			for (Object x : values) {
				symptomSet.add(String.valueOf(x));
			}
		}
		String symptomAnswer = String.join(",", symptomSet);

		Object allergy = merged.get("before_ai_result_allergy_history");
		String allergyStr;
		if (allergy == null || String.valueOf(allergy).trim().isEmpty()) {
			allergyStr = "否";
		} else {
			allergyStr = String.valueOf(allergy);
		}

		int serviceType = parseIntWithDefault(merged.get("service_type"), 0);
		int sourceFrom = parseIntWithDefault(merged.get("source_from"), 0);

		Map<String, Object> requestParams = new LinkedHashMap<>();
		requestParams.put("openid", "");
		requestParams.put("headimgurl", "");
		requestParams.put("store_id", kuaizhenStoreId);
		requestParams.put("service_type", serviceType);
		requestParams.put("is_examine", 1);
		requestParams.put("is_pregnant_woman", isPregnantWoman);
		requestParams.put("is_lactation", isLactation);
		requestParams.put("source_from", sourceFrom);
		requestParams.put("user_family_name", personnel.getUserFamilyName());
		requestParams.put("user_family_id_card", personnel.getUserFamilyIdCard());
		requestParams.put("user_family_age", personnel.getUserFamilyAge());
		requestParams.put("user_family_gender", personnel.getUserFamilyGender());
		requestParams.put("user_family_phone", personnel.getUserFamilyPhone());
		requestParams.put("relationship", personnel.getRelationship());
		requestParams.put("order_id", order.getOrderId());
		requestParams.put("before_ai_result_symptom", symptomAnswer);
		requestParams.put("before_ai_result_medicines", medicacines);
		requestParams.put(
				"before_ai_result_used_medicine", yesNoCn(merged.get("before_ai_result_used_medicine")));
		requestParams.put("before_ai_result_allergy_history", allergyStr);
		requestParams.put(
				"before_ai_result_body_abnormal", yesNoCn(merged.get("before_ai_result_body_abnormal")));
		requestParams.put(
				"user_family_addr",
				merged.get("user_family_addr") == null ? "" : String.valueOf(merged.get("user_family_addr")));
		requestParams.put("img_list", merged.getOrDefault("img_list", ""));
		requestParams.put("third_return_url", merged.getOrDefault("third_return_url", ""));

		String redirectUrl = kuaizhen580InitPreDemandHttpClient.initPreDemand(companyId, requestParams);
		if (!StringUtils.hasText(redirectUrl)) {
			throw new ResourceException("问诊单创建失败");
		}
		redirectUrl = redirectUrl + "&thirdPlatform=0";

		Map<String, Object> beforeAiDataList = new LinkedHashMap<>();
		beforeAiDataList.put("before_ai_result_symptom", merged.get("before_ai_result_symptom"));
		beforeAiDataList.put("before_ai_result_medicines", medicacines);
		beforeAiDataList.put("before_ai_result_used_medicine", merged.get("before_ai_result_used_medicine"));
		beforeAiDataList.put("before_ai_result_allergy_history", merged.get("before_ai_result_allergy_history"));
		beforeAiDataList.put("before_ai_result_body_abnormal", merged.get("before_ai_result_body_abnormal"));

		String beforeAiJson;
		try {
			beforeAiJson = objectMapper.writeValueAsString(beforeAiDataList);
		} catch (JsonProcessingException e) {
			throw new ResourceException("问诊单创建失败");
		}

		OrdersDiagnosis row = new OrdersDiagnosis();
		row.setOrderId(String.valueOf(order.getOrderId()));
		row.setUserId(order.getUserId());
		row.setCompanyId(order.getCompanyId());
		row.setKuaizhenStoreId(kuaizhenStoreId);
		row.setDistributorId(
				peek != null && peek.getDistributorId() != null ? peek.getDistributorId() : 0L);
		row.setServiceType(serviceType);
		row.setIsExamine(1);
		row.setIsPregnantWoman(isPregnantWoman);
		row.setIsLactation(isLactation);
		row.setUserFamilyName(personnel.getUserFamilyName());
		row.setUserFamilyIdCard(personnel.getUserFamilyIdCard());
		row.setUserFamilyAge(personnel.getUserFamilyAge());
		row.setUserFamilyGender(personnel.getUserFamilyGender());
		row.setUserFamilyPhone(personnel.getUserFamilyPhone());
		row.setRelationship(personnel.getRelationship());
		row.setBeforeAiDataList(beforeAiJson);
		row.setLocationUrl(redirectUrl);
		row.setPrescriptionStatus(1);
		row.setStatus(1);
		int now = (int) (System.currentTimeMillis() / 1000);
		row.setCreated(now);
		row.setUpdated(now);

		ordersDiagnosisMapper.insert(row);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("url", redirectUrl);
		return out;
	}

	private static void requirePresent(Object v, String msg) {
		if (v == null) {
			throw new BadRequestException(msg);
		}
		if (v instanceof String s && s.trim().isEmpty()) {
			throw new BadRequestException(msg);
		}
	}

	private static long parseLongOrBadRequest(Object v, String msg) {
		if (v == null) {
			throw new BadRequestException(msg);
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(msg);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(msg);
		}
	}

	private static long parseConfigStoreId(Object storeRaw) {
		if (storeRaw instanceof Number n) {
			return n.longValue();
		}
		if (storeRaw == null) {
			return 0L;
		}
		String ts = String.valueOf(storeRaw).trim();
		if (ts.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(ts);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseBinary01(Object v, String errMsg) {
		if (v == null) {
			throw new BadRequestException(errMsg);
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			int i = n.intValue();
			if (i == 0 || i == 1) {
				return i;
			}
			throw new BadRequestException(errMsg);
		}
		String s = String.valueOf(v).trim();
		if ("0".equals(s) || "1".equals(s)) {
			return Integer.parseInt(s);
		}
		if ("false".equalsIgnoreCase(s)) {
			return 0;
		}
		if ("true".equalsIgnoreCase(s)) {
			return 1;
		}
		throw new BadRequestException(errMsg);
	}

	private static String yesNoCn(Object v) {
		if (v == null) {
			return "否";
		}
		if (v instanceof Boolean b) {
			return b ? "是" : "否";
		}
		if (v instanceof Number n) {
			return n.intValue() != 0 ? "是" : "否";
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return "否";
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return "否";
		}
		return "是";
	}

	private static int parseIntWithDefault(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return def;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
