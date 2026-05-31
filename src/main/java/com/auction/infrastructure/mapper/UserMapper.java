package com.auction.infrastructure.mapper;

import com.auction.domain.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在（排除指定用户）
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND id != #{userId}")
    boolean existsByEmailExclude(@Param("email") String email, @Param("userId") Long userId);

    /**
     * 检查手机号是否存在（排除指定用户）
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE phone = #{phone} AND id != #{userId}")
    boolean existsByPhoneExclude(@Param("phone") String phone, @Param("userId") Long userId);
}
