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

package cn.shopex.ecshopx.orders.service.freight;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.service.address.EspierRegionLabelResolveService;
import cn.shopex.ecshopx.orders.domain.dto.ShippingTemplateRow;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Freight calculation aligned with legacy shipping template rules (weight / count / money / volume).
 */
@Service
public class NormalOrderShippingFreightCountService {

	private static final int BC_VOLUME_SCALE = 4;
	private static final String STATUS_FEE = "0";

	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final EspierRegionLabelResolveService espierRegionLabelResolveService;
	private final ObjectMapper objectMapper;

	public NormalOrderShippingFreightCountService(
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			EspierRegionLabelResolveService espierRegionLabelResolveService,
			ObjectMapper objectMapper) {
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.espierRegionLabelResolveService = espierRegionLabelResolveService;
		this.objectMapper = objectMapper;
	}

	public long countFreightFee(
			List<Map<String, Object>> freightItemRows,
			long companyId,
			String receiverStateLabel,
			String receiverCityLabel,
			String receiverDistrictLabel,
			boolean lastArgTrue) {
		return countFreightFee(
				freightItemRows,
				companyId,
				receiverStateLabel,
				receiverCityLabel,
				receiverDistrictLabel,
				lastArgTrue,
				null);
	}

	public long countFreightFee(
			List<Map<String, Object>> freightItemRows,
			long companyId,
			String receiverStateLabel,
			String receiverCityLabel,
			String receiverDistrictLabel,
			boolean lastArgTrue,
			Map<Integer, Integer> supplierFreightOut) {
		if (!StringUtils.hasText(receiverStateLabel)
				|| !StringUtils.hasText(receiverCityLabel)
				|| !StringUtils.hasText(receiverDistrictLabel)) {
			return 0L;
		}
		int[] area =
				espierRegionLabelResolveService.resolveIds(
						receiverStateLabel, receiverCityLabel, receiverDistrictLabel);
		int provinceId = area[0];
		int cityId = area[1];
		int districtId = area[2];

		Map<Long, Agg> grouped = new HashMap<>();
		for (Map<String, Object> row : freightItemRows) {
			Object tidObj = row.get("templates_id");
			if (tidObj == null) {
				continue;
			}
			long templateKey = toLong(tidObj);
			if (templateKey == 0L) {
				continue;
			}
			Agg agg = grouped.computeIfAbsent(templateKey, k -> new Agg());
			double w = 0.0;
			Object wObj = row.get("weight");
			if (wObj instanceof Number n) {
				w = n.doubleValue();
			}
			agg.weight += w;
			long lineFee = 0L;
			Object tf = row.get("total_fee");
			if (tf instanceof Number n) {
				lineFee = n.longValue();
			}
			agg.priceSumCents += lineFee;
			int num = 0;
			Object numObj = row.get("num");
			if (numObj instanceof Number n) {
				num = n.intValue();
			}
			agg.num += num;
			BigDecimal vol = BigDecimal.ZERO;
			Object volObj = row.get("volume");
			if (volObj instanceof Number n) {
				vol = BigDecimal.valueOf(n.longValue());
			}
			agg.volumeSum = agg.volumeSum.add(vol).setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
		}

		if (grouped.isEmpty()) {
			return 0L;
		}
		if (provinceId == 0 || cityId == 0) {
			return 0L;
		}

		long totalCents = 0L;
		for (Map.Entry<Long, Agg> e : grouped.entrySet()) {
			long templateId = e.getKey();
			Agg v = e.getValue();
			Optional<ShippingTemplateRow> opt = shippingTemplatesQueryRepository.selectTemplateById(companyId, templateId);
			if (opt.isEmpty()) {
				continue;
			}
			ShippingTemplateRow tpl = opt.get();
			if (lastArgTrue) {
				checkNopost(tpl, provinceId, cityId, districtId);
			}
			if (Boolean.FALSE.equals(tpl.getStatus())) {
				continue;
			}
			if (!STATUS_FEE.equals(tpl.getIsFree())) {
				continue;
			}
			int valuation = parseValuation(tpl.getValuation());
			double templateYuan =
					switch (valuation) {
						case 1 -> sumFeeByWeight(tpl, provinceId, cityId, districtId, v.weight, v.priceSumCents);
						case 2 -> sumFeeByNumber(tpl, provinceId, cityId, districtId, v.num, v.priceSumCents);
						case 3 -> sumFeeByPrice(tpl, provinceId, cityId, districtId, v.priceSumCents);
						case 4 -> sumFeeByVolume(tpl, provinceId, cityId, districtId, v.volumeSum, v.priceSumCents);
						default -> 0.0;
					};
			totalCents += yuanToCents(templateYuan);
			if (supplierFreightOut != null) {
				int sid = tpl.getSupplierId() == null ? 0 : tpl.getSupplierId().intValue();
				if (sid > 0) {
					supplierFreightOut.merge(sid, (int) yuanToCents(templateYuan), Integer::sum);
				}
			}
		}
		return totalCents;
	}

