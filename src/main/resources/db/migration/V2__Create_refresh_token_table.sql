CREATE TABLE "REFRESH_TOKENS" (
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "remember" BOOLEAN,
  "last_touch" TIMESTAMP,
  PRIMARY KEY ("user_id", "token")
);