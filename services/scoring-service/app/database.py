from __future__ import annotations

import asyncio
import logging
from typing import Any

import asyncpg

from app.config import Settings

logger = logging.getLogger(__name__)


class Database:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._pool: asyncpg.Pool | None = None

    @property
    def connected(self) -> bool:
        return self._pool is not None

    async def connect(self) -> None:
        if self._pool is not None:
            return
        last_error: Exception | None = None
        for attempt in range(1, max(1, self.settings.db_connect_retries) + 1):
            try:
                self._pool = await asyncpg.create_pool(
                    dsn=self.settings.database_url,
                    min_size=self.settings.db_pool_min_size,
                    max_size=self.settings.db_pool_max_size,
                    command_timeout=self.settings.db_command_timeout_seconds,
                )
                logger.info("Terhubung ke PostgreSQL scoring database")
                return
            except Exception as exc:  # asyncpg raises several connection error types
                last_error = exc
                logger.warning(
                    "Koneksi PostgreSQL gagal (percobaan %s/%s): %s",
                    attempt,
                    self.settings.db_connect_retries,
                    exc,
                )
                if attempt < self.settings.db_connect_retries:
                    await asyncio.sleep(self.settings.db_connect_retry_delay_seconds)
        raise RuntimeError("Tidak dapat terhubung ke PostgreSQL") from last_error

    async def close(self) -> None:
        if self._pool is not None:
            await self._pool.close()
            self._pool = None

    async def fetchrow(self, query: str, *args: Any) -> asyncpg.Record | None:
        pool = self._require_pool()
        return await pool.fetchrow(query, *args)

    async def fetch(self, query: str, *args: Any) -> list[asyncpg.Record]:
        pool = self._require_pool()
        return list(await pool.fetch(query, *args))

    async def fetchval(self, query: str, *args: Any) -> Any:
        pool = self._require_pool()
        return await pool.fetchval(query, *args)

    def _require_pool(self) -> asyncpg.Pool:
        if self._pool is None:
            raise RuntimeError("Database pool belum terhubung")
        return self._pool
