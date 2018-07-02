CREATE TABLE "RESET_PWD_TOKENS" (
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "expire_at" TIMESTAMP,
  CONSTRAINT  pk_reset_pwd_tokens PRIMARY KEY ("token")
);