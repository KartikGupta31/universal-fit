
import { useEffect, useState } from 'react'
import {
    getCurrentUser,
    updateMeasurementConsent,
    getMeasurements,
    saveMeasurements,
} from '../services/measurementService'
import {
    getBrands,
    getCategoryRecommendation,
} from '../services/recommendationService'
import './FindSizePage.css'

const measurementFields = [
    { key: 'height', label: 'Height', placeholder: 'e.g. 175' },
    { key: 'chest', label: 'Chest', placeholder: 'e.g. 95' },
    { key: 'waist', label: 'Waist', placeholder: 'e.g. 80' },
    { key: 'hips', label: 'Hips', placeholder: 'e.g. 96' },
    { key: 'shoulder', label: 'Shoulder width', placeholder: 'e.g. 44' },
    { key: 'neck', label: 'Neck', placeholder: 'e.g. 38' },
    { key: 'sleeve', label: 'Sleeve length', placeholder: 'e.g. 62' },
    { key: 'inseam', label: 'Inseam', placeholder: 'e.g. 81' },
    { key: 'thigh', label: 'Thigh circumference', placeholder: 'e.g. 55' },
    { key: 'footLength', label: 'Foot length', placeholder: 'e.g. 27' },
]

const emptyValues = {
    height: '',
    chest: '',
    waist: '',
    hips: '',
    shoulder: '',
    neck: '',
    sleeve: '',
    inseam: '',
    thigh: '',
    footLength: '',
}

