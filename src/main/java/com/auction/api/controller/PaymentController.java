package com.auction.api.controller;

import com.auction.common.Result;
import com.auction.service.payment.PaymentService;
import com.auction.service.payment.PaymentService.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 支付控制器：处理订单支付、查询支付状态和取消支付操作，支持同步/异步支付
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 异步支付：模拟真实支付场景，包含延迟处理和随机失败率，支持并发支付
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 支付结果
     */
    @PostMapping("/pay/async")
    public CompletableFuture<Result<PaymentResult>> payAsync(
        @RequestParam Long orderId,
        @RequestParam Long userId
    ) {
        return paymentService.processPayment(orderId, userId)
            .handle((result, ex) -> {
                if (ex != null) {
                    return Result.<PaymentResult>fail(500, "支付处理异常");
                }
                if (result.success()) {
                    return Result.ok(result);
                } else {
                    return Result.fail(400, result.message());
                }
            });
    }

    /**
     * 同步支付：立即返回支付结果，适用于快速测试和简单场景
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 支付结果
     */
    @PostMapping("/pay")
    public Result<PaymentResult> paySync(
        @RequestParam Long orderId,
        @RequestParam Long userId
    ) {
        try {
            PaymentResult result = paymentService.payOrderSync(orderId, userId);
            if (result.success()) {
                return Result.ok(result);
            } else {
                return Result.fail(400, result.message());
            }
        } catch (Exception e) {
            return Result.fail(500, "支付失败: " + e.getMessage());
        }
    }

    /**
     * 查询支付状态：根据订单状态映射支付状态（待支付、支付成功、已取消）
     *
     * @param orderId 订单ID
     * @return 支付状态
     */
    @GetMapping("/status/{orderId}")
    public Result<PaymentService.PaymentStatus> getPaymentStatus(@PathVariable Long orderId) {
        try {
            PaymentService.PaymentStatus status = paymentService.getPaymentStatus(orderId);
            return Result.ok(status);
        } catch (Exception e) {
            return Result.fail(404, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 取消支付：关闭待支付订单，释放库存并通知用户取消成功
     *
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/cancel")
    public Result<Void> cancelPayment(
        @RequestParam Long orderId,
        @RequestParam Long userId
    ) {
        try {
            paymentService.cancelPayment(orderId, userId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(400, "取消失败: " + e.getMessage());
        }
    }
}