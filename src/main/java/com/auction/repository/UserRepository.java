package com.auction.repository;

import com.auction.domain.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public User save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    public User updateById(User user) {
        userMapper.updateById(user);
        return user;
    }

    public User findByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }
}
