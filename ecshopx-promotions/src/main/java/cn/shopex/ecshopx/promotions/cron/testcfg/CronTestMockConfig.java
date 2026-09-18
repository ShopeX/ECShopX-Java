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

package cn.shopex.ecshopx.promotions.cron.testcfg;

import cn.shopex.ecshopx.common.cron.PromotionGroupRobotWechatProfilePort;
import cn.shopex.ecshopx.common.cron.PromotionScheduleFireSceneSmsOutPort;
import cn.shopex.ecshopx.common.cron.mock.NoopDmCrmDiscountCardSendPortForCron;
import cn.shopex.ecshopx.common.cron.mock.NoopPromotionGroupRobotWechatProfile;
import cn.shopex.ecshopx.common.cron.mock.NoopPromotionScheduleFireSceneSmsOut;
import cn.shopex.ecshopx.common.cron.mock.NoopTurntableClearSurplusTimesRedisPort;
import cn.shopex.ecshopx.common.cron.mock.NoopWxaCardUsedTemplateSendPortForCron;
import cn.shopex.ecshopx.common.cron.port.TurntableClearSurplusTimesRedisPort;
import cn.shopex.ecshopx.promotions.schedule.port.DmCrmDiscountCardSendPortCronAdapter;
import cn.shopex.ecshopx.promotions.schedule.port.WxaCardUsedTemplateSendPortCronAdapter;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmDiscountCardSendPort;
import cn.shopex.ecshopx.wechat.service.WxaCardUsedTemplateSendPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 profile 配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 *
 * <p>SpecificCrowdDiscountService#scheduleExpiredPromotionMonth 无 analysis §6 外发副作用，plan §4
 * mock 清单为空，本类无需为该项追加 Noop。
 */
@Profile("test-cron")
@Configuration("promotionsCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖 {@link PromotionGroupRobotWechatProfilePort} 生产实现，避免机器人成团任务在快照对比阶段访问随机微信用户表。
	 */
	@Bean
	@Primary
	public PromotionGroupRobotWechatProfilePort promotionGroupRobotWechatProfilePort() {
		return new NoopPromotionGroupRobotWechatProfile();
	}

	/**
	 * 替换计划活动消费者内场景类短信，避免外发短信（plan §4 {@code bean-alias= promo-fire-sms}）。
	 */
	@Bean
	@Primary
	public PromotionScheduleFireSceneSmsOutPort promotionScheduleFireSceneSmsOutPort() {
		return new NoopPromotionScheduleFireSceneSmsOut();
	}

	/**
	 * 替换 {@code WxaTemplateMsgCouponNotifyService} 链上底层发模板 bean，与 {@code WxaCardUsedTemplateSendNoOp} 对位，plan
	 * {@code wxa-template-coupon}。
	 */
	@Bean
	@Primary
	public WxaCardUsedTemplateSendPort wxaCardUsedTemplateSendPort() {
		return new WxaCardUsedTemplateSendPortCronAdapter(new NoopWxaCardUsedTemplateSendPortForCron());
	}

	/**
	 * 替换达摩发券 Port（plan §4 {@code dm-crm-coupon}），覆盖原 {@code DmCrmDiscountCardSendNoOp} 装配位。
	 */
	@Bean
	@Primary
	public DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort() {
		return new DmCrmDiscountCardSendPortCronAdapter(new NoopDmCrmDiscountCardSendPortForCron());
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.promotions.integration.TurntableClearSurplusTimesRedisPortImpl}，避免
	 * test-cron 下删除大转盘「剩余次数」整键。
	 */
	@Bean
	@Primary
	public TurntableClearSurplusTimesRedisPort turntableClearSurplusTimesRedisPort() {
		return new NoopTurntableClearSurplusTimesRedisPort();
	}
}
