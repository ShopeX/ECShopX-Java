package cn.shopex.ecshopx.common.cron.mock;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.aliyunsms.QuerySendDetailsResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class NoopAliyunsmsQuerySendDetailsClientTest {

	private static final String PLAIN_MOBILE = "13800138000";

	private ListAppender<ILoggingEvent> listAppender;
	private Logger logger;

	@BeforeEach
	void setUp() {
		listAppender = new ListAppender<>();
		listAppender.start();
		logger = (Logger) LoggerFactory.getLogger(NoopAliyunsmsQuerySendDetailsClient.class);
		logger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		logger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§4: test-cron Noop 打印 [cron-mock][aliyunsms-query-send-details] 与参数；mobile 明文不得落日志")
	void noop_emits_cron_mock_alias_and_does_not_log_mobile() {
		NoopAliyunsmsQuerySendDetailsClient n = new NoopAliyunsmsQuerySendDetailsClient();
		QuerySendDetailsResult r = n.querySendDetails(7L, PLAIN_MOBILE, "unit-biz-id", "20260426");
		assertThat(r.sendStatus()).isNull();
		assertThat(r.content()).isNull();
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("[cron-mock][aliyunsms-query-send-details] called#1")
								&& e.getFormattedMessage().contains("7")
								&& e.getFormattedMessage().contains("unit-biz-id")
								&& e.getFormattedMessage().contains("20260426"));
		assertThat(listAppender.list)
				.allMatch(
						e -> e.getFormattedMessage() == null
								|| !e.getFormattedMessage().contains(PLAIN_MOBILE));
	}

	@Test
	@DisplayName("Noop 可重入计数：连续调用 3 次，最后一次日志含 called#3")
	void noop_counts_incrementally() {
		NoopAliyunsmsQuerySendDetailsClient n = new NoopAliyunsmsQuerySendDetailsClient();
		n.querySendDetails(1L, PLAIN_MOBILE, "b1", "20260101");
		n.querySendDetails(2L, PLAIN_MOBILE, "b2", "20260102");
		n.querySendDetails(3L, PLAIN_MOBILE, "b3", "20260103");
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("called#3"));
	}
}
