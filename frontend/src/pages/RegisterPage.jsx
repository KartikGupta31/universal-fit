
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, UserRound, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import './AuthPage.css'

function RegisterPage() {
    const { register } = useAuth()
    const navigate = useNavigate()

    const [displayName, setDisplayName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')

        if (password.length < 8) {
            setError('Password must contain at least 8 characters.')
            return
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match.')
            return
        }

        setLoading(true)

        try {
            await register(email, password, displayName)
            navigate('/find-size')
        } catch (err) {
            setError(err.message || 'Unable to create your account.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-card">
                <div className="auth-icon">
                    <UserRound size={22} />
                </div>

                <p className="auth-overline">START YOUR FIT JOURNEY</p>
                <h1>Create your profile.</h1>

                <p className="auth-description">
                    One profile. Every brand. Create your account and discover your
                    perfect fit.
                </p>

                <form onSubmit={handleSubmit} className="auth-form">
                    <label htmlFor="displayName">FULL NAME</label>
                    <input
                        id="displayName"
                        type="text"
                        placeholder="Your full name"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        autoComplete="name"
                        required
                    />

                    <label htmlFor="registerEmail">EMAIL ADDRESS</label>
                    <input
                        id="registerEmail"
                        type="email"
                        placeholder="you@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        autoComplete="email"
                        required
                    />

                    <label htmlFor="registerPassword">PASSWORD</label>
                    <div className="password-wrapper">
                        <input
                            id="registerPassword"
                            type={showPassword ? 'text' : 'password'}
                            placeholder="Minimum 8 characters"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="new-password"
                            minLength={8}
                            required
                        />

                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? 'Hide password' : 'Show password'}
                        >
                            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>

                    <label htmlFor="confirmPassword">CONFIRM PASSWORD</label>
                    <div className="password-wrapper">
                        <input
                            id="confirmPassword"
                            type={showConfirmPassword ? 'text' : 'password'}
                            placeholder="Enter your password again"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            autoComplete="new-password"
                            required
                        />

                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            aria-label={
                                showConfirmPassword ? 'Hide password' : 'Show password'
                            }
                        >
                            {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>

                    {error && <p className="auth-error">{error}</p>}

                    <button type="submit" disabled={loading}>
                        {loading ? 'CREATING ACCOUNT...' : 'CREATE ACCOUNT'}
                        {!loading && <ArrowRight size={17} />}
                    </button>
                </form>

                <p className="auth-switch">
                    Already have an account? <Link to="/login">Sign in</Link>
                </p>
            </section>
        </main>
    )
}

export default RegisterPage;
