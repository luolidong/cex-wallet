package com.cexwallet.api.user;

import com.cexwallet.api.common.ApiResponse;
import com.cexwallet.api.user.UserDtos.CreateUserRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request.username(), request.email(), request.phone()));
    }

    @GetMapping
    public ApiResponse<List<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(userService.findAll(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<User> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.findById(id));
    }
}

