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

package cn.shopex.ecshopx.community.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ScheduleFinishGoodsRow {

	private long goodsId;

	private long buyNum;

	/** 该 goods 在明细表上的实付金额分汇总（与 PHP sum(total_fee) 一致） */
	private int totalFeeCents;

	/** 起送量；无配置时为 0 */
	private int minDeliveryNum;
}
