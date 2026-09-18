package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySendDetailJobDispatchPublisher;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

	@Mock
	private RecordMapper recordMapper;

	@Mock
	private AliyunsmsQuerySendDetailJobDispatchPublisher querySendDetailJobDispatchPublisher;

	@Mock
	private SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@InjectMocks
	private SmsService smsService;

	private static Record newRecord(
			Long id, Long companyId, String mobileCipher, String bizId, Integer created) {
		Record r = new Record();
		r.setId(id);
		r.setCompanyId(companyId);
		r.setMobile(mobileCipher);
		r.setBizId(bizId);
		r.setCreated(created);
		r.setStatus("1");
		return r;
	}

	@Test
	@DisplayName("empty pending list returns zero and does not publish")
	void schedule_empty_pending_returns_zero() {
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
		assertThat(smsService.scheduleQuerySendDetail()).isZero();
		verify(querySendDetailJobDispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyString(), anyString(), anyInt());
		verify(sensitiveFieldEncryptor, never()).decrypt(any());
	}

	@Test
	@DisplayName("selectList uses status=1, LIMIT 100, no ORDER BY")
	void schedule_selectList_semantics() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<QueryWrapper<Record>> listCap = ArgumentCaptor.forClass(QueryWrapper.class);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
		smsService.scheduleQuerySendDetail();
		verify(recordMapper).selectList(listCap.capture());
		QueryWrapper<Record> captured = listCap.getValue();
		String sqlSegment = captured.getCustomSqlSegment();
		assertThat(sqlSegment).contains("status").contains("LIMIT 100");
		assertThat(sqlSegment).doesNotContainIgnoringCase("ORDER BY");
		assertThat(captured.getParamNameValuePairs().values()).contains("1");
	}

	@Test
	@DisplayName("one valid row decrypts mobile and publishes once; return value counts publishes")
	void schedule_one_row_publishes() {
		Record row = newRecord(7L, 10L, "CIPHER_M1", "BIZ1", 1_800_000_000);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		when(sensitiveFieldEncryptor.decrypt("CIPHER_M1")).thenReturn("13800138000");

		assertThat(smsService.scheduleQuerySendDetail()).isEqualTo(1);

		verify(querySendDetailJobDispatchPublisher).publish(10L, 7L, "13800138000", "BIZ1", 1_800_000_000);
		verify(recordMapper, never()).selectOne(any());
		verify(recordMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("two valid rows publish twice")
	void schedule_two_rows_publishes_twice() {
		Record a = newRecord(21L, 11L, "CIPHER_MA", "BZ_A", 1_800_001_000);
		Record b = newRecord(22L, 22L, "CIPHER_MB", "BZ_B", 1_800_002_000);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(a, b));
		when(sensitiveFieldEncryptor.decrypt("CIPHER_MA")).thenReturn("13000000001");
		when(sensitiveFieldEncryptor.decrypt("CIPHER_MB")).thenReturn("13000000002");

		assertThat(smsService.scheduleQuerySendDetail()).isEqualTo(2);

		verify(querySendDetailJobDispatchPublisher).publish(11L, 21L, "13000000001", "BZ_A", 1_800_001_000);
		verify(querySendDetailJobDispatchPublisher).publish(22L, 22L, "13000000002", "BZ_B", 1_800_002_000);
	}

	@Test
	@DisplayName("mobile cipher null or empty skips without decrypt or publish")
	void schedule_mobile_cipher_blank_skip() {
		Record r1 = newRecord(4L, 4L, null, "BIZ_A", 1_800_000_400);
		Record r2 = newRecord(5L, 5L, "", "BIZ_B", 1_800_000_500);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r1, r2));

		assertThat(smsService.scheduleQuerySendDetail()).isZero();

		verify(sensitiveFieldEncryptor, never()).decrypt(any());
		verify(querySendDetailJobDispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("empty plaintext after decrypt skips publish")
	void schedule_mobile_plain_empty_skip() {
		Record r = newRecord(6L, 6L, "CIPHER_EMPTY", "BIZ_EMPTY", 1_800_000_600);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r));
		when(sensitiveFieldEncryptor.decrypt("CIPHER_EMPTY")).thenReturn("");

		assertThat(smsService.scheduleQuerySendDetail()).isZero();

		verify(querySendDetailJobDispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("empty bizId or null created skips publish")
	void schedule_bizid_or_created_blank_skip() {
		Record r1 = newRecord(11L, 11L, "CIPHER_M_A", "", 1_800_000_700);
		Record r2 = newRecord(12L, 12L, "CIPHER_M_B", "BIZ_OK", null);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r1, r2));

		assertThat(smsService.scheduleQuerySendDetail()).isZero();

		verify(sensitiveFieldEncryptor, never()).decrypt(any());
		verify(querySendDetailJobDispatchPublisher, never())
				.publish(anyLong(), anyLong(), anyString(), anyString(), anyInt());
	}

	@Test
	@DisplayName("created epoch is forwarded unchanged to publish for worker sendDate formatting")
	void schedule_forwards_created_epoch_to_publish() {
		Record row = newRecord(41L, 1L, "CIPHER_TZ", "BZ_TZ", 1_735_660_800);
		when(recordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		when(sensitiveFieldEncryptor.decrypt("CIPHER_TZ")).thenReturn("13399990000");

		assertThat(smsService.scheduleQuerySendDetail()).isEqualTo(1);

		verify(querySendDetailJobDispatchPublisher).publish(1L, 41L, "13399990000", "BZ_TZ", 1_735_660_800);
	}
}
