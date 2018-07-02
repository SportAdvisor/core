CREATE TABLE "REFRESH_TOKENS" (
  "refreshTokens_id_pkey" serial PRIMARY key,
  "user_id" BIGINT NOT NULL,
  "token" VARCHAR(255) NOT NULL,
  "remember" BOOLEAN,
  "last_touch" TIMESTAMP
);