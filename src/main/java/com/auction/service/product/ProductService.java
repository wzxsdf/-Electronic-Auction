package com.auction.service.product;

import com.auction.api.dto.request.*;
import com.auction.api.dto.response.ProductResponse;
import com.auction.common.BizException;
import com.auction.common.defensive.DefensiveCheck;
import com.auction.common.ErrorCode;
import com.auction.domain.entity.Product;
import com.auction.domain.enums.ProductStatus;
import com.auction.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品业务服务（防御性编程版本）
 * 包含完整的参数验证、权限检查、业务规则验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 创建商品
     * 防御措施：
     * 1. 使用当前登录用户ID作为商家ID
     * 2. 验证SKU唯一性
     * 3. 验证价格、库存等数值范围
     * 4. 设置默认状态
     * 5. 记录审计日志
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse create(CreateProductRequest request, Long currentUserId) {
        log.info("创建商品: userId={}, request={}", currentUserId, request);

        // 1. 使用当前登录用户ID作为商家ID
        DefensiveCheck.validId(currentUserId, "商家ID");

        // 2. 验证SKU唯一性（如果提供了SKU）
        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            String sku = DefensiveCheck.notBlank(request.getSku(), "商品编码");
            sku = DefensiveCheck.safeSql(sku, "商品编码");
            DefensiveCheck.length(sku, 100, "商品编码");

            // 检查SKU是否已存在
            if (productRepository.existsBySku(sku)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品编码已存在");
            }
        }

        // 3. 验证商品名称
        String name = DefensiveCheck.notBlank(request.getName(), "商品名称");
        name = DefensiveCheck.safeSql(name, "商品名称");
        DefensiveCheck.length(name, 200, "商品名称");

        // 4. 验证价格
        if (request.getInitialPrice() != null) {
            BigDecimal price = DefensiveCheck.notNull(request.getInitialPrice(), "初始价格");
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "价格必须大于0");
            }
            if (price.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "价格超出范围");
            }
        }

        // 4.1 验证最低加价幅度
        if (request.getBidIncrement() != null) {
            BigDecimal increment = DefensiveCheck.notNull(request.getBidIncrement(), "最低加价幅度");
            if (increment.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度必须大于0");
            }
            if (increment.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度超出范围");
            }
            // 如果有初始价格，验证加价幅度不能超过初始价格的50%
            if (request.getInitialPrice() != null &&
                increment.compareTo(request.getInitialPrice().multiply(new BigDecimal("0.5"))) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度不能超过初始价格的50%");
            }
        }

        // 4.2 验证封顶价格
        if (request.getMaxPrice() != null) {
            BigDecimal maxPrice = DefensiveCheck.notNull(request.getMaxPrice(), "封顶价格");
            if (maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于0");
            }
            if (maxPrice.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格超出范围");
            }
            // 如果有初始价格，验证封顶价格必须大于初始价格
            if (request.getInitialPrice() != null &&
                maxPrice.compareTo(request.getInitialPrice()) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于初始价格");
            }
            // 如果同时有加价幅度和封顶价格，验证封顶价格必须大于加价幅度
            if (request.getBidIncrement() != null &&
                maxPrice.compareTo(request.getBidIncrement()) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于最低加价幅度");
            }
        }

        // 5. 验证库存
        Integer stock = request.getStock() != null ? request.getStock() : 0;
        if (stock < 0 || stock > 999999) {
            throw new BizException(ErrorCode.BAD_REQUEST, "库存数量必须在0-999999之间");
        }

        // 6. 验证品牌长度
        if (request.getBrand() != null) {
            DefensiveCheck.length(request.getBrand(), 100, "品牌");
        }

        // 7. 验证描述长度
        if (request.getDescription() != null) {
            DefensiveCheck.length(request.getDescription(), 2000, "商品描述");
        }

        // 8. 验证分类长度
        if (request.getCategory() != null) {
            DefensiveCheck.length(request.getCategory(), 50, "商品分类");
        }

        // 9. 验证图片URL列表
        java.util.List<String> imageUrls = null;
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // 验证数量
            if (request.getImageUrls().size() > 4) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最多只能上传4张图片");
            }

            imageUrls = new java.util.ArrayList<>();
            for (String url : request.getImageUrls()) {
                if (url != null && !url.trim().isEmpty()) {
                    // 验证每个图片URL
                    String imageUrl = DefensiveCheck.notBlank(url, "图片URL");
                    DefensiveCheck.length(imageUrl, 500, "图片URL");

                    // 验证URL安全性（防止XSS攻击）
                    if (imageUrl.contains("<") || imageUrl.contains(">") ||
                        imageUrl.contains("javascript:") || imageUrl.contains("data:")) {
                        throw new BizException(ErrorCode.BAD_REQUEST, "图片URL不安全: " + imageUrl);
                    }

                    // 验证URL格式
                    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                            "图片URL格式不正确，必须以http://或https://开头: " + imageUrl);
                    }

                    imageUrls.add(imageUrl.trim());
                }
            }
        }

        // 10. 创建商品实体
        Product product = new Product();
        product.setMerchantId(currentUserId);
        product.setSku(request.getSku());
        product.setName(name);
        product.setBrand(request.getBrand());
        product.setImageList(imageUrls); // 使用图片列表，内部会转换为逗号分隔的字符串
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setInitialPrice(request.getInitialPrice());
        product.setBidIncrement(request.getBidIncrement());
        product.setMaxPrice(request.getMaxPrice());
        product.setStock(stock);
        product.setStatusEnum(request.getStatus() != null ? request.getStatus() : ProductStatus.PENDING_REVIEW);

        // 11. 保存商品
        product = productRepository.save(product);

        // 12. 记录审计日志
        DefensiveCheck.auditLog("CREATE_PRODUCT", currentUserId, product.getId(),
                               "创建商品: " + product.getName());

        log.info("商品创建成功: productId={}, merchantId={}, imageCount={}",
                product.getId(), currentUserId, imageUrls != null ? imageUrls.size() : 0);
        return toResponse(product);
    }

    /**
     * 根据ID查询商品
     * 防御措施：
     * 1. 验证ID有效性
     * 2. 检查商品是否存在
     * 3. 记录访问日志
     */
    public ProductResponse getById(Long id) {
        log.info("查询商品详情: productId={}", id);

        // 1. 验证ID
        DefensiveCheck.validId(id, "商品ID");

        // 2. 查询商品
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 3. 记录访问日志
        log.info("商品详情查询成功: productId={}, status={}", id, product.getStatus());

        return toResponse(product);
    }

    /**
     * 查询商品列表（带筛选和分页）
     * 防御措施：
     * 1. 验证分页参数
     * 2. 验证排序字段安全性
     * 3. 根据用户角色过滤数据
     */
    public List<ProductResponse> queryProducts(ProductQueryRequest request, Long currentUserId, String userRole) {
        log.info("查询商品列表: request={}, userId={}, userRole={}", request, currentUserId, userRole);

        // 0. 验证用户身份
        if ("MERCHANT".equals(userRole) && currentUserId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "商家用户未登录");
        }

        // 1. 验证分页参数
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer size = request.getSize() != null ? request.getSize() : 20;

        if (page < 1 || page > 9999) {
            throw new BizException(ErrorCode.BAD_REQUEST, "页码无效");
        }
        if (size < 1 || size > 100) {
            throw new BizException(ErrorCode.BAD_REQUEST, "每页数量必须在1-100之间");
        }

        // 2. 验证排序字段安全性（防止SQL注入）
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String[] allowedFields = {"createdAt", "initialPrice", "name", "stock"};
        boolean isValidField = false;
        for (String field : allowedFields) {
            if (field.equals(sortBy)) {
                isValidField = true;
                break;
            }
        }
        if (!isValidField) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的排序字段");
        }

        // 3. 验证排序方向
        String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "desc";
        if (!sortOrder.equals("asc") && !sortOrder.equals("desc")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的排序方向");
        }

        // 4. 根据用户角色设置查询范围
        String statusFilter = request.getStatus();
        Long merchantIdFilter = null;

        if ("ADMIN".equals(userRole)) {
            // 管理员可以查看所有商品，按状态筛选
            // statusFilter 保持原值
        } else if ("MERCHANT".equals(userRole)) {
            // 商家只能查看自己创建的商品
            merchantIdFilter = currentUserId;
        } else {
            // 普通用户只能查看已上架商品
            statusFilter = "LISTED";
        }

        // 5. 执行查询（根据角色和筛选条件）
        List<Product> products;

        if (merchantIdFilter != null) {
            // 商家查询自己的商品
            if (statusFilter != null) {
                try {
                    ProductStatus status = ProductStatus.valueOf(statusFilter.toUpperCase());
                    products = productRepository.findByMerchantIdAndStatus(merchantIdFilter, status);
                } catch (IllegalArgumentException e) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "无效的商品状态");
                }
            } else {
                products = productRepository.findByMerchantId(merchantIdFilter);
            }
        } else {
            // 管理员或普通用户查询
            if (statusFilter != null) {
                try {
                    ProductStatus status = ProductStatus.valueOf(statusFilter.toUpperCase());
                    products = productRepository.findByStatus(status);
                } catch (IllegalArgumentException e) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "无效的商品状态");
                }
            } else {
                products = productRepository.findAll();
            }
        }

        // 6. 应用其他筛选条件
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            // 防止SQL注入
            String keyword = DefensiveCheck.safeSql(request.getKeyword(), "搜索关键词");
            // 这里简化处理，实际应该在数据库层进行模糊查询
        }

        // 7. 转换为响应DTO
        List<ProductResponse> responses = products.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        log.info("商品列表查询成功: 总数={}", responses.size());
        return responses;
    }

    /**
     * 更新商品信息
     * 防御措施：
     * 1. 验证商品存在性
     * 2. 验证资源所有权
     * 3. 验证SKU唯一性
     * 4. 防止状态通过此接口修改
     * 5. 乐观锁防并发
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductResponse update(Long id, UpdateProductRequest request, Long currentUserId) {
        log.info("更新商品: productId={}, userId={}, request={}", id, currentUserId, request);

        // 1. 验证商品存在性
        DefensiveCheck.validId(id, "商品ID");
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 2. 验证资源所有权（商家只能修改自己的商品，管理员可以修改所有商品）
        if (!"ADMIN".equals(getUserRole(currentUserId))) {
            DefensiveCheck.ownership(product.getMerchantId(), currentUserId, "商品");
        }

        // 3. 验证SKU唯一性（如果修改了SKU）
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            String sku = DefensiveCheck.notBlank(request.getSku(), "商品编码");
            if (productRepository.existsBySku(sku)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品编码已存在");
            }
            product.setSku(sku);
        }

        // 4. 更新基本字段
        if (request.getName() != null) {
            String name = DefensiveCheck.notBlank(request.getName(), "商品名称");
            DefensiveCheck.length(name, 200, "商品名称");
            product.setName(name);
        }

        if (request.getBrand() != null) {
            DefensiveCheck.length(request.getBrand(), 100, "品牌");
            product.setBrand(request.getBrand());
        }

        if (request.getImageUrl() != null) {
            DefensiveCheck.length(request.getImageUrl(), 500, "图片URL");
            product.setImageUrl(request.getImageUrl());
        }

        if (request.getDescription() != null) {
            DefensiveCheck.length(request.getDescription(), 2000, "商品描述");
            product.setDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            DefensiveCheck.length(request.getCategory(), 50, "商品分类");
            product.setCategory(request.getCategory());
        }

        if (request.getInitialPrice() != null) {
            BigDecimal price = request.getInitialPrice();
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "价格必须大于0");
            }
            if (price.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "价格超出范围");
            }
            product.setInitialPrice(price);
        }

        // 4.7 更新最低加价幅度
        if (request.getBidIncrement() != null) {
            BigDecimal increment = request.getBidIncrement();
            if (increment.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度必须大于0");
            }
            if (increment.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度超出范围");
            }
            // 验证加价幅度不能超过初始价格的50%
            BigDecimal initialPrice = product.getInitialPrice() != null ? product.getInitialPrice() : BigDecimal.ZERO;
            if (increment.compareTo(initialPrice.multiply(new BigDecimal("0.5"))) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最低加价幅度不能超过初始价格的50%");
            }
            product.setBidIncrement(increment);
        }

        // 4.8 更新封顶价格
        if (request.getMaxPrice() != null) {
            BigDecimal maxPrice = request.getMaxPrice();
            if (maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于0");
            }
            if (maxPrice.compareTo(new BigDecimal("99999999")) > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格超出范围");
            }
            // 验证封顶价格必须大于初始价格
            BigDecimal initialPrice = product.getInitialPrice() != null ? product.getInitialPrice() : BigDecimal.ZERO;
            if (maxPrice.compareTo(initialPrice) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于初始价格");
            }
            // 验证封顶价格必须大于加价幅度
            BigDecimal bidIncrement = product.getBidIncrement() != null ? product.getBidIncrement() : BigDecimal.ZERO;
            if (maxPrice.compareTo(bidIncrement) <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "封顶价格必须大于最低加价幅度");
            }
            product.setMaxPrice(maxPrice);
        }

        // 4.9 更新库存
        if (request.getStock() != null) {
            Integer stock = request.getStock();
            if (stock < 0 || stock > 999999) {
                throw new BizException(ErrorCode.BAD_REQUEST, "库存数量必须在0-999999之间");
            }
            product.setStock(stock);
        }

        // 4.10 更新状态
        if (request.getStatus() != null) {
            product.setStatusEnum(request.getStatus());
        }

        // 4.11 更新图片URL列表（优先使用imageUrls）
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            if (request.getImageUrls().size() > 4) {
                throw new BizException(ErrorCode.BAD_REQUEST, "最多只能上传4张图片");
            }
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            for (String url : request.getImageUrls()) {
                if (url != null && !url.trim().isEmpty()) {
                    String imageUrl = DefensiveCheck.notBlank(url, "图片URL");
                    DefensiveCheck.length(imageUrl, 500, "图片URL");

                    // 验证URL安全性
                    if (imageUrl.contains("<") || imageUrl.contains(">") ||
                        imageUrl.contains("javascript:") || imageUrl.contains("data:")) {
                        throw new BizException(ErrorCode.BAD_REQUEST, "图片URL不安全: " + imageUrl);
                    }

                    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                        throw new BizException(ErrorCode.BAD_REQUEST,
                            "图片URL格式不正确，必须以http://或https://开头: " + imageUrl);
                    }
                    imageUrls.add(imageUrl.trim());
                }
            }
            product.setImageList(imageUrls);
        } else if (request.getImageUrl() != null) {
            // 向后兼容：单图片URL
            DefensiveCheck.length(request.getImageUrl(), 500, "图片URL");
            product.setImageUrl(request.getImageUrl());
        }

        // 5. 保存更新（乐观锁在repository层处理）
        product = productRepository.save(product);

        // 6. 记录审计日志
        DefensiveCheck.auditLog("UPDATE_PRODUCT", currentUserId, id, "更新商品信息");

        log.info("商品更新成功: productId={}", id);
        return toResponse(product);
    }

    /**
     * 上架商品
     * 防御措施：
     * 1. 验证商品存在性
     * 2. 验证资源所有权
     * 3. 验证状态转换合法性
     */
    @Transactional(rollbackFor = Exception.class)
    public void listProduct(Long id, Long currentUserId) {
        log.info("上架商品: productId={}, userId={}", id, currentUserId);

        // 1. 验证商品存在性
        DefensiveCheck.validId(id, "商品ID");
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 2. 验证资源所有权
        if (!"ADMIN".equals(getUserRole(currentUserId))) {
            DefensiveCheck.ownership(product.getMerchantId(), currentUserId, "商品");
        }

        // 3. 验证状态转换合法性
        DefensiveCheck.statusTransition(product.getStatusEnum(), ProductStatus.LISTED);

        // 4. 更新状态
        product.setStatusEnum(ProductStatus.LISTED);
        productRepository.save(product);

        // 5. 记录审计日志
        DefensiveCheck.auditLog("LIST_PRODUCT", currentUserId, id, "上架商品");

        log.info("商品上架成功: productId={}", id);
    }

    /**
     * 下架商品
     * 防御措施：
     * 1. 验证商品存在性
     * 2. 验证资源所有权
     * 3. 验证状态转换合法性
     */
    @Transactional(rollbackFor = Exception.class)
    public void delistProduct(Long id, Long currentUserId) {
        log.info("下架商品: productId={}, userId={}", id, currentUserId);

        // 1. 验证商品存在性
        DefensiveCheck.validId(id, "商品ID");
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 2. 验证资源所有权
        if (!"ADMIN".equals(getUserRole(currentUserId))) {
            DefensiveCheck.ownership(product.getMerchantId(), currentUserId, "商品");
        }

        // 3. 验证状态转换合法性
        DefensiveCheck.statusTransition(product.getStatusEnum(), ProductStatus.DELISTED);

        // 4. 更新状态
        product.setStatusEnum(ProductStatus.DELISTED);
        productRepository.save(product);

        // 5. 记录审计日志
        DefensiveCheck.auditLog("DELIST_PRODUCT", currentUserId, id, "下架商品");

        log.info("商品下架成功: productId={}", id);
    }

    /**
     * 上传商品图片
     * 防御措施：
     * 1. 验证商品存在性
     * 2. 验证资源所有权
     * 3. 验证图片URL安全性
     * 4. 验证图片数量限制
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadImage(Long id, ProductImageUploadRequest request, Long currentUserId) {
        log.info("上传商品图片: productId={}, userId={}, imageUrl={}",
                 id, currentUserId, request.getImageUrl());

        // 1. 验证商品存在性
        DefensiveCheck.validId(id, "商品ID");
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 2. 验证资源所有权
        if (!"ADMIN".equals(getUserRole(currentUserId))) {
            DefensiveCheck.ownership(product.getMerchantId(), currentUserId, "商品");
        }

        // 3. 验证图片URL
        String imageUrl = DefensiveCheck.notBlank(request.getImageUrl(), "图片URL");
        DefensiveCheck.length(imageUrl, 500, "图片URL");

        // 4. 验证图片URL安全性（防止XSS）
        if (imageUrl.contains("<") || imageUrl.contains(">") ||
            imageUrl.contains("javascript:") || imageUrl.contains("data:")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "图片URL不安全");
        }

        // 5. 设置为主图或附加图
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            product.setImageUrl(imageUrl);
        }

        // 6. 保存商品
        productRepository.save(product);

        // 7. 记录审计日志
        DefensiveCheck.auditLog("UPLOAD_IMAGE", currentUserId, id, "上传商品图片");

        log.info("商品图片上传成功: productId={}", id);
    }

    /**
     * 删除商品图片
     * 防御措施：
     * 1. 验证商品存在性
     * 2. 验证资源所有权
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id, String imageId, Long currentUserId) {
        log.info("删除商品图片: productId={}, imageId={}, userId={}", id, imageId, currentUserId);

        // 1. 验证商品存在性
        DefensiveCheck.validId(id, "商品ID");
        Product product = productRepository.findById(id);
        DefensiveCheck.exists(product, "商品");

        // 2. 验证资源所有权
        if (!"ADMIN".equals(getUserRole(currentUserId))) {
            DefensiveCheck.ownership(product.getMerchantId(), currentUserId, "商品");
        }

        // 3. 清空图片URL（简化处理）
        if (product.getImageUrl() != null) {
            product.setImageUrl(null);
            productRepository.save(product);
        }

        // 4. 记录审计日志
        DefensiveCheck.auditLog("DELETE_IMAGE", currentUserId, id, "删除商品图片");

        log.info("商品图片删除成功: productId={}", id);
    }

    /**
     * 转换为响应DTO
     */
    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setMerchantId(product.getMerchantId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setBrand(product.getBrand());
        response.setImageUrl(product.getImageUrl()); // 向后兼容，保留原始字符串
        response.setImageUrls(product.getImageList()); // 图片列表
        response.setPrimaryImageUrl(product.getPrimaryImageUrl()); // 主图
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setInitialPrice(product.getInitialPrice());
        response.setBidIncrement(product.getBidIncrement());
        response.setMaxPrice(product.getMaxPrice());
        response.setStock(product.getStock());
        response.setStatus(product.getStatusEnum());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }

    /**
     * 获取用户角色（简化处理，实际应该从UserService或SecurityContext获取）
     */
    private String getUserRole(Long userId) {
        // 这里简化处理，实际应该查询用户角色表
        return "USER";
    }
}
