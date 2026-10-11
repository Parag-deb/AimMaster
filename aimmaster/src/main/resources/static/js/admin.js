/**
 * Behavior for admin.html (Sprint 9, FR11). Polls GET /api/admin/stats every couple of
 * seconds and redraws the table. The backend is what actually enforces "admin only"
 * (AdminAuthorizer, returning 403 for anyone not on the allowlist) - this page just
 * displays whatever it's allowed to see, and shows the backend's own error message
 * otherwise.
 */
(() => {
    const token = sessionStorage.getItem('aimmaster.token');
    if (!token) {
        location.href = '/index.html';
        return;
    }

    const errorBox = document.getElementById('errorBox');
    const rowsEl = document.getElementById('roomRows');

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function render(stats) {
        document.getElementById('activeMatches').textContent = stats.activeMatchCount;
        document.getElementById('threadCount').textContent = stats.jvmActiveThreadCount;
        document.getElementById('roomCount').textContent = stats.rooms.length;

        if (stats.rooms.length === 0) {
            rowsEl.innerHTML = '<tr><td colspan="7" class="idle">No rooms with any activity right now.</td></tr>';
            return;
        }

        rowsEl.innerHTML = stats.rooms.map(room => `
            <tr>
                <td><strong>${escapeHtml(room.roomCode)}</strong></td>
                <td>${escapeHtml(room.difficulty ?? '—')}</td>
                <td class="${room.matchRunning ? 'running' : 'idle'}">${room.matchRunning ? 'RUNNING' : 'lobby'}</td>
                <td>${room.matchRunning ? room.elapsedSeconds.toFixed(1) + 's' : '—'}</td>
                <td>${room.matchRunning ? room.targetCount : '—'}</td>
                <td>${room.connectedPlayers}</td>
                <td>${room.spectators}</td>
            </tr>`).join('');
    }

    async function refresh() {
        try {
            render(await Api.getAdminStats(token));
            errorBox.textContent = '';
        } catch (err) {
            errorBox.textContent = err.message;
        }
    }

    refresh();
    setInterval(refresh, 2000);
})();
