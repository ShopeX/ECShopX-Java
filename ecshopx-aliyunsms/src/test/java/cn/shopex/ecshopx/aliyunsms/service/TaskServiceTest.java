package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.AccessKeyMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 覆盖 analysis §3 / plan §5：分支 1、1-1、2、3、3-1～3-5 及 3-2-1/3-2-2/3-4-1。
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

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

	private static Task newTask(long id, int totalNum) {
		Task t = new Task();
		t.setId(id);
		t.setStatus("1");
		t.setIsSend(1);
		t.setTotalNum(totalNum);
		return t;
	}

	private static Page<Task> pageOf(List<Task> records) {
		Page<Task> p = new Page<>(1, 100);
		p.setRecords(records);
		p.setTotal(records.size());
		return p;
	}

	@Test
	@DisplayName("§3: 1-1 无任务早退出，返回 0")
	void branch_1_1_empty() {
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(Collections.emptyList()));
		assertThat(taskService.scheduleUpdateStatus()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(recordMapper, never()).selectCount(any());
	}

	@Test
	@DisplayName("§3: 1 列表查询语义 status=1、is_send=1、第1页100条、无 ORDER BY")
	void branch_1_list_semantics() {
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(Collections.emptyList()));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<QueryWrapper<Task>> wCap = ArgumentCaptor.forClass(QueryWrapper.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Page<Task>> pCap = ArgumentCaptor.forClass(Page.class);
		taskService.scheduleUpdateStatus();
		verify(taskMapper).selectPage(pCap.capture(), wCap.capture());
		assertThat(pCap.getValue().getCurrent()).isEqualTo(1L);
		assertThat(pCap.getValue().getSize()).isEqualTo(100L);
		String seg = wCap.getValue().getCustomSqlSegment();
		assertThat(seg).contains("status").contains("is_send");
		assertThat(seg).doesNotContainIgnoringCase("ORDER BY");
	}

	@Test
	@DisplayName("§3: 2 Record 只读，不对 aliyunsms_record 做写操作")
	void branch_2_record_readonly() {
		Task t = newTask(10L, 1);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(t)));
		when(recordMapper.selectCount(any(QueryWrapper.class)))
				.thenReturn(0L) // succ
				.thenReturn(0L); // fail
		taskService.scheduleUpdateStatus();
		verify(recordMapper, never()).insert(any(Record.class));
		verify(recordMapper, never()).update(any(Record.class), any());
	}

	@Test
	@DisplayName("§3: 3-1 + 3-2 + 3-2-1 存在成功记录则回写任务为发送成功(2)，计数 +1")
	void branch_3_1_3_2_3_2_1() {
		Task t = newTask(1L, 2);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(t)));
		when(recordMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		assertThat(taskService.scheduleUpdateStatus()).isEqualTo(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getParamNameValuePairs().values()).contains("2");
		verify(taskMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("§3: 3-2-2 有成功则短路，不进入全失败(3) 判断")
	void branch_3_2_2_short_circuit() {
		Task t = newTask(2L, 1);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(t)));
		when(recordMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
		assertThat(taskService.scheduleUpdateStatus()).isEqualTo(1);
		verify(recordMapper, times(1)).selectCount(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getParamNameValuePairs().values()).contains("2").doesNotContain("3");
	}

	@Test
	@DisplayName("§3: 3-3 + 3-4 + 3-4-1 无成功、失败条数==total_num 则回写为失败(3) 与 failed_num")
	void branch_3_3_3_4_3_4_1() {
		Task t = newTask(99L, 2);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(t)));
		when(recordMapper.selectCount(any(QueryWrapper.class)))
				.thenReturn(0L) // succ
				.thenReturn(2L); // fail, equals total
		assertThat(taskService.scheduleUpdateStatus()).isEqualTo(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<UpdateWrapper<Task>> uCap = ArgumentCaptor.forClass(UpdateWrapper.class);
		verify(taskMapper).update(isNull(), uCap.capture());
		assertThat(uCap.getValue().getSqlSet()).contains("failed_num");
		assertThat(uCap.getValue().getParamNameValuePairs().values()).contains("3", 2);
	}

	@Test
	@DisplayName("§3: 3-5 无成功、失败数与 total 不等，不更新任务行")
	void branch_3_5_no_update() {
		Task t = newTask(4L, 3);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(t)));
		when(recordMapper.selectCount(any(QueryWrapper.class)))
				.thenReturn(0L)
				.thenReturn(1L);
		assertThat(taskService.scheduleUpdateStatus()).isZero();
		verify(taskMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("§3: 3 两条任务分走成功与跳过，更新笔数=1")
	void branch_3_two_tasks() {
		Task a = newTask(5L, 1);
		Task b = newTask(6L, 2);
		when(taskMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
				.thenReturn(pageOf(List.of(a, b)));
		when(recordMapper.selectCount(any(QueryWrapper.class)))
				.thenReturn(1L) // a succ
				.thenReturn(0L) // b succ
				.thenReturn(1L); // b fail != total
		assertThat(taskService.scheduleUpdateStatus()).isEqualTo(1);
		verify(taskMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}
}
