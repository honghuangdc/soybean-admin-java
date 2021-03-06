package com.soybean.uaa.controller.baseinfo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import com.google.common.collect.Maps;
import com.soybean.framework.commons.annotation.log.SysLog;
import com.soybean.framework.commons.entity.Result;
import com.soybean.framework.commons.util.BeanUtilPlus;
import com.soybean.framework.db.mybatis.conditions.Wraps;
import com.soybean.uaa.domain.dto.OrgSaveDTO;
import com.soybean.uaa.domain.entity.baseinfo.Org;
import com.soybean.uaa.service.OrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 组织架构
 *
 * @author wenxina
 */
@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * 查询系统所有的组织树
     *
     * @param name   名字
     * @param status 状态
     * @return {@link Result}<{@link List}<{@link Tree}<{@link Long}>>>
     */
    @GetMapping("/trees")
    public Result<List<Tree<Long>>> tree(String name, Boolean status) {
        List<Org> list = this.orgService.list(Wraps.<Org>lbQ().like(Org::getLabel, name).eq(Org::getStatus, status).orderByAsc(Org::getSequence));
        final List<TreeNode<Long>> nodes = list.stream().map(org -> {
            TreeNode<Long> treeNode = new TreeNode<>(org.getId(), org.getParentId(), org.getLabel(), org.getSequence());
            Map<String, Object> extra = Maps.newLinkedHashMap();
            extra.put("key", org.getId());
            extra.put("label", org.getLabel());
            extra.put("alias", org.getAlias());
            extra.put("status", org.getStatus());
            extra.put("sequence", org.getSequence());
            extra.put("tel", org.getTel());
            extra.put("description", org.getDescription());
            treeNode.setExtra(extra);
            return treeNode;
        }).collect(Collectors.toList());
        return Result.success(TreeUtil.build(nodes, 0L));
    }

    /**
     * 保存组织架构
     *
     * @param dto dto
     */
    @PostMapping
    @SysLog(value = "保存组织架构")
    public void save(@Validated @RequestBody OrgSaveDTO dto) {
        orgService.addOrg(BeanUtil.toBean(dto, Org.class));
    }

    /**
     * 编辑组织架构
     *
     * @param id  id
     * @param dto dto
     */
    @PutMapping("/{id}")
    @SysLog(value = "编辑组织架构")
    public void edit(@PathVariable Long id, @Validated @RequestBody OrgSaveDTO dto) {
        orgService.updateById(BeanUtilPlus.toBean(id, dto, Org.class));
    }

    /**
     * 删除组织架构
     *
     * @param id id
     */
    @DeleteMapping("/{id}")
    @SysLog(value = "删除组织架构")
    public void del(@PathVariable Long id) {
        orgService.remove(id);
    }

}
