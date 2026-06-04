
const AUTH_STORAGE_KEY = 'basketball_auth_token';
const REMEMBER_ME_KEY = 'basketball_remember_me';
const USER_STORAGE_KEY = 'basketball_user';

function enrichStoredUser(user) {
    if (!user || typeof user !== 'object') return user;
    user.displayName = formatUserDisplayName(user);
    return user;
}

function checkAuthStatusOnLoginPage() {
    const token = getStoredToken();
    if (token) {
        verifyToken(token).then(valid => {
            if (valid) {
                const user = getCurrentUser();
                if (user && user.mustChangePassword) {
                    redirectToPasswordChange();
                } else {
                    redirectToDashboard();
                }
            } else {
                clearAuth();
            }
        });
    }
}

async function login(email, password, rememberMe = false) {
    try {
        
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AUTH.LOGIN}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
            storeToken(data.token, rememberMe);
            
            if (data.user) {
                storeUser(data.user, rememberMe);
            }

            const user = data.user || getCurrentUser();
            if (user && user.mustChangePassword) {
                redirectToPasswordChange();
            } else {
                redirectToDashboard();
            }
            return { success: true, data };
        } else {
            const errorMsg = data.error || data.message || 'Login failed';
            console.error('Login failed:', errorMsg);
            return { success: false, error: errorMsg };
        }
    } catch (error) {
        console.error('Login error:', error);
        return { success: false, error: 'Network error. Please check your connection and try again.' };
    }
}

async function logout() {
    try {
        const token = getStoredToken();
        if (token) {
            await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AUTH.LOGOUT}`, {
                method: 'POST',
                headers: getAuthHeaders()
            });
        }
    } catch (error) {
        console.error('Logout error:', error);
    } finally {
        clearAuth();
        redirectToLogin();
    }
}

function storeToken(token, rememberMe) {
    if (rememberMe) {
        localStorage.setItem(AUTH_STORAGE_KEY, token);
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
        localStorage.setItem(REMEMBER_ME_KEY, 'true');
    } else {
        sessionStorage.setItem(AUTH_STORAGE_KEY, token);
        localStorage.removeItem(AUTH_STORAGE_KEY);
        localStorage.removeItem(REMEMBER_ME_KEY);
    }
}

function getStoredToken() {
    const preferredStorage = getPreferredAuthStorage();
    const fallbackStorage = preferredStorage === localStorage ? sessionStorage : localStorage;
    return preferredStorage.getItem(AUTH_STORAGE_KEY) || fallbackStorage.getItem(AUTH_STORAGE_KEY);
}

function getPreferredAuthStorage() {
    if (localStorage.getItem(AUTH_STORAGE_KEY)) {
        return localStorage;
    }
    if (sessionStorage.getItem(AUTH_STORAGE_KEY)) {
        return sessionStorage;
    }
    return localStorage.getItem(REMEMBER_ME_KEY) === 'true' ? localStorage : sessionStorage;
}

function storeUser(user, rememberMe) {
    const rawUser = JSON.stringify(enrichStoredUser(user));
    if (rememberMe) {
        localStorage.setItem(USER_STORAGE_KEY, rawUser);
        sessionStorage.removeItem(USER_STORAGE_KEY);
        return;
    }
    sessionStorage.setItem(USER_STORAGE_KEY, rawUser);
    localStorage.removeItem(USER_STORAGE_KEY);
}

function readUserFromStorage(storage) {
    const userJson = storage.getItem(USER_STORAGE_KEY);
    if (!userJson) {
        return null;
    }
    try {
        return enrichStoredUser(JSON.parse(userJson));
    } catch (error) {
        storage.removeItem(USER_STORAGE_KEY);
        return null;
    }
}

function clearAuth() {
    if (typeof window.clearAllStoredNavCategoryFilters === 'function') {
        window.clearAllStoredNavCategoryFilters();
    }
    localStorage.removeItem(AUTH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    localStorage.removeItem(REMEMBER_ME_KEY);
}

async function verifyToken(token) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.ME}`, {
            headers: getAuthHeaders(token)
        });
        return response.ok;
    } catch (error) {
        return false;
    }
}

function getAuthHeaders(tokenOverride = null) {
    const token = tokenOverride || getStoredToken();
    if (token) {
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };
    }
    return {
        'Content-Type': 'application/json'
    };
}

function redirectToDashboard() {
    const user = getCurrentUser();
    if (user && user.mustChangePassword) {
        redirectToPasswordChange();
        return;
    }
    if (user && user.role === 'PLAYER') {
        window.location.href = '/pages/dashboard.html';
    } else if (user && user.role === 'TRAINER') {
        window.location.href = '/pages/trainer-dashboard.html';
    } else if (user && user.role === 'ADMIN') {
        window.location.href = '/pages/admin-dashboard.html';
    } else {
        window.location.href = '/';
    }
}

function redirectToPasswordChange() {
    window.location.href = '/pages/change-password.html';
}

