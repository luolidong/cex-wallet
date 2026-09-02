package com.cexwallet.api.scanner;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.scanner.ScannerDtos.SubmitDepositRequest;
import com.cexwallet.api.scanner.ScannerDtos.SubmitDepositResponse;
import com.cexwallet.api.scanner.ScannerDtos.ScannerConfigResponse;
import com.cexwallet.api.scanner.ScannerDtos.ScannerCursorView;
import com.cexwallet.api.scanner.ScannerDtos.UpdateCursorRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/scanner")
public class InternalScannerController {
    private final ScannerDepositService scannerDepositService;
    private final ScannerRepository scannerRepository;
    private final String internalToken;

    public InternalScannerController(
            ScannerDepositService scannerDepositService,
            ScannerRepository scannerRepository,
            @Value("${app.internal.token}") String internalToken
    ) {
        this.scannerDepositService = scannerDepositService;
        this.scannerRepository = scannerRepository;
        this.internalToken = internalToken;
    }

    @GetMapping("/config")
    public ApiResponse<ScannerConfigResponse> config(
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        assertInternalToken(token);
        return ApiResponse.ok(new ScannerConfigResponse(
                scannerRepository.findScannerChains(),
                scannerRepository.findScannerTokens(),
                scannerRepository.findDepositAddresses(),
                scannerRepository.findScannerCursors()
        ));
    }

    @PostMapping("/cursors")
    public ApiResponse<ScannerCursorView> updateCursor(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody UpdateCursorRequest request
    ) {
        assertInternalToken(token);
        return ApiResponse.ok(scannerRepository.upsertScannerCursor(
                request.chainId(),
                request.scannerName(),
                request.lastScannedBlock(),
                request.lastFinalizedBlock()
        ));
    }

    @PostMapping("/deposits")
    public ApiResponse<SubmitDepositResponse> submitDeposit(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody SubmitDepositRequest request
    ) {
        assertInternalToken(token);
        return ApiResponse.ok(scannerDepositService.submitDeposit(request));
    }

    private void assertInternalToken(String token) {
        if (!internalToken.equals(token)) {
            throw new BusinessException("UNAUTHORIZED", "invalid internal token", HttpStatus.UNAUTHORIZED);
        }
    }
}
