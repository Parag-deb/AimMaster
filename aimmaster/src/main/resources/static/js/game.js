/**
 * The real game screen (Sprint 7). Three things happen here, kept in clearly
 * separate sections below so each is easy to find and reason about on its own:
 *
 *   1. NETWORK  - connect over STOMP, subscribe to this room's topic, send fire
 *      events, receive arena-state and hit-result broadcasts (Sprints 3-5).
 *   2. GESTURES - MediaPipe Hands reads the webcam, we turn its landmarks into an
 *      aim reticle position and a discrete "fire" trigger (FR3/FR4/FR5).
 *   3. RENDER   - one requestAnimationFrame loop draws the camera feed, the
 *      targets, the reticle, and the HUD onto the canvas.
 *
 * Nothing in this file ever decides a hit happened - it only ever draws what the
 * server has confirmed (arena-state / hit-result broadcasts). Firing sends a
 * *claim* ("I think I hit target X") exactly like FR5 describes; HitResolver on
 * the server is still the only thing that can actually award a point.
 */

// ===== Arena coordinate space - MUST match GameEngine.ARENA_WIDTH/HEIGHT on the server =====
const ARENA_WIDTH = 1000;
const ARENA_HEIGHT = 600;

// How close (in arena units) the reticle must be to a target's centre to claim a
// hit on it. Tune this if firing feels too forgiving or too strict.
const FIRE_HIT_RADIUS = 55;

// How close (normalized 0-1 hand-landmark distance) the thumb tip must get to the
// index finger's base before we count the thumb as "folded". MediaPipe's landmark
// scale varies a little with how close your hand is to the camera, so if fires
// trigger too easily/rarely, this is the first number to adjust.
const THUMB_FOLD_THRESHOLD = 0.08;

// How long a fired bullet takes to visibly fly from the hand to its target (ms).
// Purely visual - the server decides the hit the instant it receives the fire event.
const BULLET_TRAVEL_MS = 200;

// Sprint 9 (reconnect handling): how long to wait between reconnect attempts. A plain
// fixed interval on purpose - exponential backoff would be nicer for a big public
// service, but adds complexity that a small room-based game doesn't need.
const RECONNECT_DELAY_MS = 3000;

// If a match is supposedly live but no arena update has arrived for this long while
// the connection itself is healthy, the match must have ended (see the watchdog below).
const MATCH_SILENCE_MS = 1500;

const TARGET_VISUALS = {
    BIRD: { emoji: '🐦', color: '#7aa2ff', sprite: 'bird' },
    BALLOON: { emoji: '🎈', color: '#ff5b6e', sprite: 'balloon' },
    FRUIT: { emoji: '🍎', color: '#38e06f', sprite: 'fruit' },
};

const SPRITE_SVG = {
    bird: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 56 56"><path d="M28 12c-4 0-8 2-10 6-1-2-3-3-5-3-3 0-5 2-5 5s2 5 5 5c1 0 2-1 2-2-2 0-4 1-5 3-2-1-4-1-6-1 6-2 12-8 18-14 6 6 12 12 18 14-2 0-4 0-6 1-1-2-3-3-5-3 0 1 1 2 2 2 3 0 5-2 5-5s-2-5-5-5c-2 0-4 1-5 3-2-4-6-6-10-6z" fill="%237aa2ff"/></svg>',
    balloon: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="56" height="72" viewBox="0 0 56 72"><ellipse cx="28" cy="24" rx="22" ry="26" fill="%23ff5b6e"/><path d="M20 46c0 6 4 10 8 10s8-4 8-10" fill="none" stroke="%23ff5b6e" stroke-width="3"/><line x1="28" y1="56" x2="28" y2="68" stroke="%238b93b8" stroke-width="2"/><path d="M22 68c3-2 6-2 9 0" fill="none" stroke="%238b93b8" stroke-width="2"/></svg>',
    fruit: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56" viewBox="0 0 56 56"><circle cx="28" cy="32" r="18" fill="%2338e06f"/><path d="M28 14c0-4 3-6 3-6s-2 2-3 4" fill="%23228b3a"/><circle cx="22" cy="28" r="3" fill="rgba(255,255,255,0.25)"/><path d="M18 44c4-2 10-2 16 0" fill="none" stroke="%23228b3a" stroke-width="2"/></svg>',
};

