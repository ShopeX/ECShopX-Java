package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.mybatis.metadata.MpMetadataAnnotationHandler;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * date_status 多值筛选必须按 OR 组合各分支；曾因悬空 {@code q.or()} 后接嵌套 {@code and(...)} 被 MyBatis-Plus 拼成 AND，导致 1,2 恒为空集。
 */
class DiscountCardsAdminListFilterBuilderDateStatusTest {

	private static final long NOW = 1789969492L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		Configuration configuration = new MybatisConfiguration();
		GlobalConfigUtils.getGlobalConfig(configuration).setAnnotationHandler(new MpMetadataAnnotationHandler());
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), DiscountCards.class);
	}

	private static String sqlFor(Object dateStatus) {
		LambdaQueryWrapper<DiscountCards> w = new DiscountCardsAdminListFilterBuilder()
				.build(141L, Map.of("date_status", dateStatus), "menu", 0L, NOW);
		return w.getTargetSql();
	}

	@Test
	@DisplayName("date_status=1,2：未生效与已生效 OR 组合，不再被 AND 掉")
	void multiDateStatusOrsBranches() {
		String sql = sqlFor("1,2");
		// 回归点：旧实现这里拼成 ") AND ((begin_date < ..."，交集恒为空。
		assertThat(sql).doesNotContain(") AND ((begin_date <");
		assertThat(sql).contains(") OR (((begin_date <");
	}

	@Test
	@DisplayName("date_status=1,2,3：三个分支两两 OR")
	void threeDateStatusOrsBranches() {
		String sql = sqlFor(List.of(1, 2, 3));
		assertThat(sql).doesNotContain(") AND ((begin_date <");
		assertThat(sql).contains(") OR ((end_date >");
	}

	@Test
	@DisplayName("date_status=2 单值行为不变")
	void singleDateStatusUnchanged() {
		assertThat(sqlFor("2")).isEqualTo(sqlFor(List.of(2))).contains("begin_date < ?").contains("end_date > ?");
	}

	@Test
	@DisplayName("date_status 非法值忽略，不产生日期条件")
	void invalidDateStatusIgnored() {
		assertThat(sqlFor("9")).doesNotContain("begin_date").doesNotContain("end_date");
	}
}
