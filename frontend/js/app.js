
const body = document.body;

const savedTheme = localStorage.getItem('theme') || 'light';
body.setAttribute('data-theme', savedTheme);

const themeToggle = document.getElementById('themeToggle');
if (themeToggle) {
    updateThemeIcon();
    themeToggle.addEventListener('click', () => {
        const currentTheme = body.getAttribute('data-theme');
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        body.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeIcon();
    });
}

function updateThemeIcon() {
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        const theme = body.getAttribute('data-theme');
        const moonSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>';
        const sunSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>';
        themeToggle.innerHTML = theme === 'light' ? moonSvg : sunSvg;
    }
}

const menuToggle = document.getElementById('menuToggle');
const sidebar = document.getElementById('sidebar');

if (menuToggle && sidebar) {
    menuToggle.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
    });
}

async function checkApiHealth() {
    try {
        const apiStatusElement = document.getElementById('apiStatus');
        if (!apiStatusElement) return;
        
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.HEALTH}`);
        if (response.ok) {
            const data = await response.json();
            apiStatusElement.textContent = '✓ Connected';
            apiStatusElement.style.color = 'green';
        } else {
            throw new Error('API not responding');
        }
    } catch (error) {
        const apiStatusElement = document.getElementById('apiStatus');
        if (apiStatusElement) {
            apiStatusElement.textContent = '✗ Disconnected';
            apiStatusElement.style.color = 'red';
        }
        console.error('API Health Check Failed:', error);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    checkApiHealth();
    checkAuthStatus();
    setupNavigation();
    setupLogoutButton();
    
    setTimeout(() => {
        if (typeof checkPasswordChangeRequired === 'function') {
            checkPasswordChangeRequired();
        }
    }, 1000);
});

function checkAuthStatus() {
    if (typeof isAuthenticated !== 'function') {
        setTimeout(checkAuthStatus, 100);
        return;
    }

    if (isAuthenticated()) {
        const user = getCurrentUser();
        if (user) {
            const welcomeMsg = document.getElementById('welcomeMessage');
            if (welcomeMsg) {
                welcomeMsg.textContent = `Welcome, ${user.name}!`;
            }
            
            const authStatus = document.getElementById('authStatus');
            if (authStatus) {
                authStatus.textContent = `✓ Authenticated as ${user.role}`;
                authStatus.style.color = 'green';
            }

            const hasSidebar = document.getElementById('sidebarNav');
            const isDashboardPage = window.location.pathname.includes('/pages/dashboard.html');
            if (hasSidebar && !isDashboardPage) {
                const userInfo = document.getElementById('userInfo');
                if (userInfo) {
                    userInfo.textContent = `${user.name} (${user.role})`;
                }
            }

            showDashboardContent(user);
        }
    } else {
        const authStatus = document.getElementById('authStatus');
        if (authStatus) {
            authStatus.textContent = 'Not authenticated';
            authStatus.style.color = 'orange';
        }
        
        const welcomeMsg = document.getElementById('welcomeMessage');
        if (welcomeMsg) {
            welcomeMsg.innerHTML = 'Please <a href="/login">login</a> to continue';
        }
    }
}

function setupNavigation() {
    const sidebarNav = document.getElementById('sidebarNav');
    if (!sidebarNav) return;

    if (typeof isAuthenticated !== 'function') {
        setTimeout(setupNavigation, 100);
        return;
    }

    if (!isAuthenticated()) {
        sidebarNav.innerHTML = `
            <a href="/login" class="nav-link">Login</a>
        `;
        return;
    }

    const user = getCurrentUser();
    const role = user ? user.role : 'GUEST';

    let navItems = '';

    const homeSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>';
    const usersSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>';
    const calendarSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>';
    const checkmarkSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" style="vertical-align: middle; margin-right: 0.5rem;"><polyline points="20 6 9 17 4 12"></polyline></svg>';
    const chartSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><line x1="12" y1="20" x2="12" y2="10"></line><line x1="18" y1="20" x2="18" y2="4"></line><line x1="6" y1="20" x2="6" y2="16"></line></svg>';
    const lineChartSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><polyline points="3 3 7 9 12 5 15 11 21 3"></polyline><polyline points="21 3 21 21 3 21 3 3"></polyline></svg>';
    const settingsSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><circle cx="12" cy="12" r="3"></circle><path d="M12 1v6m0 6v6m9-9h-6m-6 0H3m15.364 6.364l-4.243-4.243m0-4.242l4.243-4.243M4.636 19.364l4.243-4.243m0-4.242L4.636 6.636"></path></svg>';
    const boxSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>';
    const exportSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>';
    const logoutSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>';

    if (role === 'TRAINER') {
        navItems += `<a href="/pages/trainer-dashboard.html" class="nav-link">${homeSvg}Dashboard</a>`;
    } else if (role === 'PLAYER') {
        navItems += `<a href="/pages/dashboard.html" class="nav-link">${homeSvg}Dashboard</a>`;
    } else if (role === 'ADMIN') {
        navItems += `<a href="/pages/admin-dashboard.html" class="nav-link">${homeSvg}Dashboard</a>`;
    } else {
        navItems += `<a href="/" class="nav-link">${homeSvg}Dashboard</a>`;
    }

    if (role === 'ADMIN' || role === 'TRAINER') {
        navItems += `<a href="/pages/attendance-history.html" class="nav-link">${checkmarkSvg}Attendance</a>`;
        navItems += `<a href="/pages/trainer-training-stats.html" class="nav-link">${chartSvg}Statistics</a>`;
        navItems += `<a href="/pages/trainer-attendance-charts.html" class="nav-link">${lineChartSvg}Charts & Analytics</a>`;
    }

    if (role === 'ADMIN') {
        navItems += '<div class="nav-divider"></div>';
        navItems += `<a href="/pages/admin-users.html" class="nav-link">${settingsSvg}User Management</a>`;
        navItems += `<a href="/pages/admin-categories.html" class="nav-link">${boxSvg}Kategórie</a>`;
        navItems += `<a href="/pages/trainer-attendance-charts.html" class="nav-link">${lineChartSvg}Grafy účasti</a>`;
        navItems += `<a href="#" class="nav-link">${exportSvg}Exports</a>`;
    }

    if (role === 'PLAYER') {
        navItems += `<a href="/pages/attendance-history.html" class="nav-link">${checkmarkSvg}My Attendance</a>`;
        navItems += `<a href="/pages/trainer-attendance-charts.html" class="nav-link">${lineChartSvg}My Analytics</a>`;
    }

    navItems += '<div class="nav-divider"></div>';
    navItems += `<a href="#" class="nav-link" id="logoutBtn">${logoutSvg}Logout</a>`;

    sidebarNav.innerHTML = navItems;
}

function setupLogoutButton() {
    if (typeof logout !== 'function') {
        setTimeout(setupLogoutButton, 100);
        return;
    }

    setTimeout(() => {
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                await logout();
            });
        }
    }, 200);
}

function showDashboardContent(user) {
    const dashboardCards = document.getElementById('dashboardCards');
    if (!dashboardCards) return;

    const usersSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>';
    const calendarSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>';
    const checkmarkSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" style="vertical-align: middle; margin-right: 0.5rem;"><polyline points="20 6 9 17 4 12"></polyline></svg>';
    const chartSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><line x1="12" y1="20" x2="12" y2="10"></line><line x1="18" y1="20" x2="18" y2="4"></line><line x1="6" y1="20" x2="6" y2="16"></line></svg>';
    const lineChartSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><polyline points="3 3 7 9 12 5 15 11 21 3"></polyline><polyline points="21 3 21 21 3 21 3 3"></polyline></svg>';
    const settingsSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><circle cx="12" cy="12" r="3"></circle><path d="M12 1v6m0 6v6m9-9h-6m-6 0H3m15.364 6.364l-4.243-4.243m0-4.242l4.243-4.243M4.636 19.364l4.243-4.243m0-4.242L4.636 6.636"></path></svg>';
    const boxSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>';
    const exportSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: middle; margin-right: 0.5rem;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>';

    let cardsHTML = '<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-top: 2rem;">';

    if (user.role === 'ADMIN') {
        cardsHTML += `
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color); cursor: pointer;" onclick="window.location.href='/pages/admin-users.html'">
                <h3>${usersSvg}User Management</h3>
                <p>Manage users, roles, and permissions</p>
            </div>
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color); cursor: pointer;" onclick="window.location.href='/pages/admin-categories.html'">
                <h3>${boxSvg}Categories</h3>
                <p>Manage team categories (U13, U15, etc.)</p>
            </div>
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color);">
                <h3>${exportSvg}Exports</h3>
                <p>Export data to SBA formats</p>
            </div>
        `;
    }

    if (user.role === 'ADMIN' || user.role === 'TRAINER') {
        cardsHTML += `
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color); cursor: pointer;" onclick="window.location.href='/pages/attendance-history.html'">
                <h3>${checkmarkSvg}Attendance</h3>
                <p>Record and manage player attendance</p>
            </div>
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color); cursor: pointer;" onclick="window.location.href='/pages/trainer-training-stats.html'">
                <h3>${chartSvg}Statistics</h3>
                <p>Record game and training statistics</p>
            </div>
            <div class="dashboard-card" style="background: var(--bg-primary); padding: 1.5rem; border-radius: 8px; border: 1px solid var(--border-color); cursor: pointer;" onclick="window.location.href='/pages/trainer-attendance-charts.html'">
                <h3>${lineChartSvg}Charts & Analytics</h3>
                <p>View performance charts and analytics</p>
            </div>
        `;
    }

    if (user.role === 'PLAYER') {
        window.location.href = '/pages/dashboard.html';
        return;
    }

    if (user.role === 'TRAINER') {
        window.location.href = '/pages/trainer-dashboard.html';
        return;
    }

    if (user.role === 'ADMIN') {
        window.location.href = '/pages/admin-dashboard.html';
        return;
    }

    cardsHTML += '</div>';
    dashboardCards.innerHTML = cardsHTML;
    dashboardCards.style.display = 'block';
}

