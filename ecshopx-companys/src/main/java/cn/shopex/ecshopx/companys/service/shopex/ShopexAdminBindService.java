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

package cn.shopex.ecshopx.companys.service.shopex;

import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.TooManyRequestsException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthService;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthTokenResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 本地 admin 绑定 Shopex 通行证（对齐 PHP {@code ShopexAdminBindService}）。
 */
@Service
public class ShopexAdminBindService {

	private static final Logger log = LoggerFactory.getLogger(ShopexAdminBindService.class);

	private static final String RATE_KEY_PREFIX = "admin_shopex_bind_failed_times:";
	private static final int RATE_LIMIT_MAX = 5;
	private static final long RATE_LIMIT_TTL_SECONDS = 1800L;

	private final OperatorsMapper operatorsMapper;
	private final CompanysMapper companysMapper;
	private final StringRedisTemplate prismRedisTemplate;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PrismOAuthService prismOAuthService;
	private final ShopexSmsClientFacade shopexSmsClientFacade;
	private final ShopexOauthCertSyncService shopexOauthCertSyncService;

	public ShopexAdminBindService(
			OperatorsMapper operatorsMapper,
			CompanysMapper companysMapper,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			PrismOAuthService prismOAuthService,
			ShopexSmsClientFacade shopexSmsClientFacade,
			ShopexOauthCertSyncService shopexOauthCertSyncService) {
		this.operatorsMapper = operatorsMapper;
		this.companysMapper = companysMapper;
		this.prismRedisTemplate = prismRedisTemplate;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.prismOAuthService = prismOAuthService;
		this.shopexSmsClientFacade = shopexSmsClientFacade;
		this.shopexOauthCertSyncService = shopexOauthCertSyncService;
	}

	/**
	 * operators 三列非空且 prism SaasCert JSON 三键非空。
	 */
	public boolean isOperatorShopexBound(Operators operator) {
		if (operator == null) {
			return false;
		}
		String passportUid = trimToEmpty(operator.getPassportUid());
		String eid = trimToEmpty(operator.getEid());
		String bindAccount = trimToEmpty(operator.getShopexBindAccount());
		if (passportUid.isEmpty() || eid.isEmpty() || bindAccount.isEmpty()) {
			return false;
		}
		Long companyId = operator.getCompanyId();
		if (companyId == null) {
			return false;
		}
		String[] triple = saasCertTripleFromRedis(companyId, passportUid);
		return !triple[0].isEmpty() && !triple[1].isEmpty() && !triple[2].isEmpty();
	}

