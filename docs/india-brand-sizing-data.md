# India brand sizing data

Data snapshot: 2026-11-12. Region: India (IN).

## Trust and eligibility rules

- Every imported brand, product and size specification is stored as `UNVERIFIED`.
- `lastVerifiedAt` remains null. No source data is marked verified by this import.
- Every usable chart keeps its official product-page URL in `ProductSizeSpec.sourceUrl`.
- Charts are scoped to the named style, category, region and sizing system. They are not generalized across an entire brand.
- `BODY` and `GARMENT` measurements remain separate.
- Product records with ambiguous measurement semantics are retained as inactive source records and blocked from recommendations.

## Recommendation-eligible data

### Levi's India

All of these are style-specific `GARMENT` measurements in centimetres.

| Category | Sizing system | Official source |
|---|---|---|
| T_SHIRT | MEN | https://levi.in/products/mens-solid-slim-fit-t-shirt-169601462 |
| T_SHIRT | WOMEN | https://levi.in/products/womens-solid-black-relaxed-fit-t-shirt-005x00000 |
| SHIRT | MEN | https://levi.in/products/mens-solid-slim-fit-shirt-329080300 |
| SHIRT | WOMEN | https://levi.in/products/womens-solid-comfort-fit-shirt-0027m0011 |
| JEANS | MEN | https://levi.in/products/mens-501-black-regular-fit-jeans-798300026 |
| JEANS | WOMEN | https://levi.in/products/levis-womens-501-original-jeans-125010640 |
| TROUSERS | MEN | https://levi.in/products/mens-slim-fit-trousers-a40690000 |
| JACKET | MEN | https://levi.in/products/mens-solid-black-jacket-002a80000 |
| JACKET | WOMEN | https://levi.in/products/womens-solid-regular-fit-jacket-006h40000 |

### UNIQLO India

These product pages explicitly label the tables as **Body Size**, so they are stored as `BODY` measurements in centimetres.

| Category | Sizing system | Official source |
|---|---|---|
| T_SHIRT | UNISEX | https://www.uniqlo.com/in/en/products/E455365-000/00?colorDisplayCode=12&sizeDisplayCode=005 |
| T_SHIRT | WOMEN | https://www.uniqlo.com/in/en/products/E474408-000/00?colorCode=COL11&sizeCode=SMA004 |
| TROUSERS | WOMEN | https://www.uniqlo.com/in/en/products/E479292-000/00?colorCode=COL58&sizeCode=SMA003 |

The women's source labels the upper-body circumference as “Bust”; it is normalized to the profile's chest circumference field. UNIQLO's India help documentation distinguishes body measurements from garment/product measurements and notes product measurements can vary by fabric and style:
https://faq-in.uniqlo.com/pkb_Home_UQ_IN?id=kA0Ie000000TNqU

## Retained but not recommendation-eligible

### H&M India

Two official India product pages expose size rows as `Width` and `Length`:

- Men's Light green Loose Fit Washed T-shirt: https://www2.hm.com/en_in/productpage.1275904004.html
- Women's Beige/Koiya Raglan-sleeved T-shirt: https://www2.hm.com/en_in/productpage.1308774001.html

The source pages do not define the width axis or whether the value is flat width or circumference. Values are stored verbatim under `width` and `length`; the associated products are inactive and their specs have `recommendationEligible=false`. Do not enable them for recommendations until the measurement method is confirmed.

H&M India sizing guidance says the size guide is accessed from each product page:
https://www2.hm.com/en_in/customer-service/sizeguide/sizing-faq.html

## Brand registered; no numeric chart imported

### Zara India

The official product page refers customers to its size guide or to request garment measurements, but the numeric measurement rows were not available in the accessible official source. No measurements were fabricated:
https://www.zara.com/in/en/zara-timeless---contrast-collar-t-shirt-p06061534.html

## Categories not yet sourced

No numeric India chart was imported for `DRESS` for any of the four brands. Zara and H&M currently have no recommendation-eligible categories; H&M's two raw source records are retained but blocked. UNIQLO currently has T_SHIRT and TROUSERS coverage. Levi's currently has T_SHIRT, SHIRT, JEANS, TROUSERS and JACKET coverage.

## Import

The seeder is idempotent and disabled by default. In the repository root `.env`, set:

```env
SEED_REAL_BRAND_DATA=true
```

Then start/rebuild the backend:

```powershell
docker compose up -d --build backend
docker compose logs backend --since 5m
```

The seeder never changes any record to `VERIFIED`. Verification requires a separate administrator review.
