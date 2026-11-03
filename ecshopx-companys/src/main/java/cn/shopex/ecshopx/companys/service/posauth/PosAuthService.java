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

package cn.shopex.ecshopx.companys.service.posauth;

import cn.shopex.ecshopx.common.auth.PosJwtIssuerPort;
import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** ECX-10532：POS 终端登录（员工号 + 密码 → 独立 POS JWT）。 */
@Service
public class PosAuthService {

	/** 启用 / 禁用（含历史 1/0）可登录；闭店 closed、撤店 delete 不可登录。 */
	private static final Set<String> LOGIN_ALLOWED_IS_VALID = Set.of("true", "false", "1", "0");

	private final PosStoreAccountMapper mapper;
	private final DistributionDistributorSelfReadMapper shopMapper;
	private final SensitiveFieldEncryptor encryptor;
	private final PosJwtIssuerPort posJwtIssuerPort;
	private final MessageSource messageSource;

	public PosAuthService(
			PosStoreAccountMapper mapper,
			DistributionDistributorSelfReadMapper shopMapper,
			SensitiveFieldEncryptor encryptor,
			PosJwtIssuerPort posJwtIssuerPort,
			MessageSource messageSource) {
		this.mapper = mapper;
		this.shopMapper = shopMapper;
		this.encryptor = encryptor;
		this.posJwtIssuerPort = posJwtIssuerPort;
		this.messageSource = messageSource;
	}

	public Map<String, Object> login(String employeeNoRaw, String passwordRaw) {
		String employeeNo = employeeNoRaw == null ? "" : employeeNoRaw.trim();
		if (!StringUtils.hasText(employeeNo) || employeeNo.length() > 30) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		if (!StringUtils.hasText(passwordRaw)) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		LambdaQueryWrapper<PosStoreAccount> w = new LambdaQueryWrapper<>();
		w.eq(PosStoreAccount::getEmployeeNo, employeeNo);
		List<PosStoreAccount> rows = mapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		if (rows.size() > 1) {
			throw new UnauthorizedException(PosAuthMessages.accountDataAbnormal(messageSource));
		}
		PosStoreAccount row = rows.get(0);
		String plain;
		try {
			plain = encryptor.decryptAlways(row.getPasswordCipher());
		} catch (RuntimeException e) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		if (!passwordRaw.equals(plain)) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		if ("disabled".equals(row.getStatus())) {
			throw new UnauthorizedException(PosAuthMessages.accountDisabled(messageSource));
		}
		if ("resigned".equals(row.getStatus())) {
			throw new UnauthorizedException(PosAuthMessages.accountResigned(messageSource));
		}
		if (!"enabled".equals(row.getStatus())) {
			throw new UnauthorizedException(PosAuthMessages.badCredentials(messageSource));
		}
		assertShopAllowsLogin(row);
		Map<String, Object> claims = claimsFrom(row);
		String token = posJwtIssuerPort.issueToken(claims);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("token", token);
		out.put("id", row.getId());
		out.put("employee_no", row.getEmployeeNo());
		out.put("name", blank(row.getName()));
		out.put("role", row.getRole());
		out.put("company_id", row.getCompanyId());
		out.put("distributor_id", row.getDistributorId());
		out.put("status", row.getStatus());
		return out;
	}

	private void assertShopAllowsLogin(PosStoreAccount row) {
		Long companyId = row.getCompanyId();
		Long distributorId = row.getDistributorId();
		if (companyId == null || companyId <= 0L || distributorId == null || distributorId <= 0L) {
			throw new UnauthorizedException(PosAuthMessages.shopMissing(messageSource));
		}
		Map<String, Object> shop = shopMapper.selectShopByCompanyAndId(companyId, distributorId);
		if (shop == null || shop.isEmpty()) {
			throw new UnauthorizedException(PosAuthMessages.shopMissing(messageSource));
		}
		String isValid = normalizeIsValid(shop.get("is_valid"));
		if (LOGIN_ALLOWED_IS_VALID.contains(isValid)) {
			return;
		}
		if ("closed".equals(isValid)) {
			throw new UnauthorizedException(PosAuthMessages.shopClosed(messageSource));
		}
		if ("delete".equals(isValid)) {
			throw new UnauthorizedException(PosAuthMessages.shopDeleted(messageSource));
		}
		throw new UnauthorizedException(PosAuthMessages.shopMissing(messageSource));
	}

	private static String normalizeIsValid(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Boolean b) {
			return b ? "true" : "false";
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v == 1) {
				return "1";
			}
			if (v == 0) {
				return "0";
			}
		}
		return String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
	}

	static Map<String, Object> claimsFrom(PosStoreAccount row) {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put(PosJwtRequestAttributes.CLAIM_TOKEN_KIND, PosJwtRequestAttributes.TOKEN_KIND_POS);
		claims.put("id", row.getId());
		claims.put("pos_store_account_id", row.getId());
		claims.put("employee_no", row.getEmployeeNo());
		// DigiOS acc / 业务侧 employee_id 与员工号同值
		claims.put("employee_id", row.getEmployeeNo());
		claims.put("name", blank(row.getName()));
		claims.put("role", row.getRole());
		claims.put("company_id", row.getCompanyId());
		claims.put("distributor_id", row.getDistributorId());
		claims.put("status", row.getStatus());
		return claims;
	}

	private static String blank(String s) {
		return s == null ? "" : s;
	}
}
