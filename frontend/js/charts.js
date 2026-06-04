if (typeof API_BASE === 'undefined') {
    var API_BASE = API_CONFIG.BASE_URL;
}

/**
 * Get authentication token
 */
function getAuthToken() {
    if (typeof getStoredToken === 'function') {
        return getStoredToken();
    }
    return localStorage.getItem('basketball_auth_token') || '';
}

/**
 * Get time-series chart data
 */
async function getTimeSeriesData(playerId = null, metric = 'INDEX', startDate = null, endDate = null, statType = null, categoryId = null) {
    const params = new URLSearchParams();
    if (playerId) params.append('playerId', playerId);
    if (categoryId) params.append('categoryId', categoryId);
    if (metric) params.append('metric', metric);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (statType) params.append('statType', statType);
    
    const queryString = params.toString();
    const url = `${API_BASE}${API_CONFIG.ENDPOINTS.CHARTS.TIME_SERIES}${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
        }
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch time-series data' }));
        throw new Error(error.error || 'Failed to fetch time-series data');
    }
    
    return await response.json();
}

/**
 * Get category distribution chart data
 */
async function getCategoryDistribution(metric = 'INDEX', startDate = null, endDate = null, statType = null) {
    const params = new URLSearchParams();
    if (metric) params.append('metric', metric);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (statType) params.append('statType', statType);
    
    const queryString = params.toString();
    const url = `${API_BASE}${API_CONFIG.ENDPOINTS.CHARTS.CATEGORY_DISTRIBUTION}${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
        }
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch category distribution' }));
        throw new Error(error.error || 'Failed to fetch category distribution');
    }
    
    return await response.json();
}

/**
 * Get comparative analysis chart data
 */
async function getComparativeAnalysis(metric = 'INDEX', comparisonType = 'players', playerIds = null, startDate = null, endDate = null, categoryId = null) {
    const params = new URLSearchParams();
    if (metric) params.append('metric', metric);
    if (comparisonType) params.append('comparisonType', comparisonType);
    if (categoryId) params.append('categoryId', categoryId);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    if (playerIds && Array.isArray(playerIds) && playerIds.length > 0) {
        playerIds.forEach(id => params.append('playerIds', id));
    }
    
    const queryString = params.toString();
    const url = `${API_BASE}${API_CONFIG.ENDPOINTS.CHARTS.COMPARATIVE}${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
        }
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch comparative analysis' }));
        throw new Error(error.error || 'Failed to fetch comparative analysis');
    }
    
    return await response.json();
}

/**
 * Get aggregated statistics
 */
async function getAggregatedStatistics(playerId = null, startDate = null, endDate = null, statType = null) {
    const params = new URLSearchParams();
    if (playerId) params.append('playerId', playerId);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (statType) params.append('statType', statType);
    
    const queryString = params.toString();
    const url = `${API_BASE}${API_CONFIG.ENDPOINTS.CHARTS.AGGREGATED}${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
        }
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch aggregated statistics' }));
        throw new Error(error.error || 'Failed to fetch aggregated statistics');
    }
    
    return await response.json();
}

if (typeof window !== 'undefined') {
    window.getTimeSeriesData = getTimeSeriesData;
    window.getCategoryDistribution = getCategoryDistribution;
    window.getComparativeAnalysis = getComparativeAnalysis;
    window.getAggregatedStatistics = getAggregatedStatistics;
}
