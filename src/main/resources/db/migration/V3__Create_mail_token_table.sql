CREATE TABLE "MAIL_TOKENS" (
  "token" VARCHAR(255) NOT NULL,
  "expire_at" TIMESTAMP,
  PRIMARY KEY ("token")
);