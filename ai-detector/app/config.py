from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # DB
    db_url: str = "mysql+asyncmy://root:root@localhost:3307/flashdeal"

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "ai-detector"
    kafka_topic_anomaly_detected: str = "anomaly-detected"

    # OpenAI
    openai_api_key: str = "dummy"

    # LangSmith 환경변수는 SDK가 os.environ에서 직접 읽음
    # (LANGSMITH_TRACING, LANGSMITH_API_KEY, LANGSMITH_ENDPOINT, LANGSMITH_PROJECT)
    # docker-compose.yml에서 컨테이너로 주입 — config.py 경유 불필요


settings = Settings()
