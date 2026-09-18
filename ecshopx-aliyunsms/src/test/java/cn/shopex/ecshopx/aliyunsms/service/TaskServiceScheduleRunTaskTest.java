package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.aliyunsms.domain.AccessKey;
import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.aliyunsms.MassTaskSendSmsResult;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.dto.AliyunsmsRunTaskMemberRow;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class TaskServiceScheduleRunTaskTest {

	@Mock
	private TaskMapper taskMapper;

	@Mock
	private RecordMapper recordMapper;

	@Mock
	private AccessKeyMapper accessKeyMapper;

	@Mock
	private TemplateMapper templateMapper;

	@Mock
	private SignMapper signMapper;

	@Mock
	private SceneMapper sceneMapper;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private AliyunsmsMassTaskSendClient aliyunsmsMassTaskSendClient;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Mock
	private AliyunsmsAddSmsBatchRecordJobDispatchPublisher addSmsBatchRecordJobDispatchPublisher;

	@InjectMocks
	private TaskService taskService;

	private ListAppender<ILoggingEvent> serviceLog;
	private Logger taskServiceLogger;

	@BeforeEach
	void attachServiceLog() {
		serviceLog = new ListAppender<>();
		serviceLog.start();
		serviceLog.list.clear();
		taskServiceLogger = (Logger) LoggerFactory.getLogger(TaskService.class);
		taskServiceLogger.addAppender(serviceLog);
	}

	@AfterEach
	void detachServiceLog() {
		if (taskServiceLogger != null && serviceLog != null) {
			taskServiceLogger.detachAppender(serviceLog);
			serviceLog.stop();
		}
	}

	private static Page<Task> pageOf(List<Task> records) {
		Page<Task> p = new Page<>(1, 100);
		p.setRecords(records);
		p.setTotal(records.size());
		return p;
	}

	@Test
	@DisplayName(
			"§1, §3-0, §4：无待办（selectPage 第1页0条，status=1/is_send=0/send_at<=now 由 Wrapper 表示）；不进入 3-1～3-5；返回 0")
	void empty_list_returns_zero() {
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(Collections.emptyList()));
		assertThat(taskService.scheduleRunTask()).isZero();
		verify(membersMapper, never()).selectMembersForAliyunsmsRunTask(anyLong(), any());
		verify(recordMapper, never()).insert(any(Record.class));
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName(
			"§1, §2, §3, §3-1, §3-2, §3-3(两号), §3-4, §3-4-1, §3-4-1-a, §3-4-1-b, §3-4-1-c, §3-4-1-d, §3-4-1-e, §3-4-1-f, §3-4-2, §4：双手机号全链路，insert=2 同 biz_id，is_send=1，返回 1")
	void one_task_success_two_mobiles() {
		Task t = new Task();
		t.setId(7L);
		t.setCompanyId(100L);
		t.setUserId("1,2");
		t.setTemplateId(10);
		t.setSignId(20);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));

		AccessKey ak = new AccessKey();
		ak.setAccesskeyId("kid");
		ak.setAccesskeySecret("sec");
		when(accessKeyMapper.selectOne(any())).thenReturn(ak);

		AliyunsmsRunTaskMemberRow r1 = new AliyunsmsRunTaskMemberRow();
		r1.setUserId(1L);
		r1.setMobileEnc("C1");
		AliyunsmsRunTaskMemberRow r2 = new AliyunsmsRunTaskMemberRow();
		r2.setUserId(2L);
		r2.setMobileEnc("C2");
		when(membersMapper.selectMembersForAliyunsmsRunTask(100L, List.of(1L, 2L))).thenReturn(List.of(r1, r2));
		when(sensitiveFieldEncryptor.decrypt("C1")).thenReturn("13000000001");
		when(sensitiveFieldEncryptor.decrypt("C2")).thenReturn("13000000002");

		Template template = new Template();
		template.setStatus("1");
		template.setTemplateCode("TCODE");
		template.setTemplateContent("hello");
		template.setTemplateType("2");
		when(templateMapper.selectById(10L)).thenReturn(template);

		Sign sign = new Sign();
		sign.setSignName("SIGN");
		when(signMapper.selectOne(any())).thenReturn(sign);

		when(aliyunsmsMassTaskSendClient.sendMassSms(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
				.thenReturn(new MassTaskSendSmsResult("OK", "OK", "BIZ-1"));

		Scene scene = new Scene();
		scene.setId(99L);
		when(sceneMapper.selectOne(any())).thenReturn(scene);

		assertThat(taskService.scheduleRunTask()).isEqualTo(1);

		verify(addSmsBatchRecordJobDispatchPublisher)
				.publish(
						eq(100L),
						eq(7),
						argThat(
								m ->
										m.size() == 2
												&& "13000000001".equals(m.get(0))
												&& "13000000002".equals(m.get(1))),
						eq(99),
						eq("TCODE"),
						eq("2"),
						eq("【SIGN】hello"),
						eq(1),
						eq("BIZ-1"));
		verify(recordMapper, never()).insert(any(Record.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getSqlSet()).contains("is_send");
	}

	@Test
	@DisplayName(
			"§1, §3, §3-1, §3-2, §3-3, §3-4, §3-4-1, §3-4-1-a, §3-5, §3-5-1, §3-5-2：无模板行；ERROR 含「执行群发短信任务」与异常文案；is_send 未改，返回 0")
	void template_invalid_no_update() {
		Task t = new Task();
		t.setId(1L);
		t.setCompanyId(1L);
		t.setUserId("1");
		t.setTemplateId(99);
		t.setSignId(1);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));

		AccessKey ak = new AccessKey();
		ak.setAccesskeyId("k");
		ak.setAccesskeySecret("s");
		when(accessKeyMapper.selectOne(any())).thenReturn(ak);
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(Collections.emptyList());
		when(templateMapper.selectById(99L)).thenReturn(null);

		assertThat(taskService.scheduleRunTask()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(recordMapper, never()).insert(any(Record.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());

		assertThat(serviceLog.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("执行群发短信任务")
								&& e.getFormattedMessage().contains("不能发送"));
	}

	@Test
	@DisplayName("§1, §3, §3-1, §3-4, §3-4-1, §3-4-1-b, §3-5, §3-5-1, §3-5-2：无有效签名行；不更 is_send，ERROR 含 3-5-1 前缀")
	void sign_invalid_3_4_1_b() {
		Task t = newTask(2L, 1L, "1", 10, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));
		stubAccessKey(1L);
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(List.of());
		Template template = newTemplateOk();
		when(templateMapper.selectById(10L)).thenReturn(template);
		when(signMapper.selectOne(any())).thenReturn(null);

		assertThat(taskService.scheduleRunTask()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());
		assertThat(serviceLog.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("执行群发短信任务"));
	}

	@Test
	@DisplayName("§1, §3, §3-1～§3-4-1, §3-4-1-c, §3-5, §3-5-1, §3-5-2：SendSms 非 OK；不更 is_send；ERROR 含消息")
	void send_sms_not_ok_3_4_1_c() {
		Task t = newTask(3L, 1L, "1", 10, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));
		stubAccessKey(1L);
		AliyunsmsRunTaskMemberRow r = new AliyunsmsRunTaskMemberRow();
		r.setUserId(1L);
		r.setMobileEnc("C");
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(List.of(r));
		when(sensitiveFieldEncryptor.decrypt("C")).thenReturn("13000000000");
		when(templateMapper.selectById(10L)).thenReturn(newTemplateOk());
		when(signMapper.selectOne(any())).thenReturn(newSign());
		when(aliyunsmsMassTaskSendClient.sendMassSms(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
				.thenReturn(new MassTaskSendSmsResult("isv.ERROR", "downstream", null));

		assertThat(taskService.scheduleRunTask()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());
		assertThat(serviceLog.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("执行群发短信任务"));
	}

	@Test
	@DisplayName("§1, §3, §3-1～§3-4-1, §3-4-1-c, §3-4-1-d, §3-5, §3-5-1, §3-5-2：无 template_type=2 推广 scene；不 NPE，不更 is_send")
	void no_scene_3_4_1_d() {
		Task t = newTask(4L, 1L, "1", 10, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));
		stubAccessKey(1L);
		AliyunsmsRunTaskMemberRow r = new AliyunsmsRunTaskMemberRow();
		r.setUserId(1L);
		r.setMobileEnc("C");
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(List.of(r));
		when(sensitiveFieldEncryptor.decrypt("C")).thenReturn("13000000000");
		when(templateMapper.selectById(10L)).thenReturn(newTemplateOk());
		when(signMapper.selectOne(any())).thenReturn(newSign());
		when(aliyunsmsMassTaskSendClient.sendMassSms(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
				.thenReturn(new MassTaskSendSmsResult("OK", "OK", "BIZ"));
		when(sceneMapper.selectOne(any())).thenReturn(null);

		assertThat(taskService.scheduleRunTask()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(recordMapper, never()).insert(any(Record.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());
	}

	@Test
	@DisplayName("§1, §3, §3-1(缺 AccessKey 行以触达 §3-5 前), §3-4, §3-5, §3-5-1, §3-5-2：AccessKey 缺失；不更 is_send")
	void missing_access_key_3_1_3_5() {
		Task t = newTask(5L, 1L, "1", 10, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));
		when(accessKeyMapper.selectOne(any())).thenReturn(null);

		assertThat(taskService.scheduleRunTask()).isZero();
		verify(membersMapper, never()).selectMembersForAliyunsmsRunTask(anyLong(), any());
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(addSmsBatchRecordJobDispatchPublisher, never())
				.publish(anyLong(), anyInt(), any(), anyInt(), any(), any(), any(), anyInt(), any());
		assertThat(serviceLog.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("执行群发短信任务"));
	}

	@Test
	@DisplayName(
			"§1, §3, §2, §3-3(空), §3-4, §3-4-1, §3-4-1-c(空号串), §3-4-1-e(0 行), §3-4-1-f, §3-4-2, §4：无手机号仍 Send+落库顺序内 0 条 record，is_send=1，返回 1")
	void empty_mobiles_still_3_3_3_4_1() {
		Task t = newTask(6L, 1L, "1", 10, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(pageOf(List.of(t)));
		stubAccessKey(1L);
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(List.of());
		when(templateMapper.selectById(10L)).thenReturn(newTemplateOk());
		when(signMapper.selectOne(any())).thenReturn(newSign());
		when(aliyunsmsMassTaskSendClient.sendMassSms(anyString(), anyString(), eq(""), anyString(), anyString(), any()))
				.thenReturn(new MassTaskSendSmsResult("OK", "OK", "BIZ-E"));
		Scene scene = new Scene();
		scene.setId(1L);
		when(sceneMapper.selectOne(any())).thenReturn(scene);

		assertThat(taskService.scheduleRunTask()).isEqualTo(1);
		verify(addSmsBatchRecordJobDispatchPublisher)
				.publish(
						eq(1L),
						eq(6),
						argThat(List::isEmpty),
						eq(1),
						eq("TCODE"),
						eq("2"),
						eq("【S】hello"),
						eq(1),
						eq("BIZ-E"));
		verify(recordMapper, never()).insert(any(Record.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getSqlSet()).contains("is_send");
	}

	@Test
	@DisplayName(
			"§1, §2, §3, §3-4, §3-4-1, §3-4-1-a, §3-5, §3-5-1, §3-5-2, §3-4-2, §4：双 task 首条全绿次条模板缺失；仅首条 is_send=1，返回 1，失败条 3-5-1 打 ERROR")
	void two_tasks_one_ok_one_fails_3_5() {
		Task tOk = newTask(7L, 1L, "1", 10, 5);
		Task tBad = newTask(8L, 1L, "2", 20, 5);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(tOk, tBad)));
		stubAccessKey(1L);

		AliyunsmsRunTaskMemberRow m1 = new AliyunsmsRunTaskMemberRow();
		m1.setUserId(1L);
		m1.setMobileEnc("C1");
		AliyunsmsRunTaskMemberRow m2 = new AliyunsmsRunTaskMemberRow();
		m2.setUserId(2L);
		m2.setMobileEnc("C2");
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(1L))).thenReturn(List.of(m1));
		when(membersMapper.selectMembersForAliyunsmsRunTask(1L, List.of(2L))).thenReturn(List.of(m2));
		when(sensitiveFieldEncryptor.decrypt("C1")).thenReturn("13000000001");
		when(sensitiveFieldEncryptor.decrypt("C2")).thenReturn("13000000002");
		Template t10 = newTemplateOk();
		when(templateMapper.selectById(10L)).thenReturn(t10);
		when(templateMapper.selectById(20L)).thenReturn(null);
		when(signMapper.selectOne(any())).thenReturn(newSign());
		when(aliyunsmsMassTaskSendClient.sendMassSms(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
				.thenReturn(new MassTaskSendSmsResult("OK", "OK", "BIZ-M"));
		Scene scene = new Scene();
		scene.setId(2L);
		when(sceneMapper.selectOne(any())).thenReturn(scene);

		assertThat(taskService.scheduleRunTask()).isEqualTo(1);
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper, times(1)).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getSqlSet()).contains("is_send");
		verify(addSmsBatchRecordJobDispatchPublisher)
				.publish(
						eq(1L),
						eq(7),
						argThat(m -> m.size() == 1 && "13000000001".equals(m.get(0))),
						eq(2),
						eq("TCODE"),
						eq("2"),
						eq("【S】hello"),
						eq(1),
						eq("BIZ-M"));
		verify(recordMapper, never()).insert(any(Record.class));
		long errLines =
				serviceLog.list.stream().filter(e -> e.getLevel() == Level.ERROR).count();
		assertThat(errLines).isEqualTo(1);
		assertThat(serviceLog.list)
				.anyMatch(
						e -> e.getLevel() == Level.ERROR
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("执行群发短信任务"));
	}

	private static Task newTask(long id, long companyId, String userIds, int templateId, int signId) {
		Task t = new Task();
		t.setId(id);
		t.setCompanyId(companyId);
		t.setUserId(userIds);
		t.setTemplateId(templateId);
		t.setSignId(signId);
		return t;
	}

	private void stubAccessKey(long companyId) {
		AccessKey ak = new AccessKey();
		ak.setAccesskeyId("k");
		ak.setAccesskeySecret("s");
		when(accessKeyMapper.selectOne(any())).thenReturn(ak);
	}

	private static Template newTemplateOk() {
		Template template = new Template();
		template.setStatus("1");
		template.setTemplateCode("TCODE");
		template.setTemplateContent("hello");
		template.setTemplateType("2");
		return template;
	}

	private static Sign newSign() {
		Sign s = new Sign();
		s.setSignName("S");
		return s;
	}
}
