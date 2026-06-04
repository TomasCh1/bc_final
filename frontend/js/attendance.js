if (typeof API_BASE === 'undefined') {
    var API_BASE = '/api';
}

/**
 * Record attendance for a single player
 */
async function recordAttendance(attendanceData) {
    const response = await fetch(`${API_BASE}/attendance`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(attendanceData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to record attendance' }));
        throw new Error(error.error || 'Failed to record attendance');
    }
    
    return await response.json();
}

/**
 * Record attendance for multiple players (bulk)
 */
async function recordBulkAttendance(bulkData) {
    const response = await fetch(`${API_BASE}/attendance/bulk`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(bulkData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to record bulk attendance' }));
        throw new Error(error.error || 'Failed to record bulk attendance');
    }
    
    return await response.json();
}

/**
 * Get attendance by ID
 */
async function getAttendanceById(id) {
    const response = await fetch(`${API_BASE}/attendance/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }
    
    return await response.json();
}

/**
 * Get attendance for an event
 */
async function getAttendanceByEvent(eventId) {
    const response = await fetch(`${API_BASE}/attendance/event/${eventId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }
    
    return await response.json();
}

/**
 * Get attendance for a player
 */
async function getAttendanceByPlayer(playerId) {
    const response = await fetch(`${API_BASE}/attendance/player/${playerId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }
    
    return await response.json();
}

/**
 * Get attendance for a player by date range
 */
async function getAttendanceByPlayerAndDateRange(playerId, startDate, endDate) {
    const params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);
    
    const response = await fetch(`${API_BASE}/attendance/player/${playerId}/range?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }
    
    return await response.json();
}

/**
 * Get attendance for all players by date range
 */
async function getAttendanceByDateRange(startDate, endDate) {
    const params = new URLSearchParams();
    params.append('startDate', startDate);
    params.append('endDate', endDate);

    const response = await fetch(`${API_BASE}/attendance/range?${params}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }

    return await response.json();
}

/**
 * Get attendance statistics for a player
 */
async function getPlayerStatistics(playerId, startDate = null, endDate = null) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const queryString = params.toString();
    const url = `${API_BASE}/attendance/player/${playerId}/statistics${queryString ? '?' + queryString : ''}`;
    
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
 * Get attendance by category
 */
async function getAttendanceByCategory(categoryId) {
    const response = await fetch(`${API_BASE}/attendance/category/${categoryId}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch attendance' }));
        throw new Error(error.error || 'Failed to fetch attendance');
    }
    
    return await response.json();
}


/**
 * Export attendance to Excel
 */
async function exportAttendanceToExcel(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.playerId) queryParams.append('playerId', params.playerId);
    if (params.eventId) queryParams.append('eventId', params.eventId);
    if (params.categoryId) queryParams.append('categoryId', params.categoryId);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    
    const url = `${API_BASE}/attendance/export/excel${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to export attendance' }));
        throw new Error(error.error || 'Failed to export attendance');
    }
    
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `attendance_report_${new Date().toISOString().split('T')[0]}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
}

/**
 * Export attendance to PDF
 */
async function exportAttendanceToPdf(params = {}) {
    const queryParams = new URLSearchParams();
    if (params.playerId) queryParams.append('playerId', params.playerId);
    if (params.eventId) queryParams.append('eventId', params.eventId);
    if (params.categoryId) queryParams.append('categoryId', params.categoryId);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    
    const url = `${API_BASE}/attendance/export/pdf${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to export attendance' }));
        throw new Error(error.error || 'Failed to export attendance');
    }
    
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `attendance_report_${new Date().toISOString().split('T')[0]}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
}

if (typeof window !== 'undefined') {
    window.recordBulkAttendance = recordBulkAttendance;
    window.recordAttendance = recordAttendance;
    window.getAttendanceByEvent = getAttendanceByEvent;
    window.getAttendanceByPlayer = getAttendanceByPlayer;
    window.getAttendanceByPlayerAndDateRange = getAttendanceByPlayerAndDateRange;
    window.getAttendanceByDateRange = getAttendanceByDateRange;
    window.getPlayerStatistics = getPlayerStatistics;
    window.exportAttendanceToExcel = exportAttendanceToExcel;
    window.exportAttendanceToPdf = exportAttendanceToPdf;
}
