package com.auction.repository;

import com.auction.domain.entity.UserBehavior;
import com.auction.infrastructure.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserBehaviorRepository {

    private final UserBehaviorMapper mapper;

    public UserBehavior save(UserBehavior behavior) {
        if (behavior.getId() == null) {
            mapper.insert(behavior);
        } else {
            mapper.updateById(behavior);
        }
        return behavior;
    }

    public UserBehavior findByUserAndAuction(Long userId, Long auctionId) {
        return mapper.selectById(null);
    }
}
