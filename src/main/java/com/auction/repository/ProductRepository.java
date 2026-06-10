package com.auction.repository;

import com.auction.domain.entity.Product;
import com.auction.domain.enums.ProductStatus;
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

    public List<Product> findByStatus(ProductStatus status) {
        return productMapper.selectList(
            new LambdaQueryWrapper<Product>().eq(Product::getStatus, status)
        );
    }

    public List<Product> findByMerchantId(Long merchantId) {
        return productMapper.selectList(
            new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId)
        );
    }

    public List<Product> findByMerchantIdAndStatus(Long merchantId, ProductStatus status) {
        return productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .eq(Product::getStatus, status)
        );
    }

    public boolean existsById(Long id) {
        return productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getId, id)
        ) > 0;
    }

    public boolean existsBySku(String sku) {
        return productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getSku, sku)
        ) > 0;
    }
}
