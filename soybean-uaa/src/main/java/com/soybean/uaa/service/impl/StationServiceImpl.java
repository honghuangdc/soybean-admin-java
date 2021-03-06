package com.soybean.uaa.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.soybean.framework.db.mybatis.SuperServiceImpl;
import com.soybean.framework.db.mybatis.auth.permission.prop.DataScope;
import com.soybean.framework.db.mybatis.auth.permission.prop.DataScopeType;
import com.soybean.framework.db.mybatis.conditions.Wraps;
import com.soybean.framework.db.mybatis.conditions.query.LbqWrapper;
import com.soybean.framework.db.page.PageRequest;
import com.soybean.uaa.domain.dto.StationPageDTO;
import com.soybean.uaa.domain.entity.baseinfo.Station;
import com.soybean.uaa.repository.StationMapper;
import com.soybean.uaa.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 业务实现类
 * 岗位
 * </p>
 *
 * @author wenxina
 * @since 2022-07-01
 */
@Slf4j
@Service
public class StationServiceImpl extends SuperServiceImpl<StationMapper, Station> implements StationService {


    @Override
    public IPage<Station> findStationPage(PageRequest params, StationPageDTO data) {
        Station station = BeanUtil.toBean(data, Station.class);
        final LbqWrapper<Station> wrapper = Wraps.<Station>lbQ().like(Station::getName, station.getName())
                .like(Station::getDescription, station.getDescription()).eq(Station::getType, data.getType())
                .eq(Station::getStatus, data.getStatus()).eq(Station::getOrgId, station.getOrgId())
                .eq(Station::getStatus, station.getStatus()).orderByAsc(Station::getSequence);
        return baseMapper.findStationPage(params.buildPage(), wrapper, DataScope.builder().scopeType(DataScopeType.ALL).build());
    }
}
