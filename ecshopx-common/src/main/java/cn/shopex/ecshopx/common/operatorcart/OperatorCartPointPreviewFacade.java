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

package cn.shopex.ecshopx.common.operatorcart;

import java.util.Map;

/**
 * POS 购物车积分抵扣预览门面（ECX-11082 后续）：按下单同口径（满减→券→积分）把积分
 * 可抵扣能力与店员输入积分的实抵结果合并进 cartdatalist 返回。
 * 字段对齐 POS 临时单信息接口：user_point / max_point / limit_point / deduct_point_rule /
 * is_open_deduct_point / point_fee / point（行级 share_points / point_fee）。
 * 仅预览，不写订单数据；cartdatalist 专用，导购端与小程序链路不受影响。
 */
public interface OperatorCartPointPreviewFacade {

	/**
	 * @param companyId  企业 id
	 * @param userId     会员 id（0 表示无会员，不应用积分抵扣）
	 * @param cartData   购物车列表返回（valid_cart 为 shop 桶列表，total_fee 为券后应付），原地更新积分字段
	 * @param pointUse   店员输入的使用积分数；null 或 0 表示仅预览可抵扣能力（余额/上限/规则）
	 */
	void applyPointPreview(long companyId, long userId, Map<String, Object> cartData, Integer pointUse);
}
