package cn.shopex.ecshopx.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.OpenScreenAdSaveRequest;
import cn.shopex.ecshopx.theme.domain.OpenScreenAd;
import cn.shopex.ecshopx.theme.mapper.OpenScreenAdMapper;
import cn.shopex.ecshopx.theme.support.OpenScreenAdColumnNamesDataMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenScreenAdSaveServiceDisplayWindowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OpenScreenAd.class);
	}

	@Mock
	private OpenScreenAdMapper openScreenAdMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OpenScreenAdColumnNamesDataMapper columnMapper = new OpenScreenAdColumnNamesDataMapper();

	@Test
	void saveWithoutStartEnd_appliesDefaultEpochWindow() {
		when(openScreenAdMapper.selectOne(any())).thenReturn(null);
		ArgumentCaptor<OpenScreenAd> captor = ArgumentCaptor.forClass(OpenScreenAd.class);
		when(openScreenAdMapper.insert(captor.capture())).thenAnswer(inv -> {
			OpenScreenAd e = inv.getArgument(0);
			e.setId(1L);
			return 1;
		});

		OpenScreenAdSaveService service =
				new OpenScreenAdSaveService(openScreenAdMapper, objectMapper, columnMapper);
		OpenScreenAdSaveRequest req = admPayloadLikeProd(false);
		req.setIsEnable("1");

		var out = service.save(38L, req);
		assertThat(out.get("status")).isEqualTo(Boolean.TRUE);

		OpenScreenAd saved = captor.getValue();
		assertThat(saved.getStartTime()).isEqualTo(0L);
		assertThat(saved.getEndTime()).isEqualTo(2_147_483_647L);
		assertThat(saved.getIsEnable()).isEqualTo(1);
	}

	@Test
	void saveWithInvalidStartTime_stillRejects() {
		OpenScreenAdSaveService service =
				new OpenScreenAdSaveService(openScreenAdMapper, objectMapper, columnMapper);
		OpenScreenAdSaveRequest req = admPayloadLikeProd(true);
		req.setStartTime("not-a-number");

		assertThatThrownBy(() -> service.save(38L, req))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("请选择展示开始时间");
	}

	private static OpenScreenAdSaveRequest admPayloadLikeProd(boolean includeTimes) {
		OpenScreenAdSaveRequest req = new OpenScreenAdSaveRequest();
		req.setAdMaterial(
				"https://shopex-onex-yundian-image.oss-cn-shanghai.aliyuncs.com/demo-ecshopx/image/38/2025/08/08/x.png");
		req.setMaterialType("1");
		req.setIsEnable("0");
		req.setShowTime("first");
		req.setPosition("right_top");
		req.setIsJump("1");
		req.setWaitingTime("5");
		req.setApp("wapp");
		req.setAdUrl(
				JsonNodeFactory.instance
						.objectNode()
						.put("id", "153")
						.put("title", "挂件-积分商品")
						.put("linkPage", "custom_page"));
		if (includeTimes) {
			req.setStartTime("1730000000");
			req.setEndTime("1731000000");
		}
		return req;
	}
}