function FindSizePage() {
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [consent, setConsent] = useState(false)
    const [consentLoading, setConsentLoading] = useState(false)

    const [unit, setUnit] = useState('CM')
    const [preferredFit, setPreferredFit] = useState('REGULAR')
    const [values, setValues] = useState(emptyValues)
    const [weightKg, setWeightKg] = useState('')
    const [hasProfile, setHasProfile] = useState(false)

    const [brands, setBrands] = useState([])
    const [selectedBrand, setSelectedBrand] = useState('')
    const [selectedCategory, setSelectedCategory] = useState('')
    const [selectedSizingSystem, setSelectedSizingSystem] = useState('UNISEX')
    const [selectedRegion, setSelectedRegion] = useState('')
    const [catalogLoading, setCatalogLoading] = useState(false)
    const [recommending, setRecommending] = useState(false)
    const [recommendation, setRecommendation] = useState(null)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    async function loadProfile() {
        const profile = await getMeasurements()

        if (profile) {
            setUnit(profile.unit || 'CM')
            setPreferredFit(profile.preferredFit || 'REGULAR')

            setValues({
                ...emptyValues,
                ...Object.fromEntries(
                    Object.entries(profile.values || {}).map(([key, value]) => [
                        key,
                        String(value),
                    ])
                ),
            })

            setWeightKg(
                profile.weightKg == null ? '' : String(profile.weightKg)
            )

            setHasProfile(true)
        } else {
            setHasProfile(false)
        }
    }

    async function loadCatalog() {
        setCatalogLoading(true)

        try {
            const brandData = await getBrands()
            setBrands(brandData?.content || [])
        } catch (err) {
            setError(`Could not load brands: ${err.message}`)
        } finally {
            setCatalogLoading(false)
        }
    }

    useEffect(() => {
        let active = true

        async function initialize() {
            try {
                const user = await getCurrentUser()

                if (!active) return

                setConsent(Boolean(user.measurementConsent))

                if (user.measurementConsent) {
                    await loadProfile()
                    if (active) await loadCatalog()
                }
            } catch (err) {
                if (active) setError(err.message)
            } finally {
                if (active) setLoading(false)
            }
        }

        initialize()

        return () => {
            active = false
        }
    }, [])

    async function handleConsent() {
        setError('')
        setConsentLoading(true)

        try {
            await updateMeasurementConsent(true)
            setConsent(true)
            await loadProfile()
            await loadCatalog()
        } catch (err) {
            setError(err.message)
        } finally {
            setConsentLoading(false)
        }
    }

    function handleUnitChange(nextUnit) {
        if (nextUnit === unit) return

        const factor = nextUnit === 'IN' ? 1 / 2.54 : 2.54

        setValues(current => {
            const converted = {}

            Object.entries(current).forEach(([key, value]) => {
                converted[key] = value === ''
                    ? ''
                    : String(Math.round(Number(value) * factor * 10) / 10)
            })

            return converted
        })

        setUnit(nextUnit)
    }

    function handleValueChange(key, value) {
        setValues(current => ({
            ...current,
            [key]: value,
        }))

        setSuccess('')
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setError('')
        setSuccess('')

        const measurementValues = {}

        Object.entries(values).forEach(([key, value]) => {
            if (value.trim() !== '') {
                measurementValues[key] = Number(value)
            }
        })

        if (Object.keys(measurementValues).length === 0) {
            setError('Please enter at least one body measurement.')
            return
        }

        if (
            Object.values(measurementValues).some(
                value => !Number.isFinite(value) || value <= 0
            )
        ) {
            setError('Please enter valid positive measurement values.')
            return
        }

        const weight = weightKg.trim() === '' ? null : Number(weightKg)

        if (
            weight !== null &&
            (!Number.isFinite(weight) || weight < 1 || weight > 500)
        ) {
            setError('Please enter a valid weight between 1 and 500 kg.')
            return
        }

        setSaving(true)

        try {
            await saveMeasurements({
                unit,
                preferredFit,
                weightKg: weight,
                values: measurementValues,
            })

            setHasProfile(true)
            setSuccess('Your measurements have been saved successfully.')
            setRecommendation(null)
        } catch (err) {
            setError(err.message)
        } finally {
            setSaving(false)
        }
    }

    async function handleRecommendation() {
        if (!selectedBrand || !selectedCategory) {
            setError('Please select a brand and clothing category first.')
            return
        }

        if (!hasProfile) {
            setError('Please save your measurements before finding your size.')
            return
        }

        setError('')
        setRecommendation(null)
        setRecommending(true)

        try {
            const result = await getCategoryRecommendation(selectedBrand, selectedCategory, selectedRegion, selectedSizingSystem)
            setRecommendation(result)
        } catch (err) {
            setError(err.message)
        } finally {
            setRecommending(false)
        }
    }

    const selectedBrandData = brands.find(brand => brand.id === selectedBrand)
    const supportedCategories = selectedBrandData?.categories || []
    const supportedSizingSystems = selectedBrandData?.sizingSystemsByCategory?.[selectedCategory] || ['UNISEX']
    const categoryLabels = { T_SHIRT: 'T-shirts', SHIRT: 'Shirts', JEANS: 'Jeans', TROUSERS: 'Trousers', JACKET: 'Jackets', HOODIE: 'Hoodies', DRESS: 'Dresses', TOP: 'Tops' }

    if (loading) {
        return (
            <main className="find-size-page">
                <div className="measurement-status">
                    Loading your measurement profile...
                </div>
            </main>
        )
    }

    if (error && !consent) {
        return (
            <main className="find-size-page">
                <div className="measurement-status measurement-error">
                    {error}
                </div>
            </main>
        )
    }

    if (!consent) {
        return (
            <main className="find-size-page">
                <section className="consent-card">
                    <p className="eyebrow">YOUR FIT PROFILE</p>
                    <h1>Your measurements.<br />Your perfect fit.</h1>

                    <p className="consent-description">
                        Save your body measurements once and use them to discover
                        your size across different clothing brands.
                    </p>

                    <div className="consent-details">
                        <h3>Before we begin</h3>
                        <p>
                            Universal Fit needs your permission to collect and store
                            your body measurements for size recommendations.
                        </p>
                        <p>
                            Your measurements are associated with your account.
                            You can delete your measurement profile whenever you choose.
                        </p>
                    </div>

                    {error && <p className="form-error">{error}</p>}

                    <button
                        className="primary-button consent-button"
                        onClick={handleConsent}
                        disabled={consentLoading}
                    >
                        {consentLoading ? 'Please wait...' : 'I agree and continue'}
                    </button>

                    <p className="consent-footnote">
                        You must provide consent before saving measurements.
                    </p>
                </section>
            </main>
        )
    }

    return (
        <main className="find-size-page">
            <section className="measurement-container">
                <div className="measurement-heading">
                    <div>
                        <p className="eyebrow">YOUR PERSONAL FIT PROFILE</p>
                        <h1>Find your size.</h1>
                        <p className="page-description">
                            Add your measurements once. Find your size across brands.
                        </p>
                    </div>

                    <span className={`profile-status ${hasProfile ? 'saved' : ''}`}>
                        {hasProfile ? 'PROFILE SAVED' : 'NEW PROFILE'}
                    </span>
                </div>

                <form onSubmit={handleSubmit}>
                    <section className="measurement-section">
                        <div className="section-heading">
                            <div>
                                <h2>Body measurements</h2>
                                <p>Enter your measurements as accurately as possible.</p>
                            </div>

                            <div className="unit-selector">
                                <button
                                    type="button"
                                    className={unit === 'CM' ? 'active' : ''}
                                    onClick={() => handleUnitChange('CM')}
                                >
                                    CM
                                </button>

                                <button
                                    type="button"
                                    className={unit === 'IN' ? 'active' : ''}
                                    onClick={() => handleUnitChange('IN')}
                                >
                                    IN
                                </button>
                            </div>
                        </div>

                        <div className="measurement-grid">
                            {measurementFields.map(field => (
                                <label className="measurement-field" key={field.key}>
                                    <span>{field.label}</span>

                                    <div className="measurement-input-wrap">
                                        <input
                                            type="number"
                                            step="0.1"
                                            min="0"
                                            placeholder={field.placeholder}
                                            value={values[field.key]}
                                            onChange={event =>
                                                handleValueChange(
                                                    field.key,
                                                    event.target.value
                                                )
                                            }
                                        />

                                        <span className="input-unit">
                                            {unit.toLowerCase()}
                                        </span>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </section>

                    <section className="measurement-section preferences-section">
                        <h2>Fit preferences</h2>
                        <p className="section-description">
                            How do you like your clothes to fit?
                        </p>

                        <div className="fit-options">
                            {[
                                { value: 'SLIM', title: 'Slim', description: 'Close to the body' },
                                { value: 'REGULAR', title: 'Regular', description: 'A balanced fit' },
                                { value: 'RELAXED', title: 'Relaxed', description: 'A little extra room' },
                                { value: 'OVERSIZED', title: 'Oversized', description: 'Loose and roomy' },
                            ].map(option => (
                                <button
                                    type="button"
                                    key={option.value}
                                    className={`fit-option ${preferredFit === option.value ? 'selected' : ''}`}
                                    onClick={() => setPreferredFit(option.value)}
                                >
                                    <strong>{option.title}</strong>
                                    <span>{option.description}</span>
                                </button>
                            ))}
                        </div>
                    </section>

                    <section className="measurement-section weight-section">
                        <div>
                            <h2>Weight</h2>
                            <p className="section-description">
                                Optional. Used as additional profile information.
                            </p>
                        </div>

                        <label className="measurement-field weight-field">
                            <span>Weight</span>

                            <div className="measurement-input-wrap">
                                <input
                                    type="number"
                                    min="1"
                                    max="500"
                                    step="0.1"
                                    placeholder="e.g. 65"
                                    value={weightKg}
                                    onChange={event => setWeightKg(event.target.value)}
                                />
                                <span className="input-unit">kg</span>
                            </div>
                        </label>
                    </section>

                    {error && <p className="form-error">{error}</p>}
                    {success && <p className="form-success">{success}</p>}

                    <div className="save-section">
                        <p>Your measurements are private and can be updated anytime.</p>

                        <button
                            type="submit"
                            className="primary-button save-button"
                            disabled={saving}
                        >
                            {saving
                                ? 'Saving...'
                                : hasProfile
                                    ? 'Update measurements'
                                    : 'Save measurements'}
                        </button>
                    </div>
                </form>

                <section className="measurement-section product-selector-section">
                    <p className="eyebrow">PERSONALIZED RECOMMENDATION</p>
                    <h2>Find your size across brands.</h2>
                    <p className="section-description">Select a brand and clothing category. We'll use your saved measurements and that brand's size chart.</p>
                    {catalogLoading ? <p>Loading brands...</p> : brands.length === 0 ? <p>No active brands are available yet.</p> : (
                        <>
                            <div className="selection-grid">
                                <label className="product-select-label">Choose a brand
                                    <select value={selectedBrand} onChange={event => {
                                        setSelectedBrand(event.target.value); setSelectedCategory(''); setSelectedSizingSystem(''); setSelectedRegion('');
                                        setRecommendation(null); setError('');
                                    }}>
                                        <option value="">Select a brand</option>
                                        {brands.map(brand => <option key={brand.id} value={brand.id}>{brand.name}</option>)}
                                    </select>
                                </label>
                                {supportedCategories.length > 0 && (
                                    <>
                                        <label className="product-select-label">Choose a category
                                            <select value={selectedCategory} disabled={!selectedBrand} onChange={event => {
                                                const systems = selectedBrandData?.sizingSystemsByCategory?.[event.target.value] || [];
                                                setSelectedCategory(event.target.value); setSelectedSizingSystem(systems[0] || ''); setRecommendation(null); setError('');
                                            }}>
                                                <option value="">Select a category</option>
                                                {supportedCategories.map(category => <option key={category} value={category}>{categoryLabels[category] || category.replaceAll('_', ' ')}</option>)}
                                            </select>
                                        </label>
                                        <label className="product-select-label">Sizing for
                                            <select value={selectedSizingSystem} disabled={!selectedCategory || supportedSizingSystems.length === 0} onChange={event => {
                                                setSelectedSizingSystem(event.target.value); setRecommendation(null); setError('');
                                            }}>
                                                {!selectedCategory && <option value="">Choose a category first</option>}
                                                {selectedCategory && supportedSizingSystems.map(system => <option key={system} value={system}>{system === 'MEN' ? 'Men' : system === 'WOMEN' ? 'Women' : 'Unisex'}</option>)}
                                            </select>
                                        </label>
                                        {selectedBrandData?.supportedRegions?.length > 1 && (
                                            <label className="product-select-label">Sizing region
                                                <select value={selectedRegion} onChange={event => {
                                                    setSelectedRegion(event.target.value); setRecommendation(null);
                                                }}>
                                                    <option value="">Auto-detect region</option>
                                                    {selectedBrandData.supportedRegions.map(region => <option key={region} value={region}>{region}</option>)}
                                                </select>
                                            </label>
                                        )}
                                    </>
                                )}
                            </div>
                            {selectedBrand && supportedCategories.length === 0 && (
                                <div className="brand-sizing-unavailable" role="status">
                                    <strong>Size recommendations coming soon</strong>
                                    <p>We're working on verifying this brand's size-chart data. Categories and sizing options will become available once reliable measurements are confirmed.</p>
                                </div>
                            )}
                            {supportedCategories.length > 0 && (
                                <button type="button" className="primary-button recommendation-button" onClick={handleRecommendation}
                                    disabled={!selectedBrand || !selectedCategory || !selectedSizingSystem || recommending || !hasProfile}>
                                    {recommending ? 'Finding your size...' : 'Find My Size'}
                                </button>
                            )}
                        </>
                    )}
                </section>

                {recommendation && (
                    <section className="recommendation-result">
                        <p className="eyebrow">YOUR SIZE RESULT</p>
                        <h2>Your personalized recommendation</h2>

                        {recommendation.recommendedSize ? (
                            <div className="recommended-size">
                                <span>RECOMMENDED SIZE</span>
                                <strong>{recommendation.recommendedSize}</strong>
                            </div>
                        ) : (
                            <p className="recommendation-warning">
                                We couldn't confidently recommend a size for this brand and category.
                            </p>
                        )}

                        <p className="recommendation-confidence">
                            Confidence: {Math.round(recommendation.confidence * 100)}%
                        </p>

                        {recommendation.uncertain && (
                            <p className="recommendation-warning">
                                This recommendation is uncertain. Please review the size guide.
                            </p>
                        )}

                        {recommendation.alternatives?.length > 0 && (
                            <div className="result-detail">
                                <h3>Alternative sizes</h3>
                                <div className="alternative-sizes">
                                    {recommendation.alternatives.map(size => (
                                        <span key={size}>{size}</span>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="result-detail">
                            <h3>Why this size?</h3>
                            <p>{recommendation.reason}</p>
                        </div>

                        {recommendation.measurementAnalysis &&
                            Object.keys(recommendation.measurementAnalysis).length > 0 && (
                                <div className="result-detail">
                                    <h3>Measurement analysis</h3>

                                    {Object.entries(recommendation.measurementAnalysis).map(
                                        ([key, value]) => (
                                            <p key={key}>
                                                <strong>{key}:</strong> {value}
                                            </p>
                                        )
                                    )}
                                </div>
                            )}

                        {recommendation.warnings?.length > 0 && (
                            <div className="result-detail">
                                <h3>Important notes</h3>
                                <ul>
                                    {recommendation.warnings.map((warning, index) => (
                                        <li key={index}>{warning}</li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        <p className="chart-verification">
                            Size chart: {recommendation.chartVerified ? 'Verified' : 'Not verified'}
                        </p>
                    </section>
                )}
            </section>
        </main>
    )
}

export default FindSizePage;
