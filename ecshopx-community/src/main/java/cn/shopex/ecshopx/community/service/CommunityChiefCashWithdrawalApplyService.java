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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.domain.CommunityChiefCashWithdrawal;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.mapper.CommunityChiefCashWithdrawalMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityChiefCashWithdrawalApplyService {

	private final CommunityChiefService communityChiefService;
	private final CommunityChiefDistributorMapper communityChiefDistributorMapper;
	private final CommunityChiefMapper communityChiefMapper;
	private final CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper;
	private final CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final CommunityChiefCashWithdrawalFrontQueryService communityChiefCashWithdrawalFrontQueryService;

	public CommunityChiefCashWithdrawalApplyService(
			CommunityChiefService communityChiefService,
			CommunityChiefDistributorMapper communityChiefDistributorMapper,
			CommunityChiefMapper communityChiefMapper,
			CommunityChiefCashWithdrawalMapper communityChiefCashWithdrawalMapper,
			CommunityChiefCashWithdrawalQueryService communityChiefCashWithdrawalQueryService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			CommunityChiefCashWithdrawalFrontQueryService communityChiefCashWithdrawalFrontQueryService) {
		this.communityChiefService = communityChiefService;
		this.communityChiefDistributorMapper = communityChiefDistributorMapper;
		this.communityChiefMapper = communityChiefMapper;
		this.communityChiefCashWithdrawalMapper = communityChiefCashWithdrawalMapper;
		this.communityChiefCashWithdrawalQueryService = communityChiefCashWithdrawalQueryService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.communityChiefCashWithdrawalFrontQueryService = communityChiefCashWithdrawalFrontQueryService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> applyForH5(
			long companyId,
			Map<String, Object> claims,
			long moneyCentsFloor,
			String payTypeRaw,
			String accountNameFromAuth) {
		long chiefId = communityChiefService.resolveChiefIdForCashWithdrawalApply(companyId, claims);

		CommunityChiefDistributor distributorRow =
				communityChiefDistributorMapper.selectOne(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, chiefId)
								.last("LIMIT 1"));
		if (distributorRow == null) {
			throw new ResourceException("当前团长没有配置店铺");
		}
		Long distLong = distributorRow.getDistributorId();
		if (distLong == null) {
			throw new ResourceException("当前团长没有配置店铺");
		}
		int distributorId = Math.toIntExact(distLong);

		if (moneyCentsFloor < 100L) {
			throw new ResourceException("佣金提现最少为1元");
		}

		String payType;
		if (payTypeRaw == null || payTypeRaw.trim().isEmpty()) {
			payType = "wechat";
		} else {
			payType = payTypeRaw.trim().toLowerCase(Locale.ROOT);
		}
		if ("wechat".equals(payType) && moneyCentsFloor > 80000L) {
			throw new ResourceException("佣金单次最多提现800元");
		}

		CommunityChief chief =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getChiefId, chiefId)
								.eq(CommunityChief::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (chief == null) {
			throw new ResourceException("不是团长，不可以申请");
		}

		String payAccountPreset = null;
		String accountNamePreset = null;
		String bankNameVal = null;

		if ("alipay".equals(payType)) {
			if (chief.getAlipayAccount() == null || chief.getAlipayName() == null) {
				throw new ResourceException("请先设置提现支付宝账号信息");
			}
			payAccountPreset = chief.getAlipayAccount();
			accountNamePreset = chief.getAlipayName();
		} else if ("bankcard".equals(payType)) {
			if (chief.getBankName() == null || chief.getBankcardNo() == null) {
				throw new ResourceException("请先设置提现银行卡信息");
			}
			bankNameVal = chief.getBankName();
			payAccountPreset = chief.getBankcardNo();
		}

		Map<Long, Map<String, Object>> agg =
				communityChiefCashWithdrawalQueryService.buildChiefRebateAggregateByChiefIds(
						companyId, List.of(chiefId));
		Map<String, Object> row = agg.get(chiefId);
		if (row == null) {
			throw new ResourceException("申请提现金额额度超出限制");
		}
		long rebate = longOrZero(row.get("cash_withdrawal_rebate"));
		if (rebate < moneyCentsFloor) {
			throw new ResourceException("申请提现金额额度超出限制");
		}

		if (moneyCentsFloor > Integer.MAX_VALUE || moneyCentsFloor < Integer.MIN_VALUE) {
			throw new BadRequestException("提现金额格式错误");
		}

		int moneyInt = (int) moneyCentsFloor;
		int nowSec = (int) (System.currentTimeMillis() / 1000L);

		String wxaAppid;
		Object v1 = claims.get("wxapp_appid");
		if (v1 != null) {
			String t1 = String.valueOf(v1).trim();
			if (!t1.isEmpty()) {
				wxaAppid = t1;
			} else {
				Object v2 = claims.get("wxa_appid");
				wxaAppid = v2 == null ? "" : String.valueOf(v2).trim();
			}
		} else {
			Object v2 = claims.get("wxa_appid");
			wxaAppid = v2 == null ? "" : String.valueOf(v2).trim();
		}

		Object mobRaw = claims.get("mobile");
		String plainMobile = mobRaw == null ? "" : String.valueOf(mobRaw);

		CommunityChiefCashWithdrawal e = new CommunityChiefCashWithdrawal();
		e.setCompanyId(companyId);
		e.setChiefId(chiefId);
		e.setDistributorId(distributorId);
		e.setPayType(payType);
		e.setStatus("apply");
		e.setMoney(moneyInt);
		e.setRemarks(null);
		e.setCreated(nowSec);
		e.setUpdated(nowSec);
		e.setWxaAppid(wxaAppid);
		e.setMobile(sensitiveFieldEncryptor.encrypt(plainMobile));

		if ("wechat".equals(payType)) {
			Object oid = claims.get("open_id");
			e.setPayAccount(oid == null ? null : String.valueOf(oid));
		} else {
			e.setPayAccount(payAccountPreset);
		}

		if ("alipay".equals(payType)) {
			e.setAccountName(accountNamePreset);
		} else {
			e.setAccountName(accountNameFromAuth);
		}

		if ("bankcard".equals(payType)) {
			e.setBankName(bankNameVal);
		} else {
			e.setBankName(null);
		}

		communityChiefCashWithdrawalMapper.insert(e);
		if (e.getId() == null) {
			throw new ResourceException("申请失败");
		}

		return communityChiefCashWithdrawalFrontQueryService.toH5WithdrawalLine(e);
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}
}
