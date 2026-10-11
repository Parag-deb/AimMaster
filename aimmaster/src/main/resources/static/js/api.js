/**
 * Thin wrappers around every REST endpoint the backend exposes (Sprints 2, 4, 6).
 * Kept in one file so index.html and game.html both reuse the exact same request
 * logic instead of each page hand-rolling its own fetch() calls (same
 * anti-duplication idea the backend follows: shared logic lives in one place).
 *
 * Every function returns a Promise that resolves to the parsed JSON body on
 * success, or throws an Error whose .message is the backend's own error message
 * (from GlobalExceptionHandler's { message: "..." } JSON body) on failure.
 */
const Api = (() => {

    async function request(method, path, body, token) {
        const headers = { 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        const response = await fetch(path, {
            method,
            headers,
            body: body ? JSON.stringify(body) : undefined,
        });

        // Every endpoint (success or error) responds with JSON - see
        // GlobalExceptionHandler for the error shape, and each *Response DTO for
        // the success shape.
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || ('Request failed with status ' + response.status));
        }
        return data;
    }

    return {
        // ===== Sprint 2: auth =====
        register(username, password) {
            return request('POST', '/api/auth/register', { username, password });
        },
        login(username, password) {
            return request('POST', '/api/auth/login', { username, password });
        },

        // ===== Sprint 2/4: rooms =====
        createRoom(token, difficulty, durationSeconds, maxPlayers) {
            return request('POST', '/api/rooms', { difficulty, durationSeconds, maxPlayers }, token);
        },
        joinRoom(token, code) {
            return request('POST', `/api/rooms/${code}/join`, null, token);
        },
        // Sprint 9 (spectator mode): read-only room info, no side effects - never
        // adds the caller to the room, never fails on a full room. Use this instead
        // of joinRoom() when the intent is only to watch.
        getRoomInfo(token, code) {
            return request('GET', `/api/rooms/${code}`, null, token);
        },
        startMatch(token, code) {
            return request('POST', `/api/rooms/${code}/start`, null, token);
        },
        stopMatch(token, code) {
            return request('POST', `/api/rooms/${code}/stop`, null, token);
        },
        // Sprint 9 (reconnect handling): the room's CURRENTLY running match state
        // (targets/leaderboard/elapsed time), for repainting immediately after a
        // (re)connect instead of waiting on the next broadcast tick.
        getLiveState(token, code) {
            return request('GET', `/api/rooms/${code}/live`, null, token);
        },

        // ===== Sprint 6: history / leaderboard =====
        getHistory(token, username) {
            return request('GET', `/api/players/${username}/history`, null, token);
        },
        getLeaderboard(token, code) {
            return request('GET', `/api/rooms/${code}/leaderboard`, null, token);
        },

        // ===== Account / Settings =====
        getMe(token) {
            return request('GET', `/api/players/me`, null, token);
        },
        updateMe(token, username, theme) {
            return request('PUT', `/api/players/me`, { username, theme }, token);
        },
        changePassword(token, currentPassword, newPassword) {
            return request('PUT', `/api/players/me/password`, { currentPassword, newPassword }, token);
        },

        // ===== Sprint 9: admin dashboard =====
        getAdminStats(token) {
            return request('GET', '/api/admin/stats', null, token);
        },
    };
})();