const BACKGROUND_SVG = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="600" viewBox="0 0 1000 600"><defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="%23182140"/><stop offset="55%" stop-color="%230b0f1a"/><stop offset="55%" stop-color="%23111a2b"/><stop offset="100%" stop-color="%230a0f18"/></linearGradient></defs><rect width="1000" height="600" fill="url(%23sky)"/><circle cx="860" cy="90" r="45" fill="%23ffd25b" opacity="0.9"/><ellipse cx="160" cy="140" rx="90" ry="28" fill="%23232c45" opacity="0.6"/><ellipse cx="740" cy="200" rx="120" ry="32" fill="%23232c45" opacity="0.5"/><ellipse cx="420" cy="110" rx="70" ry="22" fill="%23232c45" opacity="0.55"/></svg>';

const SPRITE_IMAGES = {};
const SPRITE_NAMES = Object.keys(SPRITE_SVG);
SPRITE_NAMES.forEach(name => {
    const img = new Image();
    img.src = SPRITE_SVG[name];
    SPRITE_IMAGES[name] = img;
});

const BACKGROUND_IMAGE = new Image();
BACKGROUND_IMAGE.src = '/img/image.png';

// ---------------------------------------------------------------------------
// Bootstrap: this page is useless without a logged-in player in a chosen room.
// ---------------------------------------------------------------------------
const token = sessionStorage.getItem('aimmaster.token');
const myUsername = sessionStorage.getItem('aimmaster.username');
const roomCode = sessionStorage.getItem('aimmaster.roomCode');
// Sprint 9 (spectator mode): a spectator watches only - no camera, no firing, and by
// definition never the host (landing.js sets isHost=false for spectate anyway; the
// explicit check here just makes that impossible to get wrong by hand-editing storage).
const isSpectator = sessionStorage.getItem('aimmaster.isSpectator') === 'true';
const isHost = !isSpectator && sessionStorage.getItem('aimmaster.isHost') === 'true';
const difficulty = sessionStorage.getItem('aimmaster.difficulty') || 'MEDIUM';

if (!token || !myUsername || !roomCode) {
    location.href = '/index.html';
}

// ---------------------------------------------------------------------------
// DOM references
// ---------------------------------------------------------------------------
const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const videoEl = document.getElementById('inputVideo');
const statusBanner = document.getElementById('statusBanner');
const leaderboardRows = document.getElementById('leaderboardRows');
const elapsedLabel = document.getElementById('elapsedLabel');
const myScoreLabel = document.getElementById('myScoreLabel');
const difficultyPill = document.getElementById('difficultyPill');
const hostControls = document.getElementById('hostControls');
const startMatchBtn = document.getElementById('startMatchBtn');
const stopMatchBtn = document.getElementById('stopMatchBtn');
const enableCameraBtn = document.getElementById('enableCameraBtn');

document.getElementById('roomCodeLabel').textContent = roomCode;
document.getElementById('backBtn').addEventListener('click', () => {
    sessionStorage.removeItem('aimmaster.roomCode');
    sessionStorage.removeItem('aimmaster.isHost');
    sessionStorage.removeItem('aimmaster.isSpectator');
    location.href = '/index.html';
});

difficultyPill.textContent = difficulty;
difficultyPill.className = 'pill ' + difficulty.toLowerCase();

if (isHost) {
    hostControls.style.display = 'block';
}

if (isSpectator) {
    // No camera, no firing, no host controls - just the live arena and leaderboard.
    enableCameraBtn.style.display = 'none';
    document.getElementById('spectatorBadge').style.display = 'inline-block';
    document.getElementById('hintText').textContent =
        'You are spectating — watching the match live. Spectators can\'t aim or fire.';
}

// ---------------------------------------------------------------------------
// Shared live state, written by the network section, read by the render loop.
// ---------------------------------------------------------------------------
let latestTargets = [];              // [{id, type, x, y}, ...] from the last ArenaStateMessage
let leaderboard = {};                // username -> score, built up from HitResultMessages
let floatingTexts = [];              // transient "+1" pop effects: {x, y, text, createdAt}
let matchIsLive = false;

// Sprint 9: bullet visuals, reconnect bookkeeping, and the "did the match end?" watchdog.
let bullets = [];                    // in-flight bullets: {x1,y1,x2,y2,targetId,targetType,createdAt,landsAt}
let impacts = [];                    // little expanding rings where a bullet landed: {x, y, createdAt}
let reconnectTimer = null;           // pending setTimeout id while we're waiting to retry, else null
let reconnectAttempts = 0;
let lastArenaMessageAt = 0;          // performance.now() of the newest arena-state broadcast we've seen

// ===========================================================================
// 1. NETWORK - STOMP over WebSocket (Sprint 3-5's transport, reused as-is)
// ===========================================================================
let stompClient = null;

