package com.soybean.uaa.service.impl;


import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.soybean.framework.commons.exception.CheckedException;
import com.soybean.framework.db.mybatis.SuperServiceImpl;
import com.soybean.framework.db.mybatis.conditions.Wraps;
import com.soybean.uaa.domain.entity.common.Dictionary;
import com.soybean.uaa.domain.entity.common.DictionaryItem;
import com.soybean.uaa.repository.DictionaryItemMapper;
import com.soybean.uaa.repository.DictionaryMapper;
import com.soybean.uaa.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * <p>
 * 业务实现类
 * 字典类型
 * </p>
 *
 * @author wenxian
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl extends SuperServiceImpl<DictionaryMapper, Dictionary> implements DictionaryService {

    private final DictionaryItemMapper dictionaryItemMapper;

    @Override
    public void addDictionary(Dictionary dictionary) {
        if (dictionary == null) {
            throw CheckedException.notFound("字典内容不能为空");
        }
        final Long count = this.baseMapper.selectCount(Wraps.<Dictionary>lbQ().eq(Dictionary::getCode, dictionary.getCode()));
        if (count != 0 && count > 0) {
            throw CheckedException.badRequest("字典类型编码重复");
        }
        this.baseMapper.insert(dictionary);
    }

    @DSTransactional
    @Override
    public void deleteById(Long id) {
        final Dictionary dictionary = Optional.ofNullable(this.baseMapper.selectById(id))
                .orElseThrow(() -> CheckedException.notFound("字典不存在"));
        if (dictionary.getReadonly()) {
            throw CheckedException.notFound("内置数据无法删除");
        }
        this.baseMapper.deleteById(id);
        this.dictionaryItemMapper.delete(Wraps.<DictionaryItem>lbQ().eq(DictionaryItem::getDictionaryId, id));
    }

    @DSTransactional
    @Override
    public void editDictionary(Dictionary dictionary) {
        final Dictionary record = Optional.ofNullable(this.baseMapper.selectById(dictionary.getId()))
                .orElseThrow(() -> CheckedException.notFound("字典不存在"));
        if (record.getReadonly()) {
            throw CheckedException.notFound("内置数据无法删除");
        }
        final Long count = this.baseMapper.selectCount(Wraps.<Dictionary>lbQ().ne(Dictionary::getId, dictionary.getId())
                .eq(Dictionary::getCode, dictionary.getCode()));
        if (count != 0 && count > 0) {
            throw CheckedException.badRequest("字典类型编码重复");
        }
        this.baseMapper.updateById(dictionary);
        this.dictionaryItemMapper.update(DictionaryItem.builder()
                .status(dictionary.getStatus())
                .dictionaryCode(dictionary.getCode())
                .build(), Wraps.<DictionaryItem>lbQ()
                .eq(DictionaryItem::getDictionaryId, dictionary.getId()));
    }


}
