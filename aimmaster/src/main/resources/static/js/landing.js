/**
 * Behavior for index.html. Handles:
 * - Login / register
 * - Sidebar navigation across lobby / history / account / settings / about / contact
 * - Room create / join / spectate
 * - Account profile editing and password change
 * - Settings persistence (client + backend)
 * - Contact form demo
 */
(() => {
    const authCard = document.getElementById('authCard');
    const appShell = document.getElementById('appShell');
    const tabLogin = document.getElementById('tabLogin');
    const tabRegister = document.getElementById('tabRegister');
    const authForm = document.getElementById('authForm');
    const authSubmit = document.getElementById('authSubmit');
    const authError = document.getElementById('authError');
    const welcomeUsername = document.getElementById('welcomeUsername');
    const welcomeTheme = document.getElementById('welcomeTheme');

    let mode = 'login';

    function setMode(newMode) {
        mode = newMode;
        tabLogin.classList.toggle('active', mode === 'login');
        tabRegister.classList.toggle('active', mode === 'register');
        authSubmit.textContent = mode === 'login' ? 'Log in' : 'Create account';
        authError.textContent = '';
    }

    tabLogin.addEventListener('click', () => setMode('login'));
    tabRegister.addEventListener('click', () => setMode('register'));

    authForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        authError.textContent = '';

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        try {
            const result = mode === 'login'
                ? await Api.login(username, password)
                : await Api.register(username, password);

            sessionStorage.setItem('aimmaster.token', result.token);
            sessionStorage.setItem('aimmaster.username', result.username);
            showApp(result.username);
        } catch (err) {
            authError.textContent = err.message;
        }
    });

    function showApp(username) {
        document.querySelector('.page-center').style.display = 'none';
        appShell.style.display = 'block';
        welcomeUsername.textContent = username;
        navigateTo('lobby');
        loadMe();
    }

    document.getElementById('logoutBtn').addEventListener('click', () => {
        sessionStorage.clear();
        location.reload();
    });

    // ===== Sidebar navigation =====
    document.querySelectorAll('.sidebar .nav-item[data-page]').forEach(btn => {
        btn.addEventListener('click', () => {
            navigateTo(btn.dataset.page);
        });
    });

    function navigateTo(page) {
        document.querySelectorAll('.sidebar .nav-item').forEach(b => b.classList.remove('active'));
        const target = document.querySelector(`.sidebar .nav-item[data-page="${page}"]`);
        if (target) target.classList.add('active');

        document.querySelectorAll('.page').forEach(p => p.style.display = 'none');
        const section = document.getElementById('page-' + page);
        if (section) section.style.display = 'block';

        if (page === 'history') loadHistory();
        if (page === 'account') loadAccount();
        if (page === 'settings') loadSettings();
    }

    // ===== Lobby =====
    document.getElementById('createRoomBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('createError');
        errorEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const difficulty = document.getElementById('difficulty').value;
        const durationSeconds = parseInt(document.getElementById('duration').value, 10);
        const maxPlayers = parseInt(document.getElementById('maxPlayers').value, 10);

        try {
            const room = await Api.createRoom(token, difficulty, durationSeconds, maxPlayers);
            enterRoom(room.code, true, room.difficulty, false);
        } catch (err) {
            errorEl.textContent = err.message;
        }
    });

    document.getElementById('joinRoomBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('joinError');
        errorEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const code = document.getElementById('joinCode').value.trim().toUpperCase();
        if (!code) {
            errorEl.textContent = 'Enter a room code.';
            return;
        }

        try {
            const room = await Api.joinRoom(token, code);
            const isHost = room.hostUsername === sessionStorage.getItem('aimmaster.username');
            enterRoom(room.code, isHost, room.difficulty, false);
        } catch (err) {
            errorEl.textContent = err.message;
        }
    });

    document.getElementById('spectateBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('joinError');
        errorEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const code = document.getElementById('joinCode').value.trim().toUpperCase();
        if (!code) {
            errorEl.textContent = 'Enter a room code.';
            return;
        }

        try {
            const room = await Api.getRoomInfo(token, code);
            enterRoom(room.code, false, room.difficulty, true);
        } catch (err) {
            errorEl.textContent = err.message;
        }
    });

    function enterRoom(roomCode, isHost, difficulty, isSpectator) {
        sessionStorage.setItem('aimmaster.roomCode', roomCode);
        sessionStorage.setItem('aimmaster.isHost', String(isHost));
        sessionStorage.setItem('aimmaster.difficulty', difficulty);
        sessionStorage.setItem('aimmaster.isSpectator', String(isSpectator));
        location.href = '/game.html';
    }

    // ===== History =====
    async function loadHistory() {
        const token = sessionStorage.getItem('aimmaster.token');
        const username = sessionStorage.getItem('aimmaster.username');
        const container = document.getElementById('historyContent');

        try {
            const data = await Api.getHistory(token, username);
            let html = `
                <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;">
                    <div style="flex:1;min-width:160px;background:var(--panel);border:1px solid var(--panel-border);border-radius:var(--radius);padding:14px 18px;">
                        <div style="font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;">Matches played</div>
                        <div style="font-size:26px;font-weight:800;color:var(--gold);">${data.totalMatches}</div>
                    </div>
                    <div style="flex:1;min-width:160px;background:var(--panel);border:1px solid var(--panel-border);border-radius:var(--radius);padding:14px 18px;">
                        <div style="font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;">Average score</div>
                        <div style="font-size:26px;font-weight:800;color:var(--gold);">${data.averageScore.toFixed(1)}</div>
                    </div>
                    <div style="flex:1;min-width:160px;background:var(--panel);border:1px solid var(--panel-border);border-radius:var(--radius);padding:14px 18px;">
                        <div style="font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;">Average accuracy</div>
                        <div style="font-size:26px;font-weight:800;color:var(--gold);">${data.averageAccuracyPercent.toFixed(1)}%</div>
                    </div>
                </div>
            `;

            if (!data.matches || data.matches.length === 0) {
                html += '<p style="color:var(--text-dim);">No matches played yet.</p>';
            } else {
                html += `
                    <table style="width:100%;border-collapse:collapse;background:var(--panel);border:1px solid var(--panel-border);border-radius:var(--radius);overflow:hidden;">
                        <thead>
                            <tr>
                                <th style="padding:10px 14px;text-align:left;font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid var(--panel-border);">Room</th>
                                <th style="padding:10px 14px;text-align:left;font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid var(--panel-border);">Score</th>
                                <th style="padding:10px 14px;text-align:left;font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid var(--panel-border);">Accuracy</th>
                                <th style="padding:10px 14px;text-align:left;font-size:12px;color:var(--text-dim);text-transform:uppercase;letter-spacing:1px;border-bottom:1px solid var(--panel-border);">Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.matches.map(m => `
                                <tr>
                                    <td style="padding:10px 14px;border-bottom:1px solid var(--panel-border);"><strong>${escapeHtml(m.roomCode)}</strong></td>
                                    <td style="padding:10px 14px;border-bottom:1px solid var(--panel-border);">${escapeHtml(String(m.score))}</td>
                                    <td style="padding:10px 14px;border-bottom:1px solid var(--panel-border);">${escapeHtml(m.accuracyPercent.toFixed(1) + '%')}</td>
                                    <td style="padding:10px 14px;border-bottom:1px solid var(--panel-border);">${escapeHtml(formatDate(m.timestamp))}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            }

            container.innerHTML = html;
        } catch (err) {
            container.innerHTML = '<p class="error-text">' + escapeHtml(err.message) + '</p>';
        }
    }

    // ===== Account =====
    async function loadMe() {
        const token = sessionStorage.getItem('aimmaster.token');
        try {
            const data = await Api.getMe(token);
            welcomeTheme.textContent = (data.theme || 'regular');
            welcomeTheme.style.display = 'inline-block';
        } catch (e) {
            // ignore
        }
    }

    async function loadAccount() {
        const token = sessionStorage.getItem('aimmaster.token');
        try {
            const data = await Api.getMe(token);
            document.getElementById('accountUsername').value = data.username;
            document.getElementById('accountTheme').value = data.theme || 'regular';
            document.getElementById('accountError').textContent = '';
            document.getElementById('accountSuccess').textContent = '';
            document.getElementById('passwordError').textContent = '';
            document.getElementById('passwordSuccess').textContent = '';
        } catch (err) {
            document.getElementById('accountError').textContent = err.message;
        }
    }

    document.getElementById('updateProfileBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('accountError');
        const successEl = document.getElementById('accountSuccess');
        errorEl.textContent = '';
        successEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const username = document.getElementById('accountUsername').value.trim();
        const theme = document.getElementById('accountTheme').value;

        if (username.length < 3) {
            errorEl.textContent = 'Username must be at least 3 characters.';
            return;
        }

        try {
            const updated = await Api.updateMe(token, username, theme);
            sessionStorage.setItem('aimmaster.username', updated.username);
            welcomeUsername.textContent = updated.username;
            successEl.textContent = 'Profile updated.';
            loadMe();
        } catch (err) {
            errorEl.textContent = err.message;
        }
    });

    document.getElementById('changePasswordBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('passwordError');
        const successEl = document.getElementById('passwordSuccess');
        errorEl.textContent = '';
        successEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const current = document.getElementById('currentPassword').value;
        const newPass = document.getElementById('newPassword').value;

        if (!current || !newPass) {
            errorEl.textContent = 'Fill in both password fields.';
            return;
        }

        try {
            await Api.changePassword(token, current, newPass);
            successEl.textContent = 'Password changed.';
            document.getElementById('currentPassword').value = '';
            document.getElementById('newPassword').value = '';
        } catch (err) {
            errorEl.textContent = err.message;
        }
    });

    // ===== Settings =====
    async function loadSettings() {
        const token = sessionStorage.getItem('aimmaster.token');
        try {
            const data = await Api.getMe(token);
            document.getElementById('settingSound').checked = data.soundEnabled;
            document.getElementById('settingMusic').checked = data.musicEnabled;
            document.getElementById('settingHud').checked = data.showHud;
            document.getElementById('settingThreshold').value = '0.35';
            document.getElementById('settingsError').textContent = '';
            document.getElementById('settingsSuccess').textContent = '';
        } catch (err) {
            document.getElementById('settingsError').textContent = err.message;
        }
    }

    document.getElementById('saveSettingsBtn').addEventListener('click', async () => {
        const errorEl = document.getElementById('settingsError');
        const successEl = document.getElementById('settingsSuccess');
        errorEl.textContent = '';
        successEl.textContent = '';

        const token = sessionStorage.getItem('aimmaster.token');
        const username = sessionStorage.getItem('aimmaster.username');

        // Save sound/music/hud as theme hack via localStorage for the frontend,
        // and also persist to backend via updateMe (we store theme + keep other flags
        // in localStorage for simplicity in this sprint).
        const me = await Api.getMe(token);

        const updated = await Api.updateMe(token, username, me.theme);
        localStorage.setItem('aimmaster.settings.sound', document.getElementById('settingSound').checked);
        localStorage.setItem('aimmaster.settings.music', document.getElementById('settingMusic').checked);
        localStorage.setItem('aimmaster.settings.hud', document.getElementById('settingHud').checked);
        localStorage.setItem('aimmaster.settings.crosshair', document.getElementById('settingCrosshair').value);
        localStorage.setItem('aimmaster.settings.threshold', document.getElementById('settingThreshold').value);

        successEl.textContent = 'Settings saved.';
    });

    // ===== Contact =====
    document.getElementById('sendContactBtn').addEventListener('click', () => {
        const errorEl = document.getElementById('contactError');
        const successEl = document.getElementById('contactSuccess');
        errorEl.textContent = '';
        successEl.textContent = '';

        const subject = document.getElementById('contactSubject').value.trim();
        const message = document.getElementById('contactMessage').value.trim();

        if (!subject || !message) {
            errorEl.textContent = 'Please fill in both fields.';
            return;
        }

        // Demo: no real backend endpoint, just confirm to the user.
        successEl.textContent = 'Thanks! This is a demo — the message was not actually sent.';
        document.getElementById('contactSubject').value = '';
        document.getElementById('contactMessage').value = '';
    });

    // ===== Helpers =====
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function formatDate(iso) {
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        return d.toLocaleString();
    }

    // ===== Auto-login =====
    const existingToken = sessionStorage.getItem('aimmaster.token');
    const existingUsername = sessionStorage.getItem('aimmaster.username');
    if (existingToken && existingUsername) {
        showApp(existingUsername);
    }
})();