function redirectToLogin() {
    window.location.href = '/';
}

function addChangePasswordNavActions() {
    const currentUser = getCurrentUser();

    const userDropdown = document.getElementById('userDropdown');
    if (userDropdown && !userDropdown.querySelector('.change-password-nav-item')) {
        const changePasswordItem = document.createElement('div');
        changePasswordItem.className = 'user-dropdown-item change-password-nav-item';
        changePasswordItem.textContent = 'Zmeniť heslo';
        changePasswordItem.addEventListener('click', (event) => {
            event.stopPropagation();
            redirectToPasswordChange();
        });

        const logoutItem = Array.from(userDropdown.querySelectorAll('.user-dropdown-item'))
            .find((item) => (item.textContent || '').toLowerCase().includes('odhl'));
        if (logoutItem) {
            userDropdown.insertBefore(changePasswordItem, logoutItem);
        } else {
            userDropdown.appendChild(changePasswordItem);
        }
    }

    if (currentUser && currentUser.role === 'PLAYER') {
        return;
    }

    const navSlideMenu = document.getElementById('navSlideMenu');
    if (navSlideMenu && !navSlideMenu.querySelector('.change-password-nav-pill')) {
        const changePasswordButton = document.createElement('a');
        changePasswordButton.href = '/pages/change-password.html';
        changePasswordButton.className = 'nav-pill change-password-nav-pill';
        changePasswordButton.textContent = 'Zmeniť heslo';
        changePasswordButton.addEventListener('click', (event) => {
            event.preventDefault();
            if (typeof closeNavMenu === 'function') {
                closeNavMenu();
            }
            redirectToPasswordChange();
        });

        const logoutButton = Array.from(navSlideMenu.querySelectorAll('button.nav-pill, a.nav-pill'))
            .find((btn) => (btn.textContent || '').toLowerCase().includes('odhl'));
        if (logoutButton) {
            const prev = logoutButton.previousElementSibling;
            if (!prev || !prev.classList.contains('nav-menu-divider')) {
                const divider = document.createElement('div');
                divider.className = 'nav-menu-divider';
                navSlideMenu.insertBefore(divider, logoutButton);
            }
            navSlideMenu.insertBefore(changePasswordButton, logoutButton);
        } else {
            const divider = document.createElement('div');
            divider.className = 'nav-menu-divider';
            navSlideMenu.appendChild(divider);
            navSlideMenu.appendChild(changePasswordButton);
        }
    }
}

function getDisplayRoleLabel(role) {
    if (role === 'ADMIN') return 'Admin';
    if (role === 'TRAINER') return 'Tréner';
    if (role === 'PLAYER') return 'Hráč';
    return role || 'Používateľ';
}

