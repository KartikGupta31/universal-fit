import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getCurrentUser, getMeasurements } from '../services/measurementService'
import './AccountPage.css'

function AccountPage() {
    const { user: authUser, logout } = useAuth()
    const navigate = useNavigate()
    const [account, setAccount] = useState(authUser)
    const [measurements, setMeasurements] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        let active = true

        async function loadAccount() {
            try {
                const currentUser = await getCurrentUser()
                if (!active) return
                setAccount(currentUser)

                if (currentUser.measurementConsent) {
                    const profile = await getMeasurements()
                    if (active) setMeasurements(profile)
                }
            } catch (err) {
                if (active) setError(err.message || 'Could not load your account.')
            } finally {
                if (active) setLoading(false)
            }
        }

        loadAccount()
        return () => { active = false }
    }, [])

    function handleLogout() {
        logout()
        navigate('/', { replace: true })
    }

    const measurementCount = Object.keys(measurements?.values || {}).filter(
        key => measurements.values[key] != null
    ).length

    if (loading) {
        return <section className="account-page"><div className="account-container"><p>Loading your account...</p></div></section>
    }

    return (
        <section className="account-page">
            <div className="account-container">
                <p className="account-eyebrow">UNIVERSAL FIT / YOUR SPACE</p>
                <div className="account-heading">
                    <div>
                        <h1>My Account</h1>
                        <p>Manage your profile, measurements and account access.</p>
                    </div>
                    <span className="account-role">{account?.role || 'USER'}</span>
                </div>

                {error && <p className="account-error" role="alert">{error}</p>}

                <section className="account-card">
                    <div className="account-card-heading">
                        <div className="account-avatar">{(account?.displayName || account?.name || account?.email || 'U').charAt(0).toUpperCase()}</div>
                        <div>
                            <h2>{account?.displayName || account?.name || 'Your profile'}</h2>
                            <p>{account?.email || 'Email not available'}</p>
                        </div>
                    </div>
                    <div className="account-details">
                        <div><span>Account status</span><strong>{account?.enabled === false ? 'Disabled' : 'Active'}</strong></div>
                        <div><span>Measurement consent</span><strong>{account?.measurementConsent ? 'Granted' : 'Not granted'}</strong></div>
                    </div>
                </section>

                <section className="account-card">
                    <div className="account-section-title">
                        <div>
                            <p className="account-eyebrow">YOUR FIT PROFILE</p>
                            <h2>Body measurements</h2>
                        </div>
                        <span className="account-count">{measurements ? `${measurementCount} saved` : 'Not set up'}</span>
                    </div>
                    {account?.measurementConsent ? (
                        measurements ? (
                            <>
                                <p className="account-muted">Your saved measurements are used to calculate size recommendations across supported brands.</p>
                                <div className="account-measurement-summary">
                                    <span>Unit: <strong>{measurements.unit || 'CM'}</strong></span>
                                    <span>Fit preference: <strong>{measurements.preferredFit || 'REGULAR'}</strong></span>
                                    {measurements.weightKg != null && <span>Weight: <strong>{measurements.weightKg} kg</strong></span>}
                                </div>
                            </>
                        ) : <p className="account-muted">No measurement profile found yet. Add your measurements to get started.</p>
                    ) : <p className="account-muted">Grant measurement consent when you're ready to save a private fit profile.</p>}
                    <Link className="account-primary-link" to="/find-size">{measurements ? 'VIEW OR UPDATE MEASUREMENTS' : 'SET UP MY MEASUREMENTS'}</Link>
                </section>

                <section className="account-actions-card">
                    <div>
                        <h2>Ready to find your fit?</h2>
                        <p>Use your saved measurements to get a size recommendation for a brand and category.</p>
                    </div>
                    <Link className="account-primary-link" to="/find-size">FIND MY SIZE</Link>
                </section>

                <div className="account-footer-actions">
                    <Link to="/">Back to home</Link>
                    <button type="button" onClick={handleLogout}>LOG OUT</button>
                </div>
            </div>
        </section>
    )
}

export default AccountPage
