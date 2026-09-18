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

package cn.shopex.ecshopx.common.members.port;

import java.util.Optional;

/** 会员收藏商品场景下只读商品快照（普通商品 / 积分商品）。 */
public interface MemberItemsFavSkuSnapshotPort {

	record SkuSnapshot(String itemName, int priceFen, String firstImageUrl, int point) {
	}

	Optional<SkuSnapshot> loadGoodsSnapshot(long itemId);

	Optional<SkuSnapshot> loadPointsmallSnapshot(long itemId);
}
