package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

/**
 * plan §5「end_time 与哨兵 5000000000L（§8-5）」：{@link PromotionActivityCreateService#toActivityListRow} 对永久结束时间以 long
 * 与常量比较产出 {@code is_forever}，禁止依赖字符串 {@code '5000000000'}。
 */
class PromotionActivityCreateServiceToActivityListRowTest {

	private static final long FOREVER_SENTINEL = 5000000000L;

	private PromotionActivityCreateService newService() {
		return new PromotionActivityCreateService(
				mock(PromotionActivityMapper.class),
				mock(PromotionActivityMultiLangWriteService.class),
				new ObjectMapper(),
				mock(MessageSource.class));
	}

	@Test
	@DisplayName("§8-5：end_time 实体为 5000000000L 时 is_forever 为 true（Long 哨兵比较路径）")
	void toActivityListRow_foreverEnd_isForeverUsesLongSentinel() {
		PromotionActivity row = new PromotionActivity();
		row.setActivityId(1L);
		row.setCompanyId(1L);
		row.setActivityType("member_birthday");
		row.setTitle("t");
		row.setActivityStatus("valid");
		row.setBeginTime(1L);
		row.setEndTime(FOREVER_SENTINEL);

		Map<String, Object> out = newService().toActivityListRow(row);

		assertThat(out.get("is_forever")).isEqualTo(Boolean.TRUE);
		assertThat(out.get("end_time")).isNotEqualTo("5000000000");
		assertThat(out.get("end_time")).isNotEqualTo(5000000000L);
	}
}
