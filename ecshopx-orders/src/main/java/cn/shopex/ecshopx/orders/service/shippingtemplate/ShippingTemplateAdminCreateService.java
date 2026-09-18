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

package cn.shopex.ecshopx.orders.service.shippingtemplate;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.mapper.ShippingTemplatesMapper;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingTemplateAdminCreateService {

	private final ShippingTemplatesMapper shippingTemplatesMapper;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ObjectMapper objectMapper;

	public ShippingTemplateAdminCreateService(
			ShippingTemplatesMapper shippingTemplatesMapper,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ObjectMapper objectMapper) {
		this.shippingTemplatesMapper = shippingTemplatesMapper;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createShippingTemplates(
			long companyId, long distributorId, long supplierId, Map<String, Object> mergedIn) {
		Map<String, Object> params = new LinkedHashMap<>(mergedIn);
		params.put("company_id", companyId);
		params.put("distributor_id", distributorId);
		params.put("supplier_id", supplierId);

		Map<String, Object> info = formatCityData(params);
		String nameTrimmed = (String) info.get("name");
		if (shippingTemplatesQueryRepository
				.findTemplateIdByNameCompanySupplierAndDistributor(
						companyId, nameTrimmed, supplierId, distributorId)
				.isPresent()) {
			throw new ResourceException("运费模板已存在");
		}

		ShippingTemplates entity = new ShippingTemplates();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setSupplierId(supplierId);
		entity.setName(nameTrimmed);
		entity.setIsFree((String) info.get("is_free"));
		entity.setValuation((String) info.get("valuation"));
		Integer statusInt = (Integer) info.get("status");
		entity.setStatus(statusInt != null && statusInt == 1);
		entity.setNopostConf((String) info.get("nopost_conf"));
		entity.setFeeConf(info.containsKey("fee_conf") ? (String) info.get("fee_conf") : null);
		entity.setFreeConf(info.containsKey("free_conf") ? (String) info.get("free_conf") : null);
		entity.setProtect(null);
		entity.setProtectRate(null);
		entity.setMinprice(null);

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreateTime(now);
		entity.setUpdateTime(now);

		shippingTemplatesMapper.insert(entity);
		if (entity.getTemplateId() == null) {
			throw new ResourceException("运费模板创建失败");
		}
		return toTemplateDetailRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateShippingTemplates(
			String templateIdStr,
			long companyId,
			long distributorId,
			long supplierId,
			Map<String, Object> mergedIn) {
		Map<String, Object> params = new LinkedHashMap<>(mergedIn);
		params.put("company_id", companyId);
		params.put("distributor_id", distributorId);
		params.put("supplier_id", supplierId);

		Optional<ShippingTemplates> existingOpt =
				shippingTemplatesQueryRepository.findByTemplateBusinessKey(
						templateIdStr, companyId, distributorId, supplierId);
		ShippingTemplates existing =
				existingOpt.orElseThrow(() -> new ResourceException("运费模板未创建"));

		if (looseNameNotEqual(params.get("name"), existing.getName())) {
			String lookupName = normalizeScalarForLooseName(params.get("name"));
			Optional<Long> dupId =
					shippingTemplatesQueryRepository.findTemplateIdByNameCompanySupplierAndDistributor(
							companyId, lookupName, supplierId, distributorId);
			if (dupId.isPresent() && !dupId.get().equals(existing.getTemplateId())) {
				throw new ResourceException("运费模板已存在");
			}
		}

		String requestValuationNorm = normalizeValuation(params.get("valuation"));
		Object storedValRaw = existing.getValuation() != null ? existing.getValuation() : "1";
		String storedValuationNorm = normalizeValuation(storedValRaw);
		if (!requestValuationNorm.equals(storedValuationNorm)) {
			throw new ResourceException("计价方式不能修改");
		}

		Map<String, Object> info = formatCityData(params);

		LambdaUpdateWrapper<ShippingTemplates> wrapper = new LambdaUpdateWrapper<>();
		wrapper.eq(ShippingTemplates::getTemplateId, existing.getTemplateId())
				.eq(ShippingTemplates::getCompanyId, companyId)
				.eq(ShippingTemplates::getDistributorId, distributorId)
				.eq(ShippingTemplates::getSupplierId, supplierId);

		Integer statusInt = (Integer) info.get("status");
		wrapper.set(ShippingTemplates::getStatus, Integer.valueOf(1).equals(statusInt));
		wrapper.set(ShippingTemplates::getName, (String) info.get("name"));
		if (info.containsKey("is_free")) {
			wrapper.set(ShippingTemplates::getIsFree, (String) info.get("is_free"));
		}
		if (info.containsKey("valuation")) {
			wrapper.set(ShippingTemplates::getValuation, (String) info.get("valuation"));
		}
		wrapper.set(ShippingTemplates::getNopostConf, (String) info.get("nopost_conf"));
		if (info.containsKey("fee_conf")) {
			wrapper.set(ShippingTemplates::getFeeConf, (String) info.get("fee_conf"));
		}
		if (info.containsKey("free_conf")) {
			wrapper.set(ShippingTemplates::getFreeConf, (String) info.get("free_conf"));
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		wrapper.set(ShippingTemplates::getUpdateTime, now);

		int rows = shippingTemplatesMapper.update(null, wrapper);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		ShippingTemplates fresh =
				shippingTemplatesQueryRepository
						.findByTemplateBusinessKey(templateIdStr, companyId, distributorId, supplierId)
						.orElseThrow(() -> new ResourceException("未查询到更新数据"));
		return toTemplateDetailRow(fresh);
	}

	private Map<String, Object> formatCityData(Map<String, Object> params) {
		try {
			int isFreeInt = parseIsFree(params.get("is_free"));
			Object rawName = params.get("name");
			if (rawName == null) {
				throw new BadRequestException("模板名称不能为空!");
			}
			String name = String.valueOf(rawName).trim();
			if (name.isEmpty()) {
				throw new BadRequestException("模板名称不能为空!");
			}
			int statusInt = parseStatus(params.get("status"));
			if (params.get("valuation") == null) {
				throw new BadRequestException("计价方式参数存在问题");
			}
			String valuation = normalizeValuation(params.get("valuation"));

			Map<String, Object> info = new LinkedHashMap<>();
			if (params.containsKey("company_id")) {
				info.put("company_id", params.get("company_id"));
			}
			info.put("distributor_id", params.get("distributor_id"));
			info.put("supplier_id", params.get("supplier_id"));
			info.put("is_free", isFreeInt == 1 ? "1" : "0");
			info.put("name", name);
			info.put("status", statusInt);
			info.put("valuation", valuation);

			Object rawNopost = params.get("nopost_conf");
			String nopostJson =
					objectMapper.writeValueAsString(
							rawNopost instanceof List || rawNopost instanceof Map
									? rawNopost
									: Collections.emptyList());
			if (isFreeInt == 1) {
				info.put("nopost_conf", nopostJson);
				return info;
			}

			List<Map<String, Object>> feeConfList = new ArrayList<>();
			List<Map<String, Object>> freeConfList = new ArrayList<>();

			if (isValuationWeightNumberOrVolume(valuation)) {
				Object feeRaw = params.get("fee_conf");
				if (!params.containsKey("fee_conf") || feeRaw == null) {
					throw new BadRequestException("fee_conf 格式错误");
				}
				if (!(feeRaw instanceof List<?> feeList)) {
					throw new BadRequestException("fee_conf 格式错误");
				}
				fillFeeConfWeightNumberVolume(feeConfList, feeList);
			}

			if ("1".equals(valuation) && params.containsKey("free_conf") && params.get("free_conf") != null) {
				Object freeRaw = params.get("free_conf");
				if (!(freeRaw instanceof List<?> freeList)) {
					throw new BadRequestException("free_conf 格式错误");
				}
				fillFreeConfWeight(freeConfList, freeList);
			}

			if ("2".equals(valuation) && params.containsKey("free_conf") && params.get("free_conf") != null) {
				Object freeRaw = params.get("free_conf");
				if (!(freeRaw instanceof List<?> freeList)) {
					throw new BadRequestException("free_conf 格式错误");
				}
				fillFreeConfNumber(freeConfList, freeList);
			}

			if ("3".equals(valuation) && params.containsKey("fee_conf") && params.get("fee_conf") != null) {
				Object feeRaw = params.get("fee_conf");
				if (!(feeRaw instanceof List<?> feeList)) {
					throw new BadRequestException("fee_conf 格式错误");
				}
				fillFeeConfMoney(feeConfList, feeList);
			}

			if ("4".equals(valuation) && params.containsKey("free_conf") && params.get("free_conf") != null) {
				Object freeRaw = params.get("free_conf");
				if (!(freeRaw instanceof List<?> freeList)) {
					throw new BadRequestException("free_conf 格式错误");
				}
				fillFreeConfVolume(freeConfList, freeList);
			}

			info.put("nopost_conf", nopostJson);
			info.put("fee_conf", objectMapper.writeValueAsString(feeConfList));
			info.put("free_conf", objectMapper.writeValueAsString(freeConfList));
			return info;
		} catch (BadRequestException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	private static int parseIsFree(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v != 0 && v != 1) {
				throw new BadRequestException("是否包邮参数存在问题");
			}
			return v;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			int v = Integer.parseInt(s);
			if (v != 0 && v != 1) {
				throw new BadRequestException("是否包邮参数存在问题");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("是否包邮参数存在问题");
		}
	}

	private static int parseStatus(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v != 0 && v != 1) {
				throw new BadRequestException("是否启用参数存在问题");
			}
			return v;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			int v = Integer.parseInt(s);
			if (v != 0 && v != 1) {
				throw new BadRequestException("是否启用参数存在问题");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("是否启用参数存在问题");
		}
	}

	private static String normalizeValuation(Object raw) {
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v < 1 || v > 4) {
				throw new BadRequestException("计价方式参数存在问题");
			}
			return String.valueOf(v);
		}
		String s = String.valueOf(raw).trim();
		if (!s.equals("1") && !s.equals("2") && !s.equals("3") && !s.equals("4")) {
			throw new BadRequestException("计价方式参数存在问题");
		}
		return s;
	}

	private static boolean isValuationWeightNumberOrVolume(String valuation) {
		return "1".equals(valuation) || "2".equals(valuation) || "4".equals(valuation);
	}

	private static boolean isEmptyField(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		return false;
	}

	private void fillFeeConfWeightNumberVolume(List<Map<String, Object>> out, List<?> feeList) {
		for (int k = 0; k < feeList.size(); k++) {
			Object rowObj = feeList.get(k);
			if (!(rowObj instanceof Map<?, ?> rawRow)) {
				throw new BadRequestException("fee_conf 格式错误");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) rawRow;
			if (k == 0) {
				if (isEmptyField(v.get("add_fee")) || isEmptyField(v.get("add_standard"))) {
					throw new BadRequestException("默认运费必填");
				}
				if (isEmptyField(v.get("start_fee")) || isEmptyField(v.get("start_standard"))) {
					throw new BadRequestException("增件运费必填");
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("add_fee", v.get("add_fee"));
				row.put("add_standard", v.get("add_standard"));
				row.put("start_fee", v.get("start_fee"));
				row.put("start_standard", v.get("start_standard"));
				out.add(row);
			} else {
				if (!(v.get("area") instanceof List<?>)) {
					throw new BadRequestException("地区格式错误");
				}
				if (isEmptyField(v.get("add_fee")) || isEmptyField(v.get("add_standard"))) {
					throw new BadRequestException("默认运费必填");
				}
				if (isEmptyField(v.get("start_fee")) || isEmptyField(v.get("start_standard"))) {
					throw new BadRequestException("增件运费必填");
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("area", v.get("area"));
				row.put("add_fee", v.get("add_fee"));
				row.put("add_standard", v.get("add_standard"));
				row.put("start_fee", v.get("start_fee"));
				row.put("start_standard", v.get("start_standard"));
				out.add(row);
			}
		}
	}

	private void fillFreeConfWeight(List<Map<String, Object>> out, List<?> freeList) {
		for (int k = 0; k < freeList.size(); k++) {
			Object rowObj = freeList.get(k);
			if (!(rowObj instanceof Map<?, ?> rawRow)) {
				throw new BadRequestException("free_conf 格式错误");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) rawRow;
			requireFreetype(v.get("freetype"));
			if (k == 0) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("freetype", v.get("freetype"));
				row.put("inweight", v.get("inweight"));
				row.put("upmoney", v.get("upmoney"));
				out.add(row);
			} else {
				if (!(v.get("area") instanceof List<?>)) {
					throw new BadRequestException("地区格式错误");
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("area", v.get("area"));
				row.put("freetype", v.get("freetype"));
				row.put("inweight", v.get("inweight"));
				row.put("upmoney", v.get("upmoney"));
				out.add(row);
			}
		}
	}

	private void fillFreeConfNumber(List<Map<String, Object>> out, List<?> freeList) {
		for (int k = 0; k < freeList.size(); k++) {
			Object rowObj = freeList.get(k);
			if (!(rowObj instanceof Map<?, ?> rawRow)) {
				throw new BadRequestException("free_conf 格式错误");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) rawRow;
			requireFreetype(v.get("freetype"));
			if (k == 0) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("freetype", v.get("freetype"));
				row.put("upquantity", v.get("upquantity"));
				row.put("upmoney", v.get("upmoney"));
				out.add(row);
			} else {
				if (!(v.get("area") instanceof List<?>)) {
					throw new BadRequestException("地区格式错误");
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("area", v.get("area"));
				row.put("freetype", v.get("freetype"));
				row.put("upquantity", v.get("upquantity"));
				row.put("upmoney", v.get("upmoney"));
				out.add(row);
			}
		}
	}

	private void fillFreeConfVolume(List<Map<String, Object>> out, List<?> freeList) {
		for (int k = 0; k < freeList.size(); k++) {
			Object rowObj = freeList.get(k);
			if (!(rowObj instanceof Map<?, ?> rawRow)) {
				throw new BadRequestException("free_conf 格式错误");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) rawRow;
			requireFreetype(v.get("freetype"));
			if (k == 0) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("freetype", v.get("freetype"));
				row.put("upmoney", v.get("upmoney"));
				row.put("upvolume", v.get("upvolume"));
				out.add(row);
			} else {
				if (!(v.get("area") instanceof List<?>)) {
					throw new BadRequestException("地区格式错误");
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("area", v.get("area"));
				row.put("freetype", v.get("freetype"));
				row.put("upmoney", v.get("upmoney"));
				row.put("upvolume", v.get("upvolume"));
				out.add(row);
			}
		}
	}

	private void fillFeeConfMoney(List<Map<String, Object>> out, List<?> feeList) {
		for (int k = 0; k < feeList.size(); k++) {
			Object rowObj = feeList.get(k);
			if (!(rowObj instanceof Map<?, ?> rawRow)) {
				throw new BadRequestException("fee_conf 格式错误");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> v = (Map<String, Object>) rawRow;
			Map<String, Object> block = new LinkedHashMap<>();
			List<Map<String, Object>> rulesOut = new ArrayList<>();
			if (k == 0) {
				block.put("rules", rulesOut);
				out.add(block);
			} else {
				if (!(v.get("area") instanceof List<?>)) {
					throw new BadRequestException("地区格式错误");
				}
				block.put("area", v.get("area"));
				block.put("rules", rulesOut);
				out.add(block);
			}
			Object rulesObj = v.get("rules");
			if (rulesObj == null) {
				continue;
			}
			if (!(rulesObj instanceof List<?> rules)) {
				throw new BadRequestException("fee_conf 格式错误");
			}
			for (Object ruleObj : rules) {
				if (!(ruleObj instanceof Map<?, ?> r0)) {
					throw new BadRequestException("fee_conf 格式错误");
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> v1 = (Map<String, Object>) r0;
				if (!isEmptyField(v1.get("down"))) {
					BigDecimal downBd = parseBigDecimalLenient(v1.get("down"));
					BigDecimal upBd = parseBigDecimalLenient(v1.get("up"));
					if (downBd == null || upBd == null) {
						throw new BadRequestException("fee_conf 格式错误");
					}
					if (downBd.compareTo(upBd) <= 0) {
						throw new BadRequestException("运费金额上下限不合理");
					}
				}
				Object basefeeRaw = v1.get("basefee");
				Object basefeeVal = isEmptyField(basefeeRaw) ? 0 : basefeeRaw;
				Map<String, Object> one = new LinkedHashMap<>();
				one.put("up", v1.get("up"));
				one.put("down", v1.get("down"));
				one.put("basefee", basefeeVal);
				rulesOut.add(one);
			}
		}
	}

	private static void requireFreetype(Object raw) {
		int ft;
		if (raw instanceof Number n) {
			ft = n.intValue();
		} else if (raw == null) {
			throw new BadRequestException("包邮条件参数错误");
		} else {
			try {
				ft = Integer.parseInt(String.valueOf(raw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("包邮条件参数错误");
			}
		}
		if (ft < 1 || ft > 3) {
			throw new BadRequestException("包邮条件参数错误");
		}
	}

	private static BigDecimal parseBigDecimalLenient(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof BigDecimal b) {
			return b;
		}
		if (raw instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean looseNameNotEqual(Object requestName, Object storedName) {
		return !normalizeScalarForLooseName(requestName).equals(normalizeScalarForLooseName(storedName));
	}

	private static String normalizeScalarForLooseName(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private static String formatDecimalPlain(BigDecimal value, int scale) {
		if (value == null) {
			return null;
		}
		return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
	}

	public Map<String, Object> toTemplateDetailRow(ShippingTemplates e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("template_id", e.getTemplateId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("supplier_id", e.getSupplierId());
		m.put("name", e.getName());
		m.put("is_free", e.getIsFree());
		m.put("valuation", e.getValuation());
		m.put("protect", e.getProtect());
		m.put("protect_rate", formatDecimalPlain(e.getProtectRate(), 3));
		m.put("minprice", formatDecimalPlain(e.getMinprice(), 2));
		m.put("status", e.getStatus());
		m.put("fee_conf", e.getFeeConf());
		m.put("nopost_conf", e.getNopostConf());
		m.put("free_conf", e.getFreeConf());
		m.put("create_time", e.getCreateTime());
		m.put("update_time", e.getUpdateTime());
		return m;
	}
}
