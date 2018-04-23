CREATE TABLE "ACCOUNTS" (
  "id" serial PRIMARY KEY,
  "name" VARCHAR(255),
  "email" VARCHAR(255) UNIQUE,
  "password" VARCHAR(255)
);