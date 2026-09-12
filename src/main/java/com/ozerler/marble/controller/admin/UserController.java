package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.PasswordResetRequest;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserCreateRequest;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.dto.UserUpdateRequest;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController extends AbstractController {

    private static final String USER_FORM_VIEW = Constants.VIEW_USER_FORM;
    private static final String RESET_PASSWORD_VIEW = Constants.VIEW_USER_RESET_PASSWORD;

    private final UserService userService;
    private final MessageSource messageSource;

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
    public String showCreateForm(Locale locale, Model model) {
        populateUserForm(model, new UserCreateRequest(), false, null, null, locale);
        return USER_FORM_VIEW;
    }

    @PostMapping("/create")
    public String createUser(@Valid @ModelAttribute("userForm") UserCreateRequest form,
                             BindingResult bindingResult,
                             Locale locale,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateUserForm(model, form, false, bindingResult, null, locale);
            return USER_FORM_VIEW;
        }

        try {
            userService.createUser(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("admin.users.create.success", null, locale));
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            populateUserForm(model, form, false, bindingResult, e.getMessage(), locale);
            return USER_FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Locale locale, Model model) {
        UserDto user = userService.getUserById(id);
        UserUpdateRequest form = UserUpdateRequest.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles())
                .build();

        populateUserForm(model, form, true, null, null, locale);
        return USER_FORM_VIEW;
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("userForm") UserUpdateRequest form,
                             BindingResult bindingResult,
                             Locale locale,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            populateUserForm(model, form, true, bindingResult, null, locale);
            return USER_FORM_VIEW;
        }

        try {
            userService.updateUser(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("admin.users.update.success", null, locale));
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            populateUserForm(model, form, true, bindingResult, e.getMessage(), locale);
            return USER_FORM_VIEW;
        }
    }

    @PostMapping("/{id}/toggle-status")
    public @ResponseBody BackEndResponse toggleStatus(@PathVariable("id") Long id) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Toggling status for user id {}", id);
            userService.toggleUserStatus(id);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Toggle status successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in toggleStatus user {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "toggleStatus", Constants.ERR_FATAL);
        }

        return ber;
    }

    @GetMapping("/{id}/reset-password")
    public String showResetPasswordForm(@PathVariable("id") Long id, Locale locale, Model model) {
        populateResetPasswordForm(model, id, null, null, locale);
        return RESET_PASSWORD_VIEW;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("passwordForm") PasswordResetRequest form,
                                BindingResult bindingResult,
                                Locale locale,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        form.setUserId(id);
        if (bindingResult.hasErrors()) {
            populateResetPasswordForm(model, id, form, bindingResult, locale);
            return RESET_PASSWORD_VIEW;
        }

        userService.resetPassword(form);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("admin.users.password.reset.success", null, locale));
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public @ResponseBody BackEndResponse deleteUser(@PathVariable("id") Long id) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Soft deleting user id {}", id);
            userService.softDeleteUser(id);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Delete user successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in deleteUser {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "deleteUser", Constants.ERR_FATAL);
        }

        return ber;
    }

    private void populateUserForm(Model model, Object form, boolean isEdit,
                                  BindingResult bindingResult, String extraError, Locale locale) {
        model.addAttribute("userForm", form);
        model.addAttribute("allRoles", userService.getAllRoles());
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("formErrors", collectFormErrors(bindingResult, extraError, locale));
        String titleKey = isEdit ? "admin.users.title.edit" : "admin.users.title.create";
        model.addAttribute("pageTitle", messageSource.getMessage(titleKey, null, locale));
    }

    private void populateResetPasswordForm(Model model, Long userId,
                                           PasswordResetRequest form, BindingResult bindingResult, Locale locale) {
        UserDto user = userService.getUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("passwordForm", form != null ? form : PasswordResetRequest.builder().userId(user.getId()).build());
        model.addAttribute("formErrors", collectFormErrors(bindingResult, null, locale));
        model.addAttribute("pageTitle", messageSource.getMessage("admin.users.title.reset_password", null, locale));
    }

    private List<String> collectFormErrors(BindingResult bindingResult, String extraError, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (bindingResult != null) {
            for (ObjectError error : bindingResult.getAllErrors()) {
                String message = null;
                try {
                    message = messageSource.getMessage(error, locale);
                } catch (Exception ignored) {
                }
                if (message == null || message.isBlank()) {
                    message = error.getDefaultMessage();
                }
                if (message != null && !message.isBlank()) {
                    errors.add(message);
                }
            }
        }
        if (extraError != null && !extraError.isBlank()) {
            errors.add(extraError);
        }
        return errors;
    }
}
