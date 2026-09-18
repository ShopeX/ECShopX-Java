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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeListForWechatQueryPort;
import cn.shopex.ecshopx.common.cron.merchant.CronMerchantPaymentTradeRow;
import cn.shopex.ecshopx.common.port.MerchantPaymentTradeQueryPort;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MerchantPaymentTradeQueryService implements MerchantPaymentTradeQueryPort, CronMerchantPaymentTradeListForWechatQueryPort {

	private static final String REL_SCENE_NAME_REBATE_CASH_WITHDRAWAL = "rebate_cash_withdrawal";
	private static final String REL_SCENE_NAME_COMMUNITY_CHIEF_CASH_WITHDRAWAL = "community_chief_cash_withdrawal";
	private static final String REL_SCENE_NAME_POPULARIZE_REBATE_CASH_WITHDRAWAL = "popularize_rebate_cash_withdrawal";

	private final MerchantPaymentTradeMapper merchantPaymentTradeMapper;

	public MerchantPaymentTradeQueryService(MerchantPaymentTradeMapper merchantPaymentTradeMapper) {
		this.merchantPaymentTradeMapper = merchantPaymentTradeMapper;
	}

	@Override
	public List<CronMerchantPaymentTradeRow> listWechatProcess(int page, int pageSize) {
		LambdaQueryWrapper<MerchantPaymentTrade> w = new LambdaQueryWrapper<>();
		w.eq(MerchantPaymentTrade::getPaymentAction, "WECHAT");
		w.eq(MerchantPaymentTrade::getStatus, "PROCESS");
		w.orderByDesc(MerchantPaymentTrade::getCreateTime);
		Page<MerchantPaymentTrade> p = new Page<>(page, pageSize, false);
		Page<MerchantPaymentTrade> result = merchantPaymentTradeMapper.selectPage(p, w);
		List<CronMerchantPaymentTradeRow> out = new ArrayList<>();
		for (MerchantPaymentTrade t : result.getRecords()) {
			out.add(
					new CronMerchantPaymentTradeRow(
							t.getMerchantTradeId(),
							t.getCompanyId() != null ? t.getCompanyId() : 0L,
							t.getRelSceneId(),
							t.getRelSceneName(),
							t.getPaymentNo()));
		}
		return out;
	}

	public int countByRebateCashWithdrawal(long companyId, String relSceneId) {
		long total =
				merchantPaymentTradeMapper.selectCount(
						baseWrapper(companyId, relSceneId, REL_SCENE_NAME_REBATE_CASH_WITHDRAWAL));
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	public List<Map<String, Object>> listByRebateCashWithdrawal(
			long companyId, String relSceneId, int page, int pageSize) {
		LambdaQueryWrapper<MerchantPaymentTrade> w =
				baseWrapper(companyId, relSceneId, REL_SCENE_NAME_REBATE_CASH_WITHDRAWAL);
		w.orderByDesc(MerchantPaymentTrade::getCreateTime);
		Page<MerchantPaymentTrade> p = new Page<>(page, pageSize, false);
		Page<MerchantPaymentTrade> result = merchantPaymentTradeMapper.selectPage(p, w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (MerchantPaymentTrade row : result.getRecords()) {
			out.add(toSnakeCaseMap(row));
		}
		return out;
	}

	public int countByCommunityChiefCashWithdrawal(long companyId, String relSceneId) {
		long total =
				merchantPaymentTradeMapper.selectCount(
						baseWrapper(companyId, relSceneId, REL_SCENE_NAME_COMMUNITY_CHIEF_CASH_WITHDRAWAL));
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	public List<Map<String, Object>> listByCommunityChiefCashWithdrawal(
			long companyId, String relSceneId, int page, int pageSize) {
		LambdaQueryWrapper<MerchantPaymentTrade> w =
				baseWrapper(companyId, relSceneId, REL_SCENE_NAME_COMMUNITY_CHIEF_CASH_WITHDRAWAL);
		w.orderByDesc(MerchantPaymentTrade::getCreateTime);
		Page<MerchantPaymentTrade> p = new Page<>(page, pageSize, false);
		Page<MerchantPaymentTrade> result = merchantPaymentTradeMapper.selectPage(p, w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (MerchantPaymentTrade row : result.getRecords()) {
			out.add(toSnakeCaseMap(row));
		}
		return out;
	}

	public int countByPopularizeRebateCashWithdrawal(long companyId, String relSceneId) {
		long total =
				merchantPaymentTradeMapper.selectCount(
						baseWrapper(companyId, relSceneId, REL_SCENE_NAME_POPULARIZE_REBATE_CASH_WITHDRAWAL));
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	public List<Map<String, Object>> listByPopularizeRebateCashWithdrawal(
			long companyId, String relSceneId, int page, int pageSize) {
		LambdaQueryWrapper<MerchantPaymentTrade> w =
				baseWrapper(companyId, relSceneId, REL_SCENE_NAME_POPULARIZE_REBATE_CASH_WITHDRAWAL);
		w.orderByDesc(MerchantPaymentTrade::getCreateTime);
		Page<MerchantPaymentTrade> p = new Page<>(page, pageSize, false);
		Page<MerchantPaymentTrade> result = merchantPaymentTradeMapper.selectPage(p, w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (MerchantPaymentTrade row : result.getRecords()) {
			out.add(toSnakeCaseMap(row));
		}
		return out;
	}

	private static LambdaQueryWrapper<MerchantPaymentTrade> baseWrapper(
			long companyId, String relSceneId, String relSceneName) {
		LambdaQueryWrapper<MerchantPaymentTrade> w = new LambdaQueryWrapper<>();
		w.eq(MerchantPaymentTrade::getCompanyId, companyId);
		w.eq(MerchantPaymentTrade::getRelSceneId, relSceneId);
		w.eq(MerchantPaymentTrade::getRelSceneName, relSceneName);
		return w;
	}

	private static Map<String, Object> toSnakeCaseMap(MerchantPaymentTrade t) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("merchant_trade_id", t.getMerchantTradeId());
		m.put("company_id", t.getCompanyId());
		m.put("rel_scene_id", t.getRelSceneId());
		m.put("rel_scene_name", t.getRelSceneName());
		m.put("mch_appid", t.getMchAppid());
		m.put("mchid", t.getMchid());
		m.put("payment_action", t.getPaymentAction());
		m.put("check_name", t.getCheckName());
		m.put("mobile", t.getMobile());
		m.put("re_user_name", t.getReUserName());
		m.put("user_id", t.getUserId());
		m.put("open_id", t.getOpenId());
		m.put("amount", t.getAmount());
		m.put("payment_desc", t.getPaymentDesc());
		m.put("spbill_create_ip", t.getSpbillCreateIp());
		m.put("status", t.getStatus());
		m.put("payment_no", t.getPaymentNo());
		m.put("payment_time", t.getPaymentTime());
		m.put("error_code", t.getErrorCode());
		m.put("error_desc", t.getErrorDesc());
		m.put("create_time", t.getCreateTime());
		m.put("update_time", t.getUpdateTime());
		m.put("cur_pay_fee", t.getCurPayFee());
		m.put("cur_fee_symbol", t.getCurFeeSymbol());
		m.put("cur_fee_rate", t.getCurFeeRate());
		m.put("cur_fee_type", t.getCurFeeType());
		m.put("fee_type", t.getFeeType());
		m.put("hf_order_id", t.getHfOrderId());
		m.put("hf_order_date", t.getHfOrderDate());
		m.put("hf_cash_type", t.getHfCashType());
		m.put("user_cust_id", t.getUserCustId());
		m.put("bind_card_id", t.getBindCardId());
		return m;
	}
}
