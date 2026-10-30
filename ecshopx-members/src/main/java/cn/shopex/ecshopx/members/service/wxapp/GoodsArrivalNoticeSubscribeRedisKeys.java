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

package cn.shopex.ecshopx.members.service.wxapp;

/**
 * Redis hash keys for goods-arrival subscribe UI state ({@code is_subscribe} / subscribe toggle).
 *
 * <p>Must stay aligned with {@code WxappGoodsArrivalNoticeSubscribeService} and send-side appid
 * lookup in {@code ItemStoreGoodsArrivalWxaTemplateOrchestrator}.
 */
public final class GoodsArrivalNoticeSubscribeRedisKeys {

	private static final String REDIS_KEY_PREFIX = "arrivalNoticeSub";

	private GoodsArrivalNoticeSubscribeRedisKeys() {}

	public static String hashKey(String source, long companyId, long itemId, int distributorId) {
		return hashKey(source, companyId, String.valueOf(itemId), distributorId);
	}

	public static String hashKey(String source, long companyId, String itemIdRaw, int distributorId) {
		String cid = String.valueOf(companyId);
		String iid = itemIdRaw == null ? "" : itemIdRaw;
		String src = source == null ? "" : source.trim();
		if ("wechat".equals(src)) {
			if (distributorId > 0) {
				return REDIS_KEY_PREFIX + ":" + cid + ":" + iid + ":" + distributorId;
			}
			return REDIS_KEY_PREFIX + ":" + cid + ":" + iid;
		}
		if (distributorId > 0) {
			return REDIS_KEY_PREFIX + ":" + src + ":" + cid + ":" + iid + ":" + distributorId;
		}
		return REDIS_KEY_PREFIX + ":" + src + ":" + cid + ":" + iid;
	}
}
