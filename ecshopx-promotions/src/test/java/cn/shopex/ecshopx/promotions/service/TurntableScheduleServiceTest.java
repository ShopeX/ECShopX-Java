package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.common.cron.port.TurntableClearSurplusTimesRedisPort;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurntableScheduleServiceTest {

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private LuckyDrawActivityMapper luckyDrawActivityMapper;

	@Mock
	private TurntableClearSurplusTimesRedisPort port;

	private TurntableScheduleService service;

	@BeforeEach
	void init() {
		service = new TurntableScheduleService(operatorsMapper, luckyDrawActivityMapper, new ObjectMapper(), port);
	}

	@Test
	@DisplayName("analysis 1.3–1.4: 错位分页无记录，无 Redis 删除")
	void schedule_pageEmpty_noRedis() {
		Page<Operators> empty = new Page<>(2000, 1);
		empty.setRecords(Collections.emptyList());
		when(operatorsMapper.selectPage(any(Page.class), any())).thenReturn(empty);
		TurntableScheduleService.ScheduleClearResult r = service.scheduleClearTurntableTimesOver();
		assertThat(r.operatorRowsPulled()).isZero();
		assertThat(r.redisKeysDeleted()).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 1.5: 命中一页时遍历 company 并进入 clear 链")
	void schedule_oneOperator_pulledOne_noActivity() {
		Operators op = new Operators();
		op.setCompanyId(7L);
		Page<Operators> p = new Page<>(2000, 1);
		p.setRecords(List.of(op));
		when(operatorsMapper.selectPage(any(Page.class), any())).thenReturn(p);
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(null);
		TurntableScheduleService.ScheduleClearResult r = service.scheduleClearTurntableTimesOver();
		assertThat(r.operatorRowsPulled()).isEqualTo(1);
		assertThat(r.redisKeysDeleted()).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 2.1–2.2: 无活动行不删")
	void clearForCompany_noActivity() {
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(null);
		assertThat(service.clearTurntableTimesOverForCompany(1L)).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 2.3: 模板 JSON 缺省 clear_times_after_end 视为 0，不删")
	void clearForCompany_23_defaultsToZero_noDelete() {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(0L);
		row.setActivityTemplateConfig("{}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(2L)).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 2.4: 非长期 + 结束后清次数 + 已结束 => 1 次删除")
	void clearForCompany_24_deletes() {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(0L);
		row.setActivityTemplateConfig("{\"long_term\":\"0\",\"clear_times_after_end\":\"1\"}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(3L)).isEqualTo(1);
		verify(port, times(1)).deleteEntireSurplusTimesKey(3L);
	}

	@Test
	@DisplayName("analysis 2.3: 数字 1 与 JSON 少键合并后可删，语义对齐 PHP 宽松量")
	void clearForCompany_23_jsonNumberAndMissingLongTerm() {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(0L);
		row.setActivityTemplateConfig("{\"clear_times_after_end\":1}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(4L)).isEqualTo(1);
		verify(port, times(1)).deleteEntireSurplusTimesKey(4L);
	}

	@Test
	@DisplayName("analysis 2.5: 长期活动不删")
	void clearForCompany_25_longTerm() {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(0L);
		row.setActivityTemplateConfig("{\"long_term\":\"1\",\"clear_times_after_end\":\"1\"}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(5L)).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 2.5: 未结束不删")
	void clearForCompany_25_endInFuture() {
		long farFuture = 4102444800L;
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(farFuture);
		row.setActivityTemplateConfig("{\"long_term\":\"0\",\"clear_times_after_end\":\"1\"}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(6L)).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}

	@Test
	@DisplayName("analysis 2.5: clear_times 非 1 不删")
	void clearForCompany_25_clearFlagOff() {
		LuckyDrawActivity row = new LuckyDrawActivity();
		row.setEndTime(0L);
		row.setActivityTemplateConfig("{\"long_term\":\"0\",\"clear_times_after_end\":\"0\"}");
		when(luckyDrawActivityMapper.selectOne(any())).thenReturn(row);
		assertThat(service.clearTurntableTimesOverForCompany(7L)).isZero();
		verify(port, never()).deleteEntireSurplusTimesKey(anyLong());
	}
}