function connectToRoom() {
    // Same handshake convention as config.WebSocketAuthInterceptor expects: the
    // JWT goes in the query string, because a browser can't attach a custom
    // Authorization header to a WebSocket/SockJS handshake.
    const socket = new SockJS('/ws?token=' + encodeURIComponent(token));
    const client = Stomp.over(socket);
    client.debug = null; // silence STOMP's own verbose console logging
    stompClient = client;

    // Each callback remembers WHICH client it belongs to - see onConnectionLost for why.
    client.connect({}, () => onConnected(client), () => onConnectionLost(client));
}

function onConnected(client) {
    if (client !== stompClient) {
        return; // a stale, superseded client - ignore
    }
    reconnectAttempts = 0;

    // Players and spectators listen on two different topics carrying the same
    // broadcasts (see service.RoomBroadcaster on the server).
    const topic = isSpectator
        ? '/topic/room/' + roomCode + '/spectators'
        : '/topic/room/' + roomCode;

    client.subscribe(topic, (frame) => {
        const body = JSON.parse(frame.body);

        if (body.targets !== undefined) {
            handleArenaState(body);
        } else if (body.hitByUsername !== undefined) {
            handleHitResult(body);
        } else if (body.type === 'PLAYER_JOINED') {
            handlePlayerJoined(body);
        } else if (body.username !== undefined) {
            handlePlayerLeft(body);
        }
        // (a WsTestMessage could also arrive here from Sprint 3's test client, if
        // anyone still has it open on the same room - harmlessly ignored, since it
        // matches none of the branches above.)
    });

    // Catch up on whatever happened while we weren't listening (first load, a page
    // refresh, or a reconnect after a dropped connection all take this same path).
    syncLiveState();
}

/**
 * Sprint 9 (reconnect handling): called when the connection fails to open OR drops
 * later (stomp.js reports both through the same error callback). Instead of leaving
 * the player at a dead end, retry every RECONNECT_DELAY_MS until it works.
 *
 * Two guards matter here, both against creating duplicate connections (which would
 * mean duplicate subscriptions, and so every hit popping up twice):
 *   1. a late "closed" event from an OLD client that we've already replaced must be
 *      ignored (client !== stompClient), and
 *   2. one drop can report an error more than once - only ever schedule one retry.
 */
function onConnectionLost(client) {
    if (client !== stompClient) {
        return;
    }
    if (reconnectTimer !== null) {
        return;
    }

    reconnectAttempts++;
    statusBanner.style.display = 'block';
    statusBanner.textContent = reconnectAttempts > 5
        ? 'Still can\'t reconnect — your session may have expired. Go back to the lobby and log in again.'
        : 'Connection lost — reconnecting…';

    reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        connectToRoom();
    }, RECONNECT_DELAY_MS);
}

/**
 * Sprint 9 (reconnect handling): fetch the room's CURRENT live state over REST and
 * repaint from it, instead of sitting on a blank/stale screen until the next
 * broadcast (or, for scores, until somebody scores again). This is the "catch me
 * up" half of the REST-vs-WebSocket split; the topic subscription is the "keep me
 * updated" half.
 */
async function syncLiveState() {
    const requestedAt = performance.now();
    const wasLive = matchIsLive; // captured BEFORE the await: what we believed when we asked
    let state;
    try {
        state = await Api.getLiveState(token, roomCode);
    } catch (err) {
        // Non-fatal: worst case we simply start from a blank slate and fill in from
        // the live broadcasts as they arrive.
        console.warn('Could not sync live state:', err.message);
        return;
    }

    // If we weren't showing a live match, whatever board we have is left over from an
    // earlier match (or empty) and can't be trusted - start from the server's.
    if (!wasLive) {
        leaderboard = {};
    }
    // Scores only ever go up, so merging with Math.max is always safe - and it means a
    // hit broadcast that landed while this request was in flight can never be
    // overwritten by this (slightly older) snapshot.
    for (const entry of state.leaderboard) {
        leaderboard[entry.username] = Math.max(leaderboard[entry.username] || 0, entry.score);
    }
    renderLeaderboard();
    myScoreLabel.textContent = String(leaderboard[myUsername] || 0);

    // A live broadcast that arrived during the request is fresher than this snapshot.
    if (lastArenaMessageAt > requestedAt) {
        return;
    }

    if (state.running) {
        matchIsLive = true;
        lastArenaMessageAt = performance.now(); // so the "match ended?" watchdog doesn't fire instantly
        latestTargets = state.targets;
        elapsedLabel.textContent = state.elapsedSeconds.toFixed(1) + 's';
        statusBanner.style.display = 'none';
        if (isHost) {
            startMatchBtn.style.display = 'none';
            stopMatchBtn.style.display = 'inline-block';
        }
    } else if (matchIsLive) {
        onMatchEnded(); // the match finished while we were disconnected
    } else {
        showWaitingBanner();
    }
}

