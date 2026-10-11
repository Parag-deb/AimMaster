# AimMaster — Sprints 1–3

Covers:
- Sprint 1 — OOP Skeleton
- Sprint 2 — REST Foundation (Auth + Room CRUD + DB)
- Sprint 3 — Real-Time Layer (WebSocket / STOMP)

## Requirements
- Java 17+
- Maven 3.9+ (or use your IDE's built-in Maven)
- Internet access the FIRST time you build (Maven needs to download the Spring Boot /
  H2 / JWT dependencies listed in `pom.xml`)

## Run it
```bash
mvn spring-boot:run
```
The app starts on **http://localhost:8080**.

## Verify Sprint 1 (OOP skeleton)
```bash
mvn test
```
`RoomSkeletonTest` should pass — proves Player/Room/Target behave correctly with zero
Spring/DB involvement (host auto-joins, full rooms reject new players, targets move by
velocity, etc).

## Verify Sprint 2 (REST + Auth + DB)
Register a player:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```
You'll get back `{ "token": "...", "username": "alice" }`. Copy the token.

Create a room (paste the token from above):
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PASTE_TOKEN_HERE>" \
  -d '{"difficulty":"MEDIUM","durationSeconds":120,"maxPlayers":4}'
```
You'll get back a `code` (e.g. `"code":"7F3KQZ"`). Register a second player and join:
```bash
curl -X POST http://localhost:8080/api/rooms/7F3KQZ/join \
  -H "Authorization: Bearer <SECOND_PLAYER_TOKEN>"
```
Check the data actually landed in the database at **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:mem:aimmaster`, user `sa`, blank password) — you should see rows in
`PLAYERS`, `ROOMS`, and `ROOM_PLAYERS`.

Also verify the error handling pitfalls: try joining a room code that doesn't exist —
you should get a clean `404` JSON body, not a stack trace.

## Verify Sprint 3 (WebSocket / STOMP)
1. With the app running, open **http://localhost:8080/test-client.html** in two
   separate browser tabs.
2. In each tab, paste the SAME JWT (from the login/register step above) and the SAME
   room code, click **Connect** in both.
3. In one tab, type a message and click **Send test message**.
4. **It should appear in both tabs** — that's the Sprint 3 Definition of Done: two+
   clients subscribed to the same room topic both receive the same live broadcast.
5. Open a THIRD tab with a *different* room code and confirm it does NOT receive
   messages sent to the first room — proves broadcasts are correctly isolated per room.

## Verify Sprint 4 (Game Loop & Concurrency)
1. As the room's host, start the match:
   ```bash
   curl -X POST http://localhost:8080/api/rooms/7F3KQZ/start \
     -H "Authorization: Bearer <HOST_TOKEN>"
   ```
2. In both connected test-client tabs, the **Arena** box should start updating about
   10 times a second, showing a growing/shrinking list of target ids and an
   increasing elapsed-time counter — both tabs should show the *same* targets at the
   *same* elapsed time (proves the broadcast is shared, not per-connection).
3. Create and start a **second** room (different code) in a third tab. It should
   tick independently — a different, unrelated set of targets, its own elapsed timer.
   Stopping the first room (`POST /api/rooms/7F3KQZ/stop`, host only) should freeze
   only that room's Arena box; the second room keeps ticking — proves isolation
   (each room really does have its own thread, not a shared one).
4. Try starting the same room twice in a row — the second call should cleanly fail
   with `409 Conflict` ("A match is already running..."), not crash anything.

## Verify Sprint 5 (Hit Resolution & Leaderboard)
1. With a match running and both test-client tabs connected (see Sprint 4 above),
   copy a target id shown in the Arena box into the "Target id to fire at" field in
   **both** tabs.
2. Click **Fire!** in both tabs as close together in time as you can manage by hand.
   Only one tab should log a green `HIT` line; the other's shot simply does nothing
   (it was a fraction of a second too late — the target was already dead). The score
   in the hit line should go up by exactly 1, never 2.
3. For a real, automated proof instead of a manual race with your mouse, run:
   ```bash
   mvn -Dtest=HitResolverConcurrencyTest test
   ```
   This test fires two threads at the exact same target at the same instant, 100% of
   the time — no clicking-fast-enough luck required — and asserts exactly one hit and
   exactly one point awarded.
4. Try firing at a target id that doesn't exist (typo it) — nothing should happen and
   nothing should break; that's the intended "miss" behavior.

## Verify Sprint 6 (Persistence — Match History & Stats)
1. Let a match you started in Sprint 4/5's steps run out (or stop it early as host):
   ```bash
   curl -X POST http://localhost:8080/api/rooms/7F3KQZ/stop \
     -H "Authorization: Bearer <HOST_TOKEN>"
   ```
2. Check the H2 console (http://localhost:8080/h2-console) — a `MATCH_RECORDS` table
   should now have one row per player who was in that room, even a player who never
   fired a shot (score 0, accuracy 0).
3. Fetch your own match history (a player can only view their own):
   ```bash
   curl http://localhost:8080/api/players/alice/history \
     -H "Authorization: Bearer <ALICE_TOKEN>"
   ```
   You should see that match listed, with the score/accuracy you'd expect from how
   many times you fired vs. how many hits landed. Try fetching `bob`'s history using
   `alice`'s token — you should get a clean `403 Forbidden`, not someone else's data.
4. Fetch the room's historical leaderboard snapshot:
   ```bash
   curl http://localhost:8080/api/rooms/7F3KQZ/leaderboard \
     -H "Authorization: Bearer <ALICE_TOKEN>"
   ```
   This reads from the database (past results), not the live in-memory scoreboard —
   it'll keep returning the same numbers even long after the match has ended.
5. Start and finish a SECOND match in the same room, then fetch the leaderboard again
   — you should now see accumulated results across both matches for anyone who
   played in both.

## Verify Sprint 7 (Frontend Gesture Integration)
This is the real frontend now — no more Postman/curl needed for normal play.

1. Open **http://localhost:8080/index.html** in two different browsers (or one
   normal + one incognito window, so they don't share sessionStorage).
2. In window 1: register/log in as `alice`, create a room (note the difficulty and
   room code shown), which takes you straight to the game screen.
3. In window 2: register/log in as `bob`, join `alice`'s room by code.
4. In window 1 (the host), click **Enable camera**, allow the permission prompt,
   then click **Start match**. Targets (🐦🎈🍎) should start moving across BOTH
   windows' canvases in sync.
5. In window 2, click **Enable camera** too. Point your index finger at the
   screen — a blue crosshair should track your fingertip. Fold your thumb toward
   your palm — the crosshair flashes red and, if it was over a target, that
   target should vanish and a "+1" pop should appear, with the leaderboard and
   your score updating **in both windows**.
6. If firing feels too sensitive or unresponsive, adjust `THUMB_FOLD_THRESHOLD`
   near the top of `static/js/game.js` (smaller = requires a tighter fold).
7. As the host, click **Stop match** — both windows should show "Match ended,"
   and `GET /api/players/alice/history` should now include that match.

If you'd rather not fold your thumb in front of a camera repeatedly while
testing, `test-client.html` (Sprint 3-5) still works for firing at a known
target id without any gesture involved — useful for quick backend-only checks.

## Verify Sprint 8 (Full End-to-End Integration)
No mocks needed replacing here — Sprint 7's frontend was already built directly
against the real backend. This sprint's real work is FR16 (disconnect handling)
and a related correctness fix for players who join mid-match.

**Disconnect while others remain:**
1. Get `alice` (host) and `bob` both connected and in a match, as in the Sprint 7
   steps above.
2. Just close `bob`'s browser tab entirely (don't click anything — simulate a real
   drop) while the match is still running.
3. In `alice`'s window, within a couple of seconds you should see a brief
   "bob left the room" banner — and the match keeps running and broadcasting
   normally afterward. Nothing should error or freeze on the server side (watch
   the console log — you'll see a line like `bob disconnected from room ABC123 -
   0 player(s) remain` only if someone is still counted as present; with one
   player left it'll instead say the room emptied, if alice also leaves next).

**Room empties out entirely mid-match:**
4. With only `alice` left in the room (from the step above), close her tab too
   while the match is still running.
5. Check the server console log — it should print a line like `Room ABC123
   emptied out mid-match (last player alice disconnected) - stopping its match`.
   That match's dedicated thread is now released (no leak), and a `MatchRecord`
   should already be in the database for whatever score `alice`/`bob` had at
   that point (check `GET /api/players/alice/history`, or the H2 console).

**Late joiner gets counted correctly:**
6. Create a new room as `alice`, start the match WITHOUT `bob` having joined yet.
7. Join as `bob` a few seconds into the running match, enable his camera, and
   score at least one hit.
8. End the match, then check `GET /api/players/bob/history` — the match should
   be there with `bob`'s real score, even though he wasn't in the room when it
   started.

**Full smoke test:**
9. Repeat the whole flow — register, create/join, start, gesture-fire from
   2-3 separate browser sessions at once, stop, check history for everyone —
   once start to finish without restarting the server, to confirm nothing
   about the full pipeline (auth → room → WebSocket → game loop → hit
   resolution → leaderboard → persistence) breaks under normal concurrent use.

## Verify Sprint 9 (Spectator mode, Reconnect handling, Admin dashboard, Bullets)

**Bullets (fix):** With a match running and your camera on, fold your thumb to fire.
A glowing bullet now flies from your **wrist** toward the target it's aimed at
(homing on it while it moves), and a small ring pops where it lands. The "+1" and the
score/leaderboard change appear **when the bullet lands**, not before. Firing at empty
space sends a bullet flying through your crosshair and off past it — and now also counts
as a fire *attempt*, so the accuracy stat in match history is honest.

**Spectator mode:**
1. Player A creates a room and starts a match. Note the room code.
2. In a third browser/incognito window, log in as any other user, type the room code
   and click **Spectate** (not Join).
3. You should see the arena, live targets, and the leaderboard updating, with a
   "👁 Spectating" badge, and no camera/Start/Stop buttons.
4. Check the room's player list (`GET /api/rooms/{code}` or the H2 `ROOM_PLAYERS`
   table): the spectator is **not** in it, and a full room can still be spectated.
5. Close the spectator's tab — the match keeps running and nobody sees a "left" notice.
   (Spectators never affect match lifecycle; only *players* leaving can empty a room.)

**Reconnect handling:**
1. Join a running match as a player (or spectator).
2. Simulate a drop: in Chrome DevTools → Network tab, tick **Offline** for ~10 seconds
   (or stop and restart your Wi-Fi briefly).
3. The banner shows "Connection lost — reconnecting…" and retries every 3 seconds.
4. Untick Offline. It reconnects on its own, and the targets, elapsed time, **and the
   leaderboard/your score** repaint immediately from the server — no page reload.
5. Also try simply refreshing the page mid-match: same instant catch-up. (Refreshing keeps
   your login because it lives in `sessionStorage` for that tab.)
6. If the match ended while you were away, you'll see "Match ended" instead.

**Admin dashboard:**
1. Register a user named exactly `admin` (the default allowlist entry in
   `application.properties` → `aimmaster.admin.usernames`).
2. While logged in as `admin` in that browser tab, open **http://localhost:8080/admin.html**.
3. You'll see running matches, per-room status/elapsed/targets/players/spectators, and a
   JVM thread count. Start a match in a second window: the running-match count goes up
   by one and the thread count rises by roughly one; stop it and both fall back — a
   live illustration of "one thread per room".
4. Open the same page as a normal user: the backend returns 403 ("Admin access
   required") and the page shows that message.
   `curl http://localhost:8080/api/admin/stats -H "Authorization: Bearer <TOKEN>"` works too.

**Known limitation (by design of this sprint's scope):** the server does not check that
a fire event's sender is actually a player rather than a spectator, so a hand-crafted
WebSocket message could still score. The UI never lets spectators fire, but real
enforcement is the "rate-limiting / anti-cheat" advanced feature, which wasn't selected.

## Package structure (locked, do not reorganize in later sprints)
```
com.aimmaster
├── config        WebSocket config, Security config, JWT filter/handshake
├── controller    REST controllers
├── websocket     STOMP message-handling controllers
├── service       business logic (Auth, Jwt, Room)
├── repository    Spring Data JPA repositories
├── entity        JPA entities (Player, Room)
├── dto           request/response DTOs
├── model         plain domain objects not backed by DB (Target, Difficulty)
└── exception     custom exceptions + global handler
```
