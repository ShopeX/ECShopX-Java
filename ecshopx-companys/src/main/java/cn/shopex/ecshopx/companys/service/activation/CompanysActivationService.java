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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Duration;
import cn.shopex.ecshopx.companys.service.auth.CompanysActivationCipher;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanysActivationService {

	public static final String SESSION_TOKEN = "7e23b4cecd91a0a8500c2fb65341193c";

	private final WechatAuthQueryService wechatAuthQueryService;
	private final CompanysMapper companysMapper;
	private final OperatorsMapper operatorsMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate prismRedisTemplate;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	@Value("${common.system-companys-id:0}")
	private long systemCompanysId;

	public CompanysActivationService(
			WechatAuthQueryService wechatAuthQueryService,
			CompanysMapper companysMapper,
			OperatorsMapper operatorsMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.companysMapper = companysMapper;
		this.operatorsMapper = operatorsMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.prismRedisTemplate = prismRedisTemplate;
	}

	/**
	 * 授权应用开关映射（当前为固定启用项；后续可在此接入许可证等数据源）。
	 */
	public Map<String, Object> getApplications() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("mobile_cashier", Boolean.TRUE);
		map.put("adapay", Boolean.TRUE);
		map.put("pointsmall", Boolean.TRUE);
		map.put("marketing_center", Boolean.TRUE);
		map.put("employee_purchase", Boolean.TRUE);
		map.put("seckill", Boolean.TRUE);
		return map;
	}

	/**
	 * 店铺/平台运营账号：校验公司存在且未被禁用（与 activated 中间件商户校验语义对齐）。
	 */
	public void assertShopOperatorCompanyActive(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null) {
			throw new ForbiddenException("未激活或者被禁止登入");
		}
		if (Boolean.TRUE.equals(c.getIsDisabled())) {
			throw new ForbiddenException("未激活或者被禁止登入");
		}
	}

	public Map<String, Object> getLoginToken(Map<String, Object> operator) {
		Object companyIdObj = operator.get("company_id");
		Object mobileObj = operator.get("mobile");
		if (companyIdObj == null || mobileObj == null) {
			throw new ResourceException("登录账号异常");
		}
		long companyId = toLong(companyIdObj);
		if (!systemIsSaas && companyId != systemCompanysId) {
			throw new ResourceException("请使用授权的账号登录");
		}
		String authorizerAppid = wechatAuthQueryService.getAuthorizerAppid(companyId);
		Object operatorIdObj = operator.get("operator_id");
		if (operatorIdObj == null) {
			throw new ResourceException("登录账号异常");
		}
		long operatorId;
		try {
			operatorId = toLong(operatorIdObj);
		} catch (RuntimeException ex) {
			throw new ResourceException("登录账号异常");
		}
		String sid = "select_distributor" + operatorId + "-" + companyId;
		companysRedisTemplate.opsForValue().set(sid, "");
		long nowSec = System.currentTimeMillis() / 1000L;
		String id = CompanysActivationCipher.encryptUserPayload(operatorId, SESSION_TOKEN, nowSec);
		Map<String, Object> newOperator = new LinkedHashMap<>();
		newOperator.put("id", id);
		newOperator.put("company_id", companyId);
		newOperator.put("mobile", mobileObj.toString());
		newOperator.put("operator_type", operator.get("operator_type"));
		newOperator.put("is_authorizer", authorizerAppid != null && !authorizerAppid.isEmpty());
		newOperator.put("logintype", operator.get("logintype"));
		newOperator.put("merchant_id", parseMerchantIdForJwt(operator.get("merchant_id")));
		Object shopIds = operator.get("shop_ids");
		if (shopIds != null) {
			newOperator.put("shop_ids", shopIds);
		}
		Object distributorIds = operator.get("distributor_ids");
		if (distributorIds != null) {
			newOperator.put("distributor_ids", distributorIds);
		}
		return newOperator;
	}

	public void delBlackTokenCache(long operatorId, String operatorType) {
		String key = "black_token_" + operatorId + "_" + operatorType;
		prismRedisTemplate.delete(key);
	}

	/**
	 * 标记账号需重新登录，并刷新 {@code operators.updated}。
	 */
	public void setBlackTokenCache(long operatorId, String operatorType) {
		String key = "black_token_" + operatorId + "_" + operatorType;
		long nowSec = System.currentTimeMillis() / 1000L;
		prismRedisTemplate.opsForValue().set(key, String.valueOf(nowSec));
		prismRedisTemplate.expire(key, Duration.ofSeconds(604800));
		LambdaUpdateWrapper<Operators> u = new LambdaUpdateWrapper<>();
		u.eq(Operators::getOperatorId, operatorId).set(Operators::getUpdated, (int) nowSec);
		operatorsMapper.update(null, u);
	}

	public void checkUserAuth(Map<String, Object> userData) {
		Object opType = userData.get("operator_type");
		if ("shopadmin".equals(opType) || "user".equals(opType)) {
			return;
		}
		Object source = userData.get("source");
		if (source != null && "user".equals(source.toString())) {
			return;
		}
		Object idObj = userData.get("id");
		if (idObj == null) {
			throw new ResourceException("用户信息错误，checkUserAuth");
		}
		Map<String, String> data = CompanysActivationCipher.decryptUserPayload(idObj.toString());
		if (!SESSION_TOKEN.equals(data.get("token"))) {
			throw new ResourceException("登录验证错误");
		}
	}

	public void attachOperatorIdFromSessionClaims(Map<String, Object> userData) {
		if (hasUsableOperatorIdClaim(userData)) {
			return;
		}
		Object idObj = userData.get("id");
		if (!(idObj instanceof String)) {
			return;
		}
		try {
			Map<String, String> data = CompanysActivationCipher.decryptUserPayload((String) idObj);
			String innerId = data.get("id");
			if (innerId == null || innerId.isEmpty()) {
				return;
			}
			userData.put("operator_id", Long.parseLong(innerId.trim()));
		} catch (RuntimeException ignored) {
			// leave userData unchanged
		}
	}

	private static boolean hasUsableOperatorIdClaim(Map<String, Object> userData) {
		Object op = userData.get("operator_id");
		if (op == null) {
			return false;
		}
		if (op instanceof Number) {
			return true;
		}
		try {
			Long.parseLong(op.toString().trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	/** 操作员所属商户主键；缺失或无法解析时按 0。 */
	private static long parseMerchantIdForJwt(Object merchantIdObj) {
		if (merchantIdObj == null) {
			return 0L;
		}
		if (merchantIdObj instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(merchantIdObj.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
