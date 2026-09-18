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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一批待异步/内联统计的订单明细行。 */
public record GoodsStatisticsBatch(List<GoodsDailyStatLineKey> lines) {

	public GoodsStatisticsBatch {
		if (lines == null) {
			lines = List.of();
		} else {
			lines = Collections.unmodifiableList(new ArrayList<>(lines));
		}
	}
}
