# CloudWatch Logs Insights - Metricas del chatbot

Consultas para explotar los logs estructurados emitidos por `LoggingChatbotMetricsRecorder`.

Supuestos:
- La aplicacion esta desplegada en AWS App Runner y sus logs llegan a CloudWatch Logs.
- Los eventos de metricas contienen el campo comun `chatbot_metric_type`.
- El formato actual es texto `key=value`, por ejemplo `chatbot_metric_type=message_handled ... durationMs=123 success=true`.

## 1. Contar mensajes procesados

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| stats count() as totalMessages by bin(1h)
```

## 2. Latencia media y percentil 95

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| parse @message /durationMs=(?<durationMs>\d+)/
| stats
    count() as totalMessages,
    avg(durationMs) as avgLatencyMs,
    pct(durationMs, 95) as p95LatencyMs,
    max(durationMs) as maxLatencyMs
  by bin(1h)
```

## 3. Uso de IA en respuestas finales

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| parse @message /usedAi=(?<usedAi>\w+)/
| stats count() as total by usedAi
```

## 4. Respuestas por tipo de conversacion

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| parse @message /conversationType=(?<conversationType>\w+)/
| stats count() as total by conversationType
```

## 5. Respuestas por modo de respuesta

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| parse @message /responseMode=(?<responseMode>\w+)/
| stats count() as total by responseMode
```

## 6. Tasa de exito/error

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=message_handled/
| parse @message /success=(?<success>\w+)/
| stats count() as total by success
```

## 7. Fallbacks de IA

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=fallback/
| parse @message /fallbackType=(?<fallbackType>\S+)/
| parse @message /reason=(?<reason>\S+)/
| stats count() as total by fallbackType, reason
```

## 8. Latencia de llamadas IA

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=ai_call/
| parse @message /durationMs=(?<durationMs>\d+)/
| parse @message /success=(?<success>\w+)/
| parse @message /fallback=(?<fallback>\w+)/
| stats
    count() as totalAiCalls,
    avg(durationMs) as avgAiLatencyMs,
    pct(durationMs, 95) as p95AiLatencyMs
  by success, fallback
```

## 9. Escalados

```sql
fields @timestamp, @message
| filter @message like /chatbot_metric_type=escalation/
| parse @message /success=(?<success>\w+)/
| parse @message /errorType=(?<errorType>\S+)/
| stats count() as total by success, errorType
```
