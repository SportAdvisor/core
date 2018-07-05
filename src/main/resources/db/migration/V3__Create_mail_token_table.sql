CREATE TABLE "MAIL_TOKENS" (
  "token" VARCHAR(255) NOT NULL ,
  "expire_at" TIMESTAMP,
  CONSTRAINT  mail_tokens_token_pkey PRIMARY KEY ("token")
);