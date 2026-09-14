function createChangePasswordForm() {
    return {
        showCurrent: false,
        showNew: false,
        showConfirm: false
    };
}

document.addEventListener('alpine:init', () => {
    Alpine.data('changePasswordForm', createChangePasswordForm);
});
