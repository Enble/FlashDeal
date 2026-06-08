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

    # LangSmith (선택)
    langchain_tracing_v2: bool = False
    langchain_api_key: str = ""
    langchain_project: str = "flashdeal-ai-detector"


settings = Settings()
