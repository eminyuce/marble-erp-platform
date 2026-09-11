// FilePond 4 Setup with CSRF protection and preview plugins
function initFilePond(inputSelector, hiddenTargetSelector) {
    const inputElement = document.querySelector(inputSelector);
    if (!inputElement || !window.FilePond) return;

    if (window.FilePondPluginImagePreview) {
        FilePond.registerPlugin(FilePondPluginImagePreview);
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {};
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    const pond = FilePond.create(inputElement, {
        allowMultiple: false,
        labelIdle: 'Fotoğrafı sürükleyin veya <span class="filepond--label-action">Gözatın</span>',
        server: {
            url: '/api/upload',
            headers: headers,
            process: {
                onload: (response) => {
                    const hiddenTarget = document.querySelector(hiddenTargetSelector);
                    if (hiddenTarget) {
                        hiddenTarget.value = response;
                    }
                    return response;
                }
            },
            revert: (uniqueFileId, load, error) => {
                fetch('/api/upload', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'text/plain',
                        ...headers
                    },
                    body: uniqueFileId
                }).then(res => {
                    const hiddenTarget = document.querySelector(hiddenTargetSelector);
                    if (hiddenTarget) hiddenTarget.value = '';
                    load();
                }).catch(err => error(err.message));
            }
        }
    });

    return pond;
}
