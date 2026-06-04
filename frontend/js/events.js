if (typeof API_BASE === 'undefined') {
    var API_BASE = '/api';
}

/**
 * Get all events with optional filters
 */
async function getEvents(filters = {}) {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.type) params.append('type', filters.type);
    if (filters.categoryId) params.append('categoryId', filters.categoryId);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.enrich === false) params.append('enrich', 'false');
    
    const queryString = params.toString();
    const url = `${API_BASE}/events${queryString ? '?' + queryString : ''}`;
    
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch events' }));
        throw new Error(error.error || 'Failed to fetch events');
    }
    
    return await response.json();
}

/**
 * Get season summary (wins, losses, win %, category balances, last 5 results). Numbers only, no full events.
 */
async function getSeasonSummary(filters = {}) {
    const params = new URLSearchParams();
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.categoryId != null) params.append('categoryId', filters.categoryId);
    const queryString = params.toString();
    const url = `${API_BASE}/events/season-summary${queryString ? '?' + queryString : ''}`;
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch season summary' }));
        throw new Error(error.error || 'Failed to fetch season summary');
    }
    return await response.json();
}

/**
 * Get event by ID
 */
async function getEventById(id) {
    const response = await fetch(`${API_BASE}/events/${id}`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch event' }));
        throw new Error(error.error || 'Failed to fetch event');
    }
    
    return await response.json();
}

/**
 * Get the match closest to the given date (on or after it). Returns single event or empty object.
 */
async function getClosestMatch(options = {}) {
    const params = new URLSearchParams();
    if (options.date) params.append('date', options.date);
    if (options.categoryId != null) params.append('categoryId', options.categoryId);
    const queryString = params.toString();
    const url = `${API_BASE}/events/closest-match${queryString ? '?' + queryString : ''}`;
    const response = await fetch(url, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch closest match' }));
        throw new Error(error.error || 'Failed to fetch closest match');
    }
    return await response.json();
}

/**
 * Create a new event
 */
async function createEvent(eventData) {
    const response = await fetch(`${API_BASE}/events`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(eventData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to create event' }));
        throw new Error(error.error || 'Failed to create event');
    }
    
    return await response.json();
}

/**
 * Update an existing event
 */
async function updateEvent(id, eventData) {
    const response = await fetch(`${API_BASE}/events/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(eventData)
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update event' }));
        throw new Error(error.error || 'Failed to update event');
    }
    
    return await response.json();
}

/**
 * Delete an event
 */
async function deleteEvent(id) {
    const response = await fetch(`${API_BASE}/events/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    });
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to delete event' }));
        throw new Error(error.error || 'Failed to delete event');
    }
    
    return await response.json();
}

/**
 * Get nominated players for an event (match).
 */
async function getEventNominations(id) {
    const response = await fetch(`${API_BASE}/events/${id}/nominations`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch event nominations' }));
        throw new Error(error.error || 'Failed to fetch event nominations');
    }

    return await response.json();
}

/**
 * Replace nominated players for an event (match).
 */
async function updateEventNominations(id, nominationsOrPlayerIds) {
    const useStructuredNominations = Array.isArray(nominationsOrPlayerIds)
        && nominationsOrPlayerIds.length > 0
        && typeof nominationsOrPlayerIds[0] === 'object'
        && nominationsOrPlayerIds[0] !== null;
    const payload = useStructuredNominations
        ? { nominations: nominationsOrPlayerIds }
        : { playerIds: Array.isArray(nominationsOrPlayerIds) ? nominationsOrPlayerIds : [] };
    const response = await fetch(`${API_BASE}/events/${id}/nominations`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update event nominations' }));
        throw new Error(error.error || 'Failed to update event nominations');
    }

    return await response.json();
}

/**
 * Get referees recorded for a match.
 */
async function getEventReferees(id) {
    const response = await fetch(`${API_BASE}/events/${id}/referees`, {
        method: 'GET',
        headers: getAuthHeaders()
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch event referees' }));
        throw new Error(error.error || 'Failed to fetch event referees');
    }

    return await response.json();
}

/**
 * Replace referees for a match (max 3). Admin/Trainer only.
 * @param {number} id event id
 * @param {Array<{name?: string, grade?: string}>} referees
 */
async function updateEventReferees(id, referees) {
    const payload = {
        referees: Array.isArray(referees) ? referees : []
    };
    const response = await fetch(`${API_BASE}/events/${id}/referees`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update event referees' }));
        throw new Error(error.error || 'Failed to update event referees');
    }

    return await response.json();
}

/**
 * Get opponent jersey numbers for an event (match).
 */
async function getEventOpponents(id) {
    const response = await fetch(`${API_BASE}/events/${id}/opponents`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch event opponents' }));
        throw new Error(error.error || 'Failed to fetch event opponents');
    }
    return await response.json();
}

/**
 * Replace opponent jersey numbers for an event (match).
 */
async function updateEventOpponents(id, jerseyNumbers) {
    const payload = {
        jerseyNumbers: Array.isArray(jerseyNumbers) ? jerseyNumbers : []
    };
    const response = await fetch(`${API_BASE}/events/${id}/opponents`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to update event opponents' }));
        throw new Error(error.error || 'Failed to update event opponents');
    }
    return await response.json();
}

/**
 * Get persisted live actions for a match.
 */
async function getEventLiveActions(id) {
    const response = await fetch(`${API_BASE}/events/${id}/live-actions`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch live actions' }));
        throw new Error(error.error || 'Failed to fetch live actions');
    }
    return await response.json();
}

/**
 * Create one persisted live action for a match.
 */
async function createEventLiveAction(id, payload) {
    const response = await fetch(`${API_BASE}/events/${id}/live-actions`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload || {})
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to create live action' }));
        throw new Error(error.error || 'Failed to create live action');
    }
    return await response.json();
}

/**
 * Delete one persisted live action for a match.
 */
async function deleteEventLiveAction(id, actionId) {
    const response = await fetch(`${API_BASE}/events/${id}/live-actions/${actionId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to delete live action' }));
        throw new Error(error.error || 'Failed to delete live action');
    }
    return await response.json();
}

/**
 * Get live summary (score, V/P, +/- and opponent jerseys) for a match.
 */
async function getEventLiveSummary(id) {
    const response = await fetch(`${API_BASE}/events/${id}/live-summary`, {
        method: 'GET',
        headers: getAuthHeaders()
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ error: 'Failed to fetch live summary' }));
        throw new Error(error.error || 'Failed to fetch live summary');
    }
    return await response.json();
}

if (typeof window !== 'undefined') {
    window.getEventNominations = getEventNominations;
    window.updateEventNominations = updateEventNominations;
    window.getEventReferees = getEventReferees;
    window.updateEventReferees = updateEventReferees;
    window.getEventOpponents = getEventOpponents;
    window.updateEventOpponents = updateEventOpponents;
    window.getEventLiveActions = getEventLiveActions;
    window.createEventLiveAction = createEventLiveAction;
    window.deleteEventLiveAction = deleteEventLiveAction;
    window.getEventLiveSummary = getEventLiveSummary;
}

