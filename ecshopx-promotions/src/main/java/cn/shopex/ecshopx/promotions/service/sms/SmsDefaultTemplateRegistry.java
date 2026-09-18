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

package cn.shopex.ecshopx.promotions.service.sms;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SmsDefaultTemplateRegistry {

	private static Map<String, Object> simpleDesc(String tmplTitle, String title) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("tmpl_title", tmplTitle);
		m.put("title", title);
		return m;
	}

	private static final Map<String, SmsDefaultTemplateRow> STANDARD;

	static {
		Map<String, SmsDefaultTemplateRow> m = new LinkedHashMap<>();
		m.put(
				"verification_code",
				new SmsDefaultTemplateRow(
						"验证码：{{验证码}}，30分钟内有效，如非本人操作请忽略！",
						"vcode",
						"notice",
						"verification_code",
						simpleDesc("短信验证码", "获取验证码后触发"),
						true));
		m.put(
				"trade_pay_success",
				new SmsDefaultTemplateRow(
						"您于{{支付时间}}成功支付{{支付金额}}元。",
						"trade",
						"notice",
						"trade_pay_success",
						simpleDesc("支付成功通知", "支付完成后触发"),
						false));
		m.put(
				"order_pickup",
				new SmsDefaultTemplateRow(
						"您{{订单号}}的订单提货码是{{提货码}}，30分钟内有效，请勿泄漏。",
						"trade",
						"notice",
						"order_pickup",
						simpleDesc("到店自提-发送提货码", "发送订单提货码触发"),
						false));
		m.put(
				"bargainFinish_notice",
				new SmsDefaultTemplateRow(
						"恭喜您，您的{{商品名称}}助力成功，支付金额：{{支付金额}}元，请于{{结束时间}}前完成支付！",
						"promotions",
						"notice",
						"bargainFinish_notice",
						simpleDesc("助力成功通知", "助力成功后触发"),
						false));
		m.put(
				"registration_success_notice",
				new SmsDefaultTemplateRow(
						"您好！您参与的{{活动名称}}活动，已经报名成功，活动开始时间：{{活动开始时间}} 活动地点：{{活动地点}} 活动详细地址：{{活动详细地址}} 感谢您的参与让这次活动更加精彩。欢迎参会，顺祝时祺！",
						"registration",
						"notice",
						"registration_success_notice",
						simpleDesc("活动报名成功通知", "活动报名成功后触发"),
						false));
		m.put(
				"registration_fail_notice",
				new SmsDefaultTemplateRow(
						"您好！您参与的{{活动名称}}活动，尚未登记成功。这可能由于以下原因：{{拒绝原因}} 请核对后尽快再次填报。顺祝时祺！",
						"registration",
						"notice",
						"registration_fail_notice",
						simpleDesc("活动报名审核失败通知", "活动报名审核失败后触发"),
						false));
		m.put(
				"registration_result_notice",
				new SmsDefaultTemplateRow(
						"您参与的{{活动名称}}活动，已经{{审核结果}}",
						"registration",
						"notice",
						"registration_result_notice",
						simpleDesc("报名活动审核通过通知", "报名活动审核通过后触发"),
						false));
		m.put(
				"merchant_audit_success_notice",
				new SmsDefaultTemplateRow(
						"您申请的商户入驻已审批通过，请登录（商户入驻链接（H5）请手动替换）查看商户后台登录的账号和密码。",
						"merchant",
						"notice",
						"merchant_audit_success_notice",
						simpleDesc("ECShopX商户入驻-申请入驻成功通知", "商户入驻成功后触发"),
						false));
		m.put(
				"merchant_audit_fail_notice",
				new SmsDefaultTemplateRow(
						"您提交的商户入驻审批未通过，请及时登录（商户入驻链接（H5）请手动替换）查看。",
						"merchant",
						"notice",
						"merchant_audit_fail_notice",
						simpleDesc("ECShopX商户入驻-申请入驻审批未通过通知", "商户入驻审批未通过时触发"),
						false));
		m.put(
				"merchant_enter_success_notice",
				new SmsDefaultTemplateRow(
						"商户入驻成功，请使用（商户后台登录地址请手动替换），登录账号为{{手机号}}，密码为{{随机密码}}登录商户后台。",
						"merchant",
						"notice",
						"merchant_enter_success_notice",
						simpleDesc("ECShopX商户入驻-后台添加商户成功通知", "后台添加商户功后触发"),
						false));
		m.put(
				"merchant_reset_password_notice",
				new SmsDefaultTemplateRow(
						"您的密码是{{随机密码}}，请勿泄漏。",
						"merchant",
						"notice",
						"merchant_reset_password_notice",
						simpleDesc("ECShopX商户后台登录密码重置成功通知", "商户后台登录密码重置后触发"),
						false));
		m.put(
				"admin_account_approved",
				new SmsDefaultTemplateRow(
						"{{商户名称}}提交的Adapay分账开户申请{{步骤}}已审批完成，请及时查看。",
						"adapay",
						"notice",
						"admin_account_approved",
						simpleDesc("Adapay分账-主商户开户审批结果通知", "主商户开户申请审批完成触发"),
						false));
		m.put(
				"sub_account_approved",
				new SmsDefaultTemplateRow(
						"{{商户名称}}提交的开户申请已审批完成，请及时查看。",
						"adapay",
						"notice",
						"sub_account_approved",
						simpleDesc("Adapay分账-子商户开户成功通知", "子商户开户申请审批完成触发"),
						false));
		m.put(
				"dealer_account_reset_pwd",
				new SmsDefaultTemplateRow(
						"{{经销商名称}}登录密码已重置，密码为:{{随机密码}}，请勿泄漏！",
						"adapay",
						"notice",
						"delear_account_reset_pwd",
						simpleDesc("Adapay分账-经销商后台登录密码重置", "经销商后台登录密码重置"),
						false));
		m.put(
				"member_birthday",
				new SmsDefaultTemplateRow(
						"尊敬的会员：值此您生日之际，衷心祝您生日快乐！为感谢您对本店的支持，特此赠送您专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_birthday",
						simpleDesc("场景营销-会员生日", "会员生日赠送触发"),
						false));
		m.put(
				"member_anniversary",
				new SmsDefaultTemplateRow(
						"历史上的今天，您成为了（品牌名称，请手动替换）会员。感谢您一路的支持，特此为您奉上会员专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_anniversary",
						simpleDesc("场景营销-入会周年", "会员周年日赠送触发"),
						false));
		m.put(
				"member_day",
				new SmsDefaultTemplateRow(
						"（会员日日期，请手动替换）是（品牌名称，请手动替换）会员日，特此为您奉上会员专属优惠券，请及时查看！到店更有其他惊喜",
						"member",
						"fan-out",
						"member_day",
						simpleDesc("场景营销-会员日", "会员日赠送触发"),
						false));
		m.put(
				"member_upgrade",
				new SmsDefaultTemplateRow(
						"恭喜您在（品牌名称，请手动替换）的会员升级成功，特此为您奉上会员专属优惠券，请及时查收！到店更有其他惊喜",
						"member",
						"fan-out",
						"member_upgrade",
						simpleDesc("场景营销-普通会员升级", "会员升级赠送触发"),
						false));
		m.put(
				"member_vip_upgrade",
				new SmsDefaultTemplateRow(
						"恭喜您在（品牌名称，请手动替换）的会员升级成功，特此为您奉上会员专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_vip_upgrade",
						simpleDesc("场景营销-付费会员升级", "付费会员升级赠送触发"),
						false));
		STANDARD = Collections.unmodifiableMap(m);
	}

	private static final Map<String, SmsDefaultTemplateRow> SHUYUN;

	static {
		Map<String, SmsDefaultTemplateRow> m = new LinkedHashMap<>();
		LinkedHashMap<String, Object> tradePayVars = new LinkedHashMap<>();
		tradePayVars.put("支付时间", 19);
		tradePayVars.put("支付金额", 8);
		LinkedHashMap<String, Object> tradePayDesc = new LinkedHashMap<>();
		tradePayDesc.put("tmpl_title", "支付成功通知");
		tradePayDesc.put("title", "支付完成后触发");
		tradePayDesc.put("variables", tradePayVars);
		m.put(
				"trade_pay_success",
				new SmsDefaultTemplateRow(
						"您于{{支付时间}}成功支付{{支付金额}}元。",
						"trade",
						"notice",
						"trade_pay_success",
						tradePayDesc,
						false));
		m.put(
				"bargainFinish_notice",
				new SmsDefaultTemplateRow(
						"恭喜您，您的{{商品名称}}助力成功，支付金额：{{支付金额}}元，请于{{结束时间}}前完成支付！",
						"promotions",
						"notice",
						"bargainFinish_notice",
						simpleDesc("助力成功通知", "助力成功后触发"),
						false));
		m.put(
				"registration_success_notice",
				new SmsDefaultTemplateRow(
						"您好！您参与的{{活动名称}}活动，已经报名成功，活动开始时间：{{活动开始时间}} 活动地点：{{活动地点}} 活动详细地址：{{活动详细地址}} 感谢您的参与让这次活动更加精彩。欢迎参会，顺祝时祺！",
						"registration",
						"notice",
						"registration_success_notice",
						simpleDesc("活动报名成功通知", "活动报名成功后触发"),
						false));
		m.put(
				"registration_fail_notice",
				new SmsDefaultTemplateRow(
						"您好！您参与的{{活动名称}}活动，尚未登记成功。这可能由于以下原因：{{拒绝原因}} 请核对后尽快再次填报。顺祝时祺！",
						"registration",
						"notice",
						"registration_fail_notice",
						simpleDesc("活动报名审核失败通知", "活动报名审核失败后触发"),
						false));
		m.put(
				"registration_result_notice",
				new SmsDefaultTemplateRow(
						"您参与的{{活动名称}}活动，已经{{审核结果}}",
						"registration",
						"notice",
						"registration_result_notice",
						simpleDesc("报名活动审核通过通知", "报名活动审核通过后触发"),
						false));
		LinkedHashMap<String, Object> mbDesc = new LinkedHashMap<>();
		mbDesc.put("tmpl_title", "场景营销-会员生日");
		mbDesc.put("title", "会员生日赠送触发");
		mbDesc.put("variables", Collections.emptyList());
		m.put(
				"member_birthday",
				new SmsDefaultTemplateRow(
						"尊敬的会员：值此您生日之际，衷心祝您生日快乐！为感谢您对本店的支持，特此赠送您专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_birthday",
						mbDesc,
						false));
		LinkedHashMap<String, Object> maDesc = new LinkedHashMap<>();
		maDesc.put("tmpl_title", "场景营销-入会周年");
		maDesc.put("title", "会员周年日赠送触发");
		maDesc.put("variables", Collections.emptyList());
		m.put(
				"member_anniversary",
				new SmsDefaultTemplateRow(
						"历史上的今天，您成为了（品牌名称，请手动替换）会员。感谢您一路的支持，特此为您奉上会员专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_anniversary",
						maDesc,
						false));
		LinkedHashMap<String, Object> mdDesc = new LinkedHashMap<>();
		mdDesc.put("tmpl_title", "场景营销-会员日");
		mdDesc.put("title", "会员日赠送触发");
		mdDesc.put("variables", Collections.emptyList());
		m.put(
				"member_day",
				new SmsDefaultTemplateRow(
						"（会员日日期，请手动替换）是（品牌名称，请手动替换）会员日，特此为您奉上会员专属优惠券，请及时查看！到店更有其他惊喜",
						"member",
						"fan-out",
						"member_day",
						mdDesc,
						false));
		LinkedHashMap<String, Object> muDesc = new LinkedHashMap<>();
		muDesc.put("tmpl_title", "场景营销-普通会员升级");
		muDesc.put("title", "会员升级赠送触发");
		muDesc.put("variables", Collections.emptyList());
		m.put(
				"member_upgrade",
				new SmsDefaultTemplateRow(
						"恭喜您在（品牌名称，请手动替换）的会员升级成功，特此为您奉上会员专属优惠券，请及时查收！到店更有其他惊喜",
						"member",
						"fan-out",
						"member_upgrade",
						muDesc,
						false));
		LinkedHashMap<String, Object> mvuDesc = new LinkedHashMap<>();
		mvuDesc.put("tmpl_title", "场景营销-付费会员升级");
		mvuDesc.put("title", "付费会员升级赠送触发");
		mvuDesc.put("variables", Collections.emptyList());
		m.put(
				"member_vip_upgrade",
				new SmsDefaultTemplateRow(
						"恭喜您在（品牌名称，请手动替换）的会员升级成功，特此为您奉上会员专属优惠券，请及时查收！",
						"member",
						"fan-out",
						"member_vip_upgrade",
						mvuDesc,
						false));
		SHUYUN = Collections.unmodifiableMap(m);
	}

	private final Environment environment;

	public SmsDefaultTemplateRegistry(Environment environment) {
		this.environment = environment;
	}

	public Optional<SmsDefaultTemplateRow> getByName(String tmplName) {
		boolean oemShuyun = SmsOemShuyunFlags.isOemShuyun(environment);
		return Optional.ofNullable(oemShuyun ? SHUYUN.get(tmplName) : STANDARD.get(tmplName));
	}

	public LinkedHashMap<String, SmsDefaultTemplateRow> orderedDefaultTemplateRows() {
		boolean oemShuyun = SmsOemShuyunFlags.isOemShuyun(this.environment);
		return new LinkedHashMap<>(oemShuyun ? SHUYUN : STANDARD);
	}
}
