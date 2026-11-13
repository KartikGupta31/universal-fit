import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, Search, Ruler, ShieldCheck, AlertCircle } from 'lucide-react'
import { getBrands } from '../services/recommendationService'
import './DiscoveryPages.css'

const brandDomains = {
    'Zara': 'zara.com', 'H&M': 'hm.com', 'UNIQLO': 'uniqlo.com', "Levi's": 'levi.com',
    'Nike': 'nike.com', 'adidas': 'adidas.com', 'PUMA': 'puma.com', 'GAP': 'gap.com',
    'Tommy Hilfiger': 'tommy.com', 'Demo Brand': null,
}
const categoryLabels = { T_SHIRT: 'T-shirts', SHIRT: 'Shirts', JEANS: 'Jeans', TROUSERS: 'Trousers', JACKET: 'Jackets', HOODIE: 'Hoodies', DRESS: 'Dresses', TOP: 'Tops' }

function BrandsPage() {
    const [brands, setBrands] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [search, setSearch] = useState('')
    const [category, setCategory] = useState('ALL')

    useEffect(() => {
        let active = true
        getBrands(0, 50)
            .then(data => { if (active) setBrands(data.content || []) })
            .catch(err => { if (active) setError(err.message || 'Unable to load brands right now.') })
            .finally(() => { if (active) setLoading(false) })
        return () => { active = false }
    }, [])

    const categories = useMemo(() => [...new Set(brands.flatMap(brand => brand.categories || []))].sort(), [brands])
    const filtered = useMemo(() => brands.filter(brand => {
        const matchesSearch = brand.name.toLowerCase().includes(search.trim().toLowerCase())
        const matchesCategory = category === 'ALL' || (brand.categories || []).includes(category)
        return matchesSearch && matchesCategory
    }), [brands, search, category])

    return (
        <div className="discovery-page">
            <section className="discovery-hero">
                <p className="discovery-eyebrow">THE UNIVERSAL FIT DIRECTORY</p>
                <h1>One profile.<br /><em>Every brand.</em></h1>
                <p>Explore the brands in our directory, see which categories they support, and check the status of their sizing data before finding your size.</p>
                <div className="discovery-hero-meta"><span><Ruler size={16} /> Cross-brand sizing</span><span><ShieldCheck size={16} /> Transparent data status</span></div>
            </section>

            <section className="discovery-content">
                <div className="discovery-section-heading">
                    <div><p className="discovery-eyebrow">BRAND DIRECTORY</p><h2>Find your brands.</h2></div>
                    <span className="discovery-count">{brands.length} {brands.length === 1 ? 'brand' : 'brands'}</span>
                </div>
                <div className="directory-toolbar">
                    <label className="directory-search"><Search size={17} /><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search brands..." aria-label="Search brands" /></label>
                    <select value={category} onChange={e => setCategory(e.target.value)} aria-label="Filter by category">
                        <option value="ALL">All categories</option>
                        {categories.map(item => <option key={item} value={item}>{categoryLabels[item] || item}</option>)}
                    </select>
                </div>

                {loading && <div className="discovery-state">Loading brand directory...</div>}
                {error && <div className="discovery-alert"><AlertCircle size={18} />{error}</div>}
                {!loading && !error && filtered.length === 0 && <div className="discovery-state">No brands match this search. Try another name or category.</div>}

                <div className="brand-directory-grid">
                    {filtered.map(brand => {
                        const supported = brand.categories || []
                        const hasSizingCategories = supported.length > 0
                        const verified = brand.verificationStatus === 'VERIFIED'
                        const regions = brand.supportedRegions || []
                        return (
                            <article className="brand-directory-card" key={brand.id}>
                                <div className="brand-card-top"><span className="brand-logo-wrap">
                                    {brandDomains[brand.name] ? <img className="brand-logo" src={`https://www.google.com/s2/favicons?domain=${brandDomains[brand.name]}&sz=128`} alt={`${brand.name} logo`} loading="lazy" onError={event => { event.currentTarget.style.display = 'none'; event.currentTarget.nextElementSibling.style.display = 'grid' }} /> : null}
                                    <span className="brand-logo-fallback" style={{ display: brandDomains[brand.name] ? 'none' : 'grid' }}>{brand.name.slice(0, 1).toUpperCase()}</span>
                                </span><span className={hasSizingCategories ? 'data-pill data-ready' : 'data-pill data-pending'}>{hasSizingCategories ? 'SIZING CATEGORIES LISTED' : 'NO SIZING DATA YET'}</span></div>
                                <h3 className="brand-card-name">{brand.name}</h3>
                                <p className="brand-origin">{brand.country ? `Brand origin: ${brand.country}` : 'Brand sizing directory'}</p>
                                <p className="brand-origin">{regions.length ? `Sizing regions: ${regions.join(', ')}` : 'Sizing region not specified'}</p>
                                <div className="brand-card-divider" />
                                <p className="brand-card-label">SUPPORTED CATEGORIES</p>
                                {hasSizingCategories ? <><div className="brand-category-tags">{supported.map(item => <span key={item}>{categoryLabels[item] || item}</span>)}</div><p className="brand-empty-note">Category sizing data is listed. Availability depends on region and sizing system.</p></> : <p className="brand-empty-note">No recommendation categories are available yet. Sizing data is still being added.</p>}
                                <div className="brand-card-footer"><span>{verified ? <><ShieldCheck size={14} /> Verified chart data</> : <><AlertCircle size={14} /> {brand.verificationStatus === 'DEMO_UNVERIFIED' ? 'Demo data — not for use' : 'Unverified chart data'}</>}</span>{hasSizingCategories && <Link to="/find-size">Find my size <ArrowUpRight size={14} /></Link>}</div>
                            </article>
                        )
                    })}
                </div>
                <p className="directory-footnote">A listed brand does not automatically mean every category has a usable size chart. We show availability and verification status openly; unverified data is never presented as verified.</p>
            </section>
        </div>
    )
}

export default BrandsPage
