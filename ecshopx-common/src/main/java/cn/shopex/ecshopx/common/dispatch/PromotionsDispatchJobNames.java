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

package cn.shopex.ecshopx.common.dispatch;

public final class PromotionsDispatchJobNames {

	public static final String FIRE_PROMOTIONS_ACTIVITY =
			"job:5:PromotionsBundle\\Jobs\\FirePromotionsActivity";

	/** Same logical job as {@link #FIRE_PROMOTIONS_ACTIVITY}; distinct platform messageName for job:187. */
	public static final String FIRE_PROMOTIONS_ACTIVITY_JOB_187 =
			"job:187:PromotionsBundle\\Jobs\\FirePromotionsActivity";

	public static final String SCHEDULE_FIRE_PROMOTIONS_ACTIVITY =
			"job:150:PromotionsBundle\\Jobs\\ScheduleFirePromotionsActivity";

	public static final String SCHEDULE_GIVE_PROMOTIONS_ACTIVITY =
			"job:151:PromotionsBundle\\Jobs\\ScheduleGivePromotionsActivity";

	public static final String WXOPEN_TEMPLATE_SEND =
			"job:11:PromotionsBundle\\Jobs\\WxopenTemplateSend";

	public static final String CANCEL_SECKILL_PLAT_TICKET =
			"job:111:PromotionsBundle\\Jobs\\CancelSeckillPlatTicket";

	public static final String BARGAIN_FINISH_SEND_SMS_NOTICE =
			"job:135:PromotionsBundle\\Jobs\\BargainFinishSendSmsNotice";

	public static final String SAVE_PROMOTION_ITEM_TAG =
			"job:143:PromotionsBundle\\Jobs\\SavePromotionItemTag";

	public static final String ALI_TEMPLATE_MSG_SEND =
			"job:136:PromotionsBundle\\Jobs\\AliTemplateMsgSend";

	private PromotionsDispatchJobNames() {
	}
}
