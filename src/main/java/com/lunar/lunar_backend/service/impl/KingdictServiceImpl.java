package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lunar.lunar_backend.entity.Kingdict;
import com.lunar.lunar_backend.mapper.KingdictMapper;
import com.lunar.lunar_backend.service.KingdictService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KingdictServiceImpl implements KingdictService {

    private final KingdictMapper kingdictMapper;

    public KingdictServiceImpl(KingdictMapper kingdictMapper) {
        this.kingdictMapper = kingdictMapper;
    }

    @Override
    public String formatNameWuxing(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        List<String> chars = name.trim().codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();
        List<String> uniqueChars = chars.stream().distinct().toList();
        List<Kingdict> entries = kingdictMapper.selectList(new LambdaQueryWrapper<Kingdict>()
                .in(Kingdict::getZi, uniqueChars));
        Map<String, String> wuxingByZi = entries.stream()
                .filter(entry -> StringUtils.hasText(entry.getZi()))
                .collect(Collectors.toMap(Kingdict::getZi, Kingdict::getWuxing, (first, second) -> first));

        return chars.stream()
                .map(zi -> formatZi(zi, wuxingByZi))
                .collect(Collectors.joining());
    }

    private String formatZi(String zi, Map<String, String> wuxingByZi) {
        String wuxing = wuxingByZi.get(zi);
        if (!StringUtils.hasText(wuxing)) {
            return zi;
        }
        return zi + "（" + wuxing.trim() + "）";
    }
}
