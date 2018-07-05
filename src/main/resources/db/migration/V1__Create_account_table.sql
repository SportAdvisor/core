CREATE TABLE "ACCOUNTS" (
  "id" serial,
  "name" VARCHAR(255),
  "email" VARCHAR(255) UNIQUE,
  "password" VARCHAR(255),
  CONSTRAINT  accounts_id_pkey PRIMARY KEY ("id")
);