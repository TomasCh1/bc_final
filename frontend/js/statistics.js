if (typeof API_BASE === 'undefined') {
    var API_BASE = '/api';
}

/**
 * Create statistics record
 */
async function createStatistics(statisticsData) {
    const response = await fetch(`${API_BASE}/statistics`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(statisticsData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to create statistics' }));
        throw new Error(error.error || 'Failed to create statistics');
    }
    
    return await response.json();
}

/**
 * Update statistics record
 */
async function updateStatistics(id, statisticsData) {
    const response = await fetch(`${API_BASE}/statistics/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(statisticsData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update statistics' }));
        throw new Error(error.error || 'Failed to update statistics');
    }
    
    return await response.json();
}

/**
 * Get statistics by ID
 */
async function getStatisticsById(id) {
    const response = await fetch(`${API_BASE}/statistics/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch statistics' }));
        throw new Error(error.error || 'Failed to fetch statistics');
    }
    
    return await response.json();
}

/**
 * Get all statistics with optional filters
 */
async function getStatistics(filters = {}) {
    const params = new URLSearchParams();
    if (filters.playerId) params.append('playerId', filters.playerId);
    if (filters.eventId) params.append('eventId', filters.eventId);
    if (filters.categoryId) params.append('categoryId', filters.categoryId);
    if (filters.statType) params.append('statType', filters.statType);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.opponent) params.append('opponent', filters.opponent);
    
    const queryString = params.toString();
    const url = `${API_BASE}/statistics${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch statistics' }));
        throw new Error(error.error || 'Failed to fetch statistics');
    }
    
    return await response.json();
}

/**
 * Get statistics for the most recent N games (lazy load). Returns { statistics, totalGames, hasMore }.
 */
async function getStatisticsRecentGames(filters = {}) {
    const params = new URLSearchParams();
    params.set('limit', String(filters.limit != null ? filters.limit : 10));
    params.set('offset', String(filters.offset != null ? filters.offset : 0));
    if (filters.categoryId != null) params.append('categoryId', filters.categoryId);
    
    const url = `${API_BASE}/statistics/games/recent?${params.toString()}`;
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch recent game statistics' }));
        throw new Error(error.error || 'Failed to fetch recent game statistics');
    }
    
    return await response.json();
}

/**
 * Get statistics by event
 */
async function getStatisticsByEvent(eventId) {
    const response = await fetch(`${API_BASE}/statistics/event/${eventId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch statistics' }));
        throw new Error(error.error || 'Failed to fetch statistics');
    }
    
    return await response.json();
}

/**
 * Get statistics by player
 */
async function getStatisticsByPlayer(playerId) {
    const response = await fetch(`${API_BASE}/statistics/player/${playerId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch statistics' }));
        throw new Error(error.error || 'Failed to fetch statistics');
    }
    
    return await response.json();
}

/**
 * Get statistics by stat type
 */
async function getStatisticsByType(statType) {
    const response = await fetch(`${API_BASE}/statistics/type/${statType}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch statistics' }));
        throw new Error(error.error || 'Failed to fetch statistics');
    }
    
    return await response.json();
}

/**
 * Delete statistics
 */
async function deleteStatistics(id) {
    const response = await fetch(`${API_BASE}/statistics/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to delete statistics' }));
        throw new Error(error.error || 'Failed to delete statistics');
    }
    
    return await response.json();
}

/**
 * Calculate INDEX from stat values
 */
async function calculateIndex(values) {
    const response = await fetch(`${API_BASE}/statistics/calculate-index`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(values)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to calculate INDEX' }));
        throw new Error(error.error || 'Failed to calculate INDEX');
    }
    
    return await response.json();
}

/**
 * Download one match as a filled XLSX template.
 */
async function downloadMatchTemplate(eventId) {
    if (!eventId) throw new Error('Missing eventId');
    const response = await fetch(`${API_BASE}/statistics/export/match-template?eventId=${encodeURIComponent(eventId)}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to export match template' }));
        throw new Error(error.error || 'Failed to export match template');
    }

    const blob = await response.blob();
    const disposition = response.headers.get('Content-Disposition') || '';
    const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
    const filename = match && match[1] ? match[1] : `match_statistics_${eventId}.xlsx`;

    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
}

if (typeof window !== 'undefined') {
    window.createStatistics = createStatistics;
    window.updateStatistics = updateStatistics;
    window.getStatisticsById = getStatisticsById;
    window.getStatistics = getStatistics;
    window.getStatisticsByEvent = getStatisticsByEvent;
    window.getStatisticsByPlayer = getStatisticsByPlayer;
    window.getStatisticsByType = getStatisticsByType;
    window.deleteStatistics = deleteStatistics;
    window.calculateIndex = calculateIndex;
    window.getStatisticsRecentGames = getStatisticsRecentGames;
    window.downloadMatchTemplate = downloadMatchTemplate;
}


