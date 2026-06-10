package com.auction.api.controller;

import com.auction.api.dto.request.DeleteImageRequest;
import com.auction.api.dto.response.UploadSignatureResponse;
import com.auction.common.Result;
import com.auction.infrastructure.security.CurrentUser;
import com.auction.infrastructure.security.UserPrincipal;
import com.auction.service.storage.OssDirectUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文件上传控制器（OSS直传方案）
 * 使用签名策略实现前端直传OSS，完全不需要后端做中转
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssDirectUploadService ossDirectUploadService;

    /**
     * 获取OSS上传签名
     * POST /upload/signature
     *
     * 前端获取上传签名后，可以直接使用OSS SDK上传文件
     * 优势：
     * - 不需要后端做中转
     * - 减轻服务器压力
     * - 上传速度更快
     * - 支持大文件
     *
     * @param fileName 文件名（可选）
     * @param currentUser 当前登录用户
     * @return 上传签名信息
     */
    @PostMapping("/signature")
    public Result<UploadSignatureResponse> getUploadSignature(
            @RequestParam(required = false) String fileName,
            @CurrentUser UserPrincipal currentUser) {

        log.info("获取上传签名: userId={}, fileName={}", currentUser.getUserId(), fileName);

        try {
            // 生成上传签名
            OssDirectUploadService.UploadSignature signature =
                ossDirectUploadService.generateUploadSignature(currentUser.getUserId(), fileName);

            // 构建响应
            UploadSignatureResponse response = new UploadSignatureResponse();
            response.setAccessKeyId(signature.getAccessKeyId());
            response.setPolicy(signature.getPolicy());
            response.setSignature(signature.getSignature());
            response.setExpiration(signature.getExpiration());
            response.setBucket(signature.getBucket());
            response.setRegion(signature.getRegion());
            response.setEndpoint(signature.getEndpoint());
            response.setKeyPrefix(signature.getKeyPrefix());
            response.setUploadDir(signature.getUploadDir());

            log.info("上传签名生成成功: userId={}", currentUser.getUserId());

            return Result.ok(response);

        } catch (Exception e) {
            log.error("获取上传签名失败: userId={}, error={}",
                     currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "获取上传签名失败: " + e.getMessage());
        }
    }

    /**
     * 删除图片
     * DELETE /upload/image
     *
     * @param request 删除请求
     * @param currentUser 当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/image")
    public Result<Void> deleteImage(
            @RequestBody DeleteImageRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("删除图片: userId={}, imageUrl={}", currentUser.getUserId(), request.getImageUrl());

        try {
            // 删除OSS文件
            ossDirectUploadService.deleteFile(request.getImageUrl());

            log.info("图片删除成功: userId={}, url={}",
                     currentUser.getUserId(), request.getImageUrl());

            return Result.ok();

        } catch (Exception e) {
            log.error("图片删除失败: userId={}, error={}",
                     currentUser.getUserId(), e.getMessage());
            return Result.fail(500, "图片删除失败: " + e.getMessage());
        }
    }
}
