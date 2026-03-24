# ShareCart Liquibase Project Guide

## Purpose

This document defines how to create a separate Liquibase project for the ShareCart database schema. The goal is to make database evolution explicit, versioned, repeatable, and safe.

This guide is written so GitHub Copilot can generate a dedicated Liquibase project that:

- manages the PostgreSQL schema for ShareCart
- uses SQL-based migrations instead of Hibernate schema generation
- preserves a clean migration history
- supports future schema changes without rewriting the past

This should become the source of truth for database structure and future schema evolution.

---

## Project Goal

Create a separate project whose responsibility is database schema management for ShareCart.

That project should:

- create the initial ShareCart schema
- manage all future schema changes through versioned Liquibase migrations
- run against PostgreSQL
- avoid ORM-driven schema creation
- keep migrations readable and easy to review

This project is not the REST API itself. It is the database migration project for the REST API.

---

## Current ShareCart Schema

The schema currently consists of these tables:

- `users`
- `shopping_lists`
- `list_members`
- `items`
- `item_history`

The schema uses:

- PostgreSQL
- UUID primary keys
- foreign keys between tables
- cascade delete rules on list and user relationships
- timestamps for creation and update tracking
- a uniqueness constraint on `list_members(list_id, user_id)`

---

## Source SQL Schema

