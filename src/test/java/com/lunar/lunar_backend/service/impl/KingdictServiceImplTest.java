package com.lunar.lunar_backend.service.impl;

import com.lunar.lunar_backend.entity.Kingdict;
import com.lunar.lunar_backend.mapper.KingdictMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KingdictServiceImplTest {

    @Test
    void formatNameWuxingKeepsNameOrderAndAppendsWuxing() {
        KingdictMapper mapper = mock(KingdictMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                entry("王", "水"),
                entry("家", "火"),
                entry("康", "火")
        ));
        KingdictServiceImpl service = new KingdictServiceImpl(mapper);

        String display = service.formatNameWuxing("王家康");

        assertThat(display).isEqualTo("王（水）家（火）康（火）");
    }

    private static Kingdict entry(String zi, String wuxing) {
        Kingdict kingdict = new Kingdict();
        kingdict.setZi(zi);
        kingdict.setWuxing(wuxing);
        return kingdict;
    }
}
