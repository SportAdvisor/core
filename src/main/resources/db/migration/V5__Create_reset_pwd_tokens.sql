CREATE TABLE "RESET_PWD_TOKENS" (
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "expire_at" TIMESTAMP,
  CONSTRAINT  reset_pwd_tokens_pkey PRIMARY KEY ("token")
);