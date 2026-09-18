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

package cn.shopex.ecshopx.adapay.service.callback.handler;

import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapaySettleAccount;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapaySettleAccountMapper;
import cn.shopex.ecshopx.adapay.service.callback.AdapayCallbackSmsNotifier;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayCallbackCorpMemberHandler {

	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;
	private final AdapaySettleAccountMapper adapaySettleAccountMapper;
	private final OperatorsMapper operatorsMapper;
	private final AdapayCallbackSmsNotifier adapayCallbackSmsNotifier;

	public AdapayCallbackCorpMemberHandler(
			AdapayMemberMapper adapayMemberMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper,
			AdapaySettleAccountMapper adapaySettleAccountMapper,
			OperatorsMapper operatorsMapper,
			AdapayCallbackSmsNotifier adapayCallbackSmsNotifier) {
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
		this.adapaySettleAccountMapper = adapaySettleAccountMapper;
		this.operatorsMapper = operatorsMapper;
		this.adapayCallbackSmsNotifier = adapayCallbackSmsNotifier;
	}

	public List<Object> succeeded(Map<String, Object> data) {
		long memberPk = parseLong(data.get("member_id"));
		if (memberPk <= 0L) {
			throw new BadRequestException("member_id 无效");
		}
		String appId = text(data.get("app_id"));
		String auditState = text(data.get("audit_state"));
		String auditDesc = text(data.get("audit_desc"));
		AdapayMember member =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getAppId, appId)
								.last("LIMIT 1"));
		if (member == null) {
			throw new ResourceException("开户会员信息不存在");
		}
		LambdaUpdateWrapper<AdapayMember> memberUw =
				new LambdaUpdateWrapper<AdapayMember>()
						.eq(AdapayMember::getId, memberPk)
						.eq(AdapayMember::getAppId, appId)
						.set(AdapayMember::getAuditState, auditState)
						.set(AdapayMember::getAuditDesc, auditDesc);
		if ("D".equals(auditState) || "E".equals(auditState)) {
			memberUw.set(AdapayMember::getIsCreated, true);
		}
		adapayMemberMapper.update(null, memberUw);
		adapayCorpMemberMapper.update(
				null,
				new LambdaUpdateWrapper<AdapayCorpMember>()
						.eq(AdapayCorpMember::getMemberId, memberPk)
						.eq(AdapayCorpMember::getAppId, appId)
						.set(AdapayCorpMember::getAuditState, auditState)
						.set(AdapayCorpMember::getAuditDesc, auditDesc));
		AdapayMember updatedMember =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getId, memberPk)
								.eq(AdapayMember::getAppId, appId)
								.last("LIMIT 1"));
		AdapayCorpMember corpRow =
				adapayCorpMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayCorpMember>()
								.eq(AdapayCorpMember::getMemberId, memberPk)
								.eq(AdapayCorpMember::getAppId, appId)
								.last("LIMIT 1"));
		if ("E".equals(auditState)) {
			String settleAccountId = text(data.get("settle_account_id"));
			adapaySettleAccountMapper.update(
					null,
					new LambdaUpdateWrapper<AdapaySettleAccount>()
							.eq(AdapaySettleAccount::getMemberId, memberPk)
							.eq(AdapaySettleAccount::getAppId, appId)
							.set(AdapaySettleAccount::getSettleAccountId, settleAccountId));
			if (updatedMember != null
					&& "dealer".equals(updatedMember.getOperatorType())
					&& updatedMember.getOperatorId() != null
					&& updatedMember.getCompanyId() != null) {
				int now = (int) (System.currentTimeMillis() / 1000L);
				operatorsMapper.update(
						null,
						new LambdaUpdateWrapper<Operators>()
								.eq(Operators::getCompanyId, updatedMember.getCompanyId())
								.eq(Operators::getOperatorId, updatedMember.getOperatorId().longValue())
								.set(Operators::getAdapayOpenAccountTime, String.valueOf(now)));
			}
		}
		if (updatedMember != null && isTruthySms(updatedMember.getIsSms()) && corpRow != null) {
			Map<String, Object> sms = new HashMap<>();
			sms.put("mer_name", corpRow.getName());
			sms.put("tel_no", updatedMember.getTelNo());
			sms.put("company_id", updatedMember.getCompanyId());
			adapayCallbackSmsNotifier.trySend(sms);
		}
		return List.of("success");
	}

	public List<Object> failed(Map<String, Object> data) {
		return List.of("success");
	}

	private static boolean isTruthySms(String v) {
		return "1".equals(v) || "true".equalsIgnoreCase(v);
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String text(Object o) {
		return o == null ? "" : o.toString();
	}
}
