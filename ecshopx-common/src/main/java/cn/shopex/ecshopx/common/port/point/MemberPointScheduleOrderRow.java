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

package cn.shopex.ecshopx.common.port.point;

/**
 * 待发积分批处理选中的普通订单行（字段与排程过滤及 PHP SendMemberPoint 主循环一致）。
 */
public record MemberPointScheduleOrderRow(
		long orderId,
		long companyId,
		long userId,
		long endTime,
		int getPointType,
		int getPoints,
		int extraPoints,
		/** 订单 total_fee，单位分 */
		long totalFeeCents,
		int freightFee,
		int pointFee) {}
