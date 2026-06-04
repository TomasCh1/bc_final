function ensurePlayerMatchStatsCardStyles() {
    if (typeof document === 'undefined') return;
    if (document.getElementById('playerMatchStatsCardStyles')) return;
    const style = document.createElement('style');
    style.id = 'playerMatchStatsCardStyles';
    style.textContent = `
.match-statistics {
    background: #FFFFFF;
    border: 1px solid #E0E0E0;
    border-radius: 12px;
    padding: 1.5rem;
    margin-bottom: 2rem;
    box-shadow: 2px 3px 8px 2px rgba(0, 0, 0, 0.05);
    min-width: 0;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
}
.statistics-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
    flex-wrap: wrap;
    gap: 1rem;
}
.statistics-controls {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    overflow-x: visible;
    min-width: 0;
}
.date-filter-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: #E7DFFF;
    border: 1px solid #C4B5FD;
    border-radius: 20px;
    padding: 0.5rem 1rem;
    cursor: pointer;
    transition: all 0.3s ease;
    flex-shrink: 0;
}
.date-filter-wrapper:hover { background: #DDD6FE; }
.date-filter-icon { width: 18px; height: 18px; flex-shrink: 0; }
.date-filter-icon svg { width: 100%; height: 100%; }
.date-filter-text {
    font-size: 0.9rem;
    color: #374151;
    font-weight: 500;
    white-space: nowrap;
}
.date-filter-caret {
    width: 12px;
    height: 12px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}
.date-filter-caret svg { width: 100%; height: 100%; }
.date-filter-clear {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    display: none;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    margin-left: 0.25rem;
    border-radius: 50%;
    transition: background 0.2s ease;
}
.date-filter-wrapper.has-date .date-filter-clear { display: flex; }
.date-filter-wrapper.has-date .date-filter-caret { display: none; }
.date-filter-clear:hover { background: rgba(0, 0, 0, 0.1); }
.date-filter-clear svg { width: 100%; height: 100%; }
.date-filter-dropdown {
    position: absolute;
    top: calc(100% + 0.5rem);
    right: 0;
    left: auto;
    background: #FFFFFF;
    border: 1px solid #E5E7EB;
    border-radius: 12px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06);
    padding: 1rem;
    min-width: 280px;
    max-width: 320px;
    z-index: 1000;
    display: none;
}
.date-filter-wrapper.open .date-filter-dropdown { display: block; }
.date-filter-mode {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 1rem;
    border-bottom: 1px solid #E5E7EB;
    padding-bottom: 0.75rem;
}
.date-filter-mode-btn {
    flex: 1;
    padding: 0.5rem;
    border: 1px solid #E5E7EB;
    border-radius: 6px;
    background: #FFFFFF;
    cursor: pointer;
    font-size: 0.85rem;
    font-weight: 500;
    color: #374151;
    transition: all 0.2s ease;
}
.date-filter-mode-btn:hover { background: #F9FAFB; }
.date-filter-mode-btn.active {
    background: #E7DFFF;
    border-color: #C4B5FD;
    color: #6B21A8;
}
.date-filter-inputs { display: flex; flex-direction: column; gap: 0.75rem; }
.date-filter-input-group { display: flex; flex-direction: column; gap: 0.5rem; }
.date-filter-input-group label {
    font-size: 0.85rem;
    font-weight: 500;
    color: #374151;
}
.date-filter-input-group input[type="date"] {
    padding: 0.5rem;
    border: 1px solid #E5E7EB;
    border-radius: 6px;
    font-size: 0.9rem;
    color: #374151;
}
.date-filter-input-group input[type="date"]:focus {
    outline: none;
    border-color: #C4B5FD;
    box-shadow: 0 0 0 3px rgba(196, 181, 253, 0.1);
}
.date-filter-actions {
    display: flex;
    gap: 0.5rem;
    margin-top: 1rem;
    padding-top: 0.75rem;
    border-top: 1px solid #E5E7EB;
}
.date-filter-btn {
    flex: 1;
    padding: 0.5rem;
    border: none;
    border-radius: 6px;
    font-size: 0.85rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
}
.date-filter-btn.apply { background: #6B21A8; color: #FFFFFF; }
.date-filter-btn.apply:hover { background: #5B1A8A; }
.date-filter-btn.cancel { background: #F3F4F6; color: #374151; }
.date-filter-btn.cancel:hover { background: #E5E7EB; }
.game-stats-page-size, .game-stats-filter-wrap {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}
.game-stats-page-size label {
    font-size: 0.9rem;
    font-weight: 500;
    color: #374151;
    white-space: nowrap;
    margin: 0;
    line-height: 1;
    display: flex;
    align-items: center;
}
.game-stats-filter-wrap label {
    font-size: 0.875rem;
    font-weight: 500;
    color: #374151;
}
.game-stats-page-size-select {
    padding: 0.5rem 1rem;
    border: 1px solid #E5E7EB;
    background: #FFFFFF;
    border-radius: 6px;
    font-size: 0.9rem;
    font-weight: 500;
    color: #374151;
    cursor: pointer;
    transition: all 0.2s ease;
    min-width: 3.5rem;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236B7280' d='M6 8L2 4h8z'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 0.6rem center;
    padding-right: 1.75rem;
}
.game-stats-filter-select {
    padding: 0.4rem 1.75rem 0.4rem 0.75rem;
    border: 1px solid #E5E7EB;
    border-radius: 8px;
    font-size: 0.9rem;
    background-color: #FFFFFF;
    cursor: pointer;
}
.game-stats-page-size-select:hover, .game-stats-filter-select:hover { color: #1F2937; }
.game-stats-page-size-select:focus, .game-stats-filter-select:focus { outline: none; }
.view-toggle {
    display: flex;
    gap: 0.5rem;
    background: #F8F9FA;
    border-radius: 8px;
    padding: 0.25rem;
    flex-wrap: nowrap;
    flex-shrink: 0;
}
.view-btn {
    padding: 0.5rem 1rem;
    border: 1px solid #E5E7EB;
    background: #FFFFFF;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #374151;
    font-weight: 500;
    white-space: nowrap;
}
.view-btn:hover { background: #F3F4F6; }
.view-btn.active {
    background: #1F2937;
    color: #FFFFFF;
    border-color: #1F2937;
}
.view-btn-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}
.view-btn-icon svg { width: 100%; height: 100%; color: inherit; }
.statistics-table {
    width: 100%;
    border-collapse: collapse;
}
.statistics-table th {
    background: #F8F9FA;
    padding: 0.75rem;
    text-align: left;
    font-weight: 600;
    font-size: 0.85rem;
    color: #666666;
    border-bottom: 2px solid #E0E0E0;
}
.statistics-table td {
    padding: 0.75rem;
    border-bottom: 1px solid #E0E0E0;
    font-size: 0.9rem;
}
.statistics-table tr:hover { background: #F8F9FA; }
.game-stats-footer {
    margin-top: 0.75rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
}
.game-stats-pagination-dots {
    display: flex;
    justify-content: center;
    gap: 0.4rem;
    flex-wrap: wrap;
}
.game-stats-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #D1D5DB;
    border: none;
    cursor: pointer;
    padding: 0;
    transition: background 0.2s, transform 0.15s;
}
.game-stats-dot:hover { background: #9CA3AF; transform: scale(1.15); }
.game-stats-dot.active {
    background: #6B21A8;
    transform: scale(1.2);
    box-shadow: 0 0 0 2px rgba(107, 33, 168, 0.3);
}
.win-indicator { color: #28A745; font-weight: 700; }
.loss-indicator { color: #DC2626; font-weight: 700; }
.pending-indicator { color: #9CA3AF; font-weight: 700; }
.charts-view { display: none; }
.charts-view.active { display: block; }
.chart-container {
    background: #FFFFFF;
    border: 1px solid #E0E0E0;
    border-radius: 12px;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.chart-container h3 {
    font-size: 1.1rem;
    font-weight: 600;
    color: #2C2C2C;
    margin-bottom: 1rem;
}
.chart-wrapper {
    position: relative;
    height: 300px;
    margin-bottom: 1rem;
}
.chart-controls {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
    flex-wrap: wrap;
}
.chart-controls label {
    font-size: 0.9rem;
    font-weight: 500;
    color: #666666;
}
.chart-controls select {
    padding: 0.5rem 0.75rem;
    border: 1px solid #E0E0E0;
    border-radius: 6px;
    font-size: 0.9rem;
    background: #FFFFFF;
    color: #374151;
    cursor: pointer;
}
.chart-controls select:focus {
    outline: none;
    border-color: #C4B5FD;
    box-shadow: 0 0 0 3px rgba(196, 181, 253, 0.1);
}
.loading {
    text-align: center;
    padding: 2rem;
    color: #666666;
}
@media (max-width: 768px) {
    .statistics-table { font-size: 0.8rem; }
    .statistics-table th, .statistics-table td { padding: 0.5rem; }
    .statistics-header { flex-direction: column; align-items: flex-start; }
    .statistics-controls { width: 100%; flex-direction: column; align-items: stretch; overflow-x: visible; }
    .date-filter-wrapper, .game-stats-page-size, .game-stats-filter-wrap {
        width: 100%;
        justify-content: space-between;
        position: relative;
    }
    .date-filter-dropdown { left: 0; right: 0; min-width: auto; max-width: calc(100vw - 2rem); box-sizing: border-box; }
    .view-toggle { width: 100%; justify-content: stretch; }
    .view-btn { flex: 1; justify-content: center; min-width: 0; }
    .date-filter-text, .game-stats-page-size-select, .game-stats-filter-select { font-size: 0.85rem; }
}
@media (max-width: 480px) {
    .date-filter-wrapper { padding: 0.4rem 0.75rem; }
    .date-filter-text { font-size: 0.8rem; }
    .view-btn { padding: 0.4rem 0.75rem; font-size: 0.85rem; }
    .view-btn-icon { width: 14px; height: 14px; }
}
`;
    document.head.appendChild(style);
}

