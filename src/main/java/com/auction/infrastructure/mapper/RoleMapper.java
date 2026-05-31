package com.auction.infrastructure.mapper;

import com.auction.domain.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM roles r " +
            "INNER JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "ORDER BY r.id")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据编码查询角色
     */
    @Select("SELECT * FROM roles WHERE code = #{code}")
    Role selectByCode(@Param("code") String code);
}
