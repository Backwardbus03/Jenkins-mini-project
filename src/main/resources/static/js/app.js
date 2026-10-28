/**
 * Rent Reminder Portal — UI Interactivity & Polish
 */

// ── Theme Management ──────────────────────────────────────────────────────────
(function initTheme() {
    const savedTheme = localStorage.getItem('rrp_theme') || 'dark';
    if (savedTheme === 'light') {
        document.documentElement.setAttribute('data-theme', 'light');
    }
})();

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme');
    const target = (current === 'light') ? 'dark' : 'light';
    if (target === 'light') {
        document.documentElement.setAttribute('data-theme', 'light');
    } else {
        document.documentElement.removeAttribute('data-theme');
    }
    localStorage.setItem('rrp_theme', target);
    updateThemeToggleIcons();
}

function updateThemeToggleIcons() {
    const isLight = document.documentElement.getAttribute('data-theme') === 'light';
    document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
        btn.innerHTML = isLight ? '🌙 Dark Mode' : '☀️ Light Mode';
    });
}

document.addEventListener('DOMContentLoaded', () => {
    updateThemeToggleIcons();

    // Auto-attach alert close handlers
    document.querySelectorAll('.alert').forEach(alertEl => {
        if (!alertEl.querySelector('.alert-close')) {
            const closeBtn = document.createElement('button');
            closeBtn.className = 'alert-close';
            closeBtn.type = 'button';
            closeBtn.innerHTML = '&times;';
            closeBtn.title = 'Dismiss';
            closeBtn.onclick = () => {
                alertEl.style.transition = 'opacity 0.2s, transform 0.2s';
                alertEl.style.opacity = '0';
                alertEl.style.transform = 'translateY(-4px)';
                setTimeout(() => alertEl.remove(), 200);
            };
            alertEl.appendChild(closeBtn);
        }
    });
});

// ── Demo Credential Autofill ─────────────────────────────────────────────────
function fillDemoLogin(username, password) {
    const u = document.getElementById('username');
    const p = document.getElementById('password');
    if (u && p) {
        u.value = username;
        p.value = password;
        u.style.borderColor = 'var(--accent)';
        p.style.borderColor = 'var(--accent)';
        showToast('Demo credentials filled for ' + username);
        setTimeout(() => {
            u.style.borderColor = '';
            p.style.borderColor = '';
        }, 1200);
    }
}

// ── Password Visibility Toggle ───────────────────────────────────────────────
function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        btn.innerHTML = '👁️‍🗨️';
        btn.title = 'Hide password';
    } else {
        input.type = 'password';
        btn.innerHTML = '👁️';
        btn.title = 'Show password';
    }
}

// ── Copy to Clipboard & Toast ────────────────────────────────────────────────
function copyToClipboard(text, label) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
            showToast((label || 'Copied') + ' copied to clipboard!');
        }).catch(() => {
            fallbackCopy(text, label);
        });
    } else {
        fallbackCopy(text, label);
    }
}

function fallbackCopy(text, label) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
        document.execCommand('copy');
        showToast((label || 'Copied') + ' copied to clipboard!');
    } catch (e) {
        console.error('Failed to copy', e);
    }
    document.body.removeChild(textarea);
}

function showToast(message) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = '✓ ' + message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.transition = 'opacity 0.3s, transform 0.3s';
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

// ── Live Table Filter Helper ─────────────────────────────────────────────────
function liveSearchTable(inputId, tableSelector) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.addEventListener('input', () => {
        const query = input.value.toLowerCase().trim();
        const rows = document.querySelectorAll(tableSelector + ' tbody tr');
        rows.forEach(row => {
            // If it's an empty-state row, don't hide it unless query is present
            if (row.querySelector('.empty-state')) return;
            const text = row.textContent.toLowerCase();
            row.style.display = text.includes(query) ? '' : 'none';
        });
    });
}
