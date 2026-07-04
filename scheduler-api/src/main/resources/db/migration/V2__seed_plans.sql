INSERT INTO plans (id, tier, rate_limit, max_retries, payload_size_limit) VALUES
    (gen_random_uuid(), 'FREE', 60, 3, 65536),
    (gen_random_uuid(), 'PREMIUM', 600, 5, 262144);