function showWaitingBanner() {
    statusBanner.style.display = 'block';
    if (isSpectator) {
        statusBanner.textContent = 'Waiting for the match to start…';
    } else if (isHost) {
        statusBanner.textContent = 'Ready — click "Start match" when everyone has joined.';
    } else {
        statusBanner.textContent = 'Waiting for the host to start the match…';
    }
}

/**
 * The single place that switches the screen from "match live" to "match over" -
 * used by the host's Stop button, by the watchdog below, and by syncLiveState() when
 * a reconnect discovers the match ended while we were away. The final scores stay
 * on screen (they're only cleared when the NEXT match starts - see handleArenaState).
 */
function onMatchEnded() {
    matchIsLive = false;
    latestTargets = [];
    statusBanner.style.display = 'block';
    statusBanner.textContent = 'Match ended. Check your stats from the lobby.';

    if (isHost) {
        stopMatchBtn.style.display = 'none';
        startMatchBtn.style.display = 'inline-block';
        startMatchBtn.disabled = false;
    }
}

// Watchdog: the server never sends an explicit "match ended" message (the game loop
// just stops broadcasting when the timer runs out), so a healthy connection that has
// gone quiet for MATCH_SILENCE_MS while a match was live means the match is over.
// It stays silent whenever the connection itself is down - in that case the
// reconnect logic above owns the banner, and "no messages" means nothing.
setInterval(() => {
    if (!matchIsLive || !stompClient || !stompClient.connected) {
        return;
    }
    if (performance.now() - lastArenaMessageAt > MATCH_SILENCE_MS) {
        onMatchEnded();
    }
}, 500);

function handleArenaState(message) {
    if (!matchIsLive) {
        if (message.elapsedSeconds < 3) {
            // A genuinely fresh match: the previous match's scores must not linger into
            // it. (Scores are deliberately left showing after a match ends, so players
            // can read the final standings - this is the moment they get cleared.)
            leaderboard = {};
            renderLeaderboard();
            myScoreLabel.textContent = '0';
        } else {
            // We're joining a match that's already well underway (or recovering from a
            // hiccup) - never wipe scores here, fetch the real ones instead.
            syncLiveState();
        }
    }

    matchIsLive = true;
    lastArenaMessageAt = performance.now();
    latestTargets = message.targets;
    elapsedLabel.textContent = message.elapsedSeconds.toFixed(1) + 's';
    statusBanner.style.display = 'none';

    if (isHost) {
        startMatchBtn.style.display = 'none';
        stopMatchBtn.style.display = 'inline-block';
    }
}

function handleHitResult(result) {
    // The authoritative number is recorded immediately...
    leaderboard[result.hitByUsername] = result.newScore;
    const mine = result.hitByUsername === myUsername;

    // ...but if this was MY shot and my bullet is still in the air, hold the VISIBLE
    // score change and "+1" until it actually lands, so the point appears when the
    // bullet hits the target rather than a moment before it gets there. (Locally the
    // server confirms in a few milliseconds, far faster than a bullet flight.)
    const bullet = mine ? bullets.find(b => b.targetId === result.targetId) : null;
    const target = latestTargets.find(t => t.id === result.targetId);
    const popAt = bullet ? { x: bullet.x2, y: bullet.y2 } : (target ? { x: target.x, y: target.y } : null);
    const delayMs = bullet ? Math.max(0, bullet.landsAt - performance.now()) : 0;

    setTimeout(() => {
        renderLeaderboard();
        if (mine) {
            myScoreLabel.textContent = String(result.newScore);
        }
        if (popAt) {
            floatingTexts.push({
                x: popAt.x,
                y: popAt.y,
                text: (mine ? 'YOU +1' : result.hitByUsername + ' +1'),
                createdAt: performance.now(),
            });
        }
    }, delayMs);
}

