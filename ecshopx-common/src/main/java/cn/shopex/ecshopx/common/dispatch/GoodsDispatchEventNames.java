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

public final class GoodsDispatchEventNames {

	public static final String EVENT_ITEM_CATEGORY_ADD = "event:goods:item_category_add";

	public static final String EVENT_ITEM_STORE_UPDATE = "event:258:GoodsBundle\\Events\\ItemStoreUpdateEvent";

	public static final String EVENT_ITEM_BATCH_EDIT_STATUS =
			"event:248:GoodsBundle\\Events\\ItemBatchEditStatusEvent";

	public static final String EVENT_ITEM_DELETE = "event:247:GoodsBundle\\Events\\ItemDeleteEvent";

	public static final String LISTENER_YOUSHU_ITEMS_ITEM_DELETE = "listener:youshu.items_item_delete";

	public static final String EVENT_ITEM_CREATE = "event:257:GoodsBundle\\Events\\ItemCreateEvent";

	public static final String EVENT_ITEM_TAG_EDIT = "event:259:GoodsBundle\\Events\\ItemTagEditEvent";

	public static final String EVENT_ITEM_ADD = "event:271:GoodsBundle\\Events\\ItemAddEvent";

	public static final String LISTENER_THIRDPARTY_ITEM_ADD_PUSH_MARKETING_CENTER =
			"listener:thirdparty.item_add_push_marketing_center";

	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_PRODUCT_SYNC_ON_ITEM_STORE_UPDATE =
			"listener:shuyun_open_platform.product_sync_on_item_store_update";

	private GoodsDispatchEventNames() {
	}
}
