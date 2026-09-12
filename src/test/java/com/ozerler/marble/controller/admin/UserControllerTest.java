package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("toggleStatus should return success BackEndResponse")
    void shouldReturnSuccessOnToggleStatus() {
        Long userId = 5L;

        BackEndResponse response = userController.toggleStatus(userId);

        verify(userService).toggleUserStatus(userId);
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Toggle status successful");
    }

    @Test
    @DisplayName("toggleStatus should return fatal BackEndResponse on error")
    void shouldReturnFatalOnToggleStatusError() {
        Long userId = 5L;
        doThrow(new RuntimeException("User error")).when(userService).toggleUserStatus(userId);

        BackEndResponse response = userController.toggleStatus(userId);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }

    @Test
    @DisplayName("deleteUser should return success BackEndResponse")
    void shouldReturnSuccessOnDeleteUser() {
        Long userId = 10L;

        BackEndResponse response = userController.deleteUser(userId);

        verify(userService).softDeleteUser(userId);
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Delete user successful");
    }

    @Test
    @DisplayName("deleteUser should return fatal BackEndResponse on error")
    void shouldReturnFatalOnDeleteUserError() {
        Long userId = 10L;
        doThrow(new RuntimeException("Delete error")).when(userService).softDeleteUser(userId);

        BackEndResponse response = userController.deleteUser(userId);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
    }
}