/**
     * FR17: someone joined the room mid-match. Show a transient "user joined"
     * banner so the host (and other players) see who just entered, without
     * touching game state. If the match is live, we also add them to the
     * in-memory leaderboard so they appear with score 0 instead of being
     * invisible until their first hit.
     */
    function handlePlayerJoined(message) {
        if (matchIsLive && leaderboard[message.username] === undefined) {
            leaderboard[message.username] = 0;
            renderLeaderboard();
        }

        const wasHidden = statusBanner.style.display === 'none';
        statusBanner.style.display = 'block';
        statusBanner.textContent = message.username + ' joined the room';

        setTimeout(() => {
            if (matchIsLive && statusBanner.textContent === message.username + ' joined the room') {
                statusBanner.style.display = wasHidden ? 'none' : statusBanner.style.display;
            }
        }, 3000);
    }

    /**
     * FR16: someone's WebSocket dropped while others remain in the room. This never
     * touches game state (no score change, no target change) - it's purely a courtesy
     * notice so the remaining players understand why one fewer person is shooting,
     * instead of silently wondering. The match itself keeps running untouched;
     * RoomPresenceListener on the server only stops the match if EVERYONE leaves.
     */
    function handlePlayerLeft(message) {
        const wasHidden = statusBanner.style.display === 'none';
        statusBanner.style.display = 'block';
        statusBanner.textContent = message.username + ' left the room';

        setTimeout(() => {
            // Only auto-hide it again if the match is still going and nothing else
            // (like a "match ended" message) has since taken over the banner.
            if (matchIsLive && statusBanner.textContent === message.username + ' left the room') {
                statusBanner.style.display = wasHidden ? 'none' : statusBanner.style.display;
            }
        }, 3000);
    }