	private static long yuanToCents(double yuan) {
		return BigDecimal.valueOf(yuan)
				.multiply(BigDecimal.valueOf(100))
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact();
	}

	private void checkNopost(ShippingTemplateRow tpl, int provinceId, int cityId, int districtId) {
		String raw = tpl.getNopostConf();
		if (!StringUtils.hasText(raw)) {
			return;
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (Exception e) {
			return;
		}
		if (root == null || !root.isArray()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (JsonNode n : root) {
			if (n.isIntegralNumber()) {
				ids.add(n.longValue());
			} else if (n.isTextual()) {
				try {
					ids.add(Long.parseLong(n.asText().trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		if (ids.contains((long) provinceId) || ids.contains((long) cityId) || ids.contains((long) districtId)) {
			throw new ResourceException("该地区不进行配送，如有疑问请联系商家");
		}
	}

	private static int parseValuation(String v) {
		if (!StringUtils.hasText(v)) {
			return 0;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean areaIntersects(JsonNode areaNode, int provinceId, int cityId, int districtId) {
		if (areaNode == null || !areaNode.isArray() || areaNode.isEmpty()) {
			return false;
		}
		for (JsonNode n : areaNode) {
			long id;
			if (n.isIntegralNumber()) {
				id = n.longValue();
			} else {
				try {
					id = Long.parseLong(n.asText().trim());
				} catch (Exception e) {
					continue;
				}
			}
			if (id == provinceId || id == cityId || id == districtId) {
				return true;
			}
		}
		return false;
	}

	private static BigDecimal bd(String s) {
		if (!StringUtils.hasText(s)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(s.trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static long yuanNodeToCents(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0L;
		}
		if (n.isNumber()) {
			return yuanToCents(n.doubleValue());
		}
		String t = n.asText("");
		if (!StringUtils.hasText(t)) {
			return 0L;
		}
		return yuanToCents(bd(t).doubleValue());
	}

	private static int bcComp(BigDecimal a, BigDecimal b) {
		return a.compareTo(b);
	}

	private JsonNode parseConfigArray(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			JsonNode n = objectMapper.readTree(json);
			return n != null && n.isArray() ? n : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static double round2(double v) {
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	private double sumFeeByWeight(
			ShippingTemplateRow tpl,
			int provinceId,
			int cityId,
			int districtId,
			double weight,
			long priceCents) {
		JsonNode confFree = parseConfigArray(tpl.getFreeConf());
		if (confFree != null) {
			for (int k = 1; k < confFree.size(); k++) {
				JsonNode v = confFree.get(k);
				if (v == null || !v.isObject()) {
					continue;
				}
				JsonNode area = v.get("area");
				if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
					int freetype = v.path("freetype").asInt(0);
					String inw = v.path("inweight").asText("");
					BigDecimal wBd = BigDecimal.valueOf(weight).setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
					if (freetype == 1) {
						if (StringUtils.hasText(inw) && bcComp(bd(inw), wBd) >= 0) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanNodeToCents(v.get("upmoney"))) {
							return 0.0;
						}
					} else {
						if (StringUtils.hasText(inw)
								&& priceCents >= yuanNodeToCents(v.get("upmoney"))
								&& bcComp(bd(inw), wBd) >= 0) {
							return 0.0;
						}
					}
				}
			}
			JsonNode national = confFree.get(0);
			if (national != null && national.isObject()) {
				double inweight = national.path("inweight").asDouble(0);
				double upmoney = national.path("upmoney").asDouble(0);
				if (inweight > 0 || upmoney > 0) {
					int freetype = national.path("freetype").asInt(0);
					BigDecimal wBd = BigDecimal.valueOf(weight).setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
					if (freetype == 1) {
						if (bcComp(BigDecimal.valueOf(inweight), wBd) >= 0) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					} else {
						if (bcComp(BigDecimal.valueOf(inweight), wBd) >= 0
								&& priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					}
				}
			}
		}

		JsonNode confFee = parseConfigArray(tpl.getFeeConf());
		if (confFee == null || confFee.isEmpty()) {
			return 0.0;
		}
		double fee = 0.0;
		for (int k = 1; k < confFee.size(); k++) {
			JsonNode v = confFee.get(k);
			if (v == null || !v.isObject()) {
				continue;
			}
			JsonNode area = v.get("area");
			if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
				fee += v.path("start_fee").asDouble(0.0);
				BigDecimal wBd = BigDecimal.valueOf(weight).setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
				BigDecimal startStd = bd(v.path("start_standard").asText("0"));
				double addStd = v.path("add_standard").asDouble(0.0);
				if (bcComp(wBd, startStd) > 0 && addStd > 0) {
					BigDecimal delta = wBd.subtract(startStd);
					BigDecimal units =
							delta.divide(bd(String.valueOf(addStd)), BC_VOLUME_SCALE, RoundingMode.HALF_UP);
					long u = units.setScale(0, RoundingMode.CEILING).longValue();
					fee += u * v.path("add_fee").asDouble(0.0);
				}
				return round2(fee);
			}
		}
		JsonNode def = confFee.get(0);
		if (def == null || !def.isObject()) {
			return 0.0;
		}
		fee += def.path("start_fee").asDouble(0.0);
		BigDecimal wBd = BigDecimal.valueOf(weight).setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
		BigDecimal startStd = bd(def.path("start_standard").asText("0"));
		double addStd = def.path("add_standard").asDouble(0.0);
		if (bcComp(wBd, startStd) > 0 && addStd > 0) {
			BigDecimal delta = wBd.subtract(startStd);
			BigDecimal units = delta.divide(bd(String.valueOf(addStd)), BC_VOLUME_SCALE, RoundingMode.HALF_UP);
			long u = units.setScale(0, RoundingMode.CEILING).longValue();
			fee += u * def.path("add_fee").asDouble(0.0);
		}
		return round2(fee);
	}

	private double sumFeeByNumber(
			ShippingTemplateRow tpl,
			int provinceId,
			int cityId,
			int districtId,
			int number,
			long priceCents) {
		JsonNode confFree = parseConfigArray(tpl.getFreeConf());
		if (confFree != null) {
			for (int k = 1; k < confFree.size(); k++) {
				JsonNode v = confFree.get(k);
				if (v == null || !v.isObject()) {
					continue;
				}
				JsonNode area = v.get("area");
				if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
					int freetype = v.path("freetype").asInt(0);
					String upqStr = v.path("upquantity").asText("");
					int upq = parseIntNode(v.get("upquantity"));
					if (freetype == 1) {
						if (StringUtils.hasText(upqStr) && number >= upq) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanNodeToCents(v.get("upmoney"))) {
							return 0.0;
						}
					} else {
						if (StringUtils.hasText(upqStr)
								&& number >= upq
								&& priceCents >= yuanNodeToCents(v.get("upmoney"))) {
							return 0.0;
						}
					}
				}
			}
			JsonNode national = confFree.get(0);
			if (national != null && national.isObject()) {
				int upq = national.path("upquantity").asInt(0);
				double upmoney = national.path("upmoney").asDouble(0);
				if (upq > 0 || upmoney > 0) {
					int freetype = national.path("freetype").asInt(0);
					if (freetype == 1) {
						if (number >= upq) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					} else {
						if (number >= upq && priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					}
				}
			}
		}

		JsonNode confFee = parseConfigArray(tpl.getFeeConf());
		if (confFee == null || confFee.isEmpty()) {
			return 0.0;
		}
		double fee = 0.0;
		for (int k = 1; k < confFee.size(); k++) {
			JsonNode v = confFee.get(k);
			if (v == null || !v.isObject()) {
				continue;
			}
			JsonNode area = v.get("area");
			if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
				fee += v.path("start_fee").asDouble(0.0);
				int startStd = parseIntNode(v.get("start_standard"));
				int addStd = parseIntNode(v.get("add_standard"));
				if (number > startStd && addStd > 0) {
					int delta = number - startStd;
					long u = (long) Math.ceil((double) delta / (double) addStd);
					fee += u * v.path("add_fee").asDouble(0.0);
				}
				return round2(fee);
			}
		}
		JsonNode def = confFee.get(0);
		if (def == null || !def.isObject()) {
			return 0.0;
		}
		fee += def.path("start_fee").asDouble(0.0);
		int startStd = parseIntNode(def.get("start_standard"));
		int addStd = parseIntNode(def.get("add_standard"));
		if (number > startStd && addStd > 0) {
			int delta = number - startStd;
			long u = (long) Math.ceil((double) delta / (double) addStd);
			fee += u * def.path("add_fee").asDouble(0.0);
		}
		return round2(fee);
	}

	private static int parseIntNode(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0;
		}
		if (n.isInt() || n.isLong()) {
			return n.asInt();
		}
		try {
			return Integer.parseInt(n.asText().trim());
		} catch (Exception e) {
			return 0;
		}
	}

	private double sumFeeByPrice(
			ShippingTemplateRow tpl, int provinceId, int cityId, int districtId, long priceCents) {
		JsonNode confFee = parseConfigArray(tpl.getFeeConf());
		if (confFee == null || confFee.isEmpty()) {
			return 0.0;
		}
		for (int k = 1; k < confFee.size(); k++) {
			JsonNode v = confFee.get(k);
			if (v == null || !v.isObject()) {
				continue;
			}
			JsonNode area = v.get("area");
			if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
				JsonNode rules = v.get("rules");
				if (rules != null && rules.isArray()) {
					for (JsonNode v1 : rules) {
						long upC = yuanNodeToCents(v1.get("up"));
						long downC = yuanNodeToCents(v1.get("down"));
						if (priceCents >= upC && (downC == 0L || priceCents < downC)) {
							return round2(v1.path("basefee").asDouble(0.0));
						}
					}
				}
			}
		}
		JsonNode defRules = confFee.get(0);
		if (defRules != null && defRules.isObject()) {
			JsonNode rules = defRules.get("rules");
			if (rules != null && rules.isArray()) {
				for (JsonNode v : rules) {
					long upC = yuanNodeToCents(v.get("up"));
					long downC = yuanNodeToCents(v.get("down"));
					if (priceCents >= upC && (downC == 0L || priceCents < downC)) {
						double bf = v.path("basefee").asDouble(0.0);
						return round2(bf);
					}
				}
			}
		}
		return 0.0;
	}

	private double sumFeeByVolume(
			ShippingTemplateRow tpl,
			int provinceId,
			int cityId,
			int districtId,
			BigDecimal volume,
			long priceCents) {
		JsonNode confFree = parseConfigArray(tpl.getFreeConf());
		BigDecimal vol = volume.setScale(BC_VOLUME_SCALE, RoundingMode.HALF_UP);
		if (confFree != null) {
			for (int k = 1; k < confFree.size(); k++) {
				JsonNode v = confFree.get(k);
				if (v == null || !v.isObject()) {
					continue;
				}
				JsonNode area = v.get("area");
				if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
					int freetype = v.path("freetype").asInt(0);
					String upvol = v.path("upvolume").asText("");
					if (freetype == 1) {
						if (StringUtils.hasText(upvol) && bcComp(bd(upvol), vol) >= 0) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanNodeToCents(v.get("upmoney"))) {
							return 0.0;
						}
					} else {
						if (StringUtils.hasText(upvol)
								&& bcComp(bd(upvol), vol) >= 0
								&& priceCents >= yuanNodeToCents(v.get("upmoney"))) {
							return 0.0;
						}
					}
				}
			}
			JsonNode national = confFree.get(0);
			if (national != null && national.isObject()) {
				double upvol = national.path("upvolume").asDouble(0);
				double upmoney = national.path("upmoney").asDouble(0);
				if (upvol > 0 || upmoney > 0) {
					int freetype = national.path("freetype").asInt(0);
					if (freetype == 1) {
						if (bcComp(BigDecimal.valueOf(upvol), vol) >= 0) {
							return 0.0;
						}
					} else if (freetype == 2) {
						if (priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					} else {
						if (bcComp(BigDecimal.valueOf(upvol), vol) >= 0
								&& priceCents >= yuanToCents(upmoney)) {
							return 0.0;
						}
					}
				}
			}
		}

		JsonNode confFee = parseConfigArray(tpl.getFeeConf());
		if (confFee == null || confFee.isEmpty()) {
			return 0.0;
		}
		double fee = 0.0;
		for (int k = 1; k < confFee.size(); k++) {
			JsonNode v = confFee.get(k);
			if (v == null || !v.isObject()) {
				continue;
			}
			JsonNode area = v.get("area");
			if (area != null && areaIntersects(area, provinceId, cityId, districtId)) {
				fee += v.path("start_fee").asDouble(0.0);
				BigDecimal startStd = bd(v.path("start_standard").asText("0"));
				double addStd = v.path("add_standard").asDouble(0.0);
				if (bcComp(vol, startStd) > 0 && addStd > 0) {
					BigDecimal delta = vol.subtract(startStd);
					BigDecimal units =
							delta.divide(bd(String.valueOf(addStd)), BC_VOLUME_SCALE, RoundingMode.HALF_UP);
					long u = units.setScale(0, RoundingMode.CEILING).longValue();
					fee += u * v.path("add_fee").asDouble(0.0);
				}
				return round2(fee);
			}
		}
		JsonNode def = confFee.get(0);
		if (def == null || !def.isObject()) {
			return 0.0;
		}
		fee += def.path("start_fee").asDouble(0.0);
		BigDecimal startStd = bd(def.path("start_standard").asText("0"));
		double addStd = def.path("add_standard").asDouble(0.0);
		if (bcComp(vol, startStd) > 0 && addStd > 0) {
			BigDecimal delta = vol.subtract(startStd);
			BigDecimal units = delta.divide(bd(String.valueOf(addStd)), BC_VOLUME_SCALE, RoundingMode.HALF_UP);
			long u = units.setScale(0, RoundingMode.CEILING).longValue();
			fee += u * def.path("add_fee").asDouble(0.0);
		}
		return round2(fee);
	}

	private static final class Agg {
		double weight;
		long priceSumCents;
		int num;
		BigDecimal volumeSum = BigDecimal.ZERO;
	}
}
