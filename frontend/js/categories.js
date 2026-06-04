
/**
 * Get all categories with optional filters
 */
async function getCategories(filters = {}) {
    try {
        const params = new URLSearchParams();
        if (filters.search) params.append('search', filters.search);
        if (filters.season) params.append('season', filters.season);
        if (filters.coachId) params.append('coachId', filters.coachId);
        if (filters.summary) params.append('summary', 'true');

        const url = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}${params.toString() ? '?' + params.toString() : ''}`;
        const response = await fetch(url, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch categories: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching categories:', error);
        throw error;
    }
}

/**
 * Get player count per category (stats only). Returns object { categoryId: count, ... }.
 */
async function getCategoryPlayerCounts() {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/player-counts`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) {
            throw new Error('Failed to fetch category player counts');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching category player counts:', error);
        throw error;
    }
}

/**
 * Get category by ID
 */
async function getCategoryById(id) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/${id}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch category: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching category:', error);
        throw error;
    }
}

/**
 * Create a new category
 */
async function createCategory(categoryData) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(categoryData)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to create category: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error creating category:', error);
        throw error;
    }
}

/**
 * Update category
 */
async function updateCategory(id, categoryData) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/${id}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(categoryData)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to update category: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error updating category:', error);
        throw error;
    }
}

/**
 * Delete category
 */
async function deleteCategory(id) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to delete category: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error deleting category:', error);
        throw error;
    }
}

/**
 * Assign coach to category
 */
async function assignCoachToCategory(categoryId, coachId) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/${categoryId}/assign-coach`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ coachId })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to assign coach: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error assigning coach:', error);
        throw error;
    }
}

/**
 * Remove coach from category
 */
async function removeCoachFromCategory(categoryId) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/${categoryId}/remove-coach`, {
            method: 'POST',
            headers: getAuthHeaders()
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Failed to remove coach: ${response.statusText}`);
        }

        return data;
    } catch (error) {
        console.error('Error removing coach:', error);
        throw error;
    }
}

/**
 * Search categories
 */
async function searchCategories(query) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.SEARCH}?q=${encodeURIComponent(query)}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to search categories: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error searching categories:', error);
        throw error;
    }
}

/**
 * Get categories by season
 */
async function getCategoriesBySeason(season) {
    try {
        const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CATEGORIES.BASE}/season/${encodeURIComponent(season)}`, {
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch categories by season: ${response.statusText}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error fetching categories by season:', error);
        throw error;
    }
}

