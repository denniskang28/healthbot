# LLM Service Engineering Guide

Python 3.11+ / FastAPI / supports Anthropic, OpenAI, Qwen via unified `_call_tool()`.

## How the multi-provider abstraction works

All LLM calls go through `_call_tool()` in `llm_client.py`:
- Define tools **once** in Anthropic schema format (`input_schema` key)
- `_call_tool()` auto-converts to OpenAI format for `openai` / `qwen` providers
- Returns the tool input as a plain `dict`, or `None` if unavailable
- Caller in `main.py` falls back to `mock_responses` when `None` is returned

**Never call provider SDKs directly in `main.py`.** All provider logic lives in `llm_client.py`.

## Recipe: add a new AI endpoint

**Example: adding a "symptom triage" endpoint.**

1. **Add the Pydantic models** in `models.py`:
   ```python
   class TriageRequest(BaseModel):
       userId: int
       symptoms: List[str]
       language: str = "EN"

   class TriageResponse(BaseModel):
       urgency: str          # "low" | "medium" | "high"
       recommendation: str
   ```

2. **Add the tool + function** in `llm_client.py`:
   ```python
   _TRIAGE_TOOL = {
       "name": "triage_result",
       "description": "Classify symptom urgency and give a recommendation",
       "input_schema": {
           "type": "object",
           "properties": {
               "urgency": {"type": "string", "enum": ["low", "medium", "high"]},
               "recommendation": {"type": "string"},
           },
           "required": ["urgency", "recommendation"],
       },
   }

   async def triage(symptoms: List[str], language: str) -> Optional[TriageResponse]:
       prompt = f"Triage these symptoms: {', '.join(symptoms)}. Language: {language}"
       inp = _call_tool(
           [{"role": "user", "content": prompt}],
           system="You are a medical triage assistant.",
           tools=[_TRIAGE_TOOL],
       )
       if inp is None:
           return None
       return TriageResponse(urgency=inp["urgency"], recommendation=inp["recommendation"])
   ```

3. **Add mock response** in `mock_responses.py`:
   ```python
   def get_mock_triage_response(language: str) -> TriageResponse:
       if language == "ZH":
           return TriageResponse(urgency="low", recommendation="建议休息并多喝水。")
       return TriageResponse(urgency="low", recommendation="Rest and stay hydrated.")
   ```

4. **Wire up the route** in `main.py`:
   ```python
   @app.post("/triage", response_model=TriageResponse)
   async def triage_endpoint(req: TriageRequest):
       result = await llm_client.triage(req.symptoms, req.language)
       if result is not None:
           return result
       return mock_responses.get_mock_triage_response(req.language)
   ```

## Tool schema rules

- Use `"required"` to list mandatory fields — the LLM is forced to populate them
- Enum fields constrain model output to valid values
- For arrays, always specify `"items"` with the element schema
- Keep tool descriptions action-oriented ("Generate X", "Classify Y", "Return Z")

## Environment variables

| Var | Default | Purpose |
|-----|---------|---------|
| `PROVIDER` | `anthropic` | `anthropic` / `openai` / `qwen` / `deepseek` |
| `MODEL` | provider default | Override model name |
| `ANTHROPIC_API_KEY` | — | Required for anthropic |
| `OPENAI_API_KEY` | — | Required for openai |
| `QWEN_API_KEY` | — | Required for qwen (DashScope) |
| `DEEPSEEK_API_KEY` | — | Required for deepseek (api.deepseek.com) |
| `PORT` | `8000` | Server port |

## Key files

| File | Purpose |
|------|---------|
| `llm_client.py` | All LLM logic: client init, `_call_tool()`, tool schemas, public functions |
| `models.py` | Pydantic request/response models |
| `mock_responses.py` | Fallback data when no API key is configured |
| `main.py` | FastAPI routes — thin wrappers that call `llm_client` then fall back to mock |
