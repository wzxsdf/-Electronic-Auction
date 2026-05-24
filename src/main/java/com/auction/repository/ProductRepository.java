package com.auction.repository;

import com.auction.domain.entity.Product;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.auction.infrastructure.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepository {

    private final ProductMapper productMapper;

    public Product findById(Long id) {
        return productMapper.selectById(id);
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product;
    }

    public List<Product> findAll() {
        return productMapper.selectList(null);
    }

    public boolean existsById(Long id) {
        return productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getId, id)
        ) > 0;
    }
}
