(function () {
    'use strict';

    var STORAGE_KEY = 'theme';
    var THEME_LIGHT = 'light';
    var THEME_DARK = 'dark';
    var TOGGLE_ID = 'globalThemeToggle';
    var PRETTY_TOGGLE_ID = 'themeTogglePretty';

    function getSavedTheme() {
        try {
            var value = localStorage.getItem(STORAGE_KEY);
            if (value === THEME_LIGHT || value === THEME_DARK) {
                return value;
            }
        } catch (error) {
        }
        return THEME_LIGHT;
    }

    function setSavedTheme(theme) {
        try {
            localStorage.setItem(STORAGE_KEY, theme);
        } catch (error) {
        }
    }

    function getCurrentTheme() {
        var root = document.documentElement;
        var current = root.getAttribute('data-theme') || document.body.getAttribute('data-theme');
        return current === THEME_DARK ? THEME_DARK : THEME_LIGHT;
    }

    function applyTheme(theme, persist) {
        var normalized = theme === THEME_DARK ? THEME_DARK : THEME_LIGHT;
        var root = document.documentElement;

        root.setAttribute('data-theme', normalized);
        if (document.body) {
            document.body.setAttribute('data-theme', normalized);
        }
        root.style.colorScheme = normalized;

        if (persist) {
            setSavedTheme(normalized);
        }

        syncPageClasses();
        updateToggleIcon(normalized);
        updateChartTheme(normalized);
        document.dispatchEvent(new CustomEvent('themechange', { detail: { theme: normalized } }));
    }

    function syncPageClasses() {
        if (!document.body) return;
        document.body.classList.toggle('has-login-card', !!document.querySelector('.login-card'));
    }

    function moonSvg() {
        return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"></path></svg>';
    }

    function sunSvg() {
        return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="4.5" stroke="currentColor" stroke-width="1.8"></circle><path d="M12 2.5V5M12 19V21.5M2.5 12H5M19 12h2.5M5.2 5.2 7 7M17 17l1.8 1.8M18.8 5.2 17 7M7 17l-1.8 1.8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path></svg>';
    }

    function updateToggleIcon(theme) {
        var btn = document.getElementById(TOGGLE_ID);
        if (btn) {
            btn.innerHTML = theme === THEME_DARK ? sunSvg() : moonSvg();
            btn.title = theme === THEME_DARK ? 'Switch to light mode' : 'Switch to dark mode';
            btn.setAttribute('aria-label', btn.title);
        }

        var inlineToggle = document.getElementById('themeToggle');
        if (inlineToggle) {
            inlineToggle.innerHTML = theme === THEME_DARK ? sunSvg() : moonSvg();
            inlineToggle.title = theme === THEME_DARK ? 'Switch to light mode' : 'Switch to dark mode';
            inlineToggle.setAttribute('aria-label', inlineToggle.title);
        }

        var prettyToggle = document.getElementById(PRETTY_TOGGLE_ID);
        if (prettyToggle) {
            var isDark = theme === THEME_DARK;
            prettyToggle.innerHTML = (isDark ? sunSvg() : moonSvg()) + '<span>' + (isDark ? 'Light mode' : 'Dark mode') + '</span>';
            prettyToggle.title = isDark ? 'Switch to light mode' : 'Switch to dark mode';
            prettyToggle.setAttribute('aria-label', prettyToggle.title);
        }
    }

    function createToggleButton() {
        if (!document.body || document.getElementById(TOGGLE_ID)) {
            return;
        }

        var btn = document.createElement('button');
        btn.id = TOGGLE_ID;
        btn.className = 'global-theme-toggle';
        btn.type = 'button';
        btn.addEventListener('click', function () {
            var nextTheme = getCurrentTheme() === THEME_DARK ? THEME_LIGHT : THEME_DARK;
            applyTheme(nextTheme, true);
        });

        document.body.appendChild(btn);
    }

    function createPrettyToggleButton() {
        if (!document.body || document.getElementById(PRETTY_TOGGLE_ID)) {
            return;
        }

        var host =
            document.querySelector('.dashboard-header .header-right') ||
            document.querySelector('.topbar .top-actions') ||
            document.querySelector('.dashboard-header') ||
            document.querySelector('.topbar');

        if (!host) {
            return;
        }

        var prettyBtn = document.createElement('button');
        prettyBtn.id = PRETTY_TOGGLE_ID;
        prettyBtn.className = 'theme-toggle-pretty';
        prettyBtn.type = 'button';
        prettyBtn.addEventListener('click', function () {
            var nextTheme = getCurrentTheme() === THEME_DARK ? THEME_LIGHT : THEME_DARK;
            applyTheme(nextTheme, true);
        });

        if (host.classList.contains('header-right') || host.classList.contains('top-actions')) {
            host.insertBefore(prettyBtn, host.firstChild);
        } else {
            prettyBtn.classList.add('theme-toggle-pretty-floating-header');
            host.appendChild(prettyBtn);
        }
        document.body.classList.add('has-pretty-theme-toggle');
    }

    function bindInlineToggleIfPresent() {
        var inlineToggle = document.getElementById('themeToggle');
        if (!inlineToggle || inlineToggle.dataset.themeBound === 'true') {
            return;
        }

        inlineToggle.dataset.themeBound = 'true';
        inlineToggle.addEventListener('click', function () {
            var nextTheme = getCurrentTheme() === THEME_DARK ? THEME_LIGHT : THEME_DARK;
            applyTheme(nextTheme, true);
        });
    }

    function updateChartTheme(theme) {
        if (typeof Chart === 'undefined') {
            return;
        }

        var textColor = theme === THEME_DARK ? '#d7dce8' : '#374151';
        var gridColor = theme === THEME_DARK ? 'rgba(255, 255, 255, 0.14)' : 'rgba(0, 0, 0, 0.12)';
        var borderColor = theme === THEME_DARK ? 'rgba(255, 255, 255, 0.24)' : 'rgba(0, 0, 0, 0.2)';

        Chart.defaults.color = textColor;
        Chart.defaults.borderColor = borderColor;

        var instances = Chart.instances || {};
        Object.keys(instances).forEach(function (id) {
            var chart = instances[id];
            if (!chart || !chart.options) {
                return;
            }

            var options = chart.options;
            options.plugins = options.plugins || {};
            options.plugins.legend = options.plugins.legend || {};
            options.plugins.legend.labels = options.plugins.legend.labels || {};
            options.plugins.legend.labels.color = textColor;

            options.plugins.tooltip = options.plugins.tooltip || {};
            options.plugins.tooltip.titleColor = textColor;
            options.plugins.tooltip.bodyColor = textColor;
            options.plugins.tooltip.backgroundColor = theme === THEME_DARK ? '#1f2430' : '#ffffff';
            options.plugins.tooltip.borderColor = borderColor;
            options.plugins.tooltip.borderWidth = 1;

            if (options.scales) {
                Object.keys(options.scales).forEach(function (axisKey) {
                    var axis = options.scales[axisKey];
                    if (!axis) return;

                    axis.ticks = axis.ticks || {};
                    axis.ticks.color = textColor;

                    axis.grid = axis.grid || {};
                    axis.grid.color = gridColor;
                    axis.grid.borderColor = borderColor;

                    axis.title = axis.title || {};
                    axis.title.color = textColor;
                });
            }

            chart.update('none');
        });
    }

    function initTheme() {
        createPrettyToggleButton();
        createToggleButton();
        bindInlineToggleIfPresent();
        applyTheme(getSavedTheme(), false);
        setTimeout(function () {
            createPrettyToggleButton();
            bindInlineToggleIfPresent();
            updateToggleIcon(getCurrentTheme());
        }, 200);
    }

    var initialTheme = getSavedTheme();
    document.documentElement.setAttribute('data-theme', initialTheme);

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTheme);
    } else {
        initTheme();
    }

    window.addEventListener('storage', function (event) {
        if (event.key === STORAGE_KEY && (event.newValue === THEME_LIGHT || event.newValue === THEME_DARK)) {
            applyTheme(event.newValue, false);
        }
    });
})();
