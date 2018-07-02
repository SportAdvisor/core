CREATE TABLE "RESET_PWD_TOKENS" (
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "expire_at" TIMESTAMP,
  PRIMARY KEY ("user_id", "token")
);