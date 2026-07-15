# Large Model Theory And Travel Mind Integration

## 1. Core Theory

Large language models use the Transformer architecture. Input text is split into tokens, converted to vectors, and processed through self-attention layers. Attention lets the model weigh relationships between tokens even when they are far apart in the prompt. The model then predicts the next token repeatedly until it reaches a stop condition.

Travel Mind does not train a large model. It calls DeepSeek through an OpenAI-compatible REST API and focuses on prompt design, tool use, structured output, validation, and failure handling.

## 2. Prompt And Context Design

The system separates prompts into system instructions and user context:

- The system prompt fixes the agent role, output rules, and safety boundary.
- The user prompt contains dates, cities, preferences, budget, map data, content data, and the required JSON schema.
- Tool results from AMap are supplied as grounded context so the model can use real POI names, coordinates, hotels, restaurants, and weather instead of inventing them.
- When XHS is disabled, its tools are not exposed to the agent and planning continues with AMap and user context.

## 3. Agent Workflow

```text
User request
-> Research Agent + AMap tools
-> structured research context
-> Planner Agent
-> structured TripPlan DTO
-> Review Agent
-> deterministic Java validation
-> Python comfort scoring
-> MySQL persistence
-> Vue display
```

The Research Agent chooses tool calls and summarizes constraints. The Planner Agent creates the itinerary. The Review Agent checks the result semantically. Java then performs deterministic checks for dates, day count, budget, meals, hotels, attractions, and weather before accepting the result.

## 4. Structured Output

Spring AI `BeanOutputConverter` generates a schema from Java records. The schema is inserted into the prompt, and the returned JSON is converted into `TripPlan`, `TravelResearchResult`, or review DTOs. Conversion failures return an empty result and trigger a controlled fallback instead of persisting malformed text.

## 5. Tool Calling

The project uses Spring AI Alibaba `ReactAgent`. The model can request named Java tools, Java executes them, and the results are returned to the model for the next reasoning round. The model never receives API keys; credentials remain in backend configuration.

## 6. Reliability And Security

- API keys are loaded from ignored `.env` or process environment variables.
- Settings responses expose only whether a secret is configured, never the secret value.
- The settings endpoint requires authentication.
- Missing external data produces an explicit fallback or `待临近出发确认`, not fabricated weather.
- Python AI failures do not prevent a valid trip from being saved.
- The local YOLO model and large-model API solve different tasks: YOLO classifies travel images; DeepSeek plans and reviews text-based itineraries.

## 7. Verified Integration

On 2026-07-15, `deepseek-v4-flash` was verified through the Java Spring AI client. A full one-day Hangzhou request completed the Research, Planner, Review, Python comfort scoring, MySQL persistence, and Vue display flow. The saved plan ID was `1784088459745678`.
