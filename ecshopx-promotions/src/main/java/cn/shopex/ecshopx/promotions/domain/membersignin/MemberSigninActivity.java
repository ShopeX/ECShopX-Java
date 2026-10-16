package cn.shopex.ecshopx.promotions.domain.membersignin;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员签到累计活动（无 status/stopped_at，展示态动态计算） */
@Data
@MpTable(
		value = "member_signin_activity",
		comment = "会员签到累计活动",
		indexes = {@MpIndex(name = "ix_member_signin_activity_company", columns = {"company_id"})})
public class MemberSigninActivity {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "name", columnType = "string", length = 50, comment = "活动名称")
	private String name;

	@MpField(value = "begin_time", columnType = "bigint", comment = "开始时间(秒级Unix)")
	private Long beginTime;

	@MpField(value = "end_time", columnType = "bigint", comment = "结束时间(秒级Unix)")
	private Long endTime;

	@MpField(value = "config_version", columnType = "bigint", comment = "配置乐观锁版本", defaultValue = "1")
	private Long configVersion = 1L;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
