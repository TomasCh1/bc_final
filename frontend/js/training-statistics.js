if (typeof API_BASE === 'undefined') {
    var API_BASE = '/api';
}

/**
 * Create training statistics record
 */
async function createTrainingStatistics(data) {
    const response = await fetch(`${API_BASE}/training-statistics`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to create training statistics' }));
        throw new Error(error.error || 'Failed to create training statistics');
    }
    
    return await response.json();
}

/**
 * Update training statistics record
 */
async function updateTrainingStatistics(id, data) {
    const response = await fetch(`${API_BASE}/training-statistics/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update training statistics' }));
        throw new Error(error.error || 'Failed to update training statistics');
    }
    
    return await response.json();
}

/**
 * Get training statistics by ID
 */
async function getTrainingStatisticsById(id) {
    const response = await fetch(`${API_BASE}/training-statistics/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch training statistics' }));
        throw new Error(error.error || 'Failed to fetch training statistics');
    }
    
    return await response.json();
}

/**
 * Get all training statistics for a user
 */
async function getTrainingStatisticsByUser(userId) {
    const response = await fetch(`${API_BASE}/training-statistics/user/${userId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch training statistics' }));
        throw new Error(error.error || 'Failed to fetch training statistics');
    }
    
    return await response.json();
}

/**
 * Get last N training statistics for a user
 */
async function getLastTrainingStatistics(userId, limit = 4) {
    const response = await fetch(`${API_BASE}/training-statistics/user/${userId}/last?limit=${limit}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch training statistics' }));
        throw new Error(error.error || 'Failed to fetch training statistics');
    }
    
    return await response.json();
}

/**
 * Get training statistics with filters
 */
async function getTrainingStatisticsWithFilters(filters = {}) {
    const params = new URLSearchParams();
    if (filters.userId) params.append('userId', filters.userId);
    if (filters.category) params.append('category', filters.category);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.search) params.append('search', filters.search);
    
    const queryString = params.toString();
    const url = `${API_BASE}/training-statistics${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch training statistics' }));
        throw new Error(error.error || 'Failed to fetch training statistics');
    }
    
    return await response.json();
}

/**
 * Delete training statistics
 */
async function deleteTrainingStatistics(id) {
    const response = await fetch(`${API_BASE}/training-statistics/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to delete training statistics' }));
        throw new Error(error.error || 'Failed to delete training statistics');
    }
    
    return await response.json();
}
