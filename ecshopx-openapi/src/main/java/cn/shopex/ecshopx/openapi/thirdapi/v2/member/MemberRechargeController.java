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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeTradeListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2MemberRecharge")
@RequestMapping("/api/openapi/internal/v2")
public class MemberRechargeController extends OpenapiBaseController {

	private final OpenapiMemberRechargeRuleCreatePort rechargeRuleCreatePort;
	private final OpenapiMemberRechargeRuleDeletePort rechargeRuleDeletePort;
	private final OpenapiMemberRechargeRuleUpdatePort rechargeRuleUpdatePort;
	private final OpenapiMemberRechargeRuleListPort rechargeRuleListPort;
	private final OpenapiMemberRechargeTradeListPort rechargeTradeListPort;

	public MemberRechargeController(
			OpenapiMemberRechargeRuleCreatePort rechargeRuleCreatePort,
			OpenapiMemberRechargeRuleDeletePort rechargeRuleDeletePort,
			OpenapiMemberRechargeRuleUpdatePort rechargeRuleUpdatePort,
			OpenapiMemberRechargeRuleListPort rechargeRuleListPort,
			OpenapiMemberRechargeTradeListPort rechargeTradeListPort) {
		this.rechargeRuleCreatePort = rechargeRuleCreatePort;
		this.rechargeRuleDeletePort = rechargeRuleDeletePort;
		this.rechargeRuleUpdatePort = rechargeRuleUpdatePort;
		this.rechargeRuleListPort = rechargeRuleListPort;
		this.rechargeTradeListPort = rechargeTradeListPort;
	}

	@GetMapping(value = "/ecx.member.rechargerule.get", name = "开放接口查询储值面额规则列表")
	public Map<String, Object> getRechargeRuleList(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		return rechargeRuleListPort.getRechargeRuleList(companyId);
	}

	@GetMapping(value = "/ecx.member.recharge.trade.get", name = "开放接口查询储值交易记录")
	public Map<String, Object> getRechargeTradeList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "trade_id", required = false) String tradeIdParam,
			@RequestParam(name = "shop_id", required = false) String shopIdParam,
			@RequestParam(name = "date_begin", required = false) String dateBeginParam,
			@RequestParam(name = "date_end", required = false) String dateEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberRechargeTradeListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberRechargeTradeListParams.resolve(pageParam, pageSizeParam, body);
		String mobile = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String tradeId = OpenapiRequestParams.mergeString(tradeIdParam, body, "trade_id");
		String shopId = OpenapiRequestParams.mergeString(shopIdParam, body, "shop_id");
		String dateBegin = OpenapiRequestParams.mergeString(dateBeginParam, body, "date_begin");
		String dateEnd = OpenapiRequestParams.mergeString(dateEndParam, body, "date_end");
		return rechargeTradeListPort.getRechargeTradeList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				mobile,
				tradeId,
				shopId,
				dateBegin,
				dateEnd);
	}

	@PostMapping(value = "/ecx.member.rechargerule.add", name = "开放接口新增储值面额规则")
	public Map<String, Object> createRechargeRule(
			HttpServletRequest request,
			@RequestParam(name = "fixed_money", required = false) String fixedMoneyParam,
			@RequestParam(name = "rule_type", required = false) String ruleTypeParam,
			@RequestParam(name = "rule_data", required = false) String ruleDataParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String fixedMoneyRaw = OpenapiRequestParams.mergeString(fixedMoneyParam, body, "fixed_money");
		String ruleTypeRaw = OpenapiRequestParams.mergeString(ruleTypeParam, body, "rule_type");
		String ruleDataRaw = OpenapiRequestParams.mergeString(ruleDataParam, body, "rule_data");
		return rechargeRuleCreatePort.createRechargeRule(
				companyId, fixedMoneyRaw, ruleTypeRaw, ruleDataRaw);
	}

	@PostMapping(value = "/ecx.member.rechargerule.update", name = "开放接口修改储值面额规则")
	public Map<String, Object> updateRechargeRule(
			HttpServletRequest request,
			@RequestParam(name = "rechargerule_id", required = false) String rechargeruleIdParam,
			@RequestParam(name = "fixed_money", required = false) String fixedMoneyParam,
			@RequestParam(name = "rule_type", required = false) String ruleTypeParam,
			@RequestParam(name = "rule_data", required = false) String ruleDataParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String rechargeruleIdRaw =
				OpenapiRequestParams.mergeString(rechargeruleIdParam, body, "rechargerule_id");
		String fixedMoneyRaw = OpenapiRequestParams.mergeString(fixedMoneyParam, body, "fixed_money");
		String ruleTypeRaw = OpenapiRequestParams.mergeString(ruleTypeParam, body, "rule_type");
		String ruleDataRaw = OpenapiRequestParams.mergeString(ruleDataParam, body, "rule_data");
		return rechargeRuleUpdatePort.updateRechargeRule(
				companyId, rechargeruleIdRaw, fixedMoneyRaw, ruleTypeRaw, ruleDataRaw);
	}

	@DeleteMapping(value = "/ecx.member.rechargerule.delete", name = "开放接口删除储值面额规则")
	public Map<String, Object> deleteRechargeRule(
			HttpServletRequest request,
			@RequestParam(name = "rechargerule_id", required = false) String rechargeruleIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String rechargeruleIdRaw =
				OpenapiRequestParams.mergeString(rechargeruleIdParam, body, "rechargerule_id");
		return rechargeRuleDeletePort.deleteRechargeRule(companyId, rechargeruleIdRaw);
	}
}
