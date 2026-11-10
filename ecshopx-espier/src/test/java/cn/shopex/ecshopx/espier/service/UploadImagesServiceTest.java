package cn.shopex.ecshopx.espier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.espier.domain.UploadImages;
import cn.shopex.ecshopx.espier.mapper.UploadImagesCatMapper;
import cn.shopex.ecshopx.espier.mapper.UploadImagesMapper;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadImagesServiceTest {

	@BeforeAll
	static void registerMybatisPlusEntityMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
		TableInfoHelper.initTableInfo(assistant, UploadImages.class);
	}

	@Mock
	private UploadImagesMapper uploadImagesMapper;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private StorageProperties storageProperties;

	@Mock
	private UploadImagesCatMapper uploadImagesCatMapper;

	@Mock
	private CommonLangModReadService commonLangModReadService;

	@Mock
	private CommonLangModWriteService commonLangModWriteService;

	@Mock
	private LangueProperties langueProperties;

	private UploadImagesService service;

	@BeforeEach
	void setUp() {
		service = new UploadImagesService(
				uploadImagesMapper,
				fileStorageService,
				storageProperties,
				uploadImagesCatMapper,
				commonLangModReadService,
				commonLangModWriteService,
				langueProperties);
	}

	@Test
	void getImageList_appliesStableOrderByCreatedThenImageId() {
		when(uploadImagesMapper.selectCount(any())).thenReturn(1L);
		UploadImages entity = new UploadImages();
		entity.setImageId(99L);
		entity.setImageUrl("path/key.jpg");
		when(uploadImagesMapper.selectList(any())).thenReturn(List.of(entity));
		when(fileStorageService.url(any(), any())).thenReturn("https://example.test/key.jpg");

		Map<String, Object> query = new HashMap<>();
		query.put("image_cat_id", "10");

		service.getImageList(null, 1L, "staff", null, 0L, query);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaQueryWrapper<UploadImages>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(uploadImagesMapper).selectList(captor.capture());
		String sql = captor.getValue().getSqlSegment();
		assertThat(sql).containsIgnoringCase("ORDER BY");
		assertThat(sql).containsIgnoringCase("created");
		assertThat(sql).containsIgnoringCase("image_id");
		assertThat(sql.indexOf("created")).isLessThan(sql.toLowerCase().lastIndexOf("image_id"));
	}

	@Test
	void getImageList_returnsEmptyListWhenTotalIsZero() {
		when(uploadImagesMapper.selectCount(any())).thenReturn(0L);

		Map<String, Object> data = service.getImageList(null, 1L, "staff", null, 0L, Collections.emptyMap());

		assertThat(data.get("total_count")).isEqualTo(0);
		assertThat(data.get("list")).isEqualTo(Collections.emptyList());
	}
}
