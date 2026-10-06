# AimMaster — Test Cases (Sprint 1 to Sprint 8)

## Overview

| Sprint | Test Files | Total Tests | Status |
|--------|------------|-------------|--------|
| Sprint 1 | TargetTest, RoomTest | 11 | ✅ All pass |
| Sprint 2 | PlayerTest (added), RoomTest, TargetTest | 12 | ✅ All pass |
| Sprint 3 | PlayerTest, RoomTest, TargetTest | 12 | ✅ All pass |
| Sprint 4 | PlayerTest, RoomTest, TargetTest | 12 | ✅ All pass |
| Sprint 5 | HitResolverTest, LeaderboardTest + previous | 23 | ✅ All pass |
| Sprint 6 | MatchRecordTest, GameSessionTrackingTest + previous | 27 | ✅ All pass |
| Sprint 7 | Manual verification only | — | ✅ Verified |
| Sprint 8 | Manual verification only | — | ✅ Verified |

**Grand Total: 27 unit tests** — all passing (`mvn clean test` → BUILD SUCCESS)

---

## Sprint 1 — OOP Skeleton (No Framework)

### TargetTest (`com.aimmaster.model`)

| Test Method | Description |
|-------------|-------------|
| `targetStartsAliveAtGivenPosition()` | Verifies a newly created `Target` is alive, has correct ID, type, position, and velocity |
| `moveUpdatesPositionByVelocity()` | Verifies `move()` shifts position by velocity (e.g., balloon moves up) |
| `hitMarksTargetDead()` | Verifies `hit()` sets `alive = false` |
| `moveDoesNothingAfterHit()` | Verifies `move()` is no-op after target is hit (position unchanged) |

### RoomTest (`com.aimmaster.entity`)

| Test Method | Description |
|-------------|-------------|
| `roomContainsHostOnCreation()` | Room created with host inside; code, settings, empty targets list verified |
| `addPlayerIncreasesPlayerCount()` | Adding a player increases count; player added to list |
| `addPlayerFailsWhenRoomIsFull()` | Adding beyond `maxPlayers` returns `false`, count unchanged |
| `addPlayerFailsWhenAlreadyInRoom()` | Adding same player twice returns `false`, count unchanged |
| `removePlayerDecreasesCount()` | Removing a player decreases count |
| `addTargetIncreasesTargetCount()` | Adding target increases room's target list |
| `removeTargetDecreasesTargetCount()` | Removing target empties the list |

---

## Sprint 2 — REST Foundation (Auth + Room CRUD + DB)

### PlayerTest (`com.aimmaster.entity`) — *New in Sprint 2*

| Test Method | Description |
|-------------|-------------|
| `playerCreatedWithUsernameAndPassword()` | JPA entity constructs with username/password; ID is null until persisted |

### RoomTest & TargetTest — *Unchanged from Sprint 1*

All 11 existing tests continue to pass. No Spring/DB/WebSocket dependencies introduced.

---

## Sprint 3 — Real-Time Layer (WebSocket / STOMP)

### PlayerTest, RoomTest, TargetTest — *Unchanged*

All 12 tests pass. WebSocket config and controllers are integration-tested manually via `ws-test.html` (not unit tests).

---

## Sprint 4 — Game Loop & Concurrency

### PlayerTest, RoomTest, TargetTest — *Unchanged*

All 12 tests pass. Game loop and concurrency verified manually via multi-room test runs (see history.md).

---

## Sprint 5 — Hit Resolution & Leaderboard

### HitResolverTest (`com.aimmaster.service`) — *New in Sprint 5*

| Test Method | Description |
|-------------|-------------|
| `singleHit_returnsTrue()` | Single hit on alive target returns `true`, marks target dead |
| `secondHitOnSameTarget_returnsFalse()` | Second hit on same target returns `false` (already dead) |
| `hitOnNullTarget_returnsFalse()` | Passing `null` target returns `false` (safe guard) |
| `concurrentHitsOnSameTarget_onlyOneScores()` | **Concurrency test:** 10 threads simultaneously hit same target → exactly 1 succeeds, target dead. Proves thread-safe atomic check-and-mark. |