The Liquibase project should model the following schema exactly as the baseline.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE list_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID REFERENCES shopping_lists(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(list_id, user_id)
);

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID REFERENCES shopping_lists(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    quantity VARCHAR(50),
    is_completed BOOLEAN DEFAULT FALSE,
    category VARCHAR(100),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE item_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    item_name VARCHAR(255),
    category VARCHAR(100),
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Liquibase Design Standards

The Liquibase project should follow these rules.

### Required Rules

1. Use SQL migration files for schema changes.
2. Use Liquibase only for orchestration and tracking.
3. Never edit an old migration after it has been applied anywhere outside local throwaway development.
4. Every schema change must be introduced in a new migration file.
5. Spring Boot applications consuming this schema must not use `spring.jpa.hibernate.ddl-auto=update`.
6. Keep migrations small, readable, and reviewable.
7. Prefer explicit SQL over over-abstracted Liquibase XML changesets.
8. Use deterministic incremental file naming.
9. Keep schema creation separate from later schema evolution.
10. Treat Liquibase history as an audit trail.

### Industry-Standard Practices

- keep one baseline migration for initial schema or a very small number of baseline migrations if readability clearly improves
- separate extension setup from table creation when the database depends on PostgreSQL-specific functions
- create indexes in dedicated migrations when that improves review clarity
- keep foreign keys explicit and named when practical
- use rollback planning as a future enhancement, not as an excuse to complicate the first setup
- test migrations on an empty database and on a database already at the previous version
- run migrations automatically in non-production startup only if that matches deployment policy; otherwise run them in CI/CD or release pipelines
- document whether the migration project owns all DDL for the database

---

## Important PostgreSQL Note

Your schema uses `gen_random_uuid()`.

That function depends on PostgreSQL support typically provided through the `pgcrypto` extension.

The Liquibase baseline should therefore ensure the following is created before any table that uses `gen_random_uuid()`:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

Without that extension, migrations can fail on a clean PostgreSQL database.

---

## Recommended Liquibase Project Type

Use a dedicated Maven project for Liquibase.

Recommended characteristics:

- Java 21 compatible build
- PostgreSQL target database
- Liquibase core dependency
- SQL-based migrations under `src/main/resources/db/changelog/`
- a YAML master changelog that includes SQL files in order

This project can be either:

- a standalone migration-only Maven project
- or a Spring Boot project dedicated to Liquibase execution

For simplicity and long-term clarity, a standalone Maven-based Liquibase project is usually the better design if the project only manages schema.

---

## Recommended Project Structure

```text
sharecart-liquibase/
  pom.xml
  README.md
  src/
    main/
      resources/
        db/
          changelog/
            db.changelog-master.yaml
            migrations/
              001-enable-pgcrypto.sql
              002-create-users.sql
              003-create-shopping-lists.sql
              004-create-list-members.sql
              005-create-items.sql
              006-create-item-history.sql
              007-create-indexes.sql
```

This structure keeps the baseline readable and supports future incremental additions.

---

## Recommended Migration Strategy

For the initial version of the Liquibase project, use ordered SQL migrations like this.

### 001-enable-pgcrypto.sql

Purpose:

- enable `pgcrypto` so `gen_random_uuid()` works

### 002-create-users.sql

Purpose:

- create the `users` table

### 003-create-shopping-lists.sql

Purpose:

- create the `shopping_lists` table
- add the foreign key from `owner_id` to `users(id)`

### 004-create-list-members.sql

Purpose:

- create the `list_members` table
- enforce the unique membership constraint on `(list_id, user_id)`

### 005-create-items.sql

Purpose:

- create the `items` table
- add foreign keys to `shopping_lists` and `users`

### 006-create-item-history.sql

Purpose:

- create the `item_history` table
- add foreign key to `users`

### 007-create-indexes.sql

Purpose:

- add performance indexes for expected query patterns

Recommended indexes:

- `shopping_lists(owner_id)`
- `list_members(list_id)`
- `list_members(user_id)`
- `items(list_id)`
- `items(created_by)`
- `item_history(user_id)`
- optionally `item_history(last_used_at)` if history will be sorted or filtered by recency

This split is cleaner than placing everything into one very large file, while still keeping the project simple.

---

## Alternative Baseline Strategy

If you want the absolute minimum number of files for the initial version, you may also use:

- `001-enable-pgcrypto.sql`
- `002-init-schema.sql`
- `003-create-indexes.sql`

That is acceptable if the team prefers fewer files.

However, the more granular layout is better for reviewability and long-term maintenance.

---

## Master Changelog Design

Use a YAML master changelog only to include ordered SQL files.

Recommended file:

`src/main/resources/db/changelog/db.changelog-master.yaml`

Example:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/migrations/001-enable-pgcrypto.sql
  - include:
      file: db/changelog/migrations/002-create-users.sql
  - include:
      file: db/changelog/migrations/003-create-shopping-lists.sql
  - include:
      file: db/changelog/migrations/004-create-list-members.sql
  - include:
      file: db/changelog/migrations/005-create-items.sql
  - include:
      file: db/changelog/migrations/006-create-item-history.sql
  - include:
      file: db/changelog/migrations/007-create-indexes.sql
```

The YAML file is not where schema logic belongs. It is only the ordered entry point.

---

## Spring Boot Integration Rules

If a Spring Boot application is going to use this Liquibase-managed schema, it should be configured like this:

```properties
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.liquibase.enabled=true
spring.jpa.hibernate.ddl-auto=none
```

Recommended additional behavior for JPA-based apps using Liquibase-managed schema:

- use `ddl-auto=none` in environments where Liquibase owns schema management
- optionally use `ddl-auto=validate` later if you want Hibernate to verify mappings without mutating schema
- never use `update` once Liquibase becomes the source of truth

For the current ShareCart API project, this is especially important because it currently uses `spring.jpa.hibernate.ddl-auto=update`, which should be removed or replaced when Liquibase takes ownership.

---

## Migration Naming Convention

Use incremental numeric prefixes.

Examples:

- `001-enable-pgcrypto.sql`
- `002-create-users.sql`
- `003-create-shopping-lists.sql`
- `004-create-list-members.sql`
- `005-create-items.sql`
- `006-create-item-history.sql`
- `007-create-indexes.sql`
- `008-add-list-description.sql`
- `009-add-item-priority.sql`

Rules:

- never reuse numbers
- never rename already-applied files
- never insert a new migration in the middle of existing history
- every new schema change gets the next number

---

## Future Change Process

Every future change must follow this workflow.

1. Create a new SQL migration file with the next sequential number.
2. Add that file to the master changelog.
3. Do not modify prior migration files.
4. Test on a clean database.
5. Test upgrade from the previous migration state.
6. Merge only after validating the migration runs cleanly.

Example:

If you later add a `description` column to `shopping_lists`, create:

```text
008-add-shopping-list-description.sql
```

Then add it to the master changelog.

Do not edit `003-create-shopping-lists.sql`.

---

## What Copilot Must Not Generate

Copilot must not:

- use Hibernate auto schema update as the primary schema tool
- modify old migration files after they are part of shared history
- combine unrelated future changes into earlier files
- generate complex Liquibase XML changeSets for simple DDL
- hide important PostgreSQL behavior behind abstraction
- create migrations that assume tables already exist unless explicitly writing an incremental change
- ignore `pgcrypto` when using `gen_random_uuid()`

---

## What Copilot Should Generate

Copilot should generate:

- a Maven project configured for Liquibase and PostgreSQL
- `liquibase-core` dependency
- PostgreSQL driver dependency
- a master changelog YAML file
- SQL migration files under `src/main/resources/db/changelog/migrations/`
- a baseline schema matching the ShareCart tables exactly
- dedicated index migrations
- documentation on how to run the migrations

---

## Recommended pom.xml Expectations

The generated Liquibase project should include at minimum:

- `org.liquibase:liquibase-core`
- `org.postgresql:postgresql`

If using Spring Boot in the Liquibase project, include only what is needed. Avoid pulling in unnecessary web or JPA dependencies if the project exists purely to run migrations.

The guiding principle is minimalism.

---

## Recommended SQL Style

The SQL migrations should follow these formatting rules.

- use uppercase SQL keywords
- use one statement block per logical object
- keep column definitions vertically aligned and readable
- keep foreign keys explicit
- use `IF NOT EXISTS` only where appropriate, mainly for extensions
- avoid writing defensive SQL that hides migration mistakes
- optimize for clarity over cleverness

Example style:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Recommended Index Strategy

The provided table definitions already create some structure through primary keys and unique constraints, but you should still add explicit indexes for foreign-key lookups and common access paths.

Recommended indexes:

```sql
CREATE INDEX idx_shopping_lists_owner_id ON shopping_lists(owner_id);
CREATE INDEX idx_list_members_list_id ON list_members(list_id);
CREATE INDEX idx_list_members_user_id ON list_members(user_id);
CREATE INDEX idx_items_list_id ON items(list_id);
CREATE INDEX idx_items_created_by ON items(created_by);
CREATE INDEX idx_item_history_user_id ON item_history(user_id);
CREATE INDEX idx_item_history_last_used_at ON item_history(last_used_at);
```

This keeps foreign key traversal and lookup queries efficient.

---

## Environments And Execution

For an initial setup, keep environment handling simple.

Recommended approach:

- local development points to local PostgreSQL
- CI runs migrations against a clean ephemeral PostgreSQL database
- production runs the same migration history through the deployment pipeline

Optional later improvements:

- environment-specific properties
- CI validation commands
- rollback strategy
- tagging releases in Liquibase

These are useful later, but they are not required for a good first version.

---

## Definition Of Done

The Liquibase project is correctly set up when:

- the project builds cleanly
- Liquibase can connect to PostgreSQL
- the master changelog runs without errors on an empty database
- the schema objects are created exactly as expected
- Liquibase creates its own tracking tables
- future changes can be added as new SQL migrations without editing the baseline
- consuming applications can disable Hibernate auto schema mutation

---

## Paste-Ready Copilot Instructions For The Liquibase Project

You can copy the section below into the new Liquibase repository's Copilot instructions file.

```md
This project is a dedicated Liquibase migration project for the ShareCart PostgreSQL database.

Goal:
- manage the ShareCart database schema using Liquibase
- use SQL-based migrations, not XML or YAML schema definitions
- keep schema evolution versioned, readable, and immutable

Project constraints:
- use PostgreSQL
- use Liquibase as the source of truth for schema changes
- use SQL files for migrations
- do not use Hibernate schema auto-update
- do not modify old migration files after they are applied
- every schema change must be a new migration file

Initial schema to create:
- users
- shopping_lists
- list_members
- items
- item_history

Important PostgreSQL requirement:
- create extension pgcrypto before tables because UUID defaults use gen_random_uuid()

Recommended folder structure:
src/main/resources/db/changelog/
src/main/resources/db/changelog/migrations/

Required files:
- db/changelog/db.changelog-master.yaml
- db/changelog/migrations/001-enable-pgcrypto.sql
- db/changelog/migrations/002-create-users.sql
- db/changelog/migrations/003-create-shopping-lists.sql
- db/changelog/migrations/004-create-list-members.sql
- db/changelog/migrations/005-create-items.sql
- db/changelog/migrations/006-create-item-history.sql
- db/changelog/migrations/007-create-indexes.sql

Master changelog responsibility:
- only include SQL migration files in order
- do not place schema logic in YAML

The SQL schema must match these structures:

users:
- id UUID primary key default gen_random_uuid()
- email varchar(255) unique not null
- password_hash text not null
- name varchar(100)
- created_at timestamp default current_timestamp
- updated_at timestamp default current_timestamp

shopping_lists:
- id UUID primary key default gen_random_uuid()
- name varchar(255) not null
- owner_id UUID references users(id) on delete cascade
- created_at timestamp default current_timestamp
- updated_at timestamp default current_timestamp

list_members:
- id UUID primary key default gen_random_uuid()
- list_id UUID references shopping_lists(id) on delete cascade
- user_id UUID references users(id) on delete cascade
- role varchar(50) default 'MEMBER'
- joined_at timestamp default current_timestamp
- unique(list_id, user_id)

items:
- id UUID primary key default gen_random_uuid()
- list_id UUID references shopping_lists(id) on delete cascade
- name varchar(255) not null
- quantity varchar(50)
- is_completed boolean default false
- category varchar(100)
- created_by UUID references users(id)
- created_at timestamp default current_timestamp
- updated_at timestamp default current_timestamp

item_history:
- id UUID primary key default gen_random_uuid()
- user_id UUID references users(id) on delete cascade
- item_name varchar(255)
- category varchar(100)
- last_used_at timestamp default current_timestamp

Recommended indexes:
- shopping_lists(owner_id)
- list_members(list_id)
- list_members(user_id)
- items(list_id)
- items(created_by)
- item_history(user_id)
- item_history(last_used_at)

Naming rules:
- use sequential numeric migration names
- examples: 001-enable-pgcrypto.sql, 002-create-users.sql, 008-add-item-priority.sql
- never rename applied files
- never insert a file in the middle of the sequence later

If a Spring Boot app consumes this schema, configure it with:
- spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
- spring.liquibase.enabled=true
- spring.jpa.hibernate.ddl-auto=none

When generating code for this project:
- generate a minimal Maven project
- include liquibase-core and PostgreSQL dependencies
- create SQL migrations only
- keep migrations clean and readable
- separate baseline schema from future changes
- never generate Hibernate-driven schema management
```

---

## Suggested Next Step

When you create the new Liquibase project, the first Copilot task should be:

"Create a dedicated Maven Liquibase project for the ShareCart PostgreSQL schema using SQL migrations. Add liquibase-core and PostgreSQL dependencies, create a YAML master changelog that includes ordered SQL migration files, create pgcrypto extension setup, create baseline schema files for users, shopping_lists, list_members, items, and item_history, add index migrations, and configure Spring Boot properties only if a Spring Boot runner is included. Do not use Hibernate schema auto-update and do not use XML-based schema definitions."
