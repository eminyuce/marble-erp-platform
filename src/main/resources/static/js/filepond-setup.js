// FilePond Setup with CSRF protection and multi-file attachment support
function getCsrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = {};
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

function initFilePond(inputSelector, hiddenTargetSelector) {
    const inputElement = document.querySelector(inputSelector);
    if (!inputElement || !window.FilePond) return;

    if (window.FilePondPluginImagePreview) {
        FilePond.registerPlugin(FilePondPluginImagePreview);
    }

    const headers = getCsrfHeaders();

    const pond = FilePond.create(inputElement, {
        allowMultiple: false,
        labelIdle: 'Fotoğrafı sürükleyin veya <span class="filepond--label-action">Gözatın</span>',
        server: {
            url: '/api/upload',
            headers: headers,
            process: {
                onload: (response) => {
                    let fileUrl = response;
                    try {
                        const parsed = typeof response === 'string' ? JSON.parse(response) : response;
                        if (parsed && parsed.response && parsed.response.body) {
                            fileUrl = typeof parsed.response.body === 'object' ? parsed.response.body.filePath : parsed.response.body;
                        }
                    } catch (e) {
                        // Keep raw response string
                    }
                    const hiddenTarget = document.querySelector(hiddenTargetSelector);
                    if (hiddenTarget) {
                        hiddenTarget.value = fileUrl;
                    }
                    return fileUrl;
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

/**
 * Initializes FilePond for multiple file uploads (images, PDFs, DOCX).
 * Appends hidden input elements for all uploaded file IDs into the target container.
 */
function initMultiFilePond(inputSelector, containerSelector, entityType, entityId) {
    const inputElement = document.querySelector(inputSelector);
    const container = document.querySelector(containerSelector);
    if (!inputElement || !window.FilePond) return null;

    if (window.FilePondPluginImagePreview) {
        FilePond.registerPlugin(FilePondPluginImagePreview);
    }

    const headers = getCsrfHeaders();

    const pond = FilePond.create(inputElement, {
        allowMultiple: true,
        maxFiles: 20,
        acceptedFileTypes: [
            'image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/svg+xml',
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-excel',
            'text/csv',
            'application/csv',
            'text/x-csv',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'text/plain',
            'text/markdown',
            'text/x-markdown'
        ],
        labelIdle: 'Dosyaları sürükleyin veya <span class="filepond--label-action">Gözatın</span> (Fotoğraf, SVG, PDF, Excel, CSV, DOCX, TXT, MD)',
        labelFileProcessing: 'Yükleniyor...',
        labelFileProcessingComplete: 'Yüklendi — önizlemek için tıklayın',
        labelTapToCancel: 'İptal etmek için dokunun',
        labelTapToRetry: 'Tekrar denemek için dokunun',
        labelTapToUndo: 'Kaldırmak için dokunun',
        onactivatefile: (fileItem) => {
            openUploadedFile(fileItem);
        },
        onaddfile: (error, fileItem) => {
            if (!error) {
                markFilePreviewable(fileItem);
                setTimeout(() => markFilePreviewable(fileItem), 0);
            }
        },
        onprocessfile: (error, fileItem) => {
            if (!error) {
                markFilePreviewable(fileItem);
            }
        },
        server: {
            process: (fieldName, file, metadata, load, error, progress, abort) => {
                const formData = new FormData();
                formData.append('file', file, file.name);
                formData.append('entityType', entityType || 'BLOCK');
                if (entityId) {
                    formData.append('entityId', entityId);
                }

                const request = new XMLHttpRequest();
                request.open('POST', '/api/upload');

                for (const [key, value] of Object.entries(headers)) {
                    request.setRequestHeader(key, value);
                }

                request.upload.onprogress = (e) => {
                    progress(e.lengthComputable, e.loaded, e.total);
                };

                request.onload = function () {
                    if (request.status >= 200 && request.status < 300) {
                        try {
                            const res = JSON.parse(request.responseText);
                            const fileDto = res?.response?.body;
                            const fileId = fileDto?.id;

                            if (fileId && container) {
                                let hidden = document.getElementById('file-hidden-' + fileId);
                                if (!hidden) {
                                    hidden = document.createElement('input');
                                    hidden.type = 'hidden';
                                    hidden.name = 'fileIds';
                                    hidden.value = fileId;
                                    hidden.id = 'file-hidden-' + fileId;
                                    container.appendChild(hidden);
                                }
                            }
                            if (fileId) {
                                rememberUploadedPreview(container, fileId, file.name);
                            }
                            load(fileId ? String(fileId) : request.responseText);
                        } catch (err) {
                            load(request.responseText);
                        }
                    } else {
                        error('Yükleme başarısız oldu (' + request.status + ')');
                    }
                };

                request.onerror = function () {
                    error('Ağ hatası oluştu');
                };

                request.send(formData);

                return {
                    abort: () => {
                        request.abort();
                        abort();
                    }
                };
            },
            revert: (uniqueFileId, load, error) => {
                if (!uniqueFileId) {
                    load();
                    return;
                }

                fetch('/api/upload/' + encodeURIComponent(uniqueFileId), {
                    method: 'DELETE',
                    headers: headers
                }).then(res => {
                    const hidden = document.getElementById('file-hidden-' + uniqueFileId);
                    if (hidden) {
                        hidden.remove();
                    }
                    const preview = document.getElementById('file-preview-link-' + uniqueFileId);
                    if (preview) {
                        preview.remove();
                    }
                    load();
                }).catch(err => {
                    console.error('Revert error:', err);
                    error(err.message);
                });
            }
        }
    });

    return pond;
}

/**
 * Removes an existing attached file via AJAX and removes its card from the UI.
 */
function deleteAttachedFile(fileId, elementId) {
    if (!confirm('Bu dosyayı kalıcı olarak silmek istediğinize emin misiniz?')) {
        return;
    }

    const card = document.getElementById(elementId);
    const headers = getCsrfHeaders();

    fetch('/api/upload/' + encodeURIComponent(fileId), {
        method: 'DELETE',
        headers: headers
    }).then(response => {
        if (response.ok) {
            if (card) {
                card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                card.style.opacity = '0';
                card.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    card.remove();
                    const container = document.getElementById('attached-files-list');
                    if (container && container.children.length === 0) {
                        const emptyPlaceholder = document.getElementById('no-attached-files-msg');
                        if (emptyPlaceholder) emptyPlaceholder.classList.remove('hidden');
                    }
                }, 300);
            }
        } else {
            alert('Dosya silinirken bir hata oluştu.');
        }
    }).catch(err => {
        console.error('File delete failed:', err);
        alert('Bağlantı hatası oluştu.');
    });
}

function openUploadedFile(fileItemOrId) {
    if (fileItemOrId && typeof fileItemOrId === 'object') {
        const serverId = fileItemOrId.serverId;
        if (serverId && /^\d+$/.test(String(serverId))) {
            window.open('/api/upload/view/' + encodeURIComponent(serverId), '_blank', 'noopener');
            return;
        }
        if (fileItemOrId.file) {
            const url = URL.createObjectURL(fileItemOrId.file);
            window.open(url, '_blank', 'noopener');
        }
        return;
    }
    if (fileItemOrId) {
        window.open('/api/upload/view/' + encodeURIComponent(fileItemOrId), '_blank', 'noopener');
    }
}

function markFilePreviewable(fileItem) {
    const element = document.getElementById('filepond--item-' + fileItem.id);
    if (!element) {
        return;
    }
    element.title = 'Önizlemek için tıklayın';
    element.style.cursor = 'pointer';
}

function rememberUploadedPreview(container, fileId, fileName) {
    if (!container || !fileId) {
        return;
    }
    const linkId = 'file-preview-link-' + fileId;
    if (document.getElementById(linkId)) {
        return;
    }
    const link = document.createElement('a');
    link.id = linkId;
    link.href = '/api/upload/view/' + encodeURIComponent(fileId);
    link.target = '_blank';
    link.rel = 'noopener';
    link.className = 'mt-2 inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100';
    link.textContent = 'Önizle: ' + (fileName || 'dosya');
    container.appendChild(link);
}

window.initFilePond = initFilePond;
window.initMultiFilePond = initMultiFilePond;
window.deleteAttachedFile = deleteAttachedFile;
window.openUploadedFile = openUploadedFile;

