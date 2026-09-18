package cn.shopex.ecshopx.orders.service.rights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class TimesCardServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Rights.class);
	}

	@Mock
	private RightsMapper rightsMapper;

	@InjectMocks
	private TimesCardService timesCardService;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void attachDebugAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(TimesCardService.class);
		serviceLogger.setLevel(Level.DEBUG);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachAppender() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	/** plan §5：analysis §3 步骤 1、4 — 入口无抛、返回可汇总行数 */
	@Test
	@DisplayName("analysis §3 步骤 1、4：scheduleUpdateRightStatus 返回两次影响行之和")
	void scheduleUpdateRightStatus_step1And4_returnsSum() {
		when(rightsMapper.update(isNull(), any())).thenReturn(3, 5);
		assertThat(timesCardService.scheduleUpdateRightStatus()).isEqualTo(8);
		verify(rightsMapper, times(2)).update(isNull(), any());
	}

	/** plan §5：analysis §3 步骤 2.1 — 到期批更 WHERE 含 end_time */
	@Test
	@DisplayName("analysis §3 步骤 2.1：首次 update 条件含 end_time（<= 当前秒）")
	@SuppressWarnings("unchecked")
	void scheduleUpdateRightStatus_step21_expiredBranch_firstWrapperHasEndTime() {
		when(rightsMapper.update(isNull(), any())).thenReturn(1, 0);
		assertThat(timesCardService.scheduleUpdateRightStatus()).isEqualTo(1);
		ArgumentCaptor<LambdaUpdateWrapper<Rights>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(rightsMapper, times(2)).update(isNull(), cap.capture());
		String seg0 = cap.getAllValues().get(0).getSqlSegment();
		assertThat(seg0).contains("end_time");
		assertThat(cap.getAllValues().get(1).getSqlSegment()).contains("is_not_limit_num");
	}

	/** plan §5：analysis §3 步骤 2.2 + Repository C — is_not_limit_num=2 且 total_num<=total_consum_num */
	@Test
	@DisplayName("analysis §3 步骤 2.2 + Repository C：第二次 update 含 is_not_limit_num 与 total_num 比较")
	@SuppressWarnings("unchecked")
	void scheduleUpdateRightStatus_step22_invalidBranch_applyTotalNum() {
		when(rightsMapper.update(isNull(), any())).thenReturn(0, 1);
		assertThat(timesCardService.scheduleUpdateRightStatus()).isEqualTo(1);
		ArgumentCaptor<LambdaUpdateWrapper<Rights>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(rightsMapper, times(2)).update(isNull(), cap.capture());
		assertThat(cap.getAllValues().get(0).getSqlSegment()).contains("end_time");
		String seg1 = cap.getAllValues().get(1).getSqlSegment();
		assertThat(seg1).contains("is_not_limit_num");
		assertThat(seg1).contains("total_num");
		assertThat(seg1).contains("total_consum_num");
	}

	/** plan §5：2.1 与 2.2 独立两次批更、行数可累加（语义无交集依赖由 SQL 条件保证） */
	@Test
	@DisplayName("analysis §3 步骤 2.1 / 2.2：两次批更各影响一行时行数相加")
	@SuppressWarnings("unchecked")
	void scheduleUpdateRightStatus_step21And22_disjointCountsSum() {
		when(rightsMapper.update(isNull(), any())).thenReturn(1, 1);
		assertThat(timesCardService.scheduleUpdateRightStatus()).isEqualTo(2);
		ArgumentCaptor<LambdaUpdateWrapper<Rights>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(rightsMapper, times(2)).update(isNull(), cap.capture());
		assertThat(cap.getAllValues().get(0).getSqlSegment()).contains("end_time");
		assertThat(cap.getAllValues().get(1).getSqlSegment()).contains("is_not_limit_num");
	}

	/** plan §5：analysis §3 步骤 3–4 */
	@Test
	@DisplayName("analysis §3 步骤 3–4：Mapper 抛错则吞异常、debug 日志、返回 0")
	void scheduleUpdateRightStatus_step3And4_mapperThrows_returnsZeroAndDebugLog() {
		when(rightsMapper.update(isNull(), any())).thenThrow(new RuntimeException("boom"));
		assertThat(timesCardService.scheduleUpdateRightStatus()).isZero();
		verify(rightsMapper, times(1)).update(isNull(), any());
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().startsWith("定时修改权益的状态出错:"));
	}
}
