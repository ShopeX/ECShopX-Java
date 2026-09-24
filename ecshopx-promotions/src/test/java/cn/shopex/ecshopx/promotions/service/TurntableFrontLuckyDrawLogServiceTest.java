package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TurntableFrontLuckyDrawLogServiceTest {

	@BeforeAll
	static void initTableInfo() {
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(new MybatisConfiguration(), ""), TurntableLog.class);
	}

	@Test
	void drawLogStatusFilter_matchesAdminVisibleStatuses() {
		LambdaQueryWrapper<TurntableLog> w = new LambdaQueryWrapper<>();
		TurntableFrontLuckyDrawLogService.applyDrawLogStatusFilter(w);

		assertThat(w.getSqlSegment()).contains("IN");
		assertThat(w.getParamNameValuePairs().values())
				.contains(TurntableDrawStatus.SUCCESS, TurntableDrawStatus.GRANT_FAILED)
				.doesNotContain(TurntableDrawStatus.PROCESSING, TurntableDrawStatus.COST_FAILED);
	}
}
