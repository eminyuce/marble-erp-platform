package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("usersPage exposes all roles and Turkish label map")
    void usersPage_exposesRoleFilters() {
        Model model = new ExtendedModelMap();

        String view = userController.usersPage(model);

        assertThat(view).isEqualTo("admin/users/index");
        assertThat(model.getAttribute("filterRoles")).isNotNull();
        assertThat(model.getAttribute("roleLabelMap")).isInstanceOf(java.util.Map.class);
    }

    @Test
    @DisplayName("getUsersData forwards selected roles for OR filtering")
    void getUsersData_forwardsRoleFilters() {
        TabulatorResponse<UserDto> expected = TabulatorResponse.of(List.of(), 1, 0);
        when(userService.getUsersPaged(eq(1), eq(25), eq(""), isNull(), isNull(),
                eq(List.of("ROLE_ADMIN", "ROLE_SALES", "ROLE_USER")), eq(true)))
                .thenReturn(expected);

        TabulatorResponse<UserDto> response = userController.getUsersData(
                1, 25, "", null, null, "ROLE_USER", List.of("ROLE_ADMIN", "ROLE_SALES"), true);

        assertThat(response).isSameAs(expected);
        verify(userService).getUsersPaged(eq(1), eq(25), eq(""), isNull(), isNull(),
                eq(List.of("ROLE_ADMIN", "ROLE_SALES", "ROLE_USER")), eq(true));
    }

    @Test
    @DisplayName("getUsersData with no roles still requests the unfiltered page")
    void getUsersData_withoutRoles_passesEmptyList() {
        TabulatorResponse<UserDto> expected = TabulatorResponse.of(List.of(), 1, 0);
        when(userService.getUsersPaged(eq(1), eq(10), isNull(), isNull(), isNull(),
                eq(List.of()), isNull()))
                .thenReturn(expected);

        TabulatorResponse<UserDto> response = userController.getUsersData(
                1, 10, null, null, null, null, null, null);

        assertThat(response).isSameAs(expected);
        verify(userService).getUsersPaged(eq(1), eq(10), isNull(), isNull(), isNull(),
                eq(List.of()), isNull());
    }
}
