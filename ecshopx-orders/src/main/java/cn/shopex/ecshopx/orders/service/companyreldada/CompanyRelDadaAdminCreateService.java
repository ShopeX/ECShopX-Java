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

package cn.shopex.ecshopx.orders.service.companyreldada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.DadaMerchantRegisterBody;
import cn.shopex.ecshopx.common.port.DadaMerchantRegisterPort;
import cn.shopex.ecshopx.orders.domain.CompanyRelDada;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
import cn.shopex.ecshopx.orders.support.ScalarEmptyCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompanyRelDadaAdminCreateService {

	private final CompanyRelDadaMapper companyRelDadaMapper;

	private final CompanyRelDadaCreateParamValidator companyRelDadaCreateParamValidator;

	private final DadaMerchantRegisterPort dadaMerchantRegisterPort;

	public CompanyRelDadaAdminCreateService(
			CompanyRelDadaMapper companyRelDadaMapper,
			CompanyRelDadaCreateParamValidator companyRelDadaCreateParamValidator,
			DadaMerchantRegisterPort dadaMerchantRegisterPort) {
		this.companyRelDadaMapper = companyRelDadaMapper;
		this.companyRelDadaCreateParamValidator = companyRelDadaCreateParamValidator;
		this.dadaMerchantRegisterPort = dadaMerchantRegisterPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createCompanyRelDada(long companyId, Map<String, Object> mergedRaw) {
		NormalizedCompanyRelDadaCreateParams p = companyRelDadaCreateParamValidator.validateAndNormalize(mergedRaw);

		CompanyRelDada row = companyRelDadaMapper.selectOne(new LambdaQueryWrapper<CompanyRelDada>()
				.eq(CompanyRelDada::getCompanyId, companyId)
				.last("LIMIT 1"));
		boolean noRow = row == null;

		String sourceIdPersist = p.getSourceIdOrNull();

		if (ScalarEmptyCompat.isEmpty(p.getStatus()) && ScalarEmptyCompat.isNotEmpty(p.getIsOpen())) {
			boolean needRegister = noRow
					|| !StringUtils.hasText(row.getSourceId())
					|| (looseNotEqual(p.getMobile(), row.getMobile())
							&& looseNotEqual(p.getEmail(), row.getEmail()));
			if (needRegister) {
				DadaMerchantRegisterBody body = new DadaMerchantRegisterBody(
						p.getMobile(),
						p.getCityName(),
						p.getEnterpriseName(),
						p.getEnterpriseAddress(),
						p.getContactName(),
						p.getContactPhone(),
						p.getEmail());
				String existing = noRow ? "" : nullToEmpty(row.getSourceId());
				String newSid = dadaMerchantRegisterPort.registerMerchant(companyId, existing, body);
				sourceIdPersist = newSid;
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000L);

		if (noRow) {
			CompanyRelDada e = new CompanyRelDada();
			e.setCompanyId(companyId);
			e.setSourceId(sourceIdPersist);
			e.setEnterpriseName(p.getEnterpriseName());
			e.setEnterpriseAddress(p.getEnterpriseAddress());
			e.setMobile(p.getMobile());
			e.setCityName(p.getCityName());
			e.setContactName(p.getContactName());
			e.setContactPhone(p.getContactPhone());
			e.setEmail(p.getEmail());
			e.setFreightType(p.getFreightType());
			e.setStatus(p.getStatus());
			e.setIsOpen(p.getIsOpen());
			e.setCreated(now);
			e.setUpdated(now);
			companyRelDadaMapper.insert(e);
			return toSnakeCaseApiMap(e);
		}

		row.setSourceId(sourceIdPersist);
		row.setEnterpriseName(p.getEnterpriseName());
		row.setEnterpriseAddress(p.getEnterpriseAddress());
		row.setMobile(p.getMobile());
		row.setCityName(p.getCityName());
		row.setContactName(p.getContactName());
		row.setContactPhone(p.getContactPhone());
		row.setEmail(p.getEmail());
		row.setFreightType(p.getFreightType());
		row.setStatus(p.getStatus());
		row.setIsOpen(p.getIsOpen());
		row.setUpdated(now);
		int n = companyRelDadaMapper.updateById(row);
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return toSnakeCaseApiMap(row);
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private enum ScalarKind {
		NULL,
		EMPTY,
		TEXT
	}

	private static ScalarKind kind(String x) {
		if (x == null) {
			return ScalarKind.NULL;
		}
		if (x.isEmpty()) {
			return ScalarKind.EMPTY;
		}
		return ScalarKind.TEXT;
	}

	private static boolean weakStringEqual(String l, String rRaw) {
		String r = rRaw == null ? null : rRaw.trim();
		ScalarKind kl = kind(l);
		ScalarKind kr = kind(r);
		if (kl == ScalarKind.NULL && kr == ScalarKind.NULL) {
			return true;
		}
		if (kl == ScalarKind.NULL && kr == ScalarKind.EMPTY) {
			return false;
		}
		if (kl == ScalarKind.NULL && kr == ScalarKind.TEXT) {
			return false;
		}
		if (kl == ScalarKind.EMPTY && kr == ScalarKind.NULL) {
			return false;
		}
		if (kl == ScalarKind.EMPTY && kr == ScalarKind.EMPTY) {
			return true;
		}
		if (kl == ScalarKind.EMPTY && kr == ScalarKind.TEXT) {
			return false;
		}
		if (kl == ScalarKind.TEXT && kr == ScalarKind.NULL) {
			return false;
		}
		if (kl == ScalarKind.TEXT && kr == ScalarKind.EMPTY) {
			return false;
		}
		return l.equals(r);
	}

	private static boolean looseNotEqual(String requestSide, String dbSide) {
		return !weakStringEqual(requestSide, dbSide);
	}

	private static Map<String, Object> toSnakeCaseApiMap(CompanyRelDada entity) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("company_id", entity.getCompanyId());
		m.put("source_id", entity.getSourceId());
		m.put("enterprise_name", entity.getEnterpriseName());
		m.put("enterprise_address", entity.getEnterpriseAddress());
		m.put("mobile", entity.getMobile());
		m.put("city_name", entity.getCityName());
		m.put("contact_name", entity.getContactName());
		m.put("contact_phone", entity.getContactPhone());
		m.put("email", entity.getEmail());
		m.put("freight_type", entity.getFreightType());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		m.put("status", entity.getStatus());
		m.put("is_open", entity.getIsOpen());
		return m;
	}
}
