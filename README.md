# Coupon Service

## Run

```bash
docker compose up -d                 # PostgreSQL 17 on :5433
mvn spring-boot:run                  # app on :8080
```
OR

```
You can also launch this infrastructure directly using Docker Compose within IntelliJ IDEA (utilizing Docker Desktop for Windows), and then start the application.
```

Configuration is via environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`,
`GEO_BASE_URL`, `FORWARD_HEADERS_STRATEGY`.

## API

### Swagger UI

Swagger is included in the application, allowing you to test all the endpoints within it.

[http://localhost:8080/swagger-ui/index.html#/coupon-controller/](http://localhost:8080/swagger-ui/index.html#/coupon-controller/)

### Create a coupon — `POST /api/v1/coupons`
```bash
curl -i -X POST localhost:8080/api/v1/coupons -H 'Content-Type: application/json' \
  -d '{"code":"wiosna","maxUses":100,"country":"PL"}'
```
`201 Created` + `Location: /api/v1/coupons/WIOSNA`
```json
{"code":"WIOSNA","createdAt":"2026-11-16T22:30:00.123456Z","maxUses":100,"currentUses":0,"country":"PL"}
```

### Redeem a coupon — `POST /api/v1/redemptions`
```bash
curl -i -X POST localhost:8080/api/v1/redemptions -H 'Content-Type: application/json' \
  -d '{"code":"WiOsNa","userId":"user-42"}'
```
`201 Created` → `{"code":"WIOSNA","userId":"user-42","redeemedAt":"..."}`

### Read a coupon — `GET /api/v1/coupons/{code}` (helper, handy for verification)

### Errors (RFC 9457 `application/problem+json`, stable `code` field)

| Situation                                   | HTTP | `code`                    |
|---------------------------------------------|------|---------------------------|
| Invalid payload / code / country            | 400  | `INVALID_INPUT`           |
| Coupon does not exist                       | 404  | `COUPON_NOT_FOUND`        |
| Caller's country ≠ coupon's country         | 403  | `COUNTRY_NOT_ALLOWED`     |
| Coupon reached its maximum number of uses   | 409  | `COUPON_EXHAUSTED`        |
| This user already redeemed this coupon      | 409  | `COUPON_ALREADY_REDEEMED` |
| Coupon code already taken (case-insensitive)| 409  | `COUPON_ALREADY_EXISTS`   |
| Geolocation provider failed                 | 503  | `GEOLOCATION_UNAVAILABLE` |

### Testing the country check locally
From `localhost` the caller IP is `127.0.0.1`, which cannot be geolocated, so redemption is (correctly) denied.
Start the app with `FORWARD_HEADERS_STRATEGY=framework` and send a header, e.g. `-H 'X-Forwarded-For: 5.172.200.1'`
(a Polish address) to simulate a client from a given country.

---

## Architecture

Classic layered layout, package per layer (`com.empik.coupons`):

```
controller/    REST endpoints only: parse HTTP, call a service, build the response
dto/           request/response objects of the API
service/       business use cases (CouponService, CouponRedemptionService) + their command objects
repository/    persistence: interfaces (CouponRepository, RedemptionRepository) and JDBC implementations
entity/        Coupon, Redemption (immutable records)
valueobject/   CouponCode, CountryCode, UserId, IpAddress - self-validating types used by entities and services
exception/     business exceptions, ErrorCode and the global @RestControllerAdvice
client/        outbound HTTP: CountryResolver interface + ipwho.is implementation
config/        Spring configuration
```