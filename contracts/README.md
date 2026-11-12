# Java <-> Python engine contract fixtures

`recommend-request.demo-tshirt.json` is the exact JSON the Spring Boot backend sends to
`POST /v1/recommend` for the seeded demo T-shirt (DEMO Alpha Wear, product-specific spec, CM, GARMENT).

Both test suites read this one file:
- `engine/tests/test_contract.py` posts it to FastAPI and expects 200.
- `backend/.../EngineDtosContractTest` serialises the Java DTOs and expects identical JSON.

If you change a field on either side, change this file and both tests tell you the other side is out of sync.
