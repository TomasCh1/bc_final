
const API_BASE_OVERRIDE =
    typeof window !== 'undefined' && typeof window.__API_BASE_URL__ === 'string'
        ? window.__API_BASE_URL__.trim()
        : '';

const API_CONFIG = {
    BASE_URL: API_BASE_OVERRIDE || '/api',
    ENDPOINTS: {
        HEALTH: '/health',
        AUTH: {
            LOGIN: '/auth/login',
            LOGOUT: '/auth/logout',
            FORGOT_PASSWORD: '/auth/forgot-password',
            RESET_PASSWORD: '/auth/reset-password'
        },
        USERS: {
            BASE: '/users',
            ME: '/users/me',
            SEARCH: '/users/search'
        },
        CATEGORIES: {
            BASE: '/categories',
            SEARCH: '/categories/search'
        },
        EVENTS: {
            BASE: '/events',
            LIST: '/events',
            BY_ID: (id) => `/events/${id}`,
            CREATE: '/events',
            UPDATE: (id) => `/events/${id}`,
            DELETE: (id) => `/events/${id}`,
            BY_CATEGORY: (categoryId) => `/events/category/${categoryId}`,
            BY_TYPE: (type) => `/events/type/${type}`,
            BY_DATE_RANGE: '/events/range'
        },
        ATTENDANCE: {
            BASE: '/attendance',
            RECORD: '/attendance',
            BULK: '/attendance/bulk',
            BY_ID: (id) => `/attendance/${id}`,
            BY_EVENT: (eventId) => `/attendance/event/${eventId}`,
            BY_PLAYER: (playerId) => `/attendance/player/${playerId}`,
            BY_PLAYER_RANGE: (playerId) => `/attendance/player/${playerId}/range`,
            STATISTICS: (playerId) => `/attendance/player/${playerId}/statistics`,
            BY_CATEGORY: (categoryId) => `/attendance/category/${categoryId}`
        },
        CHARTS: {
            BASE: '/charts',
            TIME_SERIES: '/charts/time-series',
            CATEGORY_DISTRIBUTION: '/charts/category-distribution',
            COMPARATIVE: '/charts/comparative',
            AGGREGATED: '/charts/aggregated'
        }
    }
};

if (typeof module !== 'undefined' && module.exports) {
    module.exports = API_CONFIG;
}

if (typeof window !== 'undefined') {
    var NAV_CATEGORY_FILTER_STORAGE_PREFIX = 'navCategoryFilter:';

    window.clearAllStoredNavCategoryFilters = function clearAllStoredNavCategoryFilters() {
        try {
            var keysToRemove = [];
            for (var i = 0; i < localStorage.length; i++) {
                var k = localStorage.key(i);
                if (k && k.indexOf(NAV_CATEGORY_FILTER_STORAGE_PREFIX) === 0) {
                    keysToRemove.push(k);
                }
            }
            for (var j = 0; j < keysToRemove.length; j++) {
                localStorage.removeItem(keysToRemove[j]);
            }
        } catch (e) {
        }
    };

    window.getNavCategoryFilterStorageKey = function getNavCategoryFilterStorageKey() {
        var user = null;
        try {
            if (typeof getCurrentUser === 'function') {
                user = getCurrentUser();
            }
        } catch (e) {
            user = null;
        }
        if (!user) {
            try {
                var raw = localStorage.getItem('basketball_user') || sessionStorage.getItem('basketball_user');
                if (raw) user = JSON.parse(raw);
            } catch (e) {
                user = null;
            }
        }
        var userId = user && user.id != null ? String(user.id) : 'anonymous';
        var role = user && user.role ? String(user.role) : 'unknown';
        return NAV_CATEGORY_FILTER_STORAGE_PREFIX + role + ':' + userId;
    };

    window.getStoredNavCategoryFilterId = function getStoredNavCategoryFilterId() {
        try {
            var key = window.getNavCategoryFilterStorageKey();
            var value = localStorage.getItem(key);
            return value == null ? null : value;
        } catch (e) {
            return null;
        }
    };

    window.setStoredNavCategoryFilterId = function setStoredNavCategoryFilterId(categoryId) {
        try {
            var key = window.getNavCategoryFilterStorageKey();
            localStorage.setItem(key, categoryId == null ? '' : String(categoryId));
        } catch (e) {
            
        }
    };
}

function formatUserDisplayName(user) {
    if (!user) return '';
    if (user.displayName) return user.displayName;
    const given = (user.name || '').trim();
    const sur = (user.surname || '').trim();
    if (given && sur) return given + ' ' + sur;
    return given || sur || '';
}

function userListLabel(user, fallback) {
    const label = formatUserDisplayName(user);
    return label || fallback || '';
}