	/**
	 * 读取 {@code prism:sha1(U+'_'+C+'_SaasCert')} 中证书三字段。
	 *
	 * @return cert_id, node_id, token（已 trim）
	 */
	public String[] saasCertTripleFromRedis(long companyId, String passportUid) {
		String key = "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
		String raw = prismRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return new String[] {"", "", ""};
		}
		try {
			JsonNode cert = objectMapper.readTree(raw);
			return new String[] {
				trimToEmpty(text(cert, "cert_id")),
				trimToEmpty(text(cert, "node_id")),
				trimToEmpty(text(cert, "token"))
			};
		} catch (Exception e) {
			return new String[] {"", "", ""};
		}
	}

	public Map<String, Object> getStatusForOperatorId(long operatorId) {
		Operators operator = operatorsMapper.selectById(operatorId);
		if (operator == null) {
			throw new ResourceException("操作员不存在");
		}
		return Map.of("bound", isOperatorShopexBound(operator));
	}

	/**
	 * 已登录本地 admin 绑定 Shopex（Prism 密码模式）；限流键独立于 admin_login_failed_times。
	 */
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> bindForAdminOperator(long operatorId, Map<String, Object> credentials) {
		Operators operator = operatorsMapper.selectById(operatorId);
		if (operator == null) {
			throw new ResourceException("操作员不存在");
		}
		if (!"admin".equals(trimToEmpty(operator.getOperatorType()))) {
			throw new ForbiddenException("仅商家超级管理员可绑定 Shopex");
		}
		if (isOperatorShopexBound(operator)) {
			throw new ConflictException("已绑定 Shopex，无需重复绑定");
		}

		String rateKey = RATE_KEY_PREFIX + operatorId;
		Long failedTimes = companysRedisTemplate.opsForValue().increment(rateKey);
		companysRedisTemplate.expire(rateKey, RATE_LIMIT_TTL_SECONDS, TimeUnit.SECONDS);
		if (failedTimes != null && failedTimes > RATE_LIMIT_MAX) {
			throw new TooManyRequestsException("绑定失败次数过多，请30分钟后再试");
		}

		String username = trimToEmpty(credentials == null ? null : credentials.get("username"));
		String password = trimToEmpty(credentials == null ? null : credentials.get("password"));
		if (username.isEmpty() || password.isEmpty()) {
			throw new ResourceException("请填写 Shopex 账号与密码");
		}

		PrismOAuthTokenResult token;
		try {
			token = prismOAuthService.exchangePasswordGrant(username, password);
		} catch (RuntimeException e) {
			log.warn(
					"shopex_bind_prism_failed operator_id={} message={}", operatorId, e.getMessage());
			throw e;
		}

		Map<String, String> data = token.data() == null ? Map.of() : token.data();
		String newPassportUid = trimToEmpty(data.get("passport_uid"));
		String newEid = trimToEmpty(data.get("eid"));
		String shopexid = trimToEmpty(data.get("shopexid"));
		if (newPassportUid.isEmpty() || newEid.isEmpty() || shopexid.isEmpty()) {
			throw new ResourceException("Prism 返回数据不完整，绑定失败");
		}

		LambdaQueryWrapper<Operators> occupiedQ = new LambdaQueryWrapper<>();
		occupiedQ.eq(Operators::getPassportUid, newPassportUid).last("LIMIT 1");
		Operators occupied = operatorsMapper.selectOne(occupiedQ);
		if (occupied != null
				&& occupied.getOperatorId() != null
				&& occupied.getOperatorId() != operatorId) {
			throw new ResourceException("该 Shopex 通行证已被其他管理员账号占用");
		}

		String mobileBefore = operator.getMobile();
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Operators> opUpdate = new LambdaUpdateWrapper<>();
		opUpdate
				.eq(Operators::getOperatorId, operatorId)
				.set(Operators::getPassportUid, newPassportUid)
				.set(Operators::getEid, newEid)
				.set(Operators::getShopexBindAccount, shopexid)
				.set(Operators::getUpdated, now);
		operatorsMapper.update(null, opUpdate);

		if (operator.getCompanyId() != null) {
			LambdaUpdateWrapper<Companys> companyUpdate = new LambdaUpdateWrapper<>();
			companyUpdate
					.eq(Companys::getCompanyId, operator.getCompanyId())
					.set(Companys::getPassportUid, newPassportUid)
					.set(Companys::getEid, newEid)
					.set(Companys::getUpdated, now);
			companysMapper.update(null, companyUpdate);
		}

		long companyId = operator.getCompanyId() == null ? 0L : operator.getCompanyId();
		String access = token.accessToken() == null ? "" : token.accessToken();
		String refresh = token.refreshToken() != null ? token.refreshToken() : "";
		shopexSmsClientFacade.setAccessToken(companyId, newPassportUid, access, token.expiresAtEpochSeconds());
		shopexSmsClientFacade.setRefreshToken(
				companyId, newPassportUid, refresh, token.refreshExpiresAtEpochSeconds());
		shopexOauthCertSyncService.syncAfterOauthLogin(companyId, newPassportUid, access);

		companysRedisTemplate.delete(rateKey);

		Operators fresh = operatorsMapper.selectById(operatorId);
		if (fresh != null && !Objects.equals(fresh.getMobile(), mobileBefore)) {
			log.error("shopex_bind_mobile_mutated_unexpected operator_id={}", operatorId);
		}
		return Map.of("bound", isOperatorShopexBound(fresh));
	}

	private static String text(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static String trimToEmpty(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
