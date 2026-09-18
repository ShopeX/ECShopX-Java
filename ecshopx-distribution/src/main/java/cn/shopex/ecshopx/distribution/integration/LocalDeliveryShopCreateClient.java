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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class LocalDeliveryShopCreateClient {

	private final JdbcTemplate jdbcTemplate;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${common.local-delivery-dirver:dada}")
	private String localDeliveryDriver;

	@Value("${ecshopx.local-delivery.create-shop-url:}")
	private String createShopUrl;

	@Value("${ecshopx.local-delivery.update-shop-url:}")
	private String updateShopUrl;

	public LocalDeliveryShopCreateClient(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * OpenAPI create：is_dada 为真时校验同城配已开启并远程建店。
	 * 与 admin 差异：无 business 校验；shop_code 始终用 originShopId 覆盖；未开启抛 OpenapiDistributorV2FailException(E5300)。
	 */
	public void applyOpenapiCreateShopIfDada(long companyId, Map<String, Object> merged) {
		if (!truthy(merged.get("is_dada"))) {
			return;
		}
		String driver = localDeliveryDriver != null ? localDeliveryDriver.trim() : "dada";
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}
		Boolean open = readOpenFlag(companyId, driver);
		if (!Boolean.TRUE.equals(open)) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.ORDER_ERROR, "该商户未开启达达同城配");
		}
		if (!StringUtils.hasText(createShopUrl)) {
			throw new ResourceException("同城配创建店铺服务未配置");
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> resp = restTemplate.postForObject(
					createShopUrl,
					Map.of("companyId", companyId, "driver", driver, "shop", merged),
					Map.class);
			if (resp == null || resp.get("originShopId") == null) {
				throw new ResourceException("同城配创建店铺失败");
			}
			String originShopId = resp.get("originShopId").toString();
			merged.put("shop_code", originShopId);
			if ("shansong".equals(driver)) {
				merged.put("shansong_shop_create", Boolean.TRUE);
				try {
					merged.put("shansong_store_id", Long.parseLong(originShopId.trim()));
				} catch (NumberFormatException ex) {
					merged.put("shansong_store_id", 0L);
				}
			} else {
				merged.put("dada_shop_create", Boolean.TRUE);
			}
		} catch (OpenapiDistributorV2FailException | ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("同城配创建店铺失败");
		}
	}

	public void applyCreateShopIfDada(long companyId, Map<String, Object> merged) {
		if (!truthy(merged.get("is_dada"))) {
			return;
		}
		String driver = localDeliveryDriver != null ? localDeliveryDriver.trim() : "dada";
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}
		Boolean open = readOpenFlag(companyId, driver);
		if (!Boolean.TRUE.equals(open)) {
			throw new ResourceException("同城配未开启");
		}
		Object business = merged.get("business");
		if (business == null || !StringUtils.hasText(business.toString())) {
			throw new ResourceException("请选择业务类型");
		}
		if (!StringUtils.hasText(createShopUrl)) {
			throw new ResourceException("同城配创建店铺服务未配置");
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> resp = restTemplate.postForObject(createShopUrl, Map.of("companyId", companyId, "driver", driver, "shop", merged), Map.class);
			if (resp == null || resp.get("originShopId") == null) {
				throw new ResourceException("同城配创建店铺失败");
			}
			String originShopId = resp.get("originShopId").toString();
			Object existingCode = merged.get("shop_code");
			if (existingCode == null || !StringUtils.hasText(existingCode.toString())) {
				merged.put("shop_code", originShopId);
			}
			if ("shansong".equals(driver)) {
				merged.put("shansong_shop_create", Boolean.TRUE);
				try {
					merged.put("shansong_store_id", Long.parseLong(originShopId.trim()));
				} catch (NumberFormatException ex) {
					merged.put("shansong_store_id", 0L);
				}
			} else {
				merged.put("dada_shop_create", Boolean.TRUE);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("同城配创建店铺失败");
		}
	}

	/**
	 * Update flow when {@code shop_code} is present: open check, name uniqueness, shop_code lock after remote create,
	 * optional create/update remote shop, flags on {@code merged}.
	 */
	public void applyUpdateForDistributor(long companyId, long distributorId, Map<String, Object> merged, Distributor existing) {
		if (truthy(merged.get("is_dada"))) {
			Boolean open = readOpenFlag(companyId, driver());
			if (!Boolean.TRUE.equals(open)) {
				throw new ResourceException("该商户未开启同城配");
			}
		}
		String shopCode = str(merged.get("shop_code"));
		if (!StringUtils.hasText(shopCode)) {
			return;
		}
		boolean hadDada = Boolean.TRUE.equals(existing.getDadaShopCreate());
		boolean hadShansong = Boolean.TRUE.equals(existing.getShansongShopCreate());
		if (hadDada || hadShansong) {
			merged.put("shop_code", existing.getShopCode() != null ? existing.getShopCode() : "");
			if (existing.getShansongStoreId() != null) {
				merged.put("shansong_store_id", existing.getShansongStoreId());
			}
		}
		if (!truthy(merged.get("is_dada"))) {
			merged.put("is_dada", Boolean.FALSE);
			if (existing.getBusiness() != null) {
				merged.put("business", existing.getBusiness());
			}
			return;
		}
		Object business = merged.get("business");
		if (business == null || !StringUtils.hasText(business.toString())) {
			throw new ResourceException("请选择业务类型");
		}
		String driver = driver();
		boolean createdForDriver = "shansong".equals(driver) ? hadShansong : hadDada;
		if (!createdForDriver) {
			applyCreateShopIfDada(companyId, merged);
			return;
		}
		if (!StringUtils.hasText(updateShopUrl)) {
			throw new ResourceException("同城配更新店铺服务未配置");
		}
		try {
			merged.put("distributor_id", distributorId);
			@SuppressWarnings("unchecked")
			Map<String, Object> resp = restTemplate.postForObject(
					updateShopUrl,
					Map.of("companyId", companyId, "driver", driver, "shop", merged, "distributorId", distributorId),
					Map.class);
			if (resp == null) {
				throw new ResourceException("同城配更新店铺失败");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("同城配更新店铺失败");
		}
	}

	private String driver() {
		String d = localDeliveryDriver != null ? localDeliveryDriver.trim() : "dada";
		return "shansong".equals(d) ? "shansong" : "dada";
	}

	private Boolean readOpenFlag(long companyId, String driver) {
		try {
			if ("shansong".equals(driver)) {
				return jdbcTemplate.query(
						"SELECT is_open FROM company_rel_shansong WHERE company_id = ? LIMIT 1",
						rs -> rs.next() ? rs.getBoolean("is_open") : Boolean.FALSE,
						companyId);
			}
			return jdbcTemplate.query(
					"SELECT is_open FROM company_rel_dada WHERE company_id = ? LIMIT 1",
					rs -> rs.next() ? rs.getBoolean("is_open") : Boolean.FALSE,
					companyId);
		} catch (Exception e) {
			return Boolean.FALSE;
		}
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		if ("false".equalsIgnoreCase(s) || "0".equals(s) || s.isEmpty()) {
			return false;
		}
		return true;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
