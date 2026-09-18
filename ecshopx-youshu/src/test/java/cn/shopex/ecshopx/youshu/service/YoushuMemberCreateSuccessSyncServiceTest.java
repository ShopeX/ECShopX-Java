package cn.shopex.ecshopx.youshu.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.youshu.integration.YoushuMemberPushPort;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YoushuMemberCreateSuccessSyncServiceTest {

	@Mock
	private YoushuSettingMapper youshuSettingMapper;

	@Mock
	private YoushuDataSourceApiPort youshuDataSourceApiPort;

	@Mock
	private YoushuMemberPushPort youshuMemberPushPort;

	@Mock
	private MembersMapper membersMapper;

	@Mock
	private MembersInfoMapper membersInfoMapper;

	@InjectMocks
	private YoushuMemberCreateSuccessSyncService service;

	@Test
	void syncMemberAfterCreate_whenNoYoushuSetting_skipsDataSourceAndPush() {
		when(youshuSettingMapper.selectOne(any())).thenReturn(null);
		service.syncMemberAfterCreate(1L, 2L);
		verify(youshuDataSourceApiPort, never()).getOrCreateDataSourceId(anyString(), anyInt(), any());
		verify(youshuMemberPushPort, never()).pushMemberRow(anyString(), any(), any());
		verify(membersMapper, never()).selectOne(any());
	}
}
