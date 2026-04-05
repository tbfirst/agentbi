package com.tbfirst.agentbiinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tbfirst.agentbiinit.model.entity.ChartEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 针对表【chart(智能 bi 图表信息表)】的数据库操作Mapper
 * 
 * 继承BaseMapper后自动拥有以下方法：
 * - insert(entity): 插入一条记录
 * - deleteById(id): 根据ID删除
 * - updateById(entity): 根据ID更新
 * - selectById(id): 根据ID查询
 * - selectList(wrapper): 条件查询列表
 * - selectPage(page, wrapper): 分页查询
 */
@Mapper
public interface ChartMapper extends BaseMapper<ChartEntity> {

    /**
     * 根据指纹查询图表记录
     * 用于缓存命中时快速获取历史分析结果
     * 
     * @param fingerprint 文件指纹
     * @return 图表实体
     */
    @Select("SELECT * FROM chart WHERE fingerprint = #{fingerprint} AND is_delete = 0 LIMIT 1")
    ChartEntity selectByFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * 根据用户ID查询该用户的所有指纹列表
     * 用于用户级别的缓存预热
     * 
     * @param userId 用户ID
     * @return 指纹列表
     */
    @Select("SELECT DISTINCT fingerprint FROM chart WHERE userId = #{userId} AND is_delete = 0 AND fingerprint IS NOT NULL")
    List<String> selectFingerprintsByUserId(@Param("userId") Long userId);

    /**
     * 统计指定时间范围内有指纹的图表数量
     * 用于监控和统计
     * 
     * @param days 最近天数
     * @return 图表数量
     */
    @Select("SELECT COUNT(*) FROM chart WHERE createTime >= DATE_SUB(NOW(), INTERVAL #{days} DAY) AND is_delete = 0 AND fingerprint IS NOT NULL")
    long countByDaysWithFingerprint(@Param("days") int days);
}




