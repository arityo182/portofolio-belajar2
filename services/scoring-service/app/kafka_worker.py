from __future__ import annotations

import asyncio
import contextlib
import json
import logging
from typing import Any

from app.callback import CallbackClient, build_scoring_completed_event
from app.config import Settings
from app.exceptions import FeatureExtractionException, ModelNotLoadedException
from app.schemas import ScoreRequest
from app.scoring_pipeline import ScoringPipeline

logger = logging.getLogger(__name__)


def _decode_event(value: bytes) -> Any | None:
    try:
        return json.loads(value.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        logger.warning("Pesan Kafka tidak valid: %s", exc)
        return None


class KafkaEventPublisher:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._producer: Any | None = None

    async def start(self) -> None:
        if not self.settings.kafka_enabled or self._producer is not None:
            return
        try:
            from aiokafka import AIOKafkaProducer  # noqa: PLC0415

            self._producer = AIOKafkaProducer(
                bootstrap_servers=self.settings.kafka_bootstrap_servers,
                value_serializer=lambda value: json.dumps(value, default=str).encode("utf-8"),
                acks="all",
                enable_idempotence=True,
            )
            await self._producer.start()
            logger.info("Kafka producer scoring terhubung")
        except Exception as exc:  # keep the HTTP API available if Kafka is down
            logger.warning("Kafka producer tidak tersedia: %s", exc)
            self._producer = None

    async def publish(self, score: Any, trace_id: str | None = None) -> bool:
        if self._producer is None and self.settings.kafka_enabled:
            await self.start()
        if self._producer is None:
            return False
        event = build_scoring_completed_event(score, trace_id)
        try:
            await self._producer.send_and_wait(
                self.settings.kafka_topic_scoring_completed,
                key=str(score.loan_application_id).encode("utf-8"),
                value=event,
            )
            return True
        except Exception as exc:
            logger.warning("Gagal mengirim loan.scoring.completed: %s", exc)
            return False

    async def stop(self) -> None:
        if self._producer is not None:
            with contextlib.suppress(Exception):
                await self._producer.stop()
            self._producer = None


class KafkaScoringWorker:
    def __init__(
        self,
        settings: Settings,
        pipeline: ScoringPipeline,
        publisher: KafkaEventPublisher,
        callback: CallbackClient,
    ) -> None:
        self.settings = settings
        self.pipeline = pipeline
        self.publisher = publisher
        self.callback = callback
        self._task: asyncio.Task[None] | None = None
        self._running = False

    async def start(self) -> None:
        if not self.settings.kafka_enabled or self._task is not None:
            return
        self._running = True
        self._task = asyncio.create_task(self._run(), name="scoring-kafka-consumer")

    async def stop(self) -> None:
        self._running = False
        if self._task is not None:
            self._task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._task
            self._task = None

    async def _run(self) -> None:
        while self._running:
            consumer: Any | None = None
            try:
                from aiokafka import AIOKafkaConsumer  # noqa: PLC0415

                consumer = AIOKafkaConsumer(
                    self.settings.kafka_topic_scoring_requested,
                    bootstrap_servers=self.settings.kafka_bootstrap_servers,
                    group_id=self.settings.kafka_group_id,
                    auto_offset_reset=self.settings.kafka_auto_offset_reset,
                    enable_auto_commit=True,
                    value_deserializer=_decode_event,
                )
                await consumer.start()
                logger.info(
                    "Kafka consumer scoring mendengarkan topic=%s",
                    self.settings.kafka_topic_scoring_requested,
                )
                async for message in consumer:
                    if not self._running:
                        break
                    await self._handle(message.value)
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                logger.warning("Kafka consumer scoring terputus: %s; retry 5s", exc)
                await asyncio.sleep(5)
            finally:
                if consumer is not None:
                    with contextlib.suppress(Exception):
                        await consumer.stop()

    async def _handle(self, event: dict[str, Any]) -> None:
        if not isinstance(event, dict):
            logger.warning("Event loan.scoring.requested bukan object JSON, diabaikan")
            return
        payload = event.get("payload", event)
        loan_application_id = payload.get("loanApplicationId") or payload.get("loan_application_id")
        borrower_id = payload.get("borrowerId") or payload.get("borrower_id")
        if not loan_application_id or not borrower_id:
            logger.warning("Event loan.scoring.requested tidak lengkap, diabaikan")
            return

        trace_id = event.get("traceId")
        try:
            request = ScoreRequest(
                loan_application_id=loan_application_id,
                borrower_id=borrower_id,
                features=None,
            )
            score = await self.pipeline.score(request)
        except (FeatureExtractionException, ModelNotLoadedException, ValueError) as exc:
            logger.warning("Scoring event gagal loanApplicationId=%s: %s", loan_application_id, exc)
            return
        except Exception:
            logger.exception("Scoring event gagal tidak terduga loanApplicationId=%s", loan_application_id)
            return

        await self.publisher.publish(score, trace_id)
        await self.callback.send(score, trace_id)
        logger.info("Scoring selesai loanApplicationId=%s riskScore=%s", loan_application_id, score.risk_score)
