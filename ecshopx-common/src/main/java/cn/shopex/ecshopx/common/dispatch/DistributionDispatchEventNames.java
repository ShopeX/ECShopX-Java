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

public final class DistributionDispatchEventNames {

	public static final String EVENT_DISTRIBUTOR_CREATE =
			"event:263:DistributionBundle\\Events\\DistributorCreateEvent";

	public static final String EVENT_DISTRIBUTION_ADD =
			"event:281:DistributionBundle\\Events\\DistributionAddEvent";

	public static final String EVENT_DISTRIBUTION_ADD_CSV289 =
			"event:289:DistributionBundle\\Events\\DistributionAddEvent";

	public static final String EVENT_DISTRIBUTION_EDIT =
			"event:282:DistributionBundle\\Events\\DistributionEditEvent";

	public static final String EVENT_DISTRIBUTION_EDIT_CSV290 =
			"event:290:DistributionBundle\\Events\\DistributionEditEvent";

	public static final String LISTENER_YOUSHU_BUNDLE_DISTRIBUTION =
			"listener:YoushuBundle\\Listeners\\Distribution";

	public static final String LISTENER_THIRDPARTY_DISTRIBUTION_ADD_PUSH_MARKETING_CENTER =
			"listener:thirdparty.distribution_add_push_marketing_center";

	public static final String LISTENER_THIRDPARTY_DISTRIBUTION_EDIT_PUSH_MARKETING_CENTER =
			"listener:thirdparty.distribution_edit_push_marketing_center";

	public static final String LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT =
			"listener:HfPayBundle\\Listeners\\HfEnterapplyInit";

	public static final String LISTENER_HFPAY_BUNDLE_HF_ENTERAPPLY_INIT_EDIT =
			"listener:HfPayBundle\\Listeners\\HfEnterapplyInit@edit";

	public static final String LISTENER_SHOP_CREATE_SEND_OME =
			"listener:SystemLinkBundle\\Listeners\\ShopCreateSendOme";

	public static final String EVENT_DISTRIBUTOR_UPDATE =
			"event:264:DistributionBundle\\Events\\DistributorUpdateEvent";

	public static final String LISTENER_SHOP_UPDATE_SEND_OME =
			"listener:SystemLinkBundle\\Listeners\\ShopUpdateSendOme";

	/** 数云开放平台：店铺创建 → shop.batch.register */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_SHOP_SYNC_ON_DISTRIBUTOR_CREATE =
			"listener:shuyun.open_platform.shop_sync_on_distributor_create";

	/** 数云开放平台：店铺更新 → shop.batch.register */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_SHOP_SYNC_ON_DISTRIBUTOR_UPDATE =
			"listener:shuyun.open_platform.shop_sync_on_distributor_update";

	public static final String EVENT_REFUND_FREIGHT_AUTO_ZY =
			"event:266:DistributionBundle\\Events\\RefundFreightAutoZyEvent";

	public static final String LISTENER_REFUND_FREIGHT_AUTO_ZY =
			"listener:DistributionBundle\\Listeners\\RefundFreightAutoZyListener";

	private DistributionDispatchEventNames() {}
}
