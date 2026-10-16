package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员签到领奖流水（日常积分 / 累计阶梯） */
@Data
@MpTable(
		value = "member_signin_reward_grant",
		comment = "会员签到领奖流水",
		uniqueIndexes = {
			@MpIndex(
					name = "uk_member_signin_reward_grant_daily",
					columns = {"company_id", "user_id", "sign_date", "record_type"}),
			@MpIndex(
					name = "uk_member_signin_reward_grant_tier",
					columns = {"company_id", "user_id", "activity_id", "tier_count", "record_type"})
		},
		indexes = {
			@MpIndex(
					name = "ix_member_signin_reward_grant_user",
					columns = {"company_id", "user_id", "activity_id"})
		})
public class MemberSigninRewardGrant {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
	private Long userId;

	@MpField(value = "record_type", columnType = "string", length = 16, comment = "daily/tier")
	private String recordType;

	@MpField(value = "activity_id", columnType = "bigint", nullable = true, comment = "活动ID(tier必填)")
	private Long activityId;

	@MpField(value = "tier_count", columnType = "integer", nullable = true, comment = "阶梯累计次数(tier必填)")
	private Integer tierCount;

	@MpField(value = "sign_date", columnType = "date", nullable = true, comment = "签到自然日(daily)")
	private java.time.LocalDate signDate;

	@MpField(value = "title", columnType = "string", length = 64, nullable = true, comment = "C端展示标题")
	private String title;

	@MpField(value = "status", columnType = "string", length = 16, comment = "pending/granted/failed")
	private String status;

	@MpField(value = "reward_snapshot", columnType = "text", nullable = true, comment = "发放内容快照")
	private String rewardSnapshot;

	@MpField(value = "error_message", columnType = "string", length = 512, nullable = true, comment = "失败原因")
	private String errorMessage;

	@MpField(value = "signin_record_id", columnType = "bigint", nullable = true, comment = "触发签到的记录ID")
	private Long signinRecordId;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
