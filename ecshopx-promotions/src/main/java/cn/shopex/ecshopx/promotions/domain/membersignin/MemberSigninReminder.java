package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import lombok.Data;

/** 会员签到漏签提醒 */
@Data
@MpTable(
		value = "member_signin_reminder",
		comment = "会员签到漏签提醒",
		uniqueIndexes = {
			@MpIndex(name = "uk_member_signin_reminder", columns = {"company_id", "user_id"})
		})
public class MemberSigninReminder {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
	private Long userId;

	@MpField(value = "enabled", columnType = "boolean", comment = "用户开关", defaultValue = "0")
	private Boolean enabled = false;

	@MpField(value = "subscribe_accepted", columnType = "boolean", comment = "是否持有有效微信订阅授权", defaultValue = "0")
	private Boolean subscribeAccepted = false;

	@MpField(value = "subscribe_template_id", columnType = "string", length = 128, nullable = true, comment = "最近一次授权模板ID")
	private String subscribeTemplateId;

	@MpField(value = "last_push_date", columnType = "date", nullable = true, comment = "最近一次推送自然日")
	private LocalDate lastPushDate;

	@MpField(
			value = "next_push_time",
			columnType = "integer",
			nullable = true,
			comment = "已排定的下一次推送时刻(epoch秒), NULL=未排定")
	private Integer nextPushTime;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
