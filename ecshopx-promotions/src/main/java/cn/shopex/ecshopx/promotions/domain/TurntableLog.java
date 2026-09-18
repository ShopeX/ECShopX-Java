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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用户大转盘抽奖纪录表 */
@Data
@MpTable(
		value = "promotions_turntable_log",
		comment = "用户大转盘中奖纪录表",
		uniqueIndexes = {
			@MpIndex(
					name = "uk_turntable_req",
					columns = {"company_id", "user_id", "act_id", "request_id"})
		},
		indexes = {
			@MpIndex(name = "idx_turntable_log_status_updated", columns = {"status", "updated"}),
			@MpIndex(name = "idx_turntable_log_act_status", columns = {"act_id", "status"}),
			@MpIndex(name = "idx_turntable_log_user_act_status", columns = {"user_id", "act_id", "status"})
		})
public class TurntableLog {

	/** id */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 用户id */
	@MpField(value = "user_id", columnType = "bigint", comment = "用户id")
	private Long userId;

	/** 抽奖活动的活动id */
	@MpField(value = "act_id", columnType = "bigint", comment = "抽奖活动的活动id")
	private Long actId;

	/** 客户端幂等号 */
	@MpField(value = "request_id", columnType = "string", length = 64, nullable = true, comment = "客户端幂等号")
	private String requestId;

	/** 主状态 PROCESSING/SUCCESS/GRANT_FAILED/COST_FAILED */
	@MpField(value = "status", columnType = "string", length = 32, nullable = true, comment = "抽奖主状态")
	private String status;

	/** PROCESSING 子进度 */
	@MpField(value = "process_step", columnType = "string", length = 64, nullable = true, comment = "PROCESSING子进度")
	private String processStep;

	/** 抽奖时活动配置版本 */
	@MpField(value = "config_version", columnType = "bigint", nullable = true, comment = "抽奖时活动配置版本")
	private Long configVersion;

	/** 稳定奖项标识 */
	@MpField(value = "prize_id", columnType = "string", length = 64, nullable = true, comment = "稳定奖项标识")
	private String prizeId;

	/** 奖品名称 */
	@MpField(value = "prize_title", columnType = "string", comment = "奖品名称")
	private String prizeTitle;

	/** 奖品类型，thanks/points/coupon/coupons */
	@MpField(value = "prize_type", columnType = "string", comment = "奖品类型，points:积分，coupon：优惠券，coupons：优惠券包")
	private String prizeType;

	/** 奖品值 */
	@MpField(value = "prize_value", columnType = "text", nullable = true, comment = "奖品值")
	private String prizeValue;

	/** 转盘扇区索引 */
	@MpField(value = "sector_index", columnType = "integer", nullable = true, comment = "转盘扇区索引")
	private Integer sectorIndex;

	/** 抽奖随机数 1-100 */
	@MpField(value = "random_value", columnType = "integer", nullable = true, comment = "抽奖随机数1-100")
	private Integer randomValue;

	/** 本次扣除会员积分 */
	@MpField(value = "cost_points", columnType = "bigint", nullable = true, comment = "本次扣除会员积分")
	private Long costPoints;

	/** 库存不足转 thanks 前原奖项 */
	@MpField(value = "original_prize_id", columnType = "string", length = 64, nullable = true, comment = "原命中奖项")
	private String originalPrizeId;

	/** 发奖外部单号 */
	@MpField(value = "grant_no", columnType = "string", length = 128, nullable = true, comment = "发奖外部单号")
	private String grantNo;

	/** 业务错误码 */
	@MpField(value = "error_code", columnType = "string", length = 64, nullable = true, comment = "业务错误码")
	private String errorCode;

	/** 错误信息 */
	@MpField(value = "error_message", columnType = "string", length = 512, nullable = true, comment = "错误信息")
	private String errorMessage;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