function applyMobileNavBehaviorForAllRoles() {
    const currentUser = getCurrentUser();
    if (!currentUser) {
        return;
    }

    const navSlideMenu = document.getElementById('navSlideMenu');
    if (!navSlideMenu) {
        return;
    }

    let displayName = formatUserDisplayName(currentUser) || 'Používateľ';
    displayName = displayName.replace(/\s*\([^)]*\)\s*$/, '').trim();
    displayName = displayName.replace(/\s*\(PLAYER\)\s*/i, '').trim();
    const displayRole = getDisplayRoleLabel(currentUser.role);
    const userInitial = (displayName.charAt(0) || 'U').toUpperCase();

    let profileBlock = navSlideMenu.querySelector('.nav-menu-profile');
    if (!profileBlock) {
        profileBlock = document.createElement('div');
        profileBlock.className = 'nav-menu-profile';
        const avatar = document.createElement('div');
        avatar.className = 'nav-menu-profile-avatar';
        avatar.id = 'navMenuProfileAvatar';
        avatar.textContent = userInitial;

        const info = document.createElement('div');
        info.className = 'nav-menu-profile-info';

        const name = document.createElement('div');
        name.className = 'nav-menu-profile-name';
        name.id = 'navMenuProfileName';

        const role = document.createElement('div');
        role.className = 'nav-menu-profile-role';
        role.id = 'navMenuProfileRole';
        role.textContent = displayRole;

        info.appendChild(name);
        info.appendChild(role);
        profileBlock.appendChild(avatar);
        profileBlock.appendChild(info);
        navSlideMenu.prepend(profileBlock);
    }

    if (!profileBlock.nextElementSibling || !profileBlock.nextElementSibling.classList.contains('nav-menu-divider')) {
        const divider = document.createElement('div');
        divider.className = 'nav-menu-divider app-profile-divider';
        navSlideMenu.insertBefore(divider, profileBlock.nextSibling);
    }

    const nameEl = navSlideMenu.querySelector('#navMenuProfileName') || navSlideMenu.querySelector('.nav-menu-profile-name');
    const roleEl = navSlideMenu.querySelector('#navMenuProfileRole') || navSlideMenu.querySelector('.nav-menu-profile-role');
    const avatarEl = navSlideMenu.querySelector('#navMenuProfileAvatar') || navSlideMenu.querySelector('.nav-menu-profile-avatar');
    if (nameEl) {
        nameEl.textContent = displayName;
        nameEl.style.whiteSpace = 'normal';
        nameEl.style.overflowWrap = 'anywhere';
        nameEl.style.wordBreak = 'break-word';
        nameEl.style.lineHeight = '1.25';
    }
    if (roleEl) roleEl.textContent = displayRole;
    if (avatarEl) avatarEl.textContent = userInitial;

    let changePasswordButton = navSlideMenu.querySelector('.app-change-password-nav-pill')
        || navSlideMenu.querySelector('.player-change-password-nav-pill')
        || navSlideMenu.querySelector('.change-password-nav-pill')
        || Array.from(navSlideMenu.querySelectorAll('a.nav-pill, button.nav-pill'))
            .find((btn) => {
                const label = (btn.textContent || '').toLowerCase();
                return label.includes('zmen') && label.includes('hesl');
            });

    let logoutButton = navSlideMenu.querySelector('.app-logout-nav-pill')
        || navSlideMenu.querySelector('.player-logout-nav-pill');
    if (!logoutButton) {
        logoutButton = Array.from(navSlideMenu.querySelectorAll('a.nav-pill, button.nav-pill'))
            .find((btn) => (btn.textContent || '').toLowerCase().includes('odhl'));
        if (logoutButton) {
            logoutButton.classList.add('app-logout-nav-pill');
        }
    }

    let actionsDivider = navSlideMenu.querySelector('.app-actions-divider')
        || navSlideMenu.querySelector('.player-actions-divider');
    if (!actionsDivider && (changePasswordButton || logoutButton)) {
        const previous = (changePasswordButton || logoutButton).previousElementSibling;
        if (previous && previous.classList.contains('nav-menu-divider')) {
            actionsDivider = previous;
            actionsDivider.classList.add('app-actions-divider');
        }
    }
    if (!actionsDivider) {
        actionsDivider = document.createElement('div');
        actionsDivider.className = 'nav-menu-divider app-actions-divider';
        const firstAction = changePasswordButton || logoutButton;
        if (firstAction) {
            navSlideMenu.insertBefore(actionsDivider, firstAction);
        } else {
            navSlideMenu.appendChild(actionsDivider);
        }
    }

    if (!changePasswordButton) {
        changePasswordButton = document.createElement('a');
        changePasswordButton.href = '/pages/change-password.html';
        changePasswordButton.className = 'nav-pill app-change-password-nav-pill';
        changePasswordButton.textContent = 'Zmeniť heslo';
        navSlideMenu.appendChild(changePasswordButton);
    } else {
        changePasswordButton.classList.add('app-change-password-nav-pill');
        if (changePasswordButton.tagName === 'A') {
            changePasswordButton.href = '/pages/change-password.html';
        }
        changePasswordButton.textContent = 'Zmeniť heslo';
    }
    if (!changePasswordButton.dataset.boundAction) {
        changePasswordButton.addEventListener('click', () => {
            if (typeof closeNavMenu === 'function') {
                closeNavMenu();
            }
            redirectToPasswordChange();
        });
        changePasswordButton.dataset.boundAction = 'true';
    }

    if (!logoutButton) {
        logoutButton = document.createElement('a');
        logoutButton.href = '#';
        logoutButton.className = 'nav-pill app-logout-nav-pill';
        logoutButton.textContent = 'Odhlásiť sa';
        navSlideMenu.appendChild(logoutButton);
    }
    logoutButton.classList.add('app-logout-nav-pill');
    logoutButton.textContent = 'Odhlásiť sa';
    if (!logoutButton.dataset.boundAction) {
        logoutButton.addEventListener('click', async (event) => {
            event.preventDefault();
            if (typeof closeNavMenu === 'function') {
                closeNavMenu();
            }
            await logout();
        });
        logoutButton.dataset.boundAction = 'true';
    }
}

function isAuthenticated() {
    return getStoredToken() !== null;
}

function getCurrentUser() {
    const preferredStorage = getPreferredAuthStorage();
    const fallbackStorage = preferredStorage === localStorage ? sessionStorage : localStorage;

    const preferredUser = readUserFromStorage(preferredStorage);
    if (preferredUser) {
        return preferredUser;
    }

    return readUserFromStorage(fallbackStorage);
}


if (typeof module !== 'undefined' && module.exports) {
    module.exports = { login, logout, getStoredToken, getAuthHeaders, isAuthenticated, getCurrentUser };
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof isAuthenticated === 'function' && isAuthenticated()) {
        addChangePasswordNavActions();
        applyMobileNavBehaviorForAllRoles();
        setTimeout(applyMobileNavBehaviorForAllRoles, 150);
        setTimeout(applyMobileNavBehaviorForAllRoles, 600);
    }
});

