---
name: conventions-source
description: Детальные архитектурные конвенции вынесены в docs/architecture.md
metadata:
  type: reference
---

Полное дерево папок `data/di/ui/utils` и подробное описание конвенций (ViewModel-паттерн, AuthInterceptor, TokenManager/CryptoManager, Navigation, multipart upload, тесты) — в [docs/architecture.md](../docs/architecture.md). В CLAUDE.md остаётся только сжатый список критичных правил.

**Why:** CLAUDE.md грузится каждую сессию — детали архитектуры читать по требованию, не держать в ядре.
**How to apply:** при добавлении нового архитектурного соглашения — описать его подробно в docs/architecture.md, а в CLAUDE.md добавить только однострочное правило, если оно критично (нарушение ломает паттерн).
