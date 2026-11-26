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

package cn.shopex.ecshopx.kaquan.service.discount;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.mybatis.metadata.MpMetadataAnnotationHandler;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ECX-11449: store_self must match self-operated distributor id 0, not a catch-all %%,%% pattern.
 */
class DiscountCardsAdminListFilterBuilderStoreSelfTest {

	private static final long NOW = 1789969492L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		Configuration configuration = new MybatisConfiguration();
		GlobalConfigUtils.getGlobalConfig(configuration).setAnnotationHandler(new MpMetadataAnnotationHandler());
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), DiscountCards.class);
	}

	private static String sqlFor(Map<String, Object> merged) {
		LambdaQueryWrapper<DiscountCards> w =
				new DiscountCardsAdminListFilterBuilder().build(141L, merged, "menu", 0L, NOW);
		return w.getTargetSql();
	}

	private static String paramSqlFor(Map<String, Object> merged) {
		LambdaQueryWrapper<DiscountCards> w =
				new DiscountCardsAdminListFilterBuilder().build(141L, merged, "menu", 0L, NOW);
		return w.getSqlSegment() + " | " + String.valueOf(w.getParamNameValuePairs());
	}

	@Test
	@DisplayName("store_self=true：匹配 ,0,，不使用会命中全店铺的 %%,%%")
	void storeSelfMatchesZeroDistributorNotCatchAll() {
		String sql = sqlFor(Map.of("store_self", "true"));
		String params = paramSqlFor(Map.of("store_self", "true"));
		assertThat(sql + " " + params).doesNotContain("%%,%%");
		assertThat(sql + " " + params).contains(",0,");
		assertThat(sql).contains("distributor_id");
	}

	@Test
	@DisplayName("store_self 未开：按 distributor_id 精确店过滤")
	void withoutStoreSelfUsesShopId() {
		String params = paramSqlFor(Map.of("distributor_id", "88"));
		assertThat(params).contains(",88,");
		assertThat(params).doesNotContain(",0,");
	}
}
