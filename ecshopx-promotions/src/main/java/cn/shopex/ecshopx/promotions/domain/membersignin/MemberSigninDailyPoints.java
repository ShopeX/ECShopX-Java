package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员签到周一至周日日常积分配置（每公司一行） */
@Data
@MpTable(value = "member_signin_daily_points", comment = "会员签到周一至周日日常积分配置")
public class MemberSigninDailyPoints {

	@MpId(value = "company_id", type = IdType.INPUT, columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "points_mon", columnType = "integer", comment = "周一积分", defaultValue = "0")
	private Integer pointsMon = 0;

	@MpField(value = "points_tue", columnType = "integer", comment = "周二积分", defaultValue = "0")
	private Integer pointsTue = 0;

	@MpField(value = "points_wed", columnType = "integer", comment = "周三积分", defaultValue = "0")
	private Integer pointsWed = 0;

	@MpField(value = "points_thu", columnType = "integer", comment = "周四积分", defaultValue = "0")
	private Integer pointsThu = 0;

	@MpField(value = "points_fri", columnType = "integer", comment = "周五积分", defaultValue = "0")
	private Integer pointsFri = 0;

	@MpField(value = "points_sat", columnType = "integer", comment = "周六积分", defaultValue = "0")
	private Integer pointsSat = 0;

	@MpField(value = "points_sun", columnType = "integer", comment = "周日积分", defaultValue = "0")
	private Integer pointsSun = 0;

	@MpField(value = "rule_desc", columnType = "text", comment = "签到规则说明")
	private String ruleDesc = "";

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
