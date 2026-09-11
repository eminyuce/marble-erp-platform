package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.*;
import com.ozerler.marble.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

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
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return userService.getUsersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateModal(Model model) {
        model.addAttribute("userForm", new UserCreateRequest());
        model.addAttribute("allRoles", userService.getAllRoles());
        return "admin/users/form :: userModalContent";
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("userForm") UserCreateRequest form,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", userService.getAllRoles());
            return "admin/users/form :: userModalContent";
        }

        try {
            userService.createUser(form);
            model.addAttribute("success", true);
            model.addAttribute("message", "Kullanıcı başarıyla oluşturuldu.");
            return "admin/users/form :: userModalSuccess";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.user", e.getMessage());
            model.addAttribute("allRoles", userService.getAllRoles());
            return "admin/users/form :: userModalContent";
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

        model.addAttribute("userForm", form);
        model.addAttribute("allRoles", userService.getAllRoles());
        model.addAttribute("isEdit", true);
        return "admin/users/form :: userModalContent";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("userForm") UserUpdateRequest form,
                             BindingResult bindingResult, Model model) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", userService.getAllRoles());
            model.addAttribute("isEdit", true);
            return "admin/users/form :: userModalContent";
        }

        try {
            userService.updateUser(form);
            model.addAttribute("success", true);
            model.addAttribute("message", "Kullanıcı bilgileri güncellendi.");
            return "admin/users/form :: userModalSuccess";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.user", e.getMessage());
            model.addAttribute("allRoles", userService.getAllRoles());
            model.addAttribute("isEdit", true);
            return "admin/users/form :: userModalContent";
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
                                BindingResult bindingResult, Model model) {
        form.setUserId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUserById(id));
            return "admin/users/reset-password :: resetPasswordModalContent";
        }

        userService.resetPassword(form);
        model.addAttribute("success", true);
        model.addAttribute("message", "Şifre başarıyla güncellendi.");
        return "admin/users/reset-password :: resetPasswordSuccess";
    }

    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.ok().build();
    }
}