function renderPlayerMatchStatsCard(targetId, options) {
    if (typeof document === 'undefined') return;
    const target = document.getElementById(targetId);
    if (!target) return;
    ensurePlayerMatchStatsCardStyles();

    const config = (options && typeof options === 'object') ? options : {};
    const title = config.title || 'Štatistiky za zápasy';

    target.innerHTML = `
        <div class="match-statistics">
            <div class="statistics-header">
                <div class="section-header">
                    <div class="section-icon">
                        <img src="/IMG/statistics-svgrepo-com (1).svg" alt="Statistics" class="section-icon">
                    </div>
                    <div class="section-title">${title}</div>
                </div>
                <div class="statistics-controls">
                    <div class="statistics-controls-bar">
                        <div class="game-stats-category-filter-wrap" id="gameStatsCategoryFilterWrap">
                            <div class="game-stats-category-filter" id="gameStatsCategoryFilter" onclick="toggleGameStatsCategoryDropdown(event)">
                                <span class="game-stats-category-text" id="gameStatsCategoryText">Kategória: Všetky</span>
                                <span class="game-stats-category-caret">▼</span>
                                <div class="game-stats-category-dropdown" id="gameStatsCategoryDropdown">
                                    <div class="game-stats-category-option active" data-category-id="" onclick="selectGameStatsCategory(event, '')">Všetky</div>
                                </div>
                            </div>
                        </div>
                        <div class="game-stats-page-size" id="gameStatsPageSizeWrap">
                            <label for="gameStatsPageSizeSelect">Zobraziť po:</label>
                            <select id="gameStatsPageSizeSelect" class="game-stats-page-size-select" onchange="setGameStatsPageSize(parseInt(this.value, 10))">
                                <option value="10" selected>10</option>
                                <option value="15">15</option>
                                <option value="20">20</option>
                                <option value="25">25</option>
                                <option value="30">30</option>
                            </select>
                        </div>
                        <div class="game-stats-filter-wrap" id="gameStatsFilterWrap">
                            <label for="gameStatsFilterSelect">Filter:</label>
                            <select id="gameStatsFilterSelect" class="game-stats-filter-select" onchange="setGameStatsFilter(this.value)">
                                <option value="all">Všetky odohraté</option>
                                <option value="withStats" selected>Iba so štatistikami</option>
                                <option value="withoutStats">Iba bez štatistík</option>
                            </select>
                        </div>
                    </div>
                    <div class="view-toggle">
                        <button class="view-btn active" id="tableViewBtn" onclick="showTableView()">
                            <span class="view-btn-icon">
                                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="2" y="2" width="4" height="4" fill="currentColor"/>
                                    <rect x="2" y="7" width="4" height="4" fill="currentColor"/>
                                    <rect x="7" y="2" width="4" height="4" fill="currentColor"/>
                                    <rect x="7" y="7" width="4" height="4" fill="currentColor"/>
                                    <rect x="12" y="2" width="2" height="4" fill="currentColor"/>
                                    <rect x="12" y="7" width="2" height="4" fill="currentColor"/>
                                    <rect x="2" y="12" width="4" height="2" fill="currentColor"/>
                                    <rect x="7" y="12" width="4" height="2" fill="currentColor"/>
                                    <rect x="12" y="12" width="2" height="2" fill="currentColor"/>
                                </svg>
                            </span>
                            <span>Tabuľka</span>
                        </button>
                        <button class="view-btn" id="graphViewBtn" onclick="showGraphView()">
                            <span class="view-btn-icon">
                                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M2 12L5 8L8 10L13 4V12H2Z" fill="currentColor" opacity="0.3"/>
                                    <path d="M2 12L5 8L8 10L13 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                            </span>
                            <span>Graf</span>
                        </button>
                    </div>
                </div>
            </div>
            <div id="statisticsContent">
                <div class="loading">Načítava sa...</div>
            </div>
            <div id="chartsContent" class="charts-view">
                <!-- Charts will be rendered here -->
            </div>
        </div>
    `;
}
