import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Check, CircleAlert, GitCompareArrows, Ruler } from 'lucide-react'
import { getBrands, getCategoryRecommendation } from '../services/recommendationService'
import './DiscoveryPages.css'

const categoryLabels = { T_SHIRT: 'T-shirts', SHIRT: 'Shirts', JEANS: 'Jeans', TROUSERS: 'Trousers', JACKET: 'Jackets', HOODIE: 'Hoodies', DRESS: 'Dresses', TOP: 'Tops' }
const systemLabels = { MEN: 'Men', WOMEN: 'Women', UNISEX: 'Unisex' }

function ComparePage() {
    const [brands, setBrands] = useState([])
    const [selected, setSelected] = useState([])
    const [category, setCategory] = useState('')
    const [systems, setSystems] = useState({})
    const [region, setRegion] = useState({})
    const [results, setResults] = useState([])
    const [loading, setLoading] = useState(true)
    const [comparing, setComparing] = useState(false)
    const [error, setError] = useState('')

    useEffect(() => {
        getBrands(0, 50).then(data => setBrands(data.content || [])).catch(err => setError(err.message || 'Unable to load brands.')).finally(() => setLoading(false))
    }, [])

    const selectedBrands = brands.filter(brand => selected.includes(brand.id))
    const categoryOptions = useMemo(() => [...new Set(selectedBrands.flatMap(brand => brand.categories || []))].sort(), [selectedBrands])
    const toggleBrand = id => {
        setResults([])
        setError('')
        setSelected(current => current.includes(id) ? current.filter(item => item !== id) : current.length < 4 ? [...current, id] : current)
    }

    useEffect(() => {
        if (category && !categoryOptions.includes(category)) setCategory('')
        const nextSystems = { ...systems }
        selectedBrands.forEach(brand => {
            const available = brand.sizingSystemsByCategory?.[category] || []
            if (!available.includes(nextSystems[brand.id])) nextSystems[brand.id] = available[0] || ''
        })
        setSystems(nextSystems)
    }, [category, selected.join(','), brands.length])

    async function handleCompare() {
        if (!category || selectedBrands.length < 2) return
        setComparing(true)
        setError('')
        setResults([])
        const output = await Promise.all(selectedBrands.map(async brand => {
            const availableSystems = brand.sizingSystemsByCategory?.[category] || []
            const sizingSystem = systems[brand.id] || availableSystems[0]
            if (!availableSystems.length || !sizingSystem) return { brand, unavailable: true, message: 'No recommendation-ready chart for this category.' }
            try {
                const data = await getCategoryRecommendation(brand.id, category, region[brand.id] || brand.supportedRegions?.[0], sizingSystem)
                return { brand, data, sizingSystem }
            } catch (err) {
                return { brand, error: err.message || 'Recommendation could not be generated.', sizingSystem }
            }
        }))
        setResults(output)
        setComparing(false)
    }

    return (
        <div className="discovery-page">
            <section className="discovery-hero compare-hero">
                <p className="discovery-eyebrow">YOUR MEASUREMENTS, SIDE BY SIDE</p>
                <h1>Compare your<br /><em>brand sizes.</em></h1>
                <p>Use your saved measurement profile to request a size recommendation from multiple brands for the same clothing category.</p>
                <div className="discovery-hero-meta"><span><GitCompareArrows size={16} /> Compare up to four brands</span><span><Ruler size={16} /> One saved profile</span></div>
            </section>

            <section className="discovery-content compare-content">
                <div className="discovery-section-heading"><div><p className="discovery-eyebrow">SIZE COMPARISON</p><h2>Choose your brands.</h2></div><span className="discovery-count">{selected.length}/4 selected</span></div>
                {loading ? <div className="discovery-state">Loading brands...</div> : (
                    <>
                        <div className="compare-brand-grid">
                            {brands.filter(brand => (brand.categories || []).length > 0).map(brand => (
                                <button type="button" key={brand.id} onClick={() => toggleBrand(brand.id)} className={selected.includes(brand.id) ? 'compare-brand-option selected' : 'compare-brand-option'} aria-pressed={selected.includes(brand.id)}>
                                    <span className="compare-brand-check">{selected.includes(brand.id) ? <Check size={15} /> : brand.name.slice(0, 1)}</span><span><strong>{brand.name}</strong><small>{brand.categories.length} supported categories</small></span>
                                </button>
                            ))}
                        </div>
                        <div className="compare-controls">
                            <label>Clothing category<select value={category} onChange={e => { setCategory(e.target.value); setResults([]) }} disabled={!selectedBrands.length}><option value="">Choose category</option>{categoryOptions.map(item => <option key={item} value={item}>{categoryLabels[item] || item}</option>)}</select></label>
                            {selectedBrands.map(brand => {
                                const available = brand.sizingSystemsByCategory?.[category] || []
                                return available.length > 1 ? <label key={brand.id}>{brand.name} sizing<select value={systems[brand.id] || available[0]} onChange={e => setSystems(current => ({ ...current, [brand.id]: e.target.value }))}>{available.map(system => <option key={system} value={system}>{systemLabels[system] || system}</option>)}</select></label> : null
                            })}
                        </div>
                        {selectedBrands.length < 2 && <p className="compare-hint">Select at least two brands to compare. Choose brands that support the same category for the most useful comparison.</p>}
                        {error && <p className="discovery-inline-error">{error}</p>}
                        <button className="discovery-primary" type="button" onClick={handleCompare} disabled={selectedBrands.length < 2 || !category || comparing}>{comparing ? 'Comparing sizes...' : 'Compare my sizes'} <ArrowRight size={16} /></button>
                    </>
                )}

                {results.length > 0 && <section className="compare-results" aria-live="polite"><div className="discovery-section-heading"><div><p className="discovery-eyebrow">YOUR RESULTS</p><h2>{categoryLabels[category] || category} size comparison.</h2></div></div><div className="compare-result-grid">{results.map(item => <article className="compare-result-card" key={item.brand.id}><p className="compare-result-brand">{item.brand.name}</p>{item.data ? <><strong className="compare-size-value">{item.data.recommendedSize || 'No size'}</strong><span className="compare-result-caption">RECOMMENDED SIZE</span><div className="compare-confidence"><span>Confidence</span><strong>{Math.round((item.data.confidence || 0) * 100)}%</strong></div><p className="compare-result-reason">{item.data.reason}</p><span className={item.data.chartVerified ? 'compare-verification verified' : 'compare-verification'}>{item.data.chartVerified ? 'Verified chart' : 'Unverified chart'}</span>{item.data.alternatives?.length > 0 && <p className="compare-alternatives">Alternatives: {item.data.alternatives.join(', ')}</p>}</> : <div className="compare-unavailable"><CircleAlert size={18} /><strong>{item.unavailable ? 'Not available yet' : 'Could not compare'}</strong><p>{item.message || item.error}</p></div>}</article>)}</div><p className="compare-disclaimer">Sizes are brand-specific labels, not a universal scale. Confidence and chart verification are shown separately for each recommendation.</p></section>}
                <div className="compare-profile-note"><Ruler size={18} /><p>Comparison uses your saved measurements and fit preference. <Link to="/find-size">Review or update your measurement profile</Link> before comparing.</p></div>
            </section>
        </div>
    )
}

export default ComparePage
