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

package cn.shopex.ecshopx.orders.service.companyrelshansong;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CompanyRelShansong;
import cn.shopex.ecshopx.orders.mapper.CompanyRelShansongMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyRelShansongAdminSaveService {

	private final CompanyRelShansongMapper companyRelShansongMapper;

	public CompanyRelShansongAdminSaveService(CompanyRelShansongMapper companyRelShansongMapper) {
		this.companyRelShansongMapper = companyRelShansongMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveInfo(long companyId, Map<String, Object> merged) {
		String shopId = requireNonBlankString(merged.get("shop_id"), "商户ID必填");
		String clientId = requireNonBlankString(merged.get("client_id"), "App-key必填");
		String appSecret = requireNonBlankString(merged.get("app_secret"), "App-密钥必填");

		Object rawOnline = merged.get("online");
		if (isMissingForBooleanField(rawOnline)) {
			throw new BadRequestException("是否上线必填");
		}
		boolean online = parseCoercedBooleanValue(rawOnline, "是否上线类型错误");

		Object rawFreight = merged.get("freight_type");
		if (isMissingForFreightType(rawFreight)) {
			throw new BadRequestException("运费承担方必填");
		}
		boolean freightType = parseCoercedBooleanValue(rawFreight, "运费承担方类型错误");

		Object rawIsOpen = merged.get("is_open");
		if (isMissingForBooleanField(rawIsOpen)) {
			throw new BadRequestException("是否开启必填");
		}
		boolean isOpen = parseCoercedBooleanValue(rawIsOpen, "是否开启参数类型错误");

		CompanyRelShansong existing = companyRelShansongMapper.selectOne(new LambdaQueryWrapper<CompanyRelShansong>()
				.eq(CompanyRelShansong::getCompanyId, companyId)
				.last("LIMIT 1"));

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			CompanyRelShansong row = new CompanyRelShansong();
			row.setCompanyId(companyId);
			row.setShopId(shopId);
			row.setClientId(clientId);
			row.setAppSecret(appSecret);
			row.setOnline(online);
			row.setFreightType(freightType);
			row.setIsOpen(isOpen);
			row.setCreated(now);
			row.setUpdated(now);
			companyRelShansongMapper.insert(row);
		} else {
			boolean dirty = !Objects.equals(existing.getShopId(), shopId)
					|| !Objects.equals(existing.getClientId(), clientId)
					|| !Objects.equals(existing.getAppSecret(), appSecret)
					|| !Objects.equals(existing.getOnline(), online)
					|| !Objects.equals(existing.getFreightType(), freightType)
					|| !Objects.equals(existing.getIsOpen(), isOpen);
			if (dirty) {
				LambdaUpdateWrapper<CompanyRelShansong> uw = new LambdaUpdateWrapper<>();
				uw.eq(CompanyRelShansong::getCompanyId, companyId);
				uw.set(CompanyRelShansong::getShopId, shopId);
				uw.set(CompanyRelShansong::getClientId, clientId);
				uw.set(CompanyRelShansong::getAppSecret, appSecret);
				uw.set(CompanyRelShansong::getOnline, online);
				uw.set(CompanyRelShansong::getFreightType, freightType);
				uw.set(CompanyRelShansong::getIsOpen, isOpen);
				uw.set(CompanyRelShansong::getUpdated, now);
				int n = companyRelShansongMapper.update(null, uw);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
			}
		}

		CompanyRelShansong loaded = companyRelShansongMapper.selectOne(new LambdaQueryWrapper<CompanyRelShansong>()
				.eq(CompanyRelShansong::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (loaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toDataMap(loaded);
	}

	public Map<String, Object> getInfo(long companyId) {
		CompanyRelShansong row = companyRelShansongMapper.selectOne(new LambdaQueryWrapper<CompanyRelShansong>()
				.eq(CompanyRelShansong::getCompanyId, companyId)
				.last("LIMIT 1"));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (row != null) {
			out.putAll(toDataMap(row));
			out.put("online", shansongOnlineOpenToResponseString(row.getOnline()));
			out.put("is_open", shansongOnlineOpenToResponseString(row.getIsOpen()));
		}
		out.put("business_list", new LinkedHashMap<>(ShansongShopBusinessList.asMap()));
		return out;
	}

	private static String shansongOnlineOpenToResponseString(Boolean value) {
		return Boolean.TRUE.equals(value) ? "1" : "0";
	}

	private static Map<String, Object> toDataMap(CompanyRelShansong e) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", e.getId());
		out.put("company_id", e.getCompanyId());
		out.put("shop_id", e.getShopId());
		out.put("client_id", e.getClientId());
		out.put("app_secret", e.getAppSecret());
		out.put("online", e.getOnline());
		out.put("freight_type", e.getFreightType());
		out.put("is_open", e.getIsOpen());
		out.put("created", e.getCreated());
		out.put("updated", e.getUpdated());
		return out;
	}

	private static String requireNonBlankString(Object raw, String missingMsg) {
		if (raw == null) {
			throw new BadRequestException(missingMsg);
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(missingMsg);
		}
		return s;
	}

	private static boolean isMissingForBooleanField(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String str && str.trim().isEmpty()) {
			return true;
		}
		return false;
	}

	private static boolean isMissingForFreightType(Object raw) {
		if (raw == null) {
			return true;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty();
	}

	private static boolean parseCoercedBooleanValue(Object raw, String typeErrorMsg) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number num) {
			int v = num.intValue();
			if (v == 1) {
				return true;
			}
			if (v == 0) {
				return false;
			}
			throw new BadRequestException(typeErrorMsg);
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(typeErrorMsg);
		}
		String lower = s.toLowerCase();
		if ("1".equals(lower) || "true".equals(lower) || "on".equals(lower) || "yes".equals(lower)) {
			return true;
		}
		if ("0".equals(lower) || "false".equals(lower) || "off".equals(lower) || "no".equals(lower)) {
			return false;
		}
		throw new BadRequestException(typeErrorMsg);
	}
}
