
(function () {
    'use strict';

    var STYLE_ID = 'appModalHelperStyles';
    var OVERLAY_CLASS = 'app-modal-overlay';

    function ensureStyles() {
        if (typeof document === 'undefined') return;
        if (document.getElementById(STYLE_ID)) return;
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent = [
            '.app-modal-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:10050;padding:1rem;}',
            '.app-modal-box{background:#fff;border-radius:12px;padding:1.5rem 1.5rem 1.25rem;max-width:420px;width:100%;box-shadow:0 12px 28px rgba(0,0,0,0.18);font-family:inherit;color:#1f2937;}',
            '[data-theme="dark"] .app-modal-box{background:#1f2430;color:#f3f4f6;}',
            '.app-modal-box h3{margin:0 0 0.6rem;font-size:1.15rem;font-weight:600;}',
            '.app-modal-box p{margin:0 0 1.25rem;font-size:0.95rem;line-height:1.45;white-space:pre-wrap;}',
            '.app-modal-actions{display:flex;justify-content:flex-end;gap:0.6rem;flex-wrap:wrap;}',
            '.app-modal-actions .btn{padding:0.5rem 1.1rem;border-radius:8px;font-weight:600;cursor:pointer;border:none;font-size:0.92rem;}',
            '.app-modal-actions .btn-primary{background:#6B2C91;color:#fff;}',
            '.app-modal-actions .btn-primary:hover{background:#5A2380;}',
            '.app-modal-actions .btn-secondary{background:#F3F4F6;color:#374151;border:1px solid #E5E7EB;}',
            '.app-modal-actions .btn-secondary:hover{background:#E5E7EB;}',
            '.app-modal-actions .btn-danger{background:#DC2626;color:#fff;}',
            '.app-modal-actions .btn-danger:hover{background:#B91C1C;}',
            '.app-modal-box.is-success h3{color:#047857;}',
            '.app-modal-box.is-error h3{color:#B91C1C;}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function buildOverlay(boxClass) {
        var overlay = document.createElement('div');
        overlay.className = OVERLAY_CLASS;
        var box = document.createElement('div');
        box.className = 'app-modal-box' + (boxClass ? ' ' + boxClass : '');
        overlay.appendChild(box);
        return { overlay: overlay, box: box };
    }

    function close(overlay) {
        if (overlay && overlay.parentNode) {
            overlay.parentNode.removeChild(overlay);
        }
    }

    function defaultTitle(type, fallback) {
        if (type === 'error') return 'Chyba';
        if (type === 'success') return 'Hotovo';
        return fallback || 'Upozornenie';
    }

    function showAppAlert(message, options) {
        ensureStyles();
        options = options || {};
        var type = options.type || 'info';
        var built = buildOverlay(type === 'error' ? 'is-error' : (type === 'success' ? 'is-success' : ''));
        var titleText = options.title || defaultTitle(type);
        var okText = options.okText || 'OK';
        var titleEl = document.createElement('h3');
        titleEl.textContent = titleText;
        var msgEl = document.createElement('p');
        msgEl.textContent = message != null ? String(message) : '';
        var actions = document.createElement('div');
        actions.className = 'app-modal-actions';
        var okBtn = document.createElement('button');
        okBtn.type = 'button';
        okBtn.className = 'btn btn-primary';
        okBtn.textContent = okText;
        actions.appendChild(okBtn);
        built.box.appendChild(titleEl);
        built.box.appendChild(msgEl);
        built.box.appendChild(actions);
        document.body.appendChild(built.overlay);
        okBtn.focus();

        return new Promise(function (resolve) {
            function done() {
                close(built.overlay);
                document.removeEventListener('keydown', onKey);
                resolve();
            }
            function onKey(e) {
                if (e.key === 'Escape' || e.key === 'Enter') {
                    e.preventDefault();
                    done();
                }
            }
            okBtn.addEventListener('click', done);
            built.overlay.addEventListener('click', function (e) {
                if (e.target === built.overlay) done();
            });
            document.addEventListener('keydown', onKey);
        });
    }

    function showAppConfirm(message, options) {
        ensureStyles();
        options = options || {};
        var built = buildOverlay();
        var titleText = options.title || 'Potvrdenie';
        var confirmText = options.confirmText || 'Potvrdiť';
        var cancelText = options.cancelText || 'Zrušiť';
        var confirmClass = options.confirmClass || 'btn-danger';
        var titleEl = document.createElement('h3');
        titleEl.textContent = titleText;
        var msgEl = document.createElement('p');
        msgEl.textContent = message != null ? String(message) : '';
        var actions = document.createElement('div');
        actions.className = 'app-modal-actions';
        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = cancelText;
        var okBtn = document.createElement('button');
        okBtn.type = 'button';
        okBtn.className = 'btn ' + confirmClass;
        okBtn.textContent = confirmText;
        actions.appendChild(cancelBtn);
        actions.appendChild(okBtn);
        built.box.appendChild(titleEl);
        built.box.appendChild(msgEl);
        built.box.appendChild(actions);
        document.body.appendChild(built.overlay);
        okBtn.focus();

        return new Promise(function (resolve) {
            function done(result) {
                close(built.overlay);
                document.removeEventListener('keydown', onKey);
                resolve(Boolean(result));
            }
            function onKey(e) {
                if (e.key === 'Escape') { e.preventDefault(); done(false); }
                else if (e.key === 'Enter') { e.preventDefault(); done(true); }
            }
            okBtn.addEventListener('click', function () { done(true); });
            cancelBtn.addEventListener('click', function () { done(false); });
            built.overlay.addEventListener('click', function (e) {
                if (e.target === built.overlay) done(false);
            });
            document.addEventListener('keydown', onKey);
        });
    }

    window.showAppAlert = showAppAlert;
    window.showAppConfirm = showAppConfirm;
})();
