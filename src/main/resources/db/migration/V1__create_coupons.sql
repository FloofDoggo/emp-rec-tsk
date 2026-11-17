CREATE TABLE coupons
(
    id           UUID         NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    max_uses     INTEGER      NOT NULL,
    current_uses INTEGER      NOT NULL DEFAULT 0,
    country      CHAR(2)      NOT NULL,
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_code_format CHECK (code ~ '^[A-Z0-9_-]{3,64}$'),
    CONSTRAINT ck_coupons_country_format CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_coupons_max_uses_positive CHECK (max_uses > 0),
    CONSTRAINT ck_coupons_uses_within_limit CHECK (current_uses >= 0 AND current_uses <= max_uses)
);

CREATE TABLE coupon_redemptions
(
    coupon_id   UUID         NOT NULL,
    user_id     VARCHAR(128) NOT NULL,
    redeemed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_coupon_redemptions PRIMARY KEY (coupon_id, user_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);
