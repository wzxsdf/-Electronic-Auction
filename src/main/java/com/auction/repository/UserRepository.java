package com.auction.repository;

import com.auction.domain.entity.User;
import com.auction.domain.enums.UserStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.auction.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户Repository
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    /**
     * 根据ID查询用户
     */
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 保存或更新用户
     */
    public User save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    /**
     * 更新用户
     */
    public User updateById(User user) {
        userMapper.updateById(user);
        return user;
    }

    /**
     * 根据用户名查询用户
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    /**
     * 根据邮箱查询用户
     */
    public User findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
    }

    /**
     * 根据手机号查询用户
     */
    public User findByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
    }

    /**
     * 查询所有用户
     */
    public List<User> findAll() {
        return userMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 根据状态查询用户列表
     */
    public List<User> findByStatus(UserStatus status) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status.name());
        return userMapper.selectList(queryWrapper);
    }

    /**
     * 分页查询用户
     */
    public Page<User> findPage(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        return userMapper.selectPage(page, new LambdaQueryWrapper<>());
    }

    /**
     * 根据状态分页查询用户
     */
    public Page<User> findPageByStatus(UserStatus status, int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status.name());
        return userMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据用户名或邮箱或手机号搜索用户
     */
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                )
        );
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        ) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        ) > 0;
    }

    /**
     * 检查手机号是否存在
     */
    public boolean existsByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        ) > 0;
    }

    /**
     * 删除用户
     */
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    /**
     * 统计用户总数
     */
    public long count() {
        return userMapper.selectCount(new LambdaQueryWrapper<>());
    }

    /**
     * 根据状态统计用户数
     */
    public long countByStatus(UserStatus status) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status.name());
        return userMapper.selectCount(queryWrapper);
    }
}
