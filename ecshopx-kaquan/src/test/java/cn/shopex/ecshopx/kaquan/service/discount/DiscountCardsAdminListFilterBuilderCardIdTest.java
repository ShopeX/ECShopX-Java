package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.mybatis.metadata.MpMetadataAnnotationHandler;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscountCardsAdminListFilterBuilderCardIdTest {

	private static final long NOW = 1789969492L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		Configuration configuration = new MybatisConfiguration();
		GlobalConfigUtils.getGlobalConfig(configuration).setAnnotationHandler(new MpMetadataAnnotationHandler());
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), DiscountCards.class);
	}

	private static String sqlFor(Map<String, Object> merged) {
		LambdaQueryWrapper<DiscountCards> w = new DiscountCardsAdminListFilterBuilder()
				.build(141L, merged, "menu", 0L, NOW);
		return w.getTargetSql();
	}

	@Test
	@DisplayName("card_id 正整数：精确 eq")
	void validCardIdExactMatch() {
		assertThat(sqlFor(Map.of("card_id", "88001"))).contains("card_id = ?");
	}

	@Test
	@DisplayName("card_id 非数字：忽略，不加 card_id 条件")
	void invalidCardIdIgnored() {
		assertThat(sqlFor(Map.of("card_id", "abc"))).doesNotContain("card_id =");
	}

	@Test
	@DisplayName("card_id ≤0：忽略")
	void nonPositiveCardIdIgnored() {
		assertThat(sqlFor(Map.of("card_id", "0"))).doesNotContain("card_id =");
		assertThat(sqlFor(Map.of("card_id", "-1"))).doesNotContain("card_id =");
	}

	@Test
	@DisplayName("card_id 与 title 可同时生效")
	void cardIdWithTitle() {
		String sql = sqlFor(Map.of("card_id", "42", "title", "新春"));
		assertThat(sql).contains("card_id = ?");
		assertThat(sql).contains("title LIKE ?");
	}
}