### LeaderboardTest (`com.aimmaster.service`) — *New in Sprint 5*

| Test Method | Description |
|-------------|-------------|
| `registerPlayer_initializesScoreToZero()` | Newly registered player starts with score 0 |
| `addScore_incrementsCorrectly()` | `addScore(roomId, playerId, 10)` returns 10, `getScore` returns 10 |
| `multipleAddScore_accumulates()` | Multiple `addScore` calls accumulate (10 + 10 = 20) |
| `getStandings_sortsByScoreDescending()` | Standings sorted highest-first (30 pts before 10 pts) |
| `unregisterPlayer_removesFromStandings()` | Unregistered player disappears from standings |
| `clearRoom_removesAllData()` | `clearRoom` empties standings and resets scores |
| `concurrentScoreAdditions_areThreadSafe()` | **Concurrency test:** 100 threads × 10 increments each → final score exactly 1000. Proves `AtomicInteger` thread-safety. |

---

## Test Execution

```bash
cd E:\AimMaster\aimmaster
# Windows PowerShell
$env:PATH = "C:\tools\maven\apache-maven-3.9.6\bin;" + $env:PATH
mvn clean test
```

### Expected Output (Sprint 6)

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Breakdown by Class

| Test Class | Tests | Sprint Added |
|------------|-------|--------------|
| PlayerTest | 1 | 2 |
| RoomTest | 7 | 1 |
| TargetTest | 4 | 1 |
| HitResolverTest | 4 | 5 |
| LeaderboardTest | 7 | 5 |
| MatchRecordTest | 2 | 6 |
| GameSessionTrackingTest | 2 | 6 |
| **Total** | **27** | — |

---

## Concurrency Test Details (Critical for Correctness)

### HitResolver — 10-Thread Race Condition Test

```java
// 10 shooters, 1 target, all fire simultaneously
ExecutorService executor = Executors.newFixedThreadPool(10);
CountDownLatch latch = new CountDownLatch(10);
AtomicInteger successCount = new AtomicInteger(0);

for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        if (hitResolver.resolveHit(target, shooterId)) {
            successCount.incrementAndGet();
        }
        latch.countDown();
    });
}
latch.await();
assertEquals(1, successCount.get()); // ONLY ONE scores
```

**Why this matters:** Without `synchronized(target)` in `HitResolver.resolveHit()`, a read-check-write race would allow multiple threads to see `isAlive()==true` and all call `hit()`, causing double-scoring.

### Leaderboard — 1000-Concurrent-Increment Test

```java
// 100 threads, each adds 1 point 10 times = 1000 total
int numThreads = 100, incrementsPerThread = 10;
for (int i = 0; i < numThreads; i++) {
    executor.submit(() -> {
        for (int j = 0; j < incrementsPerThread; j++) {
            leaderboard.addScore(1, 1, 1);
        }
    });
}
assertEquals(1000, leaderboard.getScore(1, 1));
```

**Why this matters:** Uses `AtomicInteger` for score values inside `ConcurrentHashMap`. Without atomic increments, lost updates would occur under contention.

---

## Running Individual Test Classes

```bash
# Run only Sprint 5 tests
mvn test -Dtest=HitResolverTest,LeaderboardTest

# Run only model/entity tests
mvn test -Dtest=TargetTest,RoomTest,PlayerTest
```

---

## Sprint 6 — Persistence (Match History & Stats)

### MatchRecordTest (`com.aimmaster.entity`) — *New in Sprint 6*

| Test Method | Description |
|-------------|-------------|
| `matchRecordCreatedWithAllFields()` | Verifies entity constructs with all fields (playerId, roomId, score, accuracy, shotsFired, hits, timestamp); ID is null until persisted |
| `matchRecordNoArgsConstructorExists()` | Verifies JPA-required no-arg constructor exists |

### GameSessionTrackingTest (`com.aimmaster.service`) — *New in Sprint 6*

