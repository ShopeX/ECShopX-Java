# Flyway migrations

Put Java backend database changes here using Flyway SQL migration names:

```text
VyyyyMMddHHmmss__description.sql
```

Prefer generating files from the project root:

```bash
bin/make-migration add_order_extra_index
```

The generator can prefill simple `ALTER TABLE ... ADD COLUMN ...` SQL for newly added MyBatis-Plus domain fields in the current git diff. Always review generated SQL before applying it.
