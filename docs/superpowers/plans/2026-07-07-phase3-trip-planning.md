# Phase 3 Trip Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an end-to-end intelligent trip planning workflow with async progress, structured plan generation, persistence, detail/copy/delete/chat APIs, and Vue pages.

**Architecture:** Keep `TripTaskService` as the async orchestration point. Add a deterministic MySQL-backed demo planner for local runs, preserve the existing real Agent path, and persist every valid `TripPlanResponse` into `tm_trip_plan`, `tm_trip_day`, and `tm_trip_item`.

**Tech Stack:** Java 17, Spring Boot 3, JDBC `NamedParameterJdbcTemplate`, MySQL 8, Vue 3 + Vite, Vitest.

---

### Task 1: Backend Trip Planning Core

**Files:**
- Create: `modules/trip/src/main/java/com/zkry/trip/service/TripPlanReviewer.java`
- Create: `modules/trip/src/main/java/com/zkry/trip/service/DemoTripPlannerService.java`
- Modify: `modules/trip/src/main/java/com/zkry/trip/service/TripAiPlannerService.java`
- Modify: `modules/trip/src/main/java/com/zkry/trip/service/TripTaskService.java`
- Test: `modules/trip/src/test/java/com/zkry/trip/service/TripPlanReviewerTest.java`
- Test: `modules/trip/src/test/java/com/zkry/trip/service/DemoTripPlannerServiceTest.java`

- [ ] **Step 1: Write failing reviewer tests**

```java
@Test
void acceptsCompletePlan() {
    TripPlanReviewer reviewer = new TripPlanReviewer();
    TripPlan plan = TestTripPlans.completePlan();
    assertThat(reviewer.review(plan, TestTripPlans.request()).passed()).isTrue();
}
```

Run: `mvn -pl modules/trip test -Dtest=TripPlanReviewerTest`  
Expected: fail because `TripPlanReviewer` does not exist.

- [ ] **Step 2: Implement reviewer**

Create `TripPlanReviewer` with `review(TripPlan, TripRequest)` returning `ReviewOutcome(boolean passed, List<String> issues)`.

- [ ] **Step 3: Write failing demo planner test**

```java
@Test
void createsPlanFromSeedResources() {
    DemoTripPlannerService planner = new DemoTripPlannerService(jdbcTemplate);
    TripPlanResponse response = planner.plan("demo-1", request());
    assertThat(response.data().days()).hasSize(2);
}
```

Run: `mvn -pl modules/trip test -Dtest=DemoTripPlannerServiceTest`  
Expected: fail because `DemoTripPlannerService` does not exist.

- [ ] **Step 4: Implement demo planner**

Query `tm_city`, `tm_attraction`, `tm_hotel`, `tm_restaurant`, and `tm_travel_note`; build daily attractions, meals, hotel, weather tips, and budget.

- [ ] **Step 5: Integrate task fallback**

In `TripTaskService`, use real Agent path only when runtime settings are present; otherwise call demo planner. Always review output before completion.

### Task 2: Persistence And APIs

**Files:**
- Create: `modules/trip/src/main/java/com/zkry/trip/service/TripPlanPersistenceService.java`
- Create: `modules/trip/src/main/java/com/zkry/trip/service/TripChatService.java`
- Modify: `app/src/main/java/com/zkry/api/trip/TripController.java`
- Modify: `modules/resources/src/main/java/com/zkry/resources/service/TripHistoryPersistenceService.java`
- Test: `modules/trip/src/test/java/com/zkry/trip/service/TripPlanPersistenceServiceTest.java`

- [ ] **Step 1: Write failing persistence tests**

```java
@Test
void savesLoadsCopiesAndDeletesPlan() {
    long id = service.save(1001L, response, request());
    assertThat(service.detail(id).data().city()).isEqualTo("Hangzhou");
    long copied = service.copy(id, 1001L);
    assertThat(copied).isNotEqualTo(id);
    service.delete(id);
    assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ResponseStatusException.class);
}
```

Run: `mvn -pl modules/trip test -Dtest=TripPlanPersistenceServiceTest`  
Expected: fail because persistence service does not exist.

- [ ] **Step 2: Implement persistence**

Write plan JSON to `tm_trip_plan.raw_plan_json`, days to `tm_trip_day`, and items to `tm_trip_item`. Use soft delete for delete.

- [ ] **Step 3: Add APIs**

Expose `GET /api/trip/{id}`, `POST /api/trip/{id}/copy`, `DELETE /api/trip/{id}`, `POST /api/trip/{id}/chat`.

- [ ] **Step 4: Update history**

Ensure `/api/trip/history` returns saved plans ordered by update time with ids usable by detail links.

### Task 3: Vue Planning Workflow

**Files:**
- Create: `frontend/src/api/trip.js`
- Create: `frontend/src/views/TripDetailView.vue`
- Modify: `frontend/src/views/PlanningView.vue`
- Modify: `frontend/src/views/TripHistoryView.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/layout/menu.js`
- Test: `frontend/src/api/trip.test.js`
- Test: `frontend/src/app-shell.test.js`

- [ ] **Step 1: Write API client tests**

```js
await api.submitPlan(payload);
await api.status('task1');
await api.detail(9001);
await api.copy(9001);
await api.remove(9001);
await api.chat(9001, '优化一下');
```

Run: `npm test`  
Expected: fail until `frontend/src/api/trip.js` exists.

- [ ] **Step 2: Implement API client**

Add methods for plan submit, status polling, detail, copy, delete, chat, and history.

- [ ] **Step 3: Build pages**

Planning page submits requests and polls status. Detail page renders days, budget, graph counts, copy/delete/chat actions. History page links to detail.

### Task 4: Verification And Todo

**Files:**
- Modify: `docs/project-todo-workflow.md`

- [ ] **Step 1: Run verification**

Run:

```powershell
mvn test
mvn -pl app -am package -DskipTests
cd frontend; npm test; npm run build
```

- [ ] **Step 2: Run smoke**

With Docker MySQL/Redis running, start backend jar, submit a demo plan, poll to completed, open detail, copy, chat, delete copied plan, and verify history.

- [ ] **Step 3: Update checklist**

Mark Phase 3 TODO and Done Criteria only after smoke evidence proves each item.
