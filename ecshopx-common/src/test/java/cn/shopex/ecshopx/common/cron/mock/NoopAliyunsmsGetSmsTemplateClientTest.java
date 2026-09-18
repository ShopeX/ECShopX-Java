package cn.shopex.ecshopx.common.cron.mock;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class NoopAliyunsmsGetSmsTemplateClientTest {

	private ListAppender<ILoggingEvent> listAppender;
	private Logger logger;

	@BeforeEach
	void setUp() {
		listAppender = new ListAppender<>();
		listAppender.start();
		logger = (Logger) LoggerFactory.getLogger(NoopAliyunsmsGetSmsTemplateClient.class);
		logger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		logger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§4: test-cron Noop 打印 [cron-mock][aliyunsms-get-sms-template] 与参数")
	void noop_emits_cron_mock_alias() {
		NoopAliyunsmsGetSmsTemplateClient n = new NoopAliyunsmsGetSmsTemplateClient();
		assertThat(n.getSmsTemplate(7L, "unit-template").templateStatus()).isNull();
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("[cron-mock][aliyunsms-get-sms-template] called#1")
								&& e.getFormattedMessage().contains("7")
								&& e.getFormattedMessage().contains("unit-template"));
	}
}
