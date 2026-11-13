import { useEffect, useState } from 'react'
import {
    getAdminBrands, createAdminBrand, setAdminBrandActive,
    getAdminCharts, createAdminChart, setAdminChartVerified, setAdminChartActive,
} from '../services/adminCatalogService'
import './AdminCatalogPage.css'

const emptyBrand = { name: '', slug: '', country: 'India', supportedRegions: 'IN', logoUrl: '', dataSource: '', notes: '' }
const chartExample = [
    { label: 'S', measurements: { chest: { min: 88, max: 94 }, waist: { min: 76, max: 82 } } },
    { label: 'M', measurements: { chest: { min: 95, max: 101 }, waist: { min: 83, max: 89 } } },
]

function AdminCatalogPage() {
    const [brands, setBrands] = useState([])
    const [charts, setCharts] = useState([])
    const [brandForm, setBrandForm] = useState(emptyBrand)
    const [chartForm, setChartForm] = useState({
        brandId: '', category: 'T_SHIRT', sizingSystem: 'UNISEX', region: 'IN',
        collection: '', measurementBasis: 'BODY', unit: 'CM', garmentFlat: false,
        fitType: 'REGULAR', dataSource: '', sourceUrl: '',
    })
    const [sizeRows, setSizeRows] = useState(JSON.stringify(chartExample, null, 2))
    const [activeTab, setActiveTab] = useState('charts')
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    async function load() {
        setError('')
        try {
            const [brandData, chartData] = await Promise.all([getAdminBrands(), getAdminCharts()])
            setBrands(brandData)
            setCharts(chartData)
            setChartForm(current => ({ ...current, brandId: current.brandId || brandData[0]?.id || '' }))
        } catch (e) { setError(e.message || 'Unable to load catalog data.') }
        finally { setLoading(false) }
    }
    useEffect(() => { load() }, [])

    async function saveBrand(event) {
        event.preventDefault()
        setSaving(true); setError(''); setNotice('')
        try {
            await createAdminBrand({ ...brandForm, supportedRegions: brandForm.supportedRegions.split(',').map(v => v.trim()).filter(Boolean) })
            setBrandForm(emptyBrand); setNotice('Brand created.'); await load()
        } catch (e) { setError(e.message) }
        finally { setSaving(false) }
    }

    async function saveChart(event) {
        event.preventDefault()
        setSaving(true); setError(''); setNotice('')
        try {
            const sizes = JSON.parse(sizeRows)
            if (!Array.isArray(sizes)) throw new Error('Size rows must be a JSON array.')
            await createAdminChart({ ...chartForm, sizes, garmentFlat: chartForm.measurementBasis === 'GARMENT' && chartForm.garmentFlat })
            setNotice('Size chart created as UNVERIFIED. Review the source before marking it verified.')
            await load()
        } catch (e) { setError(e instanceof SyntaxError ? 'Size rows contain invalid JSON.' : e.message) }
        finally { setSaving(false) }
    }

    async function toggleBrand(brand) {
        try { await setAdminBrandActive(brand.id, !brand.active); setNotice('Brand status updated.'); await load() }
        catch (e) { setError(e.message) }
    }
    async function verifyChart(chart, verified) {
        try { await setAdminChartVerified(chart.id, verified); setNotice(verified ? 'Chart verified.' : 'Chart returned to unverified.'); await load() }
        catch (e) { setError(e.message) }
    }
    async function toggleChart(chart) {
        try { await setAdminChartActive(chart.id, !chart.active); setNotice('Chart status updated.'); await load() }
        catch (e) { setError(e.message) }
    }

    if (loading) return <main className="admin-catalog-page"><p>Loading catalog management...</p></main>

    return <main className="admin-catalog-page">
        <header className="admin-catalog-header">
            <div><p className="catalog-eyebrow">UNIVERSAL FIT · ADMINISTRATION</p><h1>Brand & Size Data</h1>
                <p>Maintain brand coverage and trustworthy size charts used by recommendations.</p></div>
            <a href="/admin">Back to dashboard</a>
        </header>
        {error && <p className="catalog-alert" role="alert">{error}</p>}
        {notice && <p className="catalog-notice" role="status">{notice}</p>}
        <nav className="catalog-tabs">
            <button className={activeTab === 'charts' ? 'selected' : ''} onClick={() => setActiveTab('charts')}>Size charts ({charts.length})</button>
            <button className={activeTab === 'brands' ? 'selected' : ''} onClick={() => setActiveTab('brands')}>Brands ({brands.length})</button>
            <button className={activeTab === 'add-chart' ? 'selected' : ''} onClick={() => setActiveTab('add-chart')}>Add size chart</button>
            <button className={activeTab === 'add-brand' ? 'selected' : ''} onClick={() => setActiveTab('add-brand')}>Add brand</button>
        </nav>

        {activeTab === 'charts' && <section className="catalog-panel">
            <div className="catalog-panel-title"><div><p className="catalog-eyebrow">SIZING DATA</p><h2>All size charts</h2></div><button onClick={load}>Refresh</button></div>
            {charts.length === 0 ? <p className="catalog-empty">No size charts found.</p> : <div className="catalog-table-wrap"><table className="catalog-table">
                <thead><tr><th>Brand</th><th>Category</th><th>System</th><th>Region</th><th>Basis</th><th>Sizes</th><th>Verification</th><th>Actions</th></tr></thead>
                <tbody>{charts.map(chart => <tr key={chart.id}>
                    <td>{brands.find(b => b.id === chart.brandId)?.name || 'Brand'}</td><td>{chart.category}</td><td>{chart.sizingSystem}</td><td>{chart.region}</td><td>{chart.measurementBasis}</td><td>{chart.sizes?.length || 0}</td>
                    <td><span className={`catalog-status ${chart.verificationStatus === 'VERIFIED' ? 'verified' : ''}`}>{chart.verificationStatus}</span></td>
                    <td className="catalog-actions">{chart.verificationStatus !== 'VERIFIED' ?
                        <button onClick={() => verifyChart(chart, true)}>Verify</button> :
                        <button onClick={() => verifyChart(chart, false)}>Unverify</button>}
                        <button onClick={() => toggleChart(chart)}>{chart.active ? 'Deactivate' : 'Activate'}</button></td>
                </tr>)}</tbody>
            </table></div>}
        </section>}

        {activeTab === 'brands' && <section className="catalog-panel">
            <div className="catalog-panel-title"><div><p className="catalog-eyebrow">BRAND DIRECTORY</p><h2>Manage brands</h2></div></div>
            <div className="catalog-brand-grid">{brands.map(brand => <article className="catalog-brand-card" key={brand.id}>
                <div><h3>{brand.name}</h3><span className={`catalog-status ${brand.active ? 'verified' : ''}`}>{brand.active ? 'ACTIVE' : 'INACTIVE'}</span></div>
                <p>{brand.country || 'Country not set'} · {(brand.supportedRegions || []).join(', ') || 'No regions'}</p>
                <small>{(brand.categories || []).join(' · ') || 'No chart categories yet'}</small>
                <button onClick={() => toggleBrand(brand)}>{brand.active ? 'Deactivate brand' : 'Activate brand'}</button>
            </article>)}</div>
        </section>}

        {activeTab === 'add-brand' && <form className="catalog-panel catalog-form" onSubmit={saveBrand}>
            <p className="catalog-eyebrow">NEW BRAND</p><h2>Add a brand</h2>
            <div className="catalog-form-grid">
                <label>Brand name<input required value={brandForm.name} onChange={e => setBrandForm({ ...brandForm, name: e.target.value })} placeholder="e.g. Zara" /></label>
                <label>Slug<input value={brandForm.slug} onChange={e => setBrandForm({ ...brandForm, slug: e.target.value })} placeholder="zara (optional)" /></label>
                <label>Country<input value={brandForm.country} onChange={e => setBrandForm({ ...brandForm, country: e.target.value })} /></label>
                <label>Supported regions<input value={brandForm.supportedRegions} onChange={e => setBrandForm({ ...brandForm, supportedRegions: e.target.value })} placeholder="IN, EU, US" /></label>
                <label>Logo URL<input value={brandForm.logoUrl} onChange={e => setBrandForm({ ...brandForm, logoUrl: e.target.value })} /></label>
                <label>Data source<input value={brandForm.dataSource} onChange={e => setBrandForm({ ...brandForm, dataSource: e.target.value })} /></label>
                <label className="catalog-wide">Notes<textarea value={brandForm.notes} onChange={e => setBrandForm({ ...brandForm, notes: e.target.value })} /></label>
            </div>
            <button className="catalog-primary" disabled={saving}>{saving ? 'Saving...' : 'Create brand'}</button>
        </form>}

        {activeTab === 'add-chart' && <form className="catalog-panel catalog-form" onSubmit={saveChart}>
            <p className="catalog-eyebrow">NEW SIZE CHART</p><h2>Add chart data</h2>
            <p className="catalog-help">Use the source's exact measurements. Do not estimate missing values. Every new chart starts unverified.</p>
            <div className="catalog-form-grid">
                <label>Brand<select required value={chartForm.brandId} onChange={e => setChartForm({ ...chartForm, brandId: e.target.value })}>{brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select></label>
                <label>Category<select value={chartForm.category} onChange={e => setChartForm({ ...chartForm, category: e.target.value })}>{['T_SHIRT','SHIRT','JEANS','TROUSERS','JACKET','HOODIE','DRESS','TOP'].map(v => <option key={v}>{v}</option>)}</select></label>
                <label>Sizing system<select value={chartForm.sizingSystem} onChange={e => setChartForm({ ...chartForm, sizingSystem: e.target.value })}>{['MEN','WOMEN','UNISEX','KIDS'].map(v => <option key={v}>{v}</option>)}</select></label>
                <label>Region<input required value={chartForm.region} onChange={e => setChartForm({ ...chartForm, region: e.target.value.toUpperCase()})} /></label>
                <label>Measurement basis<select value={chartForm.measurementBasis} onChange={e => setChartForm({ ...chartForm, measurementBasis: e.target.value, garmentFlat: false })}><option>BODY</option><option>GARMENT</option></select></label>
                <label>Unit<select value={chartForm.unit} onChange={e => setChartForm({ ...chartForm, unit: e.target.value })}><option>CM</option><option>IN</option></select></label>
                <label>Fit type<select value={chartForm.fitType} onChange={e => setChartForm({ ...chartForm, fitType: e.target.value })}>{['SLIM','REGULAR','RELAXED','OVERSIZED'].map(v => <option key={v}>{v}</option>)}</select></label>
                <label>Collection / style<input value={chartForm.collection} onChange={e => setChartForm({ ...chartForm, collection: e.target.value })} /></label>
                {chartForm.measurementBasis === 'GARMENT' && <label className="catalog-checkbox"><input type="checkbox" checked={chartForm.garmentFlat} onChange={e => setChartForm({ ...chartForm, garmentFlat: e.target.checked })} /> Measurements are flat-laid halves</label>}
                <label>Data source<input required value={chartForm.dataSource} onChange={e => setChartForm({ ...chartForm, dataSource: e.target.value })} placeholder="Official brand size guide" /></label>
                <label>Source URL<input required type="url" value={chartForm.sourceUrl} onChange={e => setChartForm({ ...chartForm, sourceUrl: e.target.value })} placeholder="https://..." /></label>
                <label className="catalog-wide">Size rows (JSON)<textarea required rows="12" value={sizeRows} onChange={e => setSizeRows(e.target.value)} spellCheck="false" /></label>
            </div>
            <p className="catalog-help">Each row: label plus measurements object. Use min/max for ranges; use the same value for both when the source gives one number.</p>
            <button className="catalog-primary" disabled={saving || !brands.length}>{saving ? 'Saving...' : 'Save unverified chart'}</button>
        </form>}
    </main>
}

export default AdminCatalogPage
