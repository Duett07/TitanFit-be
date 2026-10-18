package com.app.be.controller.admin;

import com.app.be.dto.ApiResponse;
import com.app.be.dto.admin.AdminStatsResponse;
import com.app.be.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse response = adminStatsService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê hệ thống thành công", response));
    }
}