| Test Method | Description |
|-------------|-------------|
| `shotsFiredIncrementsOnEachHandleHit()` | Verifies shot/hit tracking logic: shots accumulate, hits accumulate, accuracy = hits/shots |
| `playerWithNoShotsHasZeroAccuracy()` | Verifies player with zero shots has 0.0 accuracy (no division by zero) |

---

## Test Execution

```bash
cd E:\AimMaster\aimmaster
# Windows PowerShell
$env:PATH = "C:\tools\maven\apache-maven-3.9.6\bin;" + $env:PATH
mvn clean test
```

### Expected Output (Sprint 6)

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Breakdown by Class

| Test Class | Tests | Sprint Added |
|------------|-------|--------------|
| PlayerTest | 1 | 2 |
| RoomTest | 7 | 1 |
| TargetTest | 4 | 1 |
| HitResolverTest | 4 | 5 |
| LeaderboardTest | 7 | 5 |
| MatchRecordTest | 2 | 6 |
| GameSessionTrackingTest | 2 | 6 |
| **Total** | **27** | — |

---

# Run only model/entity tests
mvn test -Dtest=TargetTest,RoomTest,PlayerTest
```

---

## Notes

- All tests are **unit tests** — no Spring context, no DB, no WebSocket server required.
- Tests use JUnit 5 (Jupiter).
- Concurrency tests use `ExecutorService`, `CountDownLatch`, `AtomicInteger` — standard `java.util.concurrent`.
- Player ID is set via reflection in `LeaderboardTest` because JPA `@Id` field is protected (no setter).
- Integration tests (REST endpoints, WebSocket round-trips) are manual via `curl`, Postman, or `GameClient.html` — not automated in Maven test phase.

---

## Sprint 7 — Frontend Gesture Integration (Manual Verification)

All Sprint 7 verification is done via manual browser testing (no unit tests).

### Prerequisites

- Application running (`mvn clean spring-boot:run` or built JAR)
- Browser with webcam access (Chrome recommended)
- Another browser tab or device for second player (multiplayer testing)

### Test Cases

#### TC7.1: MediaPipe Hands Loads

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open `GameClient.html` | MediaPipe Hands library loads (no console errors) |
| 2 | Click "Allow" on webcam permission | Camera feed visible behind canvas |
| 3 | Move hand in front of camera | Hand landmarks detected (green lines, red dots on hand-canvas overlay) |

#### TC7.2: Reticle Follows Index Finger

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open GameClient and log in | Canvas renders arena border |
| 2 | Move index finger horizontally | Reticle circle follows finger position on Canvas |
| 3 | Move index finger vertically | Reticle tracks vertical movement |

#### TC7.3: Thumb-Fold Fire Event Sent

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Position index finger near a target | Target within reticle range |
| 2 | Fold thumb (close fist with index pointing) | STOMP SEND to `/app/room/{roomId}/fire` visible in Network tab |
| 3 | Check WebSocket frames | Frame contains `{ targetId }` with nearest target ID |
| 4 | Open Network → WS tab | Fire messages visible with correct destination |

#### TC7.4: Fire Debounce (500ms)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fold thumb rapidly 5 times in 1 second | Only 1-2 fire events sent (not 5) |
| 2 | Check timestamps | Minimum 500ms between consecutive fire events |

#### TC7.5: Canvas Target Rendering

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Start a match (create or join room) | Arena renders with targets |
| 2 | Verify bird targets | Rendered as red ellipses |
| 3 | Verify balloon targets | Rendered as blue circles with strings |
| 4 | Verify fruit targets | Rendered as green circles |

#### TC7.6: Arena State Broadcast Received

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create room and start match | `ArenaStateBroadcast` received via STOMP subscription |
| 2 | Check target positions on Canvas | Targets appear at server-specified positions |
| 3 | Observe during match | Targets update positions on each broadcast |

#### TC7.7: Leaderboard Display

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Play a match and score hits | Leaderboard panel updates |
| 2 | Check leaderboard ordering | Players sorted by score (descending) |
| 3 | Check leader entry | Gold color indicator for top player |

#### TC7.8: Server-Authoritative Hit Resolution

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fire at a visible target | `FireBroadcast` received with `hit=true` |
| 2 | Fire at empty area (no target) | `FireBroadcast` received with `hit=false` (server rejected) |
| 3 | Verify local hit count | Only incremented for confirmed hits from server |

#### TC7.9: Match Timer

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Start a match | Timer starts in top bar |
| 2 | Observe countdown | Timer decrements (30s) |
| 3 | Time expires | Post-match summary screen appears |

#### TC7.10: Login / Register Flow

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter new username/password and click Register | Success message, login panel shown |
| 2 | Enter credentials and click Login | JWT stored, lobby panel shown |
| 3 | Try duplicate registration | Error message shown |

#### TC7.11: Room Join / Create Flow

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter existing room code and click Join | Success, WebSocket connects to room |
| 2 | Create new room | Room code displayed, WebSocket connects |
| 3 | Join with invalid code | Error message shown |

#### TC7.12: Multiplayer (Two Clients)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open GameClient in two browsers (two players) | Both connected to same room |
| 2 | Player A fires at target | Player B sees hit result via broadcast |
| 3 | Player B fires at target | Player A sees hit result via broadcast |
| 4 | Check leaderboard on both | Both show updated scores |

### Manual Verification Checklist

- [ ] TC7.1 — MediaPipe loads
- [ ] TC7.2 — Reticle follows finger
- [ ] TC7.3 — Fire event sent via WebSocket
- [ ] TC7.4 — Fire debounce works
- [ ] TC7.5 — All target types render
- [ ] TC7.6 — Arena state received
- [ ] TC7.7 — Leaderboard updates
- [ ] TC7.8 — Server validates hits
- [ ] TC7.9 — Timer runs
- [ ] TC7.10 — Auth flow works
- [ ] TC7.11 — Room flow works
- [ ] TC7.12 — Multiplayer syncs

---

## Sprint 8 — Full End-to-End Integration (Manual Verification)

All Sprint 8 verification is done via manual browser testing and backend inspection (no unit tests).

### Prerequisites

- Application running (`mvn clean spring-boot:run` or built JAR)
- Browser with webcam access (Chrome recommended)
- At least 2 browser tabs/devices for multiplayer testing

### Tasks Verified

#### Task 1: Mock → Real Backend Replacement

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open GameClient.html | All API calls go to real backend (no mock data paths) |
| 2 | Login with valid credentials | `POST /api/auth/login` returns real JWT |
| 3 | Create/join room | `POST /api/rooms` or `/api/rooms/{code}/join` returns real room data |
| 4 | Observe WebSocket broadcasts | `ArenaStateBroadcast`, `LeaderboardBroadcast`, `FireBroadcast` from server |
| 5 | Check Network tab | All XHR and WS frames point to `/api/*` or `/ws` — no mock URLs |

#### Task 2: Full Match Flow Live

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Player A logs in | JWT stored, lobby shown |
| 2 | Player A creates room | Room code shown, WebSocket connected |
| 3 | Player A clicks "Start Match" (host only) | Match starts, timer begins, targets appear |
| 4 | Player B joins same room (second tab) | Sees same targets, same leaderboard |
| 5 | Player A gestures → fires | Server resolves hit, broadcasts FireBroadcast with hit result |
| 6 | Observe leaderboard | Updates live after each hit |
| 7 | Wait for match end (duration expires) | Post-match panel appears |
| 8 | Check match summary | Shows score from server FireBroadcast |
| 9 | Check Match History section | Shows persisted MatchRecords from `GET /api/players/{id}/history` |

#### Task 3: Disconnect Handling (FR16)

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Player A and B in same room, match running | Both see targets and leaderboard |
| 2 | Close Player B's browser tab | WebSocket disconnects |
| 3 | Check server logs | "Player X disconnected from rooms {Y}" logged |
| 4 | Check Player A experience | Match continues without crash, leaderboard updated |
| 5 | Player B reconnects (reopen tab, login, rejoin) | Can rejoin room, new session tracked |
| 6 | Check room empty scenario | When last player disconnects, match stops, records persisted |

#### Task 4: Smoke Test — 2+ Concurrent Players

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open 2 browser tabs, login as different players | Both authenticated |
| 2 | Both join same room | Both connected to `/topic/room/{roomId}` |
| 3 | Host starts match | Both see same arena state |
| 4 | Both fire at targets simultaneously | Each hit validated server-side, no double-scoring |
| 5 | One player disconnects mid-match | Other player continues, no crash |
| 6 | Match ends | Both see post-match panel with history |

### Post-Match History Verification

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Complete a match | MatchRecord saved to H2 DB |
| 2 | Navigate to post-match panel | Match History section shows records |
| 3 | Verify record fields | Score, hits, shots, accuracy, timestamp all from server |
| 4 | Start another match | History shows both matches (ordered by timestamp desc) |

### Manual Verification Checklist

- [ ] All frontend calls use real backend APIs (no mock paths)
- [ ] Full match flow works end-to-end
- [ ] Disconnect doesn't crash room for other players
- [ ] Match history persisted and visible after match end
- [ ] 2+ concurrent players can play together

---

## Sprint 9 — Advanced Features (Rate Limiting + Reconnect Recovery)

### FireRateLimiterTest (`com.aimmaster.service`) — *New in Sprint 9*

| Test Method | Description |
|-------------|-------------|
| `firstFire_isAllowed()` | First fire in cooldown window returns `true` |
| `immediateSecondFire_isRejected()` | Second fire within 300ms returns `false` |
| `fireAfterCooldown_isAllowed()` | Fire after cooldown expires returns `true` |
| `differentPlayers_areIndependent()` | Two players in same room tracked independently |
| `differentRooms_areIndependent()` | Two rooms tracked independently |
| `concurrentFires_onlyOneAllowedPerWindow()` | 10 threads on same room/player → exactly 1 allowed |
| `clearRoom_removesState()` | `clearRoom()` resets that room's rate-limit state |
| `clearAll_removesAllState()` | `clearAll()` resets every room |
| `negativeInterval_throwsException()` | Negative cooldown throws `IllegalArgumentException` |
| `zeroInterval_allowsEveryFire()` | Zero cooldown means no limit |

### Manual Verification — Rate Limiting

| Test Case | Steps | Expected Result |
|-----------|-------|-----------------|
| TC9.1: Rapid fire rejected | Start match, fire 5 times in under 1 second | Only first fire processed; subsequent fires show "Fire rate limited — slow down" in lobby status |
| TC9.2: Rate limit clears after cooldown | Fire rapidly, wait 300ms, fire again | Second fire after cooldown is accepted and processed |
| TC9.3: Independent per-player | Two players in same room, Player A fires rapidly | Player A rate-limited; Player B can still fire normally |

### Manual Verification — Reconnect Recovery

| Test Case | Steps | Expected Result |
|-----------|-------|-----------------|
| TC9.4: Disconnect and auto-rejoin | Start match, close browser tab, reopen and login | Client automatically re-joins room via REST, reconnects WS, resumes seeing arena state |
| TC9.5: Reconnect restores game state | Disconnect mid-match, reconnect after 3s | Targets, leaderboard, and timer visible again after reconnect |
| TC9.6: Error messages on reconnect failure | Disconnect, block `/api/rooms/{code}/join` temporarily | Frontend shows "Rejoin failed" and retries automatically |

### Test Execution

```bash
cd E:\AimMaster\aimmaster
$env:PATH = "C:\tools\maven\apache-maven-3.9.6\bin;" + $env:PATH
mvn clean test
```

### Expected Output (Sprint 9)

```
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Breakdown by Class

| Test Class | Tests | Sprint Added |
|------------|-------|--------------|
| PlayerTest | 1 | 2 |
| RoomTest | 7 | 1 |
| TargetTest | 4 | 1 |
| HitResolverTest | 4 | 5 |
| LeaderboardTest | 7 | 5 |
| MatchRecordTest | 2 | 6 |
| GameSessionTrackingTest | 2 | 6 |
| FireRateLimiterTest | 10 | 9 |
| **Total** | **37** | — |