package cn.shopex.ecshopx.companys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.companys.domain.OperatorLogs;
import cn.shopex.ecshopx.companys.mapper.OperatorLogsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class OperatorLogsServiceTest {

	private static final ZoneId TZ = ZoneId.of("Asia/Shanghai");
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2020-06-15T10:00:00Z"), TZ);

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OperatorLogs.class);
	}

	@Mock
	private OperatorLogsMapper operatorLogsMapper;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void attachServiceLog() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(OperatorLogsService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void detach() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3.2 左枝 配置空/0：阈值=当前时刻减 3 自然月，delete 入参与 INFO 含阈值日")
	@SuppressWarnings("unchecked")
	void thresholdConfigFalsy_usesThreeMonths() {
		MockEnvironment env = new MockEnvironment();
		OperatorLogsService svc = new OperatorLogsService(operatorLogsMapper, env, FIXED_CLOCK, TZ);
		ZonedDateTime now = ZonedDateTime.ofInstant(FIXED_CLOCK.instant(), TZ);
		int expectedSec = (int) now.minusMonths(3).toEpochSecond();
		String day =
				ZonedDateTime.ofInstant(Instant.ofEpochSecond((long) expectedSec), TZ)
						.toLocalDate()
						.toString();
		when(operatorLogsMapper.delete(any())).thenReturn(2);
		int out = svc.scheduleDeleteOperatorLogs();
		assertThat(out).isEqualTo(2);
		ArgumentCaptor<LambdaQueryWrapper<OperatorLogs>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(operatorLogsMapper).delete(cap.capture());
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("created");
		assertThat(cap.getValue().getParamNameValuePairs().values()).contains(expectedSec);
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("开始执行删除操作日志脚本")
								&& e.getFormattedMessage().contains(day)
								&& e.getFormattedMessage().contains("之前"));
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("共删除2条"));
	}

	@Test
	@DisplayName("§3.2 右枝 common.del-operator-logs-date=7：阈值=now-7 天")
	@SuppressWarnings("unchecked")
	void thresholdConfigDays_usesNdays() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("common.del-operator-logs-date", "7");
		OperatorLogsService svc = new OperatorLogsService(operatorLogsMapper, env, FIXED_CLOCK, TZ);
		when(operatorLogsMapper.delete(any())).thenReturn(1);
		svc.scheduleDeleteOperatorLogs();
		ArgumentCaptor<LambdaQueryWrapper<OperatorLogs>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(operatorLogsMapper).delete(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("created");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null && e.getFormattedMessage().contains("2020-06-08"));
	}

	@Test
	@DisplayName("零行删除：delete 返回 0")
	@SuppressWarnings("unchecked")
	void deleteReturnsZero() {
		MockEnvironment env = new MockEnvironment();
		OperatorLogsService svc = new OperatorLogsService(operatorLogsMapper, env, FIXED_CLOCK, TZ);
		when(operatorLogsMapper.delete(any())).thenReturn(0);
		int out = svc.scheduleDeleteOperatorLogs();
		assertThat(out).isEqualTo(0);
		ArgumentCaptor<LambdaQueryWrapper<OperatorLogs>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(operatorLogsMapper).delete(cap.capture());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("共删除0条"));
	}

	@Test
	@DisplayName("多行物理删除：返回 mapper 行数")
	@SuppressWarnings("unchecked")
	void deleteReturnsRowCount() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("common.del-operator-logs-date", "30");
		OperatorLogsService svc = new OperatorLogsService(operatorLogsMapper, env, FIXED_CLOCK, TZ);
		when(operatorLogsMapper.delete(any())).thenReturn(5);
		int out = svc.scheduleDeleteOperatorLogs();
		assertThat(out).isEqualTo(5);
		verify(operatorLogsMapper).delete(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("共删除5条"));
	}
}
