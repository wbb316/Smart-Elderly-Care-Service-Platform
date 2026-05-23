package com.lcyl.system.mapper;

import java.util.List;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.domain.dto.AddInfo;
import com.lcyl.system.domain.vo.BedVO;
import org.apache.ibatis.annotations.Param;

/**
 * 老人Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public interface ElderMapper
{
    /**
     * 查询老人
     *
     * @param id 老人主键
     * @return 老人
     */
    public Elder selectElderById(Long id);

    /**
     * 查询老人列表
     *
     * @param elder 老人
     * @return 老人集合
     */
    public List<Elder> selectElderList(Elder elder);

    /**
     * 查询所有老人列表（不分页）
     */
    public List<Elder> selectAllElderList();

    /**
     * 新增老人
     *
     * @param elder 老人
     * @return 结果
     */
    public int insertElder(Elder elder);

    /**
     * 修改老人
     *
     * @param elder 老人
     * @return 结果
     */
    public int updateElder(Elder elder);

    /**
     * 更新老人床位ID（按姓名匹配）
     */
    public int updateBedId(@Param("bedId") Long bedId, @Param("name") String name);

    /**
     * 更新老人状态
     */
    public int updateStatus(@Param("elderId") Long elderId, @Param("status") Integer status);

    /**
     * 根据家属会员ID查询关联的老人
     */
    public List<Elder> selectElderByMember(@Param("memberId") Long memberId);

    /**
     * 新增老人-家属关联
     */
    public int insertInfo(@Param("addInfo") AddInfo addInfo, @Param("memberId") Long memberId);

    /**
     * 查询老人床位列表（家属端）
     */
    public List<BedVO> selectElderBedList(@Param("memberId") Long memberId);

    /**
     * 删除老人-家属关联
     */
    public int delElderInfoById(@Param("id") Long id);

    /**
     * 删除老人
     *
     * @param id 老人主键
     * @return 结果
     */
    public int deleteElderById(Long id);

    /**
     * 批量删除老人
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteElderByIds(Long[] ids);
}
