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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.employee.OperatorAccountOutsideLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 账号写库：创建 operators 行及经销商主账号 {@code dealer_parent_id} 回写。
 */
@Service
public class OperatorsCommandService {

	private static final Set<String> PERSIST_OPERATOR_TYPES = Set.of(
			"admin", "staff", "distributor", "dealer", "merchant", "supplier", "agent", "self_delivery_staff");

	private final OperatorsMapper operatorsMapper;
	private final ObjectMapper objectMapper;
	private final OperatorResponseAssembler operatorResponseAssembler;
	private final OperatorAccountOutsideLangWriteService operatorAccountOutsideLangWriteService;
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public OperatorsCommandService(
			OperatorsMapper operatorsMapper,
			ObjectMapper objectMapper,
			OperatorResponseAssembler operatorResponseAssembler,
			OperatorAccountOutsideLangWriteService operatorAccountOutsideLangWriteService) {
		this.operatorsMapper = operatorsMapper;
		this.objectMapper = objectMapper;
		this.operatorResponseAssembler = operatorResponseAssembler;
		this.operatorAccountOutsideLangWriteService = operatorAccountOutsideLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createOperator(Map<String, Object> data) {
		String operatorType = str(data.get("operator_type"));
		if (!PERSIST_OPERATOR_TYPES.contains(operatorType)) {
			throw new ResourceException("账号类型不存在");
		}
		String mobile = str(data.get("mobile"));
		if (mobile.isEmpty()) {
			throw new ResourceException("请填写手机号");
		}
		if (operatorsMapper.countExistingByMobileAndOperatorType(mobile, operatorType) >= 1L) {
			throw new ResourceException("该手机号已被使用");
		}
		String eid = str(data.get("eid"));
		boolean skipLoginCheck = !eid.isEmpty() || "self_delivery_staff".equals(operatorType);
		if (!skipLoginCheck) {
			String loginName = str(data.get("login_name"));
			if (loginName.isEmpty()) {
				throw new ResourceException("请填写账号名");
			}
			if (operatorsMapper.countExistingByLoginNameAndOperatorType(loginName, operatorType) >= 1L) {
				throw new ResourceException("该账号名已被使用");
			}
		}

		Operators op = new Operators();
		op.setMobile(mobile);
		op.setLoginName(str(data.get("login_name")));
		op.setOperatorType(operatorType);
		op.setCompanyId(toLongObject(data.get("company_id")));
		op.setUsername(str(data.get("username")));
		op.setHeadPortrait(str(data.get("head_portrait")));
		String plain = str(data.get("password"));
		if (!plain.isEmpty()) {
			op.setPassword(bcrypt.encode(plain));
		}
		op.setEid(eid.isEmpty() ? null : eid);
		String passportUid = str(data.get("passport_uid"));
		op.setPassportUid(passportUid.isEmpty() ? null : passportUid);
		op.setDistributorIds(jsonOrRaw(data.get("distributor_ids")));
		op.setShopIds(jsonOrRaw(data.get("shop_ids")));
		op.setRegionauthId(toLongObjectWithDefault(data.get("regionauth_id"), 0L));
		op.setContact(str(data.get("contact")));
		op.setMerchantId(toLongObjectWithDefault(data.get("merchant_id"), 0L));
		op.setIsDisable(false);
		op.setIsMerchantMain(false);
		op.setIsDistributorMain(boolObj(data.get("is_distributor_main")));
		op.setIsDealerMain(boolObj(data.get("is_dealer_main")));
		op.setDealerParentId(str(data.get("dealer_parent_id")));

		int now = (int) (System.currentTimeMillis() / 1000L);
		op.setCreated(now);
		op.setUpdated(now);

		operatorsMapper.insert(op);
		Long id = op.getOperatorId();
		if (id == null) {
			throw new ResourceException("账号创建失败");
		}

		if ("dealer".equals(operatorType) && Boolean.TRUE.equals(op.getIsDealerMain())) {
			LambdaUpdateWrapper<Operators> u = new LambdaUpdateWrapper<>();
			u.eq(Operators::getOperatorId, id).set(Operators::getDealerParentId, String.valueOf(id));
			operatorsMapper.update(null, u);
			op.setDealerParentId(String.valueOf(id));
		}

		Operators persisted = operatorsMapper.selectById(id);
		return operatorResponseAssembler.toStatusMap(persisted != null ? persisted : op);
	}

	private String jsonOrRaw(Object v) {
		if (v == null) {
			return "[]";
		}
		if (v instanceof String s) {
			return s.isEmpty() ? "[]" : s;
		}
		try {
			return objectMapper.writeValueAsString(v);
		} catch (JsonProcessingException e) {
			throw new ResourceException("账号创建失败");
		}
	}

	private static Boolean boolObj(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return !"0".equals(String.valueOf(v));
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static Long toLongObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static Long toLongObjectWithDefault(Object o, long def) {
		Long v = toLongObject(o);
		return v != null ? v : def;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateOperator(
			long operatorId, long companyId, Map<String, Object> operatorUpdatePayload) {
		Operators existing = operatorsMapper.selectById(operatorId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		if (existing.getCompanyId() == null || !existing.getCompanyId().equals(companyId)) {
			throw new ForbiddenException("无权操作该账号");
		}
		Map<String, Object> payload = new LinkedHashMap<>(operatorUpdatePayload);
		String requestedOperatorType = str(payload.get("operator_type"));
		payload.remove("operator_type");
		if (StringUtils.hasText(str(existing.getLoginName()))) {
			payload.remove("login_name");
		}

		String existingOpType = existing.getOperatorType() != null ? existing.getOperatorType() : "staff";
		String uniquenessOpType =
				StringUtils.hasText(requestedOperatorType) && PERSIST_OPERATOR_TYPES.contains(requestedOperatorType)
						? requestedOperatorType
						: existingOpType;

		if (payload.containsKey("mobile")) {
			Object mob = payload.get("mobile");
			String mobileStr = mob != null ? mob.toString() : "";
			if (!mobileStr.isBlank() && !mobileStr.equals(str(existing.getMobile()))) {
				if (operatorsMapper.countExistingByMobileAndOperatorTypeExcluding(
								operatorId, mobileStr, uniquenessOpType)
						>= 1L) {
					throw new ResourceException("该手机号已被使用");
				}
			}
		}

		String eid = str(existing.getEid());
		boolean skipLoginCheck = !eid.isEmpty() || "self_delivery_staff".equals(existingOpType);
		if (!skipLoginCheck && payload.containsKey("login_name")) {
			Object ln = payload.get("login_name");
			String loginNameStr = ln != null ? ln.toString() : "";
			if (!loginNameStr.isBlank() && !loginNameStr.equals(str(existing.getLoginName()))) {
				if (operatorsMapper.countExistingByLoginNameAndOperatorTypeExcluding(
								operatorId, loginNameStr, uniquenessOpType)
						>= 1L) {
					throw new ResourceException("该账号名已被使用");
				}
			}
		}

		LambdaUpdateWrapper<Operators> u = new LambdaUpdateWrapper<>();
		u.eq(Operators::getOperatorId, operatorId).eq(Operators::getCompanyId, companyId);

		if (payload.containsKey("password")) {
			Object pw = payload.get("password");
			if (pw != null && !pw.toString().isBlank()) {
				u.set(Operators::getPassword, bcrypt.encode(pw.toString()));
			}
		}
		if (payload.containsKey("mobile") && StringUtils.hasText(str(payload.get("mobile")))) {
			u.set(Operators::getMobile, str(payload.get("mobile")));
		}
		if (payload.containsKey("login_name") && StringUtils.hasText(str(payload.get("login_name")))) {
			u.set(Operators::getLoginName, str(payload.get("login_name")));
		}
		if (payload.containsKey("username") && StringUtils.hasText(str(payload.get("username")))) {
			u.set(Operators::getUsername, str(payload.get("username")));
		}
		if (payload.containsKey("head_portrait") && StringUtils.hasText(str(payload.get("head_portrait")))) {
			u.set(Operators::getHeadPortrait, str(payload.get("head_portrait")));
		}
		if (payload.containsKey("distributor_ids")) {
			u.set(Operators::getDistributorIds, jsonOrRaw(payload.get("distributor_ids")));
		}
		if (payload.containsKey("shop_ids")) {
			u.set(Operators::getShopIds, jsonOrRaw(payload.get("shop_ids")));
		}
		if (payload.containsKey("regionauth_id")) {
			u.set(Operators::getRegionauthId, toLongObjectWithDefault(payload.get("regionauth_id"), 0L));
		}
		if (payload.containsKey("is_distributor_main")) {
			u.set(Operators::getIsDistributorMain, boolObj(payload.get("is_distributor_main")));
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		u.set(Operators::getUpdated, now);

		int n = operatorsMapper.update(null, u);
		if (n < 1) {
			throw new ResourceException("未查询到更新数据");
		}
		Operators persisted = operatorsMapper.selectById(operatorId);
		if (persisted != null) {
			operatorAccountOutsideLangWriteService.syncOperatorsDisplayedLangFields(
					companyId, operatorId, persisted, null);
		}
		return operatorResponseAssembler.toStatusMap(persisted);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateDisableOnly(long companyId, long targetOperatorId, boolean disable) {
		LambdaUpdateWrapper<Operators> u = new LambdaUpdateWrapper<>();
		u.eq(Operators::getCompanyId, companyId).eq(Operators::getOperatorId, targetOperatorId);
		u.set(Operators::getIsDisable, disable);
		u.set(Operators::getUpdated, (int) (System.currentTimeMillis() / 1000L));
		int n = operatorsMapper.update(null, u);
		if (n < 1) {
			throw new ResourceException("未查询到更新数据");
		}
		Operators persisted = operatorsMapper.selectById(targetOperatorId);
		if (persisted == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return operatorResponseAssembler.toStatusMap(persisted);
	}

	/**
	 * 经销商主账号首次访问列表时补全 {@code dealer_parent_id} 为自身 {@code operator_id}。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void initDealerParentIdForCurrentDealer(long operatorId) {
		Operators current = operatorsMapper.selectById(operatorId);
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String opType = current.getOperatorType();
		if (opType == null || !"dealer".equals(opType)) {
			return;
		}
		String existing = current.getDealerParentId();
		if (existing != null && !existing.trim().isEmpty()) {
			return;
		}
		LambdaUpdateWrapper<Operators> u = new LambdaUpdateWrapper<>();
		u.eq(Operators::getOperatorId, operatorId).set(Operators::getDealerParentId, String.valueOf(operatorId));
		int rows = operatorsMapper.update(null, u);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteByOperatorIdOnly(long operatorId) {
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getOperatorId, operatorId);
		int rows = operatorsMapper.delete(w);
		if (rows == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteByOperatorIdAndCompanyId(long companyId, long operatorId) {
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getOperatorId, operatorId).eq(Operators::getCompanyId, companyId);
		int rows = operatorsMapper.delete(w);
		if (rows == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}
}
