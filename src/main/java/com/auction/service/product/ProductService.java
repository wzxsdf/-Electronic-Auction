package com.auction.service.product;

import com.auction.api.dto.request.CreateProductRequest;
import com.auction.api.dto.response.ProductResponse;
import com.auction.common.BizException;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Product;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setImageUrl(request.getImageUrl());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());

        product = productRepository.save(product);
        return toResponse(product);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return toResponse(product);
    }

    public List<ProductResponse> listAll() {
        return productRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setImageUrl(product.getImageUrl());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
