package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.PasswordResetRequest;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserCreateRequest;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.dto.UserUpdateRequest;
import com.ozerler.marble.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private static final String USER_FORM_FRAGMENT = "admin/users/form :: userModalContent";
    private static final String USER_SUCCESS_FRAGMENT = "admin/users/form :: userModalSuccess";

    private final UserService userService;

    @GetMapping
    public String usersPage() {
        return "admin/users/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<UserDto> getUsersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "enabled", required = false) Boolean enabled) {

        return userService.getUsersPaged(page, size, search, sortField, sortDir, role, enabled);
    }

    @GetMapping("/create")
    public String showCreateModal(Model model) {
        populateUserForm(model, new UserCreateRequest(), false, null, null);
        return USER_FORM_FRAGMENT;
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("userForm") UserCreateRequest form,
                             BindingResult bindingResult,
                             Model model,
                             HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            populateUserForm(model, form, false, bindingResult, null);
            return USER_FORM_FRAGMENT;
        }

        try {
            userService.createUser(form);
            response.setHeader("HX-Trigger", "userSaved");
            return USER_SUCCESS_FRAGMENT;
        } catch (IllegalArgumentException e) {
            populateUserForm(model, form, false, bindingResult, e.getMessage());
            return USER_FORM_FRAGMENT;
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditModal(@PathVariable("id") Long id, Model model) {
        UserDto user = userService.getUserById(id);
        UserUpdateRequest form = UserUpdateRequest.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles())
                .build();

        populateUserForm(model, form, true, null, null);
        return USER_FORM_FRAGMENT;
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("userForm") UserUpdateRequest form,
                             BindingResult bindingResult,
                             Model model,
                             HttpServletResponse response) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            populateUserForm(model, form, true, bindingResult, null);
            return USER_FORM_FRAGMENT;
        }

        try {
            userService.updateUser(form);
            response.setHeader("HX-Trigger", "userSaved");
            return USER_SUCCESS_FRAGMENT;
        } catch (IllegalArgumentException e) {
            populateUserForm(model, form, true, bindingResult, e.getMessage());
            return USER_FORM_FRAGMENT;
        }
    }

    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<Void> toggleStatus(@PathVariable("id") Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/reset-password")
    public String showResetPasswordModal(@PathVariable("id") Long id, Model model) {
        UserDto user = userService.getUserById(id);
        PasswordResetRequest form = PasswordResetRequest.builder().userId(user.getId()).build();
        model.addAttribute("user", user);
        model.addAttribute("passwordForm", form);
        return "admin/users/reset-password :: resetPasswordModalContent";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("passwordForm") PasswordResetRequest form,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletResponse response) {
        form.setUserId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUserById(id));
            return "admin/users/reset-password :: resetPasswordModalContent";
        }

        userService.resetPassword(form);
        response.setHeader("HX-Trigger", "userSaved");
        return "admin/users/reset-password :: resetPasswordSuccess";
    }

    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok().build();
    }

    private void populateUserForm(Model model, Object form, boolean isEdit,
                                  BindingResult bindingResult, String extraError) {
        model.addAttribute("userForm", form);
        model.addAttribute("allRoles", userService.getAllRoles());
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("formErrors", collectFormErrors(bindingResult, extraError));
    }

    private List<String> collectFormErrors(BindingResult bindingResult, String extraError) {
        List<String> errors = new ArrayList<>();
        if (bindingResult != null) {
            for (ObjectError error : bindingResult.getAllErrors()) {
                if (error.getDefaultMessage() != null) {
                    errors.add(error.getDefaultMessage());
                }
            }
        }
        if (extraError != null && !extraError.isBlank()) {
            errors.add(extraError);
        }
        return errors;
    }
}
