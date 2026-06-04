
/**
 * Get all users (alias for getUsers with no filters)
 */
async function getAllUsers() {
    return await getUsers();
}

/**
 * Get all users with optional filters.
 * Otherwise returns full array (or paginated shape if server sends it).
 */
async function getUsers(filters = {}) {
    try {
        const params = new URLSearchParams();
        if (filters.search) params.append('search', filters.search);
        if (filters.role) params.append('role', filters.role);
        if (filters.categoryId) params.append('categoryId', filters.categoryId);
        if (filters.summary) params.append('summary', 'true');
        if (filters.page != null) params.append('page', String(filters.page));
        if (filters.size != null) params.append('size', String(filters.size));

        const url = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}${params.toString() ? '?' + params.toString() : ''}`;
        const response = await fetch(url, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch users: ${response.statusText}`);
        }

        const data = await response.json();
        if (data != null && typeof data === 'object' && Array.isArray(data.content) && typeof data.totalElements === 'number') {
            return data;
        }
        return data;
    } catch (error) {
        console.error('Error fetching users:', error);
        throw error;
    }
}

/**
 * Get user by ID
 */
async function getUserById(id) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}/${id}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            const msg = data.error || (response.status === 404 ? 'User not found or has been deleted.' : `Failed to fetch user: ${response.statusText}`);
            throw new Error(msg);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching user:', error);
        throw error;
    }
}

/**
 * Get current user profile
 */
async function getCurrentUserProfile() {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.ME}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch current user: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching current user:', error);
        throw error;
    }
}

/**
 * Create a new user
 */
async function createUser(userData) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(userData)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to create user: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error creating user:', error);
        throw error;
    }
}

/**
 * Update user
 */
async function updateUser(id, userData) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}/${id}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(userData)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to update user: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error updating user:', error);
        throw error;
    }
}

/**
 * Delete user
 */
async function deleteUser(id) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to delete user: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error deleting user:', error);
        throw error;
    }
}

/**
 * Assign user to category
 */
async function assignUserToCategory(userId, categoryId) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}/${userId}/assign-category`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ categoryId })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to assign user to category: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error assigning user to category:', error);
        throw error;
    }
}

/**
 * Change user password
 */
async function changePassword(userId, passwordData) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.BASE}/${userId}/change-password`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(passwordData)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to change password: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error changing password:', error);
        throw error;
    }
}

/**
 * Search users
 */
async function searchUsers(query) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.USERS.SEARCH}?q=${encodeURIComponent(query)}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to search users: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error searching users:', error);
        throw error;
    }
}

