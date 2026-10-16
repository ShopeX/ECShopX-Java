package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员签到活动进度 */
@Data
@MpTable(
		value = "member_signin_activity_progress",
		comment = "会员签到活动进度",
		uniqueIndexes = {
			@MpIndex(
					name = "uk_member_signin_activity_progress",
					columns = {"company_id", "user_id", "activity_id"})
		})
public class MemberSigninActivityProgress {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
	private Long userId;

	@MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
	private Long activityId;

	@MpField(value = "progress_count", columnType = "integer", comment = "当前累计次数", defaultValue = "0")
	private Integer progressCount = 0;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