function renderLeaderboard() {
    const rows = Object.entries(leaderboard).sort((a, b) => b[1] - a[1]);
    leaderboardRows.innerHTML = rows.map(([username, score]) => {
        const meClass = username === myUsername ? ' me' : '';
        return `<div class="leaderboard-row${meClass}"><span>${escapeHtml(username)}</span><span>${score}</span></div>`;
    }).join('') || '<div class="leaderboard-row" style="color:var(--text-dim)">No hits yet</div>';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function fireAt(targetId) {
    if (isSpectator || !stompClient || !stompClient.connected) {
        return;
    }
    // targetId may be null: a shot fired at empty space. It still counts as a fire
    // ATTEMPT on the server (Sprint 6's accuracy = hits / total attempts), it just
    // can't hit anything - HitResolver already treats a null id as a plain miss.
    stompClient.send('/app/room/' + roomCode + '/fire', {}, JSON.stringify({ targetId }));
}

// ===== Host controls =====
startMatchBtn.addEventListener('click', async () => {
    startMatchBtn.disabled = true;
    try {
        await Api.startMatch(token, roomCode);
        // No need to flip buttons here - the very next ArenaStateMessage broadcast
        // does it in handleArenaState(), which keeps "what the UI shows" driven by
        // "what the server confirmed," the same rule the game itself follows.
    } catch (err) {
        statusBanner.style.display = 'block';
        statusBanner.textContent = 'Could not start match: ' + err.message;
        startMatchBtn.disabled = false;
    }
});

stopMatchBtn.addEventListener('click', async () => {
    stopMatchBtn.disabled = true;
    try {
        await Api.stopMatch(token, roomCode);
        onMatchEnded();
    } catch (err) {
        statusBanner.style.display = 'block';
        statusBanner.textContent = 'Could not stop match: ' + err.message;
    } finally {
        stopMatchBtn.disabled = false;
    }
});

// ===========================================================================
// 2. GESTURES - MediaPipe Hands: aim reticle + edge-triggered fire (FR3/FR4/FR5)
// ===========================================================================
let latestHandLandmarks = null; // raw (unmirrored) landmarks from the most recent MediaPipe result
let reticle = null;             // {x, y} in arena coordinates, or null if no hand is visible
let lastHandSeenAt = 0;
let thumbFoldState = 'IDLE';    // 'IDLE' (thumb extended) -> 'FOLDED' (fire!) -> back to 'IDLE'

// Spectators never use the camera, so they skip loading MediaPipe entirely.
let hands = null;
if (!isSpectator) {
    hands = new Hands({
        locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`,
    });
    hands.setOptions({
        maxNumHands: 1,           // one shooter, one hand - keeps detection fast and unambiguous
        modelComplexity: 1,
        minDetectionConfidence: 0.6,
        minTrackingConfidence: 0.5,
    });
    hands.onResults(onHandResults);
}

function onHandResults(results) {
    if (!results.multiHandLandmarks || results.multiHandLandmarks.length === 0) {
        latestHandLandmarks = null;
        return; // no hand visible this frame - leave the state machine alone, just stop updating the reticle
    }

    const landmarks = results.multiHandLandmarks[0];
    latestHandLandmarks = landmarks;
    lastHandSeenAt = performance.now();

    // Landmark 8 = index fingertip -> the aim point. Landmark x/y are normalized
    // [0,1] against the camera frame; mirroring is done here (1 - x) so the
    // reticle moves the way a mirror-image "selfie view" of your own hand would
    // expect, matching the mirrored video drawn underneath it.
    const indexTip = landmarks[8];
    reticle = {
        x: (1 - indexTip.x) * ARENA_WIDTH,
        y: indexTip.y * ARENA_HEIGHT,
    };

    updateFireGesture(landmarks);
}

function updateFireGesture(landmarks) {
    const thumbTip = landmarks[4];
    const indexMcp = landmarks[5]; // the base knuckle of the index finger
    const dx = thumbTip.x - indexMcp.x;
    const dy = thumbTip.y - indexMcp.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    const isFolded = distance < THUMB_FOLD_THRESHOLD;

    // Edge-triggered on purpose (FR5: "a discrete fire event") - fire once on the
    // IDLE -> FOLDED transition, then wait for the thumb to extend again before
    // another fire can happen. Without this, holding the thumb folded for half a
    // second would fire dozens of times.
    if (isFolded && thumbFoldState === 'IDLE') {
        thumbFoldState = 'FOLDED';
        onFireGestureTriggered();
    } else if (!isFolded) {
        thumbFoldState = 'IDLE';
    }
}

function onFireGestureTriggered() {
    if (!reticle || !matchIsLive) {
        return;
    }

    // Which target (if any) is the reticle actually over? This is a CLIENT-SIDE
    // GUESS, not an authoritative hit - see this file's top comment. If nothing is
    // close enough to plausibly be what the player meant, the shot still goes out
    // (a fire attempt into empty space), it just names no target.
    const nearest = findNearestTarget(reticle);

    spawnBullet(nearest);
    fireAt(nearest ? nearest.id : null);
}

function findNearestTarget(point) {
    let nearest = null;
    let nearestDistance = FIRE_HIT_RADIUS;
    for (const target of latestTargets) {
        const dx = target.x - point.x;
        const dy = target.y - point.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < nearestDistance) {
            nearest = target;
            nearestDistance = dist;
        }
    }
    return nearest;
}

/**
 * Launches the visible bullet. It leaves from the player's WRIST (landmark 0, mirrored
 * like every other landmark) so it looks like it comes out of the hand, and flies
 * toward the claimed target - or, if there was none, straight through the reticle and
 * out past it. Purely a visual: whether it actually scores is decided by the server's
 * HitResolver, and the "+1" only shows once the server confirms (see handleHitResult).
 */
function spawnBullet(targetOrNull) {
    // Where the bullet comes from: the wrist, unless it's practically on top of the
    // reticle (hand pointing straight at the camera), in which case start just below
    // the aim point so there's still a visible flight.
    let originX = reticle.x;
    let originY = Math.min(reticle.y + 160, ARENA_HEIGHT + 20);
    if (latestHandLandmarks) {
        const wrist = latestHandLandmarks[0];
        const wx = (1 - wrist.x) * ARENA_WIDTH;
        const wy = wrist.y * ARENA_HEIGHT;
        if (Math.hypot(wx - reticle.x, wy - reticle.y) >= 80) {
            originX = wx;
            originY = wy;
        }
    }

    const now = performance.now();
    let endX, endY;
    if (targetOrNull) {
        endX = targetOrNull.x;
        endY = targetOrNull.y;
    } else {
        // A whiff: continue the line origin -> reticle well beyond the reticle.
        endX = originX + (reticle.x - originX) * 1.8;
        endY = originY + (reticle.y - originY) * 1.8;
    }

    bullets.push({
        x1: originX, y1: originY,
        x2: endX, y2: endY,
        targetId: targetOrNull ? targetOrNull.id : null,
        targetType: targetOrNull ? targetOrNull.type : null,
        createdAt: now,
        landsAt: now + BULLET_TRAVEL_MS,
    });
}

// ===== Camera lifecycle =====
let detectionLoopRunning = false;

enableCameraBtn.addEventListener('click', async () => {
    enableCameraBtn.disabled = true;
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { width: 640, height: 480 } });
        videoEl.srcObject = stream;
        await videoEl.play();
        enableCameraBtn.textContent = 'Camera enabled';
        startDetectionLoop();
    } catch (err) {
        enableCameraBtn.disabled = false;
        enableCameraBtn.textContent = 'Enable camera';
        alert('Could not access the camera: ' + err.message);
    }
});

async function startDetectionLoop() {
    if (detectionLoopRunning) {
        return;
    }
    detectionLoopRunning = true;

    // Deliberately awaits each hands.send() before requesting the next frame,
    // rather than firing requestAnimationFrame calls at full speed regardless -
    // MediaPipe processes one frame at a time, so queuing up frames it hasn't
    // gotten to yet would just make detection increasingly laggy over time.
    async function loop() {
        if (videoEl.readyState >= 2) { // HAVE_CURRENT_DATA or better
            await hands.send({ image: videoEl });
        }
        requestAnimationFrame(loop);
    }
    loop();
}

// ===========================================================================
// 3. RENDER - one requestAnimationFrame loop, independent of MediaPipe's own
//    (slower, async) detection rate, so the canvas always feels smooth.
// ===========================================================================
// Background image pan offset for a continuous right-to-left drift.
let bgPanOffset = 0;

function renderFrame() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Always draw the scrolling background (never camera feed)
    drawScrollingBackground();

    drawHandSkeleton();
    drawTargets();
    drawBullets();
    drawImpacts();
    drawReticle();
    drawFloatingTexts();

    requestAnimationFrame(renderFrame);
}

function drawScrollingBackground() {
    if (!BACKGROUND_IMAGE.complete || !BACKGROUND_IMAGE.naturalWidth) return;

    const img = BACKGROUND_IMAGE;
    const iw = img.naturalWidth;
    const ih = img.naturalHeight;
    const scale = Math.max(canvas.width / iw, canvas.height / ih);
    const visibleW = canvas.width / scale;
    const visibleH = canvas.height / scale;

    // Pan right-to-left continuously, wrapping when it reaches the end
    bgPanOffset = (bgPanOffset + 0.5) % iw;
    const startX = (iw - visibleW) / 2 + bgPanOffset;

    ctx.drawImage(img, startX, (ih - visibleH) / 2, visibleW, visibleH, 0, 0, canvas.width, canvas.height);
    // Wrap: draw the overflow part on the left side
    if (startX + visibleW > iw) {
        const overflow = startX + visibleW - iw;
        ctx.drawImage(img, 0, (ih - visibleH) / 2, overflow, visibleH, canvas.width - overflow * scale, 0, overflow * scale, canvas.height);
    }
    // Wrap: draw the underflow part on the right side
    if (startX < 0) {
        const underflow = -startX;
        ctx.drawImage(img, iw - underflow, (ih - visibleH) / 2, underflow, visibleH, 0, 0, underflow * scale, canvas.height);
    }
}

function drawCameraFeed() {
    if (!videoEl.srcObject || videoEl.readyState < 2) {
        return;
    }

    ctx.save();
    ctx.translate(canvas.width, 0);
    ctx.scale(-1, 1);
    drawVideoCover(videoEl, 0, 0, canvas.width, canvas.height);
    ctx.restore();
}

function drawImageCover(img, dx, dy, dWidth, dHeight) {
    const iw = img.naturalWidth || img.width;
    const ih = img.naturalHeight || img.height;
    if (!iw || !ih) return;
    const scale = Math.max(dWidth / iw, dHeight / ih);
    const sw = dWidth / scale, sh = dHeight / scale;
    const sx = (iw - sw) / 2, sy = (ih - sh) / 2;
    ctx.drawImage(img, sx, sy, sw, sh, dx, dy, dWidth, dHeight);
}

function drawVideoCover(video, dx, dy, dWidth, dHeight) {
    const vw = video.videoWidth, vh = video.videoHeight;
    if (!vw || !vh) return;
    const scale = Math.max(dWidth / vw, dHeight / vh);
    const sw = dWidth / scale, sh = dHeight / scale;
    const sx = (vw - sw) / 2, sy = (vh - sh) / 2;
    ctx.drawImage(video, sx, sy, sw, sh, dx, dy, dWidth, dHeight);
}

function drawHandSkeleton() {
    if (!latestHandLandmarks || typeof drawConnectors === 'undefined' || typeof HAND_CONNECTIONS === 'undefined') {
        return;
    }
    // Manually mirrored (x -> 1-x) per point, matching the reticle's own mirroring -
    // NOT a canvas transform, so it lines up with the already-mirrored video without
    // a second, easy-to-get-backwards transform stacked on top.
    const mirrored = latestHandLandmarks.map(p => ({ x: 1 - p.x, y: p.y, z: p.z }));
    drawConnectors(ctx, mirrored, HAND_CONNECTIONS, { color: 'rgba(90, 140, 255, 0.5)', lineWidth: 2 });
    drawLandmarks(ctx, mirrored, { color: 'rgba(255, 210, 91, 0.8)', radius: 2 });
}

function drawTargets() {
    for (const target of latestTargets) {
        drawTargetSprite(target.type, target.x, target.y);
    }

    // A target the server has already confirmed dead is gone from the arena state
    // within ~100ms - usually before my bullet (200ms flight) has actually reached it.
    // Keep drawing it at its last known spot until the bullet lands, so the bullet
    // visibly hits something instead of the target vanishing mid-flight.
    for (const bullet of bullets) {
        if (bullet.targetId && !latestTargets.some(t => t.id === bullet.targetId)) {
            drawTargetSprite(bullet.targetType, bullet.x2, bullet.y2);
        }
    }
}

function drawTargetSprite(type, x, y) {
    const visual = TARGET_VISUALS[type] || TARGET_VISUALS.BIRD;
    const img = SPRITE_IMAGES[visual.sprite];

    ctx.beginPath();
    ctx.arc(x, y, 26, 0, Math.PI * 2);
    ctx.fillStyle = visual.color + '33'; // soft glow behind the sprite
    ctx.fill();

    if (img && img.complete && img.naturalWidth > 0) {
        const w = 52, h = 52;
        ctx.drawImage(img, x - w / 2, y - h / 2, w, h);
    } else {
        ctx.font = '32px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(visual.emoji, x, y);
    }
}

/**
 * The bullets in flight: a glowing tracer streak with a bright head, travelling from
 * where it was fired toward its target. While the claimed target still exists in the
 * arena state the bullet homes in on its CURRENT position (targets keep moving during
 * the ~200ms flight); once the server removes it, the bullet finishes toward the last
 * position it knew. On arrival it leaves a small impact ring (see drawImpacts).
 */
function drawBullets() {
    const now = performance.now();
    const stillFlying = [];

    for (const b of bullets) {
        if (b.targetId) {
            const live = latestTargets.find(t => t.id === b.targetId);
            if (live) {
                b.x2 = live.x;
                b.y2 = live.y;
            }
        }

        const progress = (now - b.createdAt) / BULLET_TRAVEL_MS;
        if (progress >= 1) {
            impacts.push({ x: b.x2, y: b.y2, createdAt: now });
            continue; // landed - not drawn as a bullet anymore
        }
        stillFlying.push(b);

        const headX = b.x1 + (b.x2 - b.x1) * progress;
        const headY = b.y1 + (b.y2 - b.y1) * progress;
        const tailProgress = Math.max(0, progress - 0.3);
        const tailX = b.x1 + (b.x2 - b.x1) * tailProgress;
        const tailY = b.y1 + (b.y2 - b.y1) * tailProgress;

        ctx.lineCap = 'round';
        ctx.strokeStyle = 'rgba(255, 210, 91, 0.9)';
        ctx.lineWidth = 5;
        ctx.beginPath();
        ctx.moveTo(tailX, tailY);
        ctx.lineTo(headX, headY);
        ctx.stroke();

        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(headX, headY, 5, 0, Math.PI * 2);
        ctx.fill();
    }

    bullets = stillFlying;
}

function drawImpacts() {
    const now = performance.now();
    impacts = impacts.filter(i => now - i.createdAt < 250);

    for (const i of impacts) {
        const age = (now - i.createdAt) / 250; // 0 -> 1
        ctx.globalAlpha = 1 - age;
        ctx.strokeStyle = '#ffd25b';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(i.x, i.y, 8 + age * 26, 0, Math.PI * 2);
        ctx.stroke();
        ctx.globalAlpha = 1;
    }
}

function drawReticle() {
    if (!reticle || performance.now() - lastHandSeenAt > 400) {
        return; // hand's been gone too long - don't show a stale, frozen reticle
    }

    const isFiring = thumbFoldState === 'FOLDED';
    ctx.strokeStyle = isFiring ? '#ff5b6e' : '#5b8cff';
    ctx.lineWidth = 3;

    ctx.beginPath();
    ctx.arc(reticle.x, reticle.y, 18, 0, Math.PI * 2);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(reticle.x - 26, reticle.y);
    ctx.lineTo(reticle.x - 10, reticle.y);
    ctx.moveTo(reticle.x + 10, reticle.y);
    ctx.lineTo(reticle.x + 26, reticle.y);
    ctx.moveTo(reticle.x, reticle.y - 26);
    ctx.lineTo(reticle.x, reticle.y - 10);
    ctx.moveTo(reticle.x, reticle.y + 10);
    ctx.lineTo(reticle.x, reticle.y + 26);
    ctx.stroke();
}

function drawFloatingTexts() {
    const now = performance.now();
    floatingTexts = floatingTexts.filter(f => now - f.createdAt < 800);

    for (const f of floatingTexts) {
        const age = (now - f.createdAt) / 800; // 0 -> 1 over its lifetime
        ctx.globalAlpha = 1 - age;
        ctx.fillStyle = '#ffd25b';
        ctx.font = 'bold 20px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(f.text, f.x, f.y - 20 - age * 30);
        ctx.globalAlpha = 1;
    }
}

// ---------------------------------------------------------------------------
// Go!
// ---------------------------------------------------------------------------
renderLeaderboard();
connectToRoom();
renderFrame();
