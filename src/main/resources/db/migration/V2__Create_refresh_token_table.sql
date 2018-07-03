CREATE TABLE "REFRESH_TOKENS" (
  "id" serial,
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "remember" BOOLEAN,
  "last_touch" TIMESTAMP,
  CONSTRAINT  refresh_tokens_id_pkey PRIMARY KEY ("id", "user_id", "token")
);