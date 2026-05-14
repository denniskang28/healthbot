# HealthBot Backend

Java Spring Boot backend for the HealthBot insurance health chatbot system.

## Run

```bash
cd backend
mvn spring-boot:run
```

Runs on port 8080. H2 console available at http://localhost:8080/h2-console

## API

- `POST /api/chat/{userId}/message` — Send chat message
- `GET /api/chat/{userId}/history` — Get chat history
- `GET /api/doctors` — List doctors
- `POST /api/consultations` — Start consultation
- `POST /api/ai-consultation/{userId}/message` — AI consultation message
- `POST /api/prescriptions` — Create prescription
- `POST /api/purchases` — Create purchase
- `PUT /api/purchases/{id}/complete` — Complete purchase
- `POST /api/appointments` — Book appointment
- `GET /admin/users/active` — Admin: active users
- `GET /admin/llm-config` — Admin: LLM config
- `PUT /admin/llm-config` — Admin: update LLM config

## WebSocket

Connect to `ws://localhost:8080/ws` (SockJS + STOMP)
Subscribe to `/topic/user-status` for real-time user state updates.
