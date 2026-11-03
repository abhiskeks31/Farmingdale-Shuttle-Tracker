INSERT INTO admin (
    username,
    email,
    password_hash,
    full_name,
    is_active,
    created_at,
    last_login
)
VALUES (
    'testadmin',
    'testadmin@example.com',
    '2a10$dDBBcphGRnG74RKGCkWb4OkEf5kn6pSq0RRTbDwZnSJ3yfO3HBkzO',
    'Test Administrator',
    TRUE,
    NOW(),
    NULL
);
