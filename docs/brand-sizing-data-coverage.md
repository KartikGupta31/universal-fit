# Brand sizing data coverage

Snapshot: 2026-11-13. All imported numeric records start as `UNVERIFIED`; administrator review is required before they can be marked verified.

| Brand | Region | Category / sizing system | Measurement basis | Coverage and limitation |
|---|---|---|---|---|
| Levi's | IN | T-shirt, shirt, jeans, trousers, jacket; MEN and WOMEN where style sources exist | GARMENT, style-specific | Separate official product-style tables. Not a universal brand chart. |
| UNIQLO | IN | T-shirt (UNISEX, WOMEN), trousers (WOMEN) | BODY | Product pages explicitly label Body Size. |
| Nike | ASIA | T-shirt (MEN, WOMEN) | BODY | Official Asian sizing charts. Region is explicitly ASIA, not India. |
| adidas | US | T-shirt (MEN) | BODY | Official US men's tops chart. Region is explicitly US, not India. |
| PUMA | IN | No numeric category yet | — | India FAQ says apparel uses US sizing and product-specific charts are on product pages; no general numeric chart imported. |
| H&M | IN | T-shirt raw rows | Unresolved raw Width/Length | Inactive and excluded from recommendations because the source does not define the width axis or flat/circumference method. |
| Zara | IN | No numeric category yet | — | Official India source did not expose numeric rows; no values inferred. |

## Official sources

- Levi's India product-specific charts: https://levi.in/products/mens-solid-slim-fit-t-shirt-169601462 and linked product sources recorded in the seeder.
- UNIQLO India Body Size product charts and measurement-method guidance: https://www.uniqlo.com/in/en/products/E455365-000/00?colorDisplayCode=12&sizeDisplayCode=005 and https://faq-in.uniqlo.com/pkb_Home_UQ_IN?id=kA0Ie000000TNqU
- Nike Asian men's tops: https://www.nike.com/size-fit/mens-tops-asian-alpha
- Nike Asian women's tops: https://img.nike.com.hk/resources/sizecart/asian-womens-tops-sizing-chart_en.html
- adidas US men's tops: https://www.adidas.com/us/help/size_charts
- PUMA India sizing FAQ: https://in.puma.com/in/en/help/how-to-place-an-order-faq
- H&M India sizing FAQ: https://www2.hm.com/en_in/customer-service/sizeguide/sizing-faq.html
- Zara India product/source page: https://www.zara.com/in/en/zara-timeless---contrast-collar-t-shirt-p06061534.html

Regional charts are never relabelled as Indian sizing. Additional regional imports must be stored as separate data with their source URL and region code.
