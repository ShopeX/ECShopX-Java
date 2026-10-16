package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import lombok.Data;

/** 会员签到记录 */
@Data
@MpTable(
		value = "member_signin_record",
		comment = "会员签到记录",
		uniqueIndexes = {
			@MpIndex(name = "uk_member_signin_record", columns = {"company_id", "user_id", "sign_date"})
		},
		indexes = {@MpIndex(name = "ix_member_signin_record_user", columns = {"company_id", "user_id"})})
public class MemberSigninRecord {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
	private Long userId;

	@MpField(value = "sign_date", columnType = "date", comment = "签到自然日")
	private LocalDate signDate;

	@MpField(value = "weekday", columnType = "integer", comment = "1=Mon … 7=Sun")
	private Integer weekday;

	@MpField(value = "daily_points", columnType = "integer", comment = "实际发放日常积分", defaultValue = "0")
	private Integer dailyPoints = 0;

	@MpField(value = "activity_id", columnType = "bigint", nullable = true, comment = "计入的活动ID")
	private Long activityId;

	@MpField(value = "activity_seq", columnType = "integer", nullable = true, comment = "活动内第几次签到")
	private Integer activitySeq;

	@MpField(value = "created", columnType = "integer")
	private Integer created;
}
