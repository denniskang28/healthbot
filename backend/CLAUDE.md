# Backend Engineering Guide

Spring Boot 3 / Java 17 / H2 (in-memory) / Spring Data JPA / WebSocket (STOMP).

## Layer structure

```
controller/   HTTP entry points, input validation, DTO mapping
service/      Business logic (LlmProxyService, SessionService)
repository/   Spring Data JPA interfaces — one per entity
model/        JPA entities (no suffix)
dto/          Request/response shapes sent over HTTP (*Dto.java)
config/       Spring config beans + DataInitializer (seed data)
websocket/    UserStatusPublisher — broadcasts session changes
```

## Recipe: add a new REST endpoint

**Example: adding a "health tips" feature.**

1. **Entity** (`model/HealthTip.java`)
   ```java
   @Entity @Table(name="health_tips") @Data @NoArgsConstructor @AllArgsConstructor
   public class HealthTip {
       @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
       private String category;
       private String content;
   }
   ```

2. **Repository** (`repository/HealthTipRepository.java`)
   ```java
   public interface HealthTipRepository extends JpaRepository<HealthTip, Long> {
       List<HealthTip> findByCategory(String category);
   }
   ```

3. **DTO** (`dto/HealthTipDto.java`) — never expose entities directly
   ```java
   public record HealthTipDto(Long id, String category, String content) {}
   ```

4. **Controller** (`controller/HealthTipController.java`)
   ```java
   @RestController @RequestMapping("/api/health-tips") @RequiredArgsConstructor
   public class HealthTipController {
       private final HealthTipRepository repo;
       @GetMapping public List<HealthTipDto> list() {
           return repo.findAll().stream()
               .map(t -> new HealthTipDto(t.getId(), t.getCategory(), t.getContent()))
               .toList();
       }
   }
   ```

5. **Seed data** in `config/DataInitializer.java` — add after existing saves:
   ```java
   healthTipRepo.save(new HealthTip(null, "general", "Drink 8 glasses of water daily."));
   ```

## LLM integration pattern

To call the LLM service from a new controller, inject `LlmProxyService` — do not create a new `RestTemplate`. Add a method following the existing `chat()` / `aiConsultation()` pattern:
- Build request body as `ObjectNode`
- POST to `getApiUrl() + "/your-new-endpoint"`
- Parse `JsonNode` response
- Return a fallback if the call fails

## WebSocket broadcast

To push an update to the admin dashboard after a state change:

```java
@Autowired UserStatusPublisher publisher;
// after updating UserSession:
publisher.publishUserStatus(updatedSession);
```

The admin subscribes to `/topic/user-status` automatically.

## Adding to Admin API (`/admin/*`)

Add new admin-only endpoints to `controller/AdminController.java`. The admin React app proxies `/admin/*` to this backend.

## Key files quick reference

| File | Purpose |
|------|---------|
| `config/DataInitializer.java` | All demo seed data |
| `service/LlmProxyService.java` | Calls Python llm-service |
| `service/SessionService.java` | User session state machine |
| `config/WebSocketConfig.java` | STOMP + SockJS endpoint registration |
| `resources/application.yml` | DB, CORS, server config |
