from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from app.config import settings

engine = create_async_engine(settings.db_url, pool_pre_ping=True)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


async def init_db():
    """DB 연결 확인. 테이블 생성은 Spring Boot(ddl-auto: update)가 담당."""
    async with engine.connect() as conn:
        await conn.run_sync(lambda _: None)


async def get_session():
    async with async_session() as session:
        yield session
