package com.auction.infrastructure.mapper;

import com.auction.domain.entity.UserRole;
import com.auction.domain.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户角色关联Mapper接口
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户ID查询角色列表（通过关联查询）
     */
    @Select("SELECT r.* FROM roles r " +
            "INNER JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "ORDER BY r.id")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询用户列表（通过关联查询）
     */
    @Select("SELECT u.* FROM users u " +
            "INNER JOIN user_roles ur ON u.id = ur.user_id " +
            "WHERE ur.role_id = #{roleId} " +
            "ORDER BY u.id")
    List<com.auction.domain.entity.User> selectUsersByRoleId(@Param("roleId") Long roleId);
}
