package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员签到活动阶梯奖励 */
@Data
@MpTable(
		value = "member_signin_activity_tier",
		comment = "会员签到活动阶梯奖励",
		uniqueIndexes = {
			@MpIndex(name = "uk_member_signin_activity_tier", columns = {"activity_id", "tier_count"})
		},
		indexes = {@MpIndex(name = "ix_member_signin_activity_tier_company", columns = {"company_id"})})
public class MemberSigninActivityTier {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
	private Long activityId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "tier_count", columnType = "integer", comment = "累计签到次数")
	private Integer tierCount;

	@MpField(value = "points_reward", columnType = "integer", nullable = true, comment = "积分奖励")
	private Integer pointsReward;

	@MpField(value = "coupon_id", columnType = "bigint", nullable = true, comment = "优惠券ID")
	private Long couponId;

	@MpField(value = "sort_order", columnType = "integer", comment = "排序", defaultValue = "0")
	private Integer sortOrder = 0;
}
