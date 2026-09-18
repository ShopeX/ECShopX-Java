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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.deposit.service.DepositTradeIdGenerator;
import cn.shopex.ecshopx.deposit.service.UserDepositBalanceMutationService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单预存款支付场景下的退款入账：写入退款流水并增加用户储值余额。
 */
@Service
public class OrdersDepositOrderRefundService {

	private final DepositTradeMapper depositTradeMapper;
	private final DepositTradeIdGenerator depositTradeIdGenerator;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final UserDepositBalanceMutationService userDepositBalanceMutationService;
	private final MemberAccountService memberAccountService;

	public OrdersDepositOrderRefundService(
			DepositTradeMapper depositTradeMapper,
			DepositTradeIdGenerator depositTradeIdGenerator,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			UserDepositBalanceMutationService userDepositBalanceMutationService,
			MemberAccountService memberAccountService) {
		this.depositTradeMapper = depositTradeMapper;
		this.depositTradeIdGenerator = depositTradeIdGenerator;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.userDepositBalanceMutationService = userDepositBalanceMutationService;
		this.memberAccountService = memberAccountService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void refundOrderDeposit(
			long companyId,
			long userId,
			int refundFeeFen,
			long orderId,
			long shopId,
			String wxaAppid) {
		if (refundFeeFen <= 0) {
			return;
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String memberCardCode =
				memberInfo.get("user_card_code") == null ? "" : String.valueOf(memberInfo.get("user_card_code"));
		String mobilePlain = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
		String openId = memberInfo.get("open_id") == null ? "" : String.valueOf(memberInfo.get("open_id"));
		String authorizerAppid =
				memberInfo.get("authorizer_appid") == null ? "" : String.valueOf(memberInfo.get("authorizer_appid"));

		String depositTradeId = depositTradeIdGenerator.nextDepositTradeId(userId);
		long nowSec = System.currentTimeMillis() / 1000L;
		String mobileEnc = StringUtils.hasText(mobilePlain) ? sensitiveFieldEncryptor.encrypt(mobilePlain) : "";

		DepositTrade row = new DepositTrade();
		row.setDepositTradeId(depositTradeId);
		row.setCompanyId(String.valueOf(companyId));
		row.setMemberCardCode(memberCardCode.isEmpty() ? null : memberCardCode);
		row.setShopId(shopId > 0L ? String.valueOf(shopId) : "0");
		row.setShopName(null);
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setOpenId(openId.isEmpty() ? null : openId);
		row.setMoney(String.valueOf(refundFeeFen));
		row.setTradeType("refund");
		row.setTradeStatus("SUCCESS");
		row.setDetail("订单" + orderId + "退款");
		row.setTimeStart(String.valueOf(nowSec));
		row.setTimeExpire(String.valueOf(nowSec));
		row.setCurPayFee(String.valueOf(refundFeeFen));
		row.setAuthorizerAppid(authorizerAppid.isEmpty() ? null : authorizerAppid);
		row.setWxaAppid(wxaAppid == null || wxaAppid.isEmpty() ? null : wxaAppid);

		depositTradeMapper.insert(row);
		userDepositBalanceMutationService.addUserDepositTotal(companyId, userId, refundFeeFen);
	}
}
